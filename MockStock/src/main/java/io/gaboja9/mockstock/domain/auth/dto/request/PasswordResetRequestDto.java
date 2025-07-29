package io.gaboja9.mockstock.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "비밀번호 재설정 요청")
public class PasswordResetRequestDto {

    @Schema(description = "현재 비밀번호", example = "password123!")
    @NotBlank(message = "현재 비밀번호를 입력해주세요")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    private String presentPassword;

    @Schema(description = "새 비밀번호", example = "newPassword123!")
    @NotBlank(message = "새 비밀번호를 입력해주세요")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이며, 영문, 숫자, 특수문자를 포함해야 합니다.")
    private String newPassword;

    @Schema(description = "비밀번호 확인", example = "password123!")
    @NotBlank(message = "비밀번호를 다시 입력해주세요")
    private String passwordConfirm;
}
