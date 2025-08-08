package com.cqs.qrmfg.aspect;

import com.cqs.qrmfg.annotation.RequireRole;
import com.cqs.qrmfg.enums.RoleType;
import com.cqs.qrmfg.exception.InsufficientRoleException;
import com.cqs.qrmfg.service.RBACAuthorizationService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AOP Aspect for processing @RequireRole annotations.
 * This aspect intercepts method calls annotated with @RequireRole and enforces
 * role-based access control before method execution.
 */
@Aspect
@Component
public class RequireRoleAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(RequireRoleAspect.class);
    
    @Autowired
    private RBACAuthorizationService rbacAuthorizationService;
    
    /**
     * Intercept method calls annotated with @RequireRole at the method level
     */
    @Before("@annotation(requireRole)")
    public void checkMethodRoleAccess(JoinPoint joinPoint, RequireRole requireRole) {
        logger.debug("Checking role access for method: {}", joinPoint.getSignature().getName());
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            logger.warn("No authenticated user found for role check on method: {}", joinPoint.getSignature().getName());
            throw new InsufficientRoleException(requireRole.value(), null, requireRole.requireAll(), 
                    "Authentication required to access this resource");
        }
        
        validateRoleAccess(auth, requireRole, joinPoint);
    }
    
    /**
     * Intercept method calls in classes annotated with @RequireRole
     */
    @Before("@within(requireRole) && execution(public * *(..))")
    public void checkClassRoleAccess(JoinPoint joinPoint, RequireRole requireRole) {
        logger.debug("Checking class-level role access for method: {}", joinPoint.getSignature().getName());
        
        // Check if method has its own @RequireRole annotation
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        if (method.isAnnotationPresent(RequireRole.class)) {
            // Method-level annotation takes precedence, skip class-level check
            logger.debug("Method has its own @RequireRole annotation, skipping class-level check");
            return;
        }
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            logger.warn("No authenticated user found for class-level role check on method: {}", joinPoint.getSignature().getName());
            throw new InsufficientRoleException(requireRole.value(), null, requireRole.requireAll(), 
                    "Authentication required to access this resource");
        }
        
        validateRoleAccess(auth, requireRole, joinPoint);
    }
    
    /**
     * Validate role access based on the annotation requirements
     */
    private void validateRoleAccess(Authentication auth, RequireRole requireRole, JoinPoint joinPoint) {
        RoleType[] requiredRoles = requireRole.value();
        boolean requireAll = requireRole.requireAll();
        boolean allowAdminBypass = requireRole.allowAdminBypass();
        String customMessage = requireRole.message();
        
        // Get user's current role
        RoleType userRole = rbacAuthorizationService.getUserPrimaryRoleType(auth);
        List<RoleType> userRoles = rbacAuthorizationService.getUserRoleTypes(auth);
        
        logger.debug("User role: {}, Required roles: {}, Require all: {}", 
                userRole, Arrays.toString(requiredRoles), requireAll);
        
        // Check for admin bypass
        if (allowAdminBypass && rbacAuthorizationService.isUserAdmin(auth)) {
            logger.debug("Admin user bypassing role requirements for method: {}", joinPoint.getSignature().getName());
            logAccessAttempt(auth, joinPoint, true, "Admin bypass");
            return;
        }
        
        // Validate role requirements
        boolean hasAccess = false;
        
        if (requireAll) {
            // User must have ALL required roles
            hasAccess = userRoles.containsAll(Arrays.asList(requiredRoles));
        } else {
            // User must have at least ONE of the required roles
            hasAccess = userRoles.stream().anyMatch(role -> Arrays.asList(requiredRoles).contains(role));
        }
        
        if (!hasAccess) {
            String errorMessage = customMessage.isEmpty() ? 
                    buildDefaultErrorMessage(requiredRoles, userRole, requireAll) : customMessage;
            
            logger.warn("Access denied for user with role {} to method: {}. Required roles: {}", 
                    userRole, joinPoint.getSignature().getName(), Arrays.toString(requiredRoles));
            
            logAccessAttempt(auth, joinPoint, false, "Insufficient role privileges");
            
            throw new InsufficientRoleException(requiredRoles, userRole, requireAll, errorMessage);
        }
        
        logger.debug("Access granted for user with role {} to method: {}", 
                userRole, joinPoint.getSignature().getName());
        
        logAccessAttempt(auth, joinPoint, true, "Role requirements satisfied");
    }
    
    /**
     * Build a default error message for role access denial
     */
    private String buildDefaultErrorMessage(RoleType[] requiredRoles, RoleType userRole, boolean requireAll) {
        if (requiredRoles.length == 1) {
            return String.format("Access denied. Required role: %s", requiredRoles[0].getRoleName());
        } else {
            String conjunction = requireAll ? "all of" : "one of";
            String roleList = Arrays.stream(requiredRoles)
                    .map(RoleType::getRoleName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            return String.format("Access denied. Required %s: [%s]", conjunction, roleList);
        }
    }
    
    /**
     * Log the access attempt for audit purposes
     */
    private void logAccessAttempt(Authentication auth, JoinPoint joinPoint, boolean granted, String reason) {
        try {
            String methodName = joinPoint.getSignature().getName();
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String resource = className + "." + methodName;
            
            Map<String, Object> context = new HashMap<>();
            context.put("method", methodName);
            context.put("class", className);
            context.put("reason", reason);
            context.put("annotationType", "RequireRole");
            
            rbacAuthorizationService.logAccessAttempt(auth, resource, "METHOD_ACCESS", granted, context);
        } catch (Exception e) {
            logger.error("Failed to log access attempt for method: {}", joinPoint.getSignature().getName(), e);
        }
    }
}