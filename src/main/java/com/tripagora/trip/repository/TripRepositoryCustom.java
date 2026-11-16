package com.tripagora.trip.repository;

import com.tripagora.trip.dto.request.TripSearchRequest;
import com.tripagora.trip.entity.Trip;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDate;

public interface TripRepositoryCustom {
    // 모집 중이고 시작일이 오늘보다 뒤인 여행을 조건으로 검색한다
    Slice<Trip> search(TripSearchRequest request, LocalDate today, Pageable pageable);
}