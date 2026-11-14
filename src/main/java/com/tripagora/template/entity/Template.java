package com.tripagora.template.entity;

import com.tripagora.common.entity.BaseEntity;
import com.tripagora.guideprofile.entity.GuideProfile;
import com.tripagora.itinerary.entity.Itinerary;
import com.tripagora.region.entity.Region;
import com.tripagora.region.entity.TemplateRegion;
import com.tripagora.review.entity.Review;
import com.tripagora.trip.entity.Trip;
import com.tripagora.tag.entity.Tag;
import com.tripagora.tag.entity.TemplateTag;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private List<Itinerary> itineraries = new ArrayList<>();

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TemplateTag> templateTags = new ArrayList<>();

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TemplateImage> templateImages = new ArrayList<>();

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TemplateRegion> templateRegions = new ArrayList<>();

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Trip> trips = new ArrayList<>();

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();


    // 템플릿은 빈 초안으로 만들고, 제목·내용은 필드별 수정 API로 채운다
    @Builder
    private Template(GuideProfile guideProfile) {
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
        this.itineraries.clear();
    }

    public void addItinerary(Itinerary itinerary) {
        itinerary.assignTo(this);
        this.itineraries.add(itinerary);
    }

    // 태그를 tags로 바꾼다. 남는 태그는 그대로 두고, 빠진 태그는 컬렉션에서 빼고(orphanRemoval로 삭제), 새 태그만 추가한다
    public void replaceTags(List<Tag> tags) {
        Set<Long> newTagIds = tags.stream().map(Tag::getId).collect(Collectors.toSet());
        this.templateTags.removeIf(templateTag -> !newTagIds.contains(templateTag.getTag().getId()));

        Set<Long> keptTagIds = this.templateTags.stream()
                .map(templateTag -> templateTag.getTag().getId())
                .collect(Collectors.toSet());
        tags.stream()
                .filter(tag -> !keptTagIds.contains(tag.getId()))
                .forEach(tag -> this.templateTags.add(TemplateTag.builder().template(this).tag(tag).build()));
    }

    // 지역을 regions로 바꾼다. 방식은 replaceTags와 같다
    public void replaceRegions(List<Region> regions) {
        Set<Long> newRegionIds = regions.stream().map(Region::getId).collect(Collectors.toSet());
        this.templateRegions.removeIf(templateRegion -> !newRegionIds.contains(templateRegion.getRegion().getId()));

        Set<Long> keptRegionIds = this.templateRegions.stream()
                .map(templateRegion -> templateRegion.getRegion().getId())
                .collect(Collectors.toSet());
        regions.stream()
                .filter(region -> !keptRegionIds.contains(region.getId()))
                .forEach(region -> this.templateRegions.add(TemplateRegion.builder().template(this).region(region).build()));
    }
}