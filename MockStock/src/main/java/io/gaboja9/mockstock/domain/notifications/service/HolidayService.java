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

            if (response == null) {
                return false;
            }

            if (response.getResponse() == null) {
                return false;
            }

            if (response.getResponse().getBody() == null) {
                return false;
            }

            if (response.getResponse().getBody().getItems() == null) {
                return false;
            }

            int targetDate = Integer.parseInt(date.format(DateTimeFormatter.ofPattern("yyyyMMdd")));

            boolean result =
                    response.getResponse().getBody().getItems().getItem().stream()
                            .anyMatch(
                                    item ->
                                            "Y".equals(item.getIsHoliday())
                                                    && item.getLocdate() == targetDate);

            return result;

        } catch (Exception e) {
            log.error("=== 공휴일 확인 중 오류 발생: {} ===", date, e);
            return false;
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

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(sb.toString(), HolidayApiResponseDto.class);

        } catch (Exception e) {
            log.error("API 호출 실패", e);
            return null;
        }
    }
}
