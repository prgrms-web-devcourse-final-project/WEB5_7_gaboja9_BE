package io.gaboja9.mockstock.domain.stock.controller;

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

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.Map;

@Tag(name = "주식 차트 API", description = "주식의 일봉/분봉/주봉 차트 데이터를 제공하는 API")
public interface StockChartControllerSpec {

    // ==================== 일봉 차트 API ====================

    @Operation(
            summary = "일봉 차트 초기 데이터 조회",
            description = "주식의 일봉 차트 초기 로드를 위한 최신 데이터를 조회합니다. 차트 페이지 첫 진입시 사용됩니다.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "일봉 데이터 조회 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = Map.class),
                                        examples =
                                                @ExampleObject(
                                                        name = "일봉 데이터 응답 예시",
                                                        value =
                                                                """
                                                                {
                                                                  "timeframe": "daily",
                                                                  "stockCode": "005930",
                                                                  "dataCount": 10,
                                                                  "data": [
                                                                    {
                                                                      "timestamp": "2025-07-08T00:00:00Z",
                                                                      "stockCode": "005930",
                                                                      "openPrice": 61600,
                                                                      "maxPrice": 62400,
                                                                      "minPrice": 61000,
                                                                      "closePrice": 61400,
                                                                      "accumTrans": 20213724
                                                                    },
                                                                    {
                                                                      "timestamp": "2025-07-07T00:00:00Z",
                                                                      "stockCode": "005930",
                                                                      "openPrice": 62900,
                                                                      "maxPrice": 63300,
                                                                      "minPrice": 61700,
                                                                      "closePrice": 61700,
                                                                      "accumTrans": 17164708
                                                                    }
                                                                  ]
                                                                }
                                                                """))),
                @ApiResponse(
                        responseCode = "400",
                        description = "잘못된 요청 파라미터",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "주식 코드 누락",
                                                    value =
                                                            """
                                                            {
                                                              "error": "주식 코드는 필수입니다. 주식 코드: null",
                                                              "errorCode": "STOCK-CHART-001",
                                                              "timestamp": "2025-07-23T14:00:00Z"
                                                            }
                                                            """),
                                            @ExampleObject(
                                                    name = "잘못된 limit 값",
                                                    value =
                                                            """
                                                            {
                                                              "error": "조회할 데이터 개수는 1개 이상 1000개 이하여야 합니다. 입력값: 1001",
                                                              "errorCode": "STOCK-CHART-002",
                                                              "timestamp": "2025-07-23T14:00:00Z"
                                                            }
                                                            """)
                                        })),
                @ApiResponse(
                        responseCode = "404",
                        description = "존재하지 않는 주식 코드",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        name = "주식 코드 존재하지 않음",
                                                        value =
                                                                """
                                                                {
                                                                  "error": "주식을 찾을 수 없습니다. ID: INVALID123",
                                                                  "errorCode": "STOCK-001",
                                                                  "timestamp": "2025-07-23T14:00:00Z"
                                                                }
                                                                """))),
                @ApiResponse(
                        responseCode = "500",
                        description = "서버 내부 오류",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        name = "데이터 조회 실패",
                                                        value =
                                                                """
                                                                {
                                                                  "error": "주식 데이터 조회에 실패했습니다. 주식 코드: 005930",
                                                                  "errorCode": "STOCK-CHART-004",
                                                                  "timestamp": "2025-07-23T14:00:00Z"
                                                                }
                                                                """)))
            })
    Map<String, Object> getInitialDailyChartData(
            @Parameter(description = "주식 코드 (예: 005930)", required = true, example = "005930")
                    @PathVariable
                    @NotBlank
                    String stockCode,
            @Parameter(description = "조회할 데이터 개수 (1-1000)", example = "100")
                    @RequestParam(defaultValue = "100")
                    @Positive
                    int limit);

    @Operation(
            summary = "일봉 차트 과거 데이터 조회",
            description = "차트 왼쪽 드래그시 과거 데이터를 추가로 로드합니다. 무한 스크롤 방식으로 동작합니다.")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "과거 데이터 조회 성공"),
                @ApiResponse(
                        responseCode = "400",
                        description = "잘못된 요청 파라미터",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        name = "기준 시점 누락",
                                                        value =
                                                                """
                                                                {
                                                                  "error": "과거 데이터 조회를 위한 기준 시점이 필요합니다.",
                                                                  "errorCode": "STOCK-CHART-003",
                                                                  "timestamp": "2025-07-23T14:00:00Z"
                                                                }
                                                                """))),
                @ApiResponse(responseCode = "404", description = "존재하지 않는 주식 코드")
            })
    Map<String, Object> loadPastDailyChartData(
            @Parameter(description = "주식 코드", required = true, example = "005930")
                    @PathVariable
                    @NotBlank
                    String stockCode,
            @Parameter(
                            description = "기준 시점 (이 시점보다 과거 데이터 조회)",
                            required = true,
                            example = "2025-07-07T00:00:00Z")
                    @RequestParam("before")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant beforeTimestamp,
            @Parameter(description = "조회할 데이터 개수", example = "50")
                    @RequestParam(defaultValue = "50")
                    @Positive
                    int limit);

    @Operation(
            summary = "일봉 차트 최신 데이터 조회",
            description = "차트 오른쪽 드래그시 최신 데이터를 추가로 로드합니다. 실시간으로 업데이트된 데이터가 있을 때 사용됩니다.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "최신 데이터 조회 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "데이터가 있는 경우",
                                                    value =
                                                            """
                                                            {
                                                              "timeframe": "daily",
                                                              "stockCode": "005930",
                                                              "dataCount": 2,
                                                              "hasMoreRecent": true,
                                                              "data": [
                                                                {
                                                                  "timestamp": "2025-07-09T00:00:00Z",
                                                                  "stockCode": "005930",
                                                                  "openPrice": 61500,
                                                                  "maxPrice": 62000,
                                                                  "minPrice": 61200,
                                                                  "closePrice": 61800,
                                                                  "accumTrans": 18000000
                                                                }
                                                              ]
                                                            }
                                                            """),
                                            @ExampleObject(
                                                    name = "데이터가 없는 경우 (일반적)",
                                                    value =
                                                            """
                                                            {
                                                              "timeframe": "daily",
                                                              "stockCode": "005930",
                                                              "dataCount": 0,
                                                              "hasMoreRecent": false,
                                                              "data": []
                                                            }
                                                            """)
                                        }))
            })
    Map<String, Object> loadRecentDailyChartData(
            @Parameter(description = "주식 코드", required = true, example = "005930")
                    @PathVariable
                    @NotBlank
                    String stockCode,
            @Parameter(
                            description = "기준 시점 (이 시점보다 미래 데이터 조회)",
                            required = true,
                            example = "2025-07-08T00:00:00Z")
                    @RequestParam("after")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant afterTimestamp,
            @Parameter(description = "조회할 데이터 개수", example = "20")
                    @RequestParam(defaultValue = "20")
                    @Positive
                    int limit);

    // ==================== 분봉 차트 API ====================

    @Operation(
            summary = "분봉 차트 초기 데이터 조회",
            description = "주식의 분봉 차트 초기 로드를 위한 최신 데이터를 조회합니다. 단기 트레이딩 분석에 사용됩니다.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "분봉 데이터 조회 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        name = "분봉 데이터 응답 예시",
                                                        value =
                                                                """
                                                                {
                                                                  "timeframe": "minute",
                                                                  "stockCode": "005930",
                                                                  "dataCount": 200,
                                                                  "data": [
                                                                    {
                                                                      "timestamp": "2025-07-23T13:30:00Z",
                                                                      "stockCode": "005930",
                                                                      "openPrice": 61400,
                                                                      "maxPrice": 61500,
                                                                      "minPrice": 61300,
                                                                      "closePrice": 61450,
                                                                      "accumTrans": 120000
                                                                    }
                                                                  ]
                                                                }
                                                                """)))
            })
    Map<String, Object> getInitialMinuteChartData(
            @Parameter(description = "주식 코드", required = true, example = "005930")
                    @PathVariable
                    @NotBlank
                    String stockCode,
            @Parameter(description = "조회할 데이터 개수 (분봉은 일봉보다 많은 데이터 필요)", example = "200")
                    @RequestParam(defaultValue = "200")
                    @Positive
                    int limit);

    @Operation(summary = "분봉 차트 과거 데이터 조회", description = "분봉 차트에서 왼쪽 드래그시 과거 데이터를 추가로 로드합니다.")
    Map<String, Object> loadPastMinuteChartData(
            @Parameter(description = "주식 코드", required = true, example = "005930")
                    @PathVariable
                    @NotBlank
                    String stockCode,
            @Parameter(description = "기준 시점", required = true, example = "2025-07-23T12:30:00Z")
                    @RequestParam("before")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant beforeTimestamp,
            @Parameter(description = "조회할 데이터 개수", example = "100")
                    @RequestParam(defaultValue = "100")
                    @Positive
                    int limit);

    @Operation(summary = "분봉 차트 최신 데이터 조회", description = "분봉 차트에서 오른쪽 드래그시 최신 데이터를 추가로 로드합니다.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "분봉 최신 데이터 조회 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "데이터가 있는 경우",
                                                    value =
                                                            """
                                                            {
                                                              "timeframe": "minute",
                                                              "stockCode": "005930",
                                                              "dataCount": 5,
                                                              "hasMoreRecent": true,
                                                              "data": [
                                                                {
                                                                  "timestamp": "2025-07-23T14:01:00Z",
                                                                  "stockCode": "005930",
                                                                  "openPrice": 61400,
                                                                  "maxPrice": 61500,
                                                                  "minPrice": 61350,
                                                                  "closePrice": 61450,
                                                                  "accumTrans": 150000
                                                                }
                                                              ]
                                                            }
                                                            """),
                                            @ExampleObject(
                                                    name = "데이터가 없는 경우 (일반적)",
                                                    value =
                                                            """
                                                            {
                                                              "timeframe": "minute",
                                                              "stockCode": "005930",
                                                              "dataCount": 0,
                                                              "hasMoreRecent": false,
                                                              "data": []
                                                            }
                                                            """)
                                        }))
            })
    Map<String, Object> loadRecentMinuteChartData(
            @Parameter(description = "주식 코드", required = true, example = "005930")
                    @PathVariable
                    @NotBlank
                    String stockCode,
            @Parameter(description = "기준 시점", required = true, example = "2025-07-23T13:00:00Z")
                    @RequestParam("after")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant afterTimestamp,
            @Parameter(description = "조회할 데이터 개수", example = "50")
                    @RequestParam(defaultValue = "50")
                    @Positive
                    int limit);

    // ==================== 주봉 차트 API ====================

    @Operation(
            summary = "주봉 차트 초기 데이터 조회",
            description = "주식의 주봉 차트 초기 로드를 위한 최신 데이터를 조회합니다. 중장기 트렌드 분석에 사용됩니다.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "주봉 데이터 조회 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = Map.class),
                                        examples =
                                                @ExampleObject(
                                                        name = "주봉 데이터 응답 예시",
                                                        value =
                                                                """
                                                                {
                                                                  "timeframe": "weekly",
                                                                  "stockCode": "005930",
                                                                  "dataCount": 52,
                                                                  "data": [
                                                                    {
                                                                      "timestamp": "2025-07-21T00:00:00Z",
                                                                      "stockCode": "005930",
                                                                      "openPrice": 61600,
                                                                      "maxPrice": 63300,
                                                                      "minPrice": 61000,
                                                                      "closePrice": 62100,
                                                                      "accumTrans": 85234567
                                                                    },
                                                                    {
                                                                      "timestamp": "2025-07-14T00:00:00Z",
                                                                      "stockCode": "005930",
                                                                      "openPrice": 62900,
                                                                      "maxPrice": 64200,
                                                                      "minPrice": 61700,
                                                                      "closePrice": 61600,
                                                                      "accumTrans": 78543210
                                                                    }
                                                                  ]
                                                                }
                                                                """))),
                @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
                @ApiResponse(responseCode = "404", description = "존재하지 않는 주식 코드")
            })
    Map<String, Object> getInitialWeeklyChartData(
            @Parameter(description = "주식 코드 (예: 005930)", required = true, example = "005930")
                    @PathVariable
                    @NotBlank
                    String stockCode,
            @Parameter(description = "조회할 데이터 개수 (1-260, 기본값 52는 1년치)", example = "52")
                    @RequestParam(defaultValue = "52")
                    @Positive
                    int limit);

    @Operation(summary = "주봉 차트 과거 데이터 조회", description = "차트 왼쪽 드래그시 과거 데이터를 추가로 로드합니다.")
    Map<String, Object> loadPastWeeklyChartData(
            @Parameter(description = "주식 코드", required = true, example = "005930")
                    @PathVariable
                    @NotBlank
                    String stockCode,
            @Parameter(
                            description = "기준 시점 (이 시점보다 과거 데이터 조회)",
                            required = true,
                            example = "2025-07-14T00:00:00Z")
                    @RequestParam("before")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant beforeTimestamp,
            @Parameter(description = "조회할 데이터 개수", example = "26")
                    @RequestParam(defaultValue = "26")
                    @Positive
                    int limit);

    @Operation(summary = "주봉 차트 최신 데이터 조회", description = "차트 오른쪽 드래그시 최신 데이터를 추가로 로드합니다.")
    Map<String, Object> loadRecentWeeklyChartData(
            @Parameter(description = "주식 코드", required = true, example = "005930")
                    @PathVariable
                    @NotBlank
                    String stockCode,
            @Parameter(
                            description = "기준 시점 (이 시점보다 미래 데이터 조회)",
                            required = true,
                            example = "2025-07-21T00:00:00Z")
                    @RequestParam("after")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant afterTimestamp,
            @Parameter(description = "조회할 데이터 개수", example = "10")
                    @RequestParam(defaultValue = "10")
                    @Positive
                    int limit);
}
