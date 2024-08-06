package me.project.joonggocore.domain.coupon;

import me.project.joonggocore.exception.AppException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static me.project.joonggocore.exception.ErrorCode.INVALID_COUPON_ISSUE_DATE;
import static me.project.joonggocore.exception.ErrorCode.INVALID_COUPON_QUANTITY;
import static org.assertj.core.api.Assertions.assertThat;

class CouponTest {


    @Test
    void availableIssueQuanttiy_success() {
        //given
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(99)
                .build();
        //when
        boolean isAvailableIssueQuantity = coupon.isAvailableIssueQuantity();

        //then
        assertThat(isAvailableIssueQuantity).isEqualTo(true);
    }

    @Test
    void availableIssueQuanttiy_fail() {
        //given
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(100)
                .build();
        //when
        boolean isAvailableIssueQuantity = coupon.isAvailableIssueQuantity();

        //then
        assertThat(isAvailableIssueQuantity).isEqualTo(false);
    }



    @Test
    void availableIssueQuanttiy_not_validate() {
        //given
        Coupon coupon = Coupon.builder()
                .totalQuantity(null)
                .issuedQuantity(100)
                .build();
        //when
        boolean isAvailableIssueQuantity = coupon.isAvailableIssueQuantity();

        //then
        assertThat(isAvailableIssueQuantity).isEqualTo(true);
    }

    @Test
    @DisplayName("발급 기간이 유효하다면 True를 반환한다.")
    void availableIssueDate_success() {
        //given
        Coupon coupon = Coupon.builder()
                .issuedStartAt(LocalDateTime.now().minusDays(7))
                .issuedEndAt(LocalDateTime.now().plusDays(7))
                .build();

        //when
        boolean isValidIssueDate = coupon.isAvailableIssueDate();

        //then
        assertThat(isValidIssueDate).isEqualTo(true);
    }

    @Test
    @DisplayName("발급 기간이 시작되지 않았다면 False를 반환한다.")
    void availableIssueDate_fail() {
        //given
        Coupon coupon = Coupon.builder()
                .issuedStartAt(LocalDateTime.now().plusDays(7))
                .issuedEndAt(LocalDateTime.now().plusDays(8))
                .build();

        //when
        boolean isValidIssueDate = coupon.isAvailableIssueDate();

        //then
        assertThat(isValidIssueDate).isEqualTo(false);
    }

    @Test
    @DisplayName("발급 기간이 지났다면 False를 반환한다.")
    void availableIssueDate_fail2() {
        //given
        Coupon coupon = Coupon.builder()
                .issuedStartAt(LocalDateTime.now().minusDays(9))
                .issuedEndAt(LocalDateTime.now().minusDays(8))
                .build();

        //when
        boolean isValidIssueDate = coupon.isAvailableIssueDate();

        //then
        assertThat(isValidIssueDate).isEqualTo(false);
    }

    @Test
    @DisplayName("발급 수량과 기한이 유효하다면 쿠폰 발급은 성공한다.")
    void issue_success() {
        //given
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(99)
                .issuedStartAt(LocalDateTime.now().minusDays(1))
                .issuedEndAt(LocalDateTime.now().plusDays(2))
                .build();

        //when
        coupon.issue();

        //then
        assertThat(coupon.getIssuedQuantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("발급 수량이 초과되면 예외를 반환한다.")
    void issue_fail() {
        //given
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(100)
                .issuedStartAt(LocalDateTime.now().minusDays(1))
                .issuedEndAt(LocalDateTime.now().plusDays(2))
                .build();

        AppException appException = Assertions.assertThrows(AppException.class, coupon::issue);
        assertThat(appException.getErrorCode()).isEqualTo(INVALID_COUPON_QUANTITY);

    }

    @Test
    @DisplayName("발급 기한이 유효하지 않으면 예외를 반환한다.")
    void issue_fail2() {
        //given
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(99)
                .issuedStartAt(LocalDateTime.now().minusDays(2))
                .issuedEndAt(LocalDateTime.now().minusDays(1))
                .build();

        AppException appException = Assertions.assertThrows(AppException.class, coupon::issue);
        assertThat(appException.getErrorCode()).isEqualTo(INVALID_COUPON_ISSUE_DATE);
    }

    @Test
    @DisplayName("발급 기한이 유효하지 않으면 예외를 반환한다.")
    void issue_fail3() {
        //given
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(99)
                .issuedStartAt(LocalDateTime.now().plusDays(2))
                .issuedEndAt(LocalDateTime.now().plusDays(3))
                .build();

        AppException appException = Assertions.assertThrows(AppException.class, coupon::issue);
        assertThat(appException.getErrorCode()).isEqualTo(INVALID_COUPON_ISSUE_DATE);

    }


}