package com.cqs.qrmfg.dto;

import java.util.Map;

public class WorkflowMonitoringDto {
    private long totalWorkflows;
    private long activeWorkflows;
    private long completedWorkflows;
    private long overdueWorkflows;
    private long workflowsWithOpenQueries;
    private Map<String, Long> workflowsByState;
    private Map<String, Long> workflowsByPlant;
    private Map<String, Long> recentActivity;
    private double averageCompletionTimeHours;
    private long totalQueries;
    private long openQueries;
    private long overdueQueries;

    // Constructors
    public WorkflowMonitoringDto() {}

    public WorkflowMonitoringDto(long totalWorkflows, long activeWorkflows, long completedWorkflows, 
                               long overdueWorkflows, long workflowsWithOpenQueries, 
                               Map<String, Long> workflowsByState, Map<String, Long> workflowsByPlant,
                               Map<String, Long> recentActivity, double averageCompletionTimeHours,
                               long totalQueries, long openQueries, long overdueQueries) {
        this.totalWorkflows = totalWorkflows;
        this.activeWorkflows = activeWorkflows;
        this.completedWorkflows = completedWorkflows;
        this.overdueWorkflows = overdueWorkflows;
        this.workflowsWithOpenQueries = workflowsWithOpenQueries;
        this.workflowsByState = workflowsByState;
        this.workflowsByPlant = workflowsByPlant;
        this.recentActivity = recentActivity;
        this.averageCompletionTimeHours = averageCompletionTimeHours;
        this.totalQueries = totalQueries;
        this.openQueries = openQueries;
        this.overdueQueries = overdueQueries;
    }

    // Getters and Setters
    public long getTotalWorkflows() {
        return totalWorkflows;
    }

    public void setTotalWorkflows(long totalWorkflows) {
        this.totalWorkflows = totalWorkflows;
    }

    public long getActiveWorkflows() {
        return activeWorkflows;
    }

    public void setActiveWorkflows(long activeWorkflows) {
        this.activeWorkflows = activeWorkflows;
    }

    public long getCompletedWorkflows() {
        return completedWorkflows;
    }

    public void setCompletedWorkflows(long completedWorkflows) {
        this.completedWorkflows = completedWorkflows;
    }

    public long getOverdueWorkflows() {
        return overdueWorkflows;
    }

    public void setOverdueWorkflows(long overdueWorkflows) {
        this.overdueWorkflows = overdueWorkflows;
    }

    public long getWorkflowsWithOpenQueries() {
        return workflowsWithOpenQueries;
    }

    public void setWorkflowsWithOpenQueries(long workflowsWithOpenQueries) {
        this.workflowsWithOpenQueries = workflowsWithOpenQueries;
    }

    public Map<String, Long> getWorkflowsByState() {
        return workflowsByState;
    }

    public void setWorkflowsByState(Map<String, Long> workflowsByState) {
        this.workflowsByState = workflowsByState;
    }

    public Map<String, Long> getWorkflowsByPlant() {
        return workflowsByPlant;
    }

    public void setWorkflowsByPlant(Map<String, Long> workflowsByPlant) {
        this.workflowsByPlant = workflowsByPlant;
    }

    public Map<String, Long> getRecentActivity() {
        return recentActivity;
    }

    public void setRecentActivity(Map<String, Long> recentActivity) {
        this.recentActivity = recentActivity;
    }

    public double getAverageCompletionTimeHours() {
        return averageCompletionTimeHours;
    }

    public void setAverageCompletionTimeHours(double averageCompletionTimeHours) {
        this.averageCompletionTimeHours = averageCompletionTimeHours;
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

    public long getOverdueQueries() {
        return overdueQueries;
    }

    public void setOverdueQueries(long overdueQueries) {
        this.overdueQueries = overdueQueries;
    }
}