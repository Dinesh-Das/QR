package com.cqs.qrmfg.exception;

import com.cqs.qrmfg.enums.RoleType;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Exception thrown when a user does not have sufficient role privileges
 * to access a particular resource or perform an action.
 */
public class InsufficientRoleException extends RBACException {
    
    private final RoleType[] requiredRoles;
    private final RoleType userRole;
    private final boolean requireAll;
    
    /**
     * Constructor for single required role
     * 
     * @param requiredRole the role required for access
     * @param userRole the user's current role
     */
    public InsufficientRoleException(RoleType requiredRole, RoleType userRole) {
        this(new RoleType[]{requiredRole}, userRole, false);
    }
    
    /**
     * Constructor for multiple required roles
     * 
     * @param requiredRoles array of roles required for access
     * @param userRole the user's current role
     * @param requireAll whether all roles are required (true) or any one is sufficient (false)
     */
    public InsufficientRoleException(RoleType[] requiredRoles, RoleType userRole, boolean requireAll) {
        super("INSUFFICIENT_ROLE", buildErrorMessage(requiredRoles, userRole, requireAll));
        this.requiredRoles = requiredRoles;
        this.userRole = userRole;
        this.requireAll = requireAll;
    }
    
    /**
     * Constructor with custom message
     * 
     * @param requiredRoles array of roles required for access
     * @param userRole the user's current role
     * @param requireAll whether all roles are required
     * @param customMessage custom error message
     */
    public InsufficientRoleException(RoleType[] requiredRoles, RoleType userRole, boolean requireAll, String customMessage) {
        super("INSUFFICIENT_ROLE", customMessage);
        this.requiredRoles = requiredRoles;
        this.userRole = userRole;
        this.requireAll = requireAll;
    }
    
    /**
     * Build a descriptive error message based on the role requirements
     */
    private static String buildErrorMessage(RoleType[] requiredRoles, RoleType userRole, boolean requireAll) {
        if (requiredRoles == null || requiredRoles.length == 0) {
            return "Access denied. No valid roles specified.";
        }
        
        String roleList = Arrays.stream(requiredRoles)
                .map(RoleType::getRoleName)
                .collect(Collectors.joining(", "));
        
        String userRoleName = userRole != null ? userRole.getRoleName() : "NONE";
        
        if (requiredRoles.length == 1) {
            return String.format("Access denied. Required role: %s, User role: %s", 
                    requiredRoles[0].getRoleName(), userRoleName);
        } else {
            String conjunction = requireAll ? "all of" : "one of";
            return String.format("Access denied. Required %s: [%s], User role: %s", 
                    conjunction, roleList, userRoleName);
        }
    }
    
    /**
     * Get the required roles for this access attempt
     */
    public RoleType[] getRequiredRoles() {
        return requiredRoles;
    }
    
    /**
     * Get the user's current role
     */
    public RoleType getUserRole() {
        return userRole;
    }
    
    /**
     * Check if all roles are required or if any one is sufficient
     */
    public boolean isRequireAll() {
        return requireAll;
    }
}