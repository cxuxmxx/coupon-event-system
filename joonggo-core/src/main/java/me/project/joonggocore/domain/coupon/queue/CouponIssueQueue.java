package me.project.joonggocore.domain.coupon.queue;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CouponIssueQueue {

    private static final String PREFIX = "coupon:{issue}:";
    private static final String REQUESTS = PREFIX + "requests";
    private static final String DEAD_LETTERS = PREFIX + "dead-letters";
    private static final String RELAY_CURSOR = PREFIX + "relay-cursor";
    private static final String ACTIVE = PREFIX + "active-coupons";
    private static final String ISSUES = PREFIX + "jobs";
    private static final String ENQUEUE = """
            if not redis.call('SET', KEYS[2], 'PENDING', 'NX') then return 0 end
            return redis.call('RPUSH', KEYS[1], ARGV[1])
            """;
    private static final String ENQUEUE_IDEMPOTENT = """
            local existing = redis.call('GET', KEYS[3])
            if existing then return existing end
            redis.call('SET', KEYS[2], 'PENDING')
            redis.call('RPUSH', KEYS[1], ARGV[1])
            redis.call('SET', KEYS[3], ARGV[1])
            return ARGV[1]
            """;
    private static final String COMPLETE = """
            if redis.call('LINDEX', KEYS[1], 0) ~= ARGV[1] then return 0 end
            redis.call('SET', KEYS[2], ARGV[2], 'EX', 86400)
            if redis.call('GET', KEYS[4]) == ARGV[1] then
                redis.call('EXPIRE', KEYS[4], 86400)
            end
            redis.call('DEL', KEYS[3])
            if ARGV[2] == 'TECHNICAL_FAILURE' then
                redis.call('HSET', KEYS[5], ARGV[3], ARGV[1])
            end
            redis.call('LPOP', KEYS[1])
            if redis.call('LLEN', KEYS[1]) == 0 then
                redis.call('ZREM', KEYS[6], ARGV[4])
            end
            return 1
            """;

    private static final String ROUTE = """
            if redis.call('LINDEX', KEYS[1], 0) ~= ARGV[1] then return 0 end
            redis.call('RPUSH', KEYS[2], ARGV[1])
            redis.call('ZADD', KEYS[3], 'NX', ARGV[3], ARGV[2])
            redis.call('LPOP', KEYS[1])
            return 1
            """;

    private static final String SCHEDULE = """
            if redis.call('LLEN', KEYS[1]) == 0 then return 0 end
            return redis.call('ZADD', KEYS[2], 'XX', ARGV[2], ARGV[1])
            """;

    private static final String DEFER = """
            if redis.call('LINDEX', KEYS[1], 0) ~= ARGV[1] then return 0 end
            redis.call('SET', KEYS[2], ARGV[2])
            return 1
            """;

    private final RedissonClient redis;
    private final RScript scripts;

    public CouponIssueQueue(RedissonClient redis) {
        this.redis = redis;
        this.scripts = redis.getScript(StringCodec.INSTANCE);
    }

    public CouponIssueJob enqueue(CouponIssueJob job) {
        if (job.idempotencyKey() == null) {
            scripts.eval(RScript.Mode.READ_WRITE, ENQUEUE, RScript.ReturnType.INTEGER,
                    List.of(REQUESTS, statusKey(job.id())), job.encode());
            return job;
        }
        String existingOrNew = scripts.eval(RScript.Mode.READ_WRITE, ENQUEUE_IDEMPOTENT,
                RScript.ReturnType.VALUE, List.of(REQUESTS, statusKey(job.id()),
                        idempotencyRedisKey(job.idempotencyKey())), job.encode());
        return CouponIssueJob.decode(existingOrNew);
    }

    public Optional<CouponIssueJob> findByIdempotencyKey(String key) {
        return Optional.ofNullable(redis.<String>getBucket(idempotencyRedisKey(key), StringCodec.INSTANCE).get())
                .map(CouponIssueJob::decode);
    }

    public String status(UUID id) {
        return redis.<String>getBucket(statusKey(id), StringCodec.INSTANCE).get();
    }

    public long relayCursor() {
        String value = redis.<String>getBucket(RELAY_CURSOR, StringCodec.INSTANCE).get();
        return value == null ? 0 : Long.parseLong(value);
    }

    public void advanceRelayCursor(long afterId) {
        redis.<String>getBucket(RELAY_CURSOR, StringCodec.INSTANCE).set(Long.toString(afterId));
    }

    public void transfer(int batchSize) {
        int remaining = route(ISSUES, batchSize);
        route(REQUESTS, remaining);
    }

    private int route(String source, int remaining) {
        var queue = redis.<String>getQueue(source, StringCodec.INSTANCE);
        while (remaining > 0) {
            String value = queue.peek();
            if (value == null) {
                break;
            }
            CouponIssueJob job = CouponIssueJob.decode(value);
            scripts.eval(RScript.Mode.READ_WRITE, ROUTE, RScript.ReturnType.INTEGER,
                    List.of(source, issueLane(job.couponId()), ACTIVE), value,
                    Long.toString(job.couponId()), System.currentTimeMillis());
            remaining--;
        }
        return remaining;
    }

    public Collection<String> readyCoupons(long now, int maxCoupons) {
        return redis.<String>getScoredSortedSet(ACTIVE, StringCodec.INSTANCE)
                .valueRange(0, true, now, true, 0, maxCoupons);
    }

    public void schedule(long couponId, long dueAt) {
        scripts.eval(RScript.Mode.READ_WRITE, SCHEDULE, RScript.ReturnType.INTEGER,
                List.of(issueLane(couponId), ACTIVE), Long.toString(couponId), dueAt);
    }

    public CouponIssueJob peek(long couponId) {
        String value = redis.<String>getQueue(issueLane(couponId), StringCodec.INSTANCE).peek();
        return value == null ? null : CouponIssueJob.decode(value);
    }

    public Retry retry(CouponIssueJob job) {
        String value = redis.<String>getBucket(retryKey(job.id()), StringCodec.INSTANCE).get();
        if (value == null) {
            return new Retry(0, 0);
        }
        String[] parts = value.split(":");
        return new Retry(Integer.parseInt(parts[0]), Long.parseLong(parts[1]));
    }

    public void defer(CouponIssueJob job, Retry retry) {
        scripts.eval(RScript.Mode.READ_WRITE, DEFER, RScript.ReturnType.INTEGER,
                List.of(issueLane(job.couponId()), retryKey(job.id())), job.encode(),
                retry.attempts() + ":" + retry.nextAttemptAt());
    }

    public record Retry(int attempts, long nextAttemptAt) {
    }

    public boolean complete(CouponIssueJob job, String result) {
        String mappingKey = job.idempotencyKey() == null ? statusKey(job.id())
                : idempotencyRedisKey(job.idempotencyKey());
        List<Object> keys = List.of(issueLane(job.couponId()), statusKey(job.id()), retryKey(job.id()),
                mappingKey, DEAD_LETTERS, ACTIVE);
        Long removed = scripts.eval(RScript.Mode.READ_WRITE, COMPLETE, RScript.ReturnType.INTEGER,
                keys, job.encode(), result, job.id().toString(), Long.toString(job.couponId()));
        return removed == 1;
    }

    private static String idempotencyRedisKey(String key) {
        return PREFIX + "idempotency:" + key;
    }

    private static String issueLane(long couponId) {
        return ISSUES + ":" + couponId;
    }

    private static String retryKey(UUID id) {
        return PREFIX + "retry:" + id;
    }

    private static String statusKey(UUID id) {
        return PREFIX + "status:" + id;
    }
}
