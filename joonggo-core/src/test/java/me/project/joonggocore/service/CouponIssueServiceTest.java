package me.project.joonggocore.service;

import me.project.joonggocore.config.TestConfig;
import me.project.joonggocore.domain.coupon.Coupon;
import me.project.joonggocore.domain.coupon.CouponIssue;
import me.project.joonggocore.domain.coupon.repository.mysql.CouponIssueJpaRepository;
import me.project.joonggocore.domain.coupon.repository.mysql.CouponIssueRepository;
import me.project.joonggocore.domain.coupon.repository.mysql.CouponJpaRepository;
import me.project.joonggocore.exception.AppException;
import me.project.joonggocore.exception.ErrorCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class CouponIssueServiceTest extends TestConfig {

    @Autowired
    CouponIssueService couponIssueService;

    @Autowired
    CouponJpaRepository couponJpaRepository;

    @Autowired
    CouponIssueJpaRepository couponIssueJpaRepository;

    @Autowired
    CouponIssueRepository couponIssueRepository;

    @BeforeEach
    public void before() {
        couponIssueJpaRepository.deleteAllInBatch();
        couponJpaRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("쿠폰 발급 이력이 존재하면 쿠폰 발급에 실패한다.")
    void coupon_issue_Fail() {
        //given
        CouponIssue couponIssue = CouponIssue.createCouponIssue(1L, 1L);
        couponIssueJpaRepository.save(couponIssue);

        //when
        AppException appException = Assertions.assertThrows(AppException.class, () -> {
            couponIssueService.saveIssuedCouponHist(1L, 1L);
        });

        //then
        assertThat(appException.getErrorCode()).isEqualTo(ErrorCode.ALREADY_ISSUED_USER);
    }

    @Test
    @DisplayName("쿠폰 발급 기한이 만료되면 쿠폰 발급에 실패한다.")
    void coupon_issue_Fail2() {
        //given
        Coupon coupon = Coupon.builder()
                .id(1L)
                .issuedStartAt(LocalDateTime.now().minusDays(7))
                .issuedEndAt(LocalDateTime.now().minusDays(5))
                .build();

        //when
        AppException appException = Assertions.assertThrows(AppException.class, () -> {
            coupon.issue();
        });

        //then
        assertThat(appException.getErrorCode()).isEqualTo(ErrorCode.INVALID_COUPON_ISSUE_DATE);
    }

    @Test
    @DisplayName("쿠폰 발급 이력이 존재하지 않고 발급 기한이 유효하다면 쿠폰 발급에 성공한다.")
    void coupon_issue_success() {
        //given
        Coupon coupon = Coupon.builder()
                .id(1L)
                .name("선착순 쿠폰")
                .totalQuantity(100)
                .issuedQuantity(10)
                .issuedStartAt(LocalDateTime.now().minusDays(1))
                .issuedEndAt(LocalDateTime.now().plusDays(2))
                .build();
        couponJpaRepository.save(coupon);

        //when
        couponIssueService.issueCoupon(1L, 1L);

        //then
        assertThat(coupon.getIssuedQuantity()).isEqualTo(10);

    }
}