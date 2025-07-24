package io.gaboja9.mockstock.domain.stock.controller;

import io.gaboja9.mockstock.domain.stock.measurement.DailyStockPrice;
import io.gaboja9.mockstock.domain.stock.measurement.MinuteStockPrice;
import io.gaboja9.mockstock.domain.stock.service.StocksDailyChartService;
import io.gaboja9.mockstock.domain.stock.service.StocksMinuteChartService;
import io.gaboja9.mockstock.domain.stock.service.StocksWeeklyChartService;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/stocks/chart")
public class StocksChartController implements StockChartControllerSpec {

  private final StocksDailyChartService stocksDailyChartService;
  private final StocksMinuteChartService stocksMinuteChartService;
  private final StocksWeeklyChartService stocksWeeklyChartService;

  // ==================== 일봉 차트 API ====================

  @Override
  @GetMapping("/daily/{stockCode}/initial")
  public Map<String, Object> getInitialDailyChartData(
      @PathVariable String stockCode,
      @RequestParam(defaultValue = "100") int limit) {

    log.info("Loading initial daily chart data for stock: {}, limit: {}", stockCode, limit);

    List<DailyStockPrice> data = stocksDailyChartService.getLatestDailyPrices(stockCode, limit);

    Map<String, Object> response = new HashMap<>();
    response.put("stockCode", stockCode);
    response.put("data", data);
    response.put("dataCount", data.size());
    response.put("timeframe", "daily");

    return response;
  }

  @Override
  @GetMapping("/daily/{stockCode}/load-past")
  public Map<String, Object> loadPastDailyChartData(
      @PathVariable String stockCode,
      @RequestParam("before") Instant beforeTimestamp,
      @RequestParam(defaultValue = "50") int limit) {

    log.info("Loading past daily chart data for stock: {} before {}, limit: {}",
        stockCode, beforeTimestamp, limit);

    List<DailyStockPrice> data = stocksDailyChartService.getMorePastData(stockCode, beforeTimestamp,
        limit);

    Map<String, Object> response = new HashMap<>();
    response.put("stockCode", stockCode);
    response.put("data", data);
    response.put("dataCount", data.size());
    response.put("timeframe", "daily");

    return response;
  }

  @Override
  @GetMapping("/daily/{stockCode}/load-recent")
  public Map<String, Object> loadRecentDailyChartData(
      @PathVariable String stockCode,
      @RequestParam("after") Instant afterTimestamp,
      @RequestParam(defaultValue = "20") int limit) {

    log.info("Loading recent daily chart data for stock: {} after {}, limit: {}",
        stockCode, afterTimestamp, limit);

    List<DailyStockPrice> data = stocksDailyChartService.getMoreRecentData(stockCode,
        afterTimestamp, limit);

    Map<String, Object> response = new HashMap<>();
    response.put("stockCode", stockCode);
    response.put("data", data);
    response.put("dataCount", data.size());
    response.put("timeframe", "daily");
    response.put("hasMoreRecent", !data.isEmpty());

    return response;
  }

  // ==================== 분봉 차트 API ====================

  @Override
  @GetMapping("/minute/{stockCode}/initial")
  public Map<String, Object> getInitialMinuteChartData(
      @PathVariable String stockCode,
      @RequestParam(defaultValue = "200") int limit) {

    log.info("Loading initial minute chart data for stock: {}, limit: {}", stockCode, limit);

    List<MinuteStockPrice> data = stocksMinuteChartService.getLatestMinutePrices(stockCode, limit);

    Map<String, Object> response = new HashMap<>();
    response.put("stockCode", stockCode);
    response.put("data", data);
    response.put("dataCount", data.size());
    response.put("timeframe", "minute");

    return response;
  }

  @Override
  @GetMapping("/minute/{stockCode}/load-past")
  public Map<String, Object> loadPastMinuteChartData(
      @PathVariable String stockCode,
      @RequestParam("before") Instant beforeTimestamp,
      @RequestParam(defaultValue = "100") int limit) {

    log.info("Loading past minute chart data for stock: {} before {}, limit: {}",
        stockCode, beforeTimestamp, limit);

    List<MinuteStockPrice> data = stocksMinuteChartService.getMorePastData(stockCode,
        beforeTimestamp, limit);

    Map<String, Object> response = new HashMap<>();
    response.put("stockCode", stockCode);
    response.put("data", data);
    response.put("dataCount", data.size());
    response.put("timeframe", "minute");

    return response;
  }

  @Override
  @GetMapping("/minute/{stockCode}/load-recent")
  public Map<String, Object> loadRecentMinuteChartData(
      @PathVariable String stockCode,
      @RequestParam("after") Instant afterTimestamp,
      @RequestParam(defaultValue = "50") int limit) {

    log.info("Loading recent minute chart data for stock: {} after {}, limit: {}",
        stockCode, afterTimestamp, limit);

    List<MinuteStockPrice> data = stocksMinuteChartService.getMoreRecentData(stockCode,
        afterTimestamp, limit);

    Map<String, Object> response = new HashMap<>();
    response.put("stockCode", stockCode);
    response.put("data", data);
    response.put("dataCount", data.size());
    response.put("timeframe", "minute");
    response.put("hasMoreRecent", !data.isEmpty());

    return response;
  }

  // ==================== 주봉 차트 API ====================

  @Override
  @GetMapping("/weekly/{stockCode}/initial")
  public Map<String, Object> getInitialWeeklyChartData(
      @PathVariable String stockCode,
      @RequestParam(defaultValue = "52") int limit) {

    log.info("Loading initial weekly chart data for stock: {}, limit: {}", stockCode, limit);

    List<DailyStockPrice> data = stocksWeeklyChartService.getLatestWeeklyPrices(stockCode, limit);

    Map<String, Object> response = new HashMap<>();
    response.put("stockCode", stockCode);
    response.put("data", data);
    response.put("dataCount", data.size());
    response.put("timeframe", "weekly");

    return response;
  }

  @Override
  @GetMapping("/weekly/{stockCode}/load-past")
  public Map<String, Object> loadPastWeeklyChartData(
      @PathVariable String stockCode,
      @RequestParam("before") Instant beforeTimestamp,
      @RequestParam(defaultValue = "26") int limit) {

    log.info("Loading past weekly chart data for stock: {} before {}, limit: {}",
        stockCode, beforeTimestamp, limit);

    List<DailyStockPrice> data = stocksWeeklyChartService.getMorePastData(stockCode,
        beforeTimestamp, limit);

    Map<String, Object> response = new HashMap<>();
    response.put("stockCode", stockCode);
    response.put("data", data);
    response.put("dataCount", data.size());
    response.put("timeframe", "weekly");

    return response;
  }

  @Override
  @GetMapping("/weekly/{stockCode}/load-recent")
  public Map<String, Object> loadRecentWeeklyChartData(
      @PathVariable String stockCode,
      @RequestParam("after") Instant afterTimestamp,
      @RequestParam(defaultValue = "10") int limit) {

    log.info("Loading recent weekly chart data for stock: {} after {}, limit: {}",
        stockCode, afterTimestamp, limit);

    List<DailyStockPrice> data = stocksWeeklyChartService.getMoreRecentData(stockCode,
        afterTimestamp, limit);

    Map<String, Object> response = new HashMap<>();
    response.put("stockCode", stockCode);
    response.put("data", data);
    response.put("dataCount", data.size());
    response.put("timeframe", "weekly");
    response.put("hasMoreRecent", !data.isEmpty());

    return response;
  }
}