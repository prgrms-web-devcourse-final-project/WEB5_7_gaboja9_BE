package io.gaboja9.mockstock.domain.notifications.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class HolidayApiResponseDto {

    private Response response;

    @Getter
    @NoArgsConstructor
    public static class Response {
        private Header header;
        private Body body;
    }

    @Getter
    @NoArgsConstructor
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Getter
    @NoArgsConstructor
    public static class Body {
        private Items items;
        private int numOfRows;
        private int pageNo;
        private int totalCount;
    }

    @Getter
    @NoArgsConstructor
    public static class Items {
        private List<HolidayItem> item;
    }

    @Getter
    @NoArgsConstructor
    public static class HolidayItem {
        private String dateKind;
        private String dateName;
        private String isHoliday;
        private int locdate;
        private int seq;
    }
}
