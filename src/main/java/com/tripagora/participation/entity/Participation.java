package com.tripagora.participation.entity;

import com.tripagora.common.entity.BaseEntity;
import com.tripagora.participation.exception.GuideCannotCancelParticipationException;
import com.tripagora.trip.entity.Trip;
import com.tripagora.user.entity.Role;
import com.tripagora.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "participation")
public class Participation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column
    private LocalDateTime lastChatReadAt;

    @Builder
    public Participation(User user, Trip trip, Role role) {
        this.user = user;
        this.trip = trip;
        this.role = role;
    }

    // 가이드는 자기 여행의 참가를 취소할 수 없다 (취소하면 여행에 가이드가 없어진다)
    public void validateCancellable() {
        if (this.role == Role.GUIDE) {
            throw new GuideCannotCancelParticipationException();
        }
    }

    public void updateLastReadAt() {
        this.lastChatReadAt = LocalDateTime.now();
    }
}