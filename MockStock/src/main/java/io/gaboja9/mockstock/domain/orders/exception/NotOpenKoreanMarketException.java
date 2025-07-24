package io.gaboja9.mockstock.domain.orders.exception;

import io.gaboja9.mockstock.global.exception.BaseException;
import io.gaboja9.mockstock.global.exception.ErrorCode;

public class NotOpenKoreanMarketException extends BaseException {
    public NotOpenKoreanMarketException() {
        super(ErrorCode.NOT_OPEN_KOREAN_MARKET, "한국 시장이 닫혀서 거래할 수 없습니다.");
    }
}
