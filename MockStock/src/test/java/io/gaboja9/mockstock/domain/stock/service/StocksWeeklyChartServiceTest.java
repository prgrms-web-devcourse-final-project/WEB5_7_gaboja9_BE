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
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

  @BeforeEach
  void setUp() {
    reset(stocksWeeklyRepository, stocksRepository);
    sampleStock = new Stocks("삼성전자", "005930");
  }

  // 테스트 데이터 생성을 위한 헬퍼 메서드
  private DailyStockPrice createSampleStockPrice(String timestamp, String stockCode,
      long closePrice) {
    DailyStockPrice price = new DailyStockPrice();
    price.setTimestamp(Instant.parse(timestamp));
    price.setStockCode(stockCode);
    price.setOpenPrice(closePrice - 100);
    price.setMaxPrice(closePrice + 200);
    price.setMinPrice(closePrice - 200);
    price.setClosePrice(closePrice);
    price.setAccumTrans(10000000L);
    return price;
  }

  @Test
  @DisplayName("getLatestWeeklyPrices()는 유효한 주식코드로 최신 주봉 데이터를 정상 조회")
  void getLatestWeeklyPrices_success() {
    // given
    String stockCode = "005930";
    int limit = 52; // 1년치
    List<DailyStockPrice> expectedData = Arrays.asList(
        createSampleStockPrice("2025-07-21T00:00:00Z", stockCode, 62100L),
        createSampleStockPrice("2025-07-14T00:00:00Z", stockCode, 61600L)
    );

    when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
    when(stocksWeeklyRepository.findLatestWeeklyPrices(stockCode, limit)).thenReturn(expectedData);

    // when
    List<DailyStockPrice> actualData = stocksWeeklyChartService.getLatestWeeklyPrices(stockCode,
        limit);

    // then
    assertThat(actualData).isEqualTo(expectedData);
    assertThat(actualData).hasSize(2);
    assertThat(actualData.get(0).getClosePrice()).isEqualTo(62100L);

    verify(stocksRepository, times(1)).findByStockCode(stockCode);
    verify(stocksWeeklyRepository, times(1)).findLatestWeeklyPrices(stockCode, limit);
  }

  @Test
  @DisplayName("getLatestWeeklyPrices()는 존재하지 않는 주식코드에 대해 NotFoundStockException 발생")
  void getLatestWeeklyPrices_notFoundStock_throwsException() {
    // given
    String invalidStockCode = "INVALID";
    int limit = 52;
    when(stocksRepository.findByStockCode(invalidStockCode)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(
        () -> stocksWeeklyChartService.getLatestWeeklyPrices(invalidStockCode, limit))
        .isInstanceOf(NotFoundStockException.class);

    verify(stocksRepository, times(1)).findByStockCode(invalidStockCode);
    verify(stocksWeeklyRepository, never()).findLatestWeeklyPrices(anyString(), anyInt());
  }


  @Test
  @DisplayName("getMorePastData()는 유효한 파라미터로 과거 주봉 데이터를 정상 조회")
  void getMorePastData_success() {
    // given
    String stockCode = "005930";
    Instant beforeTimestamp = Instant.parse("2025-07-14T00:00:00Z");
    int limit = 26;
    List<DailyStockPrice> expectedData = Arrays.asList(
        createSampleStockPrice("2025-07-07T00:00:00Z", stockCode, 63300L)
    );

    when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
    when(stocksWeeklyRepository.findWeeklyPricesBefore(stockCode, beforeTimestamp,
        limit)).thenReturn(expectedData);

    // when
    List<DailyStockPrice> actualData = stocksWeeklyChartService.getMorePastData(stockCode,
        beforeTimestamp, limit);

    // then
    assertThat(actualData).isEqualTo(expectedData);
    assertThat(actualData.get(0).getTimestamp()).isBefore(beforeTimestamp);

    verify(stocksRepository, times(1)).findByStockCode(stockCode);
    verify(stocksWeeklyRepository, times(1)).findWeeklyPricesBefore(stockCode, beforeTimestamp,
        limit);
  }

  @Test
  @DisplayName("getMoreRecentData()는 유효한 파라미터로 최신 주봉 데이터를 정상 조회")
  void getMoreRecentData_success() {
    // given
    String stockCode = "005930";
    Instant afterTimestamp = Instant.parse("2025-07-14T00:00:00Z");
    int limit = 10;
    List<DailyStockPrice> expectedData = Arrays.asList(
        createSampleStockPrice("2025-07-21T00:00:00Z", stockCode, 62100L)
    );

    when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
    when(stocksWeeklyRepository.findWeeklyPricesAfter(stockCode, afterTimestamp, limit)).thenReturn(
        expectedData);

    // when
    List<DailyStockPrice> actualData = stocksWeeklyChartService.getMoreRecentData(stockCode,
        afterTimestamp, limit);

    // then
    assertThat(actualData).isEqualTo(expectedData);
    assertThat(actualData.get(0).getTimestamp()).isAfter(afterTimestamp);

    verify(stocksRepository, times(1)).findByStockCode(stockCode);
    verify(stocksWeeklyRepository, times(1)).findWeeklyPricesAfter(stockCode, afterTimestamp,
        limit);
  }

  @Test
  @DisplayName("getMorePastData()는 null timestamp에 대해 StockChartException 발생")
  void getMorePastData_nullTimestamp_throwsException() {
    // given
    String stockCode = "005930";
    int limit = 26;
    when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));

    // when & then
    assertThatThrownBy(() -> stocksWeeklyChartService.getMorePastData(stockCode, null, limit))
        .isInstanceOf(StockChartException.class);

    verify(stocksRepository, times(1)).findByStockCode(stockCode);
    verify(stocksWeeklyRepository, never()).findWeeklyPricesBefore(anyString(), any(Instant.class),
        anyInt());
  }

  @Test
  @DisplayName("hasWeeklyData()는 데이터가 있을 때 true를 반환")
  void hasWeeklyData_exists_returnsTrue() {
    // given
    String stockCode = "005930";
    when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
    when(stocksWeeklyRepository.hasWeeklyData(stockCode)).thenReturn(true);

    // when
    boolean result = stocksWeeklyChartService.hasWeeklyData(stockCode);

    // then
    assertThat(result).isTrue();
    verify(stocksRepository, times(1)).findByStockCode(stockCode);
    verify(stocksWeeklyRepository, times(1)).hasWeeklyData(stockCode);
  }
}