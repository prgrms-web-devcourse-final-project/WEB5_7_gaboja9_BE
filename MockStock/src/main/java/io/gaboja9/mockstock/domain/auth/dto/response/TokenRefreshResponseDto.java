package io.gaboja9.mockstock.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "토큰 갱신 응답")
public class TokenRefreshResponseDto {
    @Schema(description = "새로운 액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "액세스 토큰 만료시간 (분)", example = "60")
    private Long accessTokenExpiresIn;
}
