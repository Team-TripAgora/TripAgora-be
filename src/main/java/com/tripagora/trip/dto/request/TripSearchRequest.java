package com.tripagora.trip.dto.request;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

// 여행 검색 조건 (쿼리 파라미터). 모두 생략할 수 있다
public record TripSearchRequest(
        String keyword,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate searchStartDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate searchEndDate,

        List<Long> tagIds,

        List<Long> regionIds) {
}
