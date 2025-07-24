package io.gaboja9.mockstock.domain.stock.service;

import io.gaboja9.mockstock.domain.stock.exception.NotFoundStockException;
import io.gaboja9.mockstock.domain.stock.exception.StockChartException;
import io.gaboja9.mockstock.domain.stock.measurement.MinuteStockPrice;
import io.gaboja9.mockstock.domain.stock.repository.StocksMinuteRepository;
import io.gaboja9.mockstock.domain.stock.repository.StocksRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StocksMinuteChartService {

  private final StocksMinuteRepository stocksMinuteRepository;
  private final StocksRepository stocksRepository;

  /**
   * 차트 초기 로드용 최신 데이터 조회
   *
   * @param stockCode 주식 코드
   * @param limit     초기 로드할 데이터 개수
   * @return 최신 데이터부터 과거 순으로 정렬된 주식 가격 리스트
   */
  public List<MinuteStockPrice> getLatestMinutePrices(String stockCode, int limit) {
    validateStockCode(stockCode);
    validateLimit(limit);

    try {
      return stocksMinuteRepository.findLatestMinutePrices(stockCode, limit);
    } catch (Exception e) {
      log.error("Failed to fetch latest minute prices for stock: {}", stockCode, e);
      throw StockChartException.dataFetchFailed(stockCode, e);
    }
  }

  /**
   * 차트 왼쪽 드래그시 과거 데이터 로드 (무한 스크롤)
   *
   * @param stockCode       주식 코드
   * @param beforeTimestamp 현재 차트에서 가장 오래된 시점
   * @param limit           추가로 로드할 데이터 개수
   * @return 더 과거 데이터 리스트
   */
  public List<MinuteStockPrice> getMorePastData(String stockCode, Instant beforeTimestamp,
      int limit) {
    validateStockCode(stockCode);
    validateLimit(limit);
    validateTimestamp(beforeTimestamp, "과거 데이터 조회를 위한 기준 시점이 필요합니다.");

    try {
      return stocksMinuteRepository.findMinutePricesBefore(stockCode, beforeTimestamp, limit);
    } catch (Exception e) {
      log.error("Failed to fetch past minute prices for stock: {} before {}", stockCode,
          beforeTimestamp, e);
      throw StockChartException.dataFetchFailed(stockCode, e);
    }
  }

  /**
   * 차트 오른쪽 드래그시 미래 데이터 로드 (실시간 업데이트된 데이터가 있다면)
   *
   * @param stockCode      주식 코드
   * @param afterTimestamp 현재 차트에서 가장 최신 시점
   * @param limit          추가로 로드할 데이터 개수
   * @return 더 최신 데이터 리스트
   */
  public List<MinuteStockPrice> getMoreRecentData(String stockCode, Instant afterTimestamp,
      int limit) {
    validateStockCode(stockCode);
    validateLimit(limit);
    validateTimestamp(afterTimestamp, "최신 데이터 조회를 위한 기준 시점이 필요합니다.");

    try {
      return stocksMinuteRepository.findMinutePricesAfter(stockCode, afterTimestamp, limit);
    } catch (Exception e) {
      log.error("Failed to fetch recent minute prices for stock: {} after {}", stockCode,
          afterTimestamp, e);
      throw StockChartException.dataFetchFailed(stockCode, e);
    }
  }


  //입력값 유효성 검증 (일봉과 동일)
  private void validateStockCode(String stockCode) {
    // 기본 null/empty 체크
    if (stockCode == null || stockCode.trim().isEmpty()) {
      throw StockChartException.invalidStockCode(stockCode);
    }

    // 실제 주식 존재 여부 확인
    stocksRepository
        .findByStockCode(stockCode)
        .orElseThrow(() -> new NotFoundStockException(stockCode));
  }

  private void validateLimit(int limit) {
    if (limit <= 0 || limit > 1000) {
      throw StockChartException.invalidLimit(limit);
    }
  }

  private void validateTimestamp(Instant timestamp, String message) {
    if (timestamp == null) {
      throw StockChartException.invalidTimestamp(message);
    }
  }
}