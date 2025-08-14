package org.envyw.dadmarketplace.infrastructure.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public String generateAccessToken(Long userId) {
        JwtClaimsSet claims = buildJwtClaims(userId, ACCESS_TOKEN_TYPE, ACCESS_TOKEN_EXPIRATION);
        JwtEncoderParameters parameters = JwtEncoderParameters.from(claims);

        Jwt jwt = jwtEncoder.encode(parameters);

        return jwt.getTokenValue();
    }

    public String generateRefreshToken(Long userId) {
        JwtClaimsSet claims = buildJwtClaims(userId, REFRESH_TOKEN_TYPE, REFRESH_TOKEN_EXPIRATION);
        JwtEncoderParameters parameters = JwtEncoderParameters.from(claims);

        Jwt jwt = jwtEncoder.encode(parameters);

        return jwt.getTokenValue();
    }

    private JwtClaimsSet buildJwtClaims(Long userId, String tokenType, int expirationValue) {
        Instant now = Instant.now();
        Instant expiration = now.plus(expirationValue, ChronoUnit.SECONDS);
        log.info("now={}, expiration={}, EXPIRATION_VALUE={}", now, expiration, tokenType);

        return JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(userId.toString())
                .audience(List.of(AUDIENCE))
                .issuedAt(now)
                .expiresAt(expiration)
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .claim(AUTHORITIES_CLAIM, List.of("ROLE_USER"))
                .build();
    }
}
