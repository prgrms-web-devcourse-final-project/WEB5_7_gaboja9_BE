package io.gaboja9.mockstock.domain.stock.service;

import io.gaboja9.mockstock.domain.stock.exception.NotFoundStockException;
import io.gaboja9.mockstock.domain.stock.exception.StockChartException;
import io.gaboja9.mockstock.domain.stock.measurement.MinuteStockPrice;
import io.gaboja9.mockstock.domain.stock.repository.StocksFiveMinuteRepository;
import io.gaboja9.mockstock.domain.stock.repository.StocksRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StocksFiveMinuteChartService {

    private final StocksFiveMinuteRepository stocksFiveMinuteRepository;
    private final StocksRepository stocksRepository;

    public List<MinuteStockPrice> getLatestMinutePrices(String stockCode, int limit) {
        validateStockCode(stockCode);
        return stocksFiveMinuteRepository.findLatestFiveMinutePrices(stockCode, limit);
    }

    public List<MinuteStockPrice> getMorePastData(
            String stockCode, Instant beforeTimestamp, int limit) {
        validateStockCode(stockCode);
        validateTimestamp(beforeTimestamp, "과거 데이터 조회를 위한 기준 시점이 필요합니다.");
        return stocksFiveMinuteRepository.findFiveMinutePricesBefore(
                stockCode, beforeTimestamp, limit);
    }

    public List<MinuteStockPrice> getMoreRecentData(
            String stockCode, Instant afterTimestamp, int limit) {
        validateStockCode(stockCode);
        validateTimestamp(afterTimestamp, "최신 데이터 조회를 위한 기준 시점이 필요합니다.");
        return stocksFiveMinuteRepository.findFiveMinutePricesAfter(
                stockCode, afterTimestamp, limit);
    }

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

    private void validateTimestamp(Instant timestamp, String message) {
        if (timestamp == null) {
            throw StockChartException.invalidTimestamp(message);
        }
    }
}
