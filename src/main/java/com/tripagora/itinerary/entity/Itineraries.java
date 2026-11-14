package com.tripagora.itinerary.entity;

import com.tripagora.itinerary.exception.ItineraryDaySequenceInvalidException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// 일정 목록 (일급 컬렉션). 1일차부터 빠진 날 없이 이어져야 한다.
// 템플릿·여행의 일정은 이 클래스를 거쳐서만 바뀌므로, 저장된 일정은 항상 이 규칙을 지킨다
public class Itineraries {
    private final List<Itinerary> values;

    public Itineraries(List<Itinerary> values) {
        validateDaySequence(values);
        this.values = List.copyOf(values);
    }

    public List<Itinerary> values() {
        return values;
    }

    // 마지막 일차 (여행 기간 계산에 쓴다). 일정이 없으면 당일 여행으로 보고 1
    public int lastDay() {
        return values.stream()
                .mapToInt(Itinerary::getDay)
                .max()
                .orElse(1);
    }

    private static void validateDaySequence(List<Itinerary> values) {
        Set<Integer> days = values.stream()
                .map(Itinerary::getDay)
                .collect(Collectors.toSet());

        int maxDay = days.stream().max(Integer::compareTo).orElse(0);
        for (int day = 1; day <= maxDay; day++) {
            if (!days.contains(day)) {
                throw new ItineraryDaySequenceInvalidException();
            }
        }
    }
}
