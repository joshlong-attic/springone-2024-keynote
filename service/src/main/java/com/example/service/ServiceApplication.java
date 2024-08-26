package com.example.service;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceApplication.class, args);
	}

    private final String Q = "adoptions";


    @Bean
    InitializingBean amqpInitializer(AmqpAdmin amqpAdmin) {
        return () -> {

            var queue = QueueBuilder
                    .durable(Q)
                    .build();
            var exchange = ExchangeBuilder
                    .directExchange(Q)
                    .build();
            var binding = BindingBuilder
                    .bind(queue)
                    .to(exchange)
                    .with(Q)
                    .noargs();

            amqpAdmin.declareQueue(queue );
            amqpAdmin.declareExchange(exchange);
            amqpAdmin.declareBinding(binding);

        };
    }

}
