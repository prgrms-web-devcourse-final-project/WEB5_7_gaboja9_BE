package io.gaboja9.mockstock.domain.members.repository;

import io.gaboja9.mockstock.domain.members.entity.Members;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MembersRepository extends JpaRepository<Members, Long> {}
