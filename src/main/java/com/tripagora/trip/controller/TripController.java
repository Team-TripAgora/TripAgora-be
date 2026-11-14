package com.tripagora.trip.controller;

import com.tripagora.common.code.SuccessCode;
import com.tripagora.common.response.ApiResponse;
import com.tripagora.itinerary.dto.request.ItineraryUpdateRequest;
import com.tripagora.itinerary.dto.response.ItinerariesResponse;
import com.tripagora.trip.dto.request.TripCreateRequest;
import com.tripagora.trip.dto.request.TripSearchRequest;
import com.tripagora.trip.dto.request.TripUpdateRequest;
import com.tripagora.trip.dto.response.CompletedTripListResponse;
import com.tripagora.trip.dto.response.TripCreateResponse;
import com.tripagora.trip.dto.response.TripDetailResponse;
import com.tripagora.trip.dto.response.TripListResponse;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.trip.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {
    private final TripService tripService;

    @PostMapping
    public ApiResponse<TripCreateResponse> createTrip(@AuthenticationPrincipal final long userId,
                                                            @RequestBody @Valid final TripCreateRequest request) {
        TripCreateResponse response = tripService.createTrip(userId, request);
        return ApiResponse.success(SuccessCode.CREATED, response);
    }

    @GetMapping("/{tripId}")
    public ApiResponse<TripDetailResponse> getTripDetails(@AuthenticationPrincipal final long userId,
                                                                @PathVariable final long tripId) {
        TripDetailResponse response = tripService.getTripDetails(userId, tripId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PatchMapping("/{tripId}")
    public ApiResponse<Void> updateTrip(@AuthenticationPrincipal final long userId,
                                           @PathVariable final long tripId,
                                           @RequestBody @Valid final TripUpdateRequest request) {
        tripService.updateTrip(userId, tripId, request);
        return ApiResponse.success(SuccessCode.OK);
    }

    @DeleteMapping("/{tripId}")
    public ApiResponse<Void> deleteTrip(@AuthenticationPrincipal final long userId,
                                           @PathVariable final long tripId) {
        tripService.deleteTrip(userId, tripId);
        return ApiResponse.success(SuccessCode.OK);
    }

    @GetMapping("/{tripId}/itineraries")
    public ApiResponse<ItinerariesResponse> getItineraries(@AuthenticationPrincipal final long userId,
                                                           @PathVariable final long tripId) {
        ItinerariesResponse response = tripService.getItineraries(tripId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PutMapping("/{tripId}/itineraries")
    public ApiResponse<Void> updateItineraries(@AuthenticationPrincipal final long userId,
                                               @PathVariable final long tripId,
                                               @RequestBody @Valid final ItineraryUpdateRequest request) {
        tripService.updateItineraries(userId, tripId, request);
        return ApiResponse.success(SuccessCode.OK);
    }

    @GetMapping("/me")
    public ApiResponse<TripListResponse> getMyTrips(@AuthenticationPrincipal final long userId,
                                                          @RequestParam(required = false) List<TripStatus> statuses,
                                                          @PageableDefault(size = 10) Pageable pageable) {
        TripListResponse response = tripService.getMyTrips(userId, statuses, pageable);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @GetMapping("/participating")
    public ApiResponse<TripListResponse> getParticipatingTrips(@AuthenticationPrincipal final long userId,
                                                                     @RequestParam(required = false) List<TripStatus> statuses,
                                                                     @PageableDefault(size = 10) Pageable pageable) {
        TripListResponse response = tripService.getParticipatingTrips(userId, statuses, pageable);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @GetMapping("/completed")
    public ApiResponse<CompletedTripListResponse> getCompletedTrips(@AuthenticationPrincipal final long userId,
                                                                          @PageableDefault(size = 10) Pageable pageable) {

        CompletedTripListResponse response = tripService.getCompletedTrips(userId, pageable);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PostMapping("/{tripId}/close")
    public ApiResponse<Void> closeRecruitment(@AuthenticationPrincipal final long userId,
                                              @PathVariable final long tripId) {
        tripService.closeRecruitment(userId, tripId);
        return ApiResponse.success(SuccessCode.OK);
    }

    @GetMapping("/search")
    public ApiResponse<TripListResponse> searchTrips(@ModelAttribute TripSearchRequest request,
                                                           @PageableDefault(size = 10) Pageable pageable) {
        TripListResponse response = tripService.searchTrips(request, pageable);
        return ApiResponse.success(SuccessCode.OK, response);
    }
}