package me.project.joonggocore.domain.coupon.queue;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CouponIssueQueue {

    private static final String PREFIX = "coupon:{issue}:";
    private static final String REQUESTS = PREFIX + "requests";
    private static final String DEAD_LETTERS = PREFIX + "dead-letters";
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
    private static final String TRANSFER = """
            local moved = 0
            for i = 1, tonumber(ARGV[1]) do
                local job = redis.call('LINDEX', KEYS[1], 0)
                if not job then break end
                redis.call('RPUSH', KEYS[2], job)
                redis.call('LPOP', KEYS[1])
                moved = moved + 1
            end
            return moved
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
            return 1
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

    public void transfer(int batchSize) {
        scripts.eval(RScript.Mode.READ_WRITE, TRANSFER, RScript.ReturnType.INTEGER,
                List.of(REQUESTS, ISSUES), batchSize);
    }

    public CouponIssueJob peek() {
        String value = redis.<String>getQueue(ISSUES, StringCodec.INSTANCE).peek();
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
                List.of(ISSUES, retryKey(job.id())), job.encode(),
                retry.attempts() + ":" + retry.nextAttemptAt());
    }

    public record Retry(int attempts, long nextAttemptAt) {
    }

    public boolean complete(CouponIssueJob job, String result) {
        String mappingKey = job.idempotencyKey() == null ? statusKey(job.id())
                : idempotencyRedisKey(job.idempotencyKey());
        List<Object> keys = List.of(ISSUES, statusKey(job.id()), retryKey(job.id()),
                mappingKey, DEAD_LETTERS);
        Long removed = scripts.eval(RScript.Mode.READ_WRITE, COMPLETE, RScript.ReturnType.INTEGER,
                keys, job.encode(), result, job.id().toString());
        return removed == 1;
    }

    private static String idempotencyRedisKey(String key) {
        return PREFIX + "idempotency:" + key;
    }

    private static String retryKey(UUID id) {
        return PREFIX + "retry:" + id;
    }

    private static String statusKey(UUID id) {
        return PREFIX + "status:" + id;
    }
}
