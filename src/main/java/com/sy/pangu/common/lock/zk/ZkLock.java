package com.sy.pangu.common.lock.zk;

import com.sy.pangu.common.lock.Lock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.springframework.util.backoff.FixedBackOff;

import java.util.concurrent.*;

/**
 * @author cheng.wang
 * @time 2023/12/6 9:40
 * @des 基于zk实现的锁
 */
@Data
@Slf4j
@Builder
public class ZkLock implements Lock {

    private InterProcessMutex zkLock;


    @Override
    public void lock() {
        while (true){
            try {
                if (zkLock.acquire(-1, null)){
                    break;
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()){
                    log.debug(e.getMessage(), e);
                }
                //throw new RuntimeException(e);
            }

        }
    }

    @Override
    public boolean tryLock() {
        try {
            return zkLock.acquire(-1, null);
        } catch (Exception e) {
            //throw new RuntimeException(e);
            if (log.isDebugEnabled()){
                log.debug(e.getMessage(), e);
            }
            return false;
        }
    }

    @Override
    public boolean tryLock(long waitTime, TimeUnit timeUnit) throws InterruptedException {
        try {
            return zkLock.acquire(waitTime, timeUnit);
        } catch (Exception e) {
            //throw new RuntimeException(e);
            if (log.isDebugEnabled()){
                log.debug(e.getMessage(), e);
            }
            return false;
        }
    }

    @Override
    public boolean tryLock(long waitTime, long leaseTime, TimeUnit timeUnit) throws InterruptedException {
        try {
            boolean success = zkLock.acquire(waitTime, timeUnit);
            if (success){
                //如果成功，创建延时任务释放锁，避免引入别的组件，但基于内存的如果服务挂了会有死锁问题
                ScheduledExecutorUtil.schedule(zkLock, leaseTime, timeUnit);
            }
            return success;
        } catch (Exception e) {
            //throw new RuntimeException(e);
            if (log.isDebugEnabled()){
                log.debug(e.getMessage(), e);
            }
            return false;
        }
    }

    @Override
    public void unLock() {
        try {
            zkLock.release();
        } catch (Exception e) {
            //throw new RuntimeException(e);
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public boolean isLocked() {
        return zkLock.isAcquiredInThisProcess();
    }
}
