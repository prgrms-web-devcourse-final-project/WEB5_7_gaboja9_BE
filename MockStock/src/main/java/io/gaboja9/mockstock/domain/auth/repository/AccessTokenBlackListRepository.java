package io.gaboja9.mockstock.domain.auth.repository;

import io.gaboja9.mockstock.domain.auth.entity.AccessTokenBlackList;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessTokenBlackListRepository extends JpaRepository<AccessTokenBlackList, Long> {
    boolean existsByAccessToken(String accessToken);
}
