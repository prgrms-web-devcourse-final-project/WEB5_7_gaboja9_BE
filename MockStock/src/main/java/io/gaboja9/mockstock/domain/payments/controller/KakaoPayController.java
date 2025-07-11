package io.gaboja9.mockstock.domain.payments.controller;

import io.gaboja9.mockstock.domain.payments.dto.KakaoPayApproveResponse;
import io.gaboja9.mockstock.domain.payments.dto.KakaoPayReadyResponse;
import io.gaboja9.mockstock.domain.payments.dto.PaymentRequest;
import io.gaboja9.mockstock.domain.payments.dto.PaymentResponse;
import io.gaboja9.mockstock.domain.payments.service.KakaoPayService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class KakaoPayController {
    private final KakaoPayService kakaoPayService;

    @PostMapping("/ready")
    public ResponseEntity<PaymentResponse> paymentReady(@RequestBody PaymentRequest request) {

        try {
            Long memberId = 1L;

            KakaoPayReadyResponse response =
                    kakaoPayService.paymentReady(memberId, request.getChargeAmount());

            return ResponseEntity.ok(PaymentResponse.success("결제 준비 완료", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(PaymentResponse.fail("결제 준비 실패: " + e.getMessage()));
        }
    }

    @GetMapping("/approve")
    public ResponseEntity<String> paymentApprove(@RequestParam("pg_token") String pgToken) {

        try {
            // TODO: JWT 도입시 헤더에서 추출
            Long memberId = 1L;

            KakaoPayApproveResponse response = kakaoPayService.paymentApprove(pgToken, memberId);

            // 성공 페이지로 리다이렉트하거나 결과 반환
            return ResponseEntity.ok("결제 성공");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("결제 실패: " + e.getMessage());
        }
    }

    @GetMapping("/cancel")
    public ResponseEntity<String> paymentCancel(@RequestParam("tid") String tid) {

        try {
            // TODO: JWT 도입시 헤더에서 추출
            Long memberId = 1L;
            kakaoPayService.paymentCancel(tid, memberId);

            return ResponseEntity.ok("결제 취소 성공");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("결제 취소 처리 실패" + e.getMessage());
        }
    }

    @GetMapping("/fail")
    public ResponseEntity<String> paymentFail(@RequestParam("tid") String tid) {

        try {
            Long userId = 1L;
            kakaoPayService.paymentFail(tid, userId);

            return ResponseEntity.ok("결제 실패 처리 성공");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("결제 실패 처리 실패" + e.getMessage());
        }
    }
}
