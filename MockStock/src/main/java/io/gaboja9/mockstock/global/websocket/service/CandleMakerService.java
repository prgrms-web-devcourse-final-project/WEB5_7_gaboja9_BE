package io.gaboja9.mockstock.global.websocket.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;

import io.gaboja9.mockstock.domain.notifications.scheduler.MarketTimeScheduler;
import io.gaboja9.mockstock.domain.stock.measurement.MinuteStockPrice;
import io.gaboja9.mockstock.global.websocket.dto.StockPriceDto;
import io.gaboja9.mockstock.global.websocket.dto.StocksCandleDto;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class CandleMakerService {

    private final InfluxDBClient minuteInfluxDBClient;
    private final MarketTimeScheduler marketTimeScheduler;

    @Value("${spring.influx.bucket.minute}")
    private String minuteBucket;

    @Value("${spring.influx.org}")
    private String Influxorg;

    public CandleMakerService(
            @Qualifier("minuteInfluxDBClient") InfluxDBClient minuteInfluxDBClient,
            MarketTimeScheduler marketTimeScheduler) {
        this.minuteInfluxDBClient = minuteInfluxDBClient;
        this.marketTimeScheduler = marketTimeScheduler;
    }

    // 각 종목별 현재 분봉 데이터
    private final Map<String, StocksCandleDto> currentCandles = new ConcurrentHashMap<>();

    // 종목별 전용 락 객체 (안전한 동기화)
    private final Map<String, Object> stockLocks = new ConcurrentHashMap<>();

    private Object getStockLock(String stockCode) {
        return stockLocks.computeIfAbsent(stockCode, k -> new Object());
    }

    /** 실시간 체결가로 분봉 만들고 저장 */
    public void processTick(StockPriceDto stockPricedto) {
        String stockCode = stockPricedto.getStockCode();
        long currentTime = System.currentTimeMillis();
        long candleTime = getCandleTime(currentTime);

        // 종목별 전용 락으로 안전한 동기화
        synchronized (getStockLock(stockCode)) {
            StocksCandleDto currentCandle = currentCandles.get(stockCode);

            // 새로운 분봉 시작
            if (currentCandle == null || !currentCandle.getTimestamp().equals(candleTime)) {
                // 이전 분봉 완성되면 저장
                if (currentCandle != null) {
                    saveToInfluxDB(currentCandle);
                }

                // 새 분봉 생성 (빌더 + 정적 팩토리 메서드)
                StocksCandleDto newCandle =
                        StocksCandleDto.createNew(
                                stockCode,
                                candleTime,
                                stockPricedto.getCurrentPrice(),
                                stockPricedto.getTradeVolume());

                currentCandles.put(stockCode, newCandle);
                log.info("새 분봉 시작: {}", newCandle);
            } else {
                // 기존 분봉 업데이트 (불변 객체로 새로 생성)
                StocksCandleDto updatedCandle =
                        currentCandle.updateWith(
                                stockPricedto.getCurrentPrice(), stockPricedto.getTradeVolume());
                currentCandles.put(stockCode, updatedCandle);
            }
        }
    }

    /** 분봉 시간 계산 (분 단위로 정규화) */
    private long getCandleTime(long timestamp) {
        return (timestamp / 60000) * 60000; // 1분(60000ms) 단위로 정규화
    }

    /** InfluxDB에 분봉 저장 */
    private void saveToInfluxDB(StocksCandleDto candle) {
        try {
            MinuteStockPrice minuteData = new MinuteStockPrice();
            minuteData.setTimestamp(candle.getInstant());
            minuteData.setStockCode(candle.getStockCode());
            minuteData.setOpenPrice(candle.getOpen().longValue());
            minuteData.setMaxPrice(candle.getHigh().longValue());
            minuteData.setMinPrice(candle.getLow().longValue());
            minuteData.setClosePrice(candle.getClose().longValue());
            minuteData.setAccumTrans(candle.getVolume());

            // ✅ 수정: 정의된 변수명 사용
            WriteApiBlocking writeApi = minuteInfluxDBClient.getWriteApiBlocking();
            writeApi.writeMeasurement(minuteBucket, Influxorg, WritePrecision.S, minuteData);

            log.info("분봉 저장: {}", candle);

        } catch (Exception e) {
            log.error("분봉 저장 실패: {} at {}", candle.getStockCode(), candle.getInstant(), e);
        }
    }
}
