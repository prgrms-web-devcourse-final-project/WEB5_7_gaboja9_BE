package io.gaboja9.mockstock.domain.stock.controller;

import io.gaboja9.mockstock.domain.stock.dto.StockResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "주식 관리", description = "주식 관련 API")
public interface StocksControllerSpec {

  @Operation(
      summary = "전체 주식 목록 조회",
      description = "등록된 모든 주식의 목록을 조회합니다.",
      tags = {"주식 관리"})
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "주식 목록 조회 성공",
          content =
          @Content(
              mediaType = "application/json",
              array =
              @ArraySchema(
                  schema =
                  @Schema(
                      implementation =
                          StockResponse.class)),
              examples =
              @ExampleObject(
                  name = "주식 목록 조회 성공",
                  value =
                      """
                          [
                            {
                              "stockCode": "005930",
                              "stockName": "삼성전자"
                            },
                            {
                              "stockCode": "035420",
                              "stockName": "NAVER"
                            },
                            {
                              "stockCode": "035720",
                              "stockName": "카카오"
                            },
                            {
                              "stockCode": "259960",
                              "stockName": "크래프톤"
                            },
                            {
                              "stockCode": "068270",
                              "stockName": "셀트리온"
                            }
                          ]
                          """))),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ApiResponse.class),
              examples =
              @ExampleObject(
                  name = "서버 오류",
                  value =
                      """
                          {
                            "success": false,
                            "errorCode": "INTERNAL_SERVER_ERROR",
                            "message": "서버 내부 오류가 발생했습니다.",
                            "data": null
                          }
                          """)))
  })
  ResponseEntity<List<StockResponse>> getAllStocks();

  @Operation(
      summary = "장기간 일/주/월봉 데이터 대량 수집",
      description =
          """
              지정된 기간의 일/주/월봉 데이터를 100일 단위로 분할하여 대량 수집합니다.

              **주요 특징:**
              - 100일 단위 자동 분할 처리
              - API 제한 회피를 위한 1초 딜레이 적용
              - 배치별 성공/실패 로깅
              - 중간 실패 시에도 나머지 배치 계속 진행

              **사용 예시:**
              - 3년치 데이터: 약 11개 배치, 12초 소요
              - 1년치 데이터: 약 4개 배치, 5초 소요
              """,
      tags = {"주식 데이터 수집"})
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "일봉 데이터 수집 완료",
          content =
          @Content(
              mediaType = "text/plain",
              examples =
              @ExampleObject(
                  name = "수집 성공",
                  value =
                      "장기간 일봉 데이터 수집 완료 - 총 배치: 11개, 성공: 11개, 실패:"
                          + " 0개"))),
      @ApiResponse(
          responseCode = "500",
          description = "데이터 수집 실패",
          content =
          @Content(
              mediaType = "text/plain",
              examples =
              @ExampleObject(
                  name = "수집 실패",
                  value = "장기간 일봉 데이터 수집 실패: 토큰 발급 실패")))
  })
  ResponseEntity<?> fetchLongTermDailyStockData(
      @Parameter(description = "시장 구분 코드 (J: KRX)", example = "J")
      @RequestParam(defaultValue = "J")
      String marketCode,
      @Parameter(description = "종목 코드", example = "005930", required = true) @RequestParam
      String stockCode,
      @Parameter(description = "시작 날짜 (yyyyMMdd 형식)", example = "20220101", required = true)
      @RequestParam
      String startDate,
      @Parameter(description = "종료 날짜 (yyyyMMdd 형식)", example = "20241231", required = true)
      @RequestParam
      String endDate,
      @Parameter(description = "기간 구분 코드 (D: 일봉, W: 주봉, M: 월봉)", example = "D")
      @RequestParam(defaultValue = "D")
      String periodCode);

  @Operation(
      summary = "장기간 분봉 데이터 대량 수집",
      description =
          """
              지정된 기간의 분봉 데이터를 날짜별 시간대별로 분할하여 수집합니다.

              **처리 방식:**
              - 하루 4개 시간대로 분할: 09:00-11:00, 11:00-13:00, 13:00-15:00, 15:00-15:30
              - 주말 자동 제외 (토요일, 일요일 스킵)
              - 각 배치당 1초 딜레이 적용

              **예상 배치 수:**
              - 1주일 (5일): 20개 배치
              - 1개월 (약 22일): 88개 배치

              **주의사항:**
              - 분봉 데이터는 당일부터 과거 데이터까지 수집 가능
              - 미래 날짜 요청 시 해당 날짜는 데이터 없음으로 처리
              """,
      tags = {"주식 데이터 수집"})
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "분봉 데이터 수집 완료",
          content =
          @Content(
              mediaType = "text/plain",
              examples =
              @ExampleObject(
                  name = "수집 성공",
                  value =
                      "장기간 분봉 데이터 수집 완료 - 총 배치: 20개, 성공: 20개, 실패:"
                          + " 0개"))),
      @ApiResponse(
          responseCode = "500",
          description = "분봉 데이터 수집 실패",
          content =
          @Content(
              mediaType = "text/plain",
              examples =
              @ExampleObject(
                  name = "수집 실패",
                  value = "장기간 분봉 데이터 수집 실패: API 호출 한도 초과")))
  })
  ResponseEntity<?> fetchLongTermMinuteStockData(
      @Parameter(description = "시장 구분 코드 (J: KRX)", example = "J")
      @RequestParam(defaultValue = "J")
      String marketCode,
      @Parameter(description = "종목 코드", example = "035420", required = true) @RequestParam
      String stockCode,
      @Parameter(description = "시작 날짜 (yyyyMMdd 형식)", example = "20240722", required = true)
      @RequestParam
      String startDate,
      @Parameter(description = "종료 날짜 (yyyyMMdd 형식)", example = "20240726", required = true)
      @RequestParam
      String endDate,
      @Parameter(description = "과거 데이터 포함 여부 (Y: 포함, N: 미포함)", example = "Y")
      @RequestParam(defaultValue = "Y")
      String includePastData);

  @Operation(
      summary = "전체 주식 데이터 일괄 수집",
      description =
          """
          등록된 모든 주식의 과거 데이터를 일괄적으로 수집합니다.
          
          **수집 데이터:**
          - 일봉 데이터: 2년 11개월 (약 1,065일)
          - 주봉 데이터: 2년 11개월 (약 152주)  
          - 월봉 데이터: 2년 11개월 (35개월)
          - 분봉 데이터: 최근 7일
          
          **처리 방식:**
          - 종목별 순차 처리 (동시 처리 없음)
          - 종목간 1.5초 딜레이, 기간간 0.8초 딜레이
          - 개별 종목/기간 실패가 전체 작업 중단시키지 않음
          - 백그라운드에서 실행되어 즉시 응답 반환
          
          **예상 소요시간:**
          - 20개 종목 기준: 약 1.5-2시간
          - 종목당 평균 4-5분 (일/주/월/분봉 포함)
          
          **모니터링:**
          - 실시간 진행상황: 서버 로그 확인
          - 개별 종목별 성공/실패 상태 로깅
          - 최종 완료시 전체 통계 출력
          """,
      tags = {"주식 데이터 수집"})
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "데이터 수집 작업 시작됨",
          content = @Content(
              mediaType = "text/plain",
              examples = @ExampleObject(
                  name = "작업 시작 성공",
                  value = "데이터 수집 작업 시작됨"
              )
          )
      ),
      @ApiResponse(
          responseCode = "500",
          description = "데이터 수집 시작 실패",
          content = @Content(
              mediaType = "text/plain",
              examples = @ExampleObject(
                  name = "시작 실패",
                  value = "데이터 수집 시작 실패"
              )
          )
      )
  })
  ResponseEntity<String> fetchAllStocksAllData(
      @Parameter(
          description = "시장 구분 코드 (J: KRX)",
          example = "J"
      )
      @RequestParam(defaultValue = "J") String marketCode
  );
}
