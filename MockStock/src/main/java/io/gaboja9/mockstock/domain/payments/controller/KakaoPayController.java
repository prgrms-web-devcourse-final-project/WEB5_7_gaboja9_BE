package io.gaboja9.mockstock.domain.payments.controller;

import io.gaboja9.mockstock.domain.auth.dto.MembersDetails;
import io.gaboja9.mockstock.domain.payments.dto.KakaoPayApproveResponse;
import io.gaboja9.mockstock.domain.payments.dto.KakaoPayReadyResponse;
import io.gaboja9.mockstock.domain.payments.dto.PaymentRequest;
import io.gaboja9.mockstock.domain.payments.dto.PaymentResponse;
import io.gaboja9.mockstock.domain.payments.service.KakaoPayService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class KakaoPayController implements KakaoPayControllerSpec {
    private final KakaoPayService kakaoPayService;

    @PostMapping("/ready")
    public ResponseEntity<PaymentResponse> paymentReady(
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal MembersDetails membersDetails) {
        try {
            KakaoPayReadyResponse response =
                    kakaoPayService.paymentReady(membersDetails.getId(), request.getChargeAmount());

            return ResponseEntity.ok(PaymentResponse.success("결제 준비 완료", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(PaymentResponse.fail("결제 준비 실패: " + e.getMessage()));
        }
    }

    @GetMapping("/approve")
    public ResponseEntity<PaymentResponse> paymentApprove(
            @RequestParam("pg_token") String pgToken, @RequestParam("member_id") Long memberId) {
        try {
            KakaoPayApproveResponse response = kakaoPayService.paymentApprove(pgToken, memberId);

            return ResponseEntity.ok(PaymentResponse.success("결제 승인 완료", response));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(PaymentResponse.fail("결제 승인 실패: " + e.getMessage()));
        }
    }

    @GetMapping("/cancel")
    public ResponseEntity<PaymentResponse> paymentCancel(
            @RequestParam("tid") String tid, @RequestParam("member_id") Long memberId) {
        try {
            kakaoPayService.paymentCancel(tid, memberId);

            return ResponseEntity.ok(PaymentResponse.success("결제 취소 완료", null));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(PaymentResponse.fail("결제 취소 실패: " + e.getMessage()));
        }
    }

    @GetMapping("/fail")
    public ResponseEntity<PaymentResponse> paymentFail(
            @RequestParam("tid") String tid, @RequestParam("member_id") Long memberId) {
        try {
            kakaoPayService.paymentFail(tid, memberId);

            return ResponseEntity.ok(PaymentResponse.success("결제 실패 처리 완료", null));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(PaymentResponse.fail("결제 실패 처리 실패: " + e.getMessage()));
        }
    }
}
