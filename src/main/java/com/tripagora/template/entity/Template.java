package com.tripagora.template.entity;

import com.tripagora.common.entity.BaseEntity;
import com.tripagora.guideProfile.entity.GuideProfile;
import com.tripagora.region.entity.TemplateRegion;
import com.tripagora.review.entity.Review;
import com.tripagora.session.entity.Session;
import com.tripagora.tag.entity.TemplateTag;
import com.tripagora.template.dto.request.ItineraryItemRequest;
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
@Table(name = "template")
public class Template extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_profile_id", nullable = false)
    private GuideProfile guideProfile;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @Column
    private Double avgRating = 0.0;

    @Column
    private Integer reviewCount = 0;

    @OrderBy("day ASC , startTime ASC ")
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TemplateItinerary> templateItineraries = new ArrayList<>();

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TemplateTag> templateTags = new ArrayList<>();

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TemplateImage> templateImages = new ArrayList<>();

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TemplateRegion> templateRegions = new ArrayList<>();

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Session> sessions = new ArrayList<>();

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();


    @Builder
    private Template(GuideProfile guideProfile, String title, String content) {
        this.guideProfile = guideProfile;
        this.title = "";
        this.content = "";
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void updateRating(Double avgRating, Integer reviewCount) {
        this.avgRating = avgRating;
        this.reviewCount = reviewCount;
    }

    public void clearImages() {
        this.templateImages.clear();
    }

    public void addImage(String imageUrl) {
        TemplateImage templateImage = TemplateImage.builder()
                .template(this)
                .imageUrl(imageUrl)
                .displayOrder(this.templateImages.size() + 1)
                .build();
        this.templateImages.add(templateImage);
    }

    public void clearItineraries() {
        this.templateItineraries.clear();
    }

    public void addItinerary(ItineraryItemRequest item) {
        TemplateItinerary itinerary = TemplateItinerary.builder()
                .template(this)
                .day(item.day())
                .location(item.location())
                .content(item.content())
                .startTime(item.startTime())
                .latitude(item.latitude())
                .longitude(item.longitude())
                .build();
        this.templateItineraries.add(itinerary);
    }
}