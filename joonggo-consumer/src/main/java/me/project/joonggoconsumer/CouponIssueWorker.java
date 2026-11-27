package me.project.joonggoconsumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.project.joonggocore.domain.coupon.queue.CouponIssueJob;
import me.project.joonggocore.domain.coupon.queue.CouponIssueQueue;
import me.project.joonggocore.domain.coupon.queue.CouponIssueQueue.Retry;
import me.project.joonggocore.exception.AppException;
import me.project.joonggocore.service.CouponIssueOutbox;
import me.project.joonggocore.service.CouponIssueService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.core.task.TaskRejectedException;

import java.time.Clock;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "coupon.issue.worker.enabled", matchIfMissing = true)
public class CouponIssueWorker {

    private final CouponIssueQueue queue;
    private final CouponIssueService issueService;
    private final CouponIssueOutbox outbox;
    private final RedissonClient redis;
    private final CouponLaneExecutor lanes;
    private Clock clock = Clock.systemUTC();
    private static final int MAX_ATTEMPTS = 5;

    void useClock(Clock clock) {
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${coupon.issue.worker.poll-delay-ms:250}")
    public void poll() {
        RLock relay = redis.getLock("coupon:issue:relay");
        if (relay.tryLock()) {
            try {
                publishPending();
                queue.transfer(100);
            } catch (RuntimeException e) {
                log.warn("Coupon relay could not publish pending requests", e);
            } finally {
                if (relay.isHeldByCurrentThread()) {
                    relay.unlock();
                }
            }
        }

        for (String couponId : queue.readyCoupons(clock.millis(), 32)) {
            try {
                lanes.execute(() -> {
                    try {
                        processLane(Long.parseLong(couponId));
                    } catch (RuntimeException failure) {
                        log.warn("Coupon lane {} could not be processed", couponId, failure);
                    }
                });
            } catch (TaskRejectedException busy) {
                break;
            }
        }
    }

    private void processLane(long couponId) {
        RLock lock = redis.getLock("coupon:issue:lane:" + couponId);
        if (!lock.tryLock()) {
            return;
        }

        try {
            queue.schedule(couponId, clock.millis() + 250);
            for (int i = 0; i < 100 && lock.isHeldByCurrentThread(); i++) {
                CouponIssueJob job = queue.peek(couponId);
                if (job == null) {
                    return;
                }
                if (!process(job)) {
                    queue.schedule(couponId, Math.max(clock.millis() + 250, queue.retry(job).nextAttemptAt()));
                    return;
                }
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void publishPending() {
        var page = outbox.pending(queue.relayCursor(), 100);
        for (var receipt : page) {
            queue.enqueue(receipt.job());
        }
        queue.advanceRelayCursor(page.size() < 100 ? 0 : page.get(page.size() - 1).getId());
    }

    private boolean process(CouponIssueJob job) {
        Retry retry = queue.retry(job);
        long now = clock.millis();
        if (retry.nextAttemptAt() > now) {
            return false;
        }

        String result;
        try {
            result = retry.attempts() >= MAX_ATTEMPTS
                    ? outbox.finish(job, reconcile(job)) : issue(job);
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
            return outbox.issue(job);
        } catch (AppException e) {
            log.info("Coupon request {} rejected with {}", job.id(), e.getErrorCode());
            return outbox.finish(job, e.getErrorCode().name());
        }
    }
}
