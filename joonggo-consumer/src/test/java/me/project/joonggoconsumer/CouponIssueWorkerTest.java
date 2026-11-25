package me.project.joonggoconsumer;

import me.project.joonggocore.domain.coupon.Coupon;
import me.project.joonggocore.domain.coupon.queue.CouponIssueJob;
import me.project.joonggocore.domain.coupon.queue.CouponIssueQueue;
import me.project.joonggocore.domain.coupon.repository.mysql.CouponIssueJpaRepository;
import me.project.joonggocore.domain.coupon.repository.mysql.CouponJpaRepository;
import me.project.joonggocore.domain.member.Member;
import me.project.joonggocore.domain.member.repository.MemberRepository;
import me.project.joonggocore.service.CouponIssueService;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "coupon.issue.worker.enabled=false",
        "spring.data.redis.database=15",
        "spring.datasource.url=jdbc:h2:mem:coupon-worker",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CouponIssueWorkerTest {

    @Autowired
    private CouponIssueQueue queue;
    @Autowired
    private CouponIssueService issueService;
    @Autowired
    private CouponJpaRepository coupons;
    @Autowired
    private CouponIssueJpaRepository issues;
    @Autowired
    private MemberRepository members;
    @Autowired
    private RedissonClient redis;

    @Test
    void processesInFifoOrderAndDoesNotDecrementStockAgainOnReplay() {
        LocalDateTime now = LocalDateTime.now();
        long couponId = coupons.save(Coupon.builder()
                .name("Limited coupon")
                .totalQuantity(1)
                .issuedStartAt(now.minusHours(2))
                .issuedEndAt(now.minusHours(1))
                .build()).getId();
        long firstMember = members.save(Member.builder().age(21).nickname("first").build()).getId();
        long secondMember = members.save(Member.builder().age(22).nickname("second").build()).getId();
        LocalDateTime requestedAt = now.minusMinutes(90);
        CouponIssueJob first = new CouponIssueJob(UUID.randomUUID(), couponId, firstMember, requestedAt);
        CouponIssueJob second = new CouponIssueJob(UUID.randomUUID(), couponId, secondMember, requestedAt);

        queue.enqueue(first);
        queue.enqueue(first);
        queue.enqueue(second);
        assertThat(queue.status(first.id())).isEqualTo("PENDING");
        assertThat(queue.peek()).isNull();

        CouponIssueService unavailable = mock(CouponIssueService.class);
        doThrow(new DataAccessResourceFailureException("database unavailable"))
                .when(unavailable).issueQueuedCoupon(first);
        CouponIssueWorker failedWorker = new CouponIssueWorker(queue, unavailable, redis);
        assertThatThrownBy(failedWorker::poll).isInstanceOf(DataAccessResourceFailureException.class);
        assertThat(queue.peek()).isEqualTo(first);
        assertThat(queue.status(first.id())).isEqualTo("PENDING");

        CouponIssueWorker worker = new CouponIssueWorker(queue, issueService, redis);
        worker.poll();

        assertThat(queue.status(first.id())).isEqualTo("ISSUED");
        assertThat(queue.status(second.id())).isEqualTo("INVALID_COUPON_QUANTITY");
        assertThat(queue.peek()).isNull();

        CouponIssueJob replay = new CouponIssueJob(UUID.randomUUID(), couponId, firstMember, requestedAt);
        queue.enqueue(replay);
        worker.poll();

        assertThat(queue.status(replay.id())).isEqualTo("ISSUED");
        assertThat(coupons.findById(couponId).orElseThrow().getIssuedQuantity()).isEqualTo(1);
        assertThat(issues.findAll()).singleElement().satisfies(issue -> {
            assertThat(issue.getCouponId()).isEqualTo(couponId);
            assertThat(issue.getMemberId()).isEqualTo(firstMember);
        });
    }
}
