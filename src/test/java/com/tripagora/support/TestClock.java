package com.tripagora.support;

import com.tripagora.common.config.TimeConfig;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

// 테스트에서 시각을 고정할 수 있는 시계. 평소에는 실제 한국 시간으로 흐르고, 테스트가 끝나면 원래대로 돌아간다
public class TestClock extends Clock {
    private volatile Clock delegate = Clock.system(TimeConfig.ZONE);

    public void fixAt(Instant instant) {
        this.delegate = Clock.fixed(instant, TimeConfig.ZONE);
    }

    public void reset() {
        this.delegate = Clock.system(TimeConfig.ZONE);
    }

    @Override
    public ZoneId getZone() {
        return delegate.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return delegate.withZone(zone);
    }

    @Override
    public Instant instant() {
        return delegate.instant();
    }
}
