package me.project.joonggoconsumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
class CouponLaneExecutor {

    private final Executor executor;

    @Autowired
    CouponLaneExecutor(@Value("${coupon.issue.worker.parallelism:4}") int parallelism) {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(parallelism);
        pool.setMaxPoolSize(parallelism);
        pool.setQueueCapacity(0);
        pool.setThreadNamePrefix("coupon-lane-");
        pool.initialize();
        executor = pool;
    }

    CouponLaneExecutor(Executor executor) {
        this.executor = executor;
    }

    void execute(Runnable task) {
        executor.execute(task);
    }

    @jakarta.annotation.PreDestroy
    void close() {
        if (executor instanceof ThreadPoolTaskExecutor pool) {
            pool.shutdown();
        }
    }
}
