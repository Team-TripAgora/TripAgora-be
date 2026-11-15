package com.tripagora.trip.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tripagora.trip.entity.Trip;

import java.time.LocalDate;
import java.util.List;

public record TripSummaryResponse(
        Long tripId,
        String title,
        String firstImageUrl,
        List<Long> regionIds,
        Integer maxParticipants,
        Integer currentParticipants,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate,
        String status,
        Long roomId) {

    // 여행 목록의 한 줄. 대표 사진은 첫 사진, 채팅방 id는 모집을 마감한 뒤에만 있다
    public static TripSummaryResponse from(Trip trip) {
        String firstImageUrl = trip.getImages().isEmpty() ? null : trip.getImages().get(0).getImageUrl();

        List<Long> regionIds = trip.getRegions().stream()
                .map(tripRegion -> tripRegion.getRegion().getId())
                .toList();

        Long roomId = (trip.getRoom() == null) ? null : trip.getRoom().getId();

        return new TripSummaryResponse(
                trip.getId(),
                trip.getTitle(),
                firstImageUrl,
                regionIds,
                trip.getMaxParticipants(),
                trip.getCurrentParticipants(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getStatus().name(),
                roomId
        );
    }
}
