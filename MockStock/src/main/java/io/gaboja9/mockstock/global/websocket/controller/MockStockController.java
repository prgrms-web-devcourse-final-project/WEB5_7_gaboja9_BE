package io.gaboja9.mockstock.global.websocket.controller;

import io.gaboja9.mockstock.global.websocket.dto.StockPriceDto;
import io.gaboja9.mockstock.global.websocket.service.MockStockService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MockStockController {

    private final MockStockService mockStockService;

    @PostMapping("/mockPrice/{stockCode}")
    public List<StockPriceDto> sendMockStockPrice(@PathVariable String stockCode) {
        return mockStockService.sendMockPrices(stockCode);
    }
}
