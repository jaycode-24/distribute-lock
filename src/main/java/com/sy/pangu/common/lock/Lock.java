package com.sy.pangu.common.lock;

import java.util.concurrent.TimeUnit;

/**
 * @author cheng.wang
 * @time 2023/12/5 22:13
 * @des 锁抽象
 */
public interface Lock {

    /**
     * 加锁，不设置过期时间，需要手动释放
     * 一直等待
     */
    void lock();

    /**
     * 尝试加锁，不设置过期时间，需要手动释放
     */
    boolean tryLock();

    /**
     * 尝试加锁
     * @param waitTime 等待时间
     * @param timeUnit 时间单位
     * @return 是否上锁成功
     * @throws InterruptedException 等待时间内被中断
     */
    boolean tryLock(long waitTime, TimeUnit timeUnit) throws InterruptedException;

    /**
     * 尝试加锁
     * @param waitTime 等待时间
     * @param leaseTime 释放时间
     * @param timeUnit 时间单位
     * @return 是否上锁成功
     * @throws InterruptedException 等待时间内被中断
     */
    boolean tryLock(long waitTime, long leaseTime, TimeUnit timeUnit) throws InterruptedException;

    /**
     * 解锁
     */
    void unLock();

    /**
     * 是否锁
     */
    boolean isLocked();
}
