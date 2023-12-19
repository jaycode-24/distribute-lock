package com.sy.pangu.common.lock.reqdeal;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.json.JSONUtil;
import com.sy.pangu.common.lock.Lock;
import com.sy.pangu.common.lock.LockManager;
import com.sy.pangu.common.lock.config.LockProperties;
import com.sy.pangu.common.lock.reqdeal.handler.LockHandler;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author cheng.wang
 * @time 2023/12/7 18:39
 * @des 分布式锁切面
 */
@Slf4j
@Aspect
public class DistributedLockAspect {

    {
        serviceLoader = ServiceLoader.load(LockHandler.class);
    }
    private final ServiceLoader<LockHandler> serviceLoader;

    @Value("${spring.application.name:default}")
    private String sys;

    @Autowired
    private LockManager lockManager;

    @Autowired
    private LockProperties lockProperties;

    @Pointcut("@annotation(com.sy.pangu.common.lock.reqdeal.anno.PostDistributedLock)")
    public void aroundPointCut(){

    }

    @Around("aroundPointCut()")
    public Object around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        //获取方法签名
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        Method method = methodSignature.getMethod();
        String clzName = method.getDeclaringClass().getName();
        String methodName = method.getName();
        //非post请求直接跳过
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        if (Objects.nonNull(postMapping) || (Objects.nonNull(requestMapping) && Arrays.asList(requestMapping.method()).contains(RequestMethod.POST))){
            String lockKey = UUID.randomUUID().toString();
            if (Objects.nonNull(serviceLoader)){
                Iterator<LockHandler> iterator = serviceLoader.iterator();
                while (iterator.hasNext()){
                    LockHandler lockHandler = iterator.next();
                    if (lockHandler.support(proceedingJoinPoint)){
                        try {
                            lockKey = lockHandler.lockKey();
                        } finally {
                            lockHandler.clear();
                        }
                        break;
                    }
                }
            }

            Lock lock = lockManager.createLock(sys, clzName + "#" + methodName, lockKey);
            if (Objects.nonNull(lock)){
                boolean locked = lock.tryLock(-1, 10, TimeUnit.SECONDS);
                if (locked){
                    try {
                        return proceedingJoinPoint.proceed();
                    } finally {
                        if (lock.isLocked()){
                            lock.unLock();
                        }
                    }
                }else {
                    log.info("重复请求：request：" + JSONUtil.toJsonStr(proceedingJoinPoint.getArgs()));
                    if (lockProperties.isThrowError()){
                        throw new RuntimeException("重复请求");
                    }else {
                        Class<?> returnType = method.getReturnType();
                        boolean b = returnType.getName().startsWith("java.lang.Integer");
                        if (b){
                            return 0;
                        }else if (ResponseEntity.class.isAssignableFrom(returnType)){
                            return ResponseEntity.ok("重复请求");
                        }else {
                            Object o = returnType.newInstance();
                            Field code = ReflectUtil.getField(returnType, "code");
                            Field message = ReflectUtil.getField(returnType, "message");
                            Field msg = ReflectUtil.getField(returnType, "msg");
                            if (Objects.nonNull(code)){
                                ReflectUtil.setFieldValue(o, code , 500);
                            }
                            if (Objects.nonNull(message)){
                                ReflectUtil.setFieldValue(o, message, "重复请求");
                            }
                            if (Objects.nonNull(msg)){
                                ReflectUtil.setFieldValue(o, msg, "重复请求");
                            }
                            return o;
                        }
                    }
                }
            }
        }
        return proceedingJoinPoint.proceed();
    }
}

