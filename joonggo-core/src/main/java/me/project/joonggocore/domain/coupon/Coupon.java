package me.project.joonggocore.domain.coupon;

import jakarta.persistence.*;
import lombok.*;
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

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CouponType couponType;

    @Column(nullable = false)
    private LocalDateTime issuedTime;

    private Integer totalQuantity;

    @Column(nullable = false)
    private int issuedQuantity;

    @Column(nullable = false)
    private int discountAmt;

    @Column(nullable = false)
    private int minAvailableAmt;

    @Column(nullable = false)
    private LocalDateTime issuedStartAt;

    @Column(nullable = false)
    private LocalDateTime issuedEndAt;


    private boolean isAvailableIssueQuantity() {
        if (totalQuantity == null) return true;
        else return totalQuantity > issuedQuantity;
    }

    private boolean isAvailableIssueDate() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(issuedStartAt) && now.isBefore(issuedEndAt);
    }

    public void issue() {
        if (!isAvailableIssueQuantity()) {
            throw new RuntimeException("발급 가능한 수량을 초과합니다.");
        }

        if (!isAvailableIssueDate()) {
            throw new RuntimeException("발급 가능한 일자가 아닙니다.");
        }
        issuedQuantity++;
    }
}
