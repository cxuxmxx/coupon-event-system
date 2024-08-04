package me.project.joonggocore.domain.coupon;

import jakarta.persistence.*;
import lombok.*;
import me.project.joonggocore.util.BaseEntity;

@Builder
@Getter
@Table(name = "coupon_issue")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class CouponIssue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long couponId;

    private Long memberId;


    public static CouponIssue createCouponIssue(long couponId, long memberId) {
        return CouponIssue.builder()
                .couponId(couponId)
                .memberId(memberId)
                .build();
    }
}
