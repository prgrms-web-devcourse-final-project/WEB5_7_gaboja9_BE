package io.gaboja9.mockstock.domain.auth.exception;

import io.gaboja9.mockstock.global.exception.BaseException;
import io.gaboja9.mockstock.global.exception.ErrorCode;

import java.text.MessageFormat;

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

    public static AuthException cannotResetPasswordForSocialUser() {
        return new AuthException(ErrorCode.SOCIAL_CANNOT_PASSWORD_RESET);
    }

    public static AuthException weakPassword() {
        return new AuthException(ErrorCode.WEAK_PASSWORD);
    }

    public static AuthException sameAsCurrentPassword() {
            return new AuthException(ErrorCode.SAME_AS_CURRENT_PASSWORD);
    }

    public static AuthException invalidCurrentPassword() {
        return new AuthException(ErrorCode.CURRENT_PASSWORD_INCORRECT);
    }

    public static AuthException newPasswordMismatch() {
        return new AuthException(ErrorCode.NEW_PASSWORD_CONFIRM_MISMATCH);
    }

    public static AuthException authResendTooEarly(long seconds) {
        String message =
                MessageFormat.format(ErrorCode.AUTH_RESEND_TOO_EARLY.getMessage(), seconds);
        return new AuthException(ErrorCode.AUTH_RESEND_TOO_EARLY, message);
    }

    public static AuthException socialLoginRequired(String provider) {
        String message =
                MessageFormat.format(ErrorCode.SOCIAL_LOGIN_REQUIRED.getMessage(), provider);
        return new AuthException(ErrorCode.SOCIAL_LOGIN_REQUIRED, message);
    }
}
