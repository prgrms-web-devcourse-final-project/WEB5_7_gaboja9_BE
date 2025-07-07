package io.gaboja9.mockstock.domain.members.controller;

import io.gaboja9.mockstock.domain.members.dto.response.MemberInfoDto;
import io.gaboja9.mockstock.domain.members.service.MembersService;
import io.gaboja9.mockstock.domain.portfolios.dto.response.PortfoliosResponseDto;
import io.gaboja9.mockstock.domain.portfolios.service.PortfoliosService;
import io.gaboja9.mockstock.domain.members.dto.response.MyPageResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/members/me")
@RequiredArgsConstructor
public class MembersController {

    private final PortfoliosService portfoliosService;
    private final MembersService membersService;

    @GetMapping("/portfolios")
    public MyPageResponseDto<PortfoliosResponseDto> getPortfolios() {
        // TODO : Security 도입되면 현재 로그인한 유저 id를 불러오는 것으로 수정
        Long currentId = 1L;

        PortfoliosResponseDto portfolios = portfoliosService.getPortfolios(currentId);

        MemberInfoDto memberInfoDto = membersService.getMemberInfoDto(currentId, portfolios);

        return MyPageResponseDto.<PortfoliosResponseDto>builder()
                .memberInfoDto(memberInfoDto)
                .data(portfolios)
                .build();
    }
}
