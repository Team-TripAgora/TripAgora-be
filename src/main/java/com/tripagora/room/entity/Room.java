package com.tripagora.room.entity;

import com.tripagora.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
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

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id desc")
    private List<Notice> notices = new ArrayList<>();

    // 모집을 마감할 때 만든다. 어느 여행의 방인지는 여행이 외래 키(trip.room_id)로 가리킨다
    public static Room create() {
        return new Room();
    }
}