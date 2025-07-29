package io.gaboja9.mockstock.domain.auth.entity;

import io.gaboja9.mockstock.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccessTokenBlackList extends BaseEntity {

    @Id
    @Column(name = "access_token_black_list_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String accessToken;

    @Builder
    public AccessTokenBlackList(String accessToken) {
        this.accessToken = accessToken;
    }
}