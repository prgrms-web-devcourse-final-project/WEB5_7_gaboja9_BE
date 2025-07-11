package io.gaboja9.mockstock.domain.members.controller;

import io.gaboja9.mockstock.domain.mails.dto.response.MailsResponseDto;
import io.gaboja9.mockstock.domain.mails.service.MailsService;
import io.gaboja9.mockstock.domain.members.dto.request.MemosCreateRequestDto;
import io.gaboja9.mockstock.domain.members.dto.response.MemberInfoDto;
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

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/members/me")
@RequiredArgsConstructor
public class MembersController {

    private final PortfoliosService portfoliosService;
    private final MembersService membersService;
    private final TradesService tradesService;
    private final MailsService mailsService;

    @GetMapping("/info")
    public ResponseEntity<MemberInfoDto> getMemberInfo() {
        Long currentId = 1L; // TODO: Security 도입 시 현재 로그인한 유저 ID 사용

        PortfoliosResponseDto portfolios = portfoliosService.getPortfolios(currentId);

        MemberInfoDto memberInfoDto = membersService.getMemberInfoDto(currentId, portfolios);

        return ResponseEntity.ok(memberInfoDto);
    }

    @GetMapping("/portfolios")
    public ResponseEntity<?> getPortfolios() {
        // TODO : Security 도입되면 현재 로그인한 유저 id를 불러오는 것으로 수정
        Long currentId = 1L;

        PortfoliosResponseDto portfolios = portfoliosService.getPortfolios(currentId);

        return ResponseEntity.ok(portfolios);
    }

    @GetMapping("/trades")
    public ResponseEntity<?> getTrades() {

        // TODO : Security 도입되면 현재 로그인한 유저 id를 불러오는 것으로 수정
        Long currentId = 1L;

        List<TradesResponseDto> trades = tradesService.getTrades(currentId);

        return ResponseEntity.ok(trades);
    }

    @GetMapping("/trades/search")
    public ResponseEntity<?> getTradesWithOption(@Valid TradesRequestDto dto) {

        // TODO : Security 도입되면 현재 로그인한 유저 id를 불러오는 것으로 수정
        Long currentId = 1L;

        if ((dto.getStockCode() == null || dto.getStockCode().isBlank())
                && (dto.getStockName() == null || dto.getStockName().isBlank())) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE, "stockCode 또는 stockName 중 하나는 필수입니다.");
        }

        List<TradesResponseDto> trades = tradesService.getTradesWithOption(currentId, dto);

        return ResponseEntity.ok(trades);
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

    @GetMapping("/mails")
    public ResponseEntity<?> getMails(@RequestParam(required = false) Boolean unread) {

        // TODO : Security 도입되면 현재 로그인한 유저 id를 불러오는 것으로 수정
        Long currentId = 1L;

        if (unread == null) {
            List<MailsResponseDto> allMails = mailsService.getAllMails(currentId);
            return ResponseEntity.ok(allMails);
        }

        List<MailsResponseDto> mailsByStatus = mailsService.getMailsByReadStatus(currentId, unread);
        return ResponseEntity.ok(mailsByStatus);
    }

    @PostMapping("/bankruptcy")
    public ResponseEntity<?> declareBankruptcy() {

        // TODO : Security 도입되면 현재 로그인한 유저 id를 불러오는 것으로 수정
        Long currentId = 1L;

        membersService.processBankruptcy(currentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bankruptcy")
    public ResponseEntity<?> getBankruptcy() {

        // TODO : Security 도입되면 현재 로그인한 유저 id를 불러오는 것으로 수정
        Long currentId = 1L;

        int bankruptcyCnt = membersService.getBankruptcyCnt(currentId);
        return ResponseEntity.ok(bankruptcyCnt);
    }
}
