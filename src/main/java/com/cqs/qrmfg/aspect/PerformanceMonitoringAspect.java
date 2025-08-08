package com.cqs.qrmfg.aspect;

import com.cqs.qrmfg.service.MetricsService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Aspect for monitoring performance of critical methods
 */
@Aspect
@Component
public class PerformanceMonitoringAspect {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringAspect.class);

    @Autowired
    private MetricsService metricsService;

    /**
     * Monitor database repository method execution times
     */
    @Around("execution(* com.cqs.qrmfg.repository..*(..))")
    public Object monitorRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record database query time
            metricsService.recordDatabaseQueryTime(executionTime, className + "." + methodName);
            
            if (executionTime > 1000) { // Log slow queries (> 1 second)
                logger.warn("Slow database query detected: {}.{} took {}ms", 
                           className, methodName, executionTime);
            }
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Database query failed: {}.{} took {}ms, error: {}", 
                        className, methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    /**
     * Monitor workflow service method execution times
     */
    @Around("execution(* com.cqs.qrmfg.service.impl.WorkflowServiceImpl.*(..))")
    public Object monitorWorkflowService(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record workflow processing time for state transition methods
            if (methodName.contains("transition") || methodName.contains("extend") || methodName.contains("complete")) {
                // We'll extract states from the method parameters or result if needed
                metricsService.recordWorkflowProcessingTime(executionTime, null, null);
            }
            
            if (executionTime > 2000) { // Log slow workflow operations (> 2 seconds)
                logger.warn("Slow workflow operation detected: {} took {}ms", methodName, executionTime);
            }
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Workflow operation failed: {} took {}ms, error: {}", 
                        methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    /**
     * Monitor query service method execution times
     */
    @Around("execution(* com.cqs.qrmfg.service.impl.QueryServiceImpl.*(..))")
    public Object monitorQueryService(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record query resolution time for resolve methods
            if (methodName.contains("resolve")) {
                metricsService.recordQueryResolutionTime(executionTime, null);
            }
            
            if (executionTime > 1500) { // Log slow query operations (> 1.5 seconds)
                logger.warn("Slow query operation detected: {} took {}ms", methodName, executionTime);
            }
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Query operation failed: {} took {}ms, error: {}", 
                        methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    /**
     * Monitor notification service method execution times
     */
    @Around("execution(* com.cqs.qrmfg.service.impl.NotificationServiceImpl.*(..))")
    public Object monitorNotificationService(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record notification processing time
            metricsService.recordNotificationProcessingTime(executionTime, methodName);
            
            if (executionTime > 5000) { // Log slow notifications (> 5 seconds)
                logger.warn("Slow notification processing detected: {} took {}ms", methodName, executionTime);
            }
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Notification processing failed: {} took {}ms, error: {}", 
                        methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    /**
     * Monitor REST controller method execution times
     */
    @Around("execution(* com.cqs.qrmfg.controller..*(..))")
    public Object monitorControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (executionTime > 3000) { // Log slow API calls (> 3 seconds)
                logger.warn("Slow API call detected: {}.{} took {}ms", 
                           className, methodName, executionTime);
            }
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("API call failed: {}.{} took {}ms, error: {}", 
                        className, methodName, executionTime, e.getMessage());
            throw e;
        }
    }
}