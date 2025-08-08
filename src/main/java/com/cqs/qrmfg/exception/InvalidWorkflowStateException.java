package com.cqs.qrmfg.exception;

public class InvalidWorkflowStateException extends WorkflowException {
    public InvalidWorkflowStateException(String message) {
        super(message);
    }
    
    public InvalidWorkflowStateException(String message, Throwable cause) {
        super(message, cause);
    }
}