package me.project.joonggoapi.application;

import lombok.RequiredArgsConstructor;
import me.project.joonggoapi.infrastructure.redis.CouponIssuePolicyCache;
import me.project.joonggocore.domain.coupon.queue.CouponIssueJob;
import me.project.joonggocore.domain.coupon.queue.CouponIssueQueue;
import me.project.joonggocore.domain.coupon.outbox.CouponIssueReceipt;
import me.project.joonggocore.service.CouponIssueOutbox;
import org.redisson.client.RedisException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AsyncCouponIssueService {

    private final CouponIssuePolicyCache policyCache;
    private final CouponIssueQueue queue;
    private final CouponIssueOutbox outbox;

    public Result submit(long couponId, long memberId, String idempotencyKey) {
        Optional<CouponIssueJob> existing = idempotencyKey == null ? Optional.empty()
                : outbox.findByKey(idempotencyKey).map(CouponIssueReceipt::job)
                        .or(() -> findLegacyKey(idempotencyKey));
        CouponIssueJob job = existing.orElseGet(() -> {
            LocalDateTime requestedAt = LocalDateTime.now();
            policyCache.validate(couponId, requestedAt);
            return accept(new CouponIssueJob(UUID.randomUUID(), couponId, memberId,
                    requestedAt, idempotencyKey));
        });
        if (job.couponId() != couponId || job.memberId() != memberId) {
            throw new IdempotencyConflictException();
        }
        return new Result(job.id(), Status.PENDING, null);
    }

    private Optional<CouponIssueJob> findLegacyKey(String key) {
        try {
            return queue.findByIdempotencyKey(key);
        } catch (RedisException unavailable) {
            return Optional.empty();
        }
    }

    private CouponIssueJob accept(CouponIssueJob job) {
        try {
            return outbox.accept(job);
        } catch (ConcurrencyFailureException | DataIntegrityViolationException e) {
            if (job.idempotencyKey() == null) {
                throw e;
            }
            return outbox.findByKey(job.idempotencyKey())
                    .map(CouponIssueReceipt::job)
                    .orElseThrow(() -> e);
        }
    }

    public static class IdempotencyConflictException extends RuntimeException {
    }

    public Optional<Result> findResult(UUID requestId) {
        Optional<CouponIssueReceipt> receipt = outbox.find(requestId);
        Optional<String> value = receipt.isPresent()
                ? receipt.filter(row -> !row.expiredAt(LocalDateTime.now()))
                        .map(row -> row.getResult() == null ? "PENDING" : row.getResult())
                : Optional.ofNullable(queue.status(requestId));
        return value.map(status -> switch (status) {
                    case "PENDING" -> new Result(requestId, Status.PENDING, null);
                    case "ISSUED" -> new Result(requestId, Status.ISSUED, null);
                    case "TECHNICAL_FAILURE" -> new Result(requestId, Status.FAILED, status);
                    default -> new Result(requestId, Status.REJECTED, status);
                });
    }

    public enum Status {
        PENDING, ISSUED, REJECTED, FAILED
    }

    public record Result(UUID requestId, Status status, String errorCode) {
    }
}
