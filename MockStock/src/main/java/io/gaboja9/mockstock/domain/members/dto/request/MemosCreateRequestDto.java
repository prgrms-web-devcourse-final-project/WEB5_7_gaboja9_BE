package io.gaboja9.mockstock.domain.members.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemosCreateRequestDto {

    @NotBlank
    @Size(min = 1, max = 50)
    private String memo;
}
