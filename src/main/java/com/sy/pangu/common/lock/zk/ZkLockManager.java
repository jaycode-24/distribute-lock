package com.sy.pangu.common.lock.zk;

import cn.hutool.core.util.ReflectUtil;
import com.sy.pangu.common.lock.Lock;
import com.sy.pangu.common.lock.LockManager;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;

import java.lang.reflect.Constructor;

/**
 * @author cheng.wang
 * @time 2023/12/6 9:39
 * @des zk锁管理
 */
@Data
@AllArgsConstructor
@Slf4j
public class ZkLockManager implements LockManager {

    private CuratorFramework zkClient;


    @Override
    public Lock createLock(String sys, String biz, String bizKey) {
        //因zk非公平锁都监听同一个节点，会造成惊鸿效应导致性能下降，所以这里直接返回公平锁
        return this.createFairLock(sys, biz, bizKey);
    }

    @Override
    public Lock createFairLock(String sys, String biz, String bizKey) {
        //InterProcessMutex interProcessMutex = new InterProcessMutex(zkClient, String.format("/%s/%s/%s", sys, biz, bizKey));
        InterProcessMutex interProcessMutex = null;
        try {
            //CtClass ctClass = ClassPool.getDefault().get("org.apache.curator.framework.recipes.locks.InterProcessMutex");
            //ClassLoader loader = new CustomClassLoader();
            //Constructor<?> constructor = ctClass.toClass().getConstructor(CuratorFramework.class, String.class);
            //interProcessMutex = (InterProcessMutex) constructor.newInstance(zkClient, String.format("/%s/%s/%s", sys, biz, bizKey));
            interProcessMutex = (InterProcessMutex) ModifyInterProcessMutexRelease.getInstance(zkClient, String.format("/%s/%s/%s", sys, biz, bizKey));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ZkLock.builder().zkLock(interProcessMutex).build();
    }

    @Override
    public void destroy() throws Exception {
        zkClient.close();
        ModifyInterProcessMutexRelease.delete();
        log.info("ZkLock shutdown ...");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        zkClient.start();
        log.info("ZkLock start ...");
    }
}
