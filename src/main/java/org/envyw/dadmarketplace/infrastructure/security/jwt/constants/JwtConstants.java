package org.envyw.dadmarketplace.infrastructure.security.jwt.constants;

public class JwtConstants {
    public static final String ISSUER = "dad-marketplace";
    public static final String AUDIENCE = "dad-marketplace-client";
    public static final String AUTHORITIES_CLAIM = "authorities";

    public static final String ACCESS_TOKEN_TYPE = "ACCESS";
    public static final String REFRESH_TOKEN_TYPE = "REFRESH";
    public static final String TOKEN_TYPE_CLAIM = "tokenType";

    public static final int ACCESS_TOKEN_EXPIRATION = 7200;
    public static final int REFRESH_TOKEN_EXPIRATION = 604800;
}
