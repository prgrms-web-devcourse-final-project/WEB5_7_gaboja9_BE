package io.gaboja9.mockstock.global.config;

import io.gaboja9.mockstock.domain.auth.dto.MembersDetails;
import io.gaboja9.mockstock.domain.auth.service.AuthService;
import io.gaboja9.mockstock.domain.auth.service.JwtTokenProvider;
import io.gaboja9.mockstock.domain.members.service.MembersService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        MembersDetails principal = (MembersDetails) authentication.getPrincipal();

        log.info("principal = {}", principal);

        String email = principal.getEmail();
        log.info("email = {}", email);

        String token = jwtTokenProvider.issueAcceessToken(principal.getId(), principal.getRole());
        log.info("token = {}", token);

    }
}
