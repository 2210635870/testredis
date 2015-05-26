package org.oham.testredis.interceptor;

import java.io.Serializable;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;


@Aspect
@Component
public class RedisCacheTxInterceptor {

	@Autowired
	protected RedisTemplate<Serializable, Serializable> redisTemplate;
	
	@Pointcut("execution(* org.oham.testredis.services.impl..*.*(..)) && @annotation(org.oham.testredis.annotation.RedisCacheTx)")
	private void theMethod(){}
	
	@Around("theMethod()")
	public Object openRedisTx(ProceedingJoinPoint pjp) throws Throwable {
		
		Object[] args = pjp.getArgs();
		
		redisTemplate.multi();
		Object obj = pjp.proceed(args);
		redisTemplate.exec();
		
		return obj;
	}
	
	
	@AfterThrowing(pointcut="within(org.oham.testredis.services.impl..*) && @annotation(org.oham.testredis.annotation.RedisCacheTx)", throwing="ex")
	public void rollbackTx(JoinPoint jp, Exception ex) {
		System.out.println("will rollback tx");
		redisTemplate.discard();
	}
}
