package org.envyw.dadmarketplace.infrastructure.security.jwt;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import static org.envyw.dadmarketplace.infrastructure.security.jwt.constants.JwtConstants.BEARER_PREFIX;

@Component
public class JwtUtil {
    public String extractTokenFromBearer(String bearerToken) {
        if (!StringUtils.hasText(bearerToken)) {
            throw new IllegalArgumentException("토큰이 비어있습니다");
        }

        if (bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return bearerToken;
    }
}
