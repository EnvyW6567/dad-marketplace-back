package org.envyw.dadmarketplace.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.envyw.dadmarketplace.security.dto.DiscordUserDto;
import org.envyw.dadmarketplace.service.UserService;
import org.springframework.http.HttpStatus;
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
        if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
            OAuth2User oauth2User = oauth2Token.getPrincipal();
            DiscordUserDto userInfo = this.extractDiscordUserInfo(oauth2User);

            log.info("디스코드 사용자 인증 성공: id={}, username={}, avatar={}, displayName={}", userInfo.id(),
                    userInfo.username(),
                    userInfo.avatarUrl(),
                    userInfo.displayName());

            return userService.saveOrUpdateUser(userInfo)
                    .doOnSuccess(savedUser ->
                            log.info("사용자 정보 저장/업데이트 완료: id={}, dbId={}",
                                    savedUser.getDiscordId(), savedUser.getId()))
                    .doOnError(error ->
                            log.error("사용자 정보 저장/업데이트 실패: discordId={}, error={}",
                                    userInfo.id(), error.getMessage(), error))
                    .then(redirectToHomePage(webFilterExchange))
                    .onErrorResume(error -> {
                        log.error("OAuth2 인증 성공 처리 중 오류 발생", error);
                        return redirectToHomePage(webFilterExchange);
                    });
        }

        return redirectToHomePage(webFilterExchange);
    }

    private Mono<Void> redirectToHomePage(WebFilterExchange webFilterExchange) {
        ServerHttpResponse response = webFilterExchange.getExchange().getResponse();
        response.setStatusCode(HttpStatus.FOUND);
        response.getHeaders().setLocation(URI.create("/"));
        return response.setComplete();
    }
}
