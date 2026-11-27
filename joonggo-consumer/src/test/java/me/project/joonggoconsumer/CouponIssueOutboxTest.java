package me.project.joonggoconsumer;

import me.project.joonggocore.domain.coupon.Coupon;
import me.project.joonggocore.domain.coupon.queue.CouponIssueJob;
import me.project.joonggocore.domain.coupon.queue.CouponIssueQueue;
import me.project.joonggocore.domain.coupon.repository.mysql.CouponJpaRepository;
import me.project.joonggocore.domain.member.Member;
import me.project.joonggocore.domain.member.repository.MemberRepository;
import me.project.joonggocore.service.CouponIssueOutbox;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "coupon.issue.worker.enabled=false", "spring.data.redis.database=11"
})
class CouponIssueOutboxTest {

    @Autowired CouponIssueOutbox outbox;
    @Autowired CouponIssueQueue queue;
    @Autowired CouponJpaRepository coupons;
    @Autowired MemberRepository members;
    @Autowired RedissonClient redis;

    @Test
    void retainsReceiptsAcrossQueueLossAndDoesNotRepublishTerminalRequests() {
        LocalDateTime now = LocalDateTime.now();
        long coupon = coupons.save(Coupon.builder().name("Durable request").totalQuantity(1)
                .issuedStartAt(now.minusHours(1)).issuedEndAt(now.plusHours(1)).build()).getId();
        long member = members.save(Member.builder().nickname("durable-member").age(21).build()).getId();
        CouponIssueJob job = new CouponIssueJob(UUID.randomUUID(), coupon, member, now);

        outbox.accept(job);
        assertThat(queue.status(job.id())).isNull();
        queue.enqueue(outbox.pending(0, 100).get(0).job());
        assertThat(queue.status(job.id())).isEqualTo("PENDING");
        forgetRedisRequest(job.id());

        assertThat(outbox.find(job.id())).isPresent();
        queue.enqueue(outbox.pending(0, 100).get(0).job());
        assertThat(queue.status(job.id())).isEqualTo("PENDING");
        assertThat(outbox.issue(job)).isEqualTo("ISSUED");
        forgetRedisRequest(job.id());

        assertThat(outbox.pending(0, 100)).isEmpty();
        assertThat(outbox.find(job.id()).orElseThrow().getResult()).isEqualTo("ISSUED");
        assertThat(outbox.issue(job)).isEqualTo("ISSUED");
        assertThat(coupons.findById(coupon).orElseThrow().getIssuedQuantity()).isEqualTo(1);
    }
    private void forgetRedisRequest(UUID requestId) {
        var requests = redis.<String>getList("coupon:{issue}:requests", StringCodec.INSTANCE);
        requests.readAll().stream().filter(value -> value.startsWith(requestId + "|"))
                .forEach(requests::remove);
        redis.getBucket("coupon:{issue}:status:" + requestId).delete();
    }
}
