package org.envyw.dadmarketplace.security.jwt;

import org.envyw.dadmarketplace.security.jwt.exception.JwtAuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT 인증 웹 필터 테스트")
class JwtAuthenticationWebFilterTest {

    private JwtAuthenticationWebFilter jwtAuthenticationWebFilter;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private WebFilterChain filterChain;

    private ServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        jwtAuthenticationWebFilter = new JwtAuthenticationWebFilter(jwtTokenService);

        // Mock FilterChain이 빈 Mono를 반환하도록 설정
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("Authorization 헤더에서 JWT 토큰을 추출하여 인증할 수 있어야 한다")
    void shouldAuthenticateWithJwtFromAuthorizationHeader() {
        // Given
        String jwtToken = "valid.jwt.token";
        String bearerToken = "Bearer " + jwtToken;

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/protected")
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .build();
        exchange = MockServerWebExchange.from(request);

        // JWT 인증 성공을 위한 Mock 설정
        JwtAuthenticationToken mockAuthentication = createMockAuthentication();
        when(jwtTokenService.extractTokenFromBearer(bearerToken)).thenReturn(jwtToken);
        when(jwtTokenService.authenticate(jwtToken)).thenReturn(mockAuthentication);

        // When
        Mono<Void> result = jwtAuthenticationWebFilter.filter(exchange, filterChain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        // 검증
        verify(jwtTokenService).extractTokenFromBearer(bearerToken);
        verify(jwtTokenService).authenticate(jwtToken);
        verify(filterChain).filter(exchange);
    }

    @Test
    @DisplayName("유효하지 않은 JWT 토큰은 인증을 거부하고 다음 필터로 진행해야 한다")
    void shouldRejectInvalidJwtTokenAndProceedToNextFilter() {
        // Given
        String invalidToken = "invalid.jwt.token";
        String bearerToken = "Bearer " + invalidToken;

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/protected")
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .build();
        exchange = MockServerWebExchange.from(request);

        // JWT 인증 실패를 위한 Mock 설정
        when(jwtTokenService.extractTokenFromBearer(bearerToken)).thenReturn(invalidToken);
        when(jwtTokenService.authenticate(invalidToken))
                .thenThrow(new JwtAuthenticationException("Invalid token"));

        // When
        Mono<Void> result = jwtAuthenticationWebFilter.filter(exchange, filterChain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        // 검증: 인증 실패해도 다음 필터로 진행
        verify(jwtTokenService).extractTokenFromBearer(bearerToken);
        verify(jwtTokenService).authenticate(invalidToken);
        verify(filterChain).filter(exchange);
    }

    @Test
    @DisplayName("JWT 토큰이 없으면 다음 필터로 진행해야 한다")
    void shouldProceedToNextFilterWhenNoJwtToken() {
        // Given - Authorization 헤더가 없는 요청
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/protected")
                .build();
        exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = jwtAuthenticationWebFilter.filter(exchange, filterChain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        // 검증: JWT 관련 메서드가 호출되지 않고 바로 다음 필터로 진행
        verify(jwtTokenService, never()).extractTokenFromBearer(anyString());
        verify(jwtTokenService, never()).authenticate(anyString());
        verify(filterChain).filter(exchange);
    }

    @Test
    @DisplayName("Bearer가 아닌 Authorization 헤더는 무시하고 다음 필터로 진행해야 한다")
    void shouldIgnoreNonBearerAuthorizationHeader() {
        // Given - Basic 인증 헤더
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/protected")
                .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNzd29yZA==")
                .build();
        exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = jwtAuthenticationWebFilter.filter(exchange, filterChain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        // 검증: JWT 관련 메서드가 호출되지 않음
        verify(jwtTokenService, never()).extractTokenFromBearer(anyString());
        verify(jwtTokenService, never()).authenticate(anyString());
        verify(filterChain).filter(exchange);
    }

    @Test
    @DisplayName("OAuth2 관련 경로는 JWT 필터를 건너뛰어야 한다")
    void shouldSkipJwtFilterForOAuth2Paths() {
        // Given - OAuth2 관련 경로들
        String[] oauth2Paths = {
                "/oauth2/authorization/discord",
                "/login/oauth2/code/discord",
                "/api/auth/login/discord",
                "/api/auth/status"
        };

        for (String path : oauth2Paths) {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer some.jwt.token")
                    .build();
            exchange = MockServerWebExchange.from(request);

            // When
            Mono<Void> result = jwtAuthenticationWebFilter.filter(exchange, filterChain);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            // 검증: OAuth2 경로에서는 JWT 처리하지 않음
            verify(jwtTokenService, never()).extractTokenFromBearer(anyString());
            verify(jwtTokenService, never()).authenticate(anyString());
        }

        verify(filterChain, times(oauth2Paths.length)).filter(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("JWT 토큰 추출 중 예외 발생 시 다음 필터로 진행해야 한다")
    void shouldProceedToNextFilterWhenTokenExtractionFails() {
        // Given
        String invalidBearerToken = "Bearer ";  // 빈 토큰

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/protected")
                .header(HttpHeaders.AUTHORIZATION, invalidBearerToken)
                .build();
        exchange = MockServerWebExchange.from(request);

        // 토큰 추출 시 예외 발생
        when(jwtTokenService.extractTokenFromBearer(invalidBearerToken))
                .thenThrow(new IllegalArgumentException("토큰이 비어있습니다"));

        // When
        Mono<Void> result = jwtAuthenticationWebFilter.filter(exchange, filterChain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        // 검증: 예외 발생해도 다음 필터로 진행
        verify(jwtTokenService).extractTokenFromBearer(invalidBearerToken);
        verify(jwtTokenService, never()).authenticate(anyString());
        verify(filterChain).filter(exchange);
    }

    @Test
    @DisplayName("정상적인 JWT 토큰으로 인증 성공 시 SecurityContext에 Authentication이 설정되어야 한다")
    void shouldSetAuthenticationInSecurityContextOnSuccess() {
        // Given
        String jwtToken = "valid.jwt.token";
        String bearerToken = "Bearer " + jwtToken;

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/protected")
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .build();
        exchange = MockServerWebExchange.from(request);

        JwtAuthenticationToken mockAuthentication = createMockAuthentication();
        when(jwtTokenService.extractTokenFromBearer(bearerToken)).thenReturn(jwtToken);
        when(jwtTokenService.authenticate(jwtToken)).thenReturn(mockAuthentication);

        // When
        TestPublisher<Void> chainPublisher = TestPublisher.create();
        when(filterChain.filter(exchange)).thenReturn(
                ReactiveSecurityContextHolder.getContext()
                        .doOnNext(securityContext -> {
                            Authentication auth = securityContext.getAuthentication();

                            assertThat(auth).isNotNull();
                            assertThat(auth).isInstanceOf(JwtAuthenticationToken.class);
                            assertThat(auth.getName()).isEqualTo("123456789012345678");
                        })
                        .then(chainPublisher.mono())
        );

        Mono<Void> result = jwtAuthenticationWebFilter.filter(exchange, filterChain);

        // Then
        StepVerifier.create(result)
                .then(chainPublisher::emit)
                .verifyComplete();
    }

    @Test
    @DisplayName("빈 Authorization 헤더는 무시해야 한다")
    void shouldIgnoreEmptyAuthorizationHeader() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/protected")
                .header(HttpHeaders.AUTHORIZATION, "")
                .build();
        exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = jwtAuthenticationWebFilter.filter(exchange, filterChain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(jwtTokenService, never()).extractTokenFromBearer(anyString());
        verify(jwtTokenService, never()).authenticate(anyString());
        verify(filterChain).filter(exchange);
    }

    private JwtAuthenticationToken createMockAuthentication() {
        Jwt jwt = Jwt.withTokenValue("mock.jwt.token")
                .header("alg", "RS256")
                .header("typ", "JWT")
                .issuer("https://dad-marketplace.com")
                .subject("123456789012345678")
                .audience(List.of("dad-marketplace-client"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .claim("username", "testuser")
                .claim("email", "test@example.com")
                .claim("tokenType", "ACCESS")
                .claim("authorities", List.of("ROLE_USER"))
                .build();

        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
