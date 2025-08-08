package com.cqs.qrmfg.dto;

import com.cqs.qrmfg.model.QueryStatus;
import com.cqs.qrmfg.model.QueryTeam;

import java.time.LocalDateTime;

/**
 * DTO for enhanced query context information including material, project, plant details
 */
public class QueryContextDto {
    private Long id;
    private String materialCode;
    private String materialName;
    private String materialDescription;
    private String projectCode;
    private String projectName;
    private String plantCode;
    private String plantName;
    private Integer stepNumber;
    private String fieldName;
    private String question;
    private String response;
    private QueryTeam assignedTeam;
    private QueryStatus status;
    private String raisedBy;
    private String resolvedBy;
    private String priorityLevel;
    private String queryCategory;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private int daysOpen;
    private boolean overdue;
    private boolean highPriority;
    private double slaProgress;
    private String slaStatus;

    // Constructors
    public QueryContextDto() {}

    public QueryContextDto(Long id, String materialCode, String materialName, String materialDescription,
                          String projectCode, String projectName, String plantCode, String plantName,
                          Integer stepNumber, String fieldName,
                          String question, String response, QueryTeam assignedTeam, QueryStatus status,
                          String raisedBy, String resolvedBy, String priorityLevel, String queryCategory,
                          LocalDateTime createdAt, LocalDateTime resolvedAt, int daysOpen,
                          boolean overdue, boolean highPriority, double slaProgress, String slaStatus) {
        this.id = id;
        this.materialCode = materialCode;
        this.materialName = materialName;
        this.materialDescription = materialDescription;
        this.projectCode = projectCode;
        this.projectName = projectName;
        this.plantCode = plantCode;
        this.plantName = plantName;
        this.stepNumber = stepNumber;
        this.fieldName = fieldName;
        this.question = question;
        this.response = response;
        this.assignedTeam = assignedTeam;
        this.status = status;
        this.raisedBy = raisedBy;
        this.resolvedBy = resolvedBy;
        this.priorityLevel = priorityLevel;
        this.queryCategory = queryCategory;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
        this.daysOpen = daysOpen;
        this.overdue = overdue;
        this.highPriority = highPriority;
        this.slaProgress = slaProgress;
        this.slaStatus = slaStatus;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }

    public String getMaterialDescription() { return materialDescription; }
    public void setMaterialDescription(String materialDescription) { this.materialDescription = materialDescription; }

    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getPlantCode() { return plantCode; }
    public void setPlantCode(String plantCode) { this.plantCode = plantCode; }

    public String getPlantName() { return plantName; }
    public void setPlantName(String plantName) { this.plantName = plantName; }

    public Integer getStepNumber() { return stepNumber; }
    public void setStepNumber(Integer stepNumber) { this.stepNumber = stepNumber; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public QueryTeam getAssignedTeam() { return assignedTeam; }
    public void setAssignedTeam(QueryTeam assignedTeam) { this.assignedTeam = assignedTeam; }

    public QueryStatus getStatus() { return status; }
    public void setStatus(QueryStatus status) { this.status = status; }

    public String getRaisedBy() { return raisedBy; }
    public void setRaisedBy(String raisedBy) { this.raisedBy = raisedBy; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public String getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(String priorityLevel) { this.priorityLevel = priorityLevel; }

    public String getQueryCategory() { return queryCategory; }
    public void setQueryCategory(String queryCategory) { this.queryCategory = queryCategory; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public int getDaysOpen() { return daysOpen; }
    public void setDaysOpen(int daysOpen) { this.daysOpen = daysOpen; }

    public boolean isOverdue() { return overdue; }
    public void setOverdue(boolean overdue) { this.overdue = overdue; }

    public boolean isHighPriority() { return highPriority; }
    public void setHighPriority(boolean highPriority) { this.highPriority = highPriority; }

    public double getSlaProgress() { return slaProgress; }
    public void setSlaProgress(double slaProgress) { this.slaProgress = slaProgress; }

    public String getSlaStatus() { return slaStatus; }
    public void setSlaStatus(String slaStatus) { this.slaStatus = slaStatus; }
}