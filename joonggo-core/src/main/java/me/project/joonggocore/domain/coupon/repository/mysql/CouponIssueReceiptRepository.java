package me.project.joonggocore.domain.coupon.repository.mysql;

import jakarta.persistence.LockModeType;
import me.project.joonggocore.domain.coupon.outbox.CouponIssueReceipt;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CouponIssueReceiptRepository extends JpaRepository<CouponIssueReceipt, Long> {

    Optional<CouponIssueReceipt> findByRequestId(String requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CouponIssueReceipt r where r.requestId = :id")
    Optional<CouponIssueReceipt> lockByRequestId(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CouponIssueReceipt r where r.keyHash = :hash")
    Optional<CouponIssueReceipt> lockByKeyHash(@Param("hash") String hash);

    Optional<CouponIssueReceipt> findByKeyHash(String hash);

    List<CouponIssueReceipt> findByCompletedAtIsNullAndIdGreaterThanOrderByIdAsc(long id, Pageable page);
}
