package com.tripagora.trip.entity;

import com.tripagora.common.entity.BaseEntity;
import com.tripagora.participation.entity.Participation;
import com.tripagora.review.entity.Review;
import com.tripagora.room.entity.Room;
import com.tripagora.trip.exception.TripFullException;
import com.tripagora.template.entity.Template;
import com.tripagora.template.entity.TemplateItinerary;
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
    private List<TripItinerary> tripItineraries = new ArrayList<>();

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

    public void addItinerary(TripItinerary itinerary) {
        this.tripItineraries.add(itinerary);
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

    // TODO: 추후 여행 참여 로직 작성 시 수정
    public void updateStatus(TripStatus newStatus) {
        this.status = newStatus;
    }

    public void clearItineraries() {
        this.tripItineraries.clear();
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
        int maxDay = template.getTemplateItineraries().stream()
                .mapToInt(TemplateItinerary::getDay)
                .max()
                .orElse(1);  // 일정이 없으면 당일 여행으로 간주

        return startDate.plusDays(maxDay - 1);
    }
}