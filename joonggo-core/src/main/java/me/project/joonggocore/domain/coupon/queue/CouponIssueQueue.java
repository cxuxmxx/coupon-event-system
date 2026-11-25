package me.project.joonggocore.domain.coupon.queue;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class CouponIssueQueue {

    private static final String PREFIX = "coupon:{issue}:";
    private static final String REQUESTS = PREFIX + "requests";
    private static final String ISSUES = PREFIX + "jobs";
    private static final String ENQUEUE = """
            if not redis.call('SET', KEYS[2], 'PENDING', 'NX') then return 0 end
            return redis.call('RPUSH', KEYS[1], ARGV[1])
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
            redis.call('LPOP', KEYS[1])
            return 1
            """;

    private final RedissonClient redis;
    private final RScript scripts;

    public CouponIssueQueue(RedissonClient redis) {
        this.redis = redis;
        this.scripts = redis.getScript(StringCodec.INSTANCE);
    }

    public void enqueue(CouponIssueJob job) {
        scripts.eval(RScript.Mode.READ_WRITE, ENQUEUE, RScript.ReturnType.INTEGER,
                List.of(REQUESTS, statusKey(job.id())), job.encode());
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

    public boolean complete(CouponIssueJob job, String result) {
        Long removed = scripts.eval(RScript.Mode.READ_WRITE, COMPLETE, RScript.ReturnType.INTEGER,
                List.of(ISSUES, statusKey(job.id())), job.encode(), result);
        return removed == 1;
    }

    private static String statusKey(UUID id) {
        return PREFIX + "status:" + id;
    }
}
