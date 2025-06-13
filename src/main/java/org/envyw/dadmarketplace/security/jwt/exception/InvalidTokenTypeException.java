package org.envyw.dadmarketplace.security.jwt.exception;

public class InvalidTokenTypeException extends RuntimeException {

    public InvalidTokenTypeException(String message) {
        super(message);
    }
}
