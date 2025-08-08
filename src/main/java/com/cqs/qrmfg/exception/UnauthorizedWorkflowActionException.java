package com.cqs.qrmfg.exception;

public class UnauthorizedWorkflowActionException extends WorkflowException {
    public UnauthorizedWorkflowActionException(String message) {
        super(message);
    }
    
    public UnauthorizedWorkflowActionException(String message, Throwable cause) {
        super(message, cause);
    }
}