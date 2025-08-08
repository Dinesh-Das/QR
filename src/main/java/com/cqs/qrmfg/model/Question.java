package com.cqs.qrmfg.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "QRMFG_QUESTIONS")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "questions_seq")
    @SequenceGenerator(name = "questions_seq", sequenceName = "QRMFG_QUESTIONS_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "SR_NO")
    private Integer srNo;

    @Column(name = "CHECKLIST_TEXT", length = 2000)
    private String checklistText;

    @Column(name = "COMMENTS", length = 2000)
    private String comments;

    @Column(name = "RESPONSIBLE", length = 100)
    private String responsible;

    @Column(name = "QUESTION_ID", length = 50)
    private String questionId;

    @Column(name = "QUESTION_TEXT", length = 1000)
    private String questionText;

    @Column(name = "QUESTION_TYPE", length = 50)
    private String questionType; // TEXT, SELECT, CHECKBOX, RADIO, etc.

    @Column(name = "STEP_NUMBER")
    private Integer stepNumber;

    @Column(name = "FIELD_NAME", length = 100)
    private String fieldName;

    @Column(name = "IS_REQUIRED")
    private Boolean isRequired = false;

    @Column(name = "OPTIONS", length = 2000)
    private String options; // JSON string for dropdown/radio options

    @Column(name = "VALIDATION_RULES", length = 500)
    private String validationRules;

    @Column(name = "CONDITIONAL_LOGIC", length = 1000)
    private String conditionalLogic; // JSON string for conditional display logic

    @Column(name = "DEPENDS_ON_QUESTION_ID", length = 50)
    private String dependsOnQuestionId; // ID of the question this depends on

    @Column(name = "HELP_TEXT", length = 500)
    private String helpText;

    @Column(name = "CATEGORY", length = 100)
    private String category;

    @Column(name = "ORDER_INDEX")
    private Integer orderIndex;

    @Column(name = "MATERIAL_CODE", length = 50)
    private String materialCode;

    @Column(name = "IS_ACTIVE")
    private Boolean isActive = true;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "CREATED_BY", length = 50)
    private String createdBy;

    @Column(name = "UPDATED_BY", length = 50)
    private String updatedBy;

    public Question() {}

    public Question(Integer srNo, String checklistText, String responsible) {
        this.srNo = srNo;
        this.checklistText = checklistText;
        this.responsible = responsible;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Question(String questionId, String questionText, String questionType, 
                                   Integer stepNumber, String fieldName) {
        this.questionId = questionId;
        this.questionText = questionText;
        this.questionType = questionType;
        this.stepNumber = stepNumber;
        this.fieldName = fieldName;
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
    public boolean isConditional() {
        return conditionalLogic != null && !conditionalLogic.trim().isEmpty();
    }

    public boolean hasOptions() {
        return options != null && !options.trim().isEmpty();
    }

    public String getStepTitle() {
        return String.format("Step %d", stepNumber);
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getSrNo() { return srNo; }
    public void setSrNo(Integer srNo) { this.srNo = srNo; }

    public String getChecklistText() { return checklistText; }
    public void setChecklistText(String checklistText) { this.checklistText = checklistText; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public String getResponsible() { return responsible; }
    public void setResponsible(String responsible) { this.responsible = responsible; }

    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }

    public Integer getStepNumber() { return stepNumber; }
    public void setStepNumber(Integer stepNumber) { this.stepNumber = stepNumber; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public Boolean getIsRequired() { return isRequired; }
    public void setIsRequired(Boolean isRequired) { this.isRequired = isRequired; }

    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }

    public String getValidationRules() { return validationRules; }
    public void setValidationRules(String validationRules) { this.validationRules = validationRules; }

    public String getConditionalLogic() { return conditionalLogic; }
    public void setConditionalLogic(String conditionalLogic) { this.conditionalLogic = conditionalLogic; }

    public String getDependsOnQuestionId() { return dependsOnQuestionId; }
    public void setDependsOnQuestionId(String dependsOnQuestionId) { this.dependsOnQuestionId = dependsOnQuestionId; }

    public String getHelpText() { return helpText; }
    public void setHelpText(String helpText) { this.helpText = helpText; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

    @Override
    public String toString() {
        return String.format("Question{id=%d, srNo=%d, checklistText='%s', responsible='%s'}", 
                           id, srNo, checklistText, responsible);
    }
}