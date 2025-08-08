package com.cqs.qrmfg.dto;

import com.cqs.qrmfg.model.QueryStatus;
import com.cqs.qrmfg.model.QueryTeam;

import java.time.LocalDateTime;

/**
 * DTO for query summary information
 */
public class QuerySummaryDto {
    private Long id;
    private String materialCode;
    private String materialName;
    private String assignedPlant;
    private Integer stepNumber;
    private String fieldName;
    private String question;
    private String response;
    private QueryTeam assignedTeam;
    private QueryStatus status;
    private String raisedBy;
    private String resolvedBy;
    private String priorityLevel;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private int daysOpen;
    private boolean overdue;
    private boolean highPriority;

    // Constructors
    public QuerySummaryDto() {}

    public QuerySummaryDto(Long id, String materialCode, String materialName, String assignedPlant,
                          Integer stepNumber, String fieldName, String question, String response,
                          QueryTeam assignedTeam, QueryStatus status, String raisedBy, String resolvedBy,
                          String priorityLevel, LocalDateTime createdAt, LocalDateTime resolvedAt,
                          int daysOpen, boolean overdue, boolean highPriority) {
        this.id = id;
        this.materialCode = materialCode;
        this.materialName = materialName;
        this.assignedPlant = assignedPlant;
        this.stepNumber = stepNumber;
        this.fieldName = fieldName;
        this.question = question;
        this.response = response;
        this.assignedTeam = assignedTeam;
        this.status = status;
        this.raisedBy = raisedBy;
        this.resolvedBy = resolvedBy;
        this.priorityLevel = priorityLevel;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
        this.daysOpen = daysOpen;
        this.overdue = overdue;
        this.highPriority = highPriority;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getAssignedPlant() {
        return assignedPlant;
    }

    public void setAssignedPlant(String assignedPlant) {
        this.assignedPlant = assignedPlant;
    }

    public Integer getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(Integer stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public QueryTeam getAssignedTeam() {
        return assignedTeam;
    }

    public void setAssignedTeam(QueryTeam assignedTeam) {
        this.assignedTeam = assignedTeam;
    }

    public QueryStatus getStatus() {
        return status;
    }

    public void setStatus(QueryStatus status) {
        this.status = status;
    }

    public String getRaisedBy() {
        return raisedBy;
    }

    public void setRaisedBy(String raisedBy) {
        this.raisedBy = raisedBy;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public String getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(String priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public int getDaysOpen() {
        return daysOpen;
    }

    public void setDaysOpen(int daysOpen) {
        this.daysOpen = daysOpen;
    }

    public boolean isOverdue() {
        return overdue;
    }

    public void setOverdue(boolean overdue) {
        this.overdue = overdue;
    }

    public boolean isHighPriority() {
        return highPriority;
    }

    public void setHighPriority(boolean highPriority) {
        this.highPriority = highPriority;
    }
}