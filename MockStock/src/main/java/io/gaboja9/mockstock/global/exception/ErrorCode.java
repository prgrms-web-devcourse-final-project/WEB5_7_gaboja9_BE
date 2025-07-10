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
    PAYMENT_ALREADY_PROCESSED(HttpStatus.CONFLICT, "PAYMENT-007", "이미 처리된 결제입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
