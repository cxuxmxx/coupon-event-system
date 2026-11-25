package me.project.joonggoapi.application;

import lombok.RequiredArgsConstructor;
import me.project.joonggoapi.infrastructure.redis.CouponIssuePolicyCache;
import me.project.joonggocore.domain.coupon.queue.CouponIssueJob;
import me.project.joonggocore.domain.coupon.queue.CouponIssueQueue;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AsyncCouponIssueService {

    private final CouponIssuePolicyCache policyCache;
    private final CouponIssueQueue queue;

    public Result submit(long couponId, long memberId) {
        UUID requestId = UUID.randomUUID();
        LocalDateTime requestedAt = LocalDateTime.now();
        policyCache.validate(couponId, requestedAt);
        queue.enqueue(new CouponIssueJob(requestId, couponId, memberId, requestedAt));
        return new Result(requestId, Status.PENDING, null);
    }

    public Optional<Result> findResult(UUID requestId) {
        return Optional.ofNullable(queue.status(requestId))
                .map(value -> switch (value) {
                    case "PENDING" -> new Result(requestId, Status.PENDING, null);
                    case "ISSUED" -> new Result(requestId, Status.ISSUED, null);
                    default -> new Result(requestId, Status.REJECTED, value);
                });
    }

    public enum Status {
        PENDING, ISSUED, REJECTED
    }

    public record Result(UUID requestId, Status status, String errorCode) {
    }
}
