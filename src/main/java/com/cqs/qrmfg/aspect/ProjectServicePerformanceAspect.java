package com.cqs.qrmfg.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Performance monitoring aspect for ProjectService operations
 */
@Aspect
@Component
public class ProjectServicePerformanceAspect {

    private static final Logger logger = LoggerFactory.getLogger(ProjectServicePerformanceAspect.class);

    @Around("execution(* com.cqs.qrmfg.service.impl.ProjectServiceImpl.*(..))")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (executionTime > 1000) { // Log slow operations (> 1 second)
                logger.warn("Slow ProjectService operation detected: {} took {}ms", methodName, executionTime);
            } else {
                logger.debug("ProjectService operation: {} completed in {}ms", methodName, executionTime);
            }
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("ProjectService operation: {} failed after {}ms with error: {}", 
                        methodName, executionTime, e.getMessage());
            throw e;
        }
    }
}