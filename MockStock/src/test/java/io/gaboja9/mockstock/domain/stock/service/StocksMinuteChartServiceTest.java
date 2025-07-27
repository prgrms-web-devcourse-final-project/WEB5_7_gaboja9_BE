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
import io.gaboja9.mockstock.domain.stock.measurement.MinuteStockPrice;
import io.gaboja9.mockstock.domain.stock.repository.StocksMinuteRepository;
import io.gaboja9.mockstock.domain.stock.repository.StocksRepository;

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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Nested
@ExtendWith(MockitoExtension.class)
@DisplayName("StocksMinuteChartService 테스트")
class StocksMinuteChartServiceTest {

    @Mock private StocksMinuteRepository stocksMinuteRepository;

    @Mock private StocksRepository stocksRepository;

    @InjectMocks private StocksMinuteChartService stocksMinuteChartService;

    private Stocks sampleStock;

    @BeforeEach
    void setUp() {
        reset(stocksMinuteRepository, stocksRepository);

        // Stocks 엔티티는 생성자 또는 빌더 패턴 사용
        sampleStock = new Stocks("삼성전자", "005930");
    }

    private MinuteStockPrice createSampleStockPrice(
            String timestamp, String stockCode, long closePrice) {
        MinuteStockPrice price = new MinuteStockPrice();
        price.setTimestamp(Instant.parse(timestamp));
        price.setStockCode(stockCode);
        price.setOpenPrice(closePrice - 50);
        price.setMaxPrice(closePrice + 100);
        price.setMinPrice(closePrice - 100);
        price.setClosePrice(closePrice);
        price.setAccumTrans(50000L);
        return price;
    }

    @Test
    @DisplayName("getLatestMinutePrices()는 유효한 주식코드로 최신 데이터를 정상 조회")
    void getLatestMinutePrices_success() {
        // given
        String stockCode = "005930";
        int limit = 200;

        List<MinuteStockPrice> expectedData =
                Arrays.asList(
                        createSampleStockPrice("2025-07-23T14:30:00Z", stockCode, 61400L),
                        createSampleStockPrice("2025-07-23T14:29:00Z", stockCode, 61450L),
                        createSampleStockPrice("2025-07-23T14:28:00Z", stockCode, 61380L));

        when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
        when(stocksMinuteRepository.findLatestMinutePrices(stockCode, limit))
                .thenReturn(expectedData);

        // when
        List<MinuteStockPrice> actualData =
                stocksMinuteChartService.getLatestMinutePrices(stockCode, limit);

        // then
        assertThat(actualData).hasSize(3);

        // 첫 번째 데이터 검증
        assertThat(actualData.get(0).getStockCode()).isEqualTo("005930");
        assertThat(actualData.get(0).getClosePrice()).isEqualTo(61400L);
        assertThat(actualData.get(0).getOpenPrice()).isEqualTo(61350L);
        assertThat(actualData.get(0).getMaxPrice()).isEqualTo(61500L);
        assertThat(actualData.get(0).getMinPrice()).isEqualTo(61300L);
        assertThat(actualData.get(0).getAccumTrans()).isEqualTo(50000L);

        // 두 번째 데이터 검증
        assertThat(actualData.get(1).getStockCode()).isEqualTo("005930");
        assertThat(actualData.get(1).getClosePrice()).isEqualTo(61450L);

        // 세 번째 데이터 검증
        assertThat(actualData.get(2).getStockCode()).isEqualTo("005930");
        assertThat(actualData.get(2).getClosePrice()).isEqualTo(61380L);

        // Mock 호출 검증
        verify(stocksRepository, times(1)).findByStockCode(stockCode);
        verify(stocksMinuteRepository, times(1)).findLatestMinutePrices(stockCode, limit);
    }

