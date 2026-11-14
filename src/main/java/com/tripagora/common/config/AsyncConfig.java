package com.tripagora.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@Configuration
public class AsyncConfig {
    public static final String IMAGE_DELETION_EXECUTOR = "imageDeletionExecutor";

    // 커밋 뒤 S3 이미지 삭제 전용. STOMP가 만드는 실행기들과 섞이지 않게 이름을 붙여 따로 둔다
    @Bean(name = IMAGE_DELETION_EXECUTOR)
    public ThreadPoolTaskExecutor imageDeletionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("image-delete-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }
}
