package org.envyw.dadmarketplace.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.envyw.dadmarketplace.security.jwt.exception.JwtAuthenticationException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationWebFilter implements WebFilter {

    private final JwtTokenService jwtTokenService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (isOAuth2Path(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        try {
            String token = jwtTokenService.extractTokenFromBearer(authHeader);
            Authentication authentication = jwtTokenService.authenticate(token);

            log.info("JWT 인증 성공: path={}, user={}", path, authentication.getName());

            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));

        } catch (JwtAuthenticationException e) {
            log.warn("JWT 인증 실패: path={}, error={}", path, e.getMessage());

            return chain.filter(exchange);
        } catch (Exception e) {
            log.error("JWT 필터 처리 중 예상치 못한 오류 발생: path={}", path, e);

            return chain.filter(exchange);
        }
    }

    private boolean isOAuth2Path(String path) {
        return path.startsWith("/oauth2/") ||
                path.startsWith("/login/oauth2/") ||
                path.equals("/api/auth/login/discord") ||
                path.equals("/api/auth/status");
    }
}
