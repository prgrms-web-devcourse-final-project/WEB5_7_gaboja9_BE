package io.gaboja9.mockstock.domain.payments.repository;

import io.gaboja9.mockstock.domain.payments.entity.PaymentHistory;
import io.gaboja9.mockstock.domain.payments.entity.PaymentStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {

    Optional<PaymentHistory> findByTidAndMembersId(String tid, Long membersId);

    @Query(
            """
            SELECT p.tid
            FROM PaymentHistory p
            WHERE p.members.id = :memberId
              AND p.status = 'READY'
            ORDER BY p.createdAt DESC
            LIMIT 1
            """)
    Optional<String> findLatestTidByMemberAndStatusReady(@Param("memberId") Long memberId);

    List<PaymentHistory> findByMembersIdAndStatus(Long memberId, PaymentStatus paymentStatus);

    Page<PaymentHistory> findByMembersIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    Page<PaymentHistory> findByMembersIdAndStatusOrderByCreatedAtDesc(
            Long memberId, PaymentStatus status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(ph.amount), 0) FROM PaymentHistory ph " +
            "WHERE ph.members.id = :memberId AND ph.status = :status")
    Long sumAmountByMemberIdAndStatus(Long memberId, PaymentStatus status);

    @Query("SELECT COUNT(ph) FROM PaymentHistory ph " +
            "WHERE ph.members.id = :memberId AND ph.status = :status")
    Integer countByMemberIdAndStatus(Long memberId, PaymentStatus status);

    @Query("SELECT COALESCE(SUM(ph.amount), 0) FROM PaymentHistory ph " +
            "WHERE ph.members.id = :memberId")
    Long sumTotalAmountByMemberId(Long memberId);

    @Query("SELECT COUNT(ph) FROM PaymentHistory ph " +
            "WHERE ph.members.id = :memberId")
    Integer countTotalByMemberId(Long memberId);
}
