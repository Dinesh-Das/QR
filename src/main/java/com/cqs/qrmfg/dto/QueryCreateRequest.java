package com.cqs.qrmfg.dto;

import com.cqs.qrmfg.model.QueryTeam;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class QueryCreateRequest {
    @NotBlank(message = "Question is required")
    @Size(max = 2000, message = "Question must not exceed 2000 characters")
    private String question;

    private Integer stepNumber;

    @Size(max = 100, message = "Field name must not exceed 100 characters")
    private String fieldName;

    @NotNull(message = "Assigned team is required")
    private QueryTeam assignedTeam;

    @Size(max = 20, message = "Priority level must not exceed 20 characters")
    private String priorityLevel = "NORMAL";

    @Size(max = 50, message = "Query category must not exceed 50 characters")
    private String queryCategory;

    public QueryCreateRequest() {}

    public QueryCreateRequest(String question, QueryTeam assignedTeam) {
        this.question = question;
        this.assignedTeam = assignedTeam;
    }

    public QueryCreateRequest(String question, Integer stepNumber, String fieldName, QueryTeam assignedTeam) {
        this.question = question;
        this.stepNumber = stepNumber;
        this.fieldName = fieldName;
        this.assignedTeam = assignedTeam;
    }

    // Getters and setters
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public Integer getStepNumber() { return stepNumber; }
    public void setStepNumber(Integer stepNumber) { this.stepNumber = stepNumber; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public QueryTeam getAssignedTeam() { return assignedTeam; }
    public void setAssignedTeam(QueryTeam assignedTeam) { this.assignedTeam = assignedTeam; }

    public String getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(String priorityLevel) { this.priorityLevel = priorityLevel; }

    public String getQueryCategory() { return queryCategory; }
    public void setQueryCategory(String queryCategory) { this.queryCategory = queryCategory; }

    @Override
    public String toString() {
        return String.format("QueryCreateRequest{question='%s', stepNumber=%d, fieldName='%s', assignedTeam=%s, priorityLevel='%s', queryCategory='%s'}", 
                           question != null ? question.substring(0, Math.min(question.length(), 50)) + "..." : null, 
                           stepNumber, fieldName, assignedTeam, priorityLevel, queryCategory);
    }
}