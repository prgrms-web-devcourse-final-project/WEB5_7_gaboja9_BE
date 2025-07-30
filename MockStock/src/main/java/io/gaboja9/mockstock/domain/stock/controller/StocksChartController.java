package io.gaboja9.mockstock.domain.stock.controller;

import io.gaboja9.mockstock.domain.stock.dto.StocksChartResponse;
import io.gaboja9.mockstock.domain.stock.mapper.StocksChartMapper;
import io.gaboja9.mockstock.domain.stock.measurement.DailyStockPrice;
import io.gaboja9.mockstock.domain.stock.measurement.MinuteStockPrice;
import io.gaboja9.mockstock.domain.stock.service.StocksDailyChartService;
import io.gaboja9.mockstock.domain.stock.service.StocksFiveMinuteChartService;
import io.gaboja9.mockstock.domain.stock.service.StocksMinuteChartService;
import io.gaboja9.mockstock.domain.stock.service.StocksMonthlyChartService;
import io.gaboja9.mockstock.domain.stock.service.StocksWeeklyChartService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/stocks/chart")
public class StocksChartController implements StocksChartControllerSpec {

    private final StocksDailyChartService stocksDailyChartService;
    private final StocksMinuteChartService stocksMinuteChartService;
    private final StocksWeeklyChartService stocksWeeklyChartService;
    private final StocksFiveMinuteChartService stocksFiveMinuteChartService;
    private final StocksMonthlyChartService stocksMonthlyChartService;

    private final StocksChartMapper stocksChartMapper; //  1. 매퍼 주입

    // ==================== 일봉 차트 API ====================

    @GetMapping("/daily/{stockCode}/initial")
    public StocksChartResponse<DailyStockPrice> getInitialDailyChartData(
            @PathVariable String stockCode, @RequestParam(defaultValue = "100") int limit) {

        log.info("Loading initial daily chart data for stock: {}, limit: {}", stockCode, limit);
        List<DailyStockPrice> data = stocksDailyChartService.getLatestDailyPrices(stockCode, limit);
        return stocksChartMapper.toChartResponse(stockCode, data, "daily");
    }

    @Override
    @GetMapping("/daily/{stockCode}/load-past")
    public StocksChartResponse<DailyStockPrice> loadPastDailyChartData(
            @PathVariable String stockCode,
            @RequestParam("before") Instant beforeTimestamp,
            @RequestParam(defaultValue = "50") int limit) {

        log.info(
                "Loading past daily chart data for stock: {} before {}, limit: {}",
                stockCode,
                beforeTimestamp,
                limit);
        List<DailyStockPrice> data =
                stocksDailyChartService.getMorePastData(stockCode, beforeTimestamp, limit);
        return stocksChartMapper.toChartResponse(stockCode, data, "daily");
    }

    @Override
    @GetMapping("/daily/{stockCode}/load-recent")
    public StocksChartResponse<DailyStockPrice> loadRecentDailyChartData(
            @PathVariable String stockCode,
            @RequestParam("after") Instant afterTimestamp,
            @RequestParam(defaultValue = "20") int limit) {

        log.info(
                "Loading recent daily chart data for stock: {} after {}, limit: {}",
                stockCode,
                afterTimestamp,
                limit);
        List<DailyStockPrice> data =
                stocksDailyChartService.getMoreRecentData(stockCode, afterTimestamp, limit);
        return stocksChartMapper.toChartResponse(stockCode, data, "daily", !data.isEmpty());
    }

    // ==================== 분봉 차트 API ====================

    @Override
    @GetMapping("/minute/{stockCode}/initial")
    public StocksChartResponse<MinuteStockPrice> getInitialMinuteChartData(
            @PathVariable String stockCode, @RequestParam(defaultValue = "200") int limit) {

        log.info("Loading initial minute chart data for stock: {}, limit: {}", stockCode, limit);
        List<MinuteStockPrice> data =
                stocksMinuteChartService.getLatestMinutePrices(stockCode, limit);
        return stocksChartMapper.toChartResponse(stockCode, data, "minute");
    }

    @Override
    @GetMapping("/minute/{stockCode}/load-past")
    public StocksChartResponse<MinuteStockPrice> loadPastMinuteChartData(
            @PathVariable String stockCode,
            @RequestParam("before") Instant beforeTimestamp,
            @RequestParam(defaultValue = "100") int limit) {

        log.info(
                "Loading past minute chart data for stock: {} before {}, limit: {}",
                stockCode,
                beforeTimestamp,
                limit);
        List<MinuteStockPrice> data =
                stocksMinuteChartService.getMorePastData(stockCode, beforeTimestamp, limit);
        return stocksChartMapper.toChartResponse(stockCode, data, "minute");
    }

