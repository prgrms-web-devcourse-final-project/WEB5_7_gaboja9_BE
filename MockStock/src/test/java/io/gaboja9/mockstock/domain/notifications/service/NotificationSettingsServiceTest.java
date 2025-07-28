package io.gaboja9.mockstock.domain.notifications.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import io.gaboja9.mockstock.domain.members.entity.Members;
import io.gaboja9.mockstock.domain.members.repository.MembersRepository;
import io.gaboja9.mockstock.domain.notifications.dto.request.NotificationSettingsUpdateRequestDto;
import io.gaboja9.mockstock.domain.notifications.dto.response.NotificationSettingsResponseDto;
import io.gaboja9.mockstock.domain.notifications.entity.Notifications;
import io.gaboja9.mockstock.domain.notifications.exception.NotificationException;
import io.gaboja9.mockstock.domain.notifications.repository.NotificationsRepository;
import io.gaboja9.mockstock.global.exception.BaseException;
import io.gaboja9.mockstock.global.exception.ErrorCode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class NotificationSettingsServiceTest {

    @Mock private NotificationsRepository notificationsRepository;
    @Mock private MembersRepository membersRepository;
    @InjectMocks private NotificationSettingsService notificationSettingsService;

    private Members createMember(Long id) {
        return Members.builder().id(id).email("test@test.com").build();
    }

    private Notifications createNotifications(Members member, boolean trade, boolean market) {
        return Notifications.builder()
                .members(member)
                .tradeNotificationEnabled(trade)
                .marketNotificationEnabled(market)
                .build();
    }

    private NotificationSettingsUpdateRequestDto createRequest(Boolean trade, Boolean market) {
        return NotificationSettingsUpdateRequestDto.builder()
                .tradeNotificationEnabled(trade)
                .marketNotificationEnabled(market)
                .build();
    }

    @Test
    void 알림설정_조회_성공() {
        // given
        Long memberId = 1L;
        Members member = createMember(memberId);
        Notifications notifications = createNotifications(member, false, true);

        given(membersRepository.findById(memberId)).willReturn(Optional.of(member));
        given(notificationsRepository.findByMembersId(memberId))
                .willReturn(Optional.of(notifications));

        // when
        NotificationSettingsResponseDto result =
                notificationSettingsService.getNotificationSettings(memberId);

        // then
        assertThat(result.isTradeNotificationEnabled()).isFalse();
        assertThat(result.isMarketNotificationEnabled()).isTrue();
    }

    @Test
    void 알림설정_조회_실패() {
        // given
        Long memberId = 999L;
        given(membersRepository.findById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationSettingsService.getNotificationSettings(memberId))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUNT_MEMBER);
    }

    @Test
    void 알림설정_업데이트_성공() {
        // given
        Long memberId = 1L;
        Members member = createMember(memberId);
        Notifications notifications = createNotifications(member, true, true);
        NotificationSettingsUpdateRequestDto request = createRequest(false, null);

        given(membersRepository.findById(memberId)).willReturn(Optional.of(member));
        given(notificationsRepository.findByMembersId(memberId))
                .willReturn(Optional.of(notifications));
        given(notificationsRepository.save(notifications)).willReturn(notifications);

        // when
        NotificationSettingsResponseDto result =
                notificationSettingsService.updateNotificationSettings(memberId, request);

        // then
        assertThat(result.isTradeNotificationEnabled()).isFalse();
        assertThat(result.isMarketNotificationEnabled()).isTrue();
    }

    @Test
    void 알림설정_업데이트_실패() {
        // given
        Long memberId = 1L;
        Members member = createMember(memberId);
        Notifications notifications = createNotifications(member, true, true);
        NotificationSettingsUpdateRequestDto request = createRequest(false, false);

        given(membersRepository.findById(memberId)).willReturn(Optional.of(member));
        given(notificationsRepository.findByMembersId(memberId))
                .willReturn(Optional.of(notifications));
        given(notificationsRepository.save(notifications))
                .willThrow(new DataAccessException("DB 에러") {});

        // when & then
        assertThatThrownBy(
                        () ->
                                notificationSettingsService.updateNotificationSettings(
                                        memberId, request))
                .isInstanceOf(NotificationException.class);
    }
}
