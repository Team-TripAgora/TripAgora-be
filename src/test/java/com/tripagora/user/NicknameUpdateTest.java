package com.tripagora.user;

import com.tripagora.support.IntegrationTest;
import com.tripagora.user.dto.request.NicknameUpdateRequest;
import com.tripagora.user.entity.Role;
import com.tripagora.user.exception.DuplicateNicknameException;
import com.tripagora.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NicknameUpdateTest extends IntegrationTest {
    private static final int USERS = 10;

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("지금 쓰는 닉네임으로 다시 저장해도 성공한다")
    void acceptsOwnCurrentNickname() throws Exception {
        fixture.user(1L, "여행객B", Role.TRAVELER);

        updateNickname(1L, "여행객B")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("여행객B"));
    }

    @Test
    @DisplayName("다른 사람이 쓰는 닉네임이면 409")
    void rejectsNicknameOfAnotherUser() throws Exception {
        fixture.user(1L, "가이드A", Role.GUIDE);
        fixture.user(2L, "여행객B", Role.TRAVELER);

        updateNickname(2L, "가이드A")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 존재하는 닉네임입니다."));
    }

    @Test
    @DisplayName("10명이 동시에 같은 닉네임으로 바꾸면 1명만 성공하고 나머지는 중복 닉네임 예외를 받는다")
    void concurrentChangesToSameNickname() throws Exception {
        for (long id = 1; id <= USERS; id++) {
            fixture.user(id, "racer" + id, Role.TRAVELER);
        }

        for (int round = 1; round <= 3; round++) {
            List<Throwable> results = changeAtTheSameTime("race" + round);

            long succeeded = results.stream().filter(r -> r == null).count();
            long duplicated = results.stream().filter(r -> r instanceof DuplicateNicknameException).count();
            assertThat(succeeded).as("round %d 성공", round).isEqualTo(1);
            assertThat(duplicated).as("round %d 중복 예외 (다른 예외 없음)", round).isEqualTo(USERS - 1);
        }
    }

    // 모든 스레드를 준비시킨 뒤 동시에 출발시킨다. 성공이면 null, 실패면 던져진 예외를 담는다
    private List<Throwable> changeAtTheSameTime(String nickname) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(USERS);
        CountDownLatch ready = new CountDownLatch(USERS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Throwable>> futures = new ArrayList<>();
        for (long id = 1; id <= USERS; id++) {
            long userId = id;
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    userService.updateNickname(userId, new NicknameUpdateRequest(nickname));
                    return null;
                } catch (Throwable e) {
                    return e;
                }
            }));
        }
        ready.await();
        start.countDown();

        List<Throwable> results = new ArrayList<>();
        for (Future<Throwable> future : futures) {
            results.add(future.get());
        }
        executor.shutdown();
        return results;
    }

    private ResultActions updateNickname(long userId, String nickname) throws Exception {
        return mockMvc.perform(patch("/api/users/me/nickname")
                .header("Authorization", bearer(userId, Role.TRAVELER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"" + nickname + "\"}"));
    }
}
