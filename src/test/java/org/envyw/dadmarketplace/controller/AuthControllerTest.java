package org.envyw.dadmarketplace.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(AuthController.class)
@Import(TestSecurityConfig.class)
public class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("디스코드 로그인 엔드포인트는 OAuth 인증 URL을 반환해야 한다")
    public void discordLoginShouldReturnOAuthUrl() {
        webTestClient.get()
                .uri("/api/auth/login/discord")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.redirectUrl").isNotEmpty()
                .jsonPath("$.redirectUrl").value(url ->
                        org.assertj.core.api.Assertions.assertThat(url.toString())
                                .contains("https://discord.com/api/oauth2/authorize"));
    }

    @Test
    @WithMockUser
    @DisplayName("인증된 사용자의 상태를 확인할 수 있어야 한다")
    public void shouldCheckAuthenticationStatus() {
        webTestClient.get()
                .uri("/api/auth/status")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.authenticated").isEqualTo(true);
    }
}
