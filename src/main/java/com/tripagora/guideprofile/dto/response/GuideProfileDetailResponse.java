package com.tripagora.guideprofile.dto.response;

import com.tripagora.session.dto.response.SessionListResponse;

import java.util.List;

public record GuideProfileDetailResponse(
        String nickname,
        String userImageUrl,
        String guideImageUrl,
        String bio,
        List<Long> tags,
        List<PortfolioResponse> portfolios,
        SessionListResponse SessionList) {}