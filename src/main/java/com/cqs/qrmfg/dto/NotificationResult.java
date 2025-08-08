package com.cqs.qrmfg.dto;

import java.time.LocalDateTime;
import java.util.List;

public class NotificationResult {
    private String id;
    private String status; // SUCCESS, FAILED, PENDING, PARTIAL
    private String message;
    private List<String> successfulRecipients;
    private List<String> failedRecipients;
    private String errorMessage;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private int retryCount;
    private String externalId; // ID from external service (email provider, Slack, etc.)
    
    public NotificationResult() {
        this.createdAt = LocalDateTime.now();
        this.retryCount = 0;
    }
    
    public NotificationResult(String status, String message) {
        this();
        this.status = status;
        this.message = message;
    }
    
    public static NotificationResult success(String message) {
        NotificationResult result = new NotificationResult("SUCCESS", message);
        result.setSentAt(LocalDateTime.now());
        return result;
    }
    
    public static NotificationResult success(String message, List<String> recipients) {
        NotificationResult result = success(message);
        result.setSuccessfulRecipients(recipients);
        return result;
    }
    
    public static NotificationResult failure(String errorMessage) {
        return new NotificationResult("FAILED", errorMessage);
    }
    
    public static NotificationResult failure(String errorMessage, List<String> failedRecipients) {
        NotificationResult result = failure(errorMessage);
        result.setFailedRecipients(failedRecipients);
        return result;
    }
    
    public static NotificationResult partial(List<String> successful, List<String> failed, String message) {
        NotificationResult result = new NotificationResult("PARTIAL", message);
        result.setSuccessfulRecipients(successful);
        result.setFailedRecipients(failed);
        result.setSentAt(LocalDateTime.now());
        return result;
    }
    
    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }
    
    public boolean isFailed() {
        return "FAILED".equals(status);
    }
    
    public boolean isPartial() {
        return "PARTIAL".equals(status);
    }
    
    public boolean isPending() {
        return "PENDING".equals(status);
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public List<String> getSuccessfulRecipients() { return successfulRecipients; }
    public void setSuccessfulRecipients(List<String> successfulRecipients) { this.successfulRecipients = successfulRecipients; }
    
    public List<String> getFailedRecipients() { return failedRecipients; }
    public void setFailedRecipients(List<String> failedRecipients) { this.failedRecipients = failedRecipients; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
}