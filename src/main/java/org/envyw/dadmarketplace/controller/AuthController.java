package org.envyw.dadmarketplace.controller;

import lombok.RequiredArgsConstructor;
import org.envyw.dadmarketplace.dto.request.RefreshReqDto;
import org.envyw.dadmarketplace.dto.response.RefreshResDto;
import org.envyw.dadmarketplace.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login/discord")
    public Mono<Void> discordLogin(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();

        response.setStatusCode(HttpStatus.FOUND);
        response.getHeaders().setLocation(URI.create("/oauth2/authorization/discord"));

        return response.setComplete();
    }

    @GetMapping("/status")
    public Mono<Map<String, Boolean>> getAuthStatus(ServerWebExchange exchange) {
        return exchange.getPrincipal()
                .map(principal -> Map.of("authenticated", true))
                .defaultIfEmpty(Map.of("authenticated", false));
    }

    @PostMapping("/refresh")
    public Mono<RefreshResDto> refresh(@RequestBody RefreshReqDto refreshReqDto) {
        return authService.refresh(refreshReqDto);
    }
}
