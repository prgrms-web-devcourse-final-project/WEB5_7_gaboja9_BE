package io.gaboja9.mockstock.domain.stock.repository;


import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import io.gaboja9.mockstock.domain.stock.measurement.DailyStockPrice;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

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

  /**
   * 초기 차트 데이터 로드 (저장된 주봉 데이터 조회) 데이터가 없으면 자동으로 실시간 집계로 전환
   */
  public List<DailyStockPrice> findLatestWeeklyPrices(String stockCode, int limit) {
    // 먼저 저장된 주봉 데이터 조회 시도
    List<DailyStockPrice> weeklyData = findStoredWeeklyPrices(stockCode, limit);

    // 데이터가 없거나 부족하면 실시간 집계
    if (weeklyData.isEmpty()) {
      log.info("No stored weekly data found for {}. Using real-time aggregation.", stockCode);
      return aggregateWeeklyPricesFromDaily(stockCode, limit);
    }

    // 요청한 개수보다 적으면 로그 남기고 반환
    if (weeklyData.size() < limit) {
      log.warn("Found only {} weekly records for {}, requested {}",
          weeklyData.size(), stockCode, limit);
    }

    return weeklyData;
  }

  /**
   * 저장된 주봉 데이터 조회
   */
  private List<DailyStockPrice> findStoredWeeklyPrices(String stockCode, int limit) {
    String flux = String.format("""
        from(bucket: "%s")
          |> range(start: -2y)
          |> filter(fn: (r) => r._measurement == "stock_weekly" and r.stockCode == "%s")
          |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
          |> sort(columns: ["_time"], desc: true)
          |> limit(n: %d)
        """, dailyBucket, stockCode, limit);

    QueryApi queryApi = dailyInfluxDBClient.getQueryApi();
    List<FluxTable> tables = queryApi.query(flux);

    List<DailyStockPrice> results = new ArrayList<>();
    for (FluxTable table : tables) {
      for (FluxRecord record : table.getRecords()) {
        DailyStockPrice price = mapRecordToPrice(record);
        results.add(price);
      }
    }

    return results;
  }

  /**
   * 일봉 데이터에서 실시간으로 주봉 집계 (Fallback)
   */
  private List<DailyStockPrice> aggregateWeeklyPricesFromDaily(String stockCode, int limit) {
    String flux = String.format("""
        from(bucket: "%s")
          |> range(start: -2y)
          |> filter(fn: (r) => r._measurement == "stock_daily" and r.stockCode == "%s")
          |> aggregateWindow(
              every: 1w,
              period: 1w,
              offset: -3d,
              fn: (column, tables=<-) => 
                if column == "openPrice" then tables |> first()
                else if column == "closePrice" then tables |> last()
                else if column == "maxPrice" then tables |> max()
                else if column == "minPrice" then tables |> min()
                else if column == "accumTrans" then tables |> sum()
                else tables |> first(),
              createEmpty: false
          )
          |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
          |> filter(fn: (r) => exists r.openPrice and exists r.closePrice)
          |> sort(columns: ["_time"], desc: true)
          |> limit(n: %d)
        """, dailyBucket, stockCode, limit);

    log.debug("Aggregating weekly prices from daily data for stock: {}", stockCode);

    QueryApi queryApi = dailyInfluxDBClient.getQueryApi();
    List<FluxTable> tables = queryApi.query(flux);

    List<DailyStockPrice> results = new ArrayList<>();
    for (FluxTable table : tables) {
      for (FluxRecord record : table.getRecords()) {
        DailyStockPrice price = mapRecordToPrice(record);
        results.add(price);
      }
    }

    return results;
  }

  /**
   * 차트 왼쪽 드래그시 과거 데이터 로드
   */
  public List<DailyStockPrice> findWeeklyPricesBefore(String stockCode, Instant beforeTimestamp,
      int limit) {
    // 먼저 저장된 데이터 조회
    String flux = String.format("""
        from(bucket: "%s")
          |> range(start: -5y)
          |> filter(fn: (r) => r._measurement == "stock_weekly" and r.stockCode == "%s" and r._time < time(v: "%s"))
          |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
          |> sort(columns: ["_time"], desc: true)
          |> limit(n: %d)
        """, dailyBucket, stockCode, beforeTimestamp.toString(), limit);

    QueryApi queryApi = dailyInfluxDBClient.getQueryApi();
    List<FluxTable> tables = queryApi.query(flux);

    List<DailyStockPrice> results = new ArrayList<>();
    for (FluxTable table : tables) {
      for (FluxRecord record : table.getRecords()) {
        DailyStockPrice price = mapRecordToPrice(record);
        results.add(price);
      }
    }

    // 데이터가 없으면 실시간 집계
    if (results.isEmpty()) {
      return aggregateWeeklyPricesBefore(stockCode, beforeTimestamp, limit);
    }

    return results;
  }

  /**
   * 차트 오른쪽 드래그시 최신 데이터 로드
   */
  public List<DailyStockPrice> findWeeklyPricesAfter(String stockCode, Instant afterTimestamp,
      int limit) {
    String flux = String.format("""
        from(bucket: "%s")
          |> range(start: time(v: "%s"))
          |> filter(fn: (r) => r._measurement == "stock_weekly" and r.stockCode == "%s" and r._time > time(v: "%s"))
          |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
          |> sort(columns: ["_time"], desc: false)
          |> limit(n: %d)
        """, dailyBucket, afterTimestamp.toString(), stockCode, afterTimestamp.toString(), limit);

    QueryApi queryApi = dailyInfluxDBClient.getQueryApi();
    List<FluxTable> tables = queryApi.query(flux);

    List<DailyStockPrice> results = new ArrayList<>();
    for (FluxTable table : tables) {
      for (FluxRecord record : table.getRecords()) {
        DailyStockPrice price = mapRecordToPrice(record);
        results.add(price);
      }
    }

    // 데이터가 없으면 실시간 집계
    if (results.isEmpty()) {
      return aggregateWeeklyPricesAfter(stockCode, afterTimestamp, limit);
    }

    return results;
  }

  /**
   * 주봉 데이터 존재 여부 확인
   */
  public boolean hasWeeklyData(String stockCode) {
    String flux = String.format("""
        from(bucket: "%s")
          |> range(start: -1w)
          |> filter(fn: (r) => r._measurement == "stock_weekly" and r.stockCode == "%s")
          |> limit(n: 1)
          |> count()
        """, dailyBucket, stockCode);

    QueryApi queryApi = dailyInfluxDBClient.getQueryApi();
    List<FluxTable> tables = queryApi.query(flux);

    // 결과가 있으면 데이터 존재
    return !tables.isEmpty() && !tables.get(0).getRecords().isEmpty();
  }

  /**
   * 과거 데이터 실시간 집계
   */
  private List<DailyStockPrice> aggregateWeeklyPricesBefore(String stockCode,
      Instant beforeTimestamp, int limit) {
    String flux = String.format("""
        from(bucket: "%s")
          |> range(start: -5y, stop: time(v: "%s"))
          |> filter(fn: (r) => r._measurement == "stock_daily" and r.stockCode == "%s")
          |> aggregateWindow(
              every: 1w,
              period: 1w,
              offset: -3d,
              fn: (column, tables=<-) => 
                if column == "openPrice" then tables |> first()
                else if column == "closePrice" then tables |> last()
                else if column == "maxPrice" then tables |> max()
                else if column == "minPrice" then tables |> min()
                else if column == "accumTrans" then tables |> sum()
                else tables |> first(),
              createEmpty: false
          )
          |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
          |> filter(fn: (r) => exists r.openPrice and exists r.closePrice)
          |> sort(columns: ["_time"], desc: true)
          |> limit(n: %d)
        """, dailyBucket, beforeTimestamp.toString(), stockCode, limit);

    QueryApi queryApi = dailyInfluxDBClient.getQueryApi();
    List<FluxTable> tables = queryApi.query(flux);

    List<DailyStockPrice> results = new ArrayList<>();
    for (FluxTable table : tables) {
      for (FluxRecord record : table.getRecords()) {
        DailyStockPrice price = mapRecordToPrice(record);
        results.add(price);
      }
    }

    return results;
  }

  /**
   * 최신 데이터 실시간 집계
   */
  private List<DailyStockPrice> aggregateWeeklyPricesAfter(String stockCode, Instant afterTimestamp,
      int limit) {
    String flux = String.format("""
        from(bucket: "%s")
          |> range(start: time(v: "%s"))
          |> filter(fn: (r) => r._measurement == "stock_daily" and r.stockCode == "%s")
          |> aggregateWindow(
              every: 1w,
              period: 1w,
              offset: -3d,
              fn: (column, tables=<-) => 
                if column == "openPrice" then tables |> first()
                else if column == "closePrice" then tables |> last()
                else if column == "maxPrice" then tables |> max()
                else if column == "minPrice" then tables |> min()
                else if column == "accumTrans" then tables |> sum()
                else tables |> first(),
              createEmpty: false
          )
          |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
          |> filter(fn: (r) => exists r.openPrice and exists r.closePrice and r._time > time(v: "%s"))
          |> sort(columns: ["_time"], desc: false)
          |> limit(n: %d)
        """, dailyBucket, afterTimestamp.toString(), stockCode, afterTimestamp.toString(), limit);

    QueryApi queryApi = dailyInfluxDBClient.getQueryApi();
    List<FluxTable> tables = queryApi.query(flux);

    List<DailyStockPrice> results = new ArrayList<>();
    for (FluxTable table : tables) {
      for (FluxRecord record : table.getRecords()) {
        DailyStockPrice price = mapRecordToPrice(record);
        results.add(price);
      }
    }

    return results;
  }

  /**
   * FluxRecord를 DailyStockPrice 객체로 매핑 (주봉도 동일한 구조)
   */
  private DailyStockPrice mapRecordToPrice(FluxRecord record) {
    DailyStockPrice price = new DailyStockPrice();

    price.setTimestamp((Instant) record.getTime());
    price.setStockCode((String) record.getValueByKey("stockCode"));
    price.setOpenPrice(getLong(record, "openPrice"));
    price.setMaxPrice(getLong(record, "maxPrice"));
    price.setMinPrice(getLong(record, "minPrice"));
    price.setClosePrice(getLong(record, "closePrice"));
    price.setAccumTrans(getLong(record, "accumTrans"));

    return price;
  }

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