    @Test
    @DisplayName("getLatestMinutePrices()는 null 주식코드에 대해 StockChartException")
    void getLatestMinutePrices_nullStockCode_throwsStockChartException() {
        // given
        String nullStockCode = null;
        int limit = 200;

        // when & then
        assertThatThrownBy(
                        () -> stocksMinuteChartService.getLatestMinutePrices(nullStockCode, limit))
                .isInstanceOf(StockChartException.class);

        // Repository 호출되지 않음 검증
        verify(stocksRepository, never()).findByStockCode(anyString());
        verify(stocksMinuteRepository, never()).findLatestMinutePrices(anyString(), anyInt());
    }

    @Test
    @DisplayName("getLatestMinutePrices()는 빈 문자열 주식코드에 대해 StockChartException")
    void getLatestMinutePrices_emptyStockCode_throwsStockChartException() {
        // given
        String emptyStockCode = "   ";
        int limit = 200;

        // when & then
        assertThatThrownBy(
                        () -> stocksMinuteChartService.getLatestMinutePrices(emptyStockCode, limit))
                .isInstanceOf(StockChartException.class);

        verify(stocksRepository, never()).findByStockCode(anyString());
        verify(stocksMinuteRepository, never()).findLatestMinutePrices(anyString(), anyInt());
    }

