package com.tripagora.guideprofile.dto.response;

import com.tripagora.trip.dto.response.TripListResponse;

import java.util.List;

public record GuideProfileDetailResponse(
        String nickname,
        String userImageUrl,
        String guideImageUrl,
        String bio,
        List<Long> tags,
        List<PortfolioResponse> portfolios,
        TripListResponse SessionList) {}