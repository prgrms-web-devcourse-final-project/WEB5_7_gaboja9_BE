package io.gaboja9.mockstock.domain.orders.exception;

import io.gaboja9.mockstock.global.exception.BaseException;
import io.gaboja9.mockstock.global.exception.ErrorCode;

public class OrderProcessingTimeoutException extends BaseException {
    public OrderProcessingTimeoutException() {
        super(ErrorCode.ORDER_PROCESSING_TIMEOUT, "주문 처리 대기 시간이 초과되었습니다.");
    }
}
