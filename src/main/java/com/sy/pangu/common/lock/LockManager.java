package com.sy.pangu.common.lock;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author cheng.wang
 * @time 2023/12/5 22:11
 * @des 锁管理
 */
public interface LockManager extends InitializingBean, DisposableBean {

    /**
     * 创建非公平锁
     * @param sys 系统
     * @param biz 业务
     * @param bizKey 业务唯一号
     * @return 锁对象
     */
    Lock createLock(String sys, String biz, String bizKey);

    /**
     * 创建公平锁
     * @param sys 系统
     * @param biz 业务
     * @param bizKey 业务唯一号
     * @return 锁对象
     */
    Lock createFairLock(String sys, String biz, String bizKey);
}
