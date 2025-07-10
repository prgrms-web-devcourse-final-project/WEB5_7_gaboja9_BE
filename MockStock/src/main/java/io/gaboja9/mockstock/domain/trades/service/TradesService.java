package io.gaboja9.mockstock.domain.trades.service;

import io.gaboja9.mockstock.domain.trades.dto.request.TradesRequestDto;
import io.gaboja9.mockstock.domain.trades.dto.response.TradesResponseDto;
import io.gaboja9.mockstock.domain.trades.entity.Trades;
import io.gaboja9.mockstock.domain.trades.mapper.TradesMapper;
import io.gaboja9.mockstock.domain.trades.repository.TradesRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TradesService {

    private final TradesRepository tradesRepository;
    private final TradesMapper tradesMapper;

    @Transactional(readOnly = true)
    public List<TradesResponseDto> getTradesWithOption(Long membersId, TradesRequestDto dto) {
        String stockCode = dto.getStockCode();
        String stockName = dto.getStockName();
        LocalDate startDate = dto.getStartDate();
        LocalDate endDate = dto.getEndDate();
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59, 999_999_999);

        List<Trades> tradesList =
                tradesRepository.findByStockCodeOrStockNameAndCreatedAtBetween(
                        stockCode, stockName, startDateTime, endDateTime, membersId);
        List<TradesResponseDto> dtoList = new ArrayList<>();

        for (Trades t : tradesList) {
            TradesResponseDto responseDto = tradesMapper.toDto(t);
            dtoList.add(responseDto);
        }
        return dtoList;
    }

    @Transactional(readOnly = true)
    public List<TradesResponseDto> getTrades(Long membersId) {
        List<Trades> tradesList = tradesRepository.findByMembersId(membersId);
        List<TradesResponseDto> dtoList = new ArrayList<>();
        for (Trades t : tradesList) {
            TradesResponseDto responseDto = tradesMapper.toDto(t);
            dtoList.add(responseDto);
        }
        return dtoList;
    }
}
