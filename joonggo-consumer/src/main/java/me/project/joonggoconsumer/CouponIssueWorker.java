package me.project.joonggoconsumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.project.joonggocore.domain.coupon.queue.CouponIssueJob;
import me.project.joonggocore.domain.coupon.queue.CouponIssueQueue;
import me.project.joonggocore.domain.coupon.queue.CouponIssueQueue.Retry;
import me.project.joonggocore.exception.AppException;
import me.project.joonggocore.service.CouponIssueService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "coupon.issue.worker.enabled", matchIfMissing = true)
public class CouponIssueWorker {

    private final CouponIssueQueue queue;
    private final CouponIssueService issueService;
    private final RedissonClient redis;
    private Clock clock = Clock.systemUTC();
    private static final int MAX_ATTEMPTS = 5;

    void useClock(Clock clock) {
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${coupon.issue.worker.poll-delay-ms:250}")
    public void poll() {
        queue.transfer(100);

        RLock lock = redis.getLock("coupon:issue:worker");
        if (!lock.tryLock()) {
            return;
        }

        try {
            for (int i = 0; i < 100 && lock.isHeldByCurrentThread(); i++) {
                CouponIssueJob job = queue.peek();
                if (job == null || !process(job)) {
                    return;
                }
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private boolean process(CouponIssueJob job) {
        Retry retry = queue.retry(job);
        long now = clock.millis();
        if (retry.nextAttemptAt() > now) {
            return false;
        }

        String result;
        try {
            result = retry.attempts() >= MAX_ATTEMPTS ? reconcile(job) : issue(job);
        } catch (RuntimeException failure) {
            int attempts = Math.min(MAX_ATTEMPTS, retry.attempts() + 1);
            queue.defer(job, new Retry(attempts, now + (1_000L << (attempts - 1))));
            log.warn("Coupon request {} delayed after attempt {}", job.id(), attempts, failure);
            return false;
        }
        return queue.complete(job, result);
    }

    private String reconcile(CouponIssueJob job) {
        try {
            return issueService.hasIssuedCoupon(job) ? "ISSUED" : "TECHNICAL_FAILURE";
        } catch (AppException e) {
            return e.getErrorCode().name();
        }
    }

    private String issue(CouponIssueJob job) {
        try {
            issueService.issueQueuedCoupon(job);
            return "ISSUED";
        } catch (AppException e) {
            log.info("Coupon request {} rejected with {}", job.id(), e.getErrorCode());
            return e.getErrorCode().name();
        }
    }
}
