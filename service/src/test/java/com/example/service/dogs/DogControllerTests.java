package com.example.service.dogs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.events.EventExternalized;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

import java.util.Map;

@ApplicationModuleTest //(extraIncludes = "vet")
class DogControllerTests {

    @Autowired
    DogController controller;

    @Test
    void publishesEventOnAdoption(Scenario scenario) throws InterruptedException {

        scenario.stimulate(() -> controller.adopt(45, Map.of("name", "Josh")))
                .andWaitForEventOfType(EventExternalized.class)
                .matching(it -> it.getEvent() instanceof DogAdopted)
                .toArrive();
    }
}
