package io.gaboja9.mockstock.domain.stock.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.write.Point;

import io.gaboja9.mockstock.domain.stock.measurement.MinuteStockPrice;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Slf4j
@Repository
public class StocksFiveMinuteRepository {

    private final InfluxDBClient minuteInfluxDBClient;

    @Value("${spring.influx.bucket.minute}")
    private String minuteBucket;

    @Value("${spring.influx.org}")
    private String influxOrg;

    public StocksFiveMinuteRepository(
            @Qualifier("minuteInfluxDBClient") InfluxDBClient minuteInfluxDBClient) {
        this.minuteInfluxDBClient = minuteInfluxDBClient;
    }

    /**
     * 가장 최근의 5분봉 데이터를 조회합니다.
     *
     * @param stockCode 종목 코드
     * @param limit 조회할 데이터 개수
     * @return 5분봉 데이터 리스트
     */
    public List<MinuteStockPrice> findLatestFiveMinutePrices(String stockCode, int limit) {
        // 5분봉 데이터는 변동성이 크므로 최근 30일 범위로 조회
        String flux =
                String.format(
                        """
                        from(bucket: "%s")
                          |> range(start: -30d)
                          |> filter(fn: (r) => r._measurement == "stock_5minute" and r.stockCode == "%s")
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
     * 특정 시점 이전의 5분봉 데이터를 추가로 조회합니다. (과거 데이터 로딩)
     *
     * @param stockCode 종목 코드
     * @param beforeTimestamp 기준 시점
     * @param limit 조회할 데이터 개수
     * @return 5분봉 데이터 리스트
     */
    public List<MinuteStockPrice> findFiveMinutePricesBefore(
            String stockCode, Instant beforeTimestamp, int limit) {
        String flux =
                String.format(
                        """
                        from(bucket: "%s")
                          |> range(start: -30d, stop: time(v: "%s"))
                          |> filter(fn: (r) => r._measurement == "stock_5minute" and r.stockCode == "%s")
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
     * 특정 시점 이후의 5분봉 데이터를 추가로 조회합니다. (실시간 업데이트)
     *
     * @param stockCode 종목 코드
     * @param afterTimestamp 기준 시점
     * @param limit 조회할 데이터 개수
     * @return 5분봉 데이터 리스트
     */
    public List<MinuteStockPrice> findFiveMinutePricesAfter(
            String stockCode, Instant afterTimestamp, int limit) {
        String flux =
                String.format(
                        """
                        from(bucket: "%s")
                          |> range(start: time(v: "%s"))
                          |> filter(fn: (r) => r._measurement == "stock_5minute" and r.stockCode == "%s" and r._time > time(v: "%s"))
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

    /** 1분봉을 5분봉으로 집계해서 조회 */
    public List<MinuteStockPrice> getAggregatedFiveMinuteData(String stockCode) {
        String flux =
                String.format(
                        """
                        // 1. 1분봉 데이터 조회 (최근 1주일)
                        data_1m = from(bucket: "%s")
                          |> range(start: -7d)
                          |> filter(fn: (r) => r._measurement == "stock_minute" and r.stockCode == "%s")

                        // 2. 각 필드별로 5분 단위 집계
                        open = data_1m |> filter(fn: (r) => r._field == "openPrice") |> aggregateWindow(every: 5m, fn: first, createEmpty: false)
                        high = data_1m |> filter(fn: (r) => r._field == "maxPrice") |> aggregateWindow(every: 5m, fn: max, createEmpty: false)
                        low = data_1m |> filter(fn: (r) => r._field == "minPrice") |> aggregateWindow(every: 5m, fn: min, createEmpty: false)
                        close = data_1m |> filter(fn: (r) => r._field == "closePrice") |> aggregateWindow(every: 5m, fn: last, createEmpty: false)
                        volume = data_1m |> filter(fn: (r) => r._field == "accumTrans") |> aggregateWindow(every: 5m, fn: sum, createEmpty: false)

                        // 3. 집계된 데이터들을 합치고 MinuteStockPrice 형태로 반환
                        union(tables: [open, high, low, close, volume])
                          |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
                          |> rename(columns: {_time: "timestamp"})
                          |> sort(columns: ["timestamp"], desc: false)
                        """,
                        minuteBucket, stockCode);

        QueryApi queryApi = minuteInfluxDBClient.getQueryApi();
        return queryApi.query(flux, MinuteStockPrice.class);
    }

    /** 5분봉 데이터를 InfluxDB에 저장 */
    public void saveFiveMinuteData(List<Point> points) {
        if (points == null || points.isEmpty()) {
            log.warn("저장할 5분봉 데이터가 없습니다.");
            return;
        }

        try (WriteApi writeApi = minuteInfluxDBClient.getWriteApi()) {
            writeApi.writePoints(minuteBucket, influxOrg, points);
            log.debug("5분봉 데이터 {}개 포인트 저장 완료", points.size());
        } catch (Exception e) {
            log.error("5분봉 데이터 저장 중 오류 발생", e);
            throw new RuntimeException("5분봉 데이터 저장 실패", e);
        }
    }
}
