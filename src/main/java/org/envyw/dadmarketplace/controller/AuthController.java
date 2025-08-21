package org.envyw.dadmarketplace.controller;

import lombok.RequiredArgsConstructor;
import org.envyw.dadmarketplace.application.port.out.JwtOutPort;
import org.envyw.dadmarketplace.common.utils.CookieUtil;
import org.envyw.dadmarketplace.common.utils.TokenType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtOutPort jwtOutPort;
    private final CookieUtil cookieUtil;

    @GetMapping("/login/discord")
    public Mono<Void> discordLogin(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();

        response.setStatusCode(HttpStatus.FOUND);
        response.getHeaders().setLocation(URI.create("/oauth2/authorization/discord"));

        return response.setComplete();
    }

    @PostMapping("/refresh")
    public Mono<Void> refresh(ServerWebExchange exchange) {
        String refreshToken = cookieUtil.extractTokenFromCookie(exchange, TokenType.REFRESH_TOKEN);
        String accessToken = jwtOutPort.refresh(refreshToken);

        ResponseCookie accessTokenCookie = cookieUtil.generateJwtCookie(accessToken, TokenType.ACCESS_TOKEN);
        ServerHttpResponse response = exchange.getResponse();

        response.addCookie(accessTokenCookie);
        response.setStatusCode(HttpStatus.OK);

        return response.setComplete();
    }
}
