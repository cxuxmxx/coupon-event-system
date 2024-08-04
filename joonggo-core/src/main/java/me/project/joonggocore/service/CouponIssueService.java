package me.project.joonggocore.service;

import lombok.RequiredArgsConstructor;
import me.project.joonggocore.domain.coupon.Coupon;
import me.project.joonggocore.domain.coupon.CouponIssue;
import me.project.joonggocore.domain.coupon.repository.mysql.CouponIssueJpaRepository;
import me.project.joonggocore.domain.coupon.repository.mysql.CouponIssueRepository;
import me.project.joonggocore.domain.coupon.repository.mysql.CouponJpaRepository;
import me.project.joonggocore.exception.AppException;
import me.project.joonggocore.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class CouponIssueService {

    private final CouponIssueRepository couponIssueRepository;
    private final CouponJpaRepository couponJpaRepository;
    private final CouponIssueJpaRepository couponIssueJpaRepository;

    @Transactional
    public void issueCoupon(long couponId, long memberId) {
        Coupon findCoupon = findCouponById(couponId);
        findCoupon.issue();
        saveIssuedCouponHist(couponId, memberId);
    }

    private void validateAlreadyIssuedCoupon(long memberId, long couponId) {
        CouponIssue firstCouponIssue = couponIssueRepository.findFirstCouponIssue(memberId, couponId);
        if (firstCouponIssue != null)
            throw new AppException(ErrorCode.ALREADY_ISSUED_USER, ErrorCode.ALREADY_ISSUED_USER.getMessage());
    }

    @Transactional
    public CouponIssue saveIssuedCouponHist(long couponId, long memberId) {
        validateAlreadyIssuedCoupon(couponId, memberId);
        CouponIssue couponIssue = CouponIssue.createCouponIssue(couponId, memberId);
        return couponIssueJpaRepository.save(couponIssue);
    }

    public Coupon findCouponById(long couponId) {
        return couponJpaRepository.findById(couponId)
                .orElseThrow(() -> {
                    throw new AppException(ErrorCode.NOT_FOUND_COUPON, ErrorCode.NOT_FOUND_COUPON.getMessage());
                });
    }
}
