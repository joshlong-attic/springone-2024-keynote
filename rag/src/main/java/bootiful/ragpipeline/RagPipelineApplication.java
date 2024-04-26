package bootiful.ragpipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringBootApplication
public class RagPipelineApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagPipelineApplication.class, args);
    }
}

@Configuration
class Demo implements ApplicationRunner {

    private final Resource prompt = new ClassPathResource("/prompt.txt");

    private final ObjectMapper objectMapper;
    private final ProductRepository repository;
    private final VectorStore vectorStore;
    private final ChatClient cc;
    private final TokenTextSplitter tokenTextSplitter;

    Demo(ObjectMapper objectMapper, ProductRepository repository, VectorStore vectorStore, ChatClient cc, TokenTextSplitter tokenTextSplitter) {
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.vectorStore = vectorStore;
        this.cc = cc;
        this.tokenTextSplitter = tokenTextSplitter;
    }

    @Bean
    JdbcClient jdbcClient(DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }

    @Bean
    TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }

    Recommendation recommend(Product product, String query) throws Exception {
        var productIdKey = "productId";
        var explanationKey = "explanation";
        var systemPrompt = prompt
                .getContentAsString(Charset.defaultCharset())
                .formatted(productIdKey, explanationKey);

        var similar = this.vectorStore
                .similaritySearch(SearchRequest.query(product.name() + " " + product.description()))
                .parallelStream()
                .map(doc -> repository.findById((Integer) doc.getMetadata().get("id")).get())
                .map(p -> p.id() + " | " + p.name() + " | " + p.description())
                .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));

        var systemPromptTemplate = new PromptTemplate(systemPrompt);
        var prompt = systemPromptTemplate.create(Map.of("products", similar, "question", query));

        var response = cc.call(prompt).getResult().getOutput().getContent();
        if (StringUtils.hasText(response)) {
            var map = this.objectMapper.readValue(response, Map.class);
            return new Recommendation(repository.findById(Integer.parseInt((String) map.get(productIdKey))).get(),
                    (String) map.get(explanationKey));
        }
        return null;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        for (var product : repository.findAll()) {
            var doc = new Document(
                    product.name() + " " + product.description(),
                    Map.of("price", product.price(), "description", product.description(),
                            "sku", product.sku(), "name", product.name(), "id", product.id()));
            vectorStore.accept(tokenTextSplitter.apply(List.of(doc)));
        }
        var product = repository.findById(40).get();
        var recommended = recommend(product, "i want something that will look flattering and comfortable in the spring time.");
        var response = cc.call(new Prompt("what's the weather in the south pole?",
                OpenAiChatOptions.builder().withFunction("weather").build()));
    }

    @Component
    @Description("gets the weather for a location")
    static class MockWeatherService implements Function<MockWeatherService.Request, MockWeatherService.Response> {

        enum Unit {C, F}

        record Request(String location, Unit unit) {
        }

        record Response(double temp, Unit unit) {
        }

        @Override
        public Response apply(Request request) {
            return new Response(42.0, Unit.C);
        }
    }
}

record Recommendation(Product product, String explanation) {
}

interface ProductRepository extends ListCrudRepository<Product, Integer> {
}

record Product(@Id Integer id, String description, String name, String sku, float price) {
}