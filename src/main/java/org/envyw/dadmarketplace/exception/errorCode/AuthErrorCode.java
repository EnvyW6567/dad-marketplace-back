package org.envyw.dadmarketplace.exception.errorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.text.MessageFormat;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode {
    OAUTH_URL_GENERATION_FAILED("auth.oauth.url.generation.failed", "OAuth 인증 URL을 생성하는데 실패했습니다"),
    CLIENT_REGISTRATION_NOT_FOUND("auth.client.registration.not.found", "클라이언트 등록 ID를 찾을 수 없습니다");

    private final String code;
    private final String message;

    public String formatMessage(Object... args) {
        return MessageFormat.format(message, args);
    }
}
