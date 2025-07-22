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

    // 주식 관련 에러
    NOT_FOUNT_STOCK(HttpStatus.NOT_FOUND, "STOCK-001", "존재하지 않는 주식입니다."),
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
    ORDER_PROCESSING_INTERRUPTED(HttpStatus.INTERNAL_SERVER_ERROR, "ORDER-004", "주문 처리 중 인터럽트가 발생했습니다."),
    NOT_FOUND_ORDER(HttpStatus.NOT_FOUND, "ORDER-005", "주문을 찾을 수 없습니다."),
    NOT_OPEN_KOREAN_MARKET(HttpStatus.BAD_REQUEST, "ORDER-006", "한국장이 닫혀있습니다."),

    // 포트폴리오 관련 에러
    NOT_FOUND_PORTFOLIO(HttpStatus.NOT_FOUND, "PORTFOLIO-001", "보유한 주식이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
