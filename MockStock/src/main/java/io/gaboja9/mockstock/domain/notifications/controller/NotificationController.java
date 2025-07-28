package io.gaboja9.mockstock.domain.notifications.controller;

import io.gaboja9.mockstock.domain.auth.dto.MembersDetails;
import io.gaboja9.mockstock.domain.notifications.dto.request.NotificationSettingsUpdateRequestDto;
import io.gaboja9.mockstock.domain.notifications.dto.response.NotificationSettingsResponseDto;
import io.gaboja9.mockstock.domain.notifications.service.NotificationSettingsService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationSettingsService notificationSettingsService;

    @GetMapping("/settings")
    public ResponseEntity<NotificationSettingsResponseDto> getNotificationSettings(
            @AuthenticationPrincipal MembersDetails membersDetails) {

        NotificationSettingsResponseDto settings =
                notificationSettingsService.getNotificationSettings(membersDetails.getId());

        return ResponseEntity.ok(settings);
    }

    @PutMapping("/settings")
    public ResponseEntity<NotificationSettingsResponseDto> updateNotificationSettings(
            @Valid @RequestBody NotificationSettingsUpdateRequestDto requestDto,
            @AuthenticationPrincipal MembersDetails membersDetails) {

        NotificationSettingsResponseDto updatedSettings =
                notificationSettingsService.updateNotificationSettings(
                        membersDetails.getId(), requestDto);

        return ResponseEntity.ok(updatedSettings);
    }
}
