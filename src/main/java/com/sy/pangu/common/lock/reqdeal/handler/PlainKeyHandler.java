package com.sy.pangu.common.lock.reqdeal.handler;

import com.sy.pangu.common.lock.reqdeal.anno.PostDistributedLock;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * @author cheng.wang
 * @time 2023/12/11 15:48
 * @des 最简单的string字符串的key
 */
public class PlainKeyHandler implements LockHandler{

    private final ThreadLocal<String> lockKey = new ThreadLocal<>();

    @Override
    public boolean support(ProceedingJoinPoint proceedingJoinPoint) {
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        Method method = methodSignature.getMethod();
        PostDistributedLock postDistributedLock = method.getAnnotation(PostDistributedLock.class);
        String key = postDistributedLock.key();
        if (StringUtils.isNotEmpty(key)){
            lockKey.set(postDistributedLock.key());
        }
        return !postDistributedLock.isSpel() && StringUtils.isNotEmpty(key);
    }

    @Override
    public String lockKey() {
        return lockKey.get();
    }

    @Override
    public void clear() {
        lockKey.remove();
    }
}
