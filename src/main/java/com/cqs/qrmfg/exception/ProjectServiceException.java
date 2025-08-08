package com.cqs.qrmfg.exception;

/**
 * Custom exception for ProjectService operations
 */
public class ProjectServiceException extends RuntimeException {

    private final String errorCode;
    private final String operation;

    public ProjectServiceException(String message) {
        super(message);
        this.errorCode = "PROJECT_SERVICE_ERROR";
        this.operation = "UNKNOWN";
    }

    public ProjectServiceException(String message, String errorCode, String operation) {
        super(message);
        this.errorCode = errorCode;
        this.operation = operation;
    }

    public ProjectServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "PROJECT_SERVICE_ERROR";
        this.operation = "UNKNOWN";
    }

    public ProjectServiceException(String message, Throwable cause, String errorCode, String operation) {
        super(message, cause);
        this.errorCode = errorCode;
        this.operation = operation;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getOperation() {
        return operation;
    }
}