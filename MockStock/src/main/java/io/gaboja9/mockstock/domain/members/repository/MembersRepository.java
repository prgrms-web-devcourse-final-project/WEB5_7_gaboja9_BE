package io.gaboja9.mockstock.domain.members.repository;

import java.util.Optional;

import io.gaboja9.mockstock.domain.members.entity.Members;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MembersRepository extends JpaRepository<Members, Long> {

	Optional<Members> findByEmail(String email);

}
