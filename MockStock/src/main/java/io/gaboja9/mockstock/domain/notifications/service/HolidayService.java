package io.gaboja9.mockstock.domain.notifications.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.gaboja9.mockstock.domain.notifications.dto.HolidayApiResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HolidayService {

    private final RestTemplate restTemplate;

    @Value("${holiday-api.base-url}")
    private String baseUrl;

    @Value("${holiday-api.service-key}")
    private String serviceKey;

    public boolean isHoliday(LocalDate date) {
        try {
            log.info("=== 공휴일 확인 시작: {} ===", date);

            HolidayApiResponseDto response = getHolidays(date.getYear(), date.getMonthValue());

            if (!isValidResponse(response)) {
                log.info("=== 공휴일 정보 없음 또는 API 응답 오류: {} ===", date);
                return false;
            }

            List<HolidayApiResponseDto.HolidayItem> holidays = extractHolidayItems(response);

            if (holidays.isEmpty()) {
                log.info("=== 해당 월({}/{})에 공휴일 없음 ===", date.getYear(), date.getMonthValue());
                return false;
            }

            int targetDate = Integer.parseInt(date.format(DateTimeFormatter.ofPattern("yyyyMMdd")));

            boolean result =
                    response.getResponse().getBody().getItems().getItem().stream()
                            .anyMatch(
                                    item ->
                                            "Y".equals(item.getIsHoliday())
                                                    && item.getLocdate() == targetDate);

            if (result) {
                log.info("=== 공휴일입니다: {} ===", date);
            } else {
                log.info("=== 평일입니다: {} ===", date);
            }

            return result;

        } catch (Exception e) {
            log.error("=== 공휴일 확인 중 오류 발생: {} ===", date, e);
            return false;
        }
    }

    private boolean isValidResponse(HolidayApiResponseDto response) {
        return response != null
                && response.getResponse() != null
                && response.getResponse().getBody() != null;
    }

    private List<HolidayApiResponseDto.HolidayItem> extractHolidayItems(
            HolidayApiResponseDto response) {
        try {
            var body = response.getResponse().getBody();

            // items가 null인 경우
            if (body.getItems() == null) {
                log.info("=== API 응답에 items가 없음 (해당 월에 공휴일 없음) ===");
                return List.of();
            }

            // item 리스트가 null인 경우
            if (body.getItems().getItem() == null) {
                log.info("=== API 응답에 item 리스트가 없음 (해당 월에 공휴일 없음) ===");
                return List.of();
            }

            return body.getItems().getItem();

        } catch (Exception e) {
            log.warn("=== 공휴일 목록 추출 중 오류 발생, 빈 목록 반환 ===", e);
            return List.of();
        }
    }

    private HolidayApiResponseDto getHolidays(int year, int month) {
        try {
            String url =
                    String.format(
                            "%s/getRestDeInfo?serviceKey=%s&solYear=%d&solMonth=%02d&_type=json",
                            baseUrl, serviceKey, year, month);

            URL apiUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-type", "application/json");

            BufferedReader rd;
            if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
                rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } else {
                rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            }

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            rd.close();
            conn.disconnect();

            String responseBody = sb.toString();
            log.debug("=== API 원본 응답: {} ===", responseBody);

            String cleanedResponse = responseBody.replace("\"items\":\"\"", "\"items\":null");
            log.debug("=== API 정리된 응답: {} ===", cleanedResponse);

            ObjectMapper mapper = new ObjectMapper();

            return mapper.readValue(cleanedResponse, HolidayApiResponseDto.class);
        } catch (Exception e) {
            log.error("API 호출 실패", e);
            return null;
        }
    }
}
