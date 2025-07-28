package io.gaboja9.mockstock.domain.notifications.service;

import io.gaboja9.mockstock.domain.members.entity.Members;
import io.gaboja9.mockstock.domain.members.repository.MembersRepository;
import io.gaboja9.mockstock.domain.notifications.dto.request.NotificationSettingsUpdateRequestDto;
import io.gaboja9.mockstock.domain.notifications.dto.response.NotificationSettingsResponseDto;
import io.gaboja9.mockstock.domain.notifications.entity.Notifications;
import io.gaboja9.mockstock.domain.notifications.exception.NotificationException;
import io.gaboja9.mockstock.domain.notifications.repository.NotificationsRepository;
import io.gaboja9.mockstock.global.exception.BaseException;
import io.gaboja9.mockstock.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationSettingsService {

    private final NotificationsRepository notificationsRepository;
    private final MembersRepository membersRepository;

    @Transactional(readOnly = true)
    public NotificationSettingsResponseDto getNotificationSettings(Long memberId) {
        Members member =
                membersRepository
                        .findById(memberId)
                        .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUNT_MEMBER));

        Notifications notifications =
                notificationsRepository
                        .findByMembersId(memberId)
                        .orElseGet(() -> createDefaultNotifications(member));

        return mapToResponseDto(notifications);
    }

    public NotificationSettingsResponseDto updateNotificationSettings(
            Long memberId, NotificationSettingsUpdateRequestDto requestDto) {

        try {
            Members member =
                    membersRepository
                            .findById(memberId)
                            .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUNT_MEMBER));

            Notifications notifications =
                    notificationsRepository
                            .findByMembersId(memberId)
                            .orElseGet(() -> createDefaultNotifications(member));

            updateNotificationFields(notifications, requestDto);

            Notifications savedNotifications = notificationsRepository.save(notifications);

            log.info("알림 설정 업데이트 완료 - 사용자: {}", memberId);

            return mapToResponseDto(savedNotifications);

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("알림 설정 업데이트 실패 - 사용자: {}", memberId, e);
            throw new NotificationException(
                    ErrorCode.NOTIFICATION_SETTING_UPDATE_FAILED, e.getMessage());
        }
    }

    private Notifications createDefaultNotifications(Members member) {
        log.info("기본 알림 설정 생성 - 사용자: {}", member.getId());

        Notifications notifications =
                Notifications.builder()
                        .members(member)
                        .tradeNotificationEnabled(true)
                        .marketNotificationEnabled(true)
                        .build();

        return notificationsRepository.save(notifications);
    }

    private void updateNotificationFields(
            Notifications notifications, NotificationSettingsUpdateRequestDto requestDto) {

        if (requestDto.getTradeNotificationEnabled() != null) {
            notifications.setTradeNotificationEnabled(requestDto.getTradeNotificationEnabled());
        }

        if (requestDto.getMarketNotificationEnabled() != null) {
            notifications.setMarketNotificationEnabled(requestDto.getMarketNotificationEnabled());
        }
    }

    private NotificationSettingsResponseDto mapToResponseDto(Notifications notifications) {
        return NotificationSettingsResponseDto.builder()
                .tradeNotificationEnabled(notifications.isTradeNotificationEnabled())
                .marketNotificationEnabled(notifications.isMarketNotificationEnabled())
                .build();
    }
}
