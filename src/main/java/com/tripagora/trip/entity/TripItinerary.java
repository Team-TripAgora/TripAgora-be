package com.tripagora.trip.entity;

import com.tripagora.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "trip_itinerary")
public class TripItinerary extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false)
    private Integer day;

    @Column(nullable = false)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Builder
    private TripItinerary(Trip trip, Integer day, String location, String content, LocalTime startTime, Double latitude, Double longitude) {
        this.trip = trip;
        this.day = day;
        this.location = location;
        this.content = content;
        this.startTime = startTime;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}