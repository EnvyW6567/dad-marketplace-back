package org.envyw.dadmarketplace.service;

import lombok.RequiredArgsConstructor;
import org.envyw.dadmarketplace.exception.OAuth2AuthenticationException;
import org.envyw.dadmarketplace.exception.errorCode.AuthErrorCode;
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
                .map(OAuth2AuthorizationRequest::getAuthorizationRequestUri)
                .onErrorResume(e ->
                        Mono.error(new OAuth2AuthenticationException(
                                AuthErrorCode.OAUTH_URL_GENERATION_FAILED, e)))
                .switchIfEmpty(Mono.error(new OAuth2AuthenticationException(AuthErrorCode.CLIENT_REGISTRATION_NOT_FOUND)));
    }
}
