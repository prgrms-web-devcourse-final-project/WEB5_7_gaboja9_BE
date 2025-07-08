package io.gaboja9.mockstock.domain.trades.dto.response;

import io.gaboja9.mockstock.domain.trades.entity.TradeType;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class TradesResponseDto {
    private TradeType tradeType;
    private String stockCode;
    private String stockName;
    private int price;
    private int quantity;
    private int totalAmount;
    private LocalDateTime tradeDate;
}
