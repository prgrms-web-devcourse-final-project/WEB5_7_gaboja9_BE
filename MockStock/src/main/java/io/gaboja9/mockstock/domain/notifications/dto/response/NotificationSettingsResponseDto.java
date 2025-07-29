package io.gaboja9.mockstock.domain.notifications.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingsResponseDto {
    private boolean tradeNotificationEnabled;
    private boolean marketNotificationEnabled;
}
