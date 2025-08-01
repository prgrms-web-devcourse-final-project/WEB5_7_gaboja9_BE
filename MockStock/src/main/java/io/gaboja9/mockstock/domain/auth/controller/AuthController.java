package io.gaboja9.mockstock.domain.auth.controller;

import io.gaboja9.mockstock.domain.auth.dto.MembersDetails;
import io.gaboja9.mockstock.domain.auth.dto.TokenPair;
import io.gaboja9.mockstock.domain.auth.dto.request.*;
import io.gaboja9.mockstock.domain.auth.dto.response.AuthResponseDto;
import io.gaboja9.mockstock.domain.auth.dto.response.TokenRefreshResponseDto;
import io.gaboja9.mockstock.domain.auth.entity.RefreshToken;
import io.gaboja9.mockstock.domain.auth.exception.JwtAuthenticationException;
import io.gaboja9.mockstock.domain.auth.repository.TokenRepository;
import io.gaboja9.mockstock.domain.auth.service.EmailVerificationService;
import io.gaboja9.mockstock.domain.auth.service.FormAuthService;
import io.gaboja9.mockstock.domain.auth.service.JwtTokenProvider;
import io.gaboja9.mockstock.global.config.JwtConfiguration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerSpec {

    private final FormAuthService formAuthService;
    private final EmailVerificationService emailVerificationService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRepository tokenRepository;
    private final JwtConfiguration jwtConfiguration;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponseDto> signUp(
            @Valid @RequestBody SignUpRequestDto signUpRequestDto) {
        log.info("회원가입 요청: {}", signUpRequestDto.getEmail());

        formAuthService.signUp(signUpRequestDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AuthResponseDto.success("회원가입이 완료되었습니다."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody LoginRequestDto loginRequestDto) {

        log.info("로그인 요청:{}", loginRequestDto.getEmail());

        TokenPair tokenPair = formAuthService.login(loginRequestDto);

        return ResponseEntity.ok(AuthResponseDto.success("로그인이 완료되었습니다.", tokenPair));
    }

    @PostMapping("/email")
    public ResponseEntity<AuthResponseDto> email(
            @Valid @RequestBody EmailVerificationRequestDto dto) {
        log.info("이메일 인증코드 발송 요청: {}", dto.getEmail());

        emailVerificationService.sendVerificationCodeForSignup(dto.getEmail());

        return ResponseEntity.ok(AuthResponseDto.success("인증코드가 발송되었습니다."));
    }

    @PostMapping("/email/passwordFind")
    public ResponseEntity<AuthResponseDto> emailForPasswordFind(
            @Valid @RequestBody EmailVerificationRequestDto dto) {
        log.info("이메일 인증코드 발송 요청: {}", dto.getEmail());

        emailVerificationService.sendVerificationCodeForPasswordFind(dto.getEmail());

        return ResponseEntity.ok(AuthResponseDto.success("비밀번호 찾기 인증코드가 발송되었습니다."));
    }

    @PostMapping("/passwordReset")
    public ResponseEntity<AuthResponseDto> resetPassword(
            @Valid @RequestBody PasswordResetRequestDto dto, Authentication authentication) {

        log.info("비밀번호 재설정 요청");

        MembersDetails membersDetails = (MembersDetails) authentication.getPrincipal();
        Long memberId = membersDetails.getId();

        try {
            formAuthService.resetPassword(memberId, dto);
            return ResponseEntity.ok(AuthResponseDto.success("비밀번호가 재설정되었습니다."));
        } catch (Exception e) {
            log.error("비밀번호 재설정 처리 중 오류 발생", e);
            return ResponseEntity.badRequest().body(AuthResponseDto.fail(e.getMessage()));
        }
    }

    @PostMapping("/passwordFind")
    public ResponseEntity<AuthResponseDto> findPassword(
            @Valid @RequestBody PasswordFindRequestDto dto) {

        log.info("비밀번호 찾기 요청");

        try {
            formAuthService.findPassword(dto);
            return ResponseEntity.ok(AuthResponseDto.success("비밀번호가 재설정되었습니다."));
        } catch (Exception e) {
            log.error("비밀번호 찾기 중 오류 발생", e);
            return ResponseEntity.badRequest().body(AuthResponseDto.fail(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponseDto> logout(
            Authentication authentication, HttpServletRequest request) {

        log.info("로그아웃 요청");

        if (authentication == null) {
            return ResponseEntity.badRequest().body(AuthResponseDto.fail("인증되지 않은 사용자입니다."));
        }

        MembersDetails membersDetails = (MembersDetails) authentication.getPrincipal();
        Long memberId = membersDetails.getId();

        String authorization = request.getHeader("Authorization");
        if (authorization != null) {
            try {
                String accessToken = jwtTokenProvider.extractTokenFromHeader(authorization);
                tokenRepository.addAccessTokenBlackList(accessToken);
                log.info("Access Token이 블랙리스트에 추가되었습니다.");
            } catch (Exception e) {
                log.warn("Access Token 추출 실패", e);
            }
        }

        Optional<RefreshToken> refreshTokenOptional = jwtTokenProvider.findRefreshToken(memberId);
        if (refreshTokenOptional.isPresent()) {
            RefreshToken refreshToken = refreshTokenOptional.get();
            tokenRepository.addBlackList(refreshToken);
            log.info("Refresh Token이 블랙리스트에 추가되었습니다.");
        }

        return ResponseEntity.ok(AuthResponseDto.success("로그아웃이 완료되었습니다."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(
            @Valid @RequestBody TokenRefreshRequestDto requestDto) {
        log.info("토큰 갱신 요청");

        try {
            String refreshToken = requestDto.getRefreshToken();
            String newAccessToken = jwtTokenProvider.refreshAccessToken(refreshToken);

            Long accessTokenExpiresIn = jwtConfiguration.getValidation().getAccess() / 60000;

            TokenRefreshResponseDto responseData =
                    TokenRefreshResponseDto.builder()
                            .accessToken(newAccessToken)
                            .accessTokenExpiresIn(accessTokenExpiresIn)
                            .build();

            return ResponseEntity.ok(AuthResponseDto.success("토큰 갱신이 완료되었습니다.", responseData));

        } catch (JwtAuthenticationException e) {
            log.warn("토큰 갱신 실패 - JWT 예외: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponseDto.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("토큰 갱신 처리 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponseDto.fail("토큰 갱신에 실패했습니다."));
        }
    }
}
