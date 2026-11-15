package com.tripagora.region.entity;

import com.tripagora.trip.entity.Trip;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 여행이 열릴 때 복사한 템플릿 지역
// (여행 id는 여행을 저장할 때 정해지므로, 복합 키 대신 대리 키 + unique 제약을 쓴다)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "trip_region",
        uniqueConstraints = @UniqueConstraint(name = "uk_trip_region", columnNames = {"trip_id", "region_id"}))
public class TripRegion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Builder
    private TripRegion(Trip trip, Region region) {
        this.trip = trip;
        this.region = region;
    }
}
