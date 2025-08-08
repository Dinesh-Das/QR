package com.cqs.qrmfg.model;

import org.hibernate.envers.Audited;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "QRMFG_ANSWERS")
// @Audited  // Temporarily disabled to fix constraint issues
public class Answer {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "answers_seq")
    @SequenceGenerator(name = "answers_seq", sequenceName = "QRMFG_ANSWERS_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(name = "step_number", nullable = false)
    private Integer stepNumber;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "field_value", length = 2000)
    private String fieldValue;

    @Column(name = "field_type", length = 50)
    private String fieldType = "TEXT"; // TEXT, NUMBER, DATE, BOOLEAN, SELECT, TEXTAREA

    @Column(name = "is_required")
    private Boolean isRequired = false;

    @Column(name = "validation_status", length = 20)
    private String validationStatus = "VALID"; // VALID, INVALID, PENDING

    @Column(name = "validation_message", length = 500)
    private String validationMessage;

    @Column(name = "is_draft")
    private Boolean isDraft = false;

    @Column(name = "section_name", length = 100)
    private String sectionName;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "plant_code", length = 50)
    private String plantCode;

    @Column(name = "material_code", length = 50)
    private String materialCode;

    @Column(name = "last_modified", nullable = false)
    private LocalDateTime lastModified;

    @Column(name = "modified_by", nullable = false, length = 100)
    private String modifiedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    public Answer() {}

    public Answer(Workflow workflow, Integer stepNumber, String fieldName, 
                 String fieldValue, String modifiedBy) {
        this.workflow = workflow;
        this.stepNumber = stepNumber;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.modifiedBy = modifiedBy;
        this.createdAt = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
        this.createdBy = modifiedBy;
    }

    public Answer(Workflow workflow, Integer stepNumber, String fieldName, 
                 String fieldValue, String fieldType, String modifiedBy) {
        this(workflow, stepNumber, fieldName, fieldValue, modifiedBy);
        this.fieldType = fieldType;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        lastModified = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastModified = LocalDateTime.now();
    }

    // Business logic methods
    public void updateValue(String newValue, String modifiedBy) {
        this.fieldValue = newValue;
        this.modifiedBy = modifiedBy;
        this.lastModified = LocalDateTime.now();
        this.isDraft = false; // Mark as non-draft when explicitly updated
    }

    public void saveDraft(String draftValue, String modifiedBy) {
        this.fieldValue = draftValue;
        this.modifiedBy = modifiedBy;
        this.lastModified = LocalDateTime.now();
        this.isDraft = true;
    }

    public boolean isEmpty() {
        return fieldValue == null || fieldValue.trim().isEmpty();
    }

    public boolean isValid() {
        return "VALID".equals(validationStatus);
    }

    public void markInvalid(String validationMessage) {
        this.validationStatus = "INVALID";
        this.validationMessage = validationMessage;
    }

    public void markValid() {
        this.validationStatus = "VALID";
        this.validationMessage = null;
    }

    public boolean isRequiredAndEmpty() {
        return Boolean.TRUE.equals(isRequired) && isEmpty();
    }

    public String getDisplayValue() {
        if (isEmpty()) {
            return isDraft ? "[Draft]" : "[Not Provided]";
        }
        return fieldValue;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Workflow getWorkflow() { return workflow; }
    public void setWorkflow(Workflow workflow) { this.workflow = workflow; }

    public Integer getStepNumber() { return stepNumber; }
    public void setStepNumber(Integer stepNumber) { this.stepNumber = stepNumber; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getFieldValue() { return fieldValue; }
    public void setFieldValue(String fieldValue) { this.fieldValue = fieldValue; }

    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }

    public Boolean getIsRequired() { return isRequired; }
    public void setIsRequired(Boolean isRequired) { this.isRequired = isRequired; }

    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }

    public String getValidationMessage() { return validationMessage; }
    public void setValidationMessage(String validationMessage) { this.validationMessage = validationMessage; }

    public Boolean getIsDraft() { return isDraft; }
    public void setIsDraft(Boolean isDraft) { this.isDraft = isDraft; }

    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }

    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getPlantCode() { return plantCode; }
    public void setPlantCode(String plantCode) { this.plantCode = plantCode; }

    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

    @Override
    public String toString() {
        return String.format("Answer{id=%d, workflow=%s, step=%d, field='%s', value='%s'}", 
                           id, workflow != null ? workflow.getId() : null, stepNumber, fieldName, 
                           fieldValue != null && fieldValue.length() > 50 ? fieldValue.substring(0, 50) + "..." : fieldValue);
    }
}