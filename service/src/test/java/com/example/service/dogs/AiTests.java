package com.example.service.dogs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.RelevancyEvaluator;
import org.springframework.ai.model.Content;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class AiTests {

    @Test
    @Disabled
    void evaluations(@Autowired ChatModel chatModel, @Autowired DogAdoptionService dogAdoptionService) {
        var userText = "do you have any neurotic dogs available, and if so what is the best time to pick them up?";
        var response = dogAdoptionService.doSuggest(userText).chatResponse();
        var relevancyEvaluator = new RelevancyEvaluator(ChatClient.builder(chatModel));
        var evaluationRequest = new EvaluationRequest(userText,
                (List<Content>) response.getMetadata().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS), response);
        var evaluationResponse = relevancyEvaluator.evaluate(evaluationRequest);
        Assertions.assertTrue(evaluationResponse.isPass(), "Response is not relevant to the question");
    }
}
