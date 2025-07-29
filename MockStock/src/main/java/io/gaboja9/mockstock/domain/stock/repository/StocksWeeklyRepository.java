package io.gaboja9.mockstock.domain.stock.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;

import io.gaboja9.mockstock.domain.stock.measurement.DailyStockPrice;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Slf4j
@Repository
public class StocksWeeklyRepository {

    private final InfluxDBClient dailyInfluxDBClient;

    @Value("${spring.influx.bucket.daily}")
    private String dailyBucket;

    public StocksWeeklyRepository(
            @Qualifier("dailyInfluxDBClient") InfluxDBClient dailyInfluxDBClient) {
        this.dailyInfluxDBClient = dailyInfluxDBClient;
    }

    // ==================== 저장된 주봉(stock_weekly) 데이터 조회 ====================

    public List<DailyStockPrice> findStoredWeeklyPrices(String stockCode, int limit) {
        String flux =
                String.format(
                        """
                        from(bucket: "%s")
                          |> range(start: -5y)
                          |> filter(fn: (r) => r._measurement == "stock_weekly" and r.stockCode == "%s")
                          |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
                          |> rename(columns: {_time: "timestamp"})
                          |> sort(columns: ["timestamp"], desc: true)
                          |> limit(n: %d)
                        """,
                        dailyBucket, stockCode, limit);
        QueryApi queryApi = dailyInfluxDBClient.getQueryApi();
        return queryApi.query(flux, DailyStockPrice.class);
    }

    public List<DailyStockPrice> findStoredWeeklyPricesBefore(
            String stockCode, Instant beforeTimestamp, int limit) {
        String flux =
                String.format(
                        """
                        from(bucket: "%s")
                          |> range(start: -10y, stop: time(v: "%s"))
                          |> filter(fn: (r) => r._measurement == "stock_weekly" and r.stockCode == "%s")
                          |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
                          |> rename(columns: {_time: "timestamp"})
                          |> sort(columns: ["timestamp"], desc: true)
                          |> limit(n: %d)
                        """,
                        dailyBucket, beforeTimestamp.toString(), stockCode, limit);
        QueryApi queryApi = dailyInfluxDBClient.getQueryApi();
        return queryApi.query(flux, DailyStockPrice.class);
    }

    public List<DailyStockPrice> findStoredWeeklyPricesAfter(
            String stockCode, Instant afterTimestamp, int limit) {
        String flux =
                String.format(
                        """
                        from(bucket: "%s")
                          |> range(start: time(v: "%s"))
                          |> filter(fn: (r) => r._measurement == "stock_weekly" and r.stockCode == "%s" and r._time > time(v: "%s"))
                          |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
                          |> rename(columns: {_time: "timestamp"})
                          |> sort(columns: ["timestamp"], desc: false)
                          |> limit(n: %d)
                        """,
                        dailyBucket,
                        afterTimestamp.toString(),
                        stockCode,
                        afterTimestamp.toString(),
                        limit);
        QueryApi queryApi = dailyInfluxDBClient.getQueryApi();
        return queryApi.query(flux, DailyStockPrice.class);
    }

    // ==================== 일봉(stock_daily)에서 실시간 집계 ====================

    public List<DailyStockPrice> aggregateFromDaily(String stockCode, int limit) {
        String flux =
                String.format(
                        """
                        from(bucket: "%s")
                          |> range(start: -5y)
                          |> filter(fn: (r) => r._measurement == "stock_daily" and r.stockCode == "%s")
                          |> aggregateWindow(every: 1w, period: 1w, offset: -3d, fn: (column, tables=<-) => if column == "openPrice" then tables |> first() else if column == "closePrice" then tables |> last() else if column == "maxPrice" then tables |> max() else if column == "minPrice" then tables |> min() else if column == "accumTrans" then tables |> sum() else tables |> first(), createEmpty: false)
                          |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
                          |> filter(fn: (r) => exists r.openPrice and exists r.closePrice)
                          |> rename(columns: {_time: "timestamp"})
                          |> sort(columns: ["timestamp"], desc: true)
                          |> limit(n: %d)
                        """,
                        dailyBucket, stockCode, limit);
        QueryApi queryApi = dailyInfluxDBClient.getQueryApi();
        return queryApi.query(flux, DailyStockPrice.class);
    }

    public List<DailyStockPrice> aggregateFromDailyBefore(
            String stockCode, Instant beforeTimestamp, int limit) {
        String flux =
                String.format(
                        """
                        from(bucket: "%s")
                          |> range(start: -10y, stop: time(v: "%s"))
                          |> filter(fn: (r) => r._measurement == "stock_daily" and r.stockCode == "%s")
                          |> aggregateWindow(every: 1w, period: 1w, offset: -3d, fn: (column, tables=<-) => if column == "openPrice" then tables |> first() else if column == "closePrice" then tables |> last() else if column == "maxPrice" then tables |> max() else if column == "minPrice" then tables |> min() else if column == "accumTrans" then tables |> sum() else tables |> first(), createEmpty: false)
                          |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
                          |> filter(fn: (r) => exists r.openPrice and exists r.closePrice)
                          |> rename(columns: {_time: "timestamp"})
                          |> sort(columns: ["timestamp"], desc: true)
                          |> limit(n: %d)
                        """,
                        dailyBucket, beforeTimestamp.toString(), stockCode, limit);
        QueryApi queryApi = dailyInfluxDBClient.getQueryApi();
        return queryApi.query(flux, DailyStockPrice.class);
    }

    public List<DailyStockPrice> aggregateFromDailyAfter(
            String stockCode, Instant afterTimestamp, int limit) {
        String flux =
                String.format(
                        """
                        from(bucket: "%s")
                          |> range(start: time(v: "%s"))
                          |> filter(fn: (r) => r._measurement == "stock_daily" and r.stockCode == "%s")
                          |> aggregateWindow(every: 1w, period: 1w, offset: -3d, fn: (column, tables=<-) => if column == "openPrice" then tables |> first() else if column == "closePrice" then tables |> last() else if column == "maxPrice" then tables |> max() else if column == "minPrice" then tables |> min() else if column == "accumTrans" then tables |> sum() else tables |> first(), createEmpty: false)
                          |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
                          |> filter(fn: (r) => exists r.openPrice and exists r.closePrice and r._time > time(v: "%s"))
                          |> rename(columns: {_time: "timestamp"})
                          |> sort(columns: ["timestamp"], desc: false)
                          |> limit(n: %d)
                        """,
                        dailyBucket,
                        afterTimestamp.toString(),
                        stockCode,
                        afterTimestamp.toString(),
                        limit);
        QueryApi queryApi = dailyInfluxDBClient.getQueryApi();
        return queryApi.query(flux, DailyStockPrice.class);
    }
}
