package org.envyw.dadmarketplace.security.dto;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.WebFilterExchange;
import reactor.core.publisher.Mono;

public class CustomOAuth2LoginSuccessHandler {

    public DiscordUserDto extractDiscordUserInfo(OAuth2User oAuth2User) {
        return new DiscordUserDto();
    }

    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange,
                                              OAuth2AuthenticationToken authentication) {

        return null;
    }
}
