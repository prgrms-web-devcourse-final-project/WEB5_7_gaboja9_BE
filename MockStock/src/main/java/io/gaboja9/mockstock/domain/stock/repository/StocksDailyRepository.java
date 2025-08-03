package io.gaboja9.mockstock.domain.stock.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.write.Point;
import io.gaboja9.mockstock.domain.stock.measurement.DailyStockPrice;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class StocksDailyRepository {

  private final InfluxDBClient dailyInfluxDBClient;

  @Value("${spring.influx.bucket.daily}")
  private String dailyBucket;

  public StocksDailyRepository(
      @Qualifier("dailyInfluxDBClient") InfluxDBClient dailyInfluxDBClient) {
    this.dailyInfluxDBClient = dailyInfluxDBClient;
  }

  /*
   * 초기 차트 데이터 로드 (최신 N개)
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
                  |> rename(columns: {_time: "timestamp"})
                  |> sort(columns: ["timestamp"], desc: true)
                  |> limit(n: %d)
                """,
            dailyBucket, stockCode, limit);

    log.debug("Loading latest {} daily prices for stock: {}", limit, stockCode);
    QueryApi queryApi = dailyInfluxDBClient.getQueryApi();
    return queryApi.query(flux, DailyStockPrice.class);
  }

  public List<DailyStockPrice> findDailyPricesBefore(
      String stockCode, Instant beforeTimestamp, int limit) {
    String flux =
        String.format(
            """
                from(bucket: "%s")
                  |> range(start: -3y, stop: time(v: "%s"))
                  |> filter(fn: (r) => r._measurement == "stock_daily" and r.stockCode == "%s")
                  |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
                  |> rename(columns: {_time: "timestamp"})
                  |> sort(columns: ["timestamp"], desc: true)
                  |> limit(n: %d)
                """,
            dailyBucket, beforeTimestamp.toString(), stockCode, limit);

    log.debug(
        "Loading {} daily prices before {} for stock: {}",
        limit,
        beforeTimestamp,
        stockCode);
    QueryApi queryApi = dailyInfluxDBClient.getQueryApi();
    return queryApi.query(flux, DailyStockPrice.class);
  }

  public List<DailyStockPrice> findDailyPricesAfter(
      String stockCode, Instant afterTimestamp, int limit) {
    String flux =
        String.format(
            """
                from(bucket: "%s")
                  |> range(start: time(v: "%s"))
                  |> filter(fn: (r) => r._measurement == "stock_daily" and r.stockCode == "%s" and r._time > time(v: "%s"))
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

    log.debug(
        "Loading {} daily prices after {} for stock: {}", limit, afterTimestamp, stockCode);
    QueryApi queryApi = dailyInfluxDBClient.getQueryApi();
    return queryApi.query(flux, DailyStockPrice.class);
  }

  public void savePoints(List<Point> points) {
    if (points.isEmpty()) {
      log.debug("저장할 데이터 없음");
      return;
    }

    try (WriteApi writeApi = dailyInfluxDBClient.getWriteApi()) {
      writeApi.writePoints(points);  // ✅ writePoints 사용
      log.debug("InfluxDB 저장 완료 - 건수: {}", points.size());
    }

  }
}
