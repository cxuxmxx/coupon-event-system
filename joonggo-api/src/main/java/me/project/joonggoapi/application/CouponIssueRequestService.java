package me.project.joonggoapi.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.project.joonggoapi.dto.CouponIssueRequest;
import me.project.joonggocore.service.CouponIssueService;
import me.project.joonggocore.util.DistributeLockExecutor;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class CouponIssueRequestService {

    private final CouponIssueService couponIssueService;
    private final DistributeLockExecutor distributeLockExecutor;

    public void issueRequestV1(CouponIssueRequest request) {
        log.info("[issueRequestV1] issue coupon start : couponId : {}, memberId : {}"
                , request.couponId(), request.memberId());

        distributeLockExecutor.execute("lock" + request.couponId(), 10000, 10000, () -> {
            couponIssueService.issueCoupon(request.couponId(), request.memberId());
        });

        log.info("[issueRequestV1] issue coupon end : couponId : {}, memberId : {}"
                , request.couponId(), request.memberId());
    }
}