    @Test
    @DisplayName("getLatestMinutePrices()는 존재하지 않는 주식코드에 대해 NotFoundStockException")
    void getLatestMinutePrices_notFoundStock_throwsNotFoundStockException() {
        // given
        String invalidStockCode = "INVALID";
        int limit = 200;

        when(stocksRepository.findByStockCode(invalidStockCode)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(
                        () ->
                                stocksMinuteChartService.getLatestMinutePrices(
                                        invalidStockCode, limit))
                .isInstanceOf(NotFoundStockException.class);

        verify(stocksRepository, times(1)).findByStockCode(invalidStockCode);
        verify(stocksMinuteRepository, never()).findLatestMinutePrices(anyString(), anyInt());
    }

    @Test
    @DisplayName("getLatestMinutePrices()는 잘못된 limit 값들에 대해 StockChartException")
    void getLatestMinutePrices_invalidLimit_throwsStockChartException() {
        // given
        String validStockCode = "005930";

        // Mock 설정 - 주식 코드 검증은 통과하도록
        when(stocksRepository.findByStockCode(validStockCode)).thenReturn(Optional.of(sampleStock));

        // when & then - 0 이하 테스트
        assertThatThrownBy(() -> stocksMinuteChartService.getLatestMinutePrices(validStockCode, 0))
                .isInstanceOf(StockChartException.class);

        assertThatThrownBy(() -> stocksMinuteChartService.getLatestMinutePrices(validStockCode, -5))
                .isInstanceOf(StockChartException.class);

        // when & then - 1000 초과 테스트
        assertThatThrownBy(
                        () -> stocksMinuteChartService.getLatestMinutePrices(validStockCode, 1001))
                .isInstanceOf(StockChartException.class);

        assertThatThrownBy(
                        () -> stocksMinuteChartService.getLatestMinutePrices(validStockCode, 2000))
                .isInstanceOf(StockChartException.class);

        // 주식 코드 검증은 통과했지만 limit 검증에서 실패하므로 Repository 호출되지 않음
        verify(stocksRepository, times(4)).findByStockCode(validStockCode);
        verify(stocksMinuteRepository, never()).findLatestMinutePrices(anyString(), anyInt());
    }

    @Test
    @DisplayName("getLatestMinutePrices()는 limit 경계값 1과 1000에서 정상 동작")
    void getLatestMinutePrices_boundaryLimits_success() {
        // given
        String stockCode = "005930";
        List<MinuteStockPrice> singleData =
                Arrays.asList(createSampleStockPrice("2025-07-23T14:30:00Z", stockCode, 61400L));

        when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
        when(stocksMinuteRepository.findLatestMinutePrices(stockCode, 1)).thenReturn(singleData);
        when(stocksMinuteRepository.findLatestMinutePrices(stockCode, 1000))
                .thenReturn(Collections.emptyList());

        // when & then - 최소값 1
        assertThatNoException()
                .isThrownBy(
                        () -> {
                            List<MinuteStockPrice> result =
                                    stocksMinuteChartService.getLatestMinutePrices(stockCode, 1);
                            assertThat(result).hasSize(1);
                        });

        // when & then - 최대값 1000
        assertThatNoException()
                .isThrownBy(
                        () -> {
                            List<MinuteStockPrice> result =
                                    stocksMinuteChartService.getLatestMinutePrices(stockCode, 1000);
                            assertThat(result).isEmpty();
                        });

        verify(stocksRepository, times(2)).findByStockCode(stockCode);
        verify(stocksMinuteRepository, times(1)).findLatestMinutePrices(stockCode, 1);
        verify(stocksMinuteRepository, times(1)).findLatestMinutePrices(stockCode, 1000);
    }

    @Test
    @DisplayName("getMorePastData()는 유효한 파라미터로 과거 데이터를 정상 조회")
    void getMorePastData_success() {
        // given
        String stockCode = "005930";
        Instant beforeTimestamp = Instant.parse("2025-07-23T14:00:00Z");
        int limit = 100;

        List<MinuteStockPrice> expectedPastData =
                Arrays.asList(
                        createSampleStockPrice("2025-07-23T13:59:00Z", stockCode, 61300L),
                        createSampleStockPrice("2025-07-23T13:58:00Z", stockCode, 61350L),
                        createSampleStockPrice("2025-07-23T13:57:00Z", stockCode, 61280L));

        when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
        when(stocksMinuteRepository.findMinutePricesBefore(stockCode, beforeTimestamp, limit))
                .thenReturn(expectedPastData);

        // when
        List<MinuteStockPrice> actualData =
                stocksMinuteChartService.getMorePastData(stockCode, beforeTimestamp, limit);

        // then
        assertThat(actualData).hasSize(3);

        // 모든 데이터가 기준 시점보다 과거인지 검증
        assertThat(actualData.get(0).getTimestamp()).isBefore(beforeTimestamp);
        assertThat(actualData.get(1).getTimestamp()).isBefore(beforeTimestamp);
        assertThat(actualData.get(2).getTimestamp()).isBefore(beforeTimestamp);

        // 데이터 내용 검증
        assertThat(actualData.get(0).getClosePrice()).isEqualTo(61300L);
        assertThat(actualData.get(1).getClosePrice()).isEqualTo(61350L);
        assertThat(actualData.get(2).getClosePrice()).isEqualTo(61280L);

        verify(stocksRepository, times(1)).findByStockCode(stockCode);
        verify(stocksMinuteRepository, times(1))
                .findMinutePricesBefore(stockCode, beforeTimestamp, limit);
    }

    @Test
    @DisplayName("getMorePastData()는 null timestamp에 대해 StockChartException")
    void getMorePastData_nullTimestamp_throwsStockChartException() {
        // given
        String stockCode = "005930";
        Instant nullTimestamp = null;
        int limit = 100;

        when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));

        // when & then
        assertThatThrownBy(
                        () ->
                                stocksMinuteChartService.getMorePastData(
                                        stockCode, nullTimestamp, limit))
                .isInstanceOf(StockChartException.class);

