package com.sy.pangu.common.lock.reqdeal.handler;

import org.aspectj.lang.ProceedingJoinPoint;

import java.util.UUID;

/**
 * @author cheng.wang
 * @time 2023/12/11 16:03
 * @des 默认，保证不要陷入死循环
 */
public class DefaultHandler implements LockHandler{
    @Override
    public boolean support(ProceedingJoinPoint proceedingJoinPoint) {
        return true;
    }

    @Override
    public String lockKey() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void clear() {

    }
}
