package com.example.service.vet.scheduling;

import com.example.service.dogs.DogAdopted;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class Checkup {

    // Async, after parent commit, new transaction
    @ApplicationModuleListener
    void on(DogAdopted event) {
        System.out.println("scheduled [" + event.dog() + "]");
    }
}
