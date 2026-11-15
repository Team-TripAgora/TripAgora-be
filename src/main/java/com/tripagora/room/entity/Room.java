package com.tripagora.room.entity;

import com.tripagora.common.entity.BaseEntity;
import com.tripagora.trip.entity.Trip;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 외래 키는 여행 쪽(trip.room_id)에 있다
    @OneToOne(mappedBy = "room")
    private Trip trip;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id desc")
    private List<Notice> notices = new ArrayList<>();

    @Builder
    public Room(Trip trip) {
        this.trip = trip;
    }
}