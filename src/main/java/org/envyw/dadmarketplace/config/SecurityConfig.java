package org.envyw.dadmarketplace.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;

import java.net.URI;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/", "/api/auth/login/**", "/oauth2/**").permitAll()
                        .anyExchange().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .authenticationSuccessHandler(authenticationSuccessHandler()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }

    @Bean
    public ServerAuthenticationSuccessHandler authenticationSuccessHandler() {
        RedirectServerAuthenticationSuccessHandler successHandler =
                new RedirectServerAuthenticationSuccessHandler();

        successHandler.setLocation(URI.create("/"));

        return successHandler;
    }
}
