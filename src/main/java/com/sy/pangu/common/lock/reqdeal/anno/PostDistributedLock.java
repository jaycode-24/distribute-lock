package com.sy.pangu.common.lock.reqdeal.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author cheng.wang
 * @time 2023/12/7 18:36
 * @des 锁注解：主要用在post请求上防止幂等提交
 * 1.注解中指明业务key（支持SPEL表达式）：
 * redis   ec:com.sy.pangu.ec.after.controller.admin#insert:B000001
 * zk       /lock/ec/com.sy.pangu.ec.after.controller.admin#insert/B000001
 * 2.注解中未指明业务key：
 * redis    ec:com.sy.pangu.ec.after.controller.admin#insert:1555691597
 * zk       ec/com.sy.pangu.ec.after.controller.admin#insert/1555691597
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PostDistributedLock {

    boolean isSpel() default false;

    /**
     * 标识唯一的业务参数
     * 支持SPEL表达式
     */
    String key() default "";
}
