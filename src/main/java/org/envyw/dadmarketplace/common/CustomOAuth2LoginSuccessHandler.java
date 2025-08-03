package org.envyw.dadmarketplace.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.envyw.dadmarketplace.security.dto.DiscordUserDto;
import org.envyw.dadmarketplace.security.jwt.JwtTokenService;
import org.envyw.dadmarketplace.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class CustomOAuth2LoginSuccessHandler implements ServerAuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    @Value("${app.domain}")
    private String DOMAIN;
    @Value("${app.login.redirect-url}")
    private String REDIRECT_URL;
    @Value("${app.jwt.access-token-expiration:7200}")
    private long ACCESS_TOKEN_EXPIRATION;
    @Value("${app.jwt.refresh-token-expiration:604800}")
    private long REFRESH_TOKEN_EXPIRATION;

    public DiscordUserDto extractDiscordUserInfo(OAuth2User oauth2User) {
        String id = oauth2User.getAttribute("id");
        String username = oauth2User.getAttribute("username");
        String avatar = oauth2User.getAttribute("avatar");
        String email = oauth2User.getAttribute("email");
        String displayName = oauth2User.getAttribute("global_name");

        String avatarUrl = Optional.ofNullable(avatar)
                .filter(a -> !a.isBlank())
                .map(a -> String.format("https://cdn.discordapp.com/avatars/%s/%s.png", id, a))
                .orElse("https://dafault-avatar-url.png");

        return new DiscordUserDto(id, username, avatarUrl, email, displayName);
    }

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        ServerHttpResponse response = webFilterExchange.getExchange().getResponse();

        if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
            OAuth2User oauth2User = oauth2Token.getPrincipal();
            DiscordUserDto userInfo = this.extractDiscordUserInfo(oauth2User);

            log.info("디스코드 사용자 인증 성공: id={}, username={}, avatar={}, displayName={}", userInfo.id(),
                    userInfo.username(),
                    userInfo.avatarUrl(),
                    userInfo.displayName());

            return userService.saveOrUpdateUser(userInfo)
                    .flatMap(savedUser -> {
                        String accessToken = jwtTokenService.generateAccessToken(savedUser);
                        String refreshToken = jwtTokenService.generateRefreshToken(savedUser);

                        return sendJwtTokenResponse(response, accessToken, refreshToken);
                    })
                    .onErrorResume(error -> {
                        log.error("OAuth2 인증 성공 처리 중 오류 발생", error);

                        return redirectToHomePage(response);
                    });
        }

        return redirectToHomePage(response);
    }

    private Mono<Void> sendJwtTokenResponse(ServerHttpResponse response,
                                            String accessToken,
                                            String refreshToken) {
        try {
            response.setStatusCode(HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", accessToken)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Lax")
                    .path("/")
                    .domain(DOMAIN)
                    .maxAge(Duration.ofSeconds(ACCESS_TOKEN_EXPIRATION))
                    .build();

            ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Lax")
                    .path("/")
                    .domain(DOMAIN)
                    .maxAge(Duration.ofSeconds(REFRESH_TOKEN_EXPIRATION))
                    .build();

            response.addCookie(accessTokenCookie);
            response.addCookie(refreshTokenCookie);
        } catch (Exception e) {
            log.error("JWT 토큰 응답 생성 실패", e);
        }

        return redirectToHomePage(response);
    }

    private Mono<Void> redirectToHomePage(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FOUND);
        response.getHeaders().setLocation(URI.create(REDIRECT_URL));

        return response.setComplete();
    }
}
