package com.tripagora.itinerary.entity;

import com.tripagora.common.entity.BaseEntity;
import com.tripagora.template.entity.Template;
import com.tripagora.trip.entity.Trip;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

import java.time.LocalTime;

// 템플릿 일정과 여행 일정. 한 행은 템플릿과 여행 중 정확히 한 곳에 속한다
// (여행을 열면 템플릿 일정을 복사해 여행이 따로 갖는다)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "itinerary")
@Check(name = "chk_itinerary_owner", constraints = "(template_id IS NULL) <> (trip_id IS NULL)")
public class Itinerary extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private Template template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
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

    // 주인(템플릿·여행)은 목록에 넣을 때 정한다
    @Builder
    private Itinerary(Integer day, String location, String content, LocalTime startTime, Double latitude, Double longitude) {
        this.day = day;
        this.location = location;
        this.content = content;
        this.startTime = startTime;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // 같은 내용의 새 일정 (여행을 열 때 템플릿 일정을 복사한다)
    public Itinerary copy() {
        return new Itinerary(day, location, content, startTime, latitude, longitude);
    }

    public void assignTo(Template template) {
        this.template = template;
    }

    public void assignTo(Trip trip) {
        this.trip = trip;
    }
}
