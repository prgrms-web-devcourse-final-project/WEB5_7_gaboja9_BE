package io.gaboja9.mockstock.domain.notifications.controller;

import io.gaboja9.mockstock.domain.auth.dto.MembersDetails;
import io.gaboja9.mockstock.domain.notifications.dto.request.NotificationSettingsUpdateRequestDto;
import io.gaboja9.mockstock.domain.notifications.dto.response.NotificationSettingsResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "알림", description = "사용자 알림 설정 관리 API")
public interface NotificationControllerSpec {

    @Operation(
            summary = "알림 설정 조회",
            description =
                    """
                    현재 로그인한 사용자의 알림 설정을 조회합니다.
                    
                    **조회되는 설정:**
                    - `tradeNotificationEnabled`: 매매 알림 수신 여부
                    - `marketNotificationEnabled`: 시장 시간 알림 수신 여부
                    
                    **기본 설정값:**
                    - 신규 회원은 모든 알림이 기본적으로 활성화됨
                    - 설정이 없는 경우 자동으로 기본값으로 생성
                    
                    **알림 종류:**
                    - **매매 알림**: 주식 매수/매도 완료 시 메일함으로 알림 발송
                    - **시장 시간 알림**: 장 개장/마감 10분 전 메일함으로 알림 발송 (평일 오전 8:50, 오후 3:20)
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "알림 설정 조회 성공",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = NotificationSettingsResponseDto.class),
                                    examples =
                                    @ExampleObject(
                                            name = "알림 설정 조회 응답",
                                            value =
                                                    """
                                                    {
                                                        "tradeNotificationEnabled": true,
                                                        "marketNotificationEnabled": false
                                                    }
                                                    """))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                    @ExampleObject(
                                            name = "인증 실패",
                                            value =
                                                    """
                                                    {
                                                        "error": "Unauthorized",
                                                        "message": "Authentication required"
                                                    }
                                                    """))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "사용자를 찾을 수 없음",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                    @ExampleObject(
                                            name = "사용자 없음",
                                            value =
                                                    """
                                                    {
                                                        "error": "Not Found",
                                                        "message": "사용자를 찾을 수 없습니다."
                                                    }
                                                    """))),
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
                                                        "error": "Internal Server Error",
                                                        "message": "서버 내부 오류가 발생했습니다."
                                                    }
                                                    """)))
            })
    ResponseEntity<NotificationSettingsResponseDto> getNotificationSettings(
            @Parameter(hidden = true)
            @AuthenticationPrincipal
            MembersDetails membersDetails);

    @Operation(
            summary = "알림 설정 업데이트",
            description =
                    """
                    현재 로그인한 사용자의 알림 설정을 변경합니다.
                    
                    **업데이트 가능한 설정:**
                    - `tradeNotificationEnabled`: 매매 알림 활성화/비활성화
                    - `marketNotificationEnabled`: 시장 시간 알림 활성화/비활성화
                    
                    **부분 업데이트 지원:**
                    - 원하는 설정만 포함하여 요청 가능
                    - null 값인 필드는 기존 값 유지
                    - 예: 매매 알림만 변경하고 싶은 경우 `tradeNotificationEnabled`만 포함
                    
                    **알림 동작:**
                    - **매매 알림 활성화 시**: 주식 거래 완료 후 메일함으로 거래 내역 발송
                    - **시장 시간 알림 활성화 시**: 평일 장 개장/마감 10분 전 메일함으로 시장 시간 안내 발송
                    
                    **메일함 시스템:**
                    - 시스템 내부 메일함으로 발송 (외부 이메일 아님)
                    - 마이페이지 또는 메일함 메뉴에서 확인 가능
                    - 거래 완료 즉시 또는 스케줄에 따라 자동 발송
                    
                    **실시간 적용:**
                    - 설정 변경 즉시 반영
                    - 다음 거래/스케줄부터 새 설정 적용
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "알림 설정 업데이트 성공",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = NotificationSettingsResponseDto.class),
                                    examples =
                                    @ExampleObject(
                                            name = "설정 업데이트 성공",
                                            value =
                                                    """
                                                    {
                                                        "tradeNotificationEnabled": false,
                                                        "marketNotificationEnabled": true
                                                    }
                                                    """))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 데이터",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                    @ExampleObject(
                                            name = "유효성 검사 실패",
                                            value =
                                                    """
                                                    {
                                                        "error": "Bad Request",
                                                        "message": "잘못된 입력값입니다.",
                                                        "errors": [
                                                            {
                                                                "field": "tradeNotificationEnabled",
                                                                "value": "invalid",
                                                                "reason": "Boolean 값이어야 합니다."
                                                            }
                                                        ]
                                                    }
                                                    """))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                    @ExampleObject(
                                            name = "인증 실패",
                                            value =
                                                    """
                                                    {
                                                        "error": "Unauthorized",
                                                        "message": "Authentication required"
                                                    }
                                                    """))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "사용자를 찾을 수 없음",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    examples =
                                    @ExampleObject(
                                            name = "사용자 없음",
                                            value =
                                                    """
                                                    {
                                                        "error": "Not Found",
                                                        "message": "사용자를 찾을 수 없습니다."
                                                    }
                                                    """))),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 내부 오류",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    name = "데이터베이스 오류",
                                                    value =
                                                            """
                                                            {
                                                                "error": "Internal Server Error",
                                                                "message": "알림 설정 업데이트에 실패했습니다."
                                                            }
                                                            """),
                                            @ExampleObject(
                                                    name = "일반 서버 오류",
                                                    value =
                                                            """
                                                            {
                                                                "error": "Internal Server Error",
                                                                "message": "서버 내부 오류가 발생했습니다."
                                                            }
                                                            """)
                                    }))
            })
    ResponseEntity<NotificationSettingsResponseDto> updateNotificationSettings(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "업데이트할 알림 설정 정보",
                    required = true,
                    content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = NotificationSettingsUpdateRequestDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "모든 알림 활성화",
                                            description = "매매 알림과 시장 시간 알림 모두 활성화",
                                            value =
                                                    """
                                                    {
                                                        "tradeNotificationEnabled": true,
                                                        "marketNotificationEnabled": true
                                                    }
                                                    """),
                                    @ExampleObject(
                                            name = "매매 알림만 비활성화",
                                            description = "매매 알림은 끄고 시장 시간 알림은 유지",
                                            value =
                                                    """
                                                    {
                                                        "tradeNotificationEnabled": false
                                                    }
                                                    """),
                                    @ExampleObject(
                                            name = "시장 시간 알림만 활성화",
                                            description = "시장 시간 알림만 켜고 매매 알림은 유지",
                                            value =
                                                    """
                                                    {
                                                        "marketNotificationEnabled": true
                                                    }
                                                    """),
                                    @ExampleObject(
                                            name = "모든 알림 비활성화",
                                            description = "모든 알림 완전히 끄기",
                                            value =
                                                    """
                                                    {
                                                        "tradeNotificationEnabled": false,
                                                        "marketNotificationEnabled": false
                                                    }
                                                    """)
                            }))
            @Valid
            @RequestBody
            NotificationSettingsUpdateRequestDto requestDto,
            @Parameter(hidden = true)
            @AuthenticationPrincipal
            MembersDetails membersDetails);
}