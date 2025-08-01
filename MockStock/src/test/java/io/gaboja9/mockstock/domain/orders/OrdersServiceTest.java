package io.gaboja9.mockstock.domain.orders;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gaboja9.mockstock.domain.members.entity.Members;
import io.gaboja9.mockstock.domain.members.repository.MembersRepository;
import io.gaboja9.mockstock.domain.notifications.service.NotificationsService;
import io.gaboja9.mockstock.domain.orders.dto.request.OrdersMarketTypeRequestDto;
import io.gaboja9.mockstock.domain.orders.dto.response.OrderResponseDto;
import io.gaboja9.mockstock.domain.orders.entity.Orders;
import io.gaboja9.mockstock.domain.orders.exception.InvalidSellQuantityException;
import io.gaboja9.mockstock.domain.orders.exception.NotEnoughCashException;
import io.gaboja9.mockstock.domain.orders.repository.OrdersRepository;
import io.gaboja9.mockstock.domain.orders.service.OrdersService;
import io.gaboja9.mockstock.domain.portfolios.entity.Portfolios;
import io.gaboja9.mockstock.domain.portfolios.exception.NotFoundPortfolioException;
import io.gaboja9.mockstock.domain.portfolios.repository.PortfoliosRepository;
import io.gaboja9.mockstock.domain.portfolios.service.PortfoliosService;
import io.gaboja9.mockstock.domain.trades.repository.TradesRepository;
import io.gaboja9.mockstock.global.websocket.HantuWebSocketHandler;
import io.gaboja9.mockstock.global.websocket.dto.StockPriceDto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class OrdersServiceTest {

    @Mock private MembersRepository membersRepository;

    @Mock private OrdersRepository ordersRepository;

    @Mock private TradesRepository tradesRepository;

    @Mock private PortfoliosService portfoliosService;

    @Mock private PortfoliosRepository portfoliosRepository;

    @Mock private HantuWebSocketHandler hantuWebSocketHandler;

    @Mock private NotificationsService notificationsService;

    private OrdersService ordersService;

    @BeforeEach
    void setUp() {
        ordersService =
                spy(
                        new OrdersService(
                                membersRepository,
                                ordersRepository,
                                tradesRepository,
                                portfoliosService,
                                portfoliosRepository,
                                hantuWebSocketHandler,
                                notificationsService));

        doReturn(true).when(ordersService).openKoreanMarket();
    }

    @Test
    void executeMarketBuyOrders_성공() {
        Long memberId = 1L;
        Members member =
                new Members(
                        memberId,
                        "test@example.com",
                        "testUser",
                        "google",
                        "profile.png",
                        30000000,
                        0,
                        LocalDateTime.now());
        member.setCashBalance(1_000_000);

        OrdersMarketTypeRequestDto dto =
                OrdersMarketTypeRequestDto.builder()
                        .stockCode("AAPL")
                        .stockName("애플")
                        .quantity(5)
                        .build();

        StockPriceDto mockPrice =
                StockPriceDto.builder().stockCode("AAPL").currentPrice(100_000).build();

        when(membersRepository.findByIdWithLock(memberId)).thenReturn(Optional.of(member));
        when(hantuWebSocketHandler.getLatestPrice("AAPL")).thenReturn(mockPrice);

        OrderResponseDto response = ordersService.executeMarketBuyOrders(memberId, dto);

        assertThat(response.isExecuted()).isTrue();
        assertThat(response.getPrice()).isEqualTo(100000);
        verify(ordersRepository).save(any(Orders.class));
        verify(tradesRepository).save(any());
        verify(portfoliosService).updateForBuy(memberId, "AAPL", "애플", 5, 100000);
        assertThat(member.getCashBalance()).isEqualTo(500_000);
    }

    @Test
    void executeMarketBuyOrders_실패_현금부족() {
        Long memberId = 1L;
        Members member =
                new Members(
                        memberId,
                        "test@example.com",
                        "testUser",
                        "google",
                        "profile.png",
                        30000000,
                        0,
                        LocalDateTime.now());
        member.setCashBalance(100_000);

        OrdersMarketTypeRequestDto dto =
                OrdersMarketTypeRequestDto.builder()
                        .stockCode("AAPL")
                        .stockName("애플")
                        .quantity(2)
                        .build();

        StockPriceDto mockPrice =
                StockPriceDto.builder().stockCode("AAPL").currentPrice(100_000).build();

        when(membersRepository.findByIdWithLock(memberId)).thenReturn(Optional.of(member));
        when(hantuWebSocketHandler.getLatestPrice("AAPL")).thenReturn(mockPrice);

        assertThatThrownBy(() -> ordersService.executeMarketBuyOrders(memberId, dto))
                .isInstanceOf(NotEnoughCashException.class);
    }

    @Test
    void executeMarketSellOrders_성공() {
        Long memberId = 1L;
        Members member =
                new Members(
                        memberId,
                        "test@example.com",
                        "testUser",
                        "google",
                        "profile.png",
                        30000000,
                        0,
                        LocalDateTime.now());
        member.setCashBalance(100_000);

        OrdersMarketTypeRequestDto dto =
                OrdersMarketTypeRequestDto.builder()
                        .stockCode("AAPL")
                        .stockName("애플")
                        .quantity(3)
                        .build();

        StockPriceDto mockPrice =
                StockPriceDto.builder().stockCode("AAPL").currentPrice(100_000).build();

        Portfolios portfolio = new Portfolios("AAPL", "애플", 5, 100000, member);

        when(membersRepository.findByIdWithLock(memberId)).thenReturn(Optional.of(member));
        when(portfoliosRepository.findByMembersIdAndStockCodeWithLock(memberId, "AAPL"))
                .thenReturn(Optional.of(portfolio));
        when(hantuWebSocketHandler.getLatestPrice("AAPL")).thenReturn(mockPrice);

        OrderResponseDto response = ordersService.executeMarketSellOrders(memberId, dto);

        assertThat(response.isExecuted()).isTrue();
        assertThat(response.getPrice()).isEqualTo(100000);
        verify(ordersRepository).save(any(Orders.class));
        verify(tradesRepository).save(any());
        verify(portfoliosService).updateForSell(memberId, "AAPL", 3);
        assertThat(member.getCashBalance()).isEqualTo(100_000 + 300_000);
    }

    @Test
    void executeMarketSellOrders_실패_수량초과() {
        Long memberId = 1L;
        Members member =
                new Members(
                        memberId,
                        "test@example.com",
                        "testUser",
                        "google",
                        "profile.png",
                        30000000,
                        0,
                        LocalDateTime.now());

        OrdersMarketTypeRequestDto dto =
                OrdersMarketTypeRequestDto.builder()
                        .stockCode("AAPL")
                        .stockName("애플")
                        .quantity(10)
                        .build();

        Portfolios portfolio = new Portfolios("AAPL", "애플", 5, 100000, member);

        when(membersRepository.findByIdWithLock(memberId)).thenReturn(Optional.of(member));
        when(portfoliosRepository.findByMembersIdAndStockCodeWithLock(memberId, "AAPL"))
                .thenReturn(Optional.of(portfolio));

        assertThatThrownBy(() -> ordersService.executeMarketSellOrders(memberId, dto))
                .isInstanceOf(InvalidSellQuantityException.class);
    }

    @Test
    void executeMarketSellOrders_실패_포트폴리오없음() {
        Long memberId = 1L;
        Members member =
                new Members(
                        memberId,
                        "test@example.com",
                        "testUser",
                        "google",
                        "profile.png",
                        30000000,
                        0,
                        LocalDateTime.now());

        OrdersMarketTypeRequestDto dto =
                OrdersMarketTypeRequestDto.builder()
                        .stockCode("AAPL")
                        .stockName("애플")
                        .quantity(3)
                        .build();

        when(membersRepository.findByIdWithLock(memberId)).thenReturn(Optional.of(member));
        when(portfoliosRepository.findByMembersIdAndStockCodeWithLock(memberId, "AAPL"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> ordersService.executeMarketSellOrders(memberId, dto))
                .isInstanceOf(NotFoundPortfolioException.class);
    }
}
