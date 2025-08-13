package org.envyw.dadmarketplace.infrastructure.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.envyw.dadmarketplace.application.port.out.JwtOutPort;
import org.envyw.dadmarketplace.infrastructure.security.jwt.exception.JwtAuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import static org.envyw.dadmarketplace.infrastructure.security.jwt.constants.JwtConstants.REFRESH_TOKEN_TYPE;
import static org.envyw.dadmarketplace.infrastructure.security.jwt.constants.JwtConstants.TOKEN_TYPE_CLAIM;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAdapter implements JwtOutPort {
    private final JwtProvider jwtProvider;
    private final JwtAuthenticator jwtAuthenticator;
    
    public String refresh(String refreshToken) {
        JwtAuthenticationToken authentication = jwtAuthenticator.authenticate(refreshToken);
        Jwt jwt = authentication.getToken();

        validateRefreshToken(jwt);
        Long userId = Long.valueOf(jwt.getSubject());

        return jwtProvider.generateAccessToken(userId);
    }


    private void validateRefreshToken(Jwt jwt) {
        String tokenType = jwt.getClaimAsString(TOKEN_TYPE_CLAIM);

        if (!REFRESH_TOKEN_TYPE.equals(tokenType)) {
            throw new JwtAuthenticationException("Only allow refresh token type");
        }
    }
}
