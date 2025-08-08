package com.cqs.qrmfg.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class PlantSpecificDataDto {
    private String plantCode;
    private String materialCode;
    private Map<String, Object> cqsInputs;
    private Map<String, Object> plantInputs;
    private Map<String, Object> combinedData;
    private String completionStatus;
    private Integer completionPercentage;
    private Integer totalFields;
    private Integer completedFields;
    private Integer requiredFields;
    private Integer completedRequiredFields;
    private String cqsSyncStatus;
    private LocalDateTime lastCqsSync;
    private Long workflowId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime submittedAt;
    private String submittedBy;
    private Integer version;
    private Boolean isActive;
    
    public PlantSpecificDataDto() {}
    
    public PlantSpecificDataDto(String plantCode, String materialCode) {
        this.plantCode = plantCode;
        this.materialCode = materialCode;
        this.completionStatus = "DRAFT";
        this.completionPercentage = 0;
        this.cqsSyncStatus = "PENDING";
        this.version = 1;
        this.isActive = true;
    }
    
    // Getters and setters
    public String getPlantCode() { return plantCode; }
    public void setPlantCode(String plantCode) { this.plantCode = plantCode; }
    
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
    

    
    public Map<String, Object> getCqsInputs() { return cqsInputs; }
    public void setCqsInputs(Map<String, Object> cqsInputs) { this.cqsInputs = cqsInputs; }
    
    public Map<String, Object> getPlantInputs() { return plantInputs; }
    public void setPlantInputs(Map<String, Object> plantInputs) { this.plantInputs = plantInputs; }
    
    public Map<String, Object> getCombinedData() { return combinedData; }
    public void setCombinedData(Map<String, Object> combinedData) { this.combinedData = combinedData; }
    
    public String getCompletionStatus() { return completionStatus; }
    public void setCompletionStatus(String completionStatus) { this.completionStatus = completionStatus; }
    
    public Integer getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(Integer completionPercentage) { this.completionPercentage = completionPercentage; }
    
    public Integer getTotalFields() { return totalFields; }
    public void setTotalFields(Integer totalFields) { this.totalFields = totalFields; }
    
    public Integer getCompletedFields() { return completedFields; }
    public void setCompletedFields(Integer completedFields) { this.completedFields = completedFields; }
    
    public Integer getRequiredFields() { return requiredFields; }
    public void setRequiredFields(Integer requiredFields) { this.requiredFields = requiredFields; }
    
    public Integer getCompletedRequiredFields() { return completedRequiredFields; }
    public void setCompletedRequiredFields(Integer completedRequiredFields) { this.completedRequiredFields = completedRequiredFields; }
    
    public String getCqsSyncStatus() { return cqsSyncStatus; }
    public void setCqsSyncStatus(String cqsSyncStatus) { this.cqsSyncStatus = cqsSyncStatus; }
    
    public LocalDateTime getLastCqsSync() { return lastCqsSync; }
    public void setLastCqsSync(LocalDateTime lastCqsSync) { this.lastCqsSync = lastCqsSync; }
    
    public Long getWorkflowId() { return workflowId; }
    public void setWorkflowId(Long workflowId) { this.workflowId = workflowId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    
    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
    
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}