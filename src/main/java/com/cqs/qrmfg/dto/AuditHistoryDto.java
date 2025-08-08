package com.cqs.qrmfg.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for audit history entries
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditHistoryDto {
    
    private Long id;
    private Long revisionId;
    private String revisionType; // ADD, MOD, DEL
    private String entityType;
    private String entityId;
    private String username;
    private String action;
    private String description;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime revisionDate;
    
    private List<FieldChangeDto> changes;
    private Map<String, Object> details;
    private String severity;
    private String category;
    
    // Workflow-specific fields
    private String materialCode;
    private String workflowState;
    private String newState;
    private String oldState;
    
    // Query-specific fields
    private String queryStatus;
    private String assignedTeam;
    
    // Response-specific fields
    private Integer stepNumber;
    private String fieldName;
    private String fieldValue;
    private String previousValue;
    
    public AuditHistoryDto() {}
    
    public AuditHistoryDto(String entityType, String action, String username, LocalDateTime timestamp) {
        this.entityType = entityType;
        this.action = action;
        this.username = username;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRevisionId() { return revisionId; }
    public void setRevisionId(Long revisionId) { this.revisionId = revisionId; }

    public String getRevisionType() { return revisionType; }
    public void setRevisionType(String revisionType) { this.revisionType = revisionType; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public LocalDateTime getRevisionDate() { return revisionDate; }
    public void setRevisionDate(LocalDateTime revisionDate) { this.revisionDate = revisionDate; }

    public List<FieldChangeDto> getChanges() { return changes; }
    public void setChanges(List<FieldChangeDto> changes) { this.changes = changes; }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

    public String getWorkflowState() { return workflowState; }
    public void setWorkflowState(String workflowState) { this.workflowState = workflowState; }

    public String getNewState() { return newState; }
    public void setNewState(String newState) { this.newState = newState; }

    public String getOldState() { return oldState; }
    public void setOldState(String oldState) { this.oldState = oldState; }

    public String getQueryStatus() { return queryStatus; }
    public void setQueryStatus(String queryStatus) { this.queryStatus = queryStatus; }

    public String getAssignedTeam() { return assignedTeam; }
    public void setAssignedTeam(String assignedTeam) { this.assignedTeam = assignedTeam; }

    public Integer getStepNumber() { return stepNumber; }
    public void setStepNumber(Integer stepNumber) { this.stepNumber = stepNumber; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getFieldValue() { return fieldValue; }
    public void setFieldValue(String fieldValue) { this.fieldValue = fieldValue; }

    public String getPreviousValue() { return previousValue; }
    public void setPreviousValue(String previousValue) { this.previousValue = previousValue; }

    /**
     * DTO for field changes
     */
    public static class FieldChangeDto {
        private String field;
        private String oldValue;
        private String newValue;
        private String changeType; // ADDED, MODIFIED, REMOVED

        public FieldChangeDto() {}

        public FieldChangeDto(String field, String oldValue, String newValue) {
            this.field = field;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.changeType = determineChangeType(oldValue, newValue);
        }

        private String determineChangeType(String oldValue, String newValue) {
            if (oldValue == null && newValue != null) return "ADDED";
            if (oldValue != null && newValue == null) return "REMOVED";
            return "MODIFIED";
        }

        // Getters and setters
        public String getField() { return field; }
        public void setField(String field) { this.field = field; }

        public String getOldValue() { return oldValue; }
        public void setOldValue(String oldValue) { this.oldValue = oldValue; }

        public String getNewValue() { return newValue; }
        public void setNewValue(String newValue) { this.newValue = newValue; }

        public String getChangeType() { return changeType; }
        public void setChangeType(String changeType) { this.changeType = changeType; }
    }
}