package io.gaboja9.mockstock.domain.auth.controller;

import io.gaboja9.mockstock.domain.auth.dto.request.*;
import io.gaboja9.mockstock.domain.auth.dto.response.AuthResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "인증", description = "회원 인증 및 계정 관리 API")
public interface AuthControllerSpec {

    @Operation(
            summary = "회원가입",
            description =
                    """
                    이메일과 비밀번호를 사용한 일반 회원가입을 처리합니다.
                    
                    **처리 과정:**
                    1. 이메일 중복 확인
                    2. 이메일 인증코드 검증
                    3. 비밀번호 일치 확인
                    4. 계정 생성 및 초기 자금(3,000만원) 지급
                    
                    **사전 요구사항:**
                    - 이메일 인증코드 발송(`POST /auth/email`) 완료
                    - 발송된 6자리 인증코드 보유
                    
                    **비밀번호 요구사항:**
                    - 최소 8자 이상
                    - 영문, 숫자, 특수문자 포함
                    """)
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "회원가입 성공",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AuthResponseDto.class),
                                    examples =
                                    @ExampleObject(
                                            name = "회원가입 성공",
                                            value =
                                                    """
                                                    {
                                                        "success": true,
                                                        "message": "회원가입이 완료되었습니다.",
                                                        "data": null
                                                    }
                                                    """))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    name = "이메일 중복",
                                                    value =
                                                            """
                                                            {
                                                                "success": false,
                                                                "message": "이미 존재하는 이메일입니다."
                                                            }
                                                            """),
                                            @ExampleObject(
                                                    name = "인증코드 오류",
                                                    value =
                                                            """
                                                            {
                                                                "success": false,
                                                                "message": "인증코드가 올바르지 않습니다."
                                                            }
                                                            """),
                                            @ExampleObject(
                                                    name = "비밀번호 불일치",
                                                    value =
                                                            """
                                                            {
                                                                "success": false,
                                                                "message": "비밀번호가 일치하지 않습니다."
                                                            }
                                                            """)
                                    })),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 내부 오류",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                    @ExampleObject(
                                            name = "서버 오류",
                                            value =
                                                    """
                                                    {
                                                        "success": false,
                                                        "message": "서버 내부 오류가 발생했습니다."
                                                    }
                                                    """)))
            })
    ResponseEntity<AuthResponseDto> signUp(@Valid @RequestBody SignUpRequestDto signUpRequestDto);

    @Operation(
            summary = "로그인",
            description =
                    """
                    이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.
                    
                    **응답 데이터:**
                    - `accessToken`: API 요청 시 사용할 액세스 토큰 (유효기간: 1시간)
                    - `refreshToken`: 액세스 토큰 갱신용 리프레시 토큰 (유효기간: 24시간)
                    - `accessTokenExpiresIn`: 액세스 토큰 만료 시간 (분 단위)
                    - `refreshTokenExpiresIn`: 리프레시 토큰 만료 시간 (분 단위)
                    
                    **토큰 사용법:**
                    - Authorization 헤더에 `Bearer {accessToken}` 형태로 포함
                    - 토큰 만료 시 리프레시 토큰으로 갱신 필요
                    """)
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "로그인 성공",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AuthResponseDto.class),
                                    examples =
                                    @ExampleObject(
                                            name = "로그인 성공",
                                            value =
                                                    """
                                                    {
                                                        "success": true,
                                                        "message": "로그인이 완료되었습니다.",
                                                        "data": {
                                                            "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                                                            "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
                                                            "accessTokenExpiresIn": 60,
                                                            "refreshTokenExpiresIn": 1440
                                                        }
                                                    }
                                                    """))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    name = "로그인 정보 오류",
                                                    value =
                                                            """
                                                            {
                                                                "success": false,
                                                                "message": "이메일 또는 비밀번호가 올바르지 않습니다."
                                                            }
                                                            """),
                                            @ExampleObject(
                                                    name = "소셜 로그인 필요",
                                                    value =
                                                            """
                                                            {
                                                                "success": false,
                                                                "message": "GOOGLE 계정이 존재합니다. GOOGLE 로그인을 이용해주세요."
                                                            }
                                                            """)
                                    }))
            })
    ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequestDto);

    @Operation(
            summary = "회원가입용 이메일 인증코드 발송",
            description =
                    """
                    회원가입을 위한 이메일 인증코드를 발송합니다.
                    
                    **발송 과정:**
                    1. 이메일 중복 확인 (기존 회원인 경우 오류)
                    2. 6자리 인증코드 생성
                    3. 이메일 발송 (유효기간: 5분)
                    
                    **재발송 제한:**
                    - 이전 발송 후 1분 경과 후 재발송 가능
                    - 재발송 시도 시 남은 시간(초) 안내
                    
                    **인증코드 특징:**
                    - 6자리 숫자
                    - 5분 후 자동 만료
                    - 1회 사용 후 무효화
                    """)
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "인증코드 발송 성공",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AuthResponseDto.class),
                                    examples =
                                    @ExampleObject(
                                            name = "발송 성공",
                                            value =
                                                    """
                                                    {
                                                        "success": true,
                                                        "message": "인증코드가 발송되었습니다.",
                                                        "data": null
                                                    }
                                                    """))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    name = "이메일 중복",
                                                    value =
                                                            """
                                                            {
                                                                "success": false,
                                                                "message": "이미 존재하는 이메일입니다."
                                                            }
                                                            """),
                                            @ExampleObject(
                                                    name = "재발송 제한",
                                                    value =
                                                            """
                                                            {
                                                                "success": false,
                                                                "message": "인증코드는 45초 후에 재발송할 수 있습니다."
                                                            }
                                                            """)
                                    })),
                    @ApiResponse(
                            responseCode = "500",
                            description = "이메일 발송 실패",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                    @ExampleObject(
                                            name = "발송 실패",
                                            value =
                                                    """
                                                    {
                                                        "success": false,
                                                        "message": "이메일 발송에 실패했습니다."
                                                    }
                                                    """)))
            })
    ResponseEntity<AuthResponseDto> email(@Valid @RequestBody EmailVerificationRequestDto dto);

    @Operation(
            summary = "비밀번호 찾기용 이메일 인증코드 발송",
            description =
                    """
                    비밀번호 찾기를 위한 이메일 인증코드를 발송합니다.
                    
                    **발송 과정:**
                    1. 가입된 이메일인지 확인 (미가입 시 오류)
                    2. 6자리 인증코드 생성
                    3. 이메일 발송 (유효기간: 5분)
                    
                    **회원가입용 인증코드와의 차이점:**
                    - 기존 회원만 발송 가능
                    - 비밀번호 찾기 전용
                    - 동일한 재발송 제한 적용 (1분)
                    """)
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "인증코드 발송 성공",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AuthResponseDto.class),
                                    examples =
                                    @ExampleObject(
                                            name = "발송 성공",
                                            value =
                                                    """
                                                    {
                                                        "success": true,
                                                        "message": "비밀번호 찾기 인증코드가 발송되었습니다.",
                                                        "data": null
                                                    }
                                                    """))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    name = "존재하지 않는 이메일",
                                                    value =
                                                            """
                                                            {
                                                                "success": false,
                                                                "message": "존재하지 않는 이메일입니다."
                                                            }
                                                            """),
                                            @ExampleObject(
                                                    name = "재발송 제한",
                                                    value =
                                                            """
                                                            {
                                                                "success": false,
                                                                "message": "인증코드는 30초 후에 재발송할 수 있습니다."
                                                            }
                                                            """)
                                    }))
            })
    ResponseEntity<AuthResponseDto> emailForPasswordFind(
            @Valid @RequestBody EmailVerificationRequestDto dto);

    @Operation(
            summary = "비밀번호 재설정 (로그인 상태)",
            description =
                    """
                    로그인된 사용자가 현재 비밀번호를 알고 있을 때 새 비밀번호로 변경합니다.
                    
                    **처리 과정:**
                    1. 현재 비밀번호 확인
                    2. 새 비밀번호 검증 (강도 및 일치 확인)
                    3. 기존 비밀번호와 동일한지 확인
                    4. 비밀번호 업데이트
                    
                    **제한사항:**
                    - 소셜 로그인 계정은 비밀번호 재설정 불가
                    - 현재 비밀번호와 동일한 새 비밀번호 설정 불가
                    
                    **보안 요구사항:**
                    - JWT 토큰 필요 (Authorization 헤더)
                    - 현재 비밀번호 입력 필요
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "비밀번호 재설정 성공",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AuthResponseDto.class),
                                    examples =
                                    @ExampleObject(
                                            name = "재설정 성공",
                                            value =
                                                    """
                                                    {
                                                        "success": true,
                                                        "message": "비밀번호가 재설정되었습니다.",
                                                        "data": null
                                                    }
                                                    """))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    name = "현재 비밀번호 오류",
                                                    value =
                                                            """
                                                            {
                                                                "success": false,
                                                                "message": "현재 비밀번호가 올바르지 않습니다."
                                                            }
                                                            """),
                                            @ExampleObject(
                                                    name = "새 비밀번호 불일치",
                                                    value =
                                                            """
                                                            {
                                                                "success": false,
                                                                "message": "새 비밀번호와 비밀번호 확인이 일치하지 않습니다."
                                                            }
                                                            """),
                                            @ExampleObject(
                                                    name = "동일한 비밀번호",
                                                    value =
                                                            """
                                                            {
                                                                "success": false,
                                                                "message": "새 비밀번호는 현재 비밀번호와 달라야 합니다."
                                                            }
                                                            """),
                                            @ExampleObject(
                                                    name = "소셜 계정 제한",
                                                    value =
                                                            """
                                                            {
                                                                "success": false,
                                                                "message": "소셜 로그인 계정은 비밀번호 재설정이 불가능합니다."
                                                            }
                                                            """)
                                    })),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 필요",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                    @ExampleObject(
                                            name = "인증 필요",
                                            value =
                                                    """
                                                    {
                                                        "success": false,
                                                        "message": "인증되지 않은 사용자입니다."
                                                    }
                                                    """)))
            })
    ResponseEntity<AuthResponseDto> resetPassword(
            @Valid @RequestBody PasswordResetRequestDto dto, Authentication authentication);

    @Operation(
            summary = "비밀번호 찾기 (인증코드 사용)",
            description =
                    """
                    이메일 인증코드를 사용하여 잊어버린 비밀번호를 새로 설정합니다.
                    
                    **처리 과정:**
                    1. 이메일 계정 존재 확인
                    2. 인증코드 검증
                    3. 소셜 로그인 계정 여부 확인
                    4. 새 비밀번호 검증 및 설정
                    
                    **사전 요구사항:**
                    - 비밀번호 찾기용 인증코드 발송(`POST /auth/email/passwordFind`) 완료
                    - 발송된 6자리 인증코드 보유
                    
                    **제한사항:**
                    - 소셜 로그인 전용 계정은 비밀번호 찾기 불가
                    - 인증코드 1회 사용 후 무효화
                    """)
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "비밀번호 찾기 성공",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AuthResponseDto.class),
                                    examples =
                                    @ExampleObject(
                                            name = "비밀번호 재설정 성공",
                                            value =
                                                    """
                                                    {
                                                        "success": true,
                                                        "message": "비밀번호가 재설정되었습니다.",
                                                        "data": null
                                                    }
                                                    """))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    name = "존재하지 않는 이메일",
                                                    value =
                                                            """
                                                            {
                                                                "success": false,
                                                                "message": "존재하지 않는 이메일입니다."
                                                            }
                                                            """),
                                            @ExampleObject(
                                                    name = "인증코드 오류",
                                                    value =
                                                            """
                                                            {
                                                                "success": false,
                                                                "message": "인증코드가 올바르지 않습니다."
                                                            }
                                                            """),
                                            @ExampleObject(
                                                    name = "소셜 계정 제한",
                                                    value =
                                                            """
                                                            {
                                                                "success": false,
                                                                "message": "KAKAO 계정이 존재합니다. KAKAO 로그인을 이용해주세요."
                                                            }
                                                            """),
                                            @ExampleObject(
                                                    name = "비밀번호 불일치",
                                                    value =
                                                            """
                                                            {
                                                                "success": false,
                                                                "message": "새 비밀번호와 비밀번호 확인이 일치하지 않습니다."
                                                            }
                                                            """)
                                    }))
            })
    ResponseEntity<AuthResponseDto> findPassword(@Valid @RequestBody PasswordFindRequestDto dto);

    @Operation(
            summary = "로그아웃",
            description =
                    """
                    현재 로그인된 사용자를 로그아웃 처리합니다.
                    
                    **처리 과정:**
                    1. 현재 사용중인 액세스 토큰을 블랙리스트에 추가
                    2. 사용자의 모든 리프레시 토큰 무효화
                    3. 토큰 기반 세션 종료
                    
                    **보안 특징:**
                    - 토큰 블랙리스트를 통한 즉시 무효화
                    - 다중 기기 로그인 시 모든 세션 종료
                    - 서버 측 토큰 상태 관리
                    
                    **사용법:**
                    - Authorization 헤더에 Bearer 토큰 포함 필요
                    - 로그아웃 후 모든 API 요청에 대해 재인증 필요
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "로그아웃 성공",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AuthResponseDto.class),
                                    examples =
                                    @ExampleObject(
                                            name = "로그아웃 성공",
                                            value =
                                                    """
                                                    {
                                                        "success": true,
                                                        "message": "로그아웃이 완료되었습니다.",
                                                        "data": null
                                                    }
                                                    """))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                    @ExampleObject(
                                            name = "인증되지 않은 사용자",
                                            value =
                                                    """
                                                    {
                                                        "success": false,
                                                        "message": "인증되지 않은 사용자입니다."
                                                    }
                                                    """))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 필요",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                    @ExampleObject(
                                            name = "인증 필요",
                                            value =
                                                    """
                                                    {
                                                        "error": "Unauthorized",
                                                        "message": "Authentication required"
                                                    }
                                                    """)))
            })
    ResponseEntity<AuthResponseDto> logout(
            Authentication authentication, HttpServletRequest request);

    @Operation(
            summary = "토큰 갱신",
            description =
                    """
                    만료된 액세스 토큰을 리프레시 토큰을 사용하여 갱신합니다.
                    
                    **갱신 과정:**
                    1. 리프레시 토큰 유효성 검증
                    2. 블랙리스트 확인 (로그아웃된 토큰인지)
                    3. 새로운 액세스 토큰 발급
                    4. 기존 리프레시 토큰 유지
                    
                    **토큰 생명주기:**
                    - 액세스 토큰: 1시간 (새로 발급)
                    - 리프레시 토큰: 24시간 (기존 토큰 유지)
                    
                    **사용 시점:**
                    - 액세스 토큰 만료 시 (401 Unauthorized 응답 시)
                    - 자동 갱신 로직 구현 시
                    
                    **보안 고려사항:**
                    - 리프레시 토큰도 만료 시 재로그인 필요
                    - 토큰 탈취 방지를 위한 안전한 저장 필요
                    """)
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "토큰 갱신 성공",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AuthResponseDto.class),
                                    examples =
                                    @ExampleObject(
                                            name = "갱신 성공",
                                            value =
                                                    """
                                                    {
                                                        "success": true,
                                                        "message": "토큰 갱신이 완료되었습니다.",
                                                        "data": {
                                                            "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                                                            "accessTokenExpiresIn": 60
                                                        }
                                                    }
                                                    """))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "토큰 갱신 실패",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    name = "리프레시 토큰 만료",
                                                    value =
                                                            """
                                                            {
                                                                "success": false,
                                                                "message": "토큰이 만료되었습니다."
                                                            }
                                                            """),
                                            @ExampleObject(
                                                    name = "유효하지 않은 토큰",
                                                    value =
                                                            """
                                                            {
                                                                "success": false,
                                                                "message": "유효하지 않은 토큰입니다."
                                                            }
                                                            """),
                                            @ExampleObject(
                                                    name = "로그아웃된 토큰",
                                                    value =
                                                            """
                                                            {
                                                                "success": false,
                                                                "message": "이미 로그아웃된 토큰입니다."
                                                            }
                                                            """)
                                    })),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 내부 오류",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                    @ExampleObject(
                                            name = "서버 오류",
                                            value =
                                                    """
                                                    {
                                                        "success": false,
                                                        "message": "토큰 갱신에 실패했습니다."
                                                    }
                                                    """)))
            })
    ResponseEntity<AuthResponseDto> refresh(@Valid @RequestBody TokenRefreshRequestDto requestDto);
}