package bootiful.ragpipeline;

import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class RagPipelineApplication {

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


    ApplicationRunner ragDemo(JdbcClient jdbcClient, VectorStore vectorStore) {
        return args -> {

            var log = LoggerFactory.getLogger(getClass());

            jdbcClient.sql("delete from vector_store");

            var products = jdbcClient
                    .sql("SELECT * FROM products")
                    .query((rs, rowNum) -> new Product(
                            rs.getInt("id"),
                            rs.getString("description"),
                            rs.getString("name"),
                            rs.getString("sku"),
                            rs.getFloat("price")
                    ))
                    .list();


            products
                    .parallelStream()
                    .forEach(product -> {

                        var doc = new Document(
                                product.description(),
                                Map.of("price", product.price(),
                                        "sku", product.sku(),
                                        "name", product.name(),
                                        "id", product.id()
                                ));

                        log.info("adding [" + product.id() + "] to the vector db.");
                        vectorStore.accept(List.of(doc));

                    });


        };
    }

}

record Product(@Id Integer id, String description, String name, String sku, float price) {
}