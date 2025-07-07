package io.gaboja9.mockstock.domain.trades.repository;

import io.gaboja9.mockstock.domain.trades.entity.Trades;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradesRepository extends CrudRepository<Trades, Long> {

    int countByMembersId(Long memberId);
}
