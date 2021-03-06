package com.quincy.core.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import com.quincy.core.annotation.Synchronized;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

@Aspect
@Component
public class SynchronizedAop extends JedisNeededBaseAop {
	private static String SET_KEY = "SYNCHRONIZED";

	@Pointcut("@annotation(com.quincy.core.annotation.Synchronized)")
    public void pointCut() {}

	@Override
	protected Object handle(JoinPoint joinPoint, Jedis jedis) throws NoSuchMethodException, SecurityException, InterruptedException {
		Synchronized annotation = AopHelper.getAnnotation(joinPoint, Synchronized.class);
		String key = annotation.value();
		String channels = SET_KEY+"_"+key;
		while(true) {
			if(jedis.sismember(SET_KEY, key)||jedis.sadd(SET_KEY, key)==0) {
				jedis.subscribe(new MyListener(), channels);
			} else {//Successfully held the distributed lock.
				break;
			}
		}
		return key;
	}

	@Override
	protected void destroy(JoinPoint joinPoint, Jedis jedis, Object obj) {
		String key = obj.toString();
		jedis.srem(SET_KEY, key);
		jedis.publish(SET_KEY+"_"+key, "Finished");
	}

	public class MyListener extends JedisPubSub {
		public void onMessage(String channel, String message) {
			this.unsubscribe();
		}
	}

}
