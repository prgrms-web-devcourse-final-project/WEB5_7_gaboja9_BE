package io.gaboja9.mockstock.domain.portfolios.service;

import io.gaboja9.mockstock.domain.members.entity.Members;
import io.gaboja9.mockstock.domain.members.exception.NotFoundMemberException;
import io.gaboja9.mockstock.domain.members.repository.MembersRepository;
import io.gaboja9.mockstock.domain.portfolios.dto.PortfoliosSummary;
import io.gaboja9.mockstock.domain.portfolios.dto.response.PortfolioResponseDto;
import io.gaboja9.mockstock.domain.portfolios.dto.response.PortfoliosResponseDto;
import io.gaboja9.mockstock.domain.portfolios.entity.Portfolios;
import io.gaboja9.mockstock.domain.portfolios.exception.NotFoundPortfolioException;
import io.gaboja9.mockstock.domain.portfolios.mapper.PortfoliosMapper;
import io.gaboja9.mockstock.domain.portfolios.repository.PortfoliosRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PortfoliosService {

    private final PortfoliosRepository portfoliosRepository;
    private final PortfoliosMapper portfoliosMapper;
    private final MembersRepository membersRepository;

    @Transactional(readOnly = true)
    public PortfoliosResponseDto getPortfolios(Long memberId) {
        List<Portfolios> portfoliosList = portfoliosRepository.findByMembersId(memberId);

        List<PortfolioResponseDto> dtoList = new ArrayList<>();
        for (Portfolios p : portfoliosList) {
            dtoList.add(portfoliosMapper.toDto(p));
        }

        Members member =
                membersRepository
                        .findById(memberId)
                        .orElseThrow(() -> new NotFoundMemberException(memberId));

        int cashBalance = member.getCashBalance();
        int totalInvestedAmount = member.getTotalInvestedAmount(); // 총 투입 자금

        if (totalInvestedAmount == 0) {
            totalInvestedAmount = 30_000_000;
        }

        PortfoliosSummary summary = calculateSummary(dtoList, cashBalance, totalInvestedAmount);

        return PortfoliosResponseDto.builder()
                .cashBalance(cashBalance)
                .totalEvaluationAmount(summary.getTotalEvaluationAmount())
                .totalProfit(summary.getTotalProfit())
                .totalProfitRate(summary.getTotalProfitRate())
                .portfolios(dtoList)
                .build();
    }

    public PortfoliosSummary calculateSummary(
            List<PortfolioResponseDto> dtoList, int cashBalance, int totalInvestedAmount) {
        int totalStockEvaluationAmount = 0;

        // 주식 포트폴리오 계산
        for (PortfolioResponseDto dto : dtoList) {
            totalStockEvaluationAmount += dto.getEvaluationAmount();
        }

        int totalCurrentAssets = cashBalance + totalStockEvaluationAmount;

        int totalProfit = totalCurrentAssets - totalInvestedAmount;

        double totalProfitRate = 0;
        if (totalInvestedAmount == 0) {
            totalProfitRate = 0.00;
        } else {
            totalProfitRate =
                    Math.round((double) totalProfit / totalInvestedAmount * 10000.0) / 100.0;
        }

        return PortfoliosSummary.builder()
                .totalEvaluationAmount(totalStockEvaluationAmount)
                .totalProfit(totalProfit)
                .totalProfitRate(totalProfitRate)
                .build();
    }

    @Transactional
    public void remove(Long memberId) {

        Members findMember =
                membersRepository
                        .findById(memberId)
                        .orElseThrow(() -> new NotFoundMemberException(memberId));

        portfoliosRepository.deleteByMembersId(findMember.getId());
    }

    @Transactional
    public void updateForBuy(
            Long memberId, String stockCode, String stockName, int quantity, int price) {

        Members member =
                membersRepository
                        .findById(memberId)
                        .orElseThrow(() -> new NotFoundMemberException(memberId));

        Optional<Portfolios> optionalPortfolios =
                portfoliosRepository.findByMembersIdAndStockCodeWithLock(memberId, stockCode);

        if (optionalPortfolios.isPresent()) {
            // 기존에 매수한 동일 주식이 있는 경우 평균단가 업데이트
            Portfolios portfolio = optionalPortfolios.get();
            portfolio.updateForBuy(quantity, price);
            portfoliosRepository.save(portfolio);
        } else {
            // 아닌 경우 새로운 포트폴리오 생성
            Portfolios newPortfolio = new Portfolios(stockCode, stockName, quantity, price, member);
            portfoliosRepository.save(newPortfolio);
        }
    }

    @Transactional
    public void updateForSell(Long memberId, String stockCode, int quantity) {
        membersRepository
                .findById(memberId)
                .orElseThrow(() -> new NotFoundMemberException(memberId));

        Portfolios portfolio =
                portfoliosRepository
                        .findByMembersIdAndStockCodeWithLock(memberId, stockCode)
                        .orElseThrow(() -> new NotFoundPortfolioException());

        portfolio.updateForSell(quantity);

        if (portfolio.getQuantity() == 0) {
            portfoliosRepository.delete(portfolio);
        }
    }
}
