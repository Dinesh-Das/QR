package com.cqs.qrmfg.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class NotificationRequest {
    private String id;
    private String type; // EMAIL, SLACK, SMS
    private List<String> recipients;
    private String subject;
    private String message;
    private String templateName;
    private Map<String, Object> templateData;
    private String priority; // LOW, NORMAL, HIGH, URGENT
    private LocalDateTime scheduledAt;
    private String channel; // For Slack notifications
    private String createdBy;
    private LocalDateTime createdAt;
    
    public NotificationRequest() {
        this.createdAt = LocalDateTime.now();
        this.priority = "NORMAL";
    }
    
    public NotificationRequest(String type, List<String> recipients, String subject, String message) {
        this();
        this.type = type;
        this.recipients = recipients;
        this.subject = subject;
        this.message = message;
    }
    
    public NotificationRequest(String type, List<String> recipients, String templateName, Map<String, Object> templateData) {
        this();
        this.type = type;
        this.recipients = recipients;
        this.templateName = templateName;
        this.templateData = templateData;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public List<String> getRecipients() { return recipients; }
    public void setRecipients(List<String> recipients) { this.recipients = recipients; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    
    public Map<String, Object> getTemplateData() { return templateData; }
    public void setTemplateData(Map<String, Object> templateData) { this.templateData = templateData; }
    
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}