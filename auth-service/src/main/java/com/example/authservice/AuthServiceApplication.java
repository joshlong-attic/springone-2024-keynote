package com.example.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.util.List;
import java.util.Set;

import static org.springframework.security.oauth2.core.AuthorizationGrantType.*;

@SpringBootApplication
public class AuthServiceApplication {

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
		var builder = User.builder().roles("USER").password(passwordEncoder.encode("secret"));
		var users = Set.of(builder.username("cora").build(), builder.username("josh").build());
		return new InMemoryUserDetailsManager(users);
	}

	@Bean
	RegisteredClientRepository registeredClientRepository(PasswordEncoder passwordEncoder) {
		var dogsClient = RegisteredClient
				.withId("dogs")
				.clientId("dogs")
				.clientSecret(passwordEncoder.encode("dogs"))
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.redirectUri("http://127.0.0.1:1010/login/oauth2/code/dogs")
				.authorizationGrantTypes(c -> c.addAll(Set.of(CLIENT_CREDENTIALS, REFRESH_TOKEN, AUTHORIZATION_CODE)))
				.scope("openid")
				.clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
				.build();
		return new InMemoryRegisteredClientRepository(List.of(dogsClient));
	}

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}
}
