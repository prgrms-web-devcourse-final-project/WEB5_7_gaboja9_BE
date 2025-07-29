package io.gaboja9.mockstock.domain.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gaboja9.mockstock.domain.stock.entity.Stocks;
import io.gaboja9.mockstock.domain.stock.exception.NotFoundStockException;
import io.gaboja9.mockstock.domain.stock.exception.StockChartException;
import io.gaboja9.mockstock.domain.stock.measurement.DailyStockPrice;
import io.gaboja9.mockstock.domain.stock.repository.StocksRepository;
import io.gaboja9.mockstock.domain.stock.repository.StocksWeeklyRepository;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Nested
@ExtendWith(MockitoExtension.class)
@DisplayName("StocksWeeklyChartService 테스트")
class StocksWeeklyChartServiceTest {

    @Mock
    private StocksWeeklyRepository stocksWeeklyRepository;

    @Mock
    private StocksRepository stocksRepository;

    @InjectMocks
    private StocksWeeklyChartService stocksWeeklyChartService;

    private Stocks sampleStock;
    private String stockCode = "005930";

    @BeforeEach
    void setUp() {
        reset(stocksWeeklyRepository, stocksRepository);
        sampleStock = new Stocks("삼성전자", stockCode);
    }

    private DailyStockPrice createSampleStockPrice(String timestamp, long closePrice) {
        DailyStockPrice price = new DailyStockPrice();
        price.setTimestamp(Instant.parse(timestamp));
        price.setStockCode(stockCode);
        price.setClosePrice(closePrice);
        // ... 다른 필드 설정
        return price;
    }

    // ==================== getLatestWeeklyPrices 테스트 ====================

    @Test
    @DisplayName("[getLatest] 저장된 데이터가 있으면 즉시 반환 (캐시 히트)")
    void getLatestWeeklyPrices_whenStoredDataExists_returnsStoredData() {
        // given
        int limit = 52;
        List<DailyStockPrice> storedData = List.of(createSampleStockPrice("2025-07-21T00:00:00Z", 62100L));

        when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
        when(stocksWeeklyRepository.findStoredWeeklyPrices(stockCode, limit)).thenReturn(storedData);

        // when
        List<DailyStockPrice> actualData = stocksWeeklyChartService.getLatestWeeklyPrices(stockCode, limit);

        // then
        assertThat(actualData).isEqualTo(storedData);
        verify(stocksWeeklyRepository, times(1)).findStoredWeeklyPrices(stockCode, limit);
        verify(stocksWeeklyRepository, never()).aggregateFromDaily(anyString(), anyInt());
    }

    @Test
    @DisplayName("[getLatest] 저장된 데이터가 없으면 일봉에서 집계 (캐시 미스)")
    void getLatestWeeklyPrices_whenNoStoredData_aggregatesFromDaily() {
        // given
        int limit = 52;
        List<DailyStockPrice> aggregatedData = List.of(createSampleStockPrice("2025-07-21T00:00:00Z", 62500L));

        when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
        when(stocksWeeklyRepository.findStoredWeeklyPrices(stockCode, limit)).thenReturn(
            Collections.emptyList());
        when(stocksWeeklyRepository.aggregateFromDaily(stockCode, limit)).thenReturn(aggregatedData);

        // when
        List<DailyStockPrice> actualData = stocksWeeklyChartService.getLatestWeeklyPrices(stockCode, limit);

        // then
        assertThat(actualData).isEqualTo(aggregatedData);
        verify(stocksWeeklyRepository, times(1)).findStoredWeeklyPrices(stockCode, limit);
        verify(stocksWeeklyRepository, times(1)).aggregateFromDaily(stockCode, limit);
    }

    @Test
    @DisplayName("[getLatest] 존재하지 않는 주식코드는 NotFoundStockException 발생")
    void getLatestWeeklyPrices_notFoundStock_throwsException() {
        // given
        String invalidStockCode = "INVALID";
        when(stocksRepository.findByStockCode(invalidStockCode)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> stocksWeeklyChartService.getLatestWeeklyPrices(invalidStockCode, 52))
            .isInstanceOf(NotFoundStockException.class);
        verify(stocksWeeklyRepository, never()).findStoredWeeklyPrices(anyString(), anyInt());
        verify(stocksWeeklyRepository, never()).aggregateFromDaily(anyString(), anyInt());
    }

    // ==================== getMorePastData 테스트 ====================

    @Test
    @DisplayName("[getMorePast] 저장된 과거 데이터가 있으면 즉시 반환 (캐시 히트)")
    void getMorePastData_whenStoredDataExists_returnsStoredData() {
        // given
        Instant beforeTimestamp = Instant.parse("2025-07-14T00:00:00Z");
        int limit = 26;
        List<DailyStockPrice> storedData = List.of(createSampleStockPrice("2025-07-07T00:00:00Z", 63300L));

        when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
        when(stocksWeeklyRepository.findStoredWeeklyPricesBefore(stockCode, beforeTimestamp, limit)).thenReturn(storedData);

        // when
        List<DailyStockPrice> actualData = stocksWeeklyChartService.getMorePastData(stockCode, beforeTimestamp, limit);

        // then
        assertThat(actualData).isEqualTo(storedData);
        verify(stocksWeeklyRepository, times(1)).findStoredWeeklyPricesBefore(stockCode, beforeTimestamp, limit);
        verify(stocksWeeklyRepository, never()).aggregateFromDailyBefore(anyString(), any(Instant.class), anyInt());
    }

    @Test
    @DisplayName("[getMorePast] 저장된 과거 데이터가 없으면 일봉에서 집계 (캐시 미스)")
    void getMorePastData_whenNoStoredData_aggregatesFromDaily() {
        // given
        Instant beforeTimestamp = Instant.parse("2025-07-14T00:00:00Z");
        int limit = 26;
        List<DailyStockPrice> aggregatedData = List.of(createSampleStockPrice("2025-07-07T00:00:00Z", 63300L));

        when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
        when(stocksWeeklyRepository.findStoredWeeklyPricesBefore(stockCode, beforeTimestamp, limit)).thenReturn(Collections.emptyList());
        when(stocksWeeklyRepository.aggregateFromDailyBefore(stockCode, beforeTimestamp, limit)).thenReturn(aggregatedData);

        // when
        List<DailyStockPrice> actualData = stocksWeeklyChartService.getMorePastData(stockCode, beforeTimestamp, limit);

        // then
        assertThat(actualData).isEqualTo(aggregatedData);
        verify(stocksWeeklyRepository, times(1)).findStoredWeeklyPricesBefore(stockCode, beforeTimestamp, limit);
        verify(stocksWeeklyRepository, times(1)).aggregateFromDailyBefore(stockCode, beforeTimestamp, limit);
    }

    @Test
    @DisplayName("[getMorePast] null timestamp는 StockChartException 발생")
    void getMorePastData_nullTimestamp_throwsException() {
        // given
        when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));

        // when & then
        assertThatThrownBy(() -> stocksWeeklyChartService.getMorePastData(stockCode, null, 26))
            .isInstanceOf(StockChartException.class);
    }

    // (getMoreRecentData에 대한 테스트도 위와 유사한 방식으로 2가지 시나리오로 나누어 작성할 수 있습니다.)
}