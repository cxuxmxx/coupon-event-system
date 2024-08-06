package me.project.joonggocore.domain.coupon;

import jakarta.persistence.*;
import lombok.*;
import me.project.joonggocore.exception.AppException;
import me.project.joonggocore.exception.ErrorCode;
import me.project.joonggocore.util.BaseEntity;

import java.time.LocalDateTime;

@Builder
@Getter
@Table(name = "coupons")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private CouponType couponType;

    private LocalDateTime issuedTime;

    private Integer totalQuantity;

    private int issuedQuantity;

    private int discountAmt;

    private int minAvailableAmt;

    private LocalDateTime issuedStartAt;

    private LocalDateTime issuedEndAt;

    public boolean isAvailableIssueQuantity() {
        if (totalQuantity == null) return true;
        else return totalQuantity > issuedQuantity;
    }

    public boolean isAvailableIssueDate() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(issuedStartAt) && now.isBefore(issuedEndAt);
    }

    public void issue() {
        if (!isAvailableIssueQuantity()) {
            throw new AppException(ErrorCode.INVALID_COUPON_QUANTITY,
                    ErrorCode.INVALID_COUPON_QUANTITY.getMessage()
                            + "total : %s, issued: %s".formatted(totalQuantity, issuedQuantity));
        }

        if (!isAvailableIssueDate()) {
            throw new AppException(ErrorCode.INVALID_COUPON_ISSUE_DATE,
                    ErrorCode.INVALID_COUPON_ISSUE_DATE.getMessage()
                            + "issueStartAt : %s, issueEndAt: %s".formatted(issuedStartAt, issuedEndAt));
        }
        issuedQuantity++;
    }
}
