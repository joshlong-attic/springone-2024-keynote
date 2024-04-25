package bootiful.ragpipeline;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import javax.sql.DataSource;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
                        log.info("adding [" + product.id() + "] to the vector db.");

                        var vectorStoreReadyDocs = tokenTextSplitter.apply(List.of(doc));

                        vectorStore.accept(vectorStoreReadyDocs);
                    });
        };
    }



    @Bean
    @ConditionalOnProperty ("bootiful.rag.run-rag-demo")
    ApplicationRunner ragDemoRunner(ProductService productService) {
        return args -> {

            log.info("running the RAG demo");

            var product = productService.byId(40);
            log.info("searching for similar records to\n{}", product.id() + " " + product.name() + " " + product.description());

            log.info("results:");
            var similar = productService.similarProductsTo(product);
            log.info("found {}", similar.size());
            for (var p : similar)
                System.out.println(System.lineSeparator() + System.lineSeparator() +
                        p.name() + System.lineSeparator() + p.id() + System.lineSeparator() + p.description());

        };
    }

}

@Service
class ProductService {

    private final Log log = LogFactory.getLog(getClass());

    private final JdbcClient jdbcClient;

    private final VectorStore vectorStore;

    private final RowMapper<Product> productRowMapper = (rs, rowNum) -> new Product(
            rs.getInt("id"), rs.getString("description"),
            rs.getString("name"), rs.getString("sku"), rs.getFloat("price"));

    ProductService(JdbcClient jdbcClient, VectorStore vectorStore) {
        this.jdbcClient = jdbcClient;
        this.vectorStore = vectorStore;
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
        try {
            return this.jdbcClient
                .sql("select * from products where id = ? ")
                .param(id)
                .query(this.productRowMapper)
                    .single();
        } //
        catch (Throwable throwable) {
            log.warn("could not find the ID " + id, throwable);
        }
        throw new RuntimeException("oops!") ;
    }

    Collection<Product> products() {
        return this.jdbcClient
                .sql("SELECT * FROM products")
                .query(this.productRowMapper)
                .list();
    }
}

record Product(@Id Integer id, String description, String name, String sku, float price) {
}

@ConfigurationProperties(prefix = "bootiful.rag")
record RagPipelineConfigurationProperties(
        boolean ingestProducts, boolean createVectorDatabaseEmbeddings, boolean runRagDemo) {
}