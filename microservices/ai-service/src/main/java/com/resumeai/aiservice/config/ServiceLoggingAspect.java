package com.resumeai.aiservice.config;

import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class ServiceLoggingAspect {

	@Around("execution(* com.resumeai.aiservice.service..*(..))")
	public Object logAroundServiceCall(ProceedingJoinPoint joinPoint) throws Throwable {
		long start = System.currentTimeMillis();
		try {
			Object result = joinPoint.proceed();
			log.debug("AI service call {} completed in {} ms", joinPoint.getSignature().toShortString(),
					System.currentTimeMillis() - start);
			return result;
		} catch (Throwable throwable) {
			log.error("AI service call {} failed after {} ms with args {}", joinPoint.getSignature().toShortString(),
					System.currentTimeMillis() - start, Arrays.toString(joinPoint.getArgs()));
			throw throwable;
		}
	}
}
