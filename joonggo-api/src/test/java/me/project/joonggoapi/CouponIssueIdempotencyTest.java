package me.project.joonggoapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.project.joonggocore.domain.coupon.Coupon;
import me.project.joonggocore.domain.coupon.queue.CouponIssueJob;
import me.project.joonggocore.domain.coupon.queue.CouponIssueQueue;
import me.project.joonggocore.domain.coupon.repository.mysql.CouponJpaRepository;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.data.redis.database=14")
@AutoConfigureMockMvc
class CouponIssueIdempotencyTest {

    private static final String ENDPOINT = "/api/v2/coupon/issue";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private CouponJpaRepository coupons;
    @Autowired
    private CouponIssueQueue queue;
    @Autowired
    private RedissonClient redis;

    @Test
    void concurrentReplaysShareOneQueuedRequestAndConflictingBodyIsRejected() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        long couponId = coupons.save(Coupon.builder()
                .name("Idempotency test")
                .totalQuantity(1)
                .issuedStartAt(now.minusHours(1))
                .issuedEndAt(now.plusHours(1))
                .build()).getId();
        String key = UUID.randomUUID().toString();
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        MvcResult first;
        try {
            var a = executor.submit(() -> {
                start.await();
                return accepted(key, couponId);
            });
            var b = executor.submit(() -> {
                start.await();
                return accepted(key, couponId);
            });
            start.countDown();
            first = a.get(10, TimeUnit.SECONDS);
            var second = b.get(10, TimeUnit.SECONDS);
            assertThat(second.getResponse().getHeader("Location"))
                    .isEqualTo(first.getResponse().getHeader("Location"));
            assertThat(second.getResponse().getContentAsString())
                    .isEqualTo(first.getResponse().getContentAsString());
        } finally {
            executor.shutdownNow();
        }

        mvc.perform(post(ENDPOINT).header("Idempotency-Key", key)
                .contentType("application/json").content(body(couponId, 2)))
                .andExpect(status().isConflict());

        UUID id = UUID.fromString(json.readTree(first.getResponse().getContentAsString())
                .get("requestId").asText());
        var mapping = redis.getBucket("coupon:{issue}:idempotency:" + key);
        assertThat(mapping.remainTimeToLive()).isEqualTo(-1);
        queue.transfer(100);
        CouponIssueJob job = queue.peek();
        assertThat(job.id()).isEqualTo(id);
        assertThat(queue.complete(job, "ISSUED")).isTrue();
        assertThat(queue.peek()).isNull();
        assertThat(mapping.remainTimeToLive()).isBetween(1L, 86_400_000L);

        assertThat(accepted(key, couponId).getResponse().getContentAsString())
                .isEqualTo(first.getResponse().getContentAsString());
        queue.transfer(100);
        assertThat(queue.peek()).isNull();
        mvc.perform(get(first.getResponse().getHeader("Location")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"));
    }

    private MvcResult accepted(String key, long couponId) throws Exception {
        return mvc.perform(post(ENDPOINT).header("Idempotency-Key", key)
                .contentType("application/json").content(body(couponId, 1)))
                .andExpect(status().isAccepted()).andReturn();
    }

    private String body(long couponId, long memberId) {
        return "{\"couponId\":%d,\"memberId\":%d}".formatted(couponId, memberId);
    }
}
