package bootiful.ragpipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.data.annotation.Id;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static bootiful.ragpipeline.ProductsJsonLoaderJobConfiguration.JOB_NAME;

@EnableConfigurationProperties(RagPipelineConfigurationProperties.class)
@SpringBootApplication
public class RagPipelineApplication {


    private final Logger log = LoggerFactory.getLogger(getClass());

    public static void main(String[] args) {
        SpringApplication.run(RagPipelineApplication.class, args);
    }

    @Bean
    JdbcClient jdbcClient(DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }

    @Bean
    TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }

    @Bean
    @Order(10)
    @ConditionalOnProperty ("bootiful.rag.ingest-products")
    ApplicationRunner productsIngestBatchJobRunner(JobLauncher jobLauncher, @Qualifier(JOB_NAME) Job job) {
        return args -> {
            log.info("running the batch job");
            jobLauncher.run(job, new JobParametersBuilder()
                    .addJobParameter("id", UUID.randomUUID().toString(), String.class)
                    .toJobParameters());
        };
    }

    @Bean
    @Order(20)
    @ConditionalOnProperty ("bootiful.rag.create-vector-database-embeddings")
    ApplicationRunner vectorDbInitializationRunner(ProductService productService, JdbcClient jdbcClient, VectorStore vectorStore,
                                                   TokenTextSplitter tokenTextSplitter) {
        return args -> {
            log.info("initializing the vector DB");
            jdbcClient.sql("delete from vector_store");
            var products = productService.products();
            products
                    .parallelStream()
                    .forEach(product -> {
                        var doc = new Document(
                                product.name() + " " + product.description(),
                                Map.of("price", product.price(),
                                        "description", product.description(),
                                        "sku", product.sku(),
                                        "name", product.name(),
                                        "id", product.id()
                                ));
                        log.debug("adding [" +  (product) + "] to the vector db.");

                        var vectorStoreReadyDocs = tokenTextSplitter.apply(List.of(doc));

                        vectorStore.accept(vectorStoreReadyDocs);
                    });
        };
    }


    @Bean
    @Order(30)
    @ConditionalOnProperty ("bootiful.rag.run-rag-demo")
    ApplicationRunner ragDemoRunner(ProductService productService) {
        return args -> {

            log.info("running the RAG demo");

            var product = productService.byId(40);
            log.info("searching for similar records to\n{}",  (product));

            var recommended = productService
                    .recommend(product, "i want something that will look flattering and comfortable in the spring time.");

            System.out.println(recommended.toString());

        };
    }

}

record Recommendation(Product product, String explanation) {
}


@Service
class ProductService {

    private final Log log = LogFactory.getLog(getClass());

    private final JdbcClient jdbcClient;

    private final VectorStore vectorStore;

    private final ObjectMapper objectMapper;
    private final ChatClient ai;

    private final RowMapper<Product> productRowMapper = (rs, rowNum) -> new Product(
            rs.getInt("id"), rs.getString("description"),
            rs.getString("name"), rs.getString("sku"), rs.getFloat("price"));

    ProductService(JdbcClient jdbcClient, VectorStore vectorStore, ObjectMapper objectMapper, ChatClient ai) {
        this.jdbcClient = jdbcClient;
        this.vectorStore = vectorStore;
        this.objectMapper = objectMapper;
        this.ai = ai;
    }

    Recommendation recommend(Product product, String query) throws Exception {

        var systemPrompt = """
                
                You are a personal shopper assistant whose job it is to help a shopper answer 
                questions and find the best product given the shopper's questions and the following choices:
                                
                {products}
                                
                the data is presented with three columns each, delimited by "|". The ID is the first column. Please return the ID of the product that would
                be the single best response to their query. If you do not know, return an empty string. 
                       
                Here is their question:
                                
                {question}                              
                                
                If you do know, please return the response in a JSON structure with two attributes: one, `productId`, containing the ID of the product you have chosen,
                and `explanation`, containing the reasons you think this is the best choice for the shopper.  
                                
                         
                """;

        var similar = similarProductsTo(product)
                .stream()
                .map(p -> p.id() + " | " + p.name() + " | " + p.description())
                .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));

        var systemPromptTemplate = new PromptTemplate(systemPrompt);
        var prompt = systemPromptTemplate.create(Map.of("products", similar, "question", query));

        var response = this.ai.call(prompt).getResult().getOutput().getContent();
        if (StringUtils.hasText(response)) {
            var map = this.objectMapper
                    .readValue(response, Map.class);
            Assert.state(map.containsKey("productId"), "there must be an `productId` attribute");
            Assert.state(map.containsKey("explanation"), "there must be an `explanations` attribute");
            return new Recommendation(byId(Integer.parseInt((String) map.get("productId"))), (String) map.get("explanation"));
        }
        return null;
    }

      Collection<Product> similarProductsTo(Product product) {
        var similar = this.vectorStore.similaritySearch(SearchRequest.query(product.name() + " " + product.description()));
        return similar
                .parallelStream()
                .peek(doc -> log.debug( "distance " +(Float) doc.getMetadata().get("distance") +" for ID " + doc.getMetadata().get("id")))
                .map(doc -> (Integer) doc.getMetadata().get("id"))
                .filter(id -> !id.equals(product.id()))
                .map(this::byId)
                .toList();
    }

    Product byId(Integer id) {
        return this.jdbcClient
                .sql("select * from products where id = ? ")
                .param(id)
                .query(this.productRowMapper)
                .single();
    }

    Collection<Product> products() {
        return this.jdbcClient
                .sql("SELECT * FROM products")
                .query(this.productRowMapper)
                .list();
    }
}

record Product(@Id Integer id, String description, String name, String sku, float price) {
    @Override
    public String toString() {
        return  id + " " + name;
    }
}

@ConfigurationProperties(prefix = "bootiful.rag")
record RagPipelineConfigurationProperties(
        boolean ingestProducts, boolean createVectorDatabaseEmbeddings, boolean runRagDemo) {
}