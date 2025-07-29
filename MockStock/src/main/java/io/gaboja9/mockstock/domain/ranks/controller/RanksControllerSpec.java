package io.gaboja9.mockstock.domain.ranks.controller;

import io.gaboja9.mockstock.domain.auth.dto.MembersDetails;
import io.gaboja9.mockstock.domain.ranks.dto.RankingResponse;
import io.gaboja9.mockstock.domain.ranks.entity.RanksType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "랭킹", description = "회원 랭킹 조회 및 관리 API")
public interface RanksControllerSpec {

    @Operation(
            summary = "랭킹 페이지네이션 조회",
            description =
                    """
                    회원들의 랭킹을 페이지네이션으로 조회합니다.

                    **응답 구성:**
                    - `topRankers`: 상위 5명 (항상 포함)
                    - `myRanking`: 내 랭킹 정보 (내 순위가 몇 위든 포함)
                    - `rankers`: 현재 페이지의 랭킹 목록
                    - `pagination`: 페이지네이션 메타데이터

                    **랭킹 유형:**
                    - `RETURN_RATE`: 수익률 순위 (높은 순)
                    - `PROFIT`: 수익금 순위 (많은 순)
                    - `ASSET`: 총 자산 순위 (많은 순)
                    - `BANKRUPTCY`: 파산 횟수 순위 (많은 순)
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "랭킹 조회 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = RankingResponse.class),
                                        examples =
                                                @ExampleObject(
                                                        name = "수익률 랭킹 응답 예시",
                                                        value =
                                                                """
                                                                {
                                                                    "topRankers": [
                                                                        {
                                                                            "memberId": 123,
                                                                            "nickname": "투자왕",
                                                                            "returnRate": 125.30,
                                                                            "totalProfit": 2530000,
                                                                            "bankruptcyCount": 0,
                                                                            "totalAsset": 12530000,
                                                                            "totalInvestment": 10000000,
                                                                            "rank": 1
                                                                        },
                                                                        {
                                                                            "memberId": 456,
                                                                            "nickname": "주식고수",
                                                                            "returnRate": 118.50,
                                                                            "totalProfit": 1850000,
                                                                            "bankruptcyCount": 0,
                                                                            "totalAsset": 11850000,
                                                                            "totalInvestment": 10000000,
                                                                            "rank": 2
                                                                        }
                                                                    ],
                                                                    "myRanking": {
                                                                        "memberId": 789,
                                                                        "nickname": "내닉네임",
                                                                        "returnRate": 103.20,
                                                                        "totalProfit": 320000,
                                                                        "bankruptcyCount": 1,
                                                                        "totalAsset": 10320000,
                                                                        "totalInvestment": 10000000,
                                                                        "rank": 15
                                                                    },
                                                                    "rankers": [
                                                                        {
                                                                            "memberId": 123,
                                                                            "nickname": "투자왕",
                                                                            "returnRate": 125.30,
                                                                            "totalProfit": 2530000,
                                                                            "bankruptcyCount": 0,
                                                                            "totalAsset": 12530000,
                                                                            "totalInvestment": 10000000,
                                                                            "rank": 1
                                                                        }
                                                                    ],
                                                                    "ranksType": "RETURN_RATE",
                                                                    "lastUpdated": "2025-07-28T11:30:00",
                                                                    "pagination": {
                                                                        "currentPage": 0,
                                                                        "pageSize": 5,
                                                                        "totalElements": 100,
                                                                        "totalPages": 20,
                                                                        "hasNext": true,
                                                                        "hasPrevious": false
                                                                    }
                                                                }
                                                                """))),
                @ApiResponse(
                        responseCode = "400",
                        description = "잘못된 요청 파라미터",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        name = "파라미터 오류",
                                                        value =
                                                                """
                                                                {
                                                                    "error": "Bad Request",
                                                                    "message": "Invalid ranksType. Allowed values: RETURN_RATE, PROFIT, ASSET, BANKRUPTCY"
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
                                                                    "message": "랭킹 계산 중 오류가 발생했습니다"
                                                                }
                                                                """)))
            })
    ResponseEntity<RankingResponse> getRankingWithPagination(
            @Parameter(hidden = true) // Swagger에서 숨김 (JWT에서 자동 추출)
                    @AuthenticationPrincipal
                    MembersDetails membersDetails,
            @Parameter(
                            name = "ranksType",
                            description = "조회할 랭킹 유형",
                            required = true,
                            in = ParameterIn.QUERY,
                            schema =
                                    @Schema(
                                            type = "string",
                                            allowableValues = {
                                                "RETURN_RATE",
                                                "PROFIT",
                                                "ASSET",
                                                "BANKRUPTCY"
                                            }),
                            examples = {
                                @ExampleObject(
                                        name = "수익률 랭킹",
                                        value = "RETURN_RATE",
                                        description = "수익률 기준 랭킹"),
                                @ExampleObject(
                                        name = "수익금 랭킹",
                                        value = "PROFIT",
                                        description = "수익금 기준 랭킹"),
                                @ExampleObject(
                                        name = "자산 랭킹",
                                        value = "ASSET",
                                        description = "총 자산 기준 랭킹"),
                                @ExampleObject(
                                        name = "파산 횟수 랭킹",
                                        value = "BANKRUPTCY",
                                        description = "파산 횟수 기준 랭킹")
                            })
                    @RequestParam
                    RanksType ranksType,
            @Parameter(
                            name = "page",
                            description = "페이지 번호 (0부터 시작)",
                            required = false,
                            in = ParameterIn.QUERY,
                            schema = @Schema(type = "integer", minimum = "0", defaultValue = "0"),
                            examples = {
                                @ExampleObject(name = "첫 번째 페이지", value = "0"),
                                @ExampleObject(name = "두 번째 페이지", value = "1"),
                                @ExampleObject(name = "세 번째 페이지", value = "2")
                            })
                    @RequestParam(defaultValue = "0")
                    int page,
            @Parameter(
                            name = "size",
                            description = "페이지 크기 (한 페이지당 표시할 항목 수, 최대 50)",
                            required = false,
                            in = ParameterIn.QUERY,
                            schema =
                                    @Schema(
                                            type = "integer",
                                            minimum = "1",
                                            maximum = "50",
                                            defaultValue = "5"),
                            examples = {
                                @ExampleObject(name = "5개씩", value = "5"),
                                @ExampleObject(name = "10개씩", value = "10"),
                                @ExampleObject(name = "20개씩", value = "20")
                            })
                    @RequestParam(defaultValue = "5")
                    int size);

    @Operation(
            summary = "랭킹 데이터 갱신",
            description =
                    """
                    시스템의 모든 랭킹 데이터를 갱신하고 캐시를 업데이트합니다.

                    **기능:**
                    - 모든 회원의 랭킹 데이터 재계산
                    - 캐시된 랭킹 데이터 갱신
                    - 실시간 랭킹 반영

                    **사용 시점:**
                    - 스케줄러에 의한 정기 갱신
                    - 관리자의 수동 갱신
                    - 시스템 점검 후 데이터 동기화

                    **참고사항:**
                    - 대량의 데이터 처리로 인해 처리 시간이 소요될 수 있습니다
                    - 갱신 중에는 랭킹 조회 성능에 영향을 줄 수 있습니다
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "랭킹 갱신 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(type = "string"),
                                        examples =
                                                @ExampleObject(
                                                        name = "갱신 성공 응답",
                                                        value = "\"랭킹 갱신 성공\""))),
                @ApiResponse(
                        responseCode = "401",
                        description = "인증 실패 (관리자 권한 필요)",
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
                                                                    "message": "Admin authentication required"
                                                                }
                                                                """))),
                @ApiResponse(
                        responseCode = "403",
                        description = "권한 부족 (관리자만 접근 가능)",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        name = "권한 부족",
                                                        value =
                                                                """
                                                                {
                                                                    "error": "Forbidden",
                                                                    "message": "Admin role required for ranking update"
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
                                                        name = "갱신 실패",
                                                        value =
                                                                """
                                                                {
                                                                    "error": "Internal Server Error",
                                                                    "message": "랭킹 갱신 중 오류가 발생했습니다"
                                                                }
                                                                """))),
                @ApiResponse(
                        responseCode = "503",
                        description = "서비스 일시 불가 (갱신 진행 중)",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        name = "갱신 진행 중",
                                                        value =
                                                                """
                                                                {
                                                                    "error": "Service Unavailable",
                                                                    "message": "랭킹 갱신이 이미 진행 중입니다. 잠시 후 다시 시도해주세요"
                                                                }
                                                                """)))
            })
    ResponseEntity<String> updateRanking();
}
