package com.example.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

@SpringBootTest
class ModulithTests {

    @Test
    void contextLoads() {
        var am = ApplicationModules.of(ServiceApplication.class);
        am.verify();

        for (var m : am)
            System.out.println("module [" + m + "]");

        // Creates all-docs.adoc starting with Spring Modulith 1.2.2
        new Documenter(am).writeDocumentation();
    }

}
