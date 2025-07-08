package io.gaboja9.mockstock.domain.trades.repository;

import io.gaboja9.mockstock.domain.trades.entity.Trades;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TradesRepository extends CrudRepository<Trades, Long> {

    int countByMembersId(Long memberId);

    @Query(
            "SELECT DISTINCT t FROM Trades t WHERE "
                    + "(t.stockCode = :stockCode OR t.stockName = :stockName) "
                    + "AND t.createdAt BETWEEN :startDateTime AND :endDateTime "
                    + "AND t.members.id = :membersId "
                    + "order by t.createdAt desc")
    List<Trades> findByStockCodeOrStockNameAndCreatedAtBetween(
            @Param("stockCode") String stockCode,
            @Param("stockName") String stockName,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("membersId") Long membersId);

    @Query("SELECT t FROM Trades t WHERE t.members.id = :membersId order by t.createdAt desc")
    List<Trades> findByMembersId(@Param("membersId") Long membersId);
}
