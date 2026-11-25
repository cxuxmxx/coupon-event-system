package me.project.joonggocore.service;

import lombok.RequiredArgsConstructor;
import me.project.joonggocore.domain.coupon.Coupon;
import me.project.joonggocore.domain.coupon.CouponIssue;
import me.project.joonggocore.domain.coupon.repository.mysql.CouponIssueJpaRepository;
import me.project.joonggocore.domain.coupon.repository.mysql.CouponIssueRepository;
import me.project.joonggocore.domain.coupon.repository.mysql.CouponJpaRepository;
import me.project.joonggocore.domain.coupon.queue.CouponIssueJob;
import me.project.joonggocore.domain.member.repository.MemberRepository;
import me.project.joonggocore.exception.AppException;
import me.project.joonggocore.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class CouponIssueService {

    private final CouponIssueRepository couponIssueRepository;
    private final CouponJpaRepository couponJpaRepository;
    private final CouponIssueJpaRepository couponIssueJpaRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void issueCoupon(long couponId, long memberId) {
        Coupon findCoupon = requireCoupon(couponJpaRepository.findByIdForUpdate(couponId));
        findCoupon.issue();
        saveIssuedCouponHist(couponId, memberId);
    }

    @Transactional
    public void issueQueuedCoupon(CouponIssueJob job) {
        Coupon coupon = requireCoupon(couponJpaRepository.findByIdForUpdate(job.couponId()));
        if (couponIssueRepository.findFirstCouponIssue(job.couponId(), job.memberId()) != null) {
            return;
        }
        if (!memberRepository.existsById(job.memberId())) {
            throw new AppException(ErrorCode.NOT_FOUND_MEMBER, ErrorCode.NOT_FOUND_MEMBER.getMessage());
        }
        coupon.issue(job.requestedAt());
        couponIssueJpaRepository.save(CouponIssue.createCouponIssue(job.couponId(), job.memberId()));
    }

    private void validateAlreadyIssuedCoupon(long couponId, long memberId) {
        CouponIssue firstCouponIssue = couponIssueRepository.findFirstCouponIssue(couponId, memberId);
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
        return requireCoupon(couponJpaRepository.findById(couponId));
    }

    private Coupon requireCoupon(Optional<Coupon> coupon) {
        return coupon.orElseThrow(() ->
                new AppException(ErrorCode.NOT_FOUND_COUPON, ErrorCode.NOT_FOUND_COUPON.getMessage()));
    }
}
