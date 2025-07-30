package io.gaboja9.mockstock.domain.stock.service;

import io.gaboja9.mockstock.domain.stock.exception.NotFoundStockException;
import io.gaboja9.mockstock.domain.stock.exception.StockChartException;
import io.gaboja9.mockstock.domain.stock.measurement.DailyStockPrice;
import io.gaboja9.mockstock.domain.stock.repository.StocksMonthlyRepository;
import io.gaboja9.mockstock.domain.stock.repository.StocksRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StocksMonthlyChartService {

  private final StocksRepository stocksRepository;
  private final StocksMonthlyRepository stocksMonthlyRepository;

  public List<DailyStockPrice> getLatestMonthlyPrices(String stockCode, int limit) {
    validateStockCode(stockCode);
    return stocksMonthlyRepository.findLatestMonthlyPrices(stockCode, limit);
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
}