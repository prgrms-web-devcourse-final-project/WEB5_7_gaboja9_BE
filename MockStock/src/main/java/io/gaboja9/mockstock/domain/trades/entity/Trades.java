package io.gaboja9.mockstock.domain.trades.entity;

import io.gaboja9.mockstock.domain.members.entity.Members;
import io.gaboja9.mockstock.global.common.BaseEntity;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trades extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String stockCode;

    @Enumerated(EnumType.STRING)
    private TradeType tradeType;

    private int quantity;

    private Long price;

    @ManyToOne
    @JoinColumn(name = "members_id")
    private Members members;
}
