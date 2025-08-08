package com.cqs.qrmfg.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "QRMFG_AUDIT")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_seq")
    @SequenceGenerator(name = "audit_seq", sequenceName = "QRMFG_AUDIT_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String entityType;

    @Column
    private String entityId;

    @Column(length = 1000)
    private String description;

    @Column(length = 50)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Column(length = 100)
    private String sessionId;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTime;

    @Column(length = 20)
    private String severity = "INFO";

    @Column(length = 20)
    private String category = "SECURITY";

    @Column(length = 20)
    private String eventType;

    @Column(length = 20)
    private String eventCategory;

    @Column(length = 1000)
    private String details;

    @Column(name = "resource_path", length = 100)
    private String resource;

    @Column(length = 20)
    private String result = "SUCCESS";

    @Column(length = 1000)
    private String errorMessage;

    public AuditLog() {}

    public AuditLog(User user, String action, String entityType) {
        this.user = user;
        this.action = action;
        this.entityType = entityType;
    }
    public AuditLog(User user, String action, String entityType, String entityId) {
        this.user = user;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
    }
    public AuditLog(User user, String action, String entityType, String entityId, String description) {
        this.user = user;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        eventTime = LocalDateTime.now();
    }

    // Getters and setters for all fields
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getEventType() { return eventType != null ? eventType : action; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getEventCategory() { return eventCategory != null ? eventCategory : category; }
    public void setEventCategory(String eventCategory) { this.eventCategory = eventCategory; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    // Helper methods
    public boolean isSecurityEvent() {
        return "SECURITY".equals(category) || 
               "LOGIN".equals(action) || 
               "LOGOUT".equals(action) || 
               "ACCESS_DENIED".equals(action);
    }
    public boolean isError() {
        return "ERROR".equals(severity) || "CRITICAL".equals(severity);
    }
    public boolean isDataModification() {
        return "CREATE".equals(action) || "UPDATE".equals(action) || "DELETE".equals(action);
    }
    public String getUserIdentifier() {
        return user != null ? user.getUsername() : "SYSTEM";
    }
} 