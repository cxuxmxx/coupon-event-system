package me.project.joonggocore.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Component
public class DistributeLockExecutor {

    private final RedissonClient redissonClient;

    public void execute(String lockName, long waitMs, long leaseMs, Runnable logic) {
        //1. Lock 이름 지정 및 생성
        RLock lock = redissonClient.getLock(lockName);

        try {
            //락 획득 대기 시간 및 락 획득 후 반환(릴리즈) 시간 설정
            boolean isLock = lock.tryLock(waitMs, leaseMs, TimeUnit.MILLISECONDS);
            if (!isLock) {
                throw new IllegalStateException("[" + lockName + "]" + "lock 획득 실패");
            }
            logic.run();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            // Lock 반환
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

}
