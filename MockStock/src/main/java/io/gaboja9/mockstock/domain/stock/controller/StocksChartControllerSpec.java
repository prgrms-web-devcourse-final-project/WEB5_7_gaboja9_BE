package io.gaboja9.mockstock.domain.stock.controller;

import io.gaboja9.mockstock.domain.stock.dto.StocksChartResponse;
import io.gaboja9.mockstock.domain.stock.measurement.DailyStockPrice;
import io.gaboja9.mockstock.domain.stock.measurement.MinuteStockPrice;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "주식 차트 API", description = "주식의 일봉/분봉/주봉 차트 데이터를 제공하는 API")
public interface StocksChartControllerSpec {

  // ==================== 일봉 차트 API ====================

  @Operation(summary = "일봉 차트 초기 데이터 조회", description = "차트 페이지 첫 진입시 사용할 최신 일봉 데이터를 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StocksChartResponse.class), examples = @ExampleObject(name = "성공 예시", value = """
          {"stockCode":"005930"
          ,"data":[{"timestamp":"2025-07-29T00:00:00Z","openPrice":70500,"maxPrice":70600,"minPrice":70400,"closePrice":70600,"accumTrans":25000000}],
          "dataCount":1,
          "timeframe":"daily"}"""))),
      @ApiResponse(responseCode = "404", description = "존재하지 않는 주식 코드", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "에러 예시", value = """
          {"error":"주식을 찾을 수 없습니다. ID: INVALID123","errorCode":"STOCK-001"}""")))
  })
  StocksChartResponse<DailyStockPrice> getInitialDailyChartData(
      @Parameter(description = "종목 코드", required = true) @PathVariable @NotBlank String stockCode,
      @Parameter(description = "조회할 데이터 개수", example = "100") @RequestParam(defaultValue = "100") @Positive int limit);

  @Operation(summary = "일봉 차트 과거 데이터 조회", description = "차트 스크롤 시 특정 시점 이전의 과거 데이터를 추가로 로드합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "과거 데이터 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StocksChartResponse.class)))
  })
  StocksChartResponse<DailyStockPrice> loadPastDailyChartData(
      @Parameter(description = "종목 코드", required = true) @PathVariable @NotBlank String stockCode,
      @Parameter(description = "기준 시점 (이전 데이터 조회)", required = true, example = "2025-07-28T00:00:00Z") @RequestParam("before") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant beforeTimestamp,
      @Parameter(description = "조회할 데이터 개수", example = "50") @RequestParam(defaultValue = "50") @Positive int limit);

  @Operation(summary = "일봉 차트 최신 데이터 조회", description = "실시간으로 누락된 최신 데이터를 추가로 로드합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "최신 데이터 조회 성공", content = @Content(mediaType = "application/json", examples = {
          @ExampleObject(name = "새 데이터가 있는 경우", value = """
              {"stockCode":"005930",
              "data":[{"timestamp":"2025-07-30T00:00:00Z","openPrice":70700,"maxPrice":71000,"minPrice":70600,"closePrice":70900,"accumTrans":26000000}],
              "dataCount":1,
              "timeframe":"daily",
              "hasMoreRecent":true}"""),
          @ExampleObject(name = "새 데이터가 없는 경우", value = """
              {"stockCode":"005930","data":[],"dataCount":0,"timeframe":"daily","hasMoreRecent":false}""")}))
  })
  StocksChartResponse<DailyStockPrice> loadRecentDailyChartData(
      @Parameter(description = "종목 코드", required = true) @PathVariable @NotBlank String stockCode,
      @Parameter(description = "기준 시점 (이후 데이터 조회)", required = true, example = "2025-07-29T00:00:00Z") @RequestParam("after") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant afterTimestamp,
      @Parameter(description = "조회할 데이터 개수", example = "20") @RequestParam(defaultValue = "20") @Positive int limit);

  // ==================== 분봉 차트 API ====================

  @Operation(summary = "분봉 차트 초기 데이터 조회", description = "가장 최근의 분봉 데이터를 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "분봉 데이터 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StocksChartResponse.class),
          examples = @ExampleObject(name = "성공 예시", value = """
              {"stockCode":"005930",
              "data":[{"timestamp":"2025-07-29T06:30:00Z","openPrice":70550,"maxPrice":70600,"minPrice":70550,"closePrice":70600,"accumTrans":5000}],
              "dataCount":1,
              "timeframe":"minute"}"""))),
      @ApiResponse(responseCode = "404", description = "존재하지 않는 주식 코드")})
  StocksChartResponse<MinuteStockPrice> getInitialMinuteChartData(
      @Parameter(description = "종목 코드", required = true) @PathVariable @NotBlank String stockCode,
      @Parameter(description = "조회할 데이터 개수", example = "200") @RequestParam(defaultValue = "200") @Positive int limit);

  @Operation(summary = "분봉 차트 과거 데이터 조회", description = "특정 시점 이전의 과거 분봉 데이터를 추가로 로드합니다.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "과거 데이터 조회 성공")})
  StocksChartResponse<MinuteStockPrice> loadPastMinuteChartData(
      @Parameter(description = "종목 코드", required = true) @PathVariable @NotBlank String stockCode,
      @Parameter(description = "기준 시점", required = true, example = "2025-07-29T12:30:00Z") @RequestParam("before") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant beforeTimestamp,
      @Parameter(description = "조회할 데이터 개수", example = "100") @RequestParam(defaultValue = "100") @Positive int limit);

  @Operation(summary = "분봉 차트 최신 데이터 조회", description = "누락된 최신 분봉 데이터를 추가로 로드합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "최신 데이터 조회 성공", content = @Content(mediaType = "application/json",
          examples = {
              @ExampleObject(name = "새 데이터가 있는 경우", value = """
                  {"stockCode":"005930",
                  "data":[{"timestamp":"2025-07-29T06:31:00Z","openPrice":70550,"maxPrice":70600,"minPrice":70550,"closePrice":70600,"accumTrans":5000}],
                  "dataCount":1,
                  "timeframe":"minute",
                  "hasMoreRecent":true}"""),
              @ExampleObject(name = "새 데이터가 없는 경우", value = """
                  {"stockCode":"005930","data":[],"dataCount":0,"timeframe":"minute","hasMoreRecent":false}""")}))
  })
  StocksChartResponse<MinuteStockPrice> loadRecentMinuteChartData(
      @Parameter(description = "종목 코드", required = true) @PathVariable @NotBlank String stockCode,
      @Parameter(description = "기준 시점", required = true, example = "2025-07-29T13:00:00Z") @RequestParam("after") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant afterTimestamp,
      @Parameter(description = "조회할 데이터 개수", example = "50") @RequestParam(defaultValue = "50") @Positive int limit);

  // ==================== 주봉 차트 API ====================

  @Operation(summary = "주봉 차트 초기 데이터 조회", description = "가장 최근의 주봉 데이터를 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "주봉 데이터 조회 성공", content = @Content(mediaType = "application/json",
          examples = @ExampleObject(name = "성공 예시", value = """
              {"stockCode":"005930",
              "data":[{"timestamp":"2025-07-28T00:00:00Z","openPrice":70100,"maxPrice":71000,"minPrice":70000,"closePrice":70900,"accumTrans":125000000}],
              "dataCount":1,
              "timeframe":"weekly"}"""))),
      @ApiResponse(responseCode = "404", description = "존재하지 않는 주식 코드")})
  StocksChartResponse<DailyStockPrice> getInitialWeeklyChartData(
      @Parameter(description = "종목 코드", required = true) @PathVariable @NotBlank String stockCode,
      @Parameter(description = "조회할 데이터 개수 (52개 = 1년)", example = "52") @RequestParam(defaultValue = "52") @Positive int limit);

  @Operation(summary = "주봉 차트 과거 데이터 조회", description = "특정 시점 이전의 과거 주봉 데이터를 추가로 로드합니다.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "과거 데이터 조회 성공")})
  StocksChartResponse<DailyStockPrice> loadPastWeeklyChartData(
      @Parameter(description = "종목 코드", required = true) @PathVariable @NotBlank String stockCode,
      @Parameter(description = "기준 시점", required = true, example = "2024-07-29T00:00:00Z") @RequestParam("before") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant beforeTimestamp,
      @Parameter(description = "조회할 데이터 개수 (26개 = 6개월)", example = "26") @RequestParam(defaultValue = "26") @Positive int limit);

  @Operation(summary = "주봉 차트 최신 데이터 조회", description = "누락된 최신 주봉 데이터를 추가로 로드합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "최신 데이터 조회 성공", content = @Content(mediaType = "application/json",
          examples = {
              @ExampleObject(name = "새 데이터가 있는 경우", value = """
                  {"stockCode":"005930",
                  "data":[{"timestamp":"2025-08-04T00:00:00Z","openPrice":71000,"maxPrice":71500,"minPrice":70800,"closePrice":71400,"accumTrans":130000000}],
                  "dataCount":1,
                  "timeframe":"weekly",
                  "hasMoreRecent":true}"""),
              @ExampleObject(name = "새 데이터가 없는 경우", value = """
                  {"stockCode":"005930",
                  "data":[],
                  "dataCount":0,
                  "timeframe":"weekly",
                  "hasMoreRecent":false}""")}))
  })
  StocksChartResponse<DailyStockPrice> loadRecentWeeklyChartData(
      @Parameter(description = "종목 코드", required = true) @PathVariable @NotBlank String stockCode,
      @Parameter(description = "기준 시점", required = true, example = "2025-07-28T00:00:00Z") @RequestParam("after") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant afterTimestamp,
      @Parameter(description = "조회할 데이터 개수", example = "10") @RequestParam(defaultValue = "10") @Positive int limit);
}