package io.gaboja9.mockstock.domain.orders.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.gaboja9.mockstock.domain.members.entity.Members;
import io.gaboja9.mockstock.domain.orders.entity.OrderStatus;
import io.gaboja9.mockstock.domain.orders.entity.OrderType;
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

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class LimitOrdersProcessor {

    private final OrdersRepository ordersRepository;
    private final TradesRepository tradesRepository;
    private final PortfoliosService portfoliosService;
    private final HantuWebSocketHandler hantuWebSocketHandler;
    private final OrdersService ordersService;

    // 동시성 제어를 위한 락 매니저
    private final Cache<String, Object> memberLocks = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    @Scheduled(fixedDelay = 1000)
    public void processLimitOrders() {
        if (!ordersService.openKoreanMarket()) return;
        List<Orders> pendingOrders = ordersRepository
                .findByStatusAndOrderTypeOrderByCreatedAtAsc(OrderStatus.PENDING, OrderType.LIMIT);

        for (Orders order : pendingOrders) {
            try {
                processIndividualOrder(order);
            } catch (Exception e) {
                log.error("주문 처리 중 오류 발생 {}: {}", order.getId(), e.getMessage(), e);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processIndividualOrder(Orders order) {

        Orders currentOrder = ordersRepository.findByIdWithMember(order.getId())
                .orElseThrow(() -> new NotFoundOrderException());

        if (currentOrder.getStatus() != OrderStatus.PENDING) {
            log.debug("이미 처리된 주문입니다. orderId={}, status={}",
                    order.getId(), currentOrder.getStatus());
            return;
        }

        StockPrice price = hantuWebSocketHandler.getLatestPrice(order.getStockCode());
        if (price == null) {
            log.warn("실시간 가격 정보 없음. orderId={}, stockCode={}",
                    order.getId(), order.getStockCode());
            return;
        }

        int currentPrice = price.getCurrentPrice();
        boolean shouldExecute = shouldExecuteOrder(currentOrder, currentPrice);

        if (shouldExecute) {
            executeOrderWithLock(currentOrder, currentPrice);
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

    private void executeOrderWithLock(Orders order, int executionPrice) {
        String memberKey = "member_" + order.getMembers().getId();

        Object lock = memberLocks.get(memberKey, k -> new Object());

        synchronized (lock) {
            try {
                StockPrice refreshed = hantuWebSocketHandler.getLatestPrice(order.getStockCode());
                if (refreshed == null) return;
                int currentPrice = refreshed.getCurrentPrice();
                if (!shouldExecuteOrder(order, currentPrice)) {
                    log.debug("가격 재확인 후 조건 불만족으로 주문 보류.");
                    return;
                }
                executeOrder(order, executionPrice);
            } catch (Exception e) {
                log.error("주문 실행 중 오류 발생: {}", e.getMessage(), e);
            }
        }
    }

    private void executeOrder(Orders order, int executionPrice) {
        order.execute();
        ordersRepository.save(order);
        Members member = order.getMembers();

        Trades trade = new Trades(
                order.getStockCode(),
                order.getStockName(),
                order.getTradeType(),
                order.getQuantity(),
                executionPrice,
                member
        );
        tradesRepository.save(trade);

        if (order.getTradeType() == TradeType.BUY) {
            int frozenAmount = order.getPrice() * order.getQuantity();
            int actualAmount = executionPrice * order.getQuantity();
            int refundAmount = frozenAmount - actualAmount;

            member.setCashBalance(member.getCashBalance() + refundAmount);
            portfoliosService.updateForBuy(member.getId(), order.getStockCode(), order.getStockName(), order.getQuantity(), executionPrice);
        } else if (order.getTradeType() == TradeType.SELL) {
            int actualAmount = executionPrice * order.getQuantity();
            member.setCashBalance(member.getCashBalance() + actualAmount);
        }
    }
}
