package com.tripagora.template.dto.request;

import com.tripagora.template.entity.Template;
import com.tripagora.template.entity.TemplateItinerary;
import com.tripagora.trip.entity.Trip;
import com.tripagora.trip.entity.TripItinerary;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record ItineraryItemRequest(
        @NotNull(message = "일자는 필수입니다.")
        Integer day,

        @NotBlank(message = "일정 장소는 공백일 수 없습니다.")
        @Size(max = 255, message = "일정 장소는 255자 이하여야 합니다.")
        String location,

        String content,

        @NotNull(message = "시작 시간은 필수입니다.")
        LocalTime startTime,

        @NotNull(message = "위도는 필수입니다.")
        Double latitude,

        @NotNull(message = "경도는 필수입니다.")
        Double longitude) {

    public TemplateItinerary toTemplateItinerary(Template template) {
        return TemplateItinerary.builder()
                .template(template)
                .day(day)
                .location(location)
                .content(content)
                .startTime(startTime)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }

    public TripItinerary toTripItinerary(Trip trip) {
        return TripItinerary.builder()
                .trip(trip)
                .day(day)
                .location(location)
                .content(content)
                .startTime(startTime)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }
}