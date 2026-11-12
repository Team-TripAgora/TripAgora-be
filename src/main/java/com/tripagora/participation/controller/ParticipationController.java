package com.tripagora.participation.controller;

import com.tripagora.common.code.SuccessCode;
import com.tripagora.common.response.ApiResponse;
import com.tripagora.participation.dto.response.ParticipationResponse;
import com.tripagora.participation.service.ParticipationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/participation")
@RequiredArgsConstructor
public class ParticipationController {
    private final ParticipationService participationService;

    @PostMapping("/trips/{tripId}")
    public ApiResponse<ParticipationResponse> applyForTrip(@AuthenticationPrincipal final long userId,
                                                              @PathVariable final long tripId) {
        ParticipationResponse response = participationService.applyForTrip(userId, tripId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @DeleteMapping("/trips/{tripId}")
    public ApiResponse<Void> cancelParticipation(@AuthenticationPrincipal final long userId,
                                                 @PathVariable final long tripId) {
        participationService.cancelParticipation(userId, tripId);
        return ApiResponse.success(SuccessCode.OK);
    }
}