package com.tripagora.participation.entity;

import com.tripagora.common.entity.BaseEntity;
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

    public void updateLastReadAt() {
        this.lastChatReadAt = LocalDateTime.now();
    }
}