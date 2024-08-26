package com.example.processor;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Collection;


@SpringBootApplication
public class ProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcessorApplication.class, args);
    }
}

@Configuration
class IntegrationConfiguration {

    @Bean
    IntegrationFlow inboundAmqpMessagesFlow(AdoptionService as, ConnectionFactory connectionFactory) {
        var inbound = Amqp
                .inboundAdapter(connectionFactory, "adoptions");
        return IntegrationFlows
                .from(inbound)
                .transform(new JsonToObjectTransformer(DogAdoption.class))
                .<DogAdoption>handle((payload, headers) -> {
                    as.process(payload);
                    return null;
                })
                .get();
    }


}

@Entity
class DogAdoption {

    // why JPA, why?
    public DogAdoption() {
    }

    @Id
    @GeneratedValue
    private Integer id;

    private int dog;

    DogAdoption(@JsonProperty("dog") int dog) {
        this.dog = dog;
    }

    public Integer getId() {
        return id;
    }

    public int getDog() {
        return dog;
    }

    @Override
    public String toString() {
        return "DogAdoption{" +
                "id=" + id +
                ", dog=" + dog +
                '}';
    }
}

@Service
@Transactional
class AdoptionService {

    private final DogAdoptionRepository dogAdoptionRepository;

    AdoptionService(DogAdoptionRepository dogAdoptionRepository) {
        this.dogAdoptionRepository = dogAdoptionRepository;
    }

    void process(DogAdoption dogAdoption) {
        var save = this.dogAdoptionRepository.save(dogAdoption);
        System.out.println(save);
    }

}

interface DogAdoptionRepository extends JpaRepository<DogAdoption, Integer> {
}

@Controller
@ResponseBody
class AdoptionController {

    private final DogAdoptionRepository dogAdoptionRepository;

    AdoptionController(DogAdoptionRepository dogAdoptionRepository) {
        this.dogAdoptionRepository = dogAdoptionRepository;
    }

    @GetMapping
    Collection<DogAdoption> dogAdoptions() {
        return this.dogAdoptionRepository.findAll();
    }
}