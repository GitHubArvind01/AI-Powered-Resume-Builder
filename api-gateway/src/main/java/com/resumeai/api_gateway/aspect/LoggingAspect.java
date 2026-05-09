package com.resumeai.api_gateway.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Pointcut("execution(* com.resumeai.api_gateway.filter..*(..))")
    public void filterMethods() {}

    @Around("filterMethods()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        long startTime = System.currentTimeMillis();

        log.info("Started execution: {}.{}()", className, methodName);

        try {
            Object result = joinPoint.proceed();

            long endTime = System.currentTimeMillis();
            log.info("Successfully completed: {}.{}() in {} ms",
                    className,
                    methodName,
                    endTime - startTime);

            return result;
        } catch (Exception exception) {
            long endTime = System.currentTimeMillis();
            log.error("Method execution failed: {}.{}() in {} ms. Error: {}",
                    className,
                    methodName,
                    endTime - startTime,
                    exception.getMessage(),
                    exception);

            throw exception; // Re-throw to maintain original application behavior
        }
    }
}