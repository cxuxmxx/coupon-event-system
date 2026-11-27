package me.project.joonggocore.domain.coupon.outbox;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.project.joonggocore.domain.coupon.queue.CouponIssueJob;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "coupon_issue_receipt", indexes = @Index(
        name = "idx_coupon_receipt_outbox", columnList = "completed_at,id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponIssueReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String requestId;

    @Column(nullable = false)
    private long couponId;

    @Column(nullable = false)
    private long memberId;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    @Column(unique = true, length = 64)
    private String keyHash;

    @Column(length = 64)
    private String result;

    private LocalDateTime completedAt;

    public CouponIssueReceipt(CouponIssueJob job, String keyHash) {
        requestId = job.id().toString();
        couponId = job.couponId();
        memberId = job.memberId();
        requestedAt = job.requestedAt();
        this.keyHash = keyHash;
    }

    public CouponIssueJob job() {
        return new CouponIssueJob(UUID.fromString(requestId), couponId, memberId, requestedAt);
    }

    public void complete(String result) {
        if (completedAt == null) {
            this.result = result;
            completedAt = LocalDateTime.now();
        }
    }

    public boolean expiredAt(LocalDateTime now) {
        return completedAt != null && !completedAt.plusHours(24).isAfter(now);
    }

    public void releaseIdempotencyKey() {
        keyHash = null;
    }
}
