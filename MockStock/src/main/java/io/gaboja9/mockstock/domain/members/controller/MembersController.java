package io.gaboja9.mockstock.domain.members.controller;

import io.gaboja9.mockstock.domain.members.dto.response.MemberInfoDto;
import io.gaboja9.mockstock.domain.members.dto.response.MyPageResponseDto;
import io.gaboja9.mockstock.domain.members.service.MembersService;
import io.gaboja9.mockstock.domain.portfolios.dto.response.PortfoliosResponseDto;
import io.gaboja9.mockstock.domain.portfolios.service.PortfoliosService;

import io.gaboja9.mockstock.domain.trades.dto.request.TradesRequestDto;
import io.gaboja9.mockstock.domain.trades.dto.response.TradesResponseDto;
import io.gaboja9.mockstock.domain.trades.service.TradesService;
import io.gaboja9.mockstock.global.exception.BaseException;
import io.gaboja9.mockstock.global.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/members/me")
@RequiredArgsConstructor
public class MembersController {

    private final PortfoliosService portfoliosService;
    private final MembersService membersService;
    private final TradesService tradesService;

    @GetMapping("/portfolios")
    public ResponseEntity<?> getPortfolios() {
        // TODO : Security 도입되면 현재 로그인한 유저 id를 불러오는 것으로 수정
        Long currentId = 1L;

        PortfoliosResponseDto portfolios = portfoliosService.getPortfolios(currentId);

        MemberInfoDto memberInfoDto = membersService.getMemberInfoDto(currentId, portfolios);

        return ResponseEntity.ok(
                MyPageResponseDto.<PortfoliosResponseDto>builder()
                        .memberInfoDto(memberInfoDto)
                        .data(portfolios)
                        .build()
        );
    }

    @GetMapping("/trades")
    public ResponseEntity<?> getTrades() {

        // TODO : Security 도입되면 현재 로그인한 유저 id를 불러오는 것으로 수정
        Long currentId = 1L;

        PortfoliosResponseDto portfolios = portfoliosService.getPortfolios(currentId);

        MemberInfoDto memberInfoDto = membersService.getMemberInfoDto(currentId, portfolios);

        List<TradesResponseDto> trades = tradesService.getTrades(currentId);

        return ResponseEntity.ok(
                MyPageResponseDto.<List<TradesResponseDto>>builder()
                        .memberInfoDto(memberInfoDto)
                        .data(trades)
                        .build()
        );
    }

    @GetMapping("/trades/search")
    public ResponseEntity<?> getTradesWithOption(@Valid TradesRequestDto dto) {

        // TODO : Security 도입되면 현재 로그인한 유저 id를 불러오는 것으로 수정
        Long currentId = 1L;

        if ((dto.getStockCode() == null || dto.getStockCode().isBlank()) &&
                (dto.getStockName() == null || dto.getStockName().isBlank())) {
            throw new BaseException(ErrorCode.INVALID_INPUT_VALUE, "stockCode 또는 stockName 중 하나는 필수입니다.");
        }

        PortfoliosResponseDto portfolios = portfoliosService.getPortfolios(currentId);

        MemberInfoDto memberInfoDto = membersService.getMemberInfoDto(currentId, portfolios);

        List<TradesResponseDto> trades = tradesService.getTradesWithOption(currentId, dto);

        return ResponseEntity.ok(
                MyPageResponseDto.<List<TradesResponseDto>>builder()
                        .memberInfoDto(memberInfoDto)
                        .data(trades)
                        .build()
        );
    }
}
