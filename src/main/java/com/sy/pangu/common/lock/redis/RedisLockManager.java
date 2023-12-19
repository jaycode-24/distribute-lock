package com.sy.pangu.common.lock.redis;

import com.sy.pangu.common.lock.Lock;
import com.sy.pangu.common.lock.LockManager;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @author cheng.wang
 * @time 2023/12/5 22:15
 * @des 基于redis实现的锁管理器
 */
@Data
@Builder
@Slf4j
public class RedisLockManager implements LockManager {

    private RedissonClient redissonClient;

    @Override
    public Lock createLock(String sys, String biz, String bizKey) {
        RLock rLock = redissonClient.getLock(String.format("%s:%s:%s", sys, biz, bizKey));
        return new RedisLock(rLock);
    }

    @Override
    public Lock createFairLock(String sys, String biz, String bizKey) {
        RLock rLock = redissonClient.getFairLock(String.format("%s:%s:%s", sys, biz, bizKey));
        return new RedisLock(rLock);
    }

    @Override
    public void destroy() throws Exception {
        log.info("RedisLock shutdown ...");
        redissonClient.shutdown();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("RedisLock start ...");
    }
}
