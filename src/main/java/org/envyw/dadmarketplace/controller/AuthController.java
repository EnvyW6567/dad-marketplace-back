package org.envyw.dadmarketplace.controller;

import lombok.RequiredArgsConstructor;
import org.envyw.dadmarketplace.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @GetMapping("/login/discord")
    public Mono<ResponseEntity<Map<String, String>>> getDiscordLoginUrl(ServerWebExchange exchange) {
        return authService.getAuthorizationRequestUrl(exchange, "discord")
                .map(authorizationUrl -> ResponseEntity.ok(Map.of("redirectUrl", authorizationUrl)))
                .switchIfEmpty(Mono.just(ResponseEntity.badRequest().build()));
    }

    @GetMapping("/status")
    public Mono<Map<String, Boolean>> getAuthStatus(ServerWebExchange exchange) {
        return exchange.getPrincipal()
                .map(principal -> Map.of("authenticated", true))
                .defaultIfEmpty(Map.of("authenticated", false));
    }
}
