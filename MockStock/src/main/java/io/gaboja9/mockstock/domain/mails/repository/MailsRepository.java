package io.gaboja9.mockstock.domain.mails.repository;

import io.gaboja9.mockstock.domain.mails.entity.Mails;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MailsRepository extends JpaRepository<Mails, Long> {

    List<Mails> findByMembersId(Long memberId);

    List<Mails> findByMembersIdAndReadStatus(Long memberId, boolean readStatus);
}
