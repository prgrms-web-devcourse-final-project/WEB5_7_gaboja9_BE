//package io.gaboja9.mockstock.domain.notifications.config;
//
//import io.gaboja9.mockstock.domain.notifications.service.NotificationsService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Slf4j
//@Configuration
//@RequiredArgsConstructor
//public class NotificationTestConfig {
//
//    private final NotificationsService notificationsService;
//
//    // 테스트용 알림
//    @Bean
//    public ApplicationRunner notificationTestRunner() {
//        return args -> {
//            log.info("=== 알림 시스템 테스트 시작 ===");
//
//            // 5초 후 시장 개장 알림 테스트
//            new Thread(() -> {
//                try {
//                    Thread.sleep(5000);
//                    log.info("시장 개장 알림 테스트 실행");
//                    notificationsService.sendMarketOpenNotification();
//                } catch (Exception e) {
//                    log.error("시장 개장 알림 테스트 실패", e);
//                }
//            }).start();
//
//            // 10초 후 시장 마감 알림 테스트
//            new Thread(() -> {
//                try {
//                    Thread.sleep(10000);
//                    log.info("시장 마감 알림 테스트 실행");
//                    notificationsService.sendMarketCloseNotification();
//                } catch (Exception e) {
//                    log.error("시장 마감 알림 테스트 실패", e);
//                }
//            }).start();
//        };
//    }
//}