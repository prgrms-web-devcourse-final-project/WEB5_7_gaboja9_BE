package io.gaboja9.mockstock.domain.members.entity;

import io.gaboja9.mockstock.domain.members.enums.Role;
import io.gaboja9.mockstock.global.common.BaseEntity;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Members extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String nickname;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    private String provider;

    private String profileImage;

    private int cashBalance;

    private int bankruptcyCnt;

    @Builder
    public Members(String email, String nickname, String provider, String profileImage, int cashBalance,
        int bankruptcyCnt) {
        this.email = email;
        this.nickname = nickname;
        this.provider = provider;
        this.profileImage = profileImage;
        this.cashBalance = cashBalance;
        this.bankruptcyCnt = bankruptcyCnt;
    }

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
