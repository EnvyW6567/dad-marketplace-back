package org.envyw.dadmarketplace.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.envyw.dadmarketplace.entity.User;
import org.envyw.dadmarketplace.security.jwt.exception.InvalidTokenTypeException;
import org.envyw.dadmarketplace.security.jwt.exception.JwtAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    private static final String ISSUER = "dad-marketplace";
    private static final String AUDIENCE = "dad-marketplace-client";

    private static final long ACCESS_TOKEN_EXPIRATION_HOURS = 1;
    private static final long REFRESH_TOKEN_EXPIRATION_DAYS = 7;

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";
    private static final String AUTHORITIES_CLAIM = "authorities";
    private static final String BEARER_PREFIX = "Bearer ";

    public String generateAccessToken(User user) {
        validateUser(user);

        JwtClaimsSet claims = buildAccessTokenClaims(user);
        JwtEncoderParameters parameters = JwtEncoderParameters.from(claims);

        Jwt jwt = jwtEncoder.encode(parameters);

        return jwt.getTokenValue();
    }

    public String generateRefreshToken(User user) {
        validateUser(user);

        JwtClaimsSet claims = buildRefreshTokenClaims(user);
        JwtEncoderParameters parameters = JwtEncoderParameters.from(claims);

        Jwt jwt = jwtEncoder.encode(parameters);

        return jwt.getTokenValue();
    }

    public Authentication authenticate(String tokenValue) {
        try {
            Jwt jwt = jwtDecoder.decode(tokenValue);

            // 권한 정보 추출
            List<String> authorities = jwt.getClaimAsStringList(AUTHORITIES_CLAIM);
            List<SimpleGrantedAuthority> grantedAuthorities = authorities.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                    jwt,
                    grantedAuthorities
            );

            log.info("JWT 토큰 인증 성공: subject={}", jwt.getSubject());

            return authentication;

        } catch (JwtException e) {
            log.error("JWT 토큰 인증 실패: {}", e.getMessage());
            throw new JwtAuthenticationException("JWT 토큰 인증에 실패했습니다", e);
        }
    }

    public JwtClaimsSet buildAccessTokenClaims(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(ACCESS_TOKEN_EXPIRATION_HOURS, ChronoUnit.HOURS);

        return JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.getDiscordId())
                .audience(List.of(AUDIENCE))
                .issuedAt(now)
                .expiresAt(expiration)
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("displayName", user.getDisplayName())
                .claim("avatarUrl", user.getAvatarUrl())
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .claim(AUTHORITIES_CLAIM, List.of("ROLE_USER"))
                .build();
    }

    public JwtClaimsSet buildRefreshTokenClaims(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(REFRESH_TOKEN_EXPIRATION_DAYS, ChronoUnit.DAYS);

        return JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.getDiscordId())
                .audience(List.of(AUDIENCE))
                .issuedAt(now)
                .expiresAt(expiration)
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .build();
    }

    public String extractDiscordId(Jwt jwt) {
        return jwt.getSubject();
    }

    public String extractUsername(Jwt jwt) {
        return jwt.getClaimAsString("username");
    }

    public String extractEmail(Jwt jwt) {
        return jwt.getClaimAsString("email");
    }

    public String extractDisplayName(Jwt jwt) {
        return jwt.getClaimAsString("displayName");
    }

    public boolean isAccessToken(Jwt jwt) {
        String tokenType = jwt.getClaimAsString(TOKEN_TYPE_CLAIM);
        return ACCESS_TOKEN_TYPE.equals(tokenType);
    }

    public boolean isRefreshToken(Jwt jwt) {
        String tokenType = jwt.getClaimAsString(TOKEN_TYPE_CLAIM);
        return REFRESH_TOKEN_TYPE.equals(tokenType);
    }

    public String renewAccessToken(Jwt refreshJwt, User user) {
        if (!isRefreshToken(refreshJwt)) {
            throw new InvalidTokenTypeException("Refresh Token만 사용하여 토큰을 갱신할 수 있습니다");
        }

        return generateAccessToken(user);
    }

    public String extractTokenFromBearer(String bearerToken) {
        if (!StringUtils.hasText(bearerToken)) {
            throw new IllegalArgumentException("토큰이 비어있습니다");
        }

        if (bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return bearerToken;
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("사용자 정보는 null일 수 없습니다");
        }
    }
}
