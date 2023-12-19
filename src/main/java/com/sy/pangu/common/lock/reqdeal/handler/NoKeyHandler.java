package com.sy.pangu.common.lock.reqdeal.handler;

import com.sy.pangu.common.lock.reqdeal.anno.PostDistributedLock;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;

/**
 * @author cheng.wang
 * @time 2023/12/11 15:42
 * @des 没有配置key的处理：针对post:application-json请求，直接取对象hash
 */
public class NoKeyHandler implements LockHandler{

    private final ThreadLocal<Object> requestBodyValue = new ThreadLocal<>();

    @Override
    public boolean support(ProceedingJoinPoint proceedingJoinPoint) {
        //获取方法签名
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        Method method = methodSignature.getMethod();
        PostDistributedLock postDistributedLock = method.getAnnotation(PostDistributedLock.class);
        Object[] args = proceedingJoinPoint.getArgs();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            RequestBody requestBody = parameters[i].getAnnotation(RequestBody.class);
            if (Objects.nonNull(requestBody)){
                requestBodyValue.set(args[i]);
                break;
            }
        }
        return Objects.nonNull(requestBodyValue.get()) && !postDistributedLock.isSpel() && StringUtils.isEmpty(postDistributedLock.key());
    }

    @Override
    public String lockKey() {
        return String.valueOf(requestBodyValue.get().hashCode());
    }

    @Override
    public void clear() {
        requestBodyValue.remove();
    }
}
