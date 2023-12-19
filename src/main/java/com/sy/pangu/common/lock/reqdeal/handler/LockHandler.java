package com.sy.pangu.common.lock.reqdeal.handler;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * @author cheng.wang
 * @time 2023/12/11 15:35
 * @des 锁处理
 */
public interface LockHandler {

    boolean support(ProceedingJoinPoint proceedingJoinPoint);

    String lockKey();

    void clear();
}
