package com.example.service.vet;

import com.example.service.dogs.DogAdoptedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
class Vet {

    @ApplicationModuleListener
    void on(DogAdoptedEvent dae) throws Exception {
        System.out.println("start dog adopted event [" + dae + "]");
        System.out.println("stop dog adopted event [" + dae + "]");
    }
}
