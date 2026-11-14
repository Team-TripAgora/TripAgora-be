package com.tripagora.itinerary;

import com.tripagora.itinerary.entity.Itineraries;
import com.tripagora.itinerary.entity.Itinerary;
import com.tripagora.itinerary.exception.ItineraryDaySequenceInvalidException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 일정 목록 규칙: 1일차부터 빠진 날 없이 이어져야 하고, 마지막 일차로 여행 기간을 정한다
class ItinerariesTest {

    @Test
    @DisplayName("1일차부터 이어진 일정은 만들 수 있고, 같은 날 일정이 여러 개여도 된다")
    void acceptsContinuousDays() {
        Itineraries itineraries = itinerariesOf(1, 1, 2, 3);

        assertThat(itineraries.values()).hasSize(4);
        assertThat(itineraries.lastDay()).isEqualTo(3);
    }

    @Test
    @DisplayName("빠진 일차가 있으면 예외")
    void rejectsMissingDay() {
        assertThatThrownBy(() -> itinerariesOf(1, 3))
                .isInstanceOf(ItineraryDaySequenceInvalidException.class);
    }

    @Test
    @DisplayName("1일차 없이 시작하면 예외")
    void rejectsMissingFirstDay() {
        assertThatThrownBy(() -> itinerariesOf(2, 3))
                .isInstanceOf(ItineraryDaySequenceInvalidException.class);
    }

    @Test
    @DisplayName("일정이 없으면 당일 여행으로 보고 마지막 일차는 1")
    void emptyItinerariesLastDayIsOne() {
        assertThat(itinerariesOf().lastDay()).isEqualTo(1);
    }

    private static Itineraries itinerariesOf(Integer... days) {
        List<Itinerary> values = Arrays.stream(days)
                .map(day -> Itinerary.builder()
                        .day(day)
                        .location("장소" + day)
                        .startTime(LocalTime.of(10, 0))
                        .latitude(37.5)
                        .longitude(127.0)
                        .build())
                .toList();
        return new Itineraries(values);
    }
}
