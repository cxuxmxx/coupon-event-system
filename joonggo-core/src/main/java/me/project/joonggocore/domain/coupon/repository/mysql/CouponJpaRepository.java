package me.project.joonggocore.domain.coupon.repository.mysql;

import me.project.joonggocore.domain.coupon.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {
}
