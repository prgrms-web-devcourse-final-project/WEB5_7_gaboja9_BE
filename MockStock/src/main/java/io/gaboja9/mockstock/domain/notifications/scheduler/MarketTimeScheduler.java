package io.gaboja9.mockstock.domain.notifications.scheduler;

import io.gaboja9.mockstock.domain.notifications.service.HolidayService;
import io.gaboja9.mockstock.domain.notifications.service.NotificationsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketTimeScheduler {
    private final NotificationsService notificationsService;
    private final HolidayService holidayService;

    // 매일 오전 8시 50분 - 개장 10분 전 알림 평일(월~금)에만 실행
    @Scheduled(cron = "0 50 8 * * MON-FRI", zone = "Asia/Seoul")
    public void sendMarketOpenNotification() {
        if (!isTradingDay()) {
            log.info("오늘은 거래일이 아닙니다.");
            return;
        }

        log.info("시장 개장 알림 스케줄러 실행 - {}", LocalDate.now());

        try {
            notificationsService.sendMarketOpenNotification();
            log.info("시장 개장 알림 발송 완료");
        } catch (Exception e) {
            log.error("시장 개장 알림 발송 실패", e);
        }
    }

    // 매일 오후 3시 20분 - 마감 10분 전 알림 평일(월~금)에만 실행
    @Scheduled(cron = "0 20 15 * * MON-FRI", zone = "Asia/Seoul")
    public void sendMarketCloseNotification() {
        if (!isTradingDay()) {
            log.info("오늘은 거래일이 아닙니다.");
            return;
        }

        log.info("시장 마감 알림 스케줄러 실행 - {}", LocalDate.now());

        try {
            notificationsService.sendMarketCloseNotification();
            log.info("시장 마감 알림 발송 완료");
        } catch (Exception e) {
            log.error("시장 마감 알림 발송 실패", e);
        }
    }

    private boolean isTradingDay() {
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();

        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }

        if (holidayService.isHoliday(today)) {
            log.info("오늘({})은 공휴일입니다.", today);
            return false;
        }

        return true;
    }
}
