package me.project.joonggocore.domain.coupon;

import jakarta.persistence.*;
import lombok.*;
import me.project.joonggocore.util.BaseEntity;

@Builder
@Getter
@Table(name = "coupon_issue", uniqueConstraints = @UniqueConstraint(
        name = "uk_coupon_issue_coupon_member", columnNames = {"coupon_id", "member_id"}))
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class CouponIssue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;


    public static CouponIssue createCouponIssue(long couponId, long memberId) {
        return CouponIssue.builder()
                .couponId(couponId)
                .memberId(memberId)
                .build();
    }
}
