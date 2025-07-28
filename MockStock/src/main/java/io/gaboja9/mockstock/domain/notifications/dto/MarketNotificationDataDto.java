package io.gaboja9.mockstock.domain.notifications.dto;

import io.gaboja9.mockstock.domain.notifications.enums.MarketStatus;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalTime;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class MarketNotificationDataDto extends NotificationDataDto {
    private MarketStatus marketStatus;
    private LocalTime marketTime;
}
