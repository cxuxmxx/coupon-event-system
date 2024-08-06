package me.project.joonggocore.domain.coupon.repository.mysql;

import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import lombok.RequiredArgsConstructor;
import me.project.joonggocore.domain.coupon.CouponIssue;
import org.springframework.stereotype.Repository;

import static me.project.joonggocore.domain.coupon.QCouponIssue.couponIssue;

@RequiredArgsConstructor
@Repository
public class CouponIssueRepository {

    private final JPQLQueryFactory queryFactory;

    public CouponIssue findFirstCouponIssue(long couponId, long memberId) {
        return queryFactory.selectFrom(couponIssue)
                .where(couponIssue.couponId.eq(couponId))
                .where(couponIssue.memberId.eq(memberId))
                .fetchFirst();

    }
}
