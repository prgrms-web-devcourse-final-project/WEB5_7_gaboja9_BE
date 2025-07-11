package io.gaboja9.mockstock.domain.mails.service;

import io.gaboja9.mockstock.domain.mails.dto.response.MailsResponseDto;
import io.gaboja9.mockstock.domain.mails.entity.Mails;
import io.gaboja9.mockstock.domain.mails.mapper.MailsMapper;
import io.gaboja9.mockstock.domain.mails.repository.MailsRepository;
import io.gaboja9.mockstock.domain.members.entity.Members;
import io.gaboja9.mockstock.domain.members.exception.NotFoundMemberException;
import io.gaboja9.mockstock.domain.members.repository.MembersRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MailsService {

    private final MailsRepository mailsRepository;

    private final MembersRepository membersRepository;

    private final MailsMapper mailsMapper;

    @Transactional(readOnly = true)
    public List<MailsResponseDto> getAllMails(Long memberId) {

        Members findMember =
                membersRepository
                        .findById(memberId)
                        .orElseThrow(() -> new NotFoundMemberException(memberId));

        List<Mails> mailsList = mailsRepository.findByMembersId(findMember.getId());

        return mailsMapper.toDto(mailsList);
    }

    @Transactional(readOnly = true)
    public List<MailsResponseDto> getMailsByReadStatus(Long memberId, boolean readStatus) {

        Members findMember =
                membersRepository
                        .findById(memberId)
                        .orElseThrow(() -> new NotFoundMemberException(memberId));

        List<Mails> mailsList =
                mailsRepository.findByMembersIdAndReadStatus(findMember.getId(), readStatus);

        return mailsMapper.toDto(mailsList);
    }
}
