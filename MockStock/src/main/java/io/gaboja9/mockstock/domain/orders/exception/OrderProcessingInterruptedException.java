package io.gaboja9.mockstock.domain.orders.exception;

import io.gaboja9.mockstock.global.exception.BaseException;
import io.gaboja9.mockstock.global.exception.ErrorCode;

public class OrderProcessingInterruptedException extends BaseException {
    public OrderProcessingInterruptedException() {
        super(ErrorCode.ORDER_PROCESSING_INTERRUPTED, "주문 처리 중 인터럽트가 발생했습니다.");
    }
}
