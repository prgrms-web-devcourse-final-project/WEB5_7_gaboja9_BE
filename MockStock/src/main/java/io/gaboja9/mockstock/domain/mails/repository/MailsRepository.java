package io.gaboja9.mockstock.domain.mails.repository;

import io.gaboja9.mockstock.domain.mails.entity.Mails;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MailsRepository extends JpaRepository<Mails, Long> {}
