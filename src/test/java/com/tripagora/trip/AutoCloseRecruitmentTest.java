package com.tripagora.trip;

import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.trip.scheduler.TripScheduler;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// 시작일이 된 모집 중 여행은 가이드가 마감하지 않았어도 자정 작업이 마감해 출발시킨다 (채팅방 생성 → 진행 중)
class AutoCloseRecruitmentTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TRAVELER = 2L;
    private static final long TEMPLATE = 1L;

    @Autowired
    private TripScheduler tripScheduler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = today();
        fixture.guide(GUIDE);
        fixture.traveler(TRAVELER);
        fixture.template(TEMPLATE, GUIDE, 2);
    }

    @Test
    @DisplayName("시작일이 된 모집 중 여행은 자정 작업 뒤 진행 중이 되고 채팅방이 생긴다")
    void closesRecruitmentOnStartDate() throws Exception {
        fixture.trip(1L, TEMPLATE, TripStatus.RECRUITING, today, today.plusDays(1));
        fixture.participation(1L, 1L, TRAVELER, Role.TRAVELER);

        runMidnightJobs();

        assertThat(statusOf(1L)).isEqualTo(TripStatus.IN_PROGRESS.name());
        assertThat(roomIdOf(1L)).isNotNull();
    }

    @Test
    @DisplayName("시작일이 내일인 모집 중 여행은 그대로 모집 중이다")
    void keepsRecruitingBeforeStartDate() throws Exception {
        fixture.trip(1L, TEMPLATE, TripStatus.RECRUITING, today.plusDays(1), today.plusDays(2));

        runMidnightJobs();

        assertThat(statusOf(1L)).isEqualTo(TripStatus.RECRUITING.name());
        assertThat(roomIdOf(1L)).isNull();
    }

    @Test
    @DisplayName("서버가 멈춰 종료일까지 지난 모집 중 여행도 그날 밤 작업으로 완료까지 간다")
    void catchesUpTripThatAlreadyEnded() throws Exception {
        fixture.trip(1L, TEMPLATE, TripStatus.RECRUITING, today.minusDays(3), today.minusDays(2));

        runMidnightJobs();

        assertThat(statusOf(1L)).isEqualTo(TripStatus.COMPLETED.name());
        assertThat(roomIdOf(1L)).isNotNull();
    }

    @Test
    @DisplayName("가이드의 모집 마감과 자정 작업이 동시에 돌아도 여행마다 채팅방은 하나다")
    void manualAndAutomaticCloseAtTheSameTime() throws Exception {
        int trips = 5;
        for (long id = 1; id <= trips; id++) {
            fixture.trip(id, TEMPLATE, TripStatus.RECRUITING, today, today.plusDays(1));
        }

        ExecutorService threads = Executors.newFixedThreadPool(trips + 1);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> scheduler = threads.submit(() -> {
                start.await();
                tripScheduler.closeStartedRecruitments();
                return null;
            });
            List<Future<Integer>> guideCloses = new ArrayList<>();
            for (long id = 1; id <= trips; id++) {
                long tripId = id;
                guideCloses.add(threads.submit(() -> {
                    start.await();
                    return mockMvc.perform(post("/api/trips/" + tripId + "/close").header("Authorization", bearer(GUIDE, Role.GUIDE)))
                            .andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            scheduler.get();
            for (Future<Integer> guideClose : guideCloses) {
                // 가이드가 먼저면 200, 자정 작업이 먼저면 이미 마감돼 400
                assertThat(guideClose.get()).isIn(200, 400);
            }
        } finally {
            threads.shutdownNow();
        }

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM room", Integer.class)).isEqualTo(trips);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trip WHERE status = 'IN_PROGRESS' AND room_id IS NOT NULL", Integer.class)).isEqualTo(trips);
    }

    // 자정에 도는 여행 스케줄 작업을 cron 시각 순서대로 모두 실행한다
    private void runMidnightJobs() throws Exception {
        List<Method> jobs = Arrays.stream(AopUtils.getTargetClass(tripScheduler).getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Scheduled.class))
                .sorted(Comparator.comparing(AutoCloseRecruitmentTest::secondsAfterMidnight))
                .toList();
        for (Method job : jobs) {
            job.invoke(tripScheduler);
        }
    }

    private static int secondsAfterMidnight(Method job) {
        String[] cron = job.getAnnotation(Scheduled.class).cron().split(" ");
        return Integer.parseInt(cron[2]) * 3600 + Integer.parseInt(cron[1]) * 60 + Integer.parseInt(cron[0]);
    }

    private String statusOf(long tripId) {
        return jdbcTemplate.queryForObject("SELECT status FROM trip WHERE id = ?", String.class, tripId);
    }

    private Long roomIdOf(long tripId) {
        return jdbcTemplate.queryForObject("SELECT room_id FROM trip WHERE id = ?", Long.class, tripId);
    }
}