        verify(stocksRepository, times(1)).findByStockCode(stockCode);
        verify(stocksMinuteRepository, never())
                .findMinutePricesBefore(anyString(), any(Instant.class), anyInt());
    }

    @Test
    @DisplayName("getMoreRecentData()는 유효한 파라미터로 최신 데이터를 정상 조회")
    void getMoreRecentData_success() {
        // given
        String stockCode = "005930";
        Instant afterTimestamp = Instant.parse("2025-07-23T14:30:00Z");
        int limit = 50;

        List<MinuteStockPrice> expectedRecentData =
                Arrays.asList(
                        createSampleStockPrice("2025-07-23T14:31:00Z", stockCode, 61500L),
                        createSampleStockPrice("2025-07-23T14:32:00Z", stockCode, 61520L));

        when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
        when(stocksMinuteRepository.findMinutePricesAfter(stockCode, afterTimestamp, limit))
                .thenReturn(expectedRecentData);

        // when
        List<MinuteStockPrice> actualData =
                stocksMinuteChartService.getMoreRecentData(stockCode, afterTimestamp, limit);

        // then
        assertThat(actualData).hasSize(2);

        // 모든 데이터가 기준 시점보다 미래인지 검증
        assertThat(actualData.get(0).getTimestamp()).isAfter(afterTimestamp);
        assertThat(actualData.get(1).getTimestamp()).isAfter(afterTimestamp);

        // 데이터 내용 검증
        assertThat(actualData.get(0).getClosePrice()).isEqualTo(61500L);
        assertThat(actualData.get(1).getClosePrice()).isEqualTo(61520L);

        verify(stocksRepository, times(1)).findByStockCode(stockCode);
        verify(stocksMinuteRepository, times(1))
                .findMinutePricesAfter(stockCode, afterTimestamp, limit);
    }

    @Test
    @DisplayName("getMoreRecentData()는 null timestamp에 대해 StockChartException")
    void getMoreRecentData_nullTimestamp_throwsStockChartException() {
        // given
        String stockCode = "005930";
        Instant nullTimestamp = null;
        int limit = 50;

        when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));

        // when & then
        assertThatThrownBy(
                        () ->
                                stocksMinuteChartService.getMoreRecentData(
                                        stockCode, nullTimestamp, limit))
                .isInstanceOf(StockChartException.class);

        verify(stocksRepository, times(1)).findByStockCode(stockCode);
        verify(stocksMinuteRepository, never())
                .findMinutePricesAfter(anyString(), any(Instant.class), anyInt());
    }

    @Test
    @DisplayName("Repository에서 예외 발생시 StockChartException으로 래핑하여 전파한다")
    void repositoryException_wrappedAsStockChartException() {
        // given
        String stockCode = "005930";
        int limit = 200;
        RuntimeException influxException = new RuntimeException("InfluxDB 연결 실패");

        when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
        when(stocksMinuteRepository.findLatestMinutePrices(stockCode, limit))
                .thenThrow(influxException);

        // when & then
        assertThatThrownBy(() -> stocksMinuteChartService.getLatestMinutePrices(stockCode, limit))
                .isInstanceOf(StockChartException.class)
                .hasCause(influxException);

        verify(stocksRepository, times(1)).findByStockCode(stockCode);
        verify(stocksMinuteRepository, times(1)).findLatestMinutePrices(stockCode, limit);
    }

    @Test
    @DisplayName("빈 결과를 정상적으로 처리한다")
    void emptyResult_handledCorrectly() {
        // given
        String stockCode = "005930";
        int limit = 200;

        when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
        when(stocksMinuteRepository.findLatestMinutePrices(stockCode, limit))
                .thenReturn(Collections.emptyList());

        // when
        List<MinuteStockPrice> result =
                stocksMinuteChartService.getLatestMinutePrices(stockCode, limit);

        // then
        assertThat(result).isEmpty();
        assertThat(result).isNotNull();

        verify(stocksRepository, times(1)).findByStockCode(stockCode);
        verify(stocksMinuteRepository, times(1)).findLatestMinutePrices(stockCode, limit);
    }

    @Test
    @DisplayName("유효성 검증이 올바른 순서로 실행된다")
    void validationOrder_correctSequence() {
        // given
        String nullStockCode = null;
        int invalidLimit = 0;

        // when & then - stockCode 검증이 먼저 실행되어야 함
        assertThatThrownBy(
                        () ->
                                stocksMinuteChartService.getLatestMinutePrices(
                                        nullStockCode, invalidLimit))
                .isInstanceOf(StockChartException.class);

        // stockCode가 null이므로 다른 검증이나 Repository 호출이 되지 않아야 함
        verify(stocksRepository, never()).findByStockCode(anyString());
        verify(stocksMinuteRepository, never()).findLatestMinutePrices(anyString(), anyInt());
    }

    @Test
    @DisplayName("분봉 특화 테스트 - 짧은 시간 간격의 데이터 처리")
    void minuteSpecific_shortTimeIntervalData() {
        // given
        String stockCode = "005930";
        int limit = 60; // 1시간치 분봉 데이터

        // 1분 간격의 연속 데이터
        List<MinuteStockPrice> minuteData =
                Arrays.asList(
                        createSampleStockPrice("2025-07-23T14:03:00Z", stockCode, 61400L),
                        createSampleStockPrice("2025-07-23T14:02:00Z", stockCode, 61390L),
                        createSampleStockPrice("2025-07-23T14:01:00Z", stockCode, 61410L),
                        createSampleStockPrice("2025-07-23T14:00:00Z", stockCode, 61420L));

        when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
        when(stocksMinuteRepository.findLatestMinutePrices(stockCode, limit))
                .thenReturn(minuteData);

        // when
        List<MinuteStockPrice> result =
                stocksMinuteChartService.getLatestMinutePrices(stockCode, limit);

        // then
        assertThat(result).hasSize(4);

        // 시간 순서 확인 (분봉 데이터의 연속성)
        Instant previousTime = null;
        for (MinuteStockPrice price : result) {
            if (previousTime != null) {
                // 각 데이터 포인트가 1분 차이인지 확인
                long timeDiffMinutes =
                        (previousTime.getEpochSecond() - price.getTimestamp().getEpochSecond())
                                / 60;
                assertThat(timeDiffMinutes).isEqualTo(1L);
            }
            previousTime = price.getTimestamp();
        }

        verify(stocksRepository, times(1)).findByStockCode(stockCode);
        verify(stocksMinuteRepository, times(1)).findLatestMinutePrices(stockCode, limit);
    }

    @Test
    @DisplayName("분봉 데이터의 실시간 특성 테스트 - 최신 데이터 우선")
    void minuteSpecific_realtimeDataPriority() {
        // given
        String stockCode = "005930";
        Instant currentTime = Instant.parse("2025-07-23T14:30:00Z");
        Instant afterTimestamp = currentTime.minusSeconds(300); // 5분 전
        int limit = 10;

        // 최신 5분간의 분봉 데이터
        List<MinuteStockPrice> recentData =
                Arrays.asList(
                        createSampleStockPrice("2025-07-23T14:29:00Z", stockCode, 61450L),
                        createSampleStockPrice("2025-07-23T14:28:00Z", stockCode, 61440L),
                        createSampleStockPrice("2025-07-23T14:27:00Z", stockCode, 61460L));

        when(stocksRepository.findByStockCode(stockCode)).thenReturn(Optional.of(sampleStock));
        when(stocksMinuteRepository.findMinutePricesAfter(stockCode, afterTimestamp, limit))
                .thenReturn(recentData);

        // when
        List<MinuteStockPrice> result =
                stocksMinuteChartService.getMoreRecentData(stockCode, afterTimestamp, limit);

        // then
        assertThat(result).hasSize(3);

        // 모든 데이터가 기준 시점 이후인지 확인
        for (MinuteStockPrice price : result) {
            assertThat(price.getTimestamp()).isAfter(afterTimestamp);
        }

        // 실시간 분봉 데이터의 특성상 거래량이 상대적으로 적어야 함
        for (MinuteStockPrice price : result) {
            assertThat(price.getAccumTrans()).isLessThan(100000L); // 일봉보다 적은 거래량
        }

        verify(stocksRepository, times(1)).findByStockCode(stockCode);
        verify(stocksMinuteRepository, times(1))
                .findMinutePricesAfter(stockCode, afterTimestamp, limit);
    }
}
