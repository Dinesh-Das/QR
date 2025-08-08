package com.cqs.qrmfg.dto;

import java.util.Map;

public class QuerySlaReportDto {
    private Map<String, Double> averageResolutionTimesByTeam;
    private Map<String, Long> totalQueriesByTeam;
    private Map<String, Long> resolvedQueriesByTeam;
    private Map<String, Long> overdueQueriesByTeam;
    private Map<String, Double> slaComplianceByTeam;
    private double overallAverageResolutionTime;
    private long totalQueries;
    private long totalResolvedQueries;
    private long totalOverdueQueries;
    private double overallSlaCompliance;

    // Constructors
    public QuerySlaReportDto() {}

    public QuerySlaReportDto(Map<String, Double> averageResolutionTimesByTeam,
                           Map<String, Long> totalQueriesByTeam,
                           Map<String, Long> resolvedQueriesByTeam,
                           Map<String, Long> overdueQueriesByTeam,
                           Map<String, Double> slaComplianceByTeam,
                           double overallAverageResolutionTime,
                           long totalQueries,
                           long totalResolvedQueries,
                           long totalOverdueQueries,
                           double overallSlaCompliance) {
        this.averageResolutionTimesByTeam = averageResolutionTimesByTeam;
        this.totalQueriesByTeam = totalQueriesByTeam;
        this.resolvedQueriesByTeam = resolvedQueriesByTeam;
        this.overdueQueriesByTeam = overdueQueriesByTeam;
        this.slaComplianceByTeam = slaComplianceByTeam;
        this.overallAverageResolutionTime = overallAverageResolutionTime;
        this.totalQueries = totalQueries;
        this.totalResolvedQueries = totalResolvedQueries;
        this.totalOverdueQueries = totalOverdueQueries;
        this.overallSlaCompliance = overallSlaCompliance;
    }

    // Getters and Setters
    public Map<String, Double> getAverageResolutionTimesByTeam() {
        return averageResolutionTimesByTeam;
    }

    public void setAverageResolutionTimesByTeam(Map<String, Double> averageResolutionTimesByTeam) {
        this.averageResolutionTimesByTeam = averageResolutionTimesByTeam;
    }

    public Map<String, Long> getTotalQueriesByTeam() {
        return totalQueriesByTeam;
    }

    public void setTotalQueriesByTeam(Map<String, Long> totalQueriesByTeam) {
        this.totalQueriesByTeam = totalQueriesByTeam;
    }

    public Map<String, Long> getResolvedQueriesByTeam() {
        return resolvedQueriesByTeam;
    }

    public void setResolvedQueriesByTeam(Map<String, Long> resolvedQueriesByTeam) {
        this.resolvedQueriesByTeam = resolvedQueriesByTeam;
    }

    public Map<String, Long> getOverdueQueriesByTeam() {
        return overdueQueriesByTeam;
    }

    public void setOverdueQueriesByTeam(Map<String, Long> overdueQueriesByTeam) {
        this.overdueQueriesByTeam = overdueQueriesByTeam;
    }

    public Map<String, Double> getSlaComplianceByTeam() {
        return slaComplianceByTeam;
    }

    public void setSlaComplianceByTeam(Map<String, Double> slaComplianceByTeam) {
        this.slaComplianceByTeam = slaComplianceByTeam;
    }

    public double getOverallAverageResolutionTime() {
        return overallAverageResolutionTime;
    }

    public void setOverallAverageResolutionTime(double overallAverageResolutionTime) {
        this.overallAverageResolutionTime = overallAverageResolutionTime;
    }

    public long getTotalQueries() {
        return totalQueries;
    }

    public void setTotalQueries(long totalQueries) {
        this.totalQueries = totalQueries;
    }

    public long getTotalResolvedQueries() {
        return totalResolvedQueries;
    }

    public void setTotalResolvedQueries(long totalResolvedQueries) {
        this.totalResolvedQueries = totalResolvedQueries;
    }

    public long getTotalOverdueQueries() {
        return totalOverdueQueries;
    }

    public void setTotalOverdueQueries(long totalOverdueQueries) {
        this.totalOverdueQueries = totalOverdueQueries;
    }

    public double getOverallSlaCompliance() {
        return overallSlaCompliance;
    }

    public void setOverallSlaCompliance(double overallSlaCompliance) {
        this.overallSlaCompliance = overallSlaCompliance;
    }
}