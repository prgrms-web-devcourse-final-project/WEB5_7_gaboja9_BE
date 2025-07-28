package io.gaboja9.mockstock.domain.orders.service;

import io.gaboja9.mockstock.domain.orders.entity.OrderStatus;
import io.gaboja9.mockstock.domain.orders.entity.OrderType;
import io.gaboja9.mockstock.domain.orders.entity.Orders;
import io.gaboja9.mockstock.domain.orders.repository.OrdersRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class LimitOrdersProcessor {

    private final OrdersRepository ordersRepository;
    private final OrdersService ordersService;
    private final ExecutorService virtualThreadExecutor;
    private final LimitOrdersExecutor limitOrdersExecutor;


    @Scheduled(fixedDelay = 1000)
    public void processLimitOrders() {
        if (!ordersService.openKoreanMarket()) return;

        List<Orders> pendingOrders =
                ordersRepository.findByStatusAndOrderTypeOrderByCreatedAtAsc(
                        OrderStatus.PENDING, OrderType.LIMIT);

        if (pendingOrders.isEmpty()) {
            return;
        }

        List<CompletableFuture<Void>> futures =
                pendingOrders.stream()
                        .map(
                                order ->
                                        CompletableFuture.runAsync(
                                                () -> limitOrdersExecutor.processIndividualOrder(order),
                                                virtualThreadExecutor))
                        .toList();

        try {
            // 모든 작업 완료 대기 (타임아웃 30초)
            CompletableFuture<Void> allOf =
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

            allOf.get(30, TimeUnit.SECONDS);

            log.debug("처리된 주문 수: {}", futures.size());

        } catch (TimeoutException e) {
            log.warn("주문 처리 타임아웃 발생. 처리 중인 주문이 있을 수 있습니다.", e);
            // 타임아웃이 발생해도 실행 중인 작업들은 계속 진행됨
        } catch (ExecutionException e) {
            log.error("주문 처리 중 오류 발생", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("주문 처리 중 인터럽트 발생", e);

            // 실행 중인 모든 작업 취소 시도
            futures.forEach(future -> future.cancel(true));
        }
    }
}
