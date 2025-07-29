package io.gaboja9.mockstock.domain.stock.mapper;

import io.gaboja9.mockstock.domain.stock.dto.StocksChartResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component // Spring 컨테이너에 Bean으로 등록
public class StocksChartMapper {

  // Daily 차트 응답 DTO로 변환
  public <T> StocksChartResponse<T> toChartResponse(String stockCode, List<T> data,
      String timeframe) {
    return StocksChartResponse.<T>builder()
        .stockCode(stockCode)
        .data(data)
        .dataCount(data.size())
        .timeframe(timeframe)
        .build();
  }

  // hasMoreRecent 플래그가 포함된 응답 DTO로 변환
  public <T> StocksChartResponse<T> toChartResponse(String stockCode, List<T> data,
      String timeframe, boolean hasMoreRecent) {
    return StocksChartResponse.<T>builder()
        .stockCode(stockCode)
        .data(data)
        .dataCount(data.size())
        .timeframe(timeframe)
        .hasMoreRecent(hasMoreRecent)
        .build();
  }
}