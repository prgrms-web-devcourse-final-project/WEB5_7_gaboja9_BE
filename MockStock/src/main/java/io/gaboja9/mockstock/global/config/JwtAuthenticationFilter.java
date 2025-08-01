package io.gaboja9.mockstock.global.config;

import io.gaboja9.mockstock.domain.auth.dto.MembersDetails;
import io.gaboja9.mockstock.domain.auth.dto.TokenBody;
import io.gaboja9.mockstock.domain.auth.exception.JwtAuthenticationException;
import io.gaboja9.mockstock.domain.auth.exception.JwtResponseHandler;
import io.gaboja9.mockstock.domain.auth.service.AuthService;
import io.gaboja9.mockstock.domain.auth.service.JwtTokenProvider;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;
    private final JwtResponseHandler jwtResponseHandler;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        if (shouldSkipAuthentication(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);

        if (token != null) {
            try {
                if (jwtTokenProvider.validate(token)) {
                    TokenBody tokenBody = jwtTokenProvider.parseJwt(token);
                    MembersDetails membersDetails =
                            authService.getMembersDetailsById(tokenBody.getMemberId());

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    membersDetails, token, membersDetails.getAuthorities());

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtAuthenticationException e) {
                log.warn("JWT 인증 실패: {} - {}", e.getErrorCode().getCode(), e.getMessage());
                jwtResponseHandler.handleJwtException(response, e);
                return;
            } catch (Exception e) {
                log.error("토큰 처리 중 예상치 못한 오류 발생", e);
                jwtResponseHandler.handleUnexpectedException(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean shouldSkipAuthentication(String requestURI) {
        return requestURI.startsWith("/auth/signup")
                || requestURI.equals("/auth/login")
                || requestURI.equals("/auth/email")
                || requestURI.equals("/auth/email/passwordFind")
                || requestURI.equals("/auth/passwordFind")
                || requestURI.equals("/auth/refresh")
                || requestURI.startsWith("/swagger-ui/")
                || requestURI.startsWith("/v3/api-docs/")
                || requestURI.startsWith("/actuator/")
                || requestURI.equals("/api/stocks")
                || requestURI.startsWith("/ws-stock");
    }
}
