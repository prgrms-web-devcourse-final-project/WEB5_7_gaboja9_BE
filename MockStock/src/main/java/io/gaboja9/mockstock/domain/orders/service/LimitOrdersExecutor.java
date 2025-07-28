package io.gaboja9.mockstock.domain.orders.service;

import io.gaboja9.mockstock.domain.members.entity.Members;
import io.gaboja9.mockstock.domain.notifications.service.NotificationsService;
import io.gaboja9.mockstock.domain.orders.entity.OrderStatus;
import io.gaboja9.mockstock.domain.orders.entity.Orders;
import io.gaboja9.mockstock.domain.orders.exception.NotFoundOrderException;
import io.gaboja9.mockstock.domain.orders.repository.OrdersRepository;
import io.gaboja9.mockstock.domain.portfolios.service.PortfoliosService;
import io.gaboja9.mockstock.domain.trades.entity.TradeType;
import io.gaboja9.mockstock.domain.trades.entity.Trades;
import io.gaboja9.mockstock.domain.trades.repository.TradesRepository;
import io.gaboja9.mockstock.global.websocket.HantuWebSocketHandler;
import io.gaboja9.mockstock.global.websocket.dto.StockPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class LimitOrdersExecutor {
    private final OrdersRepository ordersRepository;
    private final HantuWebSocketHandler hantuWebSocketHandler;
    private final TradesRepository tradesRepository;
    private final PortfoliosService portfoliosService;
    private final NotificationsService notificationsService;

    // Virtual Thread 환경에서는 단순한 Semaphore 기반 동시성 제어가 더 효율적
    private final ConcurrentHashMap<String, Semaphore> memberSemaphores = new ConcurrentHashMap<>();

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processIndividualOrder(Orders order) {
        try {
            Orders currentOrder =
                    ordersRepository
                            .findByIdWithMember(order.getId())
                            .orElseThrow(NotFoundOrderException::new);

            if (currentOrder.getStatus() != OrderStatus.PENDING) {
                log.debug(
                        "이미 처리된 주문입니다. orderId={}, status={}",
                        order.getId(),
                        currentOrder.getStatus());
                return;
            }

            StockPrice price = hantuWebSocketHandler.getLatestPrice(order.getStockCode());
            if (price == null) {
                log.warn(
                        "실시간 가격 정보 없음. orderId={}, stockCode={}",
                        order.getId(),
                        order.getStockCode());
                return;
            }

            int currentPrice = price.getCurrentPrice();
            boolean shouldExecute = shouldExecuteOrder(currentOrder, currentPrice);

            if (shouldExecute) {
                executeOrderWithSemaphore(currentOrder, currentPrice);
            }
        } catch (Exception e) {
            log.error("주문 처리 중 오류 발생 {}: {}", order.getId(), e.getMessage(), e);
        }
    }

    private boolean shouldExecuteOrder(Orders order, int currentPrice) {
        if (order.getTradeType() == TradeType.BUY) {
            return currentPrice <= order.getPrice();
        } else if (order.getTradeType() == TradeType.SELL) {
            return currentPrice >= order.getPrice();
        }
        return false;
    }

    private void executeOrderWithSemaphore(Orders order, int executionPrice) {
        String memberKey = "member_" + order.getMembers().getId();
        Semaphore semaphore = memberSemaphores.computeIfAbsent(memberKey, k -> new Semaphore(1));

        try {
            // Virtual Thread는 블로킹에 강하므로 타임아웃을 길게 설정
            if (semaphore.tryAcquire(30, TimeUnit.SECONDS)) {
                try {
                    // 가격 재확인
                    StockPrice refreshed =
                            hantuWebSocketHandler.getLatestPrice(order.getStockCode());
                    if (refreshed == null) return;

                    int currentPrice = refreshed.getCurrentPrice();
                    if (!shouldExecuteOrder(order, currentPrice)) {
                        log.debug("가격 재확인 후 조건 불만족으로 주문 보류. orderId={}", order.getId());
                        return;
                    }

                    executeOrder(order, executionPrice);
                } finally {
                    semaphore.release();
                }
            } else {
                log.warn("세마포어 획득 타임아웃. orderId={}", order.getId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("주문 실행 중 인터럽트 발생: orderId={}", order.getId(), e);
        } catch (Exception e) {
            log.error("주문 실행 중 오류 발생: orderId={}", order.getId(), e);
        }
    }

    private void executeOrder(Orders order, int executionPrice) {
        order.execute();
        ordersRepository.save(order);
        Members member = order.getMembers();

        Trades trade =
                new Trades(
                        order.getStockCode(),
                        order.getStockName(),
                        order.getTradeType(),
                        order.getQuantity(),
                        executionPrice,
                        member);
        tradesRepository.save(trade);

        if (order.getTradeType() == TradeType.BUY) {
            int frozenAmount = order.getPrice() * order.getQuantity();
            int actualAmount = executionPrice * order.getQuantity();
            int refundAmount = frozenAmount - actualAmount;

            member.setCashBalance(member.getCashBalance() + refundAmount);
            portfoliosService.updateForBuy(
                    member.getId(),
                    order.getStockCode(),
                    order.getStockName(),
                    order.getQuantity(),
                    executionPrice);
        } else if (order.getTradeType() == TradeType.SELL) {
            int actualAmount = executionPrice * order.getQuantity();
            member.setCashBalance(member.getCashBalance() + actualAmount);
        }

        try {
            notificationsService.sendTradeNotification(
                    member.getId(),
                    order.getStockCode(),
                    order.getStockName(),
                    order.getTradeType(),
                    order.getQuantity(),
                    executionPrice);
        } catch (Exception e) {
            log.error(
                    "지정가 {} 알림 발송 실패 - 사용자: {}, 종목: {}",
                    order.getTradeType(),
                    member.getId(),
                    order.getStockCode(),
                    e);
        }

        log.info(
                "주문 체결 완료. orderId={}, type={}, price={}, quantity={}",
                order.getId(),
                order.getTradeType(),
                executionPrice,
                order.getQuantity());
    }

    @Scheduled(fixedDelay = 300000)
    public void cleanupSemaphores() {
        // 사용하지 않는 세마포어 정리 로직
        memberSemaphores
                .entrySet()
                .removeIf(
                        entry ->
                                entry.getValue().availablePermits() == 1
                                        && !entry.getValue().hasQueuedThreads());
        log.debug("세마포어 정리 완료. 현재 크기: {}", memberSemaphores.size());
    }
}
