package io.gaboja9.mockstock.domain.mails.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gaboja9.mockstock.domain.mails.dto.response.MailsResponseDto;
import io.gaboja9.mockstock.domain.mails.entity.Mails;
import io.gaboja9.mockstock.domain.mails.mapper.MailsMapper;
import io.gaboja9.mockstock.domain.mails.repository.MailsRepository;
import io.gaboja9.mockstock.domain.members.entity.Members;
import io.gaboja9.mockstock.domain.members.exception.NotFoundMemberException;
import io.gaboja9.mockstock.domain.members.repository.MembersRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class MailsServiceTest {

    @Mock private MailsRepository mailsRepository;

    @Mock private MembersRepository membersRepository;

    @Mock private MailsMapper mailsMapper;

    @InjectMocks private MailsService mailsService;

    private Members testMember;
    private Mails testMail;
    private Mails testMail2;
    private MailsResponseDto testDto;
    private MailsResponseDto testDto2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testMember =
                new Members(
                        1L,
                        "test@example.com",
                        "testUser",
                        "google",
                        "test.png",
                        5000,
                        0,
                        LocalDateTime.now());

        testMail =
                new Mails("Test Subject", "Test Content", false, LocalDateTime.now(), testMember);
        testMail2 =
                new Mails("Test Subject2", "Test Content2", true, LocalDateTime.now(), testMember);

        testDto =
                MailsResponseDto.builder()
                        .subject("Test Subject")
                        .content("Test Content")
                        .readStatus(false)
                        .receivedAt(LocalDateTime.now())
                        .build();

        testDto2 =
                MailsResponseDto.builder()
                        .subject("Test Subject2")
                        .content("Test Content2")
                        .readStatus(true)
                        .receivedAt(LocalDateTime.now())
                        .build();
    }

    @Test
    void getAllMails_정상동작() {
        Long memberId = 1L;

        when(membersRepository.findById(memberId)).thenReturn(Optional.of(testMember));
        when(mailsRepository.findByMembersId(testMember.getId()))
                .thenReturn(List.of(testMail, testMail2));
        when(mailsMapper.toDto(List.of(testMail, testMail2)))
                .thenReturn(List.of(testDto, testDto2));

        List<MailsResponseDto> result = mailsService.getAllMails(memberId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Test Subject", result.get(0).getSubject());

        verify(membersRepository, times(1)).findById(memberId);
        verify(mailsRepository, times(1)).findByMembersId(testMember.getId());
        verify(mailsMapper, times(1)).toDto(anyList());
    }

    @Test
    void getAllMails_유저없음예외() {
        Long memberId = 2L;
        when(membersRepository.findById(memberId)).thenReturn(Optional.empty());

        assertThrows(NotFoundMemberException.class, () -> mailsService.getAllMails(memberId));

        verify(membersRepository, times(1)).findById(memberId);
        verifyNoMoreInteractions(mailsRepository);
        verifyNoMoreInteractions(mailsMapper);
    }

    @Test
    void getMailsByReadStatus_정상동작() {
        Long memberId = 1L;
        boolean readStatus = true;

        when(membersRepository.findById(memberId)).thenReturn(Optional.of(testMember));
        when(mailsRepository.findByMembersIdAndReadStatus(testMember.getId(), readStatus))
                .thenReturn(List.of(testMail2));
        when(mailsMapper.toDto(List.of(testMail2))).thenReturn(List.of(testDto2));

        List<MailsResponseDto> result = mailsService.getMailsByReadStatus(memberId, readStatus);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isReadStatus());

        verify(membersRepository, times(1)).findById(memberId);
        verify(mailsRepository, times(1))
                .findByMembersIdAndReadStatus(testMember.getId(), readStatus);
        verify(mailsMapper, times(1)).toDto(anyList());
    }

    @Test
    void getMailsByReadStatus_유저없음예외() {
        Long memberId = 2L;
        when(membersRepository.findById(memberId)).thenReturn(Optional.empty());

        assertThrows(
                NotFoundMemberException.class,
                () -> mailsService.getMailsByReadStatus(memberId, true));

        verify(membersRepository, times(1)).findById(memberId);
        verifyNoMoreInteractions(mailsRepository);
        verifyNoMoreInteractions(mailsMapper);
    }
}
