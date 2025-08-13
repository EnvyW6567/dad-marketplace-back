package org.envyw.dadmarketplace.infrastructure.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.envyw.dadmarketplace.infrastructure.security.jwt.constants.JwtConstants.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtProvider {

    private final JwtEncoder jwtEncoder;

    @Value("${app.jwt.access-token-expiration:7200}")
    private long ACCESS_TOKEN_EXPIRATION;
    @Value("${app.jwt.refresh-token-expiration:604800}")
    private long REFRESH_TOKEN_EXPIRATION;

    public String generateAccessToken(Long userId) {
        JwtClaimsSet claims = buildAccessTokenClaims(userId);
        JwtEncoderParameters parameters = JwtEncoderParameters.from(claims);

        Jwt jwt = jwtEncoder.encode(parameters);

        return jwt.getTokenValue();
    }

    public String generateRefreshToken(Long userId) {
        JwtClaimsSet claims = buildRefreshTokenClaims(userId);
        JwtEncoderParameters parameters = JwtEncoderParameters.from(claims);

        Jwt jwt = jwtEncoder.encode(parameters);

        return jwt.getTokenValue();
    }

    private JwtClaimsSet buildAccessTokenClaims(Long userId) {
        Instant now = Instant.now();
        Instant expiration = now.plus(ACCESS_TOKEN_EXPIRATION, ChronoUnit.SECONDS);
        log.info("now={}, expiration={}, EXPIRATION_VALUE={}", now, expiration, ACCESS_TOKEN_EXPIRATION);

        return JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(userId.toString())
                .audience(List.of(AUDIENCE))
                .issuedAt(now)
                .expiresAt(expiration)
                .claim("userId", userId)
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .claim(AUTHORITIES_CLAIM, List.of("ROLE_USER"))
                .build();
    }

    private JwtClaimsSet buildRefreshTokenClaims(Long userId) {
        Instant now = Instant.now();
        Instant expiration = now.plus(REFRESH_TOKEN_EXPIRATION, ChronoUnit.SECONDS);
        log.info("now={}, expiration={}, EXPIRATION_VALUE={}", now, expiration, REFRESH_TOKEN_EXPIRATION);

        return JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(userId.toString())
                .audience(List.of(AUDIENCE))
                .issuedAt(now)
                .expiresAt(expiration)
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .claim(AUTHORITIES_CLAIM, List.of("ROLE_USER"))
                .build();
    }
}
