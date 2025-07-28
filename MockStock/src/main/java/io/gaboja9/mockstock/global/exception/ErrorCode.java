package io.gaboja9.mockstock.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통 에러
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GLOBAL-001", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "GLOBAL-002", "잘못된 입력값입니다."),

    // 사용자 관련 에러
    NOT_FOUNT_MEMBER(HttpStatus.NOT_FOUND, "MEMBER-001", "사용자를 찾을 수 없습니다."),
    UNAUTHORIZED_MEMBER(HttpStatus.UNAUTHORIZED, "MEMBER-002", "인증되지 않은 사용자입니다."),

    // 결제 관련 에러
    PAYMENT_READY_FAILED(HttpStatus.BAD_REQUEST, "PAYMENT-001", "결제 준비 중 오류가 발생했습니다."),
    PAYMENT_APPROVE_FAILED(HttpStatus.BAD_REQUEST, "PAYMENT-002", "결제 승인 중 오류가 발생했습니다."),
    PAYMENT_HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT-003", "결제 내역을 찾을 수 없습니다."),
    PAYMENT_TID_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT-004", "결제 트랜잭션 ID를 찾을 수 없습니다."),
    KAKAOPAY_API_ERROR(HttpStatus.BAD_GATEWAY, "PAYMENT-005", "카카오페이 API 호출 중 오류가 발생했습니다."),
    INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "PAYMENT-006", "잘못된 결제 금액입니다."),
    PAYMENT_ALREADY_PROCESSED(HttpStatus.CONFLICT, "PAYMENT-007", "이미 처리된 결제입니다."),

    // 인증 관련 에러
    JWT_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "JWT-001", "토큰이 만료되었습니다."),
    JWT_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "JWT-002", "유효하지 않은 토큰입니다."),
    JWT_TOKEN_MALFORMED(HttpStatus.UNAUTHORIZED, "JWT-003", "잘못된 형식의 토큰입니다."),
    JWT_SIGNATURE_INVALID(HttpStatus.UNAUTHORIZED, "JWT-004", "토큰 서명이 유효하지 않습니다."),
    JWT_TOKEN_UNSUPPORTED(HttpStatus.UNAUTHORIZED, "JWT-005", "지원되지 않는 토큰입니다."),

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH-001", "이메일 또는 비밀번호가 올바르지 않습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH-002", "이미 존재하는 이메일입니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH-003", "비밀번호가 일치하지 않습니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "AUTH-004", "인증코드가 올바르지 않습니다."),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH-005", "인증코드가 만료되었습니다."),
    WEAK_PASSWORD(HttpStatus.BAD_REQUEST, "AUTH-006", "비밀번호는 최소 8자 이상이며, 영문, 숫자, 특수문자를 포함해야 합니다."),
    AUTH_RESEND_TOO_EARLY(HttpStatus.BAD_REQUEST, "AUTH-007", "인증코드는 {0}초 후에 재발송할 수 있습니다."),
    SOCIAL_LOGIN_REQUIRED(HttpStatus.BAD_REQUEST, "AUTH-008", "{0} 계정이 존재합니다. {0} 로그인을 이용해주세요."),
    SOCIAL_CANNOT_PASSWORD_RESET(
            HttpStatus.BAD_REQUEST, "AUTH-009", "소셜 로그인 계정은 비밀번호 재설정이 불가능합니다."),
    SAME_AS_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "AUTH-010", "새 비밀번호는 현재 비밀번호와 달라야 합니다."),
    CURRENT_PASSWORD_INCORRECT(HttpStatus.BAD_REQUEST, "AUTH-011", "현재 비밀번호가 올바르지 않습니다."),
    NEW_PASSWORD_CONFIRM_MISMATCH(
            HttpStatus.BAD_REQUEST, "AUTH-012", "새 비밀번호와 비밀번호 확인이 일치하지 않습니다."),

    // 주식 관련 에러
    NOT_FOUND_STOCK(HttpStatus.NOT_FOUND, "STOCK-001", "존재하지 않는 주식입니다."),
    INVALID_STOCK_CODE(HttpStatus.BAD_REQUEST, "STOCK-002", "유효하지 않은 주식 코드입니다."),
    // 관심 종목 에러
    NOT_FOUND_FAVORITE(HttpStatus.NOT_FOUND, "FAVORITE-001", "등록되지 않은 관심종목입니다."),
    FAVORITE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "FAVORITE-002", "이미 관심종목으로 등록된 주식입니다."),

    // 구독관련 에러
    INVALID_STOCK(HttpStatus.NOT_FOUND, "STOCK-001", " 없는 주식 코드 입니다."),
    SOCKET_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SOCKET-001", "소켓 에러입니다."),

    // 주문 관련 에러
    INVALID_SELL_QUANTITY(HttpStatus.BAD_REQUEST, "ORDER-001", "매도 수량이 보유 수량보다 많습니다."),
    NOT_ENOUGH_CASH(HttpStatus.BAD_REQUEST, "ORDER-002", "잔액이 부족합니다"),
    ORDER_PROCESSING_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "ORDER-003", "주문 처리 대기 시간이 초과되었습니다."),
    ORDER_PROCESSING_INTERRUPTED(
            HttpStatus.INTERNAL_SERVER_ERROR, "ORDER-004", "주문 처리 중 인터럽트가 발생했습니다."),
    NOT_FOUND_ORDER(HttpStatus.NOT_FOUND, "ORDER-005", "주문을 찾을 수 없습니다."),
    NOT_OPEN_KOREAN_MARKET(HttpStatus.BAD_REQUEST, "ORDER-006", "한국장이 닫혀있습니다."),

    // 포트폴리오 관련 에러
    NOT_FOUND_PORTFOLIO(HttpStatus.NOT_FOUND, "PORTFOLIO-001", "보유한 주식이 없습니다."),

    // 주식 차트 관련 에러
    // 주식 차트 관련 에러
    INVALID_STOCK_CODE_REQUIRED(HttpStatus.BAD_REQUEST, "STOCK-CHART-001", "주식 코드는 필수입니다."),
    INVALID_CHART_LIMIT(
            HttpStatus.BAD_REQUEST, "STOCK-CHART-002", "조회할 데이터 개수는 1개 이상 1000개 이하여야 합니다."),
    INVALID_TIMESTAMP_REQUIRED(HttpStatus.BAD_REQUEST, "STOCK-CHART-003", "기준 시점이 필요합니다."),
    STOCK_DATA_FETCH_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR, "STOCK-CHART-004", "주식 데이터 조회에 실패했습니다."),
    INFLUXDB_CONNECTION_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR, "STOCK-CHART-005", "데이터베이스 연결에 실패했습니다."),

    // 알림 관련 에러
    NOTIFICATION_SETTING_UPDATE_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR, "NOTIFICATION-001", "알림 설정 업데이트에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
