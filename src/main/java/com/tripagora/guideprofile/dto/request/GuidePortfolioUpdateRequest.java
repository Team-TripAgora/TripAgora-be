package com.tripagora.guideprofile.dto.request;

import com.tripagora.guideprofile.entity.GuidePortfolio;
import com.tripagora.guideprofile.entity.GuideProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.util.List;

public record GuidePortfolioUpdateRequest(
        @Valid
        @Size(max = 5, message = "포트폴리오는 최대 5개까지 등록 가능합니다.")
        List<PortfolioItem> portfolios
) {
    public record PortfolioItem(
            GuidePortfolio.PortfolioType type,

            @URL(message = "유효한 URL 형식이 아닙니다.")
            String url
    ) {
        public GuidePortfolio toGuidePortfolio(GuideProfile guideProfile) {
            return GuidePortfolio.builder()
                    .guideProfile(guideProfile)
                    .type(type)
                    .url(url)
                    .build();
        }
    }
}