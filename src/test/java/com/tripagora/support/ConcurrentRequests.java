package com.tripagora.support;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntFunction;

// 요청 여러 개를 스레드마다 하나씩 준비시킨 뒤 한꺼번에 보낸다. 응답 상태 코드를 요청 순서대로 돌려준다
public final class ConcurrentRequests {

    private ConcurrentRequests() {
    }

    public static Result send(MockMvc mockMvc, int count, IntFunction<RequestBuilder> request) throws Exception {
        ExecutorService threads = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> responses = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                RequestBuilder builder = request.apply(i);
                responses.add(threads.submit(() -> {
                    ready.countDown();
                    start.await();
                    return mockMvc.perform(builder).andReturn().getResponse().getStatus();
                }));
            }

            ready.await();
            long startedAt = System.nanoTime();
            start.countDown();

            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> response : responses) {
                statuses.add(response.get());
            }
            return new Result(statuses, (System.nanoTime() - startedAt) / 1_000_000);
        } finally {
            threads.shutdownNow();
        }
    }

    public record Result(List<Integer> statuses, long elapsedMillis) {
        public long count(int status) {
            return statuses.stream().filter(s -> s == status).count();
        }
    }
}
