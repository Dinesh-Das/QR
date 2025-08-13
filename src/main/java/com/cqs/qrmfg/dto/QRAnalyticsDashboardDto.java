package com.cqs.qrmfg.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class QRAnalyticsDashboardDto {
    private Long totalProduction;
    private Double qualityScore;
    private Long activeWorkflows;
    private Long completedToday;
    private Map<String, Object> productionByPlant;
    private Map<String, Object> qualityTrends;
    private Map<String, Object> workflowStatus;
    private LocalDateTime lastUpdated;
    private String plantCode;
    private String userRole;

    // Constructors
    public QRAnalyticsDashboardDto() {}

    public QRAnalyticsDashboardDto(Long totalProduction, Double qualityScore, 
                                   Long activeWorkflows, Long completedToday) {
        this.totalProduction = totalProduction;
        this.qualityScore = qualityScore;
        this.activeWorkflows = activeWorkflows;
        this.completedToday = completedToday;
        this.lastUpdated = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getTotalProduction() {
        return totalProduction;
    }

    public void setTotalProduction(Long totalProduction) {
        this.totalProduction = totalProduction;
    }

    public Double getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Double qualityScore) {
        this.qualityScore = qualityScore;
    }

    public Long getActiveWorkflows() {
        return activeWorkflows;
    }

    public void setActiveWorkflows(Long activeWorkflows) {
        this.activeWorkflows = activeWorkflows;
    }

    public Long getCompletedToday() {
        return completedToday;
    }

    public void setCompletedToday(Long completedToday) {
        this.completedToday = completedToday;
    }

    public Map<String, Object> getProductionByPlant() {
        return productionByPlant;
    }

    public void setProductionByPlant(Map<String, Object> productionByPlant) {
        this.productionByPlant = productionByPlant;
    }

    public Map<String, Object> getQualityTrends() {
        return qualityTrends;
    }

    public void setQualityTrends(Map<String, Object> qualityTrends) {
        this.qualityTrends = qualityTrends;
    }

    public Map<String, Object> getWorkflowStatus() {
        return workflowStatus;
    }

    public void setWorkflowStatus(Map<String, Object> workflowStatus) {
        this.workflowStatus = workflowStatus;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getPlantCode() {
        return plantCode;
    }

    public void setPlantCode(String plantCode) {
        this.plantCode = plantCode;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }
}