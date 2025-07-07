package io.gaboja9.mockstock.domain.mails.entity;

import io.gaboja9.mockstock.domain.members.entity.Members;
import io.gaboja9.mockstock.global.common.BaseEntity;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mails extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean deleted = false;

    private String message;

    @ManyToOne
    @JoinColumn(name = "members_id")
    private Members members;
}
