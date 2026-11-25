package me.project.joonggoconsumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.project.joonggocore.domain.coupon.queue.CouponIssueJob;
import me.project.joonggocore.domain.coupon.queue.CouponIssueQueue;
import me.project.joonggocore.exception.AppException;
import me.project.joonggocore.service.CouponIssueService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "coupon.issue.worker.enabled", matchIfMissing = true)
public class CouponIssueWorker {

    private final CouponIssueQueue queue;
    private final CouponIssueService issueService;
    private final RedissonClient redis;

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
                if (job == null || !queue.complete(job, issue(job))) {
                    return;
                }
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
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
