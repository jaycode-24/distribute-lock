package com.sy.pangu.common.lock.zk;

import com.sy.pangu.common.lock.Lock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.springframework.util.backoff.FixedBackOff;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author cheng.wang
 * @time 2023/12/13 17:56
 * @des 延时任务释放zk锁
 */
public class ScheduledExecutorUtil {

    private final static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(20);

    public static void schedule(InterProcessMutex zkLock, Long leaseTime, TimeUnit timeUnit){
        scheduledExecutorService.schedule(new RetryReleaseTask(Thread.currentThread(), zkLock, new FixedBackOff(1000L, 3)), leaseTime, timeUnit);
    }

}
