package io.gaboja9.mockstock.domain.members.controller;

import io.gaboja9.mockstock.domain.members.dto.request.MemosCreateRequestDto;
import io.gaboja9.mockstock.domain.members.dto.response.MemberInfoDto;
import io.gaboja9.mockstock.domain.members.dto.response.MyPageResponseDto;
import io.gaboja9.mockstock.domain.members.service.MembersService;
import io.gaboja9.mockstock.domain.portfolios.dto.response.PortfoliosResponseDto;
import io.gaboja9.mockstock.domain.portfolios.service.PortfoliosService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/memos")
    public ResponseEntity<?> createMemos(@Valid @RequestBody MemosCreateRequestDto dto) {

        // TODO : Security 도입되면 현재 로그인한 유저 id를 불러오는 것으로 수정
        Long currentId = 1L;

        membersService.createMemo(currentId, dto);

        return ResponseEntity.status(HttpStatus.CREATED).body("메모 저장 완료");
    }

    @GetMapping("/memos")
    public ResponseEntity<?> getMemos() {

        // TODO : Security 도입되면 현재 로그인한 유저 id를 불러오는 것으로 수정
        Long currentId = 1L;

        String memo = membersService.getMemo(currentId);

        return ResponseEntity.ok(memo);
    }
}
