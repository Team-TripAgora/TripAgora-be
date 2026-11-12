package com.tripagora.guideprofile.dto.response;

import java.util.List;

public record GuidePortfolioUpdateResponse(
        List<PortfolioResponse> portfolios) {}