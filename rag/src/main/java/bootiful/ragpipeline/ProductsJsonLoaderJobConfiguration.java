package bootiful.ragpipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.batch.JobExecutionEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.InputStreamReader;
import java.util.ArrayList;

@Order(100)
@Configuration
//@Profile("productsJsonLoader")
class ProductsJsonLoaderJobConfiguration {

    static final String JOB_NAME = "csvJob";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final JdbcClient db;

    ProductsJsonLoaderJobConfiguration(DataSource db) {
        this.db = JdbcClient.create(db);
    }

    private static Product from(JsonNode jsonNode) {
        var sku = jsonNode.get("SKU").textValue();
        var name = jsonNode.get("NAME").textValue();
        var description = jsonNode.get("DESCRIPTION").textValue().replace("## About this item", "").trim();
        var id = jsonNode.get("ID").intValue();
        var price = jsonNode.get("PRICE").floatValue();
        return new Product(id, description, name, sku, price);
    }

    @Bean
    ItemWriter<Product> productItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Product>()
                .dataSource(dataSource)
                .sql("insert into products(description, name, sku, price) values ( ?, ?, ?, ?)")
                .itemPreparedStatementSetter((item, ps) -> {
                    var col = 0;
                    ps.setString(++col, item.description());
                    ps.setString(++col, item.name());
                    ps.setString(++col, item.sku());
                    ps.setDouble(++col, item.price());
                })
                .assertUpdates(true)
                .build();
    }

    @Bean
    ItemReader<Product> productItemReader(ObjectMapper objectMapper, @Value("classpath:/products.json") Resource resource) throws Exception {
        var products = new ArrayList<Product>();
        try (var is = resource.getInputStream(); var reader = new InputStreamReader(is);) {
            var jsonNode = objectMapper.readValue(reader, JsonNode.class);
            jsonNode.elements().forEachRemaining(jn -> products.add(from(jn)));
        }
        log.debug("there are {} products in the source .json", products.size());
        return new ListItemReader<>(products);
    }

    @Bean
    Step fileToDbStep(JobRepository repository, PlatformTransactionManager transactionManager,
                      ItemReader<Product> productItemReader,
                      ItemWriter<Product> productItemWriter) {
        return new StepBuilder("step", repository)
                .<Product, Product>chunk(100, transactionManager)
                .reader(productItemReader)
                .writer(productItemWriter)
                .build();
    }

    @Bean
    Step initializationStep(JobRepository repository, JdbcClient jdbcClient,
                            PlatformTransactionManager transactionManager) {
        return new StepBuilder("initialize", repository)
                .tasklet((contribution, chunkContext) -> {
                    jdbcClient.sql("delete from products").update();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean(JOB_NAME)
    Job csvJob(JobRepository repository, Step initializationStep, Step fileToDbStep) {
        return new JobBuilder("json", repository)
                .flow(initializationStep)
                .next(fileToDbStep)
                .end()
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @EventListener(JobExecutionEvent.class)
    void jobExecutedEvent() {
        var count = this.db
                .sql("select * from products")
                .query((rs, rowNum) -> new Product(rs.getInt("id"),
                        rs.getString("description"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getFloat("price")))
                .stream()
                .count();
        log.info("wrote [{}] products to the DB", count);

    }
}
