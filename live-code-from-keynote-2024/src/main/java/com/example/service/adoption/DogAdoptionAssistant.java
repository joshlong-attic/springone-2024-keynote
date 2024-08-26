package com.example.service.adoption;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

@Configuration
class DogAdoptionAssistant {

    @Bean
    ApplicationRunner demo(ChatClient chatClient) {
        return args -> {

            var content = chatClient
                    .prompt()
                    .user("when can I pickup Prancer?")
                    .call()
                    .entity(DogAdoptionSuggestion.class);
            System.out.println("reply [" + content + "]");

        };
    }


    @Bean
    ChatClient chatClient(
            DogRepository dogRepository,
            VectorStore vectorStore,
            ChatClient.Builder builder) {

        if (false)
            dogRepository.findAll().forEach(dog -> {
                var dogument = new Document("id: %s, name: %s, description: %s"
                        .formatted(dog.id(), dog.name(), dog.description()));
                vectorStore.add(List.of(dogument));
            });

        return builder
                .defaultFunctions("pickupTimeRequestDogAdoptionFunction")
                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
                .defaultSystem("""
                                            
                        You are an AI powered assistant to help people adopt a dog\s
                        from the adoption agency named Pooch Palace with locations in\s
                        Las Vegas, Tokyo, Krakow, Singapore, Paris, London, and Sa\s
                        Francisco. If you don't know about the dogs housed at our particular\s
                        stores, then return a disappointed response suggesting we don't\s
                        have any dogs available.
                                            
                                            """)
                .build();
    }

    @Bean
    @Description("schedule a pickup time for adopted dogs")
    Function<DogAdoptionPickupTimeRequest, DogAdoptionPickupTimeResponse> pickupTimeRequestDogAdoptionFunction() {
        return dogAdoptionPickupTimeRequest -> {
            System.out.println("hello");
            return new DogAdoptionPickupTimeResponse(Instant.now().toString()); 
        } ;
    }
}

record DogAdoptionPickupTimeRequest(String name) {
}

record DogAdoptionPickupTimeResponse(String pickupTime) {
}

record DogAdoptionSuggestion(String name,
                             String description,
                             Integer id,
                             String pickupTime) {
}