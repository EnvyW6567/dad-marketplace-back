package org.envyw.dadmarketplace.security;

import org.envyw.dadmarketplace.common.CustomOAuth2LoginSuccessHandler;
import org.envyw.dadmarketplace.entity.User;
import org.envyw.dadmarketplace.security.dto.DiscordUserDto;
import org.envyw.dadmarketplace.security.jwt.JwtTokenService;
import org.envyw.dadmarketplace.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
public class CustomOAuth2LoginSuccessHandlerTest {

    @Value("${app.login.redirect-url}")
    private String REDIRECT_URL = "www.highrollermarket.com";
    @Mock
    private WebFilterExchange webFilterExchange;
    @Mock
    private ServerWebExchange exchange;
    @Mock
    private MockServerHttpResponse response;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private UserService userService;

    @Autowired
    private CustomOAuth2LoginSuccessHandler customOAuth2LoginSuccessHandler;

    @BeforeEach
    void setup() {
        System.setProperty("app.login.redirect-url", "www.highrollermarket.com");
    }

    @Test
    @DisplayName("OAuth2User에서 디스코드 사용자 정보를 추출할 수 있어야 한다")
    public void shouldExtractDiscordUserInfo() {
        // given
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "12345678");
        attributes.put("username", "테스트유저");
        attributes.put("avatar", "abcdef");
        attributes.put("email", "test@example.com");

        OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                "id");

        // when
        DiscordUserDto result = customOAuth2LoginSuccessHandler.extractDiscordUserInfo(oauth2User);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo("12345678");
        assertThat(result.username()).isEqualTo("테스트유저");
        assertThat(result.avatarUrl()).contains("12345678/abcdef");
        assertThat(result.email()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("인증 성공 시 홈페이지로 리다이렉트되어야 한다")
    public void shouldRedirectToHomePageOnSuccess() {
        // given
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "12345678");
        attributes.put("username", "테스트유저");

        OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                "id");

        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oauth2User,
                Collections.emptyList(),
                "discord");
        User mockUser = User.builder()
                .id(1L)
                .discordId("12345678")
                .username("테스트유저")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();


        when(userService.saveOrUpdateUser(any(DiscordUserDto.class)))
                .thenReturn(Mono.just(mockUser));
        when(webFilterExchange.getExchange()).thenReturn(exchange);
        when(exchange.getResponse()).thenReturn(response);
        when(response.setStatusCode(HttpStatus.FOUND)).thenReturn(true);
        when(response.getHeaders()).thenReturn(mock(HttpHeaders.class));
        when(response.setComplete()).thenReturn(Mono.empty());
        when(jwtTokenService.generateAccessToken(any(User.class))).thenReturn("jwt.token.access");
        when(jwtTokenService.generateRefreshToken(any(User.class))).thenReturn("jwt.token.refresh");

        // when
        Mono<Void> result = customOAuth2LoginSuccessHandler.onAuthenticationSuccess(webFilterExchange, authentication);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.FOUND);
        verify(response.getHeaders()).setLocation(URI.create(REDIRECT_URL));
    }
}
