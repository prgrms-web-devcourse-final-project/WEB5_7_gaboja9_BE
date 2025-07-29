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
public class StocksMinuteRepository {

  private final InfluxDBClient minuteInfluxDBClient;

  @Value("${spring.influx.bucket.minute}")
  private String minuteBucket;

  public StocksMinuteRepository(
      @Qualifier("minuteInfluxDBClient") InfluxDBClient minuteInfluxDBClient) {
    this.minuteInfluxDBClient = minuteInfluxDBClient;
  }

  public List<MinuteStockPrice> findLatestMinutePrices(String stockCode, int limit) {
    String flux = String.format(
        """
            from(bucket: "%s")
              |> range(start: -7d)
              |> filter(fn: (r) => r._measurement == "stock_minute" and r.stockCode == "%s")
              |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
              |> rename(columns: {_time: "timestamp"})
              |> sort(columns: ["timestamp"], desc: true)
              |> limit(n: %d)
            """,
        minuteBucket, stockCode, limit);

    log.debug("Loading latest {} minute prices for stock: {}", limit, stockCode);
    QueryApi queryApi = minuteInfluxDBClient.getQueryApi();
    return queryApi.query(flux, MinuteStockPrice.class);
  }

  public List<MinuteStockPrice> findMinutePricesBefore(String stockCode, Instant beforeTimestamp,
      int limit) {
    String flux = String.format(
        """
            from(bucket: "%s")
              |> range(start: -7d, stop: time(v: "%s"))
              |> filter(fn: (r) => r._measurement == "stock_minute" and r.stockCode == "%s")
              |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
              |> rename(columns: {_time: "timestamp"})
              |> sort(columns: ["timestamp"], desc: true)
              |> limit(n: %d)
            """,
        minuteBucket, beforeTimestamp.toString(), stockCode, limit);

    log.debug("Loading {} minute prices before {} for stock: {}", limit, beforeTimestamp,
        stockCode);
    QueryApi queryApi = minuteInfluxDBClient.getQueryApi();
    return queryApi.query(flux, MinuteStockPrice.class);
  }

  public List<MinuteStockPrice> findMinutePricesAfter(String stockCode, Instant afterTimestamp,
      int limit) {
    String flux = String.format(
        """
            from(bucket: "%s")
              |> range(start: time(v: "%s"))
              |> filter(fn: (r) => r._measurement == "stock_minute" and r.stockCode == "%s" and r._time > time(v: "%s"))
              |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
              |> rename(columns: {_time: "timestamp"})
              |> sort(columns: ["timestamp"], desc: false)
              |> limit(n: %d)
            """,
        minuteBucket, afterTimestamp.toString(), stockCode, afterTimestamp.toString(), limit);

    log.debug("Loading {} minute prices after {} for stock: {}", limit, afterTimestamp, stockCode);
    QueryApi queryApi = minuteInfluxDBClient.getQueryApi();
    return queryApi.query(flux, MinuteStockPrice.class);
  }
}

