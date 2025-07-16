package io.gaboja9.mockstock.domain.auth.exception;

import io.gaboja9.mockstock.global.exception.BaseException;
import io.gaboja9.mockstock.global.exception.ErrorCode;

public class AuthException extends BaseException {

    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AuthException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static AuthException invalidCredentials() {
        return new AuthException(ErrorCode.INVALID_CREDENTIALS);
    }

    public static AuthException emailAlreadyExists() {
        return new AuthException(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    public static AuthException passwordMismatch() {
        return new AuthException(ErrorCode.PASSWORD_MISMATCH);
    }

    public static AuthException invalidVerificationCode() {
        return new AuthException(ErrorCode.INVALID_VERIFICATION_CODE);
    }
}
