package com.tripagora.trip.repository;

import com.tripagora.trip.dto.request.TripSearchRequest;
import com.tripagora.trip.entity.Trip;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface TripRepositoryCustom {
    Slice<Trip> search(TripSearchRequest request, Pageable pageable);
}