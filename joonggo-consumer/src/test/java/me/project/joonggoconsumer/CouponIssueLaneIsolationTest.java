package me.project.joonggoconsumer;

import me.project.joonggocore.domain.coupon.queue.CouponIssueJob;
import me.project.joonggocore.domain.coupon.queue.CouponIssueQueue;
import me.project.joonggocore.service.CouponIssueOutbox;
import me.project.joonggocore.service.CouponIssueService;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "coupon.issue.worker.enabled=false", "spring.data.redis.database=12"
})
class CouponIssueLaneIsolationTest {

    @Autowired CouponIssueQueue queue;
    @Autowired RedissonClient redis;

    @Test
    void issuesAnotherCouponWhileFirstCouponIsBlocked() throws Exception {
        CouponIssueJob blocked = job(10);
        CouponIssueJob sameCoupon = job(10);
        CouponIssueJob independent = job(20);
        queue.enqueue(blocked);
        queue.enqueue(sameCoupon);
        queue.enqueue(independent);

        CountDownLatch waiting = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CouponIssueOutbox outbox = mock(CouponIssueOutbox.class);
        when(outbox.issue(blocked)).thenAnswer(call -> {
            waiting.countDown();
            if (!release.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test did not release blocked lane");
            }
            return "ISSUED";
        });
        when(outbox.issue(sameCoupon)).thenReturn("ISSUED");
        when(outbox.issue(independent)).thenReturn("ISSUED");

        var executor = Executors.newFixedThreadPool(2);
        var worker = new CouponIssueWorker(queue, mock(CouponIssueService.class),
                outbox, redis, new CouponLaneExecutor(executor));
        try {
            worker.poll();
            assertThat(waiting.await(5, TimeUnit.SECONDS)).isTrue();
            await().untilAsserted(() -> assertThat(queue.status(independent.id())).isEqualTo("ISSUED"));
            assertThat(queue.status(sameCoupon.id())).isEqualTo("PENDING");
            release.countDown();
            await().untilAsserted(() -> assertThat(queue.status(sameCoupon.id())).isEqualTo("ISSUED"));
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private CouponIssueJob job(long couponId) {
        return new CouponIssueJob(UUID.randomUUID(), couponId, 1, LocalDateTime.now());
    }
}
