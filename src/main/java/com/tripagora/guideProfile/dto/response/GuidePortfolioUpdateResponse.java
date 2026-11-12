package com.tripagora.guideProfile.dto.response;

import java.util.List;

public record GuidePortfolioUpdateResponse(
        List<PortfolioResponse> portfolios) {}