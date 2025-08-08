package com.cqs.qrmfg.annotation;

import com.cqs.qrmfg.enums.RoleType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for method-level role-based security.
 * This annotation can be applied to methods to enforce role-based access control.
 * 
 * Usage examples:
 * - @RequireRole(RoleType.ADMIN) - Requires ADMIN role
 * - @RequireRole({RoleType.ADMIN, RoleType.TECH_ROLE}) - Requires either ADMIN or TECH_ROLE
 * - @RequireRole(value = {RoleType.ADMIN, RoleType.JVC_ROLE}, requireAll = true) - Requires both roles
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    
    /**
     * The required role(s) for accessing the annotated method or class.
     * If multiple roles are specified, by default any one of them is sufficient.
     * 
     * @return array of required role types
     */
    RoleType[] value();
    
    /**
     * When multiple roles are specified, determines if all roles are required (true)
     * or if any one role is sufficient (false, default).
     * 
     * @return true if all specified roles are required, false if any one is sufficient
     */
    boolean requireAll() default false;
    
    /**
     * Custom error message to display when access is denied.
     * If not specified, a default message will be generated.
     * 
     * @return custom error message
     */
    String message() default "";
    
    /**
     * Whether to allow access for ADMIN role regardless of other role requirements.
     * This provides a way to give ADMIN users universal access while still
     * enforcing specific role requirements for other users.
     * 
     * @return true if ADMIN role should bypass other requirements (default), false otherwise
     */
    boolean allowAdminBypass() default true;
}