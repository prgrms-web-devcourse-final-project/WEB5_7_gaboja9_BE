package io.gaboja9.mockstock.domain.notifications.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

@SpringBootTest
class HolidayServiceTest {

    @Autowired private HolidayService holidayService;

    @Test
    void 평일은_공휴일이_아니다() {
        // given
        LocalDate target = LocalDate.of(2025, 1, 2);

        // when
        boolean result = holidayService.isHoliday(target);

        // then
        assertFalse(result);
    }

    @Test
    void 설날은_공휴일이다() {
        // given
        LocalDate target = LocalDate.of(2025, 1, 29);

        // when
        boolean result = holidayService.isHoliday(target);

        // then
        assertTrue(result);
    }

    @Test
    void 대통령_선거일은_공휴일이다() {
        // given
        LocalDate target = LocalDate.of(2025, 6, 3);

        // when
        boolean result = holidayService.isHoliday(target);

        // then
        assertTrue(result);
    }

    @Test
    void 삼일절은_공휴일이다() {
        // given
        LocalDate target = LocalDate.of(2025, 3, 1);

        // when
        boolean result = holidayService.isHoliday(target);

        // then
        assertTrue(result);
    }
}
