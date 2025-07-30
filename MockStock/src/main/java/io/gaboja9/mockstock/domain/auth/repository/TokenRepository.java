package io.gaboja9.mockstock.domain.auth.repository;

import io.gaboja9.mockstock.domain.auth.entity.AccessTokenBlackList;
import io.gaboja9.mockstock.domain.auth.entity.RefreshToken;
import io.gaboja9.mockstock.domain.auth.entity.RefreshTokenBlackList;
import io.gaboja9.mockstock.domain.members.entity.Members;

import java.util.List;
import java.util.Optional;

public interface TokenRepository {
    RefreshToken save(Members members, String token);

    RefreshTokenBlackList addBlackList(RefreshToken refreshToken);

    AccessTokenBlackList addAccessTokenBlackList(String accessToken);

    Optional<RefreshToken> findValidRefreshToken(Long memberId);

    boolean isTokenBlacklisted(String token);

    List<RefreshToken> findAllValidRefreshTokens(Long memberId);
}
