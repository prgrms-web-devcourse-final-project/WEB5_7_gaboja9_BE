package io.gaboja9.mockstock.domain.notifications.dto;

import io.gaboja9.mockstock.domain.notifications.enums.NotificationEventType;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationDto {
    private NotificationEventType type;
    private String title;
    private String message;
    private LocalDateTime timestamp;
    private NotificationDataDto data;
}
