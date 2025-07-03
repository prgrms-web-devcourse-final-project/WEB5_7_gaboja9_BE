package io.gaboja9.mockstock.domain.portfolios.repository;

import io.gaboja9.mockstock.domain.portfolios.entity.Portfolios;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfoliosRepository extends JpaRepository<Portfolios, Long> {}
