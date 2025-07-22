package io.gaboja9.mockstock.domain.orders.exception;

import io.gaboja9.mockstock.global.exception.BaseException;
import io.gaboja9.mockstock.global.exception.ErrorCode;

public class NotFoundOrderException extends BaseException {
    public NotFoundOrderException() {
        super(ErrorCode.NOT_FOUND_ORDER,"주문을 찾을 수 없습니다.");
    }
}
