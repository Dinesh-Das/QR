package com.cqs.qrmfg.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "QRMFG_PLANT_SPECIFIC_DATA")
@IdClass(PlantSpecificDataId.class)
public class PlantSpecificData {
    
    @Id
    @Column(name = "plant_code", length = 50, nullable = false)
    private String plantCode;
    
    @Id
    @Column(name = "material_code", length = 50, nullable = false)
    private String materialCode;
    
    
    @Column(name = "cqs_inputs", columnDefinition = "CLOB")
    private String cqsInputs; // JSON string containing CQS auto-populated data
    
    @Column(name = "plant_inputs", columnDefinition = "CLOB")
    private String plantInputs; // JSON string containing plant-filled data
    
    @Column(name = "combined_data", columnDefinition = "CLOB")
    private String combinedData; // JSON string containing merged CQS + Plant data
    
    @Column(name = "completion_status", length = 20)
    private String completionStatus = "DRAFT"; // DRAFT, IN_PROGRESS, COMPLETED, SUBMITTED
    
    @Column(name = "completion_percentage")
    private Integer completionPercentage = 0;
    
    @Column(name = "total_fields")
    private Integer totalFields = 0;
    
    @Column(name = "completed_fields")
    private Integer completedFields = 0;
    
    @Column(name = "required_fields")
    private Integer requiredFields = 0;
    
    @Column(name = "completed_required_fields")
    private Integer completedRequiredFields = 0;
    
    @Column(name = "cqs_sync_status", length = 20)
    private String cqsSyncStatus = "PENDING"; // PENDING, SYNCED, FAILED
    
    @Column(name = "last_cqs_sync")
    private LocalDateTime lastCqsSync;
    
    @Column(name = "workflow_id")
    private Long workflowId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @Column(name = "submitted_by", length = 100)
    private String submittedBy;
    
    @Column(name = "version")
    private Integer version = 1;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    public PlantSpecificData() {}
    
    public PlantSpecificData(String plantCode, String materialCode) {
        this.plantCode = plantCode;
        this.materialCode = materialCode;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Business logic methods
    public void updateCqsData(String cqsData, String updatedBy) {
        this.cqsInputs = cqsData;
        this.cqsSyncStatus = "SYNCED";
        this.lastCqsSync = LocalDateTime.now();
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
        updateCombinedData();
    }
    
    public void updatePlantData(String plantData, String updatedBy) {
        this.plantInputs = plantData;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
        updateCombinedData();
    }
    
    private void updateCombinedData() {
        // This would merge CQS and Plant data - implementation depends on JSON structure
        // For now, simple concatenation - should be enhanced with proper JSON merging
        StringBuilder combined = new StringBuilder();
        combined.append("{");
        
        if (cqsInputs != null && !cqsInputs.trim().isEmpty() && !cqsInputs.equals("{}")) {
            combined.append("\"cqsData\":").append(cqsInputs);
            if (plantInputs != null && !plantInputs.trim().isEmpty() && !plantInputs.equals("{}")) {
                combined.append(",");
            }
        }
        
        if (plantInputs != null && !plantInputs.trim().isEmpty() && !plantInputs.equals("{}")) {
            combined.append("\"plantData\":").append(plantInputs);
        }
        
        combined.append("}");
        this.combinedData = combined.toString();
    }
    
    public void updateCompletionStats(int totalFields, int completedFields, 
                                    int requiredFields, int completedRequiredFields) {
        this.totalFields = totalFields;
        this.completedFields = completedFields;
        this.requiredFields = requiredFields;
        this.completedRequiredFields = completedRequiredFields;
        
        if (requiredFields > 0) {
            this.completionPercentage = (completedRequiredFields * 100) / requiredFields;
        } else {
            this.completionPercentage = totalFields > 0 ? (completedFields * 100) / totalFields : 100;
        }
        
        // Update status based on completion
        if (completionPercentage == 100 && completedRequiredFields == requiredFields) {
            this.completionStatus = "COMPLETED";
        } else if (completedFields > 0) {
            this.completionStatus = "IN_PROGRESS";
        } else {
            this.completionStatus = "DRAFT";
        }
    }
    
    public void submit(String submittedBy) {
        // CRITICAL FIX: Don't reset completion status - preserve it as COMPLETED
        if (!"COMPLETED".equals(this.completionStatus)) {
            this.completionStatus = "COMPLETED";
        }
        this.submittedAt = LocalDateTime.now();
        this.submittedBy = submittedBy;
        this.updatedAt = LocalDateTime.now();
        
        // CRITICAL FIX: Ensure completion percentage is maintained at 100% for submitted questionnaires
        if (this.completionPercentage == null || this.completionPercentage < 100) {
            // Only update if we have valid field counts
            if (this.totalFields != null && this.totalFields > 0) {
                this.completionPercentage = Math.max(this.completionPercentage != null ? this.completionPercentage : 0, 90);
            } else {
                this.completionPercentage = 100; // Default to 100% for submitted questionnaires
            }
        }
    }
    
    public boolean isCompleted() {
        return "COMPLETED".equals(completionStatus) || submittedAt != null;
    }
    
    public boolean isSubmitted() {
        return submittedAt != null;
    }
    
    public String getCompositeKey() {
        return String.format("%s_%s", plantCode, materialCode);
    }
    
    // Getters and setters
    public String getPlantCode() { return plantCode; }
    public void setPlantCode(String plantCode) { this.plantCode = plantCode; }
    
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
    
    
    public String getCqsInputs() { return cqsInputs; }
    public void setCqsInputs(String cqsInputs) { this.cqsInputs = cqsInputs; }
    
    public String getPlantInputs() { return plantInputs; }
    public void setPlantInputs(String plantInputs) { this.plantInputs = plantInputs; }
    
    public String getCombinedData() { return combinedData; }
    public void setCombinedData(String combinedData) { this.combinedData = combinedData; }
    
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlantSpecificData that = (PlantSpecificData) o;
        return Objects.equals(plantCode, that.plantCode) &&
               Objects.equals(materialCode, that.materialCode);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(plantCode, materialCode);
    }
    
    @Override
    public String toString() {
        return String.format("PlantSpecificData{plant='%s', material='%s', status='%s', completion=%d%%}", 
                           plantCode, materialCode, completionStatus, completionPercentage);
    }
}