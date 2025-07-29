package io.gaboja9.mockstock.domain.stock.service;

import io.gaboja9.mockstock.domain.stock.exception.NotFoundStockException;
import io.gaboja9.mockstock.domain.stock.exception.StockChartException;
import io.gaboja9.mockstock.domain.stock.measurement.DailyStockPrice;
import io.gaboja9.mockstock.domain.stock.repository.StocksRepository;
import io.gaboja9.mockstock.domain.stock.repository.StocksWeeklyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StocksWeeklyChartService {

    private final StocksWeeklyRepository stocksWeeklyRepository;
    private final StocksRepository stocksRepository;

    public List<DailyStockPrice> getLatestWeeklyPrices(String stockCode, int limit) {
        validateStockCode(stockCode);
        validateLimit(limit);

        try {
            // 1. 저장된 주봉 데이터를 먼저 조회합니다.
            List<DailyStockPrice> weeklyData =
                    stocksWeeklyRepository.findStoredWeeklyPrices(stockCode, limit);

            // 2. 데이터가 없으면, 일봉에서 실시간으로 집계합니다.
            if (weeklyData.isEmpty()) {
                log.info("저장된 주봉 데이터 없음. 서비스에서 실시간 집계를 요청합니다. stockCode: {}", stockCode);
                return stocksWeeklyRepository.aggregateFromDaily(stockCode, limit);
            }
            return weeklyData;

        } catch (Exception e) {
            log.error("최신 주봉 데이터 조회 실패. stockCode: {}", stockCode, e);
            throw StockChartException.dataFetchFailed(stockCode, e);
        }
    }

    public List<DailyStockPrice> getMorePastData(
            String stockCode, Instant beforeTimestamp, int limit) {
        validateStockCode(stockCode);
        validateLimit(limit);
        validateTimestamp(beforeTimestamp, "과거 데이터 조회를 위한 기준 시점이 필요합니다.");

        try {
            // 1. 저장된 과거 주봉 데이터를 먼저 조회합니다.
            List<DailyStockPrice> weeklyData =
                    stocksWeeklyRepository.findStoredWeeklyPricesBefore(
                            stockCode, beforeTimestamp, limit);

            // 2. 데이터가 없으면, 일봉에서 실시간으로 집계합니다.
            if (weeklyData.isEmpty()) {
                log.info("저장된 과거 주봉 데이터 없음. 서비스에서 실시간 집계를 요청합니다. stockCode: {}", stockCode);
                return stocksWeeklyRepository.aggregateFromDailyBefore(
                        stockCode, beforeTimestamp, limit);
            }
            return weeklyData;

        } catch (Exception e) {
            log.error("과거 주봉 데이터 조회 실패. stockCode: {}, before: {}", stockCode, beforeTimestamp, e);
            throw StockChartException.dataFetchFailed(stockCode, e);
        }
    }

    public List<DailyStockPrice> getMoreRecentData(
            String stockCode, Instant afterTimestamp, int limit) {
        validateStockCode(stockCode);
        validateLimit(limit);
        validateTimestamp(afterTimestamp, "최신 데이터 조회를 위한 기준 시점이 필요합니다.");

        try {
            // 1. 저장된 최신 주봉 데이터를 먼저 조회합니다.
            List<DailyStockPrice> weeklyData =
                    stocksWeeklyRepository.findStoredWeeklyPricesAfter(
                            stockCode, afterTimestamp, limit);

            // 2. 데이터가 없으면, 일봉에서 실시간으로 집계합니다.
            if (weeklyData.isEmpty()) {
                log.info("저장된 최신 주봉 데이터 없음. 서비스에서 실시간 집계를 요청합니다. stockCode: {}", stockCode);
                return stocksWeeklyRepository.aggregateFromDailyAfter(
                        stockCode, afterTimestamp, limit);
            }
            return weeklyData;

        } catch (Exception e) {
            log.error("최신 주봉 데이터 조회 실패. stockCode: {}, after: {}", stockCode, afterTimestamp, e);
            throw StockChartException.dataFetchFailed(stockCode, e);
        }
    }

    private void validateStockCode(String stockCode) {
        if (stockCode == null || stockCode.trim().isEmpty()) {
            throw StockChartException.invalidStockCode(stockCode);
        }
        stocksRepository
                .findByStockCode(stockCode)
                .orElseThrow(() -> new NotFoundStockException(stockCode));
    }

    private void validateLimit(int limit) {
        if (limit <= 0 || limit > 260) {
            throw StockChartException.invalidLimit(limit);
        }
    }

    private void validateTimestamp(Instant timestamp, String message) {
        if (timestamp == null) {
            throw StockChartException.invalidTimestamp(message);
        }
    }
}
