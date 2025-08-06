package io.gaboja9.mockstock.domain.stock.service;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

import io.gaboja9.mockstock.domain.stock.dto.StockResponse;
import io.gaboja9.mockstock.domain.stock.measurement.MinuteStockPrice;
import io.gaboja9.mockstock.domain.stock.repository.StocksFiveMinuteRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FiveMinuteAggregationService {

    private final StocksFiveMinuteRepository fiveMinuteRepository;
    private final StocksService stocksService;

    /** 모든 종목의 최근 1주일치 데이터를 5분봉으로 집계해서 저장 */
    public void aggregateAllStocksToFiveMinute() {
        new Thread(
                        () -> {
                            try {
                                log.info("전체 종목 5분봉 집계 작업 시작");

                                // 모든 종목 조회
                                List<StockResponse> allStocks = stocksService.getAllStocks();
                                log.info("집계 대상 종목 수: {}", allStocks.size());

                                int totalAggregated = 0;
                                for (StockResponse stock : allStocks) {
                                    try {
                                        int count =
                                                aggregateStockToFiveMinute(stock.getStockCode());
                                        totalAggregated += count;
                                        log.debug("종목 {} 집계 완료: {}개", stock.getStockCode(), count);
                                    } catch (Exception e) {
                                        log.error("종목 {} 집계 중 오류", stock.getStockCode(), e);
                                    }
                                }

                                log.info("전체 종목 5분봉 집계 작업 완료: 총 {}개 데이터 생성", totalAggregated);

                            } catch (Exception e) {
                                log.error("전체 종목 집계 작업 중 오류 발생", e);
                            }
                        })
                .start();
    }

    /** 특정 종목의 1분봉을 5분봉으로 집계해서 저장 */
    private int aggregateStockToFiveMinute(String stockCode) {
        // 1. 리포지토리에서 집계된 데이터 조회
        List<MinuteStockPrice> fiveMinuteData =
                fiveMinuteRepository.getAggregatedFiveMinuteData(stockCode);

        if (fiveMinuteData.isEmpty()) {
            log.warn("집계할 1분봉 데이터가 없습니다 - 종목: {}", stockCode);
            return 0;
        }

        // 2. MinuteStockPrice를 Point로 변환
        List<Point> points =
                fiveMinuteData.stream()
                        .map(
                                data ->
                                        Point.measurement("stock_5minute")
                                                .addTag("stockCode", stockCode)
                                                .addField("openPrice", data.getOpenPrice())
                                                .addField("closePrice", data.getClosePrice())
                                                .addField("maxPrice", data.getMaxPrice())
                                                .addField("minPrice", data.getMinPrice())
                                                .addField("accumTrans", data.getAccumTrans())
                                                .time(data.getTimestamp(), WritePrecision.MS))
                        .toList();

        // 3. 리포지토리에 저장
        fiveMinuteRepository.saveFiveMinuteData(points);

        log.info("5분봉 집계 및 저장 완료 - 종목: {}, {}개 포인트", stockCode, points.size());
        return points.size();
    }
}
