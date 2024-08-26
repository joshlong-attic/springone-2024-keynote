package com.example.service.dogs;

import org.springframework.modulith.events.Externalized;

@Externalized(target = "adoptions")
public record DogAdopted(int dog) {
}
