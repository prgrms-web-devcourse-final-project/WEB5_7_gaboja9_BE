package io.gaboja9.mockstock.domain.orders.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.gaboja9.mockstock.domain.members.entity.Members;
import io.gaboja9.mockstock.domain.members.exception.NotFoundMemberException;
import io.gaboja9.mockstock.domain.members.repository.MembersRepository;
import io.gaboja9.mockstock.domain.orders.dto.request.OrdersLimitTypeRequestDto;
import io.gaboja9.mockstock.domain.orders.dto.request.OrdersMarketTypeRequestDto;
import io.gaboja9.mockstock.domain.orders.dto.response.OrderResponseDto;
import io.gaboja9.mockstock.domain.orders.entity.OrderType;
import io.gaboja9.mockstock.domain.orders.entity.Orders;
import io.gaboja9.mockstock.domain.orders.exception.InvalidSellQuantityException;
import io.gaboja9.mockstock.domain.orders.exception.NotEnoughCashException;
import io.gaboja9.mockstock.domain.orders.exception.OrderProcessingInterruptedException;
import io.gaboja9.mockstock.domain.orders.exception.OrderProcessingTimeoutException;
import io.gaboja9.mockstock.domain.orders.repository.OrdersRepository;
import io.gaboja9.mockstock.domain.portfolios.entity.Portfolios;
import io.gaboja9.mockstock.domain.portfolios.exception.NotFoundPortfolioException;
import io.gaboja9.mockstock.domain.portfolios.repository.PortfoliosRepository;
import io.gaboja9.mockstock.domain.portfolios.service.PortfoliosService;
import io.gaboja9.mockstock.domain.trades.entity.TradeType;
import io.gaboja9.mockstock.domain.trades.entity.Trades;
import io.gaboja9.mockstock.domain.trades.repository.TradesRepository;
import io.gaboja9.mockstock.global.websocket.HantuWebSocketHandler;
import io.gaboja9.mockstock.global.websocket.dto.StockPrice;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrdersService {

    private final MembersRepository membersRepository;
    private final OrdersRepository ordersRepository;
    private final TradesRepository tradesRepository;
    private final PortfoliosService portfoliosService;
    private final PortfoliosRepository portfoliosRepository;
    private final HantuWebSocketHandler hantuWebSocketHandler;

    private final Cache<String, ReentrantLock> stockLocks = Caffeine.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    /**
     * 종목 코드별로 락을 걸어서 자주 거래되는 종목의 경우 병목현상이 발생할 가능성이 있음..
     */
    private ReentrantLock getStockLock(String stockCode) {
        return stockLocks.get(stockCode, key -> new ReentrantLock());
    }

    private <T> T executeWithStockLock(String stockCode, Supplier<T> task) {
        ReentrantLock lock = getStockLock(stockCode);
        try {
            if (lock.tryLock(10, TimeUnit.SECONDS)) {
                try {
                    return task.get();
                } finally {
                    lock.unlock();
                }
            } else {
                throw new OrderProcessingTimeoutException();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OrderProcessingInterruptedException();
        }
    }

    @Transactional
    public OrderResponseDto executeMarketBuyOrders(Long memberId, OrdersMarketTypeRequestDto dto) {
        return executeWithStockLock(dto.getStockCode(), () -> {
            Members findMember =
                    membersRepository
                            .findByIdWithLock(memberId)
                            .orElseThrow(() -> new NotFoundMemberException(memberId));

            String stockCode = dto.getStockCode();
            String stockName = dto.getStockName();
            int quantity = dto.getQuantity();

            Integer currentPrice = getCurrentPriceOrNull(stockCode);
            if (currentPrice == null) {
                return OrderResponseDto.builder()
                        .executed(false)
                        .message("시장가 매수 실패: 현재 가격 정보를 불러올 수 없습니다.")
                        .build();
            }

            int totalPrice = currentPrice * quantity;
            int cashBalance = findMember.getCashBalance();

            if (cashBalance < totalPrice) {
                throw new NotEnoughCashException(cashBalance);
            }

            Orders order = new Orders(stockCode, stockName, OrderType.MARKET, TradeType.BUY, quantity, currentPrice, findMember);
            order.execute();
            ordersRepository.save(order);

            Trades trade = new Trades(stockCode, stockName, TradeType.BUY, quantity, currentPrice, findMember);
            tradesRepository.save(trade);

            findMember.setCashBalance(findMember.getCashBalance() - totalPrice);

            portfoliosService.updateForBuy(memberId, stockCode, stockName, quantity, currentPrice);

            return OrderResponseDto.builder()
                    .executed(true)
                    .message("시장가 매수 완료")
                    .price(currentPrice)
                    .build();
        });
    }

    @Transactional
    public OrderResponseDto executeMarketSellOrders(Long memberId, OrdersMarketTypeRequestDto dto) {
        return executeWithStockLock(dto.getStockCode(), () -> {
            Members findMember =
                    membersRepository
                            .findByIdWithLock(memberId)
                            .orElseThrow(() -> new NotFoundMemberException(memberId));

            String stockCode = dto.getStockCode();
            String stockName = dto.getStockName();
            int quantity = dto.getQuantity();

            Portfolios portfolio = portfoliosRepository.findByMembersIdAndStockCodeWithLock(memberId, stockCode)
                    .orElseThrow(() -> new NotFoundPortfolioException());

            if (portfolio.getQuantity() < quantity) {
                throw new InvalidSellQuantityException(quantity);
            }

            Integer currentPrice = getCurrentPriceOrNull(stockCode);
            if (currentPrice == null) {
                return OrderResponseDto.builder()
                        .executed(false)
                        .message("시장가 매도 실패: 현재 가격 정보를 불러올 수 없습니다.")
                        .build();
            }

            int totalAmount = currentPrice * quantity;

            Orders order = new Orders(stockCode, stockName, OrderType.MARKET, TradeType.SELL, quantity, currentPrice, findMember);
            order.execute();
            ordersRepository.save(order);

            Trades trades = new Trades(stockCode, stockName, TradeType.SELL, quantity, currentPrice, findMember);
            tradesRepository.save(trades);

            portfoliosService.updateForSell(memberId, stockCode, quantity);

            findMember.setCashBalance(findMember.getCashBalance() + totalAmount);

            return OrderResponseDto.builder()
                    .executed(true)
                    .message("시장가 매도 완료")
                    .price(currentPrice)
                    .build();
        });
    }

    @Transactional
    public OrderResponseDto executeLimitBuyOrders(Long memberId, OrdersLimitTypeRequestDto dto) {
        return executeWithStockLock(dto.getStockCode(), () -> {
            Members findMember =
                    membersRepository
                            .findByIdWithLock(memberId)
                            .orElseThrow(() -> new NotFoundMemberException(memberId));

            String stockCode = dto.getStockCode();
            String stockName = dto.getStockName();
            int limitPrice = dto.getPrice();
            int quantity = dto.getQuantity();
            int cashBalance = findMember.getCashBalance();

            int totalAmount = limitPrice * quantity;

            if (cashBalance < totalAmount) {
                throw new NotEnoughCashException(cashBalance);
            }

            Orders order = new Orders(stockCode, stockName, OrderType.LIMIT, TradeType.BUY, quantity, limitPrice, findMember);

            Integer currentPrice = getCurrentPriceOrNull(stockCode);
            if (currentPrice == null) {
                return OrderResponseDto.builder()
                        .executed(false)
                        .message("지정가 매수 실패: 현재 가격 정보를 불러올 수 없습니다.")
                        .build();
            }

            if (currentPrice <= limitPrice) {
                // 즉시 체결
                order.execute();
                ordersRepository.save(order);

                // 거래 내역 저장
                Trades trade = new Trades(stockCode, stockName, TradeType.BUY, quantity, currentPrice, findMember);
                tradesRepository.save(trade);

                // 잔고 차감 (실제 체결가 기준)
                int actualAmount = currentPrice * quantity;
                findMember.setCashBalance(findMember.getCashBalance() - actualAmount);

                // 포트폴리오 업데이트
                portfoliosService.updateForBuy(memberId, stockCode, stockName, quantity, currentPrice);
                return OrderResponseDto.builder()
                        .executed(true)
                        .message("지정가 매수 즉시 체결 완료")
                        .price(currentPrice)
                        .build();
            } else {
                // 대기 주문으로 등록
                ordersRepository.save(order);

                // 잔고 동결 (실제 거래소에서는 지정가 금액만큼 동결)
                findMember.setCashBalance(findMember.getCashBalance() - totalAmount);
                return OrderResponseDto.builder()
                        .executed(false)
                        .message("지정가 매수 주문 대기중")
                        .price(limitPrice)
                        .build();
            }
        });
    }

    @Transactional
    public OrderResponseDto executeLimitSellOrders(Long memberId, OrdersLimitTypeRequestDto dto) {
        return executeWithStockLock(dto.getStockCode(), () -> {
            Members findMember =
                    membersRepository
                            .findByIdWithLock(memberId)
                            .orElseThrow(() -> new NotFoundMemberException(memberId));

            String stockCode = dto.getStockCode();
            String stockName = dto.getStockName();
            int limitPrice = dto.getPrice();
            int quantity = dto.getQuantity();
            int cashBalance = findMember.getCashBalance();


            Portfolios portfolio = portfoliosRepository.findByMembersIdAndStockCodeWithLock(memberId, stockCode)
                    .orElseThrow(() -> new NotFoundPortfolioException());

            if (portfolio.getQuantity() < quantity) {
                throw new InvalidSellQuantityException(quantity);
            }
            Integer currentPrice = getCurrentPriceOrNull(stockCode);
            if (currentPrice == null) {
                return OrderResponseDto.builder()
                        .executed(false)
                        .message("지정가 매도 실패: 현재 가격 정보를 불러올 수 없습니다.")
                        .build();
            }

            Orders order = new Orders(stockCode, stockName, OrderType.LIMIT, TradeType.SELL, quantity, limitPrice, findMember);

            if (currentPrice >= limitPrice) {
                order.execute();
                ordersRepository.save(order);

                Trades trades = new Trades(stockCode, stockName, TradeType.SELL, quantity, currentPrice, findMember);
                tradesRepository.save(trades);

                int actualAmount = currentPrice * quantity;
                findMember.setCashBalance(cashBalance + actualAmount);

                portfoliosService.updateForSell(memberId, stockCode, quantity);

                return OrderResponseDto.builder()
                        .executed(true)
                        .message("지정가 매도 즉시 체결 완료")
                        .price(currentPrice)
                        .build();
            } else {
                ordersRepository.save(order);

                portfoliosService.updateForSell(memberId, stockCode, quantity);

                return OrderResponseDto.builder()
                        .executed(false)
                        .message("지정가 매도 주문 대기중")
                        .price(limitPrice)
                        .build();
            }
        });
    }

    private Integer getCurrentPriceOrNull(String stockCode) {
        StockPrice stockPrice = hantuWebSocketHandler.getLatestPrice(stockCode);
        if (stockPrice == null) {
            log.warn("현재 가격 정보를 불러올 수 없습니다: {}", stockCode);
            return null;
        }
        return stockPrice.getCurrentPrice();
    }

    @Transactional
    public void remove(Long memberId) {
        Members findMember =
                membersRepository
                        .findById(memberId)
                        .orElseThrow(() -> new NotFoundMemberException(memberId));

        ordersRepository.deleteByMembersId(memberId);

    }
}
