package io.gaboja9.mockstock.domain.portfolios.entity;

import io.gaboja9.mockstock.domain.members.entity.Members;
import io.gaboja9.mockstock.global.common.BaseEntity;

import jakarta.persistence.*;

import lombok.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Portfolios extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String stockCode;

    private int quantity;

    private int avgPrice;

    private boolean deleted = false;

    @ManyToOne
    @JoinColumn(name = "members_id")
    private Members members;
}
