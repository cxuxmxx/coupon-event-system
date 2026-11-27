package me.project.joonggocore.service;

import lombok.RequiredArgsConstructor;
import me.project.joonggocore.domain.coupon.outbox.CouponIssueReceipt;
import me.project.joonggocore.domain.coupon.queue.CouponIssueJob;
import me.project.joonggocore.domain.coupon.repository.mysql.CouponIssueReceiptRepository;
import me.project.joonggocore.domain.coupon.repository.mysql.CouponJpaRepository;
import me.project.joonggocore.exception.AppException;
import me.project.joonggocore.exception.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponIssueOutbox {

    private final CouponIssueReceiptRepository receipts;
    private final CouponJpaRepository coupons;
    private final CouponIssueService issues;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CouponIssueJob accept(CouponIssueJob job) {
        String hash = job.idempotencyKey() == null ? null : keyHash(job.memberId() + ":" + job.idempotencyKey());
        if (hash != null) {
            var existing = receipts.lockByKeyHash(hash);
            if (existing.isPresent()) {
                CouponIssueReceipt receipt = existing.get();
                if (!receipt.expiredAt(job.requestedAt())) {
                    return receipt.job();
                }
                receipt.releaseIdempotencyKey();
                receipts.flush();
            }
        }

        if (coupons.findByIdForUpdate(job.couponId()).isEmpty()) {
            throw new AppException(ErrorCode.NOT_FOUND_COUPON, ErrorCode.NOT_FOUND_COUPON.getMessage());
        }
        return receipts.saveAndFlush(new CouponIssueReceipt(job, hash)).job();
    }

    @Transactional(readOnly = true)
    public Optional<CouponIssueReceipt> findByKey(long memberId, String key) {
        LocalDateTime now = LocalDateTime.now();
        return receipts.findByKeyHash(keyHash(memberId + ":" + key))
                .filter(row -> !row.expiredAt(now))
                .or(() -> receipts.findByKeyHash(keyHash(key))
                        .filter(row -> row.getMemberId() == memberId && !row.expiredAt(now)));
    }

    @Transactional(readOnly = true)
    public Optional<CouponIssueReceipt> find(UUID requestId) {
        return receipts.findByRequestId(requestId.toString());
    }

    @Transactional(readOnly = true)
    public List<CouponIssueReceipt> pending(long afterId, int limit) {
        return receipts.findByCompletedAtIsNullAndIdGreaterThanOrderByIdAsc(afterId, PageRequest.of(0, limit));
    }

    @Transactional
    public String issue(CouponIssueJob job) {
        var receipt = receipts.lockByRequestId(job.id().toString());
        if (receipt.isEmpty()) {
            issues.issueQueuedCoupon(job);
            return "ISSUED";
        }
        CouponIssueReceipt row = receipt.get();
        if (row.getResult() == null) {
            issues.issueQueuedCoupon(row.job());
            row.complete("ISSUED");
        }
        return row.getResult();
    }

    @Transactional
    public String finish(CouponIssueJob job, String result) {
        var receipt = receipts.lockByRequestId(job.id().toString());
        if (receipt.isEmpty()) {
            return result;
        }
        CouponIssueReceipt row = receipt.get();
        row.complete(result);
        return row.getResult();
    }

    private static String keyHash(String key) {
        if (key == null) {
            return null;
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(key.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
