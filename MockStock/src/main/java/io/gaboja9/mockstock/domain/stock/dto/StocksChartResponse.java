package io.gaboja9.mockstock.domain.stock.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StocksChartResponse<T> {

  private String stockCode;
  private List<T> data;
  private int dataCount;
  private String timeframe;
  private Boolean hasMoreRecent;

}