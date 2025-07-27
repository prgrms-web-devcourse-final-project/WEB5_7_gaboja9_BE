package io.gaboja9.mockstock.domain.notifications.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import io.gaboja9.mockstock.domain.mails.entity.Mails;
import io.gaboja9.mockstock.domain.mails.repository.MailsRepository;
import io.gaboja9.mockstock.domain.members.entity.Members;
import io.gaboja9.mockstock.domain.members.repository.MembersRepository;
import io.gaboja9.mockstock.domain.notifications.entity.Notifications;
import io.gaboja9.mockstock.domain.notifications.repository.NotificationsRepository;
import io.gaboja9.mockstock.domain.trades.entity.TradeType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class NotificationsServiceTest {

    @Mock private NotificationsRepository notificationsRepository;
    @Mock private MembersRepository membersRepository;
    @Mock private MailsRepository mailsRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks private NotificationsService notificationsService;

    private Members testMember;
    private Notifications enabledNotifications;
    private Notifications disabledNotifications;

    @BeforeEach
    void setUp() {
        testMember =
                new Members(
                        1L,
                        "test@example.com",
                        "testUser",
                        "LOCAL",
                        "profile.png",
                        30000000,
                        0,
                        LocalDateTime.now());

        enabledNotifications =
                Notifications.builder()
                        .tradeNotificationEnabled(true)
                        .marketNotificationEnabled(true)
                        .members(testMember)
                        .build();

        disabledNotifications =
                Notifications.builder()
                        .tradeNotificationEnabled(false)
                        .marketNotificationEnabled(false)
                        .members(testMember)
                        .build();
    }

    @Test
    void 매매알림이_활성화된_사용자는_매매알림을_받는다() {
        // given
        given(notificationsRepository.findByMembersId(1L))
                .willReturn(Optional.of(enabledNotifications));
        given(membersRepository.findById(1L)).willReturn(Optional.of(testMember));

        // when
        notificationsService.sendTradeNotification(1L, "005930", "삼성전자", TradeType.BUY, 10, 80000);

        // then
        // 메일 저장 확인
        verify(mailsRepository).save(any(Mails.class));

        // WebSocket 알림 발송 확인
        verify(messagingTemplate).convertAndSendToUser(eq("1"), eq("/queue/notifications"), any());
    }

    @Test
    void 매매알림이_비활성화된_사용자는_매매알림을_받지않는다() {
        // given
        given(notificationsRepository.findByMembersId(1L))
                .willReturn(Optional.of(disabledNotifications));

        // when
        notificationsService.sendTradeNotification(1L, "005930", "삼성전자", TradeType.BUY, 10, 80000);

        // then
        verify(mailsRepository, never()).save(any(Mails.class));
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void 알림설정이_없는_사용자는_기본값으로_매매알림을_받는다() {
        // given
        given(notificationsRepository.findByMembersId(1L)).willReturn(Optional.empty());
        given(membersRepository.findById(1L)).willReturn(Optional.of(testMember));

        // when
        notificationsService.sendTradeNotification(1L, "005930", "삼성전자", TradeType.BUY, 10, 80000);

        // then
        verify(mailsRepository).save(any(Mails.class));
        verify(messagingTemplate).convertAndSendToUser(eq("1"), eq("/queue/notifications"), any());
    }

    @Test
    void 시장알림이_활성화된_사용자들은_개장알림을_받는다() {
        // given
        List<Members> allMembers = Arrays.asList(testMember);
        given(membersRepository.findAll()).willReturn(allMembers);
        given(notificationsRepository.findByMembersId(1L))
                .willReturn(Optional.of(enabledNotifications));

        // when
        notificationsService.sendMarketOpenNotification();

        // then
        verify(mailsRepository).save(any(Mails.class));
        verify(messagingTemplate).convertAndSendToUser(eq("1"), eq("/queue/notifications"), any());
    }

    @Test
    void 시장알림이_비활성화된_사용자들은_개장알림을_받지않는다() {
        // given
        List<Members> allMembers = Arrays.asList(testMember);
        given(membersRepository.findAll()).willReturn(allMembers);
        given(notificationsRepository.findByMembersId(1L))
                .willReturn(Optional.of(disabledNotifications));

        // when
        notificationsService.sendMarketOpenNotification();

        // then
        verify(mailsRepository, never()).save(any(Mails.class));
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void 사용자가_없으면_개장알림을_발송하지않는다() {
        // given
        given(membersRepository.findAll()).willReturn(Collections.emptyList());

        // when
        notificationsService.sendMarketOpenNotification();

        // then
        verify(mailsRepository, never()).save(any(Mails.class));
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void 시장알림이_활성화된_사용자들은_마감알림을_받는다() {
        // given
        List<Members> allMembers = Arrays.asList(testMember);
        given(membersRepository.findAll()).willReturn(allMembers);
        given(notificationsRepository.findByMembersId(1L))
                .willReturn(Optional.of(enabledNotifications));

        // when
        notificationsService.sendMarketCloseNotification();

        // then
        verify(mailsRepository).save(any(Mails.class));
        verify(messagingTemplate).convertAndSendToUser(eq("1"), eq("/queue/notifications"), any());
    }

    @Test
    void 시장알림이_비활성화된_사용자들은_마감알림을_받지않는다() {
        // given
        List<Members> allMembers = Arrays.asList(testMember);
        given(membersRepository.findAll()).willReturn(allMembers);
        given(notificationsRepository.findByMembersId(1L))
                .willReturn(Optional.of(disabledNotifications));

        // when
        notificationsService.sendMarketCloseNotification();

        // then
        verify(mailsRepository, never()).save(any(Mails.class));
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void 알림설정이_없는_사용자는_기본값으로_마감알림을_받는다() {
        // given
        List<Members> allMembers = Arrays.asList(testMember);
        given(membersRepository.findAll()).willReturn(allMembers);
        given(notificationsRepository.findByMembersId(1L)).willReturn(Optional.empty());

        // when
        notificationsService.sendMarketCloseNotification();

        // then
        verify(mailsRepository).save(any(Mails.class));
        verify(messagingTemplate).convertAndSendToUser(eq("1"), eq("/queue/notifications"), any());
    }
}
