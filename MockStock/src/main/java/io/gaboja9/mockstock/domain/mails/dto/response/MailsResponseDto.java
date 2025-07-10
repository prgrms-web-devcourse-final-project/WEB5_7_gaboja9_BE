package io.gaboja9.mockstock.domain.mails.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MailsResponseDto {

    private String subject;

    private String content;

    private boolean readStatus;

    private LocalDateTime receivedAt;
}
