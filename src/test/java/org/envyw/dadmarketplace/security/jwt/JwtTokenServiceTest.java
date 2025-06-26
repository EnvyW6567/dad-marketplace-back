package org.envyw.dadmarketplace.security.jwt;

import org.envyw.dadmarketplace.entity.User;
import org.envyw.dadmarketplace.security.jwt.exception.InvalidTokenTypeException;
import org.envyw.dadmarketplace.security.jwt.exception.JwtAuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT 토큰 서비스 테스트")
class JwtTokenServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private JwtDecoder jwtDecoder;

    @InjectMocks
    private JwtTokenService jwtTokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 데이터 준비
        testUser = User.builder()
                .id(1L)
                .discordId("123456789012345678")
                .username("testuser")
                .displayName("Test User")
                .email("test@example.com")
                .avatarUrl("https://avatar.com/test.png")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("사용자 정보로 JWT Access Token을 생성할 수 있어야 한다")
    void shouldGenerateAccessTokenFromUser() {
        // Given
        Jwt mockJwt = createMockJwt("ACCESS");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        // When
        String token = jwtTokenService.generateAccessToken(testUser);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isEqualTo("mock.jwt.token");

        verify(jwtEncoder).encode(argThat(params -> {
            JwtClaimsSet claims = params.getClaims();
            return "123456789012345678".equals(claims.getSubject()) &&
                    "ACCESS".equals(claims.getClaim("tokenType")) &&
                    "testuser".equals(claims.getClaim("username"));
        }));
    }

    @Test
    @DisplayName("사용자 정보로 JWT Refresh Token을 생성할 수 있어야 한다")
    void shouldGenerateRefreshTokenFromUser() {
        // Given
        Jwt mockJwt = createMockJwt("REFRESH");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        // When
        String token = jwtTokenService.generateRefreshToken(testUser);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isEqualTo("mock.jwt.token");

        // Refresh Token은 최소한의 정보만 포함하는지 검증
        verify(jwtEncoder).encode(argThat(params -> {
            JwtClaimsSet claims = params.getClaims();
            return "123456789012345678".equals(claims.getSubject()) &&
                    "REFRESH".equals(claims.getClaim("tokenType")) &&
                    claims.getClaim("email") == null && // 민감한 정보 제외
                    claims.getClaim("username") == null;
        }));
    }

    @Test
    @DisplayName("null 사용자로 토큰 생성 시 예외가 발생해야 한다")
    void shouldThrowExceptionWhenUserIsNull() {
        // When & Then
        assertThatThrownBy(() -> jwtTokenService.generateAccessToken(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 정보는 null일 수 없습니다");

        assertThatThrownBy(() -> jwtTokenService.generateRefreshToken(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 정보는 null일 수 없습니다");

        // JwtEncoder가 호출되지 않았는지 확인
        verify(jwtEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("JWT 토큰을 파싱하여 Authentication 객체를 생성할 수 있어야 한다")
    void shouldCreateAuthenticationFromJwtToken() {
        // Given
        String tokenValue = "valid.jwt.token";
        Jwt jwt = createMockJwt("ACCESS");
        when(jwtDecoder.decode(tokenValue)).thenReturn(jwt);

        // When
        Authentication authentication = jwtTokenService.authenticate(tokenValue);

        // Then
        assertThat(authentication).isInstanceOf(JwtAuthenticationToken.class);
        assertThat(authentication.getName()).isEqualTo("123456789012345678");
        assertThat(authentication.getPrincipal()).isEqualTo(jwt);

        // JwtDecoder가 올바른 토큰으로 호출되었는지 검증
        verify(jwtDecoder).decode(tokenValue);
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 인증 시 예외가 발생해야 한다")
    void shouldThrowExceptionForInvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";
        when(jwtDecoder.decode(invalidToken))
                .thenThrow(new JwtException("Invalid JWT token"));

        // When & Then
        assertThatThrownBy(() -> jwtTokenService.authenticate(invalidToken))
                .isInstanceOf(JwtAuthenticationException.class)
                .hasMessage("JWT 토큰 인증에 실패했습니다")
                .hasCauseInstanceOf(JwtException.class);

        verify(jwtDecoder).decode(invalidToken);
    }

    @Test
    @DisplayName("만료된 토큰으로 인증 시 예외가 발생해야 한다")
    void shouldThrowExceptionForExpiredToken() {
        // Given
        String expiredToken = "expired.jwt.token";
        when(jwtDecoder.decode(expiredToken))
                .thenThrow(new JwtException("JWT token is expired"));

        // When & Then
        assertThatThrownBy(() -> jwtTokenService.authenticate(expiredToken))
                .isInstanceOf(JwtAuthenticationException.class)
                .hasMessage("JWT 토큰 인증에 실패했습니다");

        verify(jwtDecoder).decode(expiredToken);
    }

    @Test
    @DisplayName("토큰에서 사용자 정보를 추출할 수 있어야 한다")
    void shouldExtractUserInfoFromJwt() {
        // Given
        Jwt jwt = createMockJwt("ACCESS");

        // When
        String discordId = jwtTokenService.extractDiscordId(jwt);
        String username = jwtTokenService.extractUsername(jwt);
        String displayName = jwtTokenService.extractDisplayName(jwt);

        // Then
        assertThat(discordId).isEqualTo("123456789012345678");
        assertThat(username).isEqualTo("testuser");
        assertThat(displayName).isEqualTo("Test User");
    }

    @Test
    @DisplayName("토큰 타입을 검증할 수 있어야 한다")
    void shouldValidateTokenType() {
        // Given
        Jwt accessJwt = createMockJwt("ACCESS");
        Jwt refreshJwt = createMockJwt("REFRESH");

        // When & Then
        assertThat(jwtTokenService.isAccessToken(accessJwt)).isTrue();
        assertThat(jwtTokenService.isRefreshToken(accessJwt)).isFalse();

        assertThat(jwtTokenService.isAccessToken(refreshJwt)).isFalse();
        assertThat(jwtTokenService.isRefreshToken(refreshJwt)).isTrue();
    }

    @Test
    @DisplayName("Refresh Token으로 새로운 Access Token을 생성할 수 있어야 한다")
    void shouldRenewAccessTokenWithRefreshToken() {
        // Given
        Jwt refreshJwt = createMockJwt("REFRESH");
        Jwt newAccessJwt = createMockJwt("ACCESS");

        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(newAccessJwt);

        // When
        String newAccessToken = jwtTokenService.renewAccessToken(refreshJwt, testUser);

        // Then
        assertThat(newAccessToken).isNotNull();
        assertThat(newAccessToken).isEqualTo("mock.jwt.token");

        // 새로운 Access Token 생성을 위해 JwtEncoder가 호출되었는지 검증
        verify(jwtEncoder).encode(argThat(params -> {
            JwtClaimsSet claims = params.getClaims();
            return "ACCESS".equals(claims.getClaim("tokenType"));
        }));
    }

    @Test
    @DisplayName("Access Token으로는 토큰 갱신이 불가능해야 한다")
    void shouldNotRenewWithAccessToken() {
        // Given
        Jwt accessJwt = createMockJwt("ACCESS");

        // When & Then
        assertThatThrownBy(() -> jwtTokenService.renewAccessToken(accessJwt, testUser))
                .isInstanceOf(InvalidTokenTypeException.class)
                .hasMessage("Refresh Token만 사용하여 토큰을 갱신할 수 있습니다");

        // 잘못된 토큰 타입으로는 JwtEncoder가 호출되지 않아야 함
        verify(jwtEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("Bearer 토큰에서 실제 토큰 값을 추출할 수 있어야 한다")
    void shouldExtractTokenFromBearerString() {
        // Given
        String bearerToken = "Bearer valid.jwt.token";

        // When
        String extractedToken = jwtTokenService.extractTokenFromBearer(bearerToken);

        // Then
        assertThat(extractedToken).isEqualTo("valid.jwt.token");
    }

    @Test
    @DisplayName("Bearer 접두사가 없는 토큰도 처리할 수 있어야 한다")
    void shouldHandleTokenWithoutBearerPrefix() {
        // Given
        String tokenWithoutBearer = "valid.jwt.token";

        // When
        String extractedToken = jwtTokenService.extractTokenFromBearer(tokenWithoutBearer);

        // Then
        assertThat(extractedToken).isEqualTo("valid.jwt.token");
    }

    @Test
    @DisplayName("빈 문자열이나 null 토큰은 예외를 발생시켜야 한다")
    void shouldThrowExceptionForEmptyOrNullToken() {
        // When & Then
        assertThatThrownBy(() -> jwtTokenService.extractTokenFromBearer(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("토큰이 비어있습니다");

        assertThatThrownBy(() -> jwtTokenService.extractTokenFromBearer(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("토큰이 비어있습니다");

        assertThatThrownBy(() -> jwtTokenService.extractTokenFromBearer("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("토큰이 비어있습니다");
    }

    @Test
    @DisplayName("JwtEncoder 호출 시 올바른 Claims가 전달되는지 검증")
    void shouldPassCorrectClaimsToJwtEncoder() {
        // Given
        Jwt mockJwt = createMockJwt("ACCESS");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        // When
        jwtTokenService.generateAccessToken(testUser);

        // Then - ArgumentCaptor를 사용하여 전달된 파라미터 상세 검증
        verify(jwtEncoder).encode(argThat(params -> {
            JwtClaimsSet claims = params.getClaims();

            boolean basicClaimsValid = "dad-marketplace".equals(claims.getClaim("iss")) &&
                    "123456789012345678".equals(claims.getSubject()) &&
                    claims.getAudience().contains("dad-marketplace-client");

            boolean userClaimsValid = "testuser".equals(claims.getClaim("username")) &&
                    "test@example.com".equals(claims.getClaim("email")) &&
                    "Test User".equals(claims.getClaim("displayName"));

            boolean metaClaimsValid = "ACCESS".equals(claims.getClaim("tokenType")) &&
                    claims.getIssuedAt() != null &&
                    claims.getExpiresAt() != null;

            return basicClaimsValid && userClaimsValid && metaClaimsValid;
        }));
    }

    @Test
    @DisplayName("JwtDecoder 예외 발생 시 적절히 처리되어야 한다")
    void shouldHandleJwtDecoderExceptions() {
        // Given
        String malformedToken = "malformed.token";
        when(jwtDecoder.decode(malformedToken))
                .thenThrow(new JwtException("Malformed JWT token"));

        // When & Then
        assertThatThrownBy(() -> jwtTokenService.authenticate(malformedToken))
                .isInstanceOf(JwtAuthenticationException.class)
                .hasMessage("JWT 토큰 인증에 실패했습니다")
                .hasCauseInstanceOf(JwtException.class);

        verify(jwtDecoder).decode(malformedToken);
    }

    private Jwt createMockJwt(String tokenType) {
        return Jwt.withTokenValue("mock.jwt.token")
                .header("alg", "RS256")
                .header("typ", "JWT")
                .issuer("dad-marketplace")
                .subject("123456789012345678")
                .audience(List.of("dad-marketplace-client"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .claim("username", "testuser")
                .claim("email", "test@example.com")
                .claim("displayName", "Test User")
                .claim("avatarUrl", "https://avatar.com/test.png")
                .claim("tokenType", tokenType)
                .claim("authorities", List.of("ROLE_USER"))
                .build();
    }
}
