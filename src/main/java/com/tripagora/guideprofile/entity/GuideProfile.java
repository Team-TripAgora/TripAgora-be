package com.tripagora.guideprofile.entity;

import com.tripagora.common.entity.BaseEntity;
import com.tripagora.guideprofile.dto.request.GuidePortfolioUpdateRequest;
import com.tripagora.template.entity.Template;
import com.tripagora.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "guide_profile")
public class GuideProfile extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column
    private String imageUrl;

    @Column
    private Double totalAvgRating = 0.0;

    @Column
    private Integer totalReviewCount = 0;

    @OneToMany(mappedBy = "guideProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GuidePortfolio> portfolios = new ArrayList<>();

    @OneToMany(mappedBy = "guideProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Template> templates = new ArrayList<>();

    @Builder
    private GuideProfile (User user) {
        this.user = user;
    }

    public void updateBio(String bio) {
        this.bio = bio;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void updatePortfolios(List<GuidePortfolioUpdateRequest.PortfolioItem> newPortfolios) {
        this.portfolios.clear();

        if (newPortfolios != null) {
            newPortfolios.forEach(item -> {
                this.portfolios.add(GuidePortfolio.builder()
                        .guideProfile(this)
                        .type(item.type())
                        .url(item.url())
                        .build());
            });
        }
    }

    public void updateRating(double newAverage, int newReviewCount) {
        this.totalAvgRating = newAverage;
        this.totalReviewCount = newReviewCount;
    }
}