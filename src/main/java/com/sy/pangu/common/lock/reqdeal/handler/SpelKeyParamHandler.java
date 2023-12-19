package com.sy.pangu.common.lock.reqdeal.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.sy.pangu.common.lock.reqdeal.anno.PostDistributedLock;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author cheng.wang
 * @time 2023/12/11 15:50
 * @des 针对url-params的参数
 */
public class SpelKeyParamHandler implements LockHandler{

    private final ThreadLocal<Map<String, Object>> paramThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<String> keyThreadlocal = new ThreadLocal<>();
    @Override
    public boolean support(ProceedingJoinPoint proceedingJoinPoint) {
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        Method method = methodSignature.getMethod();
        Object[] args = proceedingJoinPoint.getArgs();
        Parameter[] parameters = method.getParameters();
        PostDistributedLock postDistributedLock = method.getAnnotation(PostDistributedLock.class);

        Map<String, Object> param = new HashMap<>();
        for (int i = 0; i < parameters.length; i++) {
            RequestParam requestParam = parameters[i].getAnnotation(RequestParam.class);
            if (Objects.nonNull(requestParam)){
                String name = StringUtils.isNotEmpty(requestParam.value()) ? requestParam.value() : requestParam.name();
                if (StringUtils.isNotEmpty(name)){
                    param.put(name, args[i]);
                }
            }
        }
        keyThreadlocal.set(postDistributedLock.key());
        paramThreadLocal.set(param);
        return postDistributedLock.isSpel() && StringUtils.isNotEmpty(postDistributedLock.key()) && CollUtil.isNotEmpty(param);
    }

    @Override
    public String lockKey() {
        SpelExpressionParser spelExpressionParser = new SpelExpressionParser();
        Expression expression = spelExpressionParser.parseExpression(keyThreadlocal.get());
        StandardEvaluationContext standardEvaluationContext = new StandardEvaluationContext();
        for (Map.Entry<String, Object> entry : paramThreadLocal.get().entrySet()) {
            standardEvaluationContext.setVariable(entry.getKey(), entry.getValue());
        }
        return expression.getValue(standardEvaluationContext, String.class);
    }

    @Override
    public void clear() {
        paramThreadLocal.remove();
        keyThreadlocal.remove();
    }
}
