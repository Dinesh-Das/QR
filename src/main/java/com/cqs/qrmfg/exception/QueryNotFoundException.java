package com.cqs.qrmfg.exception;

public class QueryNotFoundException extends WorkflowException {
    public QueryNotFoundException(Long queryId) {
        super("Query not found with ID: " + queryId);
    }
    
    public QueryNotFoundException(String message) {
        super(message);
    }
    
    public QueryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}