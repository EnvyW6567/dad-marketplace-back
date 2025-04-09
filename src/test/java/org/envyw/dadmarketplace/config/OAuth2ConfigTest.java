package org.envyw.dadmarketplace.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureWebTestClient
public class OAuth2ConfigTest {

    @Autowired
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @Test
    @DisplayName("디스코드 OAuth2 클라이언트 설정이 존재해야 한다")
    public void discordClientRegistrationShouldExist() {
        // When
        Mono<Boolean> hasDiscordRegistration = clientRegistrationRepository.findByRegistrationId("discord")
                .map(registration -> true)
                .defaultIfEmpty(false);

        // Then
        StepVerifier.create(hasDiscordRegistration)
                .assertNext(exists -> assertThat(exists).isTrue())
                .verifyComplete();
    }

    @Test
    @DisplayName("디스코드 OAuth2 클라이언트는 올바른 인증 타입과 스코프를 가져야 한다")
    public void discordClientShouldHaveCorrectGrantTypeAndScopes() {
        // When
        Mono<ClientRegistration> discordClient = clientRegistrationRepository.findByRegistrationId("discord");

        // Then
        StepVerifier.create(discordClient)
                .assertNext(registration -> {

                    assertThat(registration.getAuthorizationGrantType())
                            .isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);

                    assertThat(registration.getScopes())
                            .containsExactlyInAnyOrder("identify", "email");

                    assertThat(registration.getRedirectUri())
                            .endsWith("/login/oauth2/code/discord");
                })
                .verifyComplete();
    }
}
