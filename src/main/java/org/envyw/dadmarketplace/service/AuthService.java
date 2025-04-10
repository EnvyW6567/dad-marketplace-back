package org.envyw.dadmarketplace.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver;

    public Mono<String> getAuthorizationRequestUrl(ServerWebExchange exchange, String clientRegistrationId) {
        return authorizationRequestResolver
                .resolve(exchange, clientRegistrationId)
                .map(OAuth2AuthorizationRequest::getAuthorizationRequestUri);
    }
}
