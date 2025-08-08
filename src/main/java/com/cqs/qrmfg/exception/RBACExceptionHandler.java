package com.cqs.qrmfg.exception;

import com.cqs.qrmfg.enums.RoleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Global exception handler for RBAC-related exceptions.
 * Provides consistent error responses for role-based access control failures.
 */
@ControllerAdvice
public class RBACExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(RBACExceptionHandler.class);
    
    /**
     * Handle InsufficientRoleException
     */
    @ExceptionHandler(InsufficientRoleException.class)
    public ResponseEntity<RBACErrorResponse> handleInsufficientRoleException(InsufficientRoleException ex) {
        logger.warn("Insufficient role access attempt: {}", ex.getMessage());
        
        String requestPath = getCurrentRequestPath();
        RBACErrorResponse errorResponse = RBACErrorResponse.fromInsufficientRoleException(ex, requestPath);
        
        // Add helpful information for the user
        if (ex.getUserRole() != null) {
            errorResponse.setUserRole(ex.getUserRole().getRoleName());
        }
        
        // Add user-friendly guidance
        errorResponse.setUserMessage(buildUserFriendlyRoleMessage(ex));
        addRoleGuidance(errorResponse, ex);
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    /**
     * Handle PlantAccessDeniedException
     */
    @ExceptionHandler(PlantAccessDeniedException.class)
    public ResponseEntity<RBACErrorResponse> handlePlantAccessDeniedException(PlantAccessDeniedException ex) {
        logger.warn("Plant access denied: {}", ex.getMessage());
        
        String requestPath = getCurrentRequestPath();
        RBACErrorResponse errorResponse = RBACErrorResponse.fromPlantAccessDeniedException(ex, requestPath);
        
        // Add user-friendly guidance for plant access
        errorResponse.setUserMessage(buildUserFriendlyPlantMessage(ex));
        addPlantGuidance(errorResponse, ex);
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    /**
     * Handle general RBACException
     */
    @ExceptionHandler(RBACException.class)
    public ResponseEntity<RBACErrorResponse> handleRBACException(RBACException ex) {
        logger.warn("RBAC access denied: {}", ex.getMessage());
        
        String requestPath = getCurrentRequestPath();
        RBACErrorResponse errorResponse = RBACErrorResponse.fromException(ex, requestPath);
        
        // Add general guidance for RBAC errors
        addGeneralRBACGuidance(errorResponse, ex);
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    /**
     * Get the current request path for error context
     */
    private String getCurrentRequestPath() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getRequestURI();
            }
        } catch (Exception e) {
            logger.debug("Could not determine request path", e);
        }
        return "unknown";
    }
    
    /**
     * Build user-friendly message for role-based access denial
     */
    private String buildUserFriendlyRoleMessage(InsufficientRoleException ex) {
        if (ex.getRequiredRoles().length == 1) {
            return String.format("You need %s role to access this feature. Your current role is %s.", 
                    ex.getRequiredRoles()[0].getRoleName(), 
                    ex.getUserRole() != null ? ex.getUserRole().getRoleName() : "not assigned");
        } else {
            String conjunction = ex.isRequireAll() ? "all of these roles" : "one of these roles";
            String roleList = java.util.Arrays.stream(ex.getRequiredRoles())
                    .map(role -> role.getRoleName())
                    .collect(java.util.stream.Collectors.joining(", "));
            return String.format("You need %s: [%s] to access this feature. Your current role is %s.", 
                    conjunction, roleList, 
                    ex.getUserRole() != null ? ex.getUserRole().getRoleName() : "not assigned");
        }
    }
    
    /**
     * Build user-friendly message for plant access denial
     */
    private String buildUserFriendlyPlantMessage(PlantAccessDeniedException ex) {
        if (ex.getRequestedPlantCode() != null) {
            if (ex.getUserAssignedPlants() != null && !ex.getUserAssignedPlants().isEmpty()) {
                return String.format("You don't have access to plant '%s'. You can access data for: %s", 
                        ex.getRequestedPlantCode(), 
                        String.join(", ", ex.getUserAssignedPlants()));
            } else {
                return String.format("You don't have access to plant '%s'. No plants are currently assigned to your account.", 
                        ex.getRequestedPlantCode());
            }
        } else {
            return "Plant access filtering failed. Please contact your administrator.";
        }
    }
    
    /**
     * Add helpful guidance for role-based access issues
     */
    private void addRoleGuidance(RBACErrorResponse errorResponse, InsufficientRoleException ex) {
        java.util.List<String> guidance = new java.util.ArrayList<>();
        
        if (ex.getUserRole() == null) {
            guidance.add("Contact your administrator to assign you a role");
        } else {
            guidance.add("Contact your administrator to request elevated access");
            guidance.add("Verify you are logged in with the correct account");
        }
        
        // Add role-specific guidance
        if (ex.getRequiredRoles().length == 1) {
            switch (ex.getRequiredRoles()[0]) {
                case ADMIN:
                    guidance.add("Administrator access is required for system management functions");
                    break;
                case JVC_ROLE:
                    guidance.add("JVC role is required for JVC-specific operations");
                    break;
                case CQS_ROLE:
                    guidance.add("CQS role is required for quality system operations");
                    break;
                case TECH_ROLE:
                    guidance.add("Technical role is required for system configuration");
                    break;
                case PLANT_ROLE:
                    guidance.add("Plant role is required for plant-specific operations");
                    break;
            }
        }
        
        errorResponse.setAllowedActions(guidance);
    }
    
    /**
     * Add helpful guidance for plant access issues
     */
    private void addPlantGuidance(RBACErrorResponse errorResponse, PlantAccessDeniedException ex) {
        java.util.List<String> guidance = new java.util.ArrayList<>();
        
        if (ex.getUserAssignedPlants() != null && !ex.getUserAssignedPlants().isEmpty()) {
            guidance.add("You can access data for these plants: " + String.join(", ", ex.getUserAssignedPlants()));
            guidance.add("Contact your administrator to request access to additional plants");
        } else {
            guidance.add("No plants are currently assigned to your account");
            guidance.add("Contact your administrator to assign plant access");
        }
        
        guidance.add("Verify you have the correct PLANT_ROLE assigned");
        
        errorResponse.setAllowedActions(guidance);
    }
    
    /**
     * Add general guidance for RBAC errors
     */
    private void addGeneralRBACGuidance(RBACErrorResponse errorResponse, RBACException ex) {
        java.util.List<String> guidance = new java.util.ArrayList<>();
        
        guidance.add("Contact your administrator for access assistance");
        guidance.add("Verify you are logged in with the correct account");
        guidance.add("Check if your session has expired and try logging in again");
        
        errorResponse.setAllowedActions(guidance);
    }
}