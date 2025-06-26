package org.envyw.dadmarketplace.service;

import org.envyw.dadmarketplace.dto.request.RefreshReqDto;
import org.envyw.dadmarketplace.dto.response.RefreshResDto;
import org.envyw.dadmarketplace.entity.User;
import org.envyw.dadmarketplace.security.jwt.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
public class AuthServiceTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Jwt refreshJwt;
    private RefreshReqDto refreshReqDto;
    private String discordId;

    @BeforeEach
    void setUp() {
        discordId = "12345678";
        testUser = User.builder()
                .id(1L)
                .discordId(discordId)
                .username("testuser")
                .displayName("Test User")
                .email("test@example.com")
                .avatarUrl("https://avatar.com/test.png")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        refreshJwt = createMockRefreshJwtToken();
        refreshReqDto = new RefreshReqDto("valid.refresh.token");
    }

    @Test
    @DisplayName("유효하지 않은 Authentication 객체가 반환되면 예외가 발생해야 한다")
    void shouldThrowExceptionWhenInvalidAuthenticationToken() {
        // Given
        String refreshToken = "valid.refresh.token";
        RefreshReqDto refreshReq = new RefreshReqDto(refreshToken);

        // JwtAuthenticationToken이 아닌 다른 Authentication 객체(UsernamePasswordAuthenticationToken) 생성
        Authentication nonJwtAuthentication = new UsernamePasswordAuthenticationToken(
                "testuser", "password", List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        when(jwtTokenService.authenticate(refreshToken)).thenReturn(nonJwtAuthentication);

        // When
        Mono<RefreshResDto> result = authService.refresh(refreshReq);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                                throwable.getMessage().equals("유효하지 않은 인증 토큰입니다"))
                .verify();

        verify(jwtTokenService).authenticate(refreshToken);
    }

    @Test
    @DisplayName("유효한 토큰에 대해서 액세스 토큰을 발행해야 한다")
    void shouldGenerateAccessToken() {
        // Given
        String refreshToken = "valid.refresh.token";
        String newAccessToken = "new.access.token";

        RefreshReqDto refreshReq = new RefreshReqDto(refreshToken);

        when(jwtTokenService.authenticate(refreshToken)).thenReturn(new JwtAuthenticationToken(refreshJwt));
        when(jwtTokenService.extractDiscordId(refreshJwt)).thenReturn(discordId);
        when(userService.findByDiscordId(discordId)).thenReturn(Mono.just(testUser));
        when(jwtTokenService.renewAccessToken(refreshJwt, testUser)).thenReturn(newAccessToken);

        // When
        Mono<RefreshResDto> result = authService.refresh(refreshReq);

        //Then
        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.accessToken()).isEqualTo(newAccessToken);
                })
                .verifyComplete();

        verify(jwtTokenService).authenticate(refreshToken);
        verify(jwtTokenService).extractDiscordId(refreshJwt);
        verify(jwtTokenService).renewAccessToken(refreshJwt, testUser);
        verify(userService).findByDiscordId(discordId);
    }

    @Test
    @DisplayName("토큰 갱신 중 JwtTokenService에서 예외 발생 시 적절히 처리되어야 한다")
    void shouldHandleJwtTokenServiceException() {
        // Given
        JwtAuthenticationToken authentication = createMockAuthentication(refreshJwt);

        when(jwtTokenService.authenticate(refreshReqDto.refreshToken())).thenReturn(authentication);
        when(jwtTokenService.extractDiscordId(refreshJwt)).thenReturn(discordId);
        when(userService.findByDiscordId(discordId)).thenReturn(Mono.just(testUser));
        when(jwtTokenService.renewAccessToken(refreshJwt, testUser))
                .thenThrow(new RuntimeException("토큰 생성 실패"));

        // When
        Mono<RefreshResDto> result = authService.refresh(refreshReqDto);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("토큰 생성 실패"))
                .verify();

        verify(jwtTokenService).renewAccessToken(refreshJwt, testUser);
    }

    private Jwt createMockRefreshJwtToken() {
        return Jwt.withTokenValue("mock.jwt.token")
                .header("alg", "RS256")
                .header("typ", "JWT")
                .issuer("dad-marketplace")
                .subject("123456789012345678")
                .audience(List.of("dad-marketplace-client"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .claim("tokenType", "REFRESH")
                .claim("username", "testuser")
                .claim("email", "test@example.com")
                .claim("displayName", "Test User")
                .claim("authorities", List.of("ROLE_USER"))
                .build();
    }

    private JwtAuthenticationToken createMockAuthentication(Jwt jwt) {
        return new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

}
