package me.project.joonggocore.domain.coupon.queue;

import java.time.LocalDateTime;
import java.util.UUID;

public record CouponIssueJob(UUID id, long couponId, long memberId, LocalDateTime requestedAt) {

    String encode() {
        return id + "|" + couponId + "|" + memberId + "|" + requestedAt;
    }

    static CouponIssueJob decode(String value) {
        String[] fields = value.split("\\|");
        return new CouponIssueJob(UUID.fromString(fields[0]), Long.parseLong(fields[1]),
                Long.parseLong(fields[2]), LocalDateTime.parse(fields[3]));
    }
}
