package com.cqs.qrmfg.exception;

public class InvalidQueryStateException extends WorkflowException {
    public InvalidQueryStateException(String message) {
        super(message);
    }
    
    public InvalidQueryStateException(String message, Throwable cause) {
        super(message, cause);
    }
}