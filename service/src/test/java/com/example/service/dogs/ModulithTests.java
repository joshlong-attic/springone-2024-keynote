package com.example.service.dogs;

import com.example.service.ServiceApplication;
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

        new Documenter(am)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }

}
