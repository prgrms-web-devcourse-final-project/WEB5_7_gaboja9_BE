package io.gaboja9.mockstock.domain.members.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import io.gaboja9.mockstock.domain.members.dto.request.MemosCreateRequestDto;
import io.gaboja9.mockstock.domain.members.dto.response.MemberInfoDto;
import io.gaboja9.mockstock.domain.members.entity.Members;
import io.gaboja9.mockstock.domain.members.exception.NotFoundMemberException;
import io.gaboja9.mockstock.domain.members.repository.MembersRepository;
import io.gaboja9.mockstock.domain.portfolios.dto.response.PortfoliosResponseDto;
import io.gaboja9.mockstock.domain.ranks.service.RanksService;
import io.gaboja9.mockstock.domain.trades.repository.TradesRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class MembersServiceTest {

    @InjectMocks private MembersService membersService;

    @Mock private MembersRepository membersRepository;

    @Mock private TradesRepository tradesRepository;

    @Mock private RanksService ranksService;

    @Test
    void getMemberInfoDto_정상() {
        // given
        Long memberId = 1L;

        Members member =
                new Members(
                        memberId,
                        "test@example.com",
                        "testUser",
                        "google",
                        "test.png",
                        5000,
                        0,
                        LocalDateTime.now().minusDays(15));

        PortfoliosResponseDto portfolios =
                PortfoliosResponseDto.builder()
                        .totalProfit(10000)
                        .totalEvaluationAmount(50000)
                        .cashBalance(20000)
                        .portfolios(null)
                        .build();

        given(membersRepository.findById(memberId)).willReturn(Optional.of(member));
        given(tradesRepository.countByMembersId(memberId)).willReturn(5);

        // when
        MemberInfoDto result = membersService.getMemberInfoDto(memberId, portfolios);

        // then
        assertThat(result.getNickname()).isEqualTo("testUser");
        assertThat(result.getProfileImage()).isEqualTo("test.png");
        assertThat(result.getTotalProfit()).isEqualTo(10000);
        assertThat(result.getTotalEvaluationAmount()).isEqualTo(50000);
        assertThat(result.getTradeCnt()).isEqualTo(5);
        assertThat(result.getPeriod()).isEqualTo(15);
    }

    @Test
    void getMemberInfoDto_유저없음_예외발생() {
        // given
        Long memberId = 999L;
        PortfoliosResponseDto dummyPortfolios =
                PortfoliosResponseDto.builder()
                        .totalProfit(0)
                        .totalEvaluationAmount(0)
                        .cashBalance(0)
                        .portfolios(null)
                        .build();

        given(membersRepository.findById(memberId)).willReturn(Optional.empty());

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(
                NotFoundMemberException.class,
                () -> membersService.getMemberInfoDto(memberId, dummyPortfolios));
    }

    @Test
    public void createMemo_정상() {
        // given
        Long memberId = 1L;

        Members member =
                new Members(
                        memberId,
                        "test@example.com",
                        "testUser",
                        "google",
                        "test.png",
                        5000,
                        0,
                        LocalDateTime.now());

        MemosCreateRequestDto dto = MemosCreateRequestDto.builder().memo("테스트 메모").build();

        given(membersRepository.findById(memberId)).willReturn(Optional.of(member));

        // when
        membersService.createMemo(memberId, dto);

        // then
        assertThat(member.getMemo()).isEqualTo("테스트 메모");
        then(membersRepository).should().findById(memberId);
    }

    @Test
    public void createMemo_유저없음예외() {
        // given
        Long memberId = 1L;
        MemosCreateRequestDto dto = MemosCreateRequestDto.builder().memo("테스트 메모").build();
        given(membersRepository.findById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> membersService.createMemo(memberId, dto))
                .isInstanceOf(NotFoundMemberException.class)
                .hasMessageContaining(memberId.toString());
    }

    @Test
    public void getMemo_정상() {
        // given
        Long memberId = 1L;

        Members member =
                new Members(
                        memberId,
                        "test@example.com",
                        "testUser",
                        "google",
                        "test.png",
                        5000,
                        0,
                        LocalDateTime.now());

        MemosCreateRequestDto dto = MemosCreateRequestDto.builder().memo("테스트 메모").build();

        given(membersRepository.findById(memberId)).willReturn(Optional.of(member));

        // when
        membersService.createMemo(memberId, dto);
        String memo = membersService.getMemo(memberId);

        // then
        assertThat(dto.getMemo()).isEqualTo(memo);
        then(membersRepository).should(times(2)).findById(memberId);
    }
}
