package io.gaboja9.mockstock.domain.mails.mapper;

import io.gaboja9.mockstock.domain.mails.dto.response.MailsResponseDto;
import io.gaboja9.mockstock.domain.mails.entity.Mails;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class MailsMapper {

    public List<MailsResponseDto> toDto(List<Mails> mailsList) {

        if (mailsList == null) {
            return Collections.emptyList();
        }

        return mailsList.stream()
                .map(
                        mail ->
                                MailsResponseDto.builder()
                                        .subject(mail.getSubject())
                                        .content(mail.getContent())
                                        .readStatus(mail.isReadStatus())
                                        .receivedAt(mail.getCreatedAt())
                                        .build())
                .toList();
    }
}
