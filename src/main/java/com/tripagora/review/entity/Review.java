package com.tripagora.review.entity;

import com.tripagora.common.entity.BaseEntity;
import com.tripagora.guideprofile.entity.GuideProfile;
import com.tripagora.trip.entity.Trip;
import com.tripagora.template.entity.Template;
import com.tripagora.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "review",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_review_author_trip",
                        columnNames = {"author_id", "trip_id"}
                )
        }
)
public class Review extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_profile_id", nullable = false)
    private GuideProfile guideProfile;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Integer rating;

    @Builder
    private Review(User author, Trip trip, Template template, GuideProfile guideProfile, String content, Integer rating) {
        this.author = author;
        this.trip = trip;
        this.template = template;
        this.guideProfile = guideProfile;
        this.content = content;
        this.rating = rating;
    }

    public void update(String content, Integer rating) {
        this.content = content;
        this.rating = rating;
    }
}