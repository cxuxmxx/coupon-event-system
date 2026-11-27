package me.project.joonggoconsumer;

import me.project.joonggocore.domain.coupon.Coupon;
import me.project.joonggocore.domain.coupon.CouponIssue;
import me.project.joonggocore.domain.coupon.queue.CouponIssueJob;
import me.project.joonggocore.domain.coupon.queue.CouponIssueQueue;
import me.project.joonggocore.domain.coupon.repository.mysql.CouponIssueJpaRepository;
import me.project.joonggocore.domain.coupon.repository.mysql.CouponJpaRepository;
import me.project.joonggocore.domain.member.Member;
import me.project.joonggocore.domain.member.repository.MemberRepository;
import me.project.joonggocore.service.CouponIssueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfEnvironmentVariable(named = "COUPON_TEST_MYSQL_URL", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "coupon.issue.worker.enabled=false", "spring.data.redis.database=13",
        "spring.datasource.url=${COUPON_TEST_MYSQL_URL}",
        "spring.datasource.username=${COUPON_TEST_MYSQL_USER:root}",
        "spring.datasource.password=${COUPON_TEST_MYSQL_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CouponIssueMysqlIntegrationTest {

    @Autowired CouponIssueQueue queue;
    @Autowired CouponIssueService service;
    @Autowired CouponJpaRepository coupons;
    @Autowired CouponIssueJpaRepository issues;
    @Autowired MemberRepository members;
    @Autowired RedissonClient redis;

    @Test
    void enforcesInventoryMemberUniquenessAndReplayAfterCommit() {
        LocalDateTime now = LocalDateTime.now();
        long coupon = coupons.save(Coupon.builder().name("Limited").totalQuantity(100)
                .issuedStartAt(now.minusHours(1)).issuedEndAt(now.plusHours(1)).build()).getId();
        List<Member> participants = members.saveAll(IntStream.rangeClosed(1, 1000)
                .<Member>mapToObj(i -> Member.builder().age(i).nickname("member-" + i).build()).toList());
        List<CouponIssueJob> jobs = participants.stream()
                .map(member -> new CouponIssueJob(UUID.randomUUID(), coupon, member.getId(), now))
                .toList();
        jobs.forEach(queue::enqueue);
        queue.transfer(100);
        service.issueQueuedCoupon(jobs.get(0));

        var worker = new CouponIssueWorker(queue, service, redis);
        for (int i = 0; i < 10; i++) {
            worker.poll();
        }
        assertThat(queue.status(jobs.get(0).id())).isEqualTo("ISSUED");
        assertThat(queue.status(jobs.get(99).id())).isEqualTo("ISSUED");
        assertThat(queue.status(jobs.get(100).id())).isEqualTo("INVALID_COUPON_QUANTITY");
        assertThat(issues.count()).isEqualTo(100);
        assertThat(coupons.findById(coupon).orElseThrow().getIssuedQuantity()).isEqualTo(100);

        long winner = participants.get(0).getId();
        var replay = new CouponIssueJob(UUID.randomUUID(), coupon, winner, now, UUID.randomUUID().toString());
        queue.enqueue(replay);
        worker.poll();
        assertThat(queue.status(replay.id())).isEqualTo("ISSUED");
        assertThat(issues.count()).isEqualTo(100);
        assertThat(coupons.findById(coupon).orElseThrow().getIssuedQuantity()).isEqualTo(100);
        assertThatThrownBy(() -> issues.saveAndFlush(CouponIssue.createCouponIssue(coupon, winner)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
