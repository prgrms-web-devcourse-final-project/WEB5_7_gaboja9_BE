package io.gaboja9.mockstock.domain.mails.exception;

import io.gaboja9.mockstock.global.exception.BaseException;
import io.gaboja9.mockstock.global.exception.ErrorCode;

public class NotFoundMailException extends BaseException {
    public NotFoundMailException() {
        super(ErrorCode.NOT_FOUND_MAIL, "메일을 찾을 수 없습니다");
    }
}
