package com.example.service.dogs;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Configuration
@RegisterReflectionForBinding(DogAdoptionSuggestion.class)
class AssistantConfiguration {

    @Bean
    @Description("determine the best time to pickup a dog for adoption")
    Function<DogAdoptionPickupRequest, DogAdoptionPickupResponse> dogAdoptionPickupTimeFunction() {
        return dogAdoptionPickupRequest -> {
            System.out.println("got a request [" + dogAdoptionPickupRequest + "]");
            return new DogAdoptionPickupResponse(new Date());
        };
    }

    @Bean
    ChatClient chatClient(ChatClient.Builder builder, DogRepository repository, VectorStore vectorStore) {

            repository.findAll().forEach(dog -> {
                var dogument = new Document("id: %s, name: %s, description: %s".formatted(
                        Integer.toString(dog.id()), dog.name(), dog.description()),
                        Map.of("dogId", dog.id()));
                vectorStore.add(List.of(dogument));
            });

        var system = """
                You are an AI powered assistant to help people adopt a dog from the adoption 
                agency named Spring's Pet Emporium with locations in Las Vegas, Krakow, Singapore, Paris, London, 
                and San Francisco. If you don't know about the dogs housed at our 
                particular stores, then return a disappointed response suggesting we don't 
                have any dogs available.
                """;
        return builder
                .defaultFunctions("dogAdoptionPickupTimeFunction")
                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults().withTopK(100)))
                .defaultSystem(system)
                .build();
    }
}

@Service
class DogAdoptionService {

    private final ChatClient chatClient;

    DogAdoptionService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    // visible for testing
    ChatClient.ChatClientRequest.CallResponseSpec doSuggest(String query) {
        return chatClient
                .prompt()
                .user(query)
                .call();
    }

    DogAdoptionSuggestion suggest(String question) {
        return doSuggest(question).entity(DogAdoptionSuggestion.class);
    }

}


@Controller
@ResponseBody
class AssistantController {

    private final DogAdoptionService dogAdoptionService;

    AssistantController(DogAdoptionService dogAdoptionService) {
        this.dogAdoptionService = dogAdoptionService;
    }

    @GetMapping("/assistant")
    DogAdoptionSuggestion dogAdoptionSuggestion(
            @RequestParam(defaultValue = "do you have any neurotics dogs?") String question) {
        return this.dogAdoptionService.suggest(question);
    }
}

record DogAdoptionPickupRequest(String dogName) {
}

record DogAdoptionPickupResponse(Date pickupTime) {
}

