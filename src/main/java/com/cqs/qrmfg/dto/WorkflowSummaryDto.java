package com.cqs.qrmfg.dto;

import com.cqs.qrmfg.model.WorkflowState;

import java.time.LocalDateTime;

/**
 * DTO for workflow summary information
 */
public class WorkflowSummaryDto {
    private Long id;
    private String projectCode;
    private String materialCode;
    private String materialName;
    private String materialDescription;
    private String itemDescription;
    private WorkflowState currentState;
    private String assignedPlant;
    private String plantCode;
    private String initiatedBy;
    private int daysPending;
    private long totalQueries;
    private long openQueries;
    private long documentCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastModified;
    private boolean overdue;

    // Constructors
    public WorkflowSummaryDto() {}

    public WorkflowSummaryDto(Long id, String projectCode, String materialCode, String materialName, 
                             String materialDescription, String itemDescription, WorkflowState currentState, String assignedPlant, 
                             String plantCode, String initiatedBy, int daysPending, 
                             long totalQueries, long openQueries, long documentCount, LocalDateTime createdAt, 
                             LocalDateTime lastModified, boolean overdue) {
        this.id = id;
        this.projectCode = projectCode;
        this.materialCode = materialCode;
        this.materialName = materialName;
        this.materialDescription = materialDescription;
        this.itemDescription = itemDescription;
        this.currentState = currentState;
        this.assignedPlant = assignedPlant;
        this.plantCode = plantCode;
        this.initiatedBy = initiatedBy;
        this.daysPending = daysPending;
        this.totalQueries = totalQueries;
        this.openQueries = openQueries;
        this.documentCount = documentCount;
        this.createdAt = createdAt;
        this.lastModified = lastModified;
        this.overdue = overdue;
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

    public WorkflowState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(WorkflowState currentState) {
        this.currentState = currentState;
    }

    public String getAssignedPlant() {
        return assignedPlant;
    }

    public void setAssignedPlant(String assignedPlant) {
        this.assignedPlant = assignedPlant;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public void setInitiatedBy(String initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    public int getDaysPending() {
        return daysPending;
    }

    public void setDaysPending(int daysPending) {
        this.daysPending = daysPending;
    }

    public long getTotalQueries() {
        return totalQueries;
    }

    public void setTotalQueries(long totalQueries) {
        this.totalQueries = totalQueries;
    }

    public long getOpenQueries() {
        return openQueries;
    }

    public void setOpenQueries(long openQueries) {
        this.openQueries = openQueries;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public boolean isOverdue() {
        return overdue;
    }

    public void setOverdue(boolean overdue) {
        this.overdue = overdue;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getMaterialDescription() {
        return materialDescription;
    }

    public void setMaterialDescription(String materialDescription) {
        this.materialDescription = materialDescription;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public String getPlantCode() {
        return plantCode;
    }

    public void setPlantCode(String plantCode) {
        this.plantCode = plantCode;
    }



    public long getDocumentCount() {
        return documentCount;
    }

    public void setDocumentCount(long documentCount) {
        this.documentCount = documentCount;
    }

    // Alias for frontend compatibility
    public WorkflowState getState() {
        return currentState;
    }

    public void setState(WorkflowState state) {
        this.currentState = state;
    }

    // Alias for frontend compatibility
    public long getOpenQueriesCount() {
        return openQueries;
    }

    public void setOpenQueriesCount(long openQueriesCount) {
        this.openQueries = openQueriesCount;
    }
}