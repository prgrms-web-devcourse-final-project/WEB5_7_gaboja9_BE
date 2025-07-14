package io.gaboja9.mockstock.domain.auth.service;

import io.gaboja9.mockstock.domain.auth.dto.TokenBody;
import io.gaboja9.mockstock.domain.members.enums.Role;
import io.gaboja9.mockstock.global.config.JwtConfiguration;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Date;

import javax.crypto.SecretKey;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtConfiguration jwtConfiguration;

    public String issueAcceessToken(Long id, Role role) {
        return issue(id, role, jwtConfiguration.getValidation().getAccess());
    }

    public String issueRefreshToken(Long id, Role role) {
        return issue(id, role, jwtConfiguration.getValidation().getRefresh());
    }

    private String issue(Long id, Role role, Long expTime) {

        return Jwts.builder()
                .subject(id.toString())
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + expTime))
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    public boolean validate(String token) {

        try {

            Jwts.parser().verifyWith(getSecretKey()).build().parseClaimsJws(token);

            return true;

        } catch (JwtException e) {
            log.error("token = {}", token);
            log.error("잘못된 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.error("token = {}", token);
            log.error("이상한 토큰입니다.");
        } catch (Exception e) {
            log.error("token = {}", token);
            log.error(";;;");
        }

        return false;
    }

    public TokenBody parseJwt(String token) {
        Jws<Claims> parsed = Jwts.parser().verifyWith(getSecretKey()).build().parseClaimsJws(token);

        String sub = parsed.getPayload().getSubject();
        String role = parsed.getPayload().get("role").toString();

        return new TokenBody(Long.parseLong(sub), Role.valueOf(role));
    }

    @NotNull
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtConfiguration.getSecrets().getAppKey().getBytes());
    }
}
