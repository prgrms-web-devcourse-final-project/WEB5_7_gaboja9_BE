package io.gaboja9.mockstock.domain.stock.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;

import io.gaboja9.mockstock.domain.stock.measurement.DailyStockPrice;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class StocksDailyRepository {

    private final InfluxDBClient dailyInfluxDBClient;

    @Value("${spring.influx.bucket.daily}")
    private String dailyBucket;

    // 수동 생성자 - 당신이 보여준 패턴과 동일
    public StocksDailyRepository(
            @Qualifier("dailyInfluxDBClient") InfluxDBClient dailyInfluxDBClient) {
        this.dailyInfluxDBClient = dailyInfluxDBClient;
    }

    /**
     * 초기 차트 데이터 로드 (최신 N개)
     *
     * @param stockCode 주식 코드
     * @param limit 가져올 개수 (기본 50~100개 추천)
     * @return 최신 데이터부터 과거 순으로 정렬된 리스트
     */
    public List<DailyStockPrice> findLatestDailyPrices(String stockCode, int limit) {
        String flux =
                String.format(
                        """
                        from(bucket: "%s")
                          |> range(start: -3y)
                          |> filter(fn: (r) => r._measurement == "stock_daily" and r.stockCode == "%s")
                          |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
                          |> sort(columns: ["_time"], desc: true)
                          |> limit(n: %d)
                        """,
                        dailyBucket, stockCode, limit);

        log.debug("Loading latest {} daily prices for stock: {}", limit, stockCode);

        QueryApi queryApi = dailyInfluxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(flux);

        List<DailyStockPrice> results = new ArrayList<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                DailyStockPrice price = mapRecordToPrice(record);
                results.add(price);
            }
        }

        log.debug(
                "Retrieved {} latest daily price records for stock: {}", results.size(), stockCode);
        return results;
    }

    /**
     * 차트 왼쪽 드래그시 과거 데이터 로드
     *
     * @param stockCode 주식 코드
     * @param beforeTimestamp 이 시점보다 이전 데이터를 가져옴 (exclusive)
     * @param limit 가져올 개수
     * @return 과거 데이터 (시간 내림차순)
     */
    public List<DailyStockPrice> findDailyPricesBefore(
            String stockCode, Instant beforeTimestamp, int limit) {
        String flux =
                String.format(
                        """
                        from(bucket: "%s")
                          |> range(start: -3y)
                          |> filter(fn: (r) => r._measurement == "stock_daily" and r.stockCode == "%s" and r._time < time(v: "%s"))
                          |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
                          |> sort(columns: ["_time"], desc: true)
                          |> limit(n: %d)
                        """,
                        dailyBucket, stockCode, beforeTimestamp.toString(), limit);

        log.debug(
                "Loading {} daily prices before {} for stock: {}",
                limit,
                beforeTimestamp,
                stockCode);

        QueryApi queryApi = dailyInfluxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(flux);

        List<DailyStockPrice> results = new ArrayList<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                DailyStockPrice price = mapRecordToPrice(record);
                results.add(price);
            }
        }

        log.debug(
                "Retrieved {} daily price records before {} for stock: {}",
                results.size(),
                beforeTimestamp,
                stockCode);
        return results;
    }

    /**
     * 차트 오른쪽 드래그시 미래 데이터 로드 (실시간 데이터가 있다면)
     *
     * @param stockCode 주식 코드
     * @param afterTimestamp 이 시점보다 이후 데이터를 가져옴 (exclusive)
     * @param limit 가져올 개수
     * @return 미래 데이터 (시간 오름차순)
     */
    public List<DailyStockPrice> findDailyPricesAfter(
            String stockCode, Instant afterTimestamp, int limit) {
        String flux =
                String.format(
                        """
                        from(bucket: "%s")
                          |> range(start: time(v: "%s"))
                          |> filter(fn: (r) => r._measurement == "stock_daily" and r.stockCode == "%s" and r._time > time(v: "%s"))
                          |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
                          |> sort(columns: ["_time"], desc: false)
                          |> limit(n: %d)
                        """,
                        dailyBucket,
                        afterTimestamp.toString(),
                        stockCode,
                        afterTimestamp.toString(),
                        limit);

        log.debug(
                "Loading {} daily prices after {} for stock: {}", limit, afterTimestamp, stockCode);

        QueryApi queryApi = dailyInfluxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(flux);

        List<DailyStockPrice> results = new ArrayList<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                DailyStockPrice price = mapRecordToPrice(record);
                results.add(price);
            }
        }

        log.debug(
                "Retrieved {} daily price records after {} for stock: {}",
                results.size(),
                afterTimestamp,
                stockCode);
        return results;
    }

    /** FluxRecord를 DailyStockPrice 객체로 매핑 차트용 모든 필드 포함 (OHLC + Volume) */
    private DailyStockPrice mapRecordToPrice(FluxRecord record) {
        DailyStockPrice price = new DailyStockPrice();

        // 시간 정보 (차트의 X축)
        price.setTimestamp((Instant) record.getTime());

        // 주식 코드
        price.setStockCode((String) record.getValueByKey("stockCode"));

        // OHLC 데이터 (캔들스틱 차트용)
        price.setOpenPrice(getLong(record, "openPrice")); // 시가 (Open)
        price.setMaxPrice(getLong(record, "maxPrice")); // 고가 (High)
        price.setMinPrice(getLong(record, "minPrice")); // 저가 (Low)
        price.setClosePrice(getLong(record, "closePrice")); // 종가 (Close)

        // 거래량 데이터 (볼륨 차트용)
        price.setAccumTrans(getLong(record, "accumTrans")); // 누적거래량 (Volume)

        return price;
    }

    /** FluxRecord에서 Long 값을 안전하게 추출 */
    private Long getLong(FluxRecord record, String field) {
        Object value = record.getValueByKey(field);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }
}
