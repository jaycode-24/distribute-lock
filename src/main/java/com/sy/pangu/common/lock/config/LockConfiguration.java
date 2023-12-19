package com.sy.pangu.common.lock.config;

import com.sy.pangu.common.lock.LockManager;
import com.sy.pangu.common.lock.redis.RedisLockManager;
import com.sy.pangu.common.lock.reqdeal.DistributedLockAspect;
import com.sy.pangu.common.lock.zk.ModifyInterProcessMutexRelease;
import com.sy.pangu.common.lock.zk.ZkLockManager;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SentinelServersConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author cheng.wang
 * @time 2023/12/5 22:18
 * @des 配置
 * redis > zk > 本地锁
 */
@Configuration
@Slf4j
@AutoConfigureAfter({RedisAutoConfiguration.class, RedisProperties.class})
@EnableConfigurationProperties(LockProperties.class)
@Data
public class LockConfiguration {

    private final static String SINGLE_REDIS_ADDRESS = "redis://%s:%s";
    private final static String MULTI_REDIS_ADDRESS = "redis://%s";
    private final static String ZK_LOCK_PREFIX = "lock";

    @Configuration
    @ConditionalOnClass(RedisProperties.class)
    @ConditionalOnProperty(value = "lock.lockType", havingValue = "redis", matchIfMissing = true)
    public static class RedisLockConfiguration {
        @Bean
        public RedissonClient redissonClient(RedisProperties redisProperties, LockProperties lockProperties) {
            return Redisson.create(createConfig(redisProperties, lockProperties));
        }

        /**
         * redis锁
         *
         * @param redissonClient redissonClient
         * @return redis锁管理
         */
        @Bean
        @ConditionalOnBean(value = {RedissonClient.class})
        public LockManager redisLockManager(RedissonClient redissonClient) {
            return RedisLockManager.builder()
                    .redissonClient(redissonClient).build();
        }
    }

    @Configuration
    @ConditionalOnClass(CuratorFramework.class)
    @ConditionalOnProperty(value = "lock.lockType", havingValue = "zk")
    public static class ZkLockConfiguration {

        @Bean
        public LockManager zkLockManager(@Autowired LockProperties lockProperties) {
            //可重入锁
            CuratorFramework curatorFramework;
            if (StringUtils.isNotEmpty(lockProperties.getZkConfig().getUser()) && StringUtils.isNotEmpty(lockProperties.getZkConfig().getPassword())){
                curatorFramework = CuratorFrameworkFactory.builder()
                        .authorization(lockProperties.getZkConfig().getUser(), lockProperties.getZkConfig().getPassword().getBytes(StandardCharsets.UTF_8))
                        .connectString(lockProperties.getZkConfig().getAddress())
                        .namespace(ZK_LOCK_PREFIX)
                        //最大重试3 * 3000
                        .retryPolicy(new BoundedExponentialBackoffRetry(1000, 3000, 3))
                        .build();
            }else {
                curatorFramework = CuratorFrameworkFactory.builder()
                        .connectString(lockProperties.getZkConfig().getAddress())
                        .namespace(ZK_LOCK_PREFIX)
                        //最大重试3 * 3000
                        .retryPolicy(new BoundedExponentialBackoffRetry(1000, 3000, 3))
                        .build();
            }
            log.info("启用zk分布式锁，node：" + lockProperties.getZkConfig().getAddress());
            ModifyInterProcessMutexRelease.addReleaseMethod();
            return new ZkLockManager(curatorFramework);
        }

    }

    /**
     * redis配置
     * 从RedisProperties甄别是那种模式
     * 支持单机（主从）、集群、哨兵（这些模式是部署层面的模式）
     * <p>
     * 从redisson配置层面有五种：单机，主从1（主写从读），主从2（主写都读），哨兵，集群
     * 正常情况下，不需要通过redisson来保证故障迁移和主从切换，应该从部署层面，所以这里只会配置三种
     */
    private static Config createConfig(RedisProperties redisProperties, LockProperties lockProperties) {
        Config config = new Config();
        if (StringUtils.isNotEmpty(redisProperties.getHost())) {
            //单机模式
            String singleRedisNode = String.format(SINGLE_REDIS_ADDRESS, redisProperties.getHost(), redisProperties.getPort());
            config.useSingleServer()
                    .setAddress(singleRedisNode)
                    .setDatabase(redisProperties.getDatabase())
                    .setPassword(redisProperties.getPassword())
                    .setConnectionMinimumIdleSize(lockProperties.getRedisConfig().getConnectionMinimumIdleSize())
                    .setConnectionPoolSize(lockProperties.getRedisConfig().getConnectionPoolSize());
            log.info(String.format("启用单机or主从redis分布式锁，node：%s", singleRedisNode));
        } else if (Objects.nonNull(redisProperties.getCluster()) && !CollectionUtils.isEmpty(redisProperties.getCluster().getNodes())) {
            //集群模式
            ClusterServersConfig clusterServersConfig = config.useClusterServers();
            for (String node : redisProperties.getCluster().getNodes()) {
                clusterServersConfig.addNodeAddress(String.format(MULTI_REDIS_ADDRESS, node));
            }
            clusterServersConfig.setPassword(redisProperties.getPassword())
                    .setMasterConnectionPoolSize(lockProperties.getRedisConfig().getConnectionPoolSize())
                    .setMasterConnectionMinimumIdleSize(lockProperties.getRedisConfig().getConnectionMinimumIdleSize())
                    .setSlaveConnectionPoolSize(lockProperties.getRedisConfig().getConnectionPoolSize())
                    .setSlaveConnectionMinimumIdleSize(lockProperties.getRedisConfig().getConnectionMinimumIdleSize());
            log.info(String.format("启用集群redis分布式，node：%s", String.join(",", clusterServersConfig.getNodeAddresses())));
        } else if (Objects.nonNull(redisProperties.getSentinel()) && !CollectionUtils.isEmpty(redisProperties.getSentinel().getNodes())) {
            //哨兵模式
            SentinelServersConfig sentinelServersConfig = config.useSentinelServers();
            for (String node : redisProperties.getSentinel().getNodes()) {
                sentinelServersConfig.addSentinelAddress(String.format(MULTI_REDIS_ADDRESS, node));
            }
            sentinelServersConfig.setMasterName(redisProperties.getSentinel().getMaster())
                    .setDatabase(redisProperties.getDatabase())
                    .setPassword(redisProperties.getPassword())
                    .setMasterName(redisProperties.getSentinel().getMaster())
                    .setSentinelPassword(redisProperties.getSentinel().getPassword())
                    .setMasterConnectionPoolSize(lockProperties.getRedisConfig().getConnectionPoolSize())
                    .setMasterConnectionMinimumIdleSize(lockProperties.getRedisConfig().getConnectionMinimumIdleSize())
                    .setSlaveConnectionPoolSize(lockProperties.getRedisConfig().getConnectionPoolSize())
                    .setSlaveConnectionMinimumIdleSize(lockProperties.getRedisConfig().getConnectionMinimumIdleSize());
            log.info(String.format("启用哨兵redis分布式锁，node：%s", String.join(",", sentinelServersConfig.getSentinelAddresses())));
        }
        return config;
    }

    /**
     * 分布式锁注解切面
     */
    @Bean
    public DistributedLockAspect distributedLockAspect() {
        return new DistributedLockAspect();
    }
}
