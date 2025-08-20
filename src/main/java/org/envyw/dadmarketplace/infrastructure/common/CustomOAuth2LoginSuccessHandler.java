package org.envyw.dadmarketplace.infrastructure.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.envyw.dadmarketplace.application.service.UserService;
import org.envyw.dadmarketplace.common.enums.TokenType;
import org.envyw.dadmarketplace.common.utils.CookieUtil;
import org.envyw.dadmarketplace.infrastructure.security.dto.DiscordUser;
import org.envyw.dadmarketplace.infrastructure.security.jwt.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class CustomOAuth2LoginSuccessHandler implements ServerAuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtProvider jwtProvider;
    private final CookieUtil cookieUtil;
    @Value("${app.login.redirect-url}")
    private String REDIRECT_URL;

    public DiscordUser extractDiscordUserInfo(OAuth2User oauth2User) {
        String id = oauth2User.getAttribute("id");
        String username = oauth2User.getAttribute("username");
        String avatar = oauth2User.getAttribute("avatar");
        String email = oauth2User.getAttribute("email");
        String displayName = oauth2User.getAttribute("global_name");

        String avatarUrl = Optional.ofNullable(avatar)
                .filter(a -> !a.isBlank())
                .map(a -> String.format("https://cdn.discordapp.com/avatars/%s/%s.png", id, a))
                .orElse("https://dafault-avatar-url.png");

        return new DiscordUser(id, username, avatarUrl, email, displayName);
    }

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        ServerHttpResponse response = webFilterExchange.getExchange().getResponse();

        if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
            OAuth2User oauth2User = oauth2Token.getPrincipal();
            DiscordUser discordUser = this.extractDiscordUserInfo(oauth2User);

            log.info("디스코드 사용자 인증 성공: id={}, username={}, avatar={}, displayName={}", discordUser.id(),
                    discordUser.username(),
                    discordUser.avatarUrl(),
                    discordUser.displayName());

            return userService.saveOrUpdateUser(discordUser)
                    .flatMap(userId -> {
                        String accessToken = jwtProvider.generateAccessToken(userId);
                        String refreshToken = jwtProvider.generateRefreshToken(userId);

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
            ResponseCookie accessTokenCookie = cookieUtil.generateJwtCookie(accessToken, TokenType.ACCESS_TOKEN);
            ResponseCookie refreshTokenCookie = cookieUtil.generateJwtCookie(refreshToken, TokenType.REFRESH_TOKEN);

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
