package io.gaboja9.mockstock.domain.stock.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import io.gaboja9.mockstock.domain.stock.measurement.MinuteStockPrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Slf4j
@Repository
public class StocksFiveMinuteRepository { // 클래스 이름을 5분봉용으로 명확하게 구분

    private final InfluxDBClient minuteInfluxDBClient;

    // StocksMinuteRepository와 동일한 버킷을 사용
    @Value("${spring.influx.bucket.minute}")
    private String minuteBucket;

    public StocksFiveMinuteRepository(
        @Qualifier("minuteInfluxDBClient") InfluxDBClient minuteInfluxDBClient) {
        this.minuteInfluxDBClient = minuteInfluxDBClient;
    }

    /**
     * 가장 최근의 5분봉 데이터를 조회합니다. (1분봉을 5분봉으로 집계)
     */
    public List<MinuteStockPrice> findLatestFiveMinutePrices(String stockCode, int limit) {
        String flux =
            String.format(
                """
                // 1. 1분봉 데이터 조회
                data_1m = from(bucket: "%s")
                  |> range(start: -30d)
                  |> filter(fn: (r) => r._measurement == "stock_minute" and r.stockCode == "%s")

                // 2. 각 필드별로 5분 단위 집계
                open = data_1m |> filter(fn: (r) => r._field == "openPrice") |> aggregateWindow(every: 5m, fn: first, createEmpty: false)
                high = data_1m |> filter(fn: (r) => r._field == "maxPrice") |> aggregateWindow(every: 5m, fn: max, createEmpty: false)
                low = data_1m |> filter(fn: (r) => r._field == "minPrice") |> aggregateWindow(every: 5m, fn: min, createEmpty: false)
                close = data_1m |> filter(fn: (r) => r._field == "closePrice") |> aggregateWindow(every: 5m, fn: last, createEmpty: false)
                volume = data_1m |> filter(fn: (r) => r._field == "accumTrans") |> aggregateWindow(every: 5m, fn: sum, createEmpty: false)

                // 3. 집계된 데이터들을 하나로 합치고 객체에 매핑
                union(tables: [open, high, low, close, volume])
                  |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
                  |> rename(columns: {_time: "timestamp"})
                  |> sort(columns: ["timestamp"], desc: true)
                  |> limit(n: %d)
                """,
                minuteBucket, stockCode, limit);
        QueryApi queryApi = minuteInfluxDBClient.getQueryApi();
        return queryApi.query(flux, MinuteStockPrice.class);
    }

    /**
     * 특정 시점 이전의 5분봉 데이터를 추가로 조회합니다. (1분봉을 5분봉으로 집계)
     */
    public List<MinuteStockPrice> findFiveMinutePricesBefore(
        String stockCode, Instant beforeTimestamp, int limit) {
        String flux =
            String.format(
                """
                data_1m = from(bucket: "%s")
                  |> range(start: -30d, stop: time(v: "%s"))
                  |> filter(fn: (r) => r._measurement == "stock_minute" and r.stockCode == "%s")

                open = data_1m |> filter(fn: (r) => r._field == "openPrice") |> aggregateWindow(every: 5m, fn: first, createEmpty: false)
                high = data_1m |> filter(fn: (r) => r._field == "maxPrice") |> aggregateWindow(every: 5m, fn: max, createEmpty: false)
                low = data_1m |> filter(fn: (r) => r._field == "minPrice") |> aggregateWindow(every: 5m, fn: min, createEmpty: false)
                close = data_1m |> filter(fn: (r) => r._field == "closePrice") |> aggregateWindow(every: 5m, fn: last, createEmpty: false)
                volume = data_1m |> filter(fn: (r) => r._field == "accumTrans") |> aggregateWindow(every: 5m, fn: sum, createEmpty: false)

                union(tables: [open, high, low, close, volume])
                  |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
                  |> rename(columns: {_time: "timestamp"})
                  |> sort(columns: ["timestamp"], desc: true)
                  |> limit(n: %d)
                """,
                minuteBucket, beforeTimestamp.toString(), stockCode, limit);
        QueryApi queryApi = minuteInfluxDBClient.getQueryApi();
        return queryApi.query(flux, MinuteStockPrice.class);
    }

    /**
     * 특정 시점 이후의 5분봉 데이터를 추가로 조회합니다. (1분봉을 5분봉으로 집계)
     */
    public List<MinuteStockPrice> findFiveMinutePricesAfter(
        String stockCode, Instant afterTimestamp, int limit) {
        String flux =
            String.format(
                """
                import "experimental"

                data_1m = from(bucket: "%s")
                  |> range(start: experimental.subDuration(d: 5m, from: time(v: "%s")))
                  |> filter(fn: (r) => r._measurement == "stock_minute" and r.stockCode == "%s")

                open = data_1m |> filter(fn: (r) => r._field == "openPrice") |> aggregateWindow(every: 5m, fn: first, createEmpty: false)
                high = data_1m |> filter(fn: (r) => r._field == "maxPrice") |> aggregateWindow(every: 5m, fn: max, createEmpty: false)
                low = data_1m |> filter(fn: (r) => r._field == "minPrice") |> aggregateWindow(every: 5m, fn: min, createEmpty: false)
                close = data_1m |> filter(fn: (r) => r._field == "closePrice") |> aggregateWindow(every: 5m, fn: last, createEmpty: false)
                volume = data_1m |> filter(fn: (r) => r._field == "accumTrans") |> aggregateWindow(every: 5m, fn: sum, createEmpty: false)

                union(tables: [open, high, low, close, volume])
                  |> filter(fn: (r) => r._time > time(v: "%s"))
                  |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
                  |> rename(columns: {_time: "timestamp"})
                  |> sort(columns: ["timestamp"], desc: false)
                  |> limit(n: %d)
                """,
                minuteBucket,
                afterTimestamp.toString(),
                stockCode,
                afterTimestamp.toString(),
                limit);
        QueryApi queryApi = minuteInfluxDBClient.getQueryApi();
        return queryApi.query(flux, MinuteStockPrice.class);
    }
}