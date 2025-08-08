package com.cqs.qrmfg.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "QRMFG_QUESTION_TEMPLATES")
public class QuestionTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "question_templates_seq")
    @SequenceGenerator(name = "question_templates_seq", sequenceName = "QRMFG_QUESTION_TEMPLATES_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "SR_NO", nullable = false)
    private Integer srNo;

    @Column(name = "CATEGORY", nullable = false, length = 100)
    private String category;

    @Column(name = "QUESTION_TEXT", nullable = false, length = 2000)
    private String questionText;

    @Column(name = "COMMENTS", length = 2000)
    private String comments;

    @Column(name = "RESPONSIBLE", nullable = false, length = 100)
    private String responsible;

    @Column(name = "QUESTION_TYPE", length = 50)
    private String questionType = "TEXT"; // TEXT, SELECT, CHECKBOX, RADIO, TEXTAREA, DISPLAY

    @Column(name = "STEP_NUMBER", nullable = false)
    private Integer stepNumber;

    @Column(name = "FIELD_NAME", nullable = false, length = 100)
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

    @Column(name = "ORDER_INDEX", nullable = false)
    private Integer orderIndex;

    @Column(name = "IS_ACTIVE")
    private Boolean isActive = true;

    @Column(name = "VERSION")
    private Integer version = 1;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "CREATED_BY", length = 50)
    private String createdBy;

    @Column(name = "UPDATED_BY", length = 50)
    private String updatedBy;

    public QuestionTemplate() {}

    public QuestionTemplate(Integer srNo, String category, String questionText, 
                           String responsible, String questionType, Integer stepNumber) {
        this.srNo = srNo;
        this.category = category;
        this.questionText = questionText;
        this.responsible = responsible;
        this.questionType = questionType;
        this.stepNumber = stepNumber;
        this.fieldName = "question_" + srNo;
        this.orderIndex = srNo;
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
        return String.format("Step %d - %s", stepNumber, category);
    }

    public boolean isForCQS() {
        return "CQS".equalsIgnoreCase(responsible);
    }

    public boolean isForPlant() {
        return "Plant".equalsIgnoreCase(responsible) || 
               "All Plants".equalsIgnoreCase(responsible) ||
               "Plant to fill data".equalsIgnoreCase(responsible);
    }

    public boolean isDisplayOnly() {
        return "NONE".equalsIgnoreCase(responsible) || "DISPLAY".equalsIgnoreCase(questionType);
    }

    // Create material-specific master question from template
    public Question createMasterQuestion(String materialCode) {
        Question master = new Question();
        
        master.setSrNo(this.srNo);
        master.setCategory(this.category);
        master.setQuestionText(this.questionText);
        master.setChecklistText(this.questionText);
        master.setComments(this.comments);
        master.setResponsible(this.responsible);
        master.setQuestionType(this.questionType);
        master.setStepNumber(this.stepNumber);
        master.setFieldName(this.fieldName);
        master.setIsRequired(this.isRequired);
        master.setOptions(this.options);
        master.setValidationRules(this.validationRules);
        master.setConditionalLogic(this.conditionalLogic);
        master.setDependsOnQuestionId(this.dependsOnQuestionId);
        master.setHelpText(this.helpText);
        master.setOrderIndex(this.orderIndex);
        master.setMaterialCode(materialCode);
        master.setIsActive(true);
        
        // Generate material-specific question ID
        master.setQuestionId(String.format("%s_Q_%03d", materialCode, this.srNo));
        
        master.setCreatedAt(LocalDateTime.now());
        master.setUpdatedAt(LocalDateTime.now());
        master.setCreatedBy("SYSTEM");
        master.setUpdatedBy("SYSTEM");
        
        return master;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getSrNo() { return srNo; }
    public void setSrNo(Integer srNo) { this.srNo = srNo; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public String getResponsible() { return responsible; }
    public void setResponsible(String responsible) { this.responsible = responsible; }

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

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    @Override
    public String toString() {
        return String.format("QuestionTemplate{id=%d, srNo=%d, category='%s', responsible='%s'}", 
                           id, srNo, category, responsible);
    }
}