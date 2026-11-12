package com.tripagora.session.controller;

import com.tripagora.common.code.SuccessCode;
import com.tripagora.common.response.ApiResponse;
import com.tripagora.session.dto.request.SessionCreateRequest;
import com.tripagora.session.dto.request.SessionSearchRequest;
import com.tripagora.session.dto.request.SessionUpdateRequest;
import com.tripagora.session.dto.response.CompletedSessionListResponse;
import com.tripagora.session.dto.response.SessionCreateResponse;
import com.tripagora.session.dto.response.SessionDetailResponse;
import com.tripagora.session.dto.response.SessionListResponse;
import com.tripagora.session.entity.SessionStatus;
import com.tripagora.session.service.SessionService;
import com.tripagora.template.dto.request.ItineraryUpdateRequest;
import com.tripagora.template.dto.response.ItinerariesResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {
    private final SessionService sessionService;

    @PostMapping
    public ApiResponse<SessionCreateResponse> createSession(@AuthenticationPrincipal final long userId,
                                                            @RequestBody @Valid final SessionCreateRequest request) {
        SessionCreateResponse response = sessionService.createSession(userId, request);
        return ApiResponse.success(SuccessCode.CREATED, response);
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<SessionDetailResponse> getSessionDetails(@AuthenticationPrincipal final long userId,
                                                                @PathVariable final long sessionId) {
        SessionDetailResponse response = sessionService.getSessionDetails(userId, sessionId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PatchMapping("/{sessionId}")
    public ApiResponse<Void> updateSession(@AuthenticationPrincipal final long userId,
                                           @PathVariable final long sessionId,
                                           @RequestBody @Valid final SessionUpdateRequest request) {
        sessionService.updateSession(userId, sessionId, request);
        return ApiResponse.success(SuccessCode.OK);
    }

    @DeleteMapping("/{sessionId}")
    public ApiResponse<Void> deleteSession(@AuthenticationPrincipal final long userId,
                                           @PathVariable final long sessionId) {
        sessionService.deleteSession(userId, sessionId);
        return ApiResponse.success(SuccessCode.OK);
    }

    @GetMapping("/{sessionId}/itineraries")
    public ApiResponse<ItinerariesResponse> getItineraries(@AuthenticationPrincipal final long userId,
                                                           @PathVariable final long sessionId) {
        ItinerariesResponse response = sessionService.getItineraries(sessionId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PutMapping("/{sessionId}/itineraries")
    public ApiResponse<Void> updateItineraries(@AuthenticationPrincipal final long userId,
                                               @PathVariable final long sessionId,
                                               @RequestBody @Valid final ItineraryUpdateRequest request) {
        sessionService.updateItineraries(userId, sessionId, request);
        return ApiResponse.success(SuccessCode.OK);
    }

    @GetMapping("/my")
    public ApiResponse<SessionListResponse> getMySessions(@AuthenticationPrincipal final long userId,
                                                          @RequestParam(required = false) List<SessionStatus> statuses,
                                                          @PageableDefault(size = 10) Pageable pageable) {
        SessionListResponse response = sessionService.getMySessions(userId, statuses, pageable);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @GetMapping("/participating")
    public ApiResponse<SessionListResponse> getParticipatingSessions(@AuthenticationPrincipal final long userId,
                                                                     @RequestParam(required = false) List<SessionStatus> statuses,
                                                                     @PageableDefault(size = 10) Pageable pageable) {
        SessionListResponse response = sessionService.getParticipatingSessions(userId, statuses, pageable);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @GetMapping("/completed")
    public ApiResponse<CompletedSessionListResponse> getCompletedSessions(@AuthenticationPrincipal final long userId,
                                                                          @PageableDefault(size = 10) Pageable pageable) {

        CompletedSessionListResponse response = sessionService.getCompletedSessions(userId, pageable);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PostMapping("/{sessionId}/close")
    public ApiResponse<Void> closeRecruitment(@AuthenticationPrincipal final long userId,
                                              @PathVariable final long sessionId) {
        sessionService.closeRecruitment(userId, sessionId);
        return ApiResponse.success(SuccessCode.OK);
    }

    @GetMapping("/search")
    public ApiResponse<SessionListResponse> searchSessions(@ModelAttribute SessionSearchRequest request,
                                                           @PageableDefault(size = 10) Pageable pageable) {
        SessionListResponse response = sessionService.searchSessions(request, pageable);
        return ApiResponse.success(SuccessCode.OK, response);
    }
}