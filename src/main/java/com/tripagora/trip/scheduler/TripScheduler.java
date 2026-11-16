package com.tripagora.trip.scheduler;

import com.tripagora.trip.entity.Trip;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.trip.repository.TripRepository;
import com.tripagora.trip.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripScheduler {
    private final TripRepository tripRepository;
    private final TripService tripService;
    private final Clock clock;

    // 시작일이 된 모집 중 여행은 가이드가 마감하지 않았어도 마감해 출발시킨다 (모집은 시작 전날까지).
    // 여행마다 잠그고 다시 확인하므로 트랜잭션은 여행 단위다 (이 메서드에는 걸지 않는다)
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void closeStartedRecruitments() {
        LocalDate today = LocalDate.now(clock);
        log.info("[TripScheduler] 시작일이 된 모집 중 여행 자동 마감 시작 (시작일이 {} 이하)", today);

        List<Long> tripIds = tripRepository.findIdsByStatusAndStartDateLessThanEqual(TripStatus.RECRUITING, today);
        long closed = tripIds.stream()
                .filter(tripId -> tripService.closeRecruitmentOnStartDate(tripId, today))
                .count();

        log.info("[TripScheduler] 시작일이 된 모집 중 여행 자동 마감 완료 (총 {}건)", closed);
    }

    @Transactional
    @Scheduled(cron = "0 1 0 * * *", zone = "Asia/Seoul")
    public void startTrips() {
        LocalDate today = LocalDate.now(clock);
        log.info("[TripScheduler] 'IN_PROGRESS' 상태 변경 작업 시작 (시작일이 {} 이하)", today);

        List<Trip> tripsToStart = tripRepository.findByStatusAndStartDateLessThanEqual(
                TripStatus.RECRUITMENT_CLOSED, today
        );

        tripsToStart.forEach(trip -> {
            trip.start();
            log.info(" - 여행 ID {} 상태 변경: IN_PROGRESS (시작일 {})", trip.getId(), trip.getStartDate());
        });

        log.info("[TripScheduler] 'IN_PROGRESS' 상태 변경 작업 완료 (총 {}건)", tripsToStart.size());
    }

    @Transactional
    @Scheduled(cron = "0 3 0 * * *", zone = "Asia/Seoul")
    public void completeTrips() {
        LocalDate today = LocalDate.now(clock);
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