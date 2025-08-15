package org.envyw.dadmarketplace.infrastructure.security.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtProvider")
class JwtProviderTest {

    @Mock
    private JwtEncoder jwtEncoder;
    @InjectMocks
    private JwtProvider jwtProvider;

    private Jwt createMockJwt(String tokenValue) {
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "RS256")
                .claim("test", "value")
                .build();
    }

    @Nested
    @DisplayName("generateAccessToken 메서드")
    class GenerateAccessTokenMethod {

        @Test
        @DisplayName("Access Token 생성하고 토큰 값 반환")
        void generatesAccessTokenAndReturnsTokenValue() {
            // Given
            Long userId = 123L;
            Jwt mockJwt = createMockJwt("access.token.value");
            when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

            // When
            String result = jwtProvider.generateAccessToken(userId);

            // Then
            assertThat(result).isEqualTo("access.token.value");
        }

        @Test
        @DisplayName("올바른 Access Token Claims로 JWT 생성")
        void generatesJwtWithCorrectAccessTokenClaims() {
            // Given
            Long userId = 456L;
            Jwt mockJwt = createMockJwt("token");
            when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

            ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);

            // When
            jwtProvider.generateAccessToken(userId);

            // Then
            verify(jwtEncoder).encode(captor.capture());
            JwtClaimsSet claims = captor.getValue().getClaims();

            assertThat(claims.<String>getClaim("iss")).isEqualTo("dad-marketplace");
            assertThat(claims.getSubject()).isEqualTo("456");
            assertThat(claims.getAudience()).containsExactly("dad-marketplace-client");
            assertThat(claims.<String>getClaim("tokenType")).isNotNull().isEqualTo("ACCESS");
            assertThat(claims.<List<String>>getClaim("authorities")).isEqualTo(List.of("ROLE_USER"));
            assertThat(claims.getIssuedAt()).isBeforeOrEqualTo(Instant.now());
            assertThat(claims.getExpiresAt()).isAfter(Instant.now().plus(7199, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("generateRefreshToken 메서드")
    class GenerateRefreshTokenMethod {

        @Test
        @DisplayName("Refresh Token 생성하고 토큰 값 반환")
        void generatesRefreshTokenAndReturnsTokenValue() {
            // Given
            Long userId = 789L;
            Jwt mockJwt = createMockJwt("refresh.token.value");
            when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

            // When
            String result = jwtProvider.generateRefreshToken(userId);

            // Then
            assertThat(result).isEqualTo("refresh.token.value");
        }

        @Test
        @DisplayName("올바른 Refresh Token Claims로 JWT 생성")
        void generatesJwtWithCorrectRefreshTokenClaims() {
            // Given
            Long userId = 101L;
            Jwt mockJwt = createMockJwt("token");
            when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

            ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);

            // When
            jwtProvider.generateRefreshToken(userId);

            // Then
            verify(jwtEncoder).encode(captor.capture());
            JwtClaimsSet claims = captor.getValue().getClaims();

            assertThat(claims.<String>getClaim("iss")).isEqualTo("dad-marketplace");
            assertThat(claims.getSubject()).isEqualTo("101");
            assertThat(claims.getAudience()).containsExactly("dad-marketplace-client");
            assertThat(claims.<String>getClaim("tokenType")).isEqualTo("REFRESH");
            assertThat(claims.<List<String>>getClaim("authorities")).isEqualTo(List.of("ROLE_USER"));
            assertThat(claims.getIssuedAt()).isBeforeOrEqualTo(Instant.now());
            assertThat(claims.getExpiresAt()).isAfter(Instant.now().plus(604799, ChronoUnit.SECONDS));
        }
    }
}
