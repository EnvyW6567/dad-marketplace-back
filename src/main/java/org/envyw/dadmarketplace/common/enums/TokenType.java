package org.envyw.dadmarketplace.common.enums;

import lombok.Getter;

import static org.envyw.dadmarketplace.infrastructure.security.jwt.constants.JwtConstants.ACCESS_TOKEN_EXPIRATION;
import static org.envyw.dadmarketplace.infrastructure.security.jwt.constants.JwtConstants.REFRESH_TOKEN_EXPIRATION;

@Getter
public enum TokenType {
    ACCESS_TOKEN("accessToken", ACCESS_TOKEN_EXPIRATION),
    REFRESH_TOKEN("refreshToken", REFRESH_TOKEN_EXPIRATION);

    private final String tokenName;
    private final int expiration;

    TokenType(String tokenName, int expiration) {
        this.tokenName = tokenName;
        this.expiration = expiration;
    }
}
