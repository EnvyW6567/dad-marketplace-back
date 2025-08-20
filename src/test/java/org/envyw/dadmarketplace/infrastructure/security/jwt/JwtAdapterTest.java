package org.envyw.dadmarketplace.infrastructure.security.jwt;

import org.envyw.dadmarketplace.infrastructure.security.jwt.exception.JwtAuthenticationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAdapter")
class JwtAdapterTest {

    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private JwtAuthenticator jwtAuthenticator;
    @InjectMocks
    private JwtAdapter jwtAdapter;

    private Jwt createRefreshJwt(String userId) {
        return createJwtWithTokenType(userId, "REFRESH");
    }

    private Jwt createAccessJwt(String userId) {
        return createJwtWithTokenType(userId, "ACCESS");
    }

    private Jwt createJwtWithoutTokenType(String userId) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(userId)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
    }

    private Jwt createJwtWithTokenType(String userId, String tokenType) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(userId)
                .claim("tokenType", tokenType)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
    }

    @Nested
    @DisplayName("refresh 메서드")
    class RefreshMethod {

        @Test
        @DisplayName("유효한 Refresh Token으로 새 Access Token 반환")
        void returnsNewAccessTokenWithValidRefreshToken() {
            // Given
            String refreshToken = "valid.refresh.token";
            Jwt jwt = createRefreshJwt("123");
            JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of());

            when(jwtAuthenticator.authenticate(refreshToken)).thenReturn(auth);
            when(jwtProvider.generateAccessToken(123L)).thenReturn("new.access.token");

            // When
            String result = jwtAdapter.refresh(refreshToken);

            // Then
            assertThat(result).isEqualTo("new.access.token");
        }

        @Test
        @DisplayName("Access Token 타입일 때 예외 발생")
        void throwsExceptionWhenTokenTypeIsAccess() {
            // Given
            Jwt jwt = createAccessJwt("123");
            JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of());
            when(jwtAuthenticator.authenticate("token")).thenReturn(auth);

            // When & Then
            assertThatThrownBy(() -> jwtAdapter.refresh("token"))
                    .isInstanceOf(JwtAuthenticationException.class)
                    .hasMessage("Only allow refresh token type");
        }

        @Test
        @DisplayName("토큰 타입이 null일 때 예외 발생")
        void throwsExceptionWhenTokenTypeIsNull() {
            // Given
            Jwt jwt = createJwtWithoutTokenType("123");
            JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of());
            when(jwtAuthenticator.authenticate("token")).thenReturn(auth);

            // When & Then
            assertThatThrownBy(() -> jwtAdapter.refresh("token"))
                    .isInstanceOf(JwtAuthenticationException.class)
                    .hasMessage("Only allow refresh token type");
        }

        @Test
        @DisplayName("잘못된 토큰 타입일 때 예외 발생")
        void throwsExceptionWhenTokenTypeIsInvalid() {
            // Given
            Jwt jwt = createJwtWithTokenType("123", "INVALID");
            JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of());
            when(jwtAuthenticator.authenticate("token")).thenReturn(auth);

            // When & Then
            assertThatThrownBy(() -> jwtAdapter.refresh("token"))
                    .isInstanceOf(JwtAuthenticationException.class)
                    .hasMessage("Only allow refresh token type");
        }
    }
}
