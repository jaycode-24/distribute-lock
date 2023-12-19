package com.sy.pangu.common.lock.config;

import lombok.Data;
import org.aspectj.weaver.patterns.PointcutRewriter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author cheng.wang
 * @time 2023/12/12 16:01
 * @des 锁配置
 */
@ConfigurationProperties(prefix = "lock")
@RefreshScope
@Data
public class LockProperties {

    /**
     * 锁类型
     */
    private String lockType = "redis";

    private RedisLockConfig redisConfig = new RedisLockConfig();

    private ZkLockConfig zkConfig = new ZkLockConfig();

    private boolean throwError = true;



    @Data
    public static class RedisLockConfig{
        //最小空闲连接
        private int connectionMinimumIdleSize = 24;
        //最大连接
        private int connectionPoolSize = 64;
    }

    @Data
    public static class ZkLockConfig{

        //连接地址 ip:port
        private String address = "";

        //用户名
        private String user = "";

        //密码
        private String password = "";

    }
}
