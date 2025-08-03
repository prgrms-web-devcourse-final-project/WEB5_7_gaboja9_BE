package io.gaboja9.mockstock.domain.mails.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MailsRequestDto {
    @Schema(description = "메일 ID")
    private Long mailId;
}
