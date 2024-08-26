package com.example.gateway;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.rewritePath;
import static org.springframework.cloud.gateway.server.mvc.filter.TokenRelayFilterFunctions.tokenRelay;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.security.config.Customizer.withDefaults;


@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    private static final String WILDCARD = "**";

    private static final String API_SERVICE_PREFIX = "/api/";
    private static final String API_SERVICE_HOST = "http://localhost:8080";

    private static final String UI_PREFIX = "/";
    private static final String UI_HOST = "http://localhost:9000";

    @Bean
    RouterFunction<ServerResponse> apiRoutes() {
        return route("crmGets")
                .GET(API_SERVICE_PREFIX + WILDCARD, http(API_SERVICE_HOST))
                .POST(API_SERVICE_PREFIX + WILDCARD, http(API_SERVICE_HOST))
                .before(rewritePath(API_SERVICE_PREFIX + "(?<segment>.*)", "/${segment}"))
                .filter(tokenRelay())
                .build();
    }

    @Bean
    RouterFunction<ServerResponse> uiRoute() {
        return route("ui")
                .GET(UI_PREFIX + WILDCARD, http(UI_HOST))
                .before(rewritePath(UI_PREFIX + "(?<segment>.*)", "/${segment}"))
                .build();
    }

    @Bean
    SecurityFilterChain mySecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .authorizeHttpRequests(a -> a.anyRequest().authenticated())
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .oauth2Login(withDefaults())
                .oauth2Client(withDefaults())
                .build();
    }
}