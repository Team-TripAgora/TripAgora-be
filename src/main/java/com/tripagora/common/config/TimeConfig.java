package com.tripagora.common.config;

import org.springframework.boot.autoconfigure.validation.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

// '오늘'과 현재 시각은 서버 시간대와 상관없이 한국 시간으로 계산한다 (서버 JVM이 UTC여도)
@Configuration
public class TimeConfig {
    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock clock() {
        return Clock.system(ZONE);
    }

    // 날짜 검증(@FutureOrPresent 등)의 '오늘'도 같은 시계로 계산한다
    @Bean
    public ValidationConfigurationCustomizer validationClockCustomizer(Clock clock) {
        return configuration -> configuration.clockProvider(() -> clock);
    }
}
