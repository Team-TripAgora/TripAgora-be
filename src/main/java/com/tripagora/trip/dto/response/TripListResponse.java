package com.tripagora.trip.dto.response;

import com.tripagora.trip.entity.Trip;
import org.springframework.data.domain.Slice;

import java.util.List;

public record TripListResponse(
        List<TripSummaryResponse> trips,
        boolean hasNext) {

    // 검색·내 여행·참가 여행·찜·가이드 프로필이 같은 모양의 여행 목록을 쓴다
    public static TripListResponse from(Slice<Trip> tripSlice) {
        List<TripSummaryResponse> trips = tripSlice.getContent().stream()
                .map(TripSummaryResponse::from)
                .toList();

        return new TripListResponse(trips, tripSlice.hasNext());
    }
}
