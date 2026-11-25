package me.project.joonggocore.domain.coupon.queue;

import lombok.RequiredArgsConstructor;
import me.project.joonggocore.domain.coupon.Coupon;
import me.project.joonggocore.exception.AppException;
import me.project.joonggocore.exception.ErrorCode;
import me.project.joonggocore.service.CouponIssueService;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class CouponIssuePolicyCache {

    private final RedissonClient redis;
    private final CouponIssueService issueService;

    public void validate(long couponId, LocalDateTime requestedAt) {
        RBucket<String> bucket = redis.getBucket("coupon:policy:" + couponId, StringCodec.INSTANCE);
        String policy = bucket.get();
        if (policy == null) {
            Coupon coupon = issueService.findCouponById(couponId);
            if (coupon.getIssuedStartAt() == null || coupon.getIssuedEndAt() == null) {
                throw invalidDate();
            }
            policy = coupon.getIssuedStartAt() + "|" + coupon.getIssuedEndAt();
            bucket.set(policy, 30, TimeUnit.SECONDS);
        }

        String[] bounds = policy.split("\\|");
        if (!requestedAt.isAfter(LocalDateTime.parse(bounds[0]))
                || !requestedAt.isBefore(LocalDateTime.parse(bounds[1]))) {
            throw invalidDate();
        }
    }

    private AppException invalidDate() {
        return new AppException(ErrorCode.INVALID_COUPON_ISSUE_DATE,
                ErrorCode.INVALID_COUPON_ISSUE_DATE.getMessage());
    }
}
