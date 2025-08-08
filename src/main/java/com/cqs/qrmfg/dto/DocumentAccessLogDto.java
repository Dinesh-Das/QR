package com.cqs.qrmfg.dto;

import com.cqs.qrmfg.model.DocumentAccessType;

import java.time.LocalDateTime;

/**
 * DTO for document access log information
 */
public class DocumentAccessLogDto {
    private Long id;
    private String accessedBy;
    private DocumentAccessType accessType;
    private LocalDateTime accessTime;
    private String ipAddress;
    private Boolean accessGranted;
    private String denialReason;
    private String documentName;
    private Long workflowId;

    // Constructors
    public DocumentAccessLogDto() {}

    public DocumentAccessLogDto(Long id, String accessedBy, DocumentAccessType accessType, 
                               LocalDateTime accessTime, String ipAddress, Boolean accessGranted, 
                               String denialReason, String documentName, Long workflowId) {
        this.id = id;
        this.accessedBy = accessedBy;
        this.accessType = accessType;
        this.accessTime = accessTime;
        this.ipAddress = ipAddress;
        this.accessGranted = accessGranted;
        this.denialReason = denialReason;
        this.documentName = documentName;
        this.workflowId = workflowId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccessedBy() {
        return accessedBy;
    }

    public void setAccessedBy(String accessedBy) {
        this.accessedBy = accessedBy;
    }

    public DocumentAccessType getAccessType() {
        return accessType;
    }

    public void setAccessType(DocumentAccessType accessType) {
        this.accessType = accessType;
    }

    public LocalDateTime getAccessTime() {
        return accessTime;
    }

    public void setAccessTime(LocalDateTime accessTime) {
        this.accessTime = accessTime;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Boolean getAccessGranted() {
        return accessGranted;
    }

    public void setAccessGranted(Boolean accessGranted) {
        this.accessGranted = accessGranted;
    }

    public String getDenialReason() {
        return denialReason;
    }

    public void setDenialReason(String denialReason) {
        this.denialReason = denialReason;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
    }
}