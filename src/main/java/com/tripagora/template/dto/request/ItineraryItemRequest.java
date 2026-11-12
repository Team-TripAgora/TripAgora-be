package com.tripagora.template.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record ItineraryItemRequest(
        @NotNull(message = "일자는 필수입니다.")
        Integer day,

        @NotBlank(message = "일정 장소는 공백일 수 없습니다.")
        String location,

        String content,

        @NotNull(message = "시작 시간은 필수입니다.")
        LocalTime startTime,

        @NotNull(message = "위도는 필수입니다.")
        Double latitude,

        @NotNull(message = "경도는 필수입니다.")
        Double longitude) {}