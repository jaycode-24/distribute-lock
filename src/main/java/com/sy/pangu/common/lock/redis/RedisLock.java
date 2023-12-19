package com.sy.pangu.common.lock.redis;

import com.sy.pangu.common.lock.Lock;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.redisson.api.RLock;

import java.util.concurrent.TimeUnit;

/**
 * @author cheng.wang
 * @time 2023/12/5 22:16
 * @des 基于redis实现的锁
 */
@Data
@AllArgsConstructor
public class RedisLock implements Lock {

    private RLock rLock;

    @Override
    public void lock() {
        rLock.lock();
    }
    @Override
    public boolean tryLock(){
        return rLock.tryLock();
    }
    @Override
    public boolean tryLock(long waitTime, TimeUnit timeUnit) throws InterruptedException {
        return rLock.tryLock(waitTime, timeUnit);
    }
    @Override
    public boolean tryLock(long waitTime, long leaseTime, TimeUnit timeUnit) throws InterruptedException {
        return rLock.tryLock(waitTime, leaseTime, timeUnit);
    }

    @Override
    public void unLock() {
        rLock.unlock();
    }

    @Override
    public boolean isLocked() {
        return rLock.isLocked();
    }
}
