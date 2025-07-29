package io.gaboja9.mockstock.domain.ranks.service;

import io.gaboja9.mockstock.domain.members.entity.Members;
import io.gaboja9.mockstock.domain.members.repository.MembersRepository;
import io.gaboja9.mockstock.domain.notifications.scheduler.MarketTimeScheduler;
import io.gaboja9.mockstock.domain.payments.entity.PaymentHistory;
import io.gaboja9.mockstock.domain.payments.entity.PaymentStatus;
import io.gaboja9.mockstock.domain.payments.repository.PaymentHistoryRepository;
import io.gaboja9.mockstock.domain.portfolios.entity.Portfolios;
import io.gaboja9.mockstock.domain.portfolios.repository.PortfoliosRepository;
import io.gaboja9.mockstock.domain.ranks.dto.PaginationInfo;
import io.gaboja9.mockstock.domain.ranks.dto.RankingRequest;
import io.gaboja9.mockstock.domain.ranks.dto.RankingResponse;
import io.gaboja9.mockstock.domain.ranks.dto.RanksDto;
import io.gaboja9.mockstock.domain.ranks.entity.RanksType;
import io.gaboja9.mockstock.global.websocket.HantuWebSocketHandler;
import io.gaboja9.mockstock.global.websocket.dto.StockPrice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RanksService {

    private final MembersRepository membersRepository;
    private final HantuWebSocketHandler hantuWebSocketHandler;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final PortfoliosRepository portfoliosRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MarketTimeScheduler marketTimeScheduler;

    private static final String RANKING_KEY_PREFIX = "ranking:";
    private static final String LAST_UPDATE_KEY = "ranking:last_update";

    private boolean isMarketOpen() {
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();

        if (!marketTimeScheduler.isTradingDay()) {
            return false;
        }

        LocalTime marketOpen = LocalTime.of(9, 0);
        LocalTime marketClose = LocalTime.of(15, 30);

        return !currentTime.isBefore(marketOpen) && !currentTime.isAfter(marketClose);
    }

    @Scheduled(cron = "0 0,30 9-15 * * MON-FRI")
    public void updateRanksScheduled() {
        if (!isMarketOpen()) {
            log.info("시장 마감 시간입니다. 랭킹 업데이트를 건너뜁니다.");
            return;
        }

        log.info("랭킹 업데이트 시작: {}", LocalDateTime.now());
        updateAndCacheRanks();
        log.info("랭킹 업데이트 완료");
    }

    public void updateAndCacheRanks() {
        try {
            List<RanksDto> allRanks = calculateAllMemberRankings();

            cacheRankingByType(allRanks, RanksType.RETURN_RATE);
            cacheRankingByType(allRanks, RanksType.PROFIT);
            cacheRankingByType(allRanks, RanksType.ASSET);
            cacheRankingByType(allRanks, RanksType.BANKRUPTCY);

            redisTemplate.opsForValue().set(LAST_UPDATE_KEY, LocalDateTime.now().toString());
        } catch (Exception e) {
            log.error("랭킹 업데이트 중 오류 발생", e);
        }
    }

    private void cacheRankingByType(List<RanksDto> allRanks, RanksType ranksType) {
        List<RanksDto> sortedRanks = new ArrayList<>(allRanks);

        switch (ranksType) {
            case RETURN_RATE:
                sortedRanks.sort((a, b) -> Double.compare(b.getReturnRate(), a.getReturnRate()));
                break;
            case PROFIT:
                sortedRanks.sort((a, b) -> Double.compare(b.getTotalProfit(), a.getTotalProfit()));
                break;
            case ASSET:
                sortedRanks.sort((a, b) -> Long.compare(b.getTotalAsset(), a.getTotalAsset()));
                break;
            case BANKRUPTCY:
                sortedRanks.sort(
                        (a, b) -> Integer.compare(b.getBankruptcyCount(), a.getBankruptcyCount()));
                break;
        }

        assignRanks(sortedRanks);

        String key = RANKING_KEY_PREFIX + ranksType.name().toLowerCase();
        redisTemplate.opsForValue().set(key, sortedRanks, Duration.ofHours(1));
    }

    public RankingResponse getRankingWithPagination(Long memberId, RankingRequest request) {
        String key = RANKING_KEY_PREFIX + request.getRanksType().name().toLowerCase();

        @SuppressWarnings("unchecked")
        List<RanksDto> cachedRanks = (List<RanksDto>) redisTemplate.opsForValue().get(key);

        if (cachedRanks == null || cachedRanks.isEmpty()) {
            log.warn("캐시된 랭킹이 없습니다. 실시간 계산: {}", request.getRanksType());
            cachedRanks = calculateRealTimeRankingList(request.getRanksType());
        }

        return buildPaginatedRankingResponse(cachedRanks, memberId, request);
    }

    private RankingResponse buildPaginatedRankingResponse(
            List<RanksDto> allRanks, Long memberId, RankingRequest request) {
        // 상위 5명 추출
        List<RanksDto> topRankers = allRanks.stream().limit(5).collect(Collectors.toList());

        // 내 랭킹 찾기
        RanksDto myRanking =
                allRanks.stream()
                        .filter(ranking -> ranking.getMemberId().equals(memberId))
                        .findFirst()
                        .orElse(null);

        // 페이지네이션 처리
        int totalElements = allRanks.size();
        int totalPages = (int) Math.ceil((double) totalElements / request.getSize());
        int startIndex = request.getPage() * request.getSize();
        int endIndex = Math.min(startIndex + request.getSize(), totalElements);

        List<RanksDto> currentPageRanks = new ArrayList<>();
        if (startIndex < totalElements) {
            currentPageRanks = allRanks.subList(startIndex, endIndex);
        }

        // 페이지네이션 정보 생성
        PaginationInfo paginationInfo =
                PaginationInfo.builder()
                        .currentPage(request.getPage())
                        .pageSize(request.getSize())
                        .totalElements(totalElements)
                        .totalPages(totalPages)
                        .hasNext(request.getPage() < totalPages - 1)
                        .hasPrevious(request.getPage() > 0)
                        .build();

        // 마지막 업데이트 시간 조회
        LocalDateTime lastUpdated =
                (LocalDateTime) redisTemplate.opsForValue().get(LAST_UPDATE_KEY);
        if (lastUpdated == null) {
            lastUpdated = LocalDateTime.now();
        }

        return RankingResponse.builder()
                .topRankers(topRankers)
                .myRanking(myRanking)
                .rankers(currentPageRanks)
                .ranksType(request.getRanksType())
                .lastUpdated(lastUpdated)
                .pagination(paginationInfo)
                .build();
    }

    private List<RanksDto> calculateRealTimeRankingList(RanksType type) {
        List<RanksDto> allRanks = calculateAllMemberRankings();

        switch (type) {
            case RETURN_RATE:
                allRanks.sort((a, b) -> Double.compare(b.getReturnRate(), a.getReturnRate()));
                break;
            case PROFIT:
                allRanks.sort((a, b) -> Double.compare(b.getTotalProfit(), a.getTotalProfit()));
                break;
            case ASSET:
                allRanks.sort((a, b) -> Long.compare(b.getTotalAsset(), a.getTotalAsset()));
                break;
            case BANKRUPTCY:
                allRanks.sort(
                        (a, b) -> Integer.compare(b.getBankruptcyCount(), a.getBankruptcyCount()));
                break;
        }

        assignRanks(allRanks);
        return allRanks;
    }

    private void assignRanks(List<RanksDto> ranks) {
        for (int i = 0; i < ranks.size(); i++) {
            ranks.get(i).setRank(i + 1);
        }
    }

    private List<RanksDto> calculateAllMemberRankings() {
        List<Members> allMembers = membersRepository.findAll();

        return allMembers.stream().map(this::calculateMemberRanking).collect(Collectors.toList());
    }

    private RanksDto calculateMemberRanking(Members member) {
        long totalInvestment = calculateTotalInvestment(member.getId());

        long totalAsset = calculateTotalAsset(member);

        long totalProfit = totalAsset - totalInvestment;

        double returnRate =
                totalInvestment > 0 ? ((double) totalProfit / totalInvestment) * 100 : 0.0;

        return RanksDto.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .returnRate(Math.round(returnRate * 100.0) / 100.0) // 소수점 2자리
                .totalProfit(totalProfit)
                .bankruptcyCount(member.getBankruptcyCnt())
                .totalAsset(totalAsset)
                .totalInvestment(totalInvestment)
                .build();
    }

    // 총 투자원금 (기본금 + 충전금)
    private long calculateTotalInvestment(Long memberId) {
        long basicAmount = 30_000_000;

        long totalChargeAmount =
                paymentHistoryRepository
                        .findByMembersIdAndStatus(memberId, PaymentStatus.APPROVED)
                        .stream()
                        .mapToLong(PaymentHistory::getAmount)
                        .sum();

        return basicAmount + totalChargeAmount;
    }

    // 총 자산 (현금 + 보유 주식)

    private long calculateTotalAsset(Members member) {
        long cashBalance = member.getCashBalance();
        long stockValue = calculateStockValue(member.getId());

        return cashBalance + stockValue;
    }

    // 보유 주식 현재 가격

    private long calculateStockValue(Long memberId) {
        List<Portfolios> portfolios = portfoliosRepository.findByMembersId(memberId);

        return portfolios.stream()
                .mapToLong(
                        portfolio -> {
                            int currentPrice = getCurrentPriceOrNull(portfolio.getStockCode());
                            return (long) portfolio.getQuantity() * currentPrice;
                        })
                .sum();
    }

    private RankingResponse buildRankingResponse(
            List<RanksDto> allRanks, Long memberId, RanksType type) {

        List<RanksDto> topRankers = allRanks.stream().limit(5).toList();

        RanksDto myRanking =
                allRanks.stream()
                        .filter(ranking -> ranking.getMemberId().equals(memberId))
                        .findFirst()
                        .orElse(null);

        return RankingResponse.builder()
                .topRankers(topRankers)
                .myRanking(myRanking)
                .ranksType(type)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    private Integer getCurrentPriceOrNull(String stockCode) {
        StockPrice stockPrice = hantuWebSocketHandler.getLatestPrice(stockCode);
        if (stockPrice == null) {
            log.warn("현재 가격 정보를 불러올 수 없습니다: {}", stockCode);
            return null;
        }
        return stockPrice.getCurrentPrice();
    }

    public Integer getMemberReturnRateRank(Long memberId) {
        String key = RANKING_KEY_PREFIX + RanksType.RETURN_RATE.name().toLowerCase();

        @SuppressWarnings("unchecked")
        List<RanksDto> cachedRanks = (List<RanksDto>) redisTemplate.opsForValue().get(key);

        if (cachedRanks == null || cachedRanks.isEmpty()) {
            log.warn("수익률 기준 캐시 랭킹이 없습니다. 실시간 계산을 수행합니다.");
            cachedRanks = calculateRealTimeRankingList(RanksType.RETURN_RATE);
        }

        return cachedRanks.stream()
                .filter(ranking -> ranking.getMemberId().equals(memberId))
                .findFirst()
                .map(RanksDto::getRank)
                .orElse(null);
    }
}
