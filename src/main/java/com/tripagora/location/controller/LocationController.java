package com.tripagora.location.controller;

import com.tripagora.common.code.SuccessCode;
import com.tripagora.common.response.ApiResponse;
import com.tripagora.location.dto.response.LocationResponse;
import com.tripagora.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class LocationController {
    private final LocationService locationService;

    @GetMapping("/{roomId}/locations")
    public ApiResponse<List<LocationResponse>> getLocations(@AuthenticationPrincipal final long userId,
                                                            @PathVariable final long roomId) {
        List<LocationResponse> response = locationService.getRoomLocations(userId, roomId);
        return ApiResponse.success(SuccessCode.OK, response);
    }
}