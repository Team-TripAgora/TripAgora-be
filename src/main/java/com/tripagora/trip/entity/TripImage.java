package com.tripagora.trip.entity;

import com.tripagora.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 여행이 열릴 때 복사한 템플릿 사진. S3 파일은 템플릿과 공유한다
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "trip_image")
public class TripImage extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false)
    private String imageUrl;

    @Column
    private int displayOrder;

    @Builder
    private TripImage(Trip trip, String imageUrl, int displayOrder) {
        this.trip = trip;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }
}
