package com.example.service.adoption;

import org.springframework.data.repository.ListCrudRepository;

interface DogRepository extends ListCrudRepository<Dog, Integer> {
}
