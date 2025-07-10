package io.gaboja9.mockstock.domain.members.entity;

import io.gaboja9.mockstock.global.common.BaseEntity;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Members extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String nickname;

    private String provider;

    private String profileImage;

    @Setter private int cashBalance = 30_000_000;

    @Setter private int bankruptcyCnt;

    @Setter private String memo;

    // 테스트용 생성자
    public Members(
            Long id,
            String email,
            String nickname,
            String provider,
            String profileImage,
            int cashBalance,
            int bankruptcyCnt,
            LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.provider = provider;
        this.profileImage = profileImage;
        this.cashBalance = cashBalance;
        this.bankruptcyCnt = bankruptcyCnt;
        this.setCreatedAt(createdAt);
    }
}
