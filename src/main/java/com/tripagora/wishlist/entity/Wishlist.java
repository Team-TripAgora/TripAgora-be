package com.tripagora.wishlist.entity;

import com.tripagora.common.entity.BaseEntity;
import com.tripagora.trip.entity.Trip;
import com.tripagora.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "wishlist",
        uniqueConstraints = @UniqueConstraint(name = Wishlist.UNIQUE_USER_TRIP, columnNames = {"user_id", "trip_id"}))
public class Wishlist extends BaseEntity {
    // 한 사용자는 한 여행을 한 번만 찜한다
    public static final String UNIQUE_USER_TRIP = "uk_wishlist_user_trip";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Builder
    private Wishlist(User user, Trip trip) {
        this.user = user;
        this.trip = trip;
    }
}