package io.gaboja9.mockstock.domain.favorites.repository;

import io.gaboja9.mockstock.domain.favorites.entity.Favorites;

import org.springframework.data.repository.CrudRepository;

public interface FavoritesRepository extends CrudRepository<Favorites, Long> {}
