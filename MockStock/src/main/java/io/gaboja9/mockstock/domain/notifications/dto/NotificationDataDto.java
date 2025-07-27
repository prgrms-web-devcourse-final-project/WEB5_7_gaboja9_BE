package io.gaboja9.mockstock.domain.notifications.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TradeNotificationDataDto.class, name = "TRADE"),
    @JsonSubTypes.Type(value = MarketNotificationDataDto.class, name = "MARKET")
})
public abstract class NotificationDataDto {}
