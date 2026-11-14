package com.tripagora.trip.entity;

import com.tripagora.common.entity.BaseEntity;
import com.tripagora.itinerary.entity.Itineraries;
import com.tripagora.itinerary.entity.Itinerary;
import com.tripagora.participation.entity.Participation;
import com.tripagora.participation.exception.ParticipationCancelNotAllowedException;
import com.tripagora.review.entity.Review;
import com.tripagora.room.entity.Room;
import com.tripagora.trip.exception.TripAlreadyEndedException;
import com.tripagora.trip.exception.TripDeleteNotAllowedException;
import com.tripagora.trip.exception.TripFullException;
import com.tripagora.trip.exception.TripNotInProgressException;
import com.tripagora.trip.exception.TripNotRecruitingException;
import com.tripagora.template.entity.Template;
import com.tripagora.wishlist.entity.Wishlist;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "trip")
public class Trip extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;

    @Column(nullable = false)
    private Integer maxParticipants;

    @Column(nullable = false)
    private Integer currentParticipants = 0;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStatus status;

    @OrderBy("day ASC , startTime ASC ")
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Itinerary> itineraries = new ArrayList<>();

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participation> participants = new ArrayList<>();

    @OneToOne(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private Room room;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Wishlist> wishlists = new ArrayList<>();

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    @Builder
    private Trip(Template template, Integer maxParticipants, LocalDate startDate) {
        this.template = template;
        this.maxParticipants = maxParticipants;
        this.startDate = startDate;
        this.endDate = calculateEndDate(template, startDate);
        this.status = TripStatus.RECRUITING;
    }

    // 일정을 통째로 바꾼다 (일차 검사는 Itineraries를 만들 때 끝난다)
    public void replaceItineraries(Itineraries itineraries) {
        this.itineraries.clear();
        itineraries.values().forEach(itinerary -> {
            itinerary.assignTo(this);
            this.itineraries.add(itinerary);
        });
    }

    // 템플릿 일정을 복사해 이 여행의 일정으로 갖는다 (이후 템플릿 일정이 바뀌어도 영향 없음)
    public void copyItinerariesFrom(Template template) {
        replaceItineraries(new Itineraries(template.getItineraries().stream()
                .map(Itinerary::copy)
                .toList()));
    }

    public void addParticipation(Participation participation) {
        this.participants.add(participation);
    }

    public void assignRoom(Room room) {
        this.room = room;
    }

    public void updateInfo(Integer maxParticipants, LocalDate startDate) {
        this.maxParticipants = maxParticipants;
        this.startDate = startDate;
        this.endDate = calculateEndDate(this.template, startDate);
    }

    // 모집을 마감한다. 시작일이 아직이면 모집 마감, 시작일이 됐으면 바로 진행 중
    public void closeRecruitment(LocalDate today) {
        if (this.endDate.isBefore(today)) {
            throw new TripAlreadyEndedException();
        }
        if (this.status != TripStatus.RECRUITING) {
            throw new TripNotRecruitingException();
        }

        this.status = this.startDate.isAfter(today) ? TripStatus.RECRUITMENT_CLOSED : TripStatus.IN_PROGRESS;
    }

    // 시작일이 된 모집 마감 여행을 진행 중으로 바꾼다 (스케줄러)
    public void start() {
        if (this.status != TripStatus.RECRUITMENT_CLOSED) {
            throw new IllegalStateException("모집 마감 상태의 여행만 시작할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = TripStatus.IN_PROGRESS;
    }

    // 종료일이 지난 진행 중 여행을 완료로 바꾼다 (스케줄러)
    public void complete() {
        if (this.status != TripStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 여행만 완료할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = TripStatus.COMPLETED;
    }

    // 참가 신청은 모집 중에만
    public void validateJoinable() {
        if (this.status != TripStatus.RECRUITING) {
            throw new TripNotRecruitingException();
        }
    }

    // 참가 취소(참가 기록 삭제)는 모집 중에만. 완료된 여행은 취소 대신 숨긴다
    public void validateCancellable() {
        if (this.status != TripStatus.RECRUITING) {
            throw new ParticipationCancelNotAllowedException();
        }
    }

    public boolean isCompleted() {
        return this.status == TripStatus.COMPLETED;
    }

    // 정원·시작일 수정은 모집 중에만
    public void validateUpdatable() {
        if (this.status != TripStatus.RECRUITING) {
            throw new TripNotRecruitingException();
        }
    }

    // 삭제는 모집 중에만
    public void validateDeletable() {
        if (this.status != TripStatus.RECRUITING) {
            throw new TripDeleteNotAllowedException();
        }
    }

    // 일정 수정은 진행 중에만
    public void validateItineraryEditable() {
        if (this.status != TripStatus.IN_PROGRESS) {
            throw new TripNotInProgressException();
        }
    }

    public void increaseParticipant() {
        if (this.currentParticipants >= this.maxParticipants) {
            throw new TripFullException();
        }

        this.currentParticipants++;
    }

    public void decreaseParticipant() {
        if (this.currentParticipants > 0) {
            this.currentParticipants--;
        }
    }

    public static LocalDate calculateEndDate(Template template, LocalDate startDate) {
        int lastDay = new Itineraries(template.getItineraries()).lastDay();

        return startDate.plusDays(lastDay - 1);
    }
}