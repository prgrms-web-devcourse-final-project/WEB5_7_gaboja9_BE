package io.gaboja9.mockstock.domain.stock.exception;

import io.gaboja9.mockstock.global.exception.BaseException;
import io.gaboja9.mockstock.global.exception.ErrorCode;

public class StockChartException extends BaseException {

  // 주식 코드 유효성 검증 예외
  public static StockChartException invalidStockCode() {
    return new StockChartException(ErrorCode.INVALID_STOCK_CODE_REQUIRED);
  }

  public static StockChartException invalidStockCode(String stockCode) {
    return new StockChartException(ErrorCode.INVALID_STOCK_CODE_REQUIRED,
        "주식 코드: " + stockCode);  // ErrorCode 메시지 + 값만 추가
  }

  // 차트 데이터 조회 한도 예외
  public static StockChartException invalidLimit() {
    return new StockChartException(ErrorCode.INVALID_CHART_LIMIT);
  }

  public static StockChartException invalidLimit(int limit) {
    return new StockChartException(ErrorCode.INVALID_CHART_LIMIT,
        "입력값: " + limit);  // ErrorCode 메시지 + 값만 추가
  }

  // 필수 시점 정보 누락 예외
  public static StockChartException invalidTimestamp() {
    return new StockChartException(ErrorCode.INVALID_TIMESTAMP_REQUIRED);
  }

  public static StockChartException invalidTimestamp(String message) {
    return new StockChartException(ErrorCode.INVALID_TIMESTAMP_REQUIRED, message);
  }

  // 주식 데이터 조회 실패 예외
  public static StockChartException dataFetchFailed(String stockCode) {
    return new StockChartException(ErrorCode.STOCK_DATA_FETCH_FAILED,
        "주식 코드: " + stockCode);  // ErrorCode 메시지 + 값만 추가
  }

  public static StockChartException dataFetchFailed(String stockCode, Throwable cause) {
    return new StockChartException(ErrorCode.STOCK_DATA_FETCH_FAILED,
        "주식 코드: " + stockCode, cause);  // ErrorCode 메시지 + 값만 추가
  }

  // InfluxDB 연결 실패 예외
  public static StockChartException influxConnectionError(String message) {
    return new StockChartException(ErrorCode.INFLUXDB_CONNECTION_ERROR, message);
  }

  public static StockChartException influxConnectionError(String message, Throwable cause) {
    return new StockChartException(ErrorCode.INFLUXDB_CONNECTION_ERROR, message, cause);
  }

  // 생성자들 (private로 숨김 - 정적 팩토리 메서드만 사용)
  private StockChartException(ErrorCode errorCode) {
    super(errorCode);
  }

  private StockChartException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }

  private StockChartException(ErrorCode errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }
}