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

    public Result submit(long couponId, long memberId, String idempotencyKey) {
        Optional<CouponIssueJob> existing = idempotencyKey == null
                ? Optional.empty() : queue.findByIdempotencyKey(idempotencyKey);
        CouponIssueJob job = existing.orElseGet(() -> {
            LocalDateTime requestedAt = LocalDateTime.now();
            policyCache.validate(couponId, requestedAt);
            return queue.enqueue(new CouponIssueJob(UUID.randomUUID(), couponId, memberId,
                    requestedAt, idempotencyKey));
        });
        if (job.couponId() != couponId || job.memberId() != memberId) {
            throw new IdempotencyConflictException();
        }
        return new Result(job.id(), Status.PENDING, null);
    }

    public static class IdempotencyConflictException extends RuntimeException {
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
