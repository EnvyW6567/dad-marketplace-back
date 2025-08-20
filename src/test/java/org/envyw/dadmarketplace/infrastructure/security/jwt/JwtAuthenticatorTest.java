package org.envyw.dadmarketplace.infrastructure.security.jwt;

import org.envyw.dadmarketplace.infrastructure.security.jwt.exception.JwtAuthenticationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticator")
class JwtAuthenticatorTest {

    @Mock
    private JwtDecoder jwtDecoder;
    @InjectMocks
    private JwtAuthenticator jwtAuthenticator;

    private Jwt createJwtWithAuthorities(List<String> authorities) {
        var builder = Jwt.withTokenValue("test.token")
                .header("alg", "RS256")
                .subject("user123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS));

        if (authorities != null) {
            builder.claim("authorities", authorities);
        }

        return builder.build();
    }

    @Nested
    @DisplayName("authenticate 메서드")
    class AuthenticateMethod {

        @Test
        @DisplayName("유효한 토큰으로 JwtAuthenticationToken 반환")
        void returnsJwtAuthenticationTokenWithValidToken() {
            // Given
            String tokenValue = "valid.jwt.token";
            Jwt jwt = createJwtWithAuthorities(List.of("ROLE_USER", "ROLE_ADMIN"));
            when(jwtDecoder.decode(tokenValue)).thenReturn(jwt);

            // When
            JwtAuthenticationToken result = jwtAuthenticator.authenticate(tokenValue);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getToken()).isEqualTo(jwt);
            assertThat(result.getAuthorities()).hasSize(2);
            assertThat(result.getAuthorities())
                    .containsExactlyInAnyOrder(
                            new SimpleGrantedAuthority("ROLE_USER"),
                            new SimpleGrantedAuthority("ROLE_ADMIN")
                    );
        }

        @Test
        @DisplayName("권한이 없는 토큰으로 빈 권한 목록 반환")
        void returnsEmptyAuthoritiesWhenNoAuthorities() {
            // Given
            String tokenValue = "token.without.authorities";
            Jwt jwt = createJwtWithAuthorities(List.of());
            when(jwtDecoder.decode(tokenValue)).thenReturn(jwt);

            // When
            JwtAuthenticationToken result = jwtAuthenticator.authenticate(tokenValue);

            // Then
            assertThat(result.getAuthorities()).isEmpty();
        }

        @Test
        @DisplayName("단일 권한을 가진 토큰으로 올바른 권한 반환")
        void returnsSingleAuthorityWhenTokenHasOneAuthority() {
            // Given
            String tokenValue = "single.authority.token";
            Jwt jwt = createJwtWithAuthorities(List.of("ROLE_USER"));
            when(jwtDecoder.decode(tokenValue)).thenReturn(jwt);

            // When
            JwtAuthenticationToken result = jwtAuthenticator.authenticate(tokenValue);

            // Then
            assertThat(result.getAuthorities()).hasSize(1);
            assertThat(result.getAuthorities())
                    .containsExactly(new SimpleGrantedAuthority("ROLE_USER"));
        }

        @Test
        @DisplayName("JwtException 발생 시 JwtAuthenticationException 던짐")
        void throwsExceptionWhenJwtDecodingFails() {
            // Given
            String tokenValue = "invalid.token";
            JwtException jwtException = new JwtException("Invalid JWT");
            when(jwtDecoder.decode(tokenValue)).thenThrow(jwtException);

            // When & Then
            assertThatThrownBy(() -> jwtAuthenticator.authenticate(tokenValue))
                    .isInstanceOf(JwtAuthenticationException.class)
                    .hasMessage("JWT 토큰 인증에 실패했습니다")
                    .hasCause(jwtException);
        }

        @Test
        @DisplayName("만료된 토큰일 때 예외 던짐")
        void throwsExceptionWhenTokenExpired() {
            // Given
            String tokenValue = "expired.token";
            JwtException expiredException = new JwtException("JWT is expired");
            when(jwtDecoder.decode(tokenValue)).thenThrow(expiredException);

            // When & Then
            assertThatThrownBy(() -> jwtAuthenticator.authenticate(tokenValue))
                    .isInstanceOf(JwtAuthenticationException.class)
                    .hasMessage("JWT 토큰 인증에 실패했습니다")
                    .hasCause(expiredException);
        }

        @Test
        @DisplayName("잘못된 형식 토큰일 때 예외 던짐")
        void throwsExceptionWhenTokenMalformed() {
            // Given
            String tokenValue = "malformed.token";
            JwtException malformedException = new JwtException("Malformed JWT");
            when(jwtDecoder.decode(tokenValue)).thenThrow(malformedException);

            // When & Then
            assertThatThrownBy(() -> jwtAuthenticator.authenticate(tokenValue))
                    .isInstanceOf(JwtAuthenticationException.class)
                    .hasMessage("JWT 토큰 인증에 실패했습니다")
                    .hasCause(malformedException);
        }
    }
}
