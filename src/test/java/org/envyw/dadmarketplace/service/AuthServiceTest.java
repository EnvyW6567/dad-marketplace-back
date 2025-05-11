package org.envyw.dadmarketplace.service;

import org.envyw.dadmarketplace.exception.errorCode.AuthErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("디스코드 인증 URL을 정상적으로 반환해야 한다")
    public void shouldReturnDiscordAuthorizationUrl() {
        // Given
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        String clientRegistrationId = "discord";
        String expectedUrl = "https://discord.com/api/oauth2/authorize?client_id=mock-client-id&response_type=code&redirect_uri=http://localhost:8080/login/oauth2/code/discord&scope=identify%20email";

        OAuth2AuthorizationRequest authRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("mock-client-id")
                .authorizationUri("https://discord.com/api/oauth2/authorize")
                .redirectUri("http://localhost:8080/login/oauth2/code/discord")
                .scopes(Set.of("identify", "email"))
                .state("mock-state")
                .authorizationRequestUri(expectedUrl)
                .build();

        when(authorizationRequestResolver.resolve(any(ServerWebExchange.class), eq(clientRegistrationId)))
                .thenReturn(Mono.just(authRequest));

        // When
        Mono<String> resultMono = authService.getAuthorizationRequestUrl(exchange, clientRegistrationId);

        // Then
        StepVerifier.create(resultMono)
                .assertNext(url -> assertThat(url).isEqualTo(expectedUrl))
                .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 클라이언트 등록 ID에 대해 빈 Mono를 반환해야 한다")
    public void shouldReturnEmptyMonoForNonExistentClientRegistrationId() {
        // Given
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        String nonExistentClientId = "non-existent";

        when(authorizationRequestResolver.resolve(any(ServerWebExchange.class), eq(nonExistentClientId)))
                .thenReturn(Mono.empty());

        // When
        Mono<String> resultMono = authService.getAuthorizationRequestUrl(exchange, nonExistentClientId);

        // Then
        StepVerifier.create(resultMono)
                .verifyComplete();
    }

    @Test
    @DisplayName("OAuth 인증 과정에서 예외가 발생하면 커스텀 에러로 처리되어야 한다")
    public void shouldHandleOAuthExceptionWithCustomError() {
        // Given
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        String clientRegistrationId = "discord";

        when(authorizationRequestResolver.resolve(any(ServerWebExchange.class), eq(clientRegistrationId)))
                .thenReturn(Mono.error(new IllegalArgumentException("OAuth 인증 과정에서 오류 발생")));

        // When
        Mono<String> resultMono = authService.getAuthorizationRequestUrl(exchange, clientRegistrationId);

        // Then
        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable ->
                        throwable instanceof AuthenticationException &&
                                throwable.getMessage().contains(AuthErrorCode.OAUTH_URL_GENERATION_FAILED.formatMessage()))
                .verify();
    }
}
