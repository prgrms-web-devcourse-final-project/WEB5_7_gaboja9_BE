package io.gaboja9.mockstock.domain.auth.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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
@Schema(description = "비밀번호 찾기 요청")
public class PasswordFindRequestDto {

    @Schema(description = "이메일", example = "test@test.com")
    @NotBlank(message = "비밀번호를 찾고자하는 이메일을 입력해주세요")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @Schema(description = "인증코드", example = "123456")
    @NotBlank(message = "인증코드를 입력해주세요")
    @Size(min = 6, max = 6, message = "인증코드는 6자리여야 합니다")
    private String verificationCode;

    @Schema(description = "새 비밀번호", example = "newPassword123!")
    @NotBlank(message = "새 비밀번호를 입력해주세요")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이며, 영문, 숫자, 특수문자를 포함해야 합니다.")
    private String newPassword;

    @Schema(description = "비밀번호 확인", example = "password123!")
    @NotBlank(message = "비밀번호를 다시 입력해주세요")
    private String passwordConfirm;

}
