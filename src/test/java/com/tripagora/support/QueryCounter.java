package com.tripagora.support;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

// Hibernate 통계로 실행된 SQL 수를 센다. clear() 후 측정할 동작을 실행하고 count()를 읽는다
public class QueryCounter {
    private final Statistics statistics;

    public QueryCounter(EntityManagerFactory entityManagerFactory) {
        this.statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    public void clear() {
        statistics.clear();
    }

    public long count() {
        return statistics.getPrepareStatementCount();
    }
}
