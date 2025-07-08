package io.gaboja9.mockstock.domain.trades.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gaboja9.mockstock.domain.members.entity.Members;
import io.gaboja9.mockstock.domain.trades.dto.request.TradesRequestDto;
import io.gaboja9.mockstock.domain.trades.dto.response.TradesResponseDto;
import io.gaboja9.mockstock.domain.trades.entity.TradeType;
import io.gaboja9.mockstock.domain.trades.entity.Trades;
import io.gaboja9.mockstock.domain.trades.mapper.TradesMapper;
import io.gaboja9.mockstock.domain.trades.repository.TradesRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class TradesServiceTest {

    @Mock private TradesRepository tradesRepository;

    @Mock private TradesMapper tradesMapper;

    @InjectMocks private TradesService tradesService;

    @Test
    void getTradesWithOption_정상() {
        // given
        Long memberId = 1L;

        Members member =
                new Members(
                        memberId,
                        "test@example.com",
                        "testUser",
                        "google",
                        "test.png",
                        5000,
                        0,
                        LocalDateTime.now());

        TradesRequestDto dto =
                TradesRequestDto.builder()
                        .stockCode("005930")
                        .stockName("삼성전자")
                        .startDate(LocalDate.of(2025, 7, 1))
                        .endDate(LocalDate.of(2025, 7, 8))
                        .build();

        Trades trade = new Trades("005930", "삼성전자", TradeType.BUY, 10, 70000, member);
        trade.setCreatedAt(LocalDateTime.of(2025, 7, 2, 9, 0));

        TradesResponseDto responseDto =
                TradesResponseDto.builder()
                        .stockCode("005930")
                        .stockName("삼성전자")
                        .quantity(10)
                        .price(70000)
                        .totalAmount(700000)
                        .tradeType(TradeType.BUY)
                        .tradeDate(trade.getCreatedAt())
                        .build();

        List<Trades> tradesList = List.of(trade);

        // mocking
        when(tradesRepository.findByStockCodeOrStockNameAndCreatedAtBetween(
                        eq("005930"),
                        eq("삼성전자"),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        eq(memberId)))
                .thenReturn(tradesList);

        when(tradesMapper.toDto(trade)).thenReturn(responseDto);

        // when
        List<TradesResponseDto> result = tradesService.getTradesWithOption(memberId, dto);

        // then
        assertEquals(1, result.size());
        assertEquals("005930", result.get(0).getStockCode());
        verify(tradesRepository)
                .findByStockCodeOrStockNameAndCreatedAtBetween(
                        eq("005930"),
                        eq("삼성전자"),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        eq(memberId));
        verify(tradesMapper).toDto(trade);
    }

    @Test
    void getTrades_기본조회_정상() {
        // given
        Long memberId = 1L;

        Members member =
                new Members(
                        memberId,
                        "test@example.com",
                        "testUser",
                        "google",
                        "test.png",
                        5000,
                        0,
                        LocalDateTime.now());

        Trades trade = new Trades("000660", "SK하이닉스", TradeType.SELL, 5, 120000, member);
        trade.setCreatedAt(LocalDateTime.of(2025, 7, 3, 15, 30));

        TradesResponseDto responseDto =
                TradesResponseDto.builder()
                        .stockCode("000660")
                        .stockName("SK하이닉스")
                        .price(120000)
                        .quantity(5)
                        .totalAmount(600000)
                        .tradeType(TradeType.SELL)
                        .tradeDate(trade.getCreatedAt())
                        .build();

        when(tradesRepository.findByMembersId(memberId)).thenReturn(List.of(trade));
        when(tradesMapper.toDto(trade)).thenReturn(responseDto);

        // when
        List<TradesResponseDto> result = tradesService.getTrades(memberId);

        // then
        assertEquals(1, result.size());
        assertEquals("000660", result.get(0).getStockCode());
        verify(tradesRepository).findByMembersId(memberId);
        verify(tradesMapper).toDto(trade);
    }
}
