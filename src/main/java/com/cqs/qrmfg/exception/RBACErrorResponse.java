package com.cqs.qrmfg.exception;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Structured error response for RBAC-related exceptions.
 * This class provides a consistent format for returning RBAC error information to clients.
 */
public class RBACErrorResponse {
    
    private String errorCode;
    private String message;
    private String userMessage;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String requestPath;
    private List<String> allowedActions;
    private String userId;
    private String userRole;
    
    /**
     * Default constructor
     */
    public RBACErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Constructor with basic error information
     * 
     * @param errorCode machine-readable error code
     * @param message error message
     */
    public RBACErrorResponse(String errorCode, String message) {
        this();
        this.errorCode = errorCode;
        this.message = message;
        this.userMessage = message;
    }
    
    /**
     * Constructor with error code, message, and request path
     * 
     * @param errorCode machine-readable error code
     * @param message error message
     * @param requestPath the request path that caused the error
     */
    public RBACErrorResponse(String errorCode, String message, String requestPath) {
        this(errorCode, message);
        this.requestPath = requestPath;
    }
    
    /**
     * Static factory method to create error response from RBACException
     * 
     * @param exception the RBAC exception
     * @param requestPath the request path that caused the error
     * @return configured error response
     */
    public static RBACErrorResponse fromException(RBACException exception, String requestPath) {
        RBACErrorResponse response = new RBACErrorResponse();
        response.setErrorCode(exception.getErrorCode());
        response.setMessage(exception.getMessage());
        response.setUserMessage(exception.getUserMessage());
        response.setRequestPath(requestPath);
        return response;
    }
    
    /**
     * Static factory method to create error response from InsufficientRoleException
     * 
     * @param exception the insufficient role exception
     * @param requestPath the request path that caused the error
     * @return configured error response with role-specific information
     */
    public static RBACErrorResponse fromInsufficientRoleException(InsufficientRoleException exception, String requestPath) {
        RBACErrorResponse response = fromException(exception, requestPath);
        
        if (exception.getUserRole() != null) {
            response.setUserRole(exception.getUserRole().getRoleName());
        }
        
        return response;
    }
    
    /**
     * Static factory method to create error response from PlantAccessDeniedException
     * 
     * @param exception the plant access denied exception
     * @param requestPath the request path that caused the error
     * @return configured error response with plant-specific information
     */
    public static RBACErrorResponse fromPlantAccessDeniedException(PlantAccessDeniedException exception, String requestPath) {
        RBACErrorResponse response = fromException(exception, requestPath);
        
        // Add allowed actions for plant access errors
        if (exception.getUserAssignedPlants() != null && !exception.getUserAssignedPlants().isEmpty()) {
            response.setAllowedActions(Arrays.asList("Access data for assigned plants: " + 
                    String.join(", ", exception.getUserAssignedPlants())));
        }
        
        return response;
    }
    
    // Getters and setters
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getUserMessage() {
        return userMessage;
    }
    
    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getRequestPath() {
        return requestPath;
    }
    
    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }
    
    public List<String> getAllowedActions() {
        return allowedActions;
    }
    
    public void setAllowedActions(List<String> allowedActions) {
        this.allowedActions = allowedActions;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUserRole() {
        return userRole;
    }
    
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }
}