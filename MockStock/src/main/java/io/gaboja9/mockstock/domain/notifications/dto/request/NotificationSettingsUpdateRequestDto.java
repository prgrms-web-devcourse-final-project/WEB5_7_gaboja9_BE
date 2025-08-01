package io.gaboja9.mockstock.domain.notifications.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingsUpdateRequestDto {
    private Boolean tradeNotificationEnabled;
    private Boolean marketNotificationEnabled;
}
