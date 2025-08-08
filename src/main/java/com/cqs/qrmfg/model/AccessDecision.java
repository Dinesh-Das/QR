package com.cqs.qrmfg.model;

import java.util.Map;

/**
 * Represents an access control decision with details
 */
public class AccessDecision {
    private boolean granted;
    private String message;
    private Map<String, Object> details;
    
    public AccessDecision(boolean granted, String message) {
        this.granted = granted;
        this.message = message;
    }
    
    public AccessDecision(boolean granted, String message, Map<String, Object> details) {
        this.granted = granted;
        this.message = message;
        this.details = details;
    }
    
    public boolean isGranted() {
        return granted;
    }
    
    public void setGranted(boolean granted) {
        this.granted = granted;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Map<String, Object> getDetails() {
        return details;
    }
    
    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}