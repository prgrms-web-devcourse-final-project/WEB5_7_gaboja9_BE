package io.gaboja9.mockstock.domain.portfolios.repository;

import io.gaboja9.mockstock.domain.portfolios.entity.Portfolios;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfoliosRepository extends JpaRepository<Portfolios, Long> {

    List<Portfolios> findByMembersId(Long memberId);

    void deleteByMembersId(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Portfolios p WHERE p.members.id = :memberId AND p.stockCode = :stockCode")
    Optional<Portfolios> findByMembersIdAndStockCodeWithLock(@Param("memberId") Long memberId,
                                                             @Param("stockCode") String stockCode);
}
