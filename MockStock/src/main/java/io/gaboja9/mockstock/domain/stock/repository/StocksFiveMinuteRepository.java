package io.gaboja9.mockstock.domain.stock.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import io.gaboja9.mockstock.domain.stock.measurement.MinuteStockPrice;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;


@Slf4j
@Repository
public class StocksFiveMinuteRepository {

  private final InfluxDBClient minuteInfluxDBClient;

  @Value("${spring.influx.bucket.minute}")
  private String minuteBucket;

  public StocksFiveMinuteRepository(
      @Qualifier("minuteInfluxDBClient") InfluxDBClient minuteInfluxDBClient) {
    this.minuteInfluxDBClient = minuteInfluxDBClient;
  }

  /**
   * 가장 최근의 5분봉 데이터를 조회합니다.
   *
   * @param stockCode 종목 코드
   * @param limit     조회할 데이터 개수
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
   * @param stockCode       종목 코드
   * @param beforeTimestamp 기준 시점
   * @param limit           조회할 데이터 개수
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
   * @param stockCode      종목 코드
   * @param afterTimestamp 기준 시점
   * @param limit          조회할 데이터 개수
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
            minuteBucket, afterTimestamp.toString(), stockCode, afterTimestamp.toString(), limit);
    QueryApi queryApi = minuteInfluxDBClient.getQueryApi();
    return queryApi.query(flux, MinuteStockPrice.class);
  }
}
