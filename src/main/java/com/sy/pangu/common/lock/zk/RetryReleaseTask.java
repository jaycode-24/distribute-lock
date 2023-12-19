package com.sy.pangu.common.lock.zk;

import cn.hutool.core.util.ReflectUtil;
import com.sy.pangu.common.lock.Lock;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.backoff.FixedBackOff;

/**
 * @author cheng.wang
 * @time 2023/12/13 17:20
 * @des 锁释放-加入重试
 */
@AllArgsConstructor
@Slf4j
public class RetryReleaseTask implements Runnable{

    private Thread currentThread;

    private InterProcessMutex zkLock;

    //重试策略
    private FixedBackOff backOff;


    @Override
    public void run() {
        //重试
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy((int) backOff.getMaxAttempts()));
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(backOff.getInterval());
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
        try {
            retryTemplate.execute(new RetryCallback<Void, Exception>() {
                @Override
                public Void doWithRetry(RetryContext retryContext) throws Exception {
                    if (zkLock.isAcquiredInThisProcess()){
                        ReflectUtil.invoke(zkLock, "release", currentThread);
                        return null;
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            //throw new RuntimeException(e);
            log.error(e.getMessage(), e);
        }
    }
}
