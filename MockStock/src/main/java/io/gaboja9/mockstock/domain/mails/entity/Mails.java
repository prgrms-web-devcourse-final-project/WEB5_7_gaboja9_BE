package io.gaboja9.mockstock.domain.mails.entity;

import io.gaboja9.mockstock.domain.members.entity.Members;
import io.gaboja9.mockstock.global.common.BaseEntity;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mails extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean deleted = false;

    private String subject;

    private String content;

    private boolean readStatus = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "members_id")
    private Members members;

    // 테스트용 생성자
    public Mails(
            String subject,
            String content,
            boolean readStatus,
            LocalDateTime createdAt,
            Members members) {
        this.subject = subject;
        this.content = content;
        this.readStatus = readStatus;
        this.setCreatedAt(createdAt);
        this.members = members;
    }
}
