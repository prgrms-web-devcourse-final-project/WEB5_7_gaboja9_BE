package io.gaboja9.mockstock.domain.stock.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;

import io.gaboja9.mockstock.domain.stock.measurement.DailyStockPrice;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class StocksMonthlyRepository {

    private final InfluxDBClient dailyInfluxDBClient;

    @Value("${spring.influx.bucket.daily}")
    private String dailyBucket;

    /**
     * 최근 3년간의 월봉 데이터를 조회합니다.
     *
     * @param stockCode 종목 코드
     * @param limit 조회할 개수
     * @return 월봉 데이터 리스트
     */
    public List<DailyStockPrice> findLatestMonthlyPrices(String stockCode, int limit) {
        String flux =
                String.format(
                        """
                        from(bucket: "%s")
                          |> range(start: -3y)
                          |> filter(fn: (r) => r._measurement == "stock_monthly" and r.stockCode == "%s")
                          |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
                          |> rename(columns: {_time: "timestamp"})
                          |> sort(columns: ["timestamp"], desc: true)
                          |> limit(n: %d)
                        """,
                        dailyBucket, stockCode, limit);
        QueryApi queryApi = dailyInfluxDBClient.getQueryApi();
        return queryApi.query(flux, DailyStockPrice.class);
    }
}