    @Override
    @GetMapping("/minute/{stockCode}/load-recent")
    public StocksChartResponse<MinuteStockPrice> loadRecentMinuteChartData(
            @PathVariable String stockCode,
            @RequestParam("after") Instant afterTimestamp,
            @RequestParam(defaultValue = "50") int limit) {

        log.info(
                "Loading recent minute chart data for stock: {} after {}, limit: {}",
                stockCode,
                afterTimestamp,
                limit);
        List<MinuteStockPrice> data =
                stocksMinuteChartService.getMoreRecentData(stockCode, afterTimestamp, limit);
        return stocksChartMapper.toChartResponse(stockCode, data, "minute", !data.isEmpty());
    }

    // ==================== 주봉 차트 API ====================

    @Override
    @GetMapping("/weekly/{stockCode}/initial")
    public StocksChartResponse<DailyStockPrice> getInitialWeeklyChartData(
            @PathVariable String stockCode, @RequestParam(defaultValue = "52") int limit) {

        log.info("Loading initial weekly chart data for stock: {}, limit: {}", stockCode, limit);
        List<DailyStockPrice> data =
                stocksWeeklyChartService.getLatestWeeklyPrices(stockCode, limit);
        return stocksChartMapper.toChartResponse(stockCode, data, "weekly");
    }

    @Override
    @GetMapping("/weekly/{stockCode}/load-past")
    public StocksChartResponse<DailyStockPrice> loadPastWeeklyChartData(
            @PathVariable String stockCode,
            @RequestParam("before") Instant beforeTimestamp,
            @RequestParam(defaultValue = "26") int limit) {

        log.info(
                "Loading past weekly chart data for stock: {} before {}, limit: {}",
                stockCode,
                beforeTimestamp,
                limit);
        List<DailyStockPrice> data =
                stocksWeeklyChartService.getMorePastData(stockCode, beforeTimestamp, limit);
        return stocksChartMapper.toChartResponse(stockCode, data, "weekly");
    }

    @Override
    @GetMapping("/weekly/{stockCode}/load-recent")
    public StocksChartResponse<DailyStockPrice> loadRecentWeeklyChartData(
            @PathVariable String stockCode,
            @RequestParam("after") Instant afterTimestamp,
            @RequestParam(defaultValue = "10") int limit) {

        log.info(
                "Loading recent weekly chart data for stock: {} after {}, limit: {}",
                stockCode,
                afterTimestamp,
                limit);
        List<DailyStockPrice> data =
                stocksWeeklyChartService.getMoreRecentData(stockCode, afterTimestamp, limit);
        return stocksChartMapper.toChartResponse(stockCode, data, "weekly", !data.isEmpty());
    }

    // ============ 5분봉 조회 ========

    @GetMapping("/5minute/{stockCode}/initial")
    public StocksChartResponse<MinuteStockPrice> getInitialFiveMinuteChartData(
            @PathVariable String stockCode, @RequestParam(defaultValue = "200") int limit) {

        log.info("Loading initial 5minute chart data for stock: {}, limit: {}", stockCode, limit);
        List<MinuteStockPrice> data =
                stocksFiveMinuteChartService.getLatestMinutePrices(stockCode, limit);
        return stocksChartMapper.toChartResponse(stockCode, data, "5minute");
    }

    @GetMapping("/5minute/{stockCode}/load-past")
    public StocksChartResponse<MinuteStockPrice> loadPastFiveMinuteChartData(
            @PathVariable String stockCode,
            @RequestParam("before") Instant beforeTimestamp,
            @RequestParam(defaultValue = "100") int limit) {

        log.info(
                "Loading past 5minute chart data for stock: {} before {}, limit: {}",
                stockCode,
                beforeTimestamp,
                limit);
        List<MinuteStockPrice> data =
                stocksFiveMinuteChartService.getMorePastData(stockCode, beforeTimestamp, limit);
        return stocksChartMapper.toChartResponse(stockCode, data, "5minute");
    }

    @GetMapping("/5minute/{stockCode}/load-recent")
    public StocksChartResponse<MinuteStockPrice> loadRecentFiveMinuteChartData(
            @PathVariable String stockCode,
            @RequestParam("after") Instant afterTimestamp,
            @RequestParam(defaultValue = "50") int limit) {

        log.info(
                "Loading recent 5minute chart data for stock: {} after {}, limit: {}",
                stockCode,
                afterTimestamp,
                limit);
        List<MinuteStockPrice> data =
                stocksFiveMinuteChartService.getMoreRecentData(stockCode, afterTimestamp, limit);
        // 실시간 업데이트는 새로운 데이터가 있는지 여부를 포함하여 응답
        return stocksChartMapper.toChartResponse(stockCode, data, "5minute", !data.isEmpty());
    }

    //  ========================월봉 ====================

    @GetMapping("/monthly/{stockCode}/initial")
    public StocksChartResponse<DailyStockPrice> getInitialMonthlyChartData(
            @PathVariable String stockCode, @RequestParam(defaultValue = "36") int limit) {

        log.info("Loading initial monthly chart data for stock: {}, limit: {}", stockCode, limit);
        List<DailyStockPrice> data =
                stocksMonthlyChartService.getLatestMonthlyPrices(stockCode, limit);
        return stocksChartMapper.toChartResponse(stockCode, data, "monthly");
    }
}
