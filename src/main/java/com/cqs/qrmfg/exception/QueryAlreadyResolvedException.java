package com.cqs.qrmfg.exception;

public class QueryAlreadyResolvedException extends QueryException {
    public QueryAlreadyResolvedException(Long queryId) {
        super("Query with ID " + queryId + " is already resolved");
    }
    
    public QueryAlreadyResolvedException(String message) {
        super(message);
    }
    
    public QueryAlreadyResolvedException(String message, Throwable cause) {
        super(message, cause);
    }
}