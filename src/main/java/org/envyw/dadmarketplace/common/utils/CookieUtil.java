package org.envyw.dadmarketplace.common.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import java.time.Duration;

@Component
public class CookieUtil {
    @Value("${app.domain}")
    private String DOMAIN;

    public String extractTokenFromCookie(ServerWebExchange exchange, TokenType tokenType) {
        MultiValueMap<String, HttpCookie> cookies = exchange.getRequest().getCookies();

        if (cookies == null || !cookies.containsKey(tokenType.getTokenName())) {
            return null;
        }
        HttpCookie cookie = cookies.getFirst(tokenType.getTokenName());

        return cookie != null ? cookie.getValue() : null;
    }

    public ResponseCookie generateJwtCookie(String token, TokenType tokenType) {
        return ResponseCookie.from(tokenType.getTokenName(), token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .domain(DOMAIN)
                .maxAge(Duration.ofSeconds(tokenType.getExpiration()))
                .build();
    }

}
