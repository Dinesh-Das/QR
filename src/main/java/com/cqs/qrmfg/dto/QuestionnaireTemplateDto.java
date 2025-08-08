package com.cqs.qrmfg.dto;

import java.time.LocalDateTime;
import java.util.List;

public class QuestionnaireTemplateDto {
    private Long id;
    private Integer srNo;
    private String checklistText;
    private String comments;
    private String responsible;
    private String questionId;
    private String questionText;
    private String questionType;
    private Integer stepNumber;
    private String fieldName;
    private Boolean isRequired;
    private List<String> options;
    private String validationRules;
    private String conditionalLogic;
    private String helpText;
    private String category;
    private Integer orderIndex;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    
    // New fields for template structure
    private List<QuestionnaireStepDto> steps;
    private String materialCode;
    private String plantCode;
    private String templateType;
    private Integer version;

    public QuestionnaireTemplateDto() {}

    public QuestionnaireTemplateDto(Long id, String questionId, String questionText, String questionType, 
                                   Integer stepNumber, String fieldName, Boolean isRequired) {
        this.id = id;
        this.questionId = questionId;
        this.questionText = questionText;
        this.questionType = questionType;
        this.stepNumber = stepNumber;
        this.fieldName = fieldName;
        this.isRequired = isRequired;
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

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public String getValidationRules() { return validationRules; }
    public void setValidationRules(String validationRules) { this.validationRules = validationRules; }

    public String getConditionalLogic() { return conditionalLogic; }
    public void setConditionalLogic(String conditionalLogic) { this.conditionalLogic = conditionalLogic; }

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
    
    public List<QuestionnaireStepDto> getSteps() { return steps; }
    public void setSteps(List<QuestionnaireStepDto> steps) { this.steps = steps; }
    
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
    
    public String getPlantCode() { return plantCode; }
    public void setPlantCode(String plantCode) { this.plantCode = plantCode; }
    
    public String getTemplateType() { return templateType; }
    public void setTemplateType(String templateType) { this.templateType = templateType; }
    
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}