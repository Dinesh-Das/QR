package com.cqs.qrmfg.dto;

import java.util.List;

public class QuestionnaireStepDto {
    private Integer stepNumber;
    private String title;
    private String description;
    private String category;
    private List<QuestionnaireFieldDto> fields;
    private List<QuestionnaireTemplateDto> questions; // Keep for backward compatibility
    private Integer totalFields;
    private Integer requiredFields;
    private Integer totalQuestions; // Keep for backward compatibility
    private Boolean isCompleted;
    private Boolean isActive;

    public QuestionnaireStepDto() {
        this.isActive = true;
        this.isCompleted = false;
    }

    public QuestionnaireStepDto(Integer stepNumber, String title, String description) {
        this();
        this.stepNumber = stepNumber;
        this.title = title;
        this.description = description;
    }

    // Legacy constructor for backward compatibility - using different parameter order
    public QuestionnaireStepDto(Integer stepNumber, String stepTitle, String category, boolean isLegacy) {
        this();
        this.stepNumber = stepNumber;
        this.title = stepTitle;
        this.category = category;
    }

    // Getters and setters
    public Integer getStepNumber() { return stepNumber; }
    public void setStepNumber(Integer stepNumber) { this.stepNumber = stepNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<QuestionnaireFieldDto> getFields() { return fields; }
    public void setFields(List<QuestionnaireFieldDto> fields) { 
        this.fields = fields;
        if (fields != null) {
            this.totalFields = fields.size();
            this.requiredFields = (int) fields.stream().filter(QuestionnaireFieldDto::isRequired).count();
        }
    }

    public List<QuestionnaireTemplateDto> getQuestions() { return questions; }
    public void setQuestions(List<QuestionnaireTemplateDto> questions) { 
        this.questions = questions;
        this.totalQuestions = questions != null ? questions.size() : 0;
    }

    public Integer getTotalFields() { return totalFields; }
    public void setTotalFields(Integer totalFields) { this.totalFields = totalFields; }
    
    public Integer getRequiredFields() { return requiredFields; }
    public void setRequiredFields(Integer requiredFields) { this.requiredFields = requiredFields; }

    public Integer getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; }

    public Boolean getIsCompleted() { return isCompleted; }
    public void setIsCompleted(Boolean isCompleted) { this.isCompleted = isCompleted; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    // Legacy getters for backward compatibility
    public String getStepTitle() { return title; }
    public void setStepTitle(String stepTitle) { this.title = stepTitle; }
}