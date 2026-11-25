package me.project.joonggocore.domain.coupon.queue;

import java.time.LocalDateTime;
import java.util.UUID;

public record CouponIssueJob(UUID id, long couponId, long memberId, LocalDateTime requestedAt,
                             String idempotencyKey) {

    public CouponIssueJob(UUID id, long couponId, long memberId, LocalDateTime requestedAt) {
        this(id, couponId, memberId, requestedAt, null);
    }

    String encode() {
        String payload = id + "|" + couponId + "|" + memberId + "|" + requestedAt;
        return idempotencyKey == null ? payload : payload + "|" + idempotencyKey;
    }

    static CouponIssueJob decode(String value) {
        String[] fields = value.split("\\|");
        return new CouponIssueJob(UUID.fromString(fields[0]), Long.parseLong(fields[1]),
                Long.parseLong(fields[2]), LocalDateTime.parse(fields[3]), fields.length == 5 ? fields[4] : null);
    }
}
