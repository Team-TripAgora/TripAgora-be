package com.tripagora.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

// 앱의 Clock 대신 테스트 시계를 쓴다
@TestConfiguration
public class TestClockConfig {

    @Bean
    @Primary
    public TestClock testClock() {
        return new TestClock();
    }
}
