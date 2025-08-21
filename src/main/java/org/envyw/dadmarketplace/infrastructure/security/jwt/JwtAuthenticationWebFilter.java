package org.envyw.dadmarketplace.infrastructure.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.envyw.dadmarketplace.common.utils.CookieUtil;
import org.envyw.dadmarketplace.common.utils.TokenType;
import org.envyw.dadmarketplace.infrastructure.security.jwt.exception.JwtAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationWebFilter implements WebFilter {

    private final CookieUtil cookieUtil;
    private final JwtAuthenticator jwtAuthenticator;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 인증이 필요하지 않는 Path는 필터링 대상에서 제외
        if (isOAuth2Path(path)) {
            return chain.filter(exchange);
        }

        try {
            String token = cookieUtil.extractTokenFromCookie(exchange, TokenType.ACCESS_TOKEN);
            Authentication authentication = jwtAuthenticator.authenticate(token);

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
