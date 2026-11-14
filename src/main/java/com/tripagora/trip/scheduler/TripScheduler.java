package com.tripagora.trip.scheduler;

import com.tripagora.trip.entity.Trip;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripScheduler {
    private final TripRepository tripRepository;

    @Transactional
    @Scheduled(cron = "0 1 0 * * *", zone = "Asia/Seoul")
    public void startTrips() {
        LocalDate today = LocalDate.now();
        log.info("[TripScheduler] 'IN_PROGRESS' 상태 변경 작업 시작 (대상 날짜: {})", today);

        List<Trip> tripsToStart = tripRepository.findByStatusAndStartDate(
                TripStatus.RECRUITMENT_CLOSED, today
        );

        tripsToStart.forEach(trip -> {
            trip.start();
            log.info(" - 여행 ID {} 상태 변경: IN_PROGRESS (시작일 도래)", trip.getId());
        });

        log.info("[TripScheduler] 'IN_PROGRESS' 상태 변경 작업 완료 (총 {}건)", tripsToStart.size());
    }

    @Transactional
    @Scheduled(cron = "0 3 0 * * *", zone = "Asia/Seoul")
    public void completeTrips() {
        LocalDate today = LocalDate.now();
        log.info("[TripScheduler] 'COMPLETED' 상태 변경 작업 시작 (대상 날짜: {} 이전)", today);

        List<Trip> tripsToComplete = tripRepository.findByStatusAndEndDateBefore(
                TripStatus.IN_PROGRESS,
                today
        );

        tripsToComplete.forEach(trip -> {
            trip.complete();
            log.info(" - 여행 ID {} 상태 변경: COMPLETED (종료일 경과)", trip.getId());
        });

        log.info("[TripScheduler] 'COMPLETED' 상태 변경 작업 완료 (총 {}건)", tripsToComplete.size());
    }
}