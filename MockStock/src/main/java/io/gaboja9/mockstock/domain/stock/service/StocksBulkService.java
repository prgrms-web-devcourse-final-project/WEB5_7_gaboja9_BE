package io.gaboja9.mockstock.domain.stock.service;

import io.gaboja9.mockstock.domain.stock.dto.StockResponse;

import jakarta.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StocksBulkService {

    private final StocksDataService stocksDataService;
    private final StocksMinuteService stocksMinuteService;
    private final StocksService stocksService;
    private final ExecutorService executorService; // 병렬 처리를 위한 스레드 풀

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MAX_CONCURRENT_STOCKS = 5; // 동시 처리할 종목 수
    private static final int DELAY_BETWEEN_PERIODS_MS = 500; // 기간간 0.5초 딜레이

    public StocksBulkService(
            StocksDataService stocksDataService,
            StocksMinuteService stocksMinuteService,
            StocksService stocksService) {
        this.stocksDataService = stocksDataService;
        this.stocksMinuteService = stocksMinuteService;
        this.stocksService = stocksService;
        // 서비스 생성 시 고정된 크기의 스레드 풀 생성
        this.executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_STOCKS);
    }

    public void fetchAllStocksAllData(String marketCode) {
        long startTime = System.currentTimeMillis();
        List<StockResponse> allStocks = stocksService.getAllStocks();
        log.info(
                " 전체 주식 데이터 수집 시작 - 총 {}개 종목 ({}개씩 병렬 처리)",
                allStocks.size(),
                MAX_CONCURRENT_STOCKS);

        // 날짜 계산
        LocalDate today = LocalDate.now();
        LocalDate longTermStart = today.minusYears(2).minusMonths(11);
        LocalDate minuteStart = today.minusDays(7);
        String longTermStartStr = longTermStart.format(DATE_FMT);
        String todayStr = today.format(DATE_FMT);
        String minuteStartStr = minuteStart.format(DATE_FMT);

        //  스레드에 안전한 카운터
        AtomicInteger processedCount = new AtomicInteger(0);

        //  각 종목을 비동기 작업으로 제출
        List<CompletableFuture<Void>> futures =
                allStocks.stream()
                        .map(
                                stock ->
                                        CompletableFuture.runAsync(
                                                () ->
                                                        processSingleStock(
                                                                stock,
                                                                marketCode,
                                                                longTermStartStr,
                                                                todayStr,
                                                                minuteStartStr,
                                                                processedCount,
                                                                allStocks.size()),
                                                executorService))
                        .collect(Collectors.toList());

        // 모든 비동기 작업이 완료될 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long endTime = System.currentTimeMillis();
        long durationMinutes = (endTime - startTime) / (1000 * 60);
        log.info("🎉 전체 주식 데이터 수집 완료! 소요시간: {}분", durationMinutes);
    }

    /** 개별 종목의 모든 데이터를 순차적으로 처리하는 메서드 */
    private void processSingleStock(
            StockResponse stock,
            String marketCode,
            String longTermStartStr,
            String todayStr,
            String minuteStartStr,
            AtomicInteger processedCount,
            int totalSize) {

        String stockCode = stock.getStockCode();
        int currentCount = processedCount.incrementAndGet();

        log.info(" [{}/{}] {} 종목 처리 시작", currentCount, totalSize, stockCode);

        try {
            // 1. 월봉 데이터 수집
            fetchMonthlyData(marketCode, stockCode, longTermStartStr, todayStr);
            log.debug("{} 월봉 수집 완료", stockCode);
            sleep(DELAY_BETWEEN_PERIODS_MS);

            // 2. 주봉 데이터 수집
            fetchWeeklyData(marketCode, stockCode, longTermStartStr, todayStr);
            log.debug("{} 주봉 수집 완료", stockCode);
            sleep(DELAY_BETWEEN_PERIODS_MS);

            // 3. 일봉 데이터 수집
            fetchDailyData(marketCode, stockCode, longTermStartStr, todayStr);
            log.debug("{} 일봉 수집 완료", stockCode);
            sleep(DELAY_BETWEEN_PERIODS_MS);

            // 4. 분봉 데이터 수집
            stocksMinuteService.fetchAndSaveLongTermMinuteData(
                    marketCode, stockCode, minuteStartStr, todayStr, "Y");
            log.debug("{} 분봉 수집 완료", stockCode);

            log.info("[{}/{}] {} 종목 처리 완료", currentCount, totalSize, stockCode);

        } catch (Exception e) {
            log.error(
                    "[{}/{}] {} 종목 처리 중 오류 발생: {}",
                    currentCount,
                    totalSize,
                    stockCode,
                    e.getMessage());
        }
    }

    private void fetchMonthlyData(
            String marketCode, String stockCode, String startDate, String endDate) {
        stocksDataService.fetchAndSaveData(marketCode, stockCode, startDate, endDate, "M");
    }

    private void fetchWeeklyData(
            String marketCode, String stockCode, String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate, DATE_FMT);
        LocalDate mid = start.plusWeeks(76);

        stocksDataService.fetchAndSaveData(
                marketCode, stockCode, start.format(DATE_FMT), mid.format(DATE_FMT), "W");
        sleep(DELAY_BETWEEN_PERIODS_MS);
        stocksDataService.fetchAndSaveData(
                marketCode, stockCode, mid.plusDays(1).format(DATE_FMT), endDate, "W");
    }

    private void fetchDailyData(
            String marketCode, String stockCode, String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate, DATE_FMT);
        LocalDate end = LocalDate.parse(endDate, DATE_FMT);
        LocalDate current = start;

        while (current.isBefore(end) || current.isEqual(end)) {
            LocalDate batchEnd = current.plusMonths(3).minusDays(1);
            if (batchEnd.isAfter(end)) {
                batchEnd = end;
            }
            stocksDataService.fetchAndSaveData(
                    marketCode,
                    stockCode,
                    current.format(DATE_FMT),
                    batchEnd.format(DATE_FMT),
                    "D");
            current = batchEnd.plusDays(1);
            if (current.isBefore(end) || current.isEqual(end)) {
                sleep(DELAY_BETWEEN_PERIODS_MS);
            }
        }
    }

    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("작업 중단됨", e);
        }
    }

    // 서비스 종료 시 스레드 풀을 안전하게 종료
    @PreDestroy
    public void cleanup() {
        if (executorService != null) {
            executorService.shutdown();
            log.info("스레드 풀이 안전하게 종료되었습니다.");
        }
    }
}
