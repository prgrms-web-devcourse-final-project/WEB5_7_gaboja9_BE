package io.gaboja9.mockstock.domain.notifications.exception;

import io.gaboja9.mockstock.global.exception.BaseException;
import io.gaboja9.mockstock.global.exception.ErrorCode;

public class NotificationException extends BaseException {

    public NotificationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public NotificationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static NotificationException updateFailed() {
        return new NotificationException(ErrorCode.NOTIFICATION_SETTING_UPDATE_FAILED);
    }

    public static NotificationException updateFailed(String message) {
        return new NotificationException(ErrorCode.NOTIFICATION_SETTING_UPDATE_FAILED, message);
    }
}
