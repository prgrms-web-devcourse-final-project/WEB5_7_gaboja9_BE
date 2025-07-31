package io.gaboja9.mockstock.domain.payments.service;

import io.gaboja9.mockstock.domain.members.entity.Members;
import io.gaboja9.mockstock.domain.members.exception.NotFoundMemberException;
import io.gaboja9.mockstock.domain.members.repository.MembersRepository;
import io.gaboja9.mockstock.domain.payments.dto.*;
import io.gaboja9.mockstock.domain.payments.entity.PaymentHistory;
import io.gaboja9.mockstock.domain.payments.entity.PaymentStatus;
import io.gaboja9.mockstock.domain.payments.exception.PaymentException;
import io.gaboja9.mockstock.domain.payments.exception.PaymentHistoryException;
import io.gaboja9.mockstock.domain.payments.repository.PaymentHistoryRepository;
import io.gaboja9.mockstock.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoPayService {

    @Value("${kakaopay.secret-key}")
    private String secretKey;

    @Value("${kakaopay.base-url}")
    private String baseUrl;

    @Value("${kakaopay.cid}")
    private String cid;

    @Value("${app.base-url}")
    private String appBaseUrl;

    private final RestTemplate restTemplate;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final MembersRepository membersRepository;

    public KakaoPayReadyResponse paymentReady(Long memberId, int amount) {

        Members member =
                membersRepository
                        .findById(memberId)
                        .orElseThrow(() -> new NotFoundMemberException(memberId));

        String partnerOrderId = generateOrderId();

        PaymentHistory paymentHistory =
                PaymentHistory.builder()
                        .members(member)
                        .partnerOrderId(partnerOrderId)
                        .amount(amount)
                        .status(PaymentStatus.READY)
                        .build();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "SECRET_KEY " + secretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> params = new HashMap<>();
        params.put("cid", cid);
        params.put("partner_order_id", partnerOrderId);
        params.put("partner_user_id", String.valueOf(memberId));
        params.put("item_name", "모의투자 포인트 충전");
        params.put("quantity", "1");
        params.put("total_amount", String.valueOf(amount));
        params.put("tax_free_amount", "0");
        params.put("approval_url", appBaseUrl + "/payments/approve?member_id=" + memberId);
        params.put("cancel_url", appBaseUrl + "/payments/cancel?member_id=" + memberId);
        params.put("fail_url", appBaseUrl + "/payments/fail?member_id=" + memberId);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<KakaoPayReadyResponse> response =
                    restTemplate.exchange(
                            baseUrl + "/online/v1/payment/ready",
                            HttpMethod.POST,
                            entity,
                            KakaoPayReadyResponse.class);

            KakaoPayReadyResponse readyResponse = response.getBody();

            if (readyResponse == null) {
                throw new PaymentException(ErrorCode.KAKAOPAY_API_ERROR);
            }

            paymentHistory.setTid(readyResponse.getTid());
            paymentHistoryRepository.save(paymentHistory);

            return readyResponse;
        } catch (Exception e) {
            paymentHistory.setStatus(PaymentStatus.FAILED);
            paymentHistoryRepository.save(paymentHistory);
            throw new PaymentException(ErrorCode.PAYMENT_READY_FAILED);
        }
    }

    public KakaoPayApproveResponse paymentApprove(String pgToken, Long memberId) {

        String tid = getLatestReadyTid(memberId);

        PaymentHistory paymentHistory =
                paymentHistoryRepository
                        .findByTidAndMembersId(tid, memberId)
                        .orElseThrow(
                                () ->
                                        new PaymentHistoryException(
                                                ErrorCode.PAYMENT_HISTORY_NOT_FOUND));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "SECRET_KEY " + secretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> params = new HashMap<>();
        params.put("cid", cid);
        params.put("tid", tid);
        params.put("partner_order_id", paymentHistory.getPartnerOrderId());
        params.put("partner_user_id", String.valueOf(memberId));
        params.put("pg_token", pgToken);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<KakaoPayApproveResponse> response =
                    restTemplate.exchange(
                            baseUrl + "/online/v1/payment/approve",
                            HttpMethod.POST,
                            entity,
                            KakaoPayApproveResponse.class);

            KakaoPayApproveResponse approveResponse = response.getBody();

            if (approveResponse != null) {

                Members member = paymentHistory.getMembers();
                int chargeAmount = paymentHistory.getAmount();
                member.setTotalInvestedAmount(member.getTotalInvestedAmount() + chargeAmount);
                member.setCashBalance(member.getCashBalance() + chargeAmount);
                membersRepository.save(member);

                paymentHistory.setStatus(PaymentStatus.APPROVED);
                paymentHistoryRepository.save(paymentHistory);
            }
            return approveResponse;

        } catch (Exception e) {
            paymentHistory.setStatus(PaymentStatus.FAILED);
            paymentHistoryRepository.save(paymentHistory);
            throw new PaymentException(ErrorCode.PAYMENT_APPROVE_FAILED);
        }
    }

    public void paymentCancel(String tid, Long membersId) {
        PaymentHistory paymentHistory =
                paymentHistoryRepository
                        .findByTidAndMembersId(tid, membersId)
                        .orElseThrow(() -> new RuntimeException("임시 오류"));

        paymentHistory.setStatus(PaymentStatus.CANCELLED);
        paymentHistoryRepository.save(paymentHistory);
    }


    public void paymentFail(String tid, Long membersId) {
        PaymentHistory paymentHistory =
                paymentHistoryRepository
                        .findByTidAndMembersId(tid, membersId)
                        .orElseThrow(
                                () ->
                                        new PaymentHistoryException(
                                                ErrorCode.PAYMENT_HISTORY_NOT_FOUND));

        paymentHistory.setStatus(PaymentStatus.FAILED);
        paymentHistoryRepository.save(paymentHistory);
    }

    private String generateOrderId() {
        return "ORDER_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
    }

    public String getLatestReadyTid(Long memberId) {
        return paymentHistoryRepository.findLatestTidByMemberAndStatusReady(memberId).orElse(null);
    }

    /** 충전 내역 조회 (페이지네이션) */
    public PaymentHistoryResponse getPaymentHistory(Long memberId, PaymentHistoryRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        Page<PaymentHistory> paymentPage;

        if (request.getStatus() != null) {
            // 상태별 필터링
            paymentPage =
                    paymentHistoryRepository.findByMembersIdAndStatusOrderByCreatedAtDesc(
                            memberId, request.getStatus(), pageable);
        } else {
            // 전체 조회
            paymentPage =
                    paymentHistoryRepository.findByMembersIdOrderByCreatedAtDesc(
                            memberId, pageable);
        }

        List<PaymentHistoryDto> paymentDtos =
                paymentPage.getContent().stream()
                        .map(PaymentHistoryDto::from)
                        .collect(Collectors.toList());

        // 페이지네이션 정보
        PaymentHistoryResponse.PaginationInfo paginationInfo =
                PaymentHistoryResponse.PaginationInfo.builder()
                        .currentPage(paymentPage.getNumber())
                        .pageSize(paymentPage.getSize())
                        .totalPages(paymentPage.getTotalPages())
                        .totalElements(paymentPage.getTotalElements())
                        .hasNext(paymentPage.hasNext())
                        .hasPrevious(paymentPage.hasPrevious())
                        .build();

        // 결제 요약 정보
        PaymentHistoryResponse.PaymentSummary summary = getPaymentSummary(memberId);

        return PaymentHistoryResponse.builder()
                .payments(paymentDtos)
                .pagination(paginationInfo)
                .summary(summary)
                .build();
    }

    /** 결제 요약 정보 조회 */
    public PaymentHistoryResponse.PaymentSummary getPaymentSummary(Long memberId) {
        // 총 충전 금액/횟수
        Long totalAmount = paymentHistoryRepository.sumTotalAmountByMemberId(memberId);
        Integer totalCount = paymentHistoryRepository.countTotalByMemberId(memberId);

        // 승인된 충전 금액/횟수
        Long approvedAmount =
                paymentHistoryRepository.sumAmountByMemberIdAndStatus(
                        memberId, PaymentStatus.APPROVED);
        Integer approvedCount =
                paymentHistoryRepository.countByMemberIdAndStatus(memberId, PaymentStatus.APPROVED);

        return PaymentHistoryResponse.PaymentSummary.builder()
                .totalChargedAmount(totalAmount != null ? totalAmount : 0L)
                .totalChargeCount(totalCount != null ? totalCount : 0)
                .approvedAmount(approvedAmount != null ? approvedAmount : 0L)
                .approvedCount(approvedCount != null ? approvedCount : 0)
                .build();
    }

    /** 특정 충전 내역 상세 조회 */
    public PaymentHistoryDto getPaymentDetail(Long memberId, Long paymentId) {
        PaymentHistory paymentHistory =
                paymentHistoryRepository
                        .findById(paymentId)
                        .orElseThrow(
                                () ->
                                        new PaymentHistoryException(
                                                ErrorCode.PAYMENT_HISTORY_NOT_FOUND));

        // 본인의 결제 내역인지 확인
        if (!paymentHistory.getMembers().getId().equals(memberId)) {
            throw new PaymentException(ErrorCode.PAYMENT_ACCESS_DENIED);
        }

        return PaymentHistoryDto.from(paymentHistory);
    }
}
