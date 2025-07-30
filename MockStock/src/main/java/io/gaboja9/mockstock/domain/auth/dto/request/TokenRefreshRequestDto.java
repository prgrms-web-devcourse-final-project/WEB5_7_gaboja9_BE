package io.gaboja9.mockstock.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "토큰 갱신 요청")
public class TokenRefreshRequestDto {
    @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    @NotBlank(message = "리프레시 토큰을 입력해주세요")
    private String refreshToken;
}
