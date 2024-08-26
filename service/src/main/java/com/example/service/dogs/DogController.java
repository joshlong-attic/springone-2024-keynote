package com.example.service.dogs;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

// cora's code
@Transactional
@Controller
@ResponseBody
@RequestMapping ("/dogs")
class DogController {

    private final DogRepository repository;

    private final ApplicationEventPublisher publisher;

    DogController(DogRepository repository, ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @GetMapping
    Collection<Dog> getDogs() {
        return this.repository.findAll();
    }

    @PostMapping("/{dogId}/adoptions")
    void adopt(@PathVariable Integer dogId,
               @RequestBody Map<String, String> owner) {
        this.repository.findById(dogId).ifPresent(dog -> {
            // Set the new owner's name
            var nDog = new Dog(dog.id(), dog.gender(), dog.name(), owner.get("name"), dog.dob(), dog.description() ,
                    dog.image());
            var saved = this.repository.save(nDog);
            System.out.println("adopted [" + saved + "]");

            // Tell any module in the application!
            this.publisher.publishEvent(new DogAdopted(dog.id()));
        });
    }
}

interface DogRepository extends ListCrudRepository<Dog, Integer> {
}

// look mom, no Lombok!
record Dog(@Id Integer id, char gender, String name, String owner, LocalDate dob, String description ,
           String image) {
}

record DogAdoptionSuggestion(Integer id, String name, String description, Date pickup) {
}


