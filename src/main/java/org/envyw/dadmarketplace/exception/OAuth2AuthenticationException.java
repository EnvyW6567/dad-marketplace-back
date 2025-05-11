package org.envyw.dadmarketplace.exception;

import org.envyw.dadmarketplace.exception.errorCode.AuthErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class OAuth2AuthenticationException extends AuthenticationException {
    private final String errorCode;

    public OAuth2AuthenticationException(AuthErrorCode authErrorCode, Throwable cause) {
        super(authErrorCode.formatMessage(), cause);
        this.errorCode = authErrorCode.getCode();
    }

    public OAuth2AuthenticationException(AuthErrorCode authErrorCode) {
        super(authErrorCode.formatMessage());
        this.errorCode = authErrorCode.getCode();
    }

    public OAuth2AuthenticationException(String msg, String errorCode) {
        super(msg);
        this.errorCode = errorCode;
    }
}
