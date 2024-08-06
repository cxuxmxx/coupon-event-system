package me.project.joonggocore.domain.coupon.repository.mysql;

import me.project.joonggocore.domain.coupon.CouponIssue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponIssueJpaRepository extends JpaRepository<CouponIssue, Long> {
}
