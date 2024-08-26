package com.example.service.dogs;

import org.springframework.modulith.events.Externalized;

@Externalized(target = "adoptions::adoptions")
public record DogAdoptedEvent(int dog) {
}
