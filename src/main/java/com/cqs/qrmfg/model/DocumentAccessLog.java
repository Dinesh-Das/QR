package com.cqs.qrmfg.model;

import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity to track document access and downloads for audit purposes
 */
@Entity
@Table(name = "QRMFG_ACCESS_LOG")
// @Audited  // Temporarily disabled to fix constraint issues
public class DocumentAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "accessed_by", nullable = false, length = 100)
    private String accessedBy;

    @Column(name = "access_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DocumentAccessType accessType;

    @Column(name = "access_time", nullable = false)
    private LocalDateTime accessTime;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "workflow_id")
    private Long workflowId;

    @Column(name = "access_granted", nullable = false)
    private Boolean accessGranted;

    @Column(name = "denial_reason", length = 255)
    private String denialReason;

    // Constructors
    public DocumentAccessLog() {}

    public DocumentAccessLog(Document document, String accessedBy, DocumentAccessType accessType, 
                           LocalDateTime accessTime, String ipAddress, String userAgent, Long workflowId,
                           Boolean accessGranted, String denialReason) {
        this.document = document;
        this.accessedBy = accessedBy;
        this.accessType = accessType;
        this.accessTime = accessTime;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.workflowId = workflowId;
        this.accessGranted = accessGranted;
        this.denialReason = denialReason;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
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

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
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
}