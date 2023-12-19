package com.sy.pangu.common.lock.reqdeal.handler;

import cn.hutool.core.util.StrUtil;
import com.sy.pangu.common.lock.reqdeal.anno.PostDistributedLock;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;

/**
 * @author cheng.wang
 * @time 2023/12/11 15:47
 * @des 针对是spel表达式的:application-json
 */
public class SpelKeyJsonHandler implements LockHandler{

    private final ThreadLocal<String> key = new ThreadLocal<>();

    private final ThreadLocal<Object> requestBodyValue = new ThreadLocal<>();
    @Override
    public boolean support(ProceedingJoinPoint proceedingJoinPoint) {
        //获取方法签名
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        Method method = methodSignature.getMethod();
        PostDistributedLock postDistributedLock = method.getAnnotation(PostDistributedLock.class);
        Object[] args = proceedingJoinPoint.getArgs();
        Parameter[] parameters = method.getParameters();
        boolean support = false;
        for (int i = 0; i < parameters.length; i++) {
            RequestBody requestBody = parameters[i].getAnnotation(RequestBody.class);
            if (Objects.nonNull(requestBody)){
                support = postDistributedLock.isSpel() && StringUtils.isNotEmpty(postDistributedLock.key());
                if (support){
                    key.set(postDistributedLock.key());
                    requestBodyValue.set(args[i]);
                }
            }
        }
        return support;
    }

    @Override
    public String lockKey() {
        SpelExpressionParser spelExpressionParser = new SpelExpressionParser();
        Expression expression = spelExpressionParser.parseExpression(key.get());
        StandardEvaluationContext standardEvaluationContext = new StandardEvaluationContext();
        //获取别名
        String expressionString = expression.getExpressionString();
        String alias = StrUtil.subBetween(expressionString, "#", ".");
        standardEvaluationContext.setVariable(alias, requestBodyValue.get());
        return expression.getValue(standardEvaluationContext, String.class);
    }

    @Override
    public void clear() {
        key.remove();
        requestBodyValue.remove();
    }
}
