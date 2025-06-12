package org.envyw.dadmarketplace.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@WebFluxTest(AuthController.class)
@Import(TestSecurityConfig.class)
public class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("인증된 Discord OAuth2 사용자의 상태를 확인할 수 있어야 한다")
    public void shouldCheckOAuth2AuthenticationStatus() {
        // Given
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "12345678");
        attributes.put("username", "테스트유저");
        attributes.put("discriminator", "1234");
        attributes.put("avatar", "abcdef");
        attributes.put("email", "test@example.com");

        OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "id");

        OAuth2AuthenticationToken authenticationToken = new OAuth2AuthenticationToken(
                oauth2User,
                oauth2User.getAuthorities(),
                "discord");

        // When, Then
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authenticationToken))
                .get()
                .uri("/api/auth/status")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.authenticated").isEqualTo(true);
    }

    @Test
    @DisplayName("인증되지 않은 사용자의 상태도 올바르게 확인되어야 한다")
    public void shouldHandleUnauthenticatedUser() {
        // When, Then
        webTestClient.get()
                .uri("/api/auth/status")
                .exchange()
                .expectStatus().isFound()
                .expectHeader().valueMatches("Location", "/oauth2/authorization/discord");
    }
}
