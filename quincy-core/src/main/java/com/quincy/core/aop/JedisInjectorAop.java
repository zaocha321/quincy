package com.quincy.core.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Aspect
@Component
public class JedisInjectorAop {
	@Autowired
	private JedisPool jedisPool;
	
	@Pointcut("@annotation(com.quincy.core.annotation.JedisInjector)")
    public void pointCut() {}

    @Around("pointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
    	MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
        Class<?>[] clazzes = methodSignature.getParameterTypes();
        Object[] args = joinPoint.getArgs();
    	Jedis jedis = null;
    	try {
        	for(int i=0;i<clazzes.length;i++) {
        		Class<?> clazz = clazzes[i];
        		if(Jedis.class.getName().equals(clazz.getName())) {
        			jedis = jedisPool.getResource();
        			args[i] = jedis;
        			break;
        		}
        	}
            return joinPoint.proceed(args);
    	} finally {
    		if(jedis!=null)
    			jedis.close();
    	}
    }
}
