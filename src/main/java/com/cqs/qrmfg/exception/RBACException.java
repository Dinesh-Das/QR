package com.cqs.qrmfg.exception;

/**
 * Base exception class for all RBAC (Role-Based Access Control) related exceptions.
 * This provides a common structure for RBAC errors with error codes and user-friendly messages.
 */
public class RBACException extends RuntimeException {
    
    private final String errorCode;
    private final String userMessage;
    
    /**
     * Constructor with error code and message
     * 
     * @param errorCode machine-readable error code
     * @param message human-readable error message
     */
    public RBACException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.userMessage = message;
    }
    
    /**
     * Constructor with error code, message, and cause
     * 
     * @param errorCode machine-readable error code
     * @param message human-readable error message
     * @param cause the underlying cause of the exception
     */
    public RBACException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userMessage = message;
    }
    
    /**
     * Constructor with error code, user message, and system message
     * 
     * @param errorCode machine-readable error code
     * @param userMessage user-friendly message for display
     * @param systemMessage detailed system message for logging
     * @param cause the underlying cause of the exception
     */
    public RBACException(String errorCode, String userMessage, String systemMessage, Throwable cause) {
        super(systemMessage, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }
    
    /**
     * Get the machine-readable error code
     * 
     * @return error code string
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Get the user-friendly error message
     * 
     * @return user message string
     */
    public String getUserMessage() {
        return userMessage;
    }
    
    /**
     * Check if this exception has a specific error code
     * 
     * @param code the error code to check
     * @return true if the error codes match
     */
    public boolean hasErrorCode(String code) {
        return errorCode != null && errorCode.equals(code);
    }
}