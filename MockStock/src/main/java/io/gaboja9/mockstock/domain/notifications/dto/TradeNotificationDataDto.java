package io.gaboja9.mockstock.domain.notifications.dto;

import io.gaboja9.mockstock.domain.trades.entity.TradeType;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class TradeNotificationDataDto extends NotificationDataDto {
    private String stockCode;
    private String stockName;
    private TradeType tradeType;
    private int quantity;
    private int price;
    private int totalAmount;
}
