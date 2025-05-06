package org.envyw.dadmarketplace.exception;

import org.springframework.security.core.AuthenticationException;

public class OAuth2AuthenticationException extends AuthenticationException {
    public OAuth2AuthenticationException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public OAuth2AuthenticationException(String msg) {
        super(msg);
    }
}
