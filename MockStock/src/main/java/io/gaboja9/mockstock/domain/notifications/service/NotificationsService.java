package io.gaboja9.mockstock.domain.notifications.service;

import io.gaboja9.mockstock.domain.mails.entity.Mails;
import io.gaboja9.mockstock.domain.mails.repository.MailsRepository;
import io.gaboja9.mockstock.domain.members.entity.Members;
import io.gaboja9.mockstock.domain.members.exception.NotFoundMemberException;
import io.gaboja9.mockstock.domain.members.repository.MembersRepository;
import io.gaboja9.mockstock.domain.notifications.entity.Notifications;
import io.gaboja9.mockstock.domain.notifications.enums.NotificationType;
import io.gaboja9.mockstock.domain.notifications.repository.NotificationsRepository;
import io.gaboja9.mockstock.domain.trades.entity.TradeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationsService {

    private final NotificationsRepository notificationsRepository;
    private final MembersRepository membersRepository;
    private final MailsRepository mailsRepository;

    // 1. 매매 완료시 메일 발송
    public void sendTradeNotification(
            Long memberId,
            String stockCode,
            String stockName,
            TradeType tradeType,
            int quantity,
            int price) {

        if (!isTradeNotificationEnabled(memberId)) {
            log.debug("사용자 {}의 매매 알림이 비활성화되어 있습니다.", memberId);
            return;
        }

        Optional<Members> member = membersRepository.findById(memberId);

        String subject = createTradeSubject(stockName, tradeType, quantity, price);
        String content = createTradeContent(stockCode, stockName, tradeType, quantity, price);

        Mails mail = new Mails(subject, content, true, null, member.get());
        mailsRepository.save(mail);

        log.info("매매 알림 발송 완료 - 사용자: {}, 종목: {}, 타입: {}", memberId, stockName, tradeType.name());
    }

    // 2. 사용자의 매매 알림 설정 확인
    private boolean isTradeNotificationEnabled(Long memberId) {
        return notificationsRepository
                .findByMembersId(memberId)
                .map(Notifications::isTradeNotificationEnabled)
                .orElse(true);
    }

    // 3. 매매 알림 제목 생성
    private String createTradeSubject(
            String stockName, TradeType tradeType, int quantity, int price) {

        String action = tradeType == TradeType.BUY ? "매수" : "매도";
        String formattedPrice = NumberFormat.getNumberInstance(Locale.KOREA).format(price);

        return String.format(
                "%s %s %d주 %s 완료 (%s원)",
                NotificationType.TRADE.getPrefix(), stockName, quantity, action, formattedPrice);
    }

    // 4. 매매 알림 내용 생성
    private String createTradeContent(
            String stockCode, String stockName, TradeType tradeType, int quantity, int price) {
        String action = tradeType == TradeType.BUY ? "매수" : "매도";
        String formattedPrice = NumberFormat.getNumberInstance(Locale.KOREA).format(price);
        String totalAmount = NumberFormat.getNumberInstance(Locale.KOREA).format(quantity * price);

        StringBuilder content = new StringBuilder();
        content.append(String.format("$s가 안료되었습니다.\n\n", action));
        content.append(String.format("종목명: %s (%s) \n", stockName, stockCode));
        content.append(String.format("거래수량: %d주 \n", quantity));
        content.append(String.format("체결가격: %s원 \n", formattedPrice));
        content.append(String.format("총 거래내역: %s원 \n\n", totalAmount));
        content.append("거래 내역은 마이페이지에서 확인하실 수 있습니다.");

        return content.toString();
    }
}
