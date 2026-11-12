package com.tripagora.guideprofile.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "guide_portfolio")
public class GuidePortfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_profile_id", nullable = false)
    private GuideProfile guideProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PortfolioType type;

    @Column(nullable = false)
    private String url;

    @Builder
    private GuidePortfolio(GuideProfile guideProfile, PortfolioType type, String url) {
        this.guideProfile = guideProfile;
        this.type = type;
        this.url = url;
    }

    protected void updateUrl(String url) {
        this.url = url;
    }

    public enum PortfolioType {
        FACEBOOK, INSTAGRAM, TWITTER, WEBSITE, YOUTUBE
    }
}