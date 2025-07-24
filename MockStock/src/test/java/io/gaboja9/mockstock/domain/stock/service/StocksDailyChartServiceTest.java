package io.gaboja9.mockstock.domain.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
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
import io.gaboja9.mockstock.domain.stock.repository.StocksDailyRepository;
import io.gaboja9.mockstock.domain.stock.repository.StocksRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
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
@DisplayName("StocksDailyChartService 테스트")
class StocksDailyChartServiceTest {

  @Mock
  private StocksDailyRepository stocksDailyRepository;

  @Mock
  private StocksRepository stocksRepository;

  @InjectMocks
  private StocksDailyChartService stocksDailyChartService;

  private Stocks sampleStock;

  @BeforeEach
  void setUp() {
    reset(stocksDailyRepository, stocksRepository);

    // Stocks 엔티티는 생성자 또는 빌더 패턴 사용
    sampleStock = new Stocks("삼성전자", "005930");
  }

  private DailyStockPrice createSampleStockPrice(String timestamp, String stockCode,
      long closePrice) {
    DailyStockPrice price = new DailyStockPrice();
    price.setTimestamp(Instant.parse(timestamp));
    price.setStockCode(stockCode);
    price.setOpenPrice(closePrice - 100);
    price.setMaxPrice(closePrice + 200);
    price.setMinPrice(closePrice - 200);
    price.setClosePrice(closePrice);
    price.setAccumTrans(1000000L);
    return price;
  }

  @Test
  @DisplayName("getLatestDailyPrices()는 유효한 주식코드로 최신 데이터를 정상 조회")
  void getLatestDailyPrices_success() {
    // given
    String stockCode = "005930";
    int limit = 10;

    List<DailyStockPrice> expectedData = Arrays.asList(
        createSampleStockPrice("2025-07-08T00:00:00Z", stockCode, 61400L),
        createSampleStockPrice("2025-07-07T00:00:00Z", stockCode, 61700L),
        createSampleStockPrice("2025-07-04T00:00:00Z", stockCode, 63300L)
    );

    when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
    when(stocksDailyRepository.findLatestDailyPrices(stockCode, limit)).thenReturn(expectedData);

    // when
    List<DailyStockPrice> actualData = stocksDailyChartService.getLatestDailyPrices(stockCode,
        limit);

    // then
    assertThat(actualData).hasSize(3);

    // 첫 번째 데이터 검증
    assertThat(actualData.get(0).getStockCode()).isEqualTo("005930");
    assertThat(actualData.get(0).getClosePrice()).isEqualTo(61400L);
    assertThat(actualData.get(0).getOpenPrice()).isEqualTo(61300L);
    assertThat(actualData.get(0).getMaxPrice()).isEqualTo(61600L);
    assertThat(actualData.get(0).getMinPrice()).isEqualTo(61200L);
    assertThat(actualData.get(0).getAccumTrans()).isEqualTo(1000000L);

    // 두 번째 데이터 검증
    assertThat(actualData.get(1).getStockCode()).isEqualTo("005930");
    assertThat(actualData.get(1).getClosePrice()).isEqualTo(61700L);

    // 세 번째 데이터 검증
    assertThat(actualData.get(2).getStockCode()).isEqualTo("005930");
    assertThat(actualData.get(2).getClosePrice()).isEqualTo(63300L);

    // Mock 호출 검증
    verify(stocksRepository, times(1)).findByStockCode(stockCode);
    verify(stocksDailyRepository, times(1)).findLatestDailyPrices(stockCode, limit);
  }

  @Test
  @DisplayName("getLatestDailyPrices()는 null 주식코드에 대해 StockChartException")
  void getLatestDailyPrices_nullStockCode_throwsStockChartException() {
    // given
    String nullStockCode = null;
    int limit = 10;

    // when & then
    assertThatThrownBy(() -> stocksDailyChartService.getLatestDailyPrices(nullStockCode, limit))
        .isInstanceOf(StockChartException.class);

    // Repository 호출되지 않음 검증
    verify(stocksRepository, never()).findByStockCode(anyString());
    verify(stocksDailyRepository, never()).findLatestDailyPrices(anyString(), anyInt());
  }

  @Test
  @DisplayName("getLatestDailyPrices()는 빈 문자열 주식코드에 대해 StockChartException")
  void getLatestDailyPrices_emptyStockCode_throwsStockChartException() {
    // given
    String emptyStockCode = "   ";
    int limit = 10;

    // when & then
    assertThatThrownBy(() -> stocksDailyChartService.getLatestDailyPrices(emptyStockCode, limit))
        .isInstanceOf(StockChartException.class);

    verify(stocksRepository, never()).findByStockCode(anyString());
    verify(stocksDailyRepository, never()).findLatestDailyPrices(anyString(), anyInt());
  }

  @Test
  @DisplayName("getLatestDailyPrices()는 존재하지 않는 주식코드에 대해 NotFoundStockException")
  void getLatestDailyPrices_notFoundStock_throwsNotFoundStockException() {
    // given
    String invalidStockCode = "INVALID";
    int limit = 10;

    when(stocksRepository.findByStockCode(invalidStockCode)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> stocksDailyChartService.getLatestDailyPrices(invalidStockCode, limit))
        .isInstanceOf(NotFoundStockException.class);

    verify(stocksRepository, times(1)).findByStockCode(invalidStockCode);
    verify(stocksDailyRepository, never()).findLatestDailyPrices(anyString(), anyInt());
  }

  @Test
  @DisplayName("getLatestDailyPrices()는 잘못된 limit 값들에 대해 StockChartException")
  void getLatestDailyPrices_invalidLimit_throwsStockChartException() {
    // given
    String validStockCode = "005930";  // 유효한 주식 코드 사용

    // Mock 설정 - 주식 코드 검증은 통과하도록
    when(stocksRepository.findByStockCode(validStockCode)).thenReturn(Optional.of(sampleStock));

    // when & then - 0 이하 테스트
    assertThatThrownBy(() -> stocksDailyChartService.getLatestDailyPrices(validStockCode, 0))
        .isInstanceOf(StockChartException.class);

    assertThatThrownBy(() -> stocksDailyChartService.getLatestDailyPrices(validStockCode, -5))
        .isInstanceOf(StockChartException.class);

    // when & then - 1000 초과 테스트
    assertThatThrownBy(() -> stocksDailyChartService.getLatestDailyPrices(validStockCode, 1001))
        .isInstanceOf(StockChartException.class);

    assertThatThrownBy(() -> stocksDailyChartService.getLatestDailyPrices(validStockCode, 2000))
        .isInstanceOf(StockChartException.class);

    // 주식 코드 검증은 통과했지만 limit 검증에서 실패하므로 Repository 호출되지 않음
    verify(stocksRepository, times(4)).findByStockCode(validStockCode); // 4번 호출됨
    verify(stocksDailyRepository, never()).findLatestDailyPrices(anyString(), anyInt());
  }

  @Test
  @DisplayName("getLatestDailyPrices()는 limit 경계값 1과 1000에서 정상 동작")
  void getLatestDailyPrices_boundaryLimits_success() {
    // given
    String stockCode = "005930";
    List<DailyStockPrice> singleData = Arrays.asList(
        createSampleStockPrice("2025-07-08T00:00:00Z", stockCode, 61400L)
    );

    when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
    when(stocksDailyRepository.findLatestDailyPrices(stockCode, 1)).thenReturn(singleData);
    when(stocksDailyRepository.findLatestDailyPrices(stockCode, 1000)).thenReturn(
        Collections.emptyList());

    // when & then - 최소값 1
    assertThatNoException().isThrownBy(() -> {
      List<DailyStockPrice> result = stocksDailyChartService.getLatestDailyPrices(stockCode, 1);
      assertThat(result).hasSize(1);
    });

    // when & then - 최대값 1000
    assertThatNoException().isThrownBy(() -> {
      List<DailyStockPrice> result = stocksDailyChartService.getLatestDailyPrices(stockCode, 1000);
      assertThat(result).isEmpty();
    });

    verify(stocksRepository, times(2)).findByStockCode(stockCode);
    verify(stocksDailyRepository, times(1)).findLatestDailyPrices(stockCode, 1);
    verify(stocksDailyRepository, times(1)).findLatestDailyPrices(stockCode, 1000);
  }

  @Test
  @DisplayName("getMorePastData()는 유효한 파라미터로 과거 데이터를 정상 조회")
  void getMorePastData_success() {
    // given
    String stockCode = "005930";
    Instant beforeTimestamp = Instant.parse("2025-07-07T00:00:00Z");
    int limit = 5;

    List<DailyStockPrice> expectedPastData = Arrays.asList(
        createSampleStockPrice("2025-07-04T00:00:00Z", stockCode, 63300L),
        createSampleStockPrice("2025-07-03T00:00:00Z", stockCode, 63800L),
        createSampleStockPrice("2025-07-02T00:00:00Z", stockCode, 60800L)
    );

    when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
    when(stocksDailyRepository.findDailyPricesBefore(stockCode, beforeTimestamp, limit))
        .thenReturn(expectedPastData);

    // when
    List<DailyStockPrice> actualData = stocksDailyChartService.getMorePastData(stockCode,
        beforeTimestamp, limit);

    // then
    assertThat(actualData).hasSize(3);

    // 모든 데이터가 기준 시점보다 과거인지 검증
    assertThat(actualData.get(0).getTimestamp()).isBefore(beforeTimestamp);
    assertThat(actualData.get(1).getTimestamp()).isBefore(beforeTimestamp);
    assertThat(actualData.get(2).getTimestamp()).isBefore(beforeTimestamp);

    // 데이터 내용 검증
    assertThat(actualData.get(0).getClosePrice()).isEqualTo(63300L);
    assertThat(actualData.get(1).getClosePrice()).isEqualTo(63800L);
    assertThat(actualData.get(2).getClosePrice()).isEqualTo(60800L);

    verify(stocksRepository, times(1)).findByStockCode(stockCode);
    verify(stocksDailyRepository, times(1)).findDailyPricesBefore(stockCode, beforeTimestamp,
        limit);
  }

  @Test
  @DisplayName("getMorePastData()는 null timestamp에 대해 StockChartException")
  void getMorePastData_nullTimestamp_throwsStockChartException() {
    // given
    String stockCode = "005930";
    Instant nullTimestamp = null;
    int limit = 5;

    when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));

    // when & then
    assertThatThrownBy(
        () -> stocksDailyChartService.getMorePastData(stockCode, nullTimestamp, limit))
        .isInstanceOf(StockChartException.class);

    verify(stocksRepository, times(1)).findByStockCode(stockCode);
    verify(stocksDailyRepository, never()).findDailyPricesBefore(anyString(), any(Instant.class),
        anyInt());
  }

  @Test
  @DisplayName("getMoreRecentData()는 유효한 파라미터로 최신 데이터를 정상 조회")
  void getMoreRecentData_success() {
    // given
    String stockCode = "005930";
    Instant afterTimestamp = Instant.parse("2025-07-08T00:00:00Z");
    int limit = 3;

    List<DailyStockPrice> expectedRecentData = Arrays.asList(
        createSampleStockPrice("2025-07-09T00:00:00Z", stockCode, 62000L),
        createSampleStockPrice("2025-07-10T00:00:00Z", stockCode, 62500L)
    );

    when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
    when(stocksDailyRepository.findDailyPricesAfter(stockCode, afterTimestamp, limit))
        .thenReturn(expectedRecentData);

    // when
    List<DailyStockPrice> actualData = stocksDailyChartService.getMoreRecentData(stockCode,
        afterTimestamp, limit);

    // then
    assertThat(actualData).hasSize(2);

    // 모든 데이터가 기준 시점보다 미래인지 검증
    assertThat(actualData.get(0).getTimestamp()).isAfter(afterTimestamp);
    assertThat(actualData.get(1).getTimestamp()).isAfter(afterTimestamp);

    // 데이터 내용 검증
    assertThat(actualData.get(0).getClosePrice()).isEqualTo(62000L);
    assertThat(actualData.get(1).getClosePrice()).isEqualTo(62500L);

    verify(stocksRepository, times(1)).findByStockCode(stockCode);
    verify(stocksDailyRepository, times(1)).findDailyPricesAfter(stockCode, afterTimestamp, limit);
  }

  @Test
  @DisplayName("getMoreRecentData()는 null timestamp에 대해 StockChartException을 발생시킨다")
  void getMoreRecentData_nullTimestamp_throwsStockChartException() {
    // given
    String stockCode = "005930";
    Instant nullTimestamp = null;
    int limit = 5;

    when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));

    // when & then
    assertThatThrownBy(
        () -> stocksDailyChartService.getMoreRecentData(stockCode, nullTimestamp, limit))
        .isInstanceOf(StockChartException.class);

    verify(stocksRepository, times(1)).findByStockCode(stockCode);
    verify(stocksDailyRepository, never()).findDailyPricesAfter(anyString(), any(Instant.class),
        anyInt());
  }

  @Test
  @DisplayName("Repository에서 예외 발생시 StockChartException으로 래핑하여 전파한다")
  void repositoryException_wrappedAsStockChartException() {
    // given
    String stockCode = "005930";
    int limit = 10;
    RuntimeException influxException = new RuntimeException("InfluxDB 연결 실패");

    when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
    when(stocksDailyRepository.findLatestDailyPrices(stockCode, limit)).thenThrow(influxException);

    // when & then
    assertThatThrownBy(() -> stocksDailyChartService.getLatestDailyPrices(stockCode, limit))
        .isInstanceOf(StockChartException.class)
        .hasCause(influxException);

    verify(stocksRepository, times(1)).findByStockCode(stockCode);
    verify(stocksDailyRepository, times(1)).findLatestDailyPrices(stockCode, limit);
  }

  @Test
  @DisplayName("빈 결과를 정상적으로 처리한다")
  void emptyResult_handledCorrectly() {
    // given
    String stockCode = "005930";
    int limit = 10;

    when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
    when(stocksDailyRepository.findLatestDailyPrices(stockCode, limit))
        .thenReturn(Collections.emptyList());

    // when
    List<DailyStockPrice> result = stocksDailyChartService.getLatestDailyPrices(stockCode, limit);

    // then
    assertThat(result).isEmpty();
    assertThat(result).isNotNull();

    verify(stocksRepository, times(1)).findByStockCode(stockCode);
    verify(stocksDailyRepository, times(1)).findLatestDailyPrices(stockCode, limit);
  }

  @Test
  @DisplayName("유효성 검증이 올바른 순서로 실행된다")
  void validationOrder_correctSequence() {
    // given
    String nullStockCode = null;
    int invalidLimit = 0;

    // when & then - stockCode 검증이 먼저 실행되어야 함
    assertThatThrownBy(
        () -> stocksDailyChartService.getLatestDailyPrices(nullStockCode, invalidLimit))
        .isInstanceOf(StockChartException.class);

    // stockCode가 null이므로 다른 검증이나 Repository 호출이 되지 않아야 함
    verify(stocksRepository, never()).findByStockCode(anyString());
    verify(stocksDailyRepository, never()).findLatestDailyPrices(anyString(), anyInt());
  }


  @Test
  @DisplayName("미래 데이터 조회 - null timestamp 예외")
  void getMoreRecentData_NullTimestamp_ThrowsStockChartException() {
    // given
    String stockCode = "005930";
    Instant nullTimestamp = null;
    int limit = 5;

    when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));

    // when & then
    assertThatThrownBy(
        () -> stocksDailyChartService.getMoreRecentData(stockCode, nullTimestamp, limit))
        .isInstanceOf(StockChartException.class);
  }

  @Test
  @DisplayName("Repository 예외 발생시 StockChartException으로 래핑")
  void getLatestDailyPrices_RepositoryException_ThrowsStockChartException() {
    // given
    String stockCode = "005930";
    int limit = 10;

    when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
    when(stocksDailyRepository.findLatestDailyPrices(stockCode, limit))
        .thenThrow(new RuntimeException("InfluxDB 연결 실패"));

    // when & then
    assertThatThrownBy(() -> stocksDailyChartService.getLatestDailyPrices(stockCode, limit))
        .isInstanceOf(StockChartException.class);
  }

  @Test
  @DisplayName("빈 결과 처리")
  void getLatestDailyPrices_EmptyResult() {
    // given
    String stockCode = "005930";
    int limit = 10;

    when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
    when(stocksDailyRepository.findLatestDailyPrices(stockCode, limit))
        .thenReturn(Collections.emptyList());

    // when
    List<DailyStockPrice> result = stocksDailyChartService.getLatestDailyPrices(stockCode, limit);

    // then
    assertThat(result).isEmpty();
    verify(stocksRepository).findByStockCode(stockCode);
    verify(stocksDailyRepository).findLatestDailyPrices(stockCode, limit);
  }

  @Test
  @DisplayName("limit 경계값 테스트")
  void validateLimit_BoundaryValues() {
    // given
    String stockCode = "005930";
    when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
    when(stocksDailyRepository.findLatestDailyPrices(stockCode, 1))
        .thenReturn(Collections.singletonList(
            createSampleStockPrice("2025-07-08T00:00:00Z", stockCode, 61400L)));
    when(stocksDailyRepository.findLatestDailyPrices(stockCode, 1000))
        .thenReturn(Collections.emptyList());

    // when & then - 최소값 1 (성공)
    assertThatNoException().isThrownBy(() ->
        stocksDailyChartService.getLatestDailyPrices(stockCode, 1));

    // when & then - 최대값 1000 (성공)
    assertThatNoException().isThrownBy(() ->
        stocksDailyChartService.getLatestDailyPrices(stockCode, 1000));
  }

  @Test
  @DisplayName("모든 유효성 검증이 순서대로 실행되는지 확인")
  void validationOrder_Test() {
    // given
    String invalidStockCode = null;
    int invalidLimit = 0;

    // when & then - stockCode 검증이 먼저 실행되어야 함
    assertThatThrownBy(
        () -> stocksDailyChartService.getLatestDailyPrices(invalidStockCode, invalidLimit))
        .isInstanceOf(StockChartException.class);

    // stockCode가 null이므로 stocksRepository 호출되지 않아야 함
    verify(stocksRepository, never()).findByStockCode(anyString());
  }
}