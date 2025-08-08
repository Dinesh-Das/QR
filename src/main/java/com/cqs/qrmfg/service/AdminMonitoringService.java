package com.cqs.qrmfg.service;

import com.cqs.qrmfg.dto.QuerySlaReportDto;
import com.cqs.qrmfg.dto.UserRoleAssignmentDto;
import com.cqs.qrmfg.dto.WorkflowMonitoringDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service for admin monitoring and reporting features
 */
public interface AdminMonitoringService {

    /**
     * Get workflow monitoring dashboard data
     * @return WorkflowMonitoringDto containing dashboard metrics
     */
    WorkflowMonitoringDto getWorkflowMonitoringDashboard();

    /**
     * Get workflow status distribution
     * @return Map of workflow states and their counts
     */
    Map<String, Long> getWorkflowStatusDistribution();

    /**
     * Get query SLA report with metrics on resolution times
     * @param startDate Optional start date for filtering
     * @param endDate Optional end date for filtering
     * @return QuerySlaReportDto with SLA metrics
     */
    QuerySlaReportDto getQuerySlaReport(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get average resolution times by team
     * @param startDate Optional start date for filtering
     * @param endDate Optional end date for filtering
     * @return Map of team names to average resolution times in hours
     */
    Map<String, Double> getAverageResolutionTimesByTeam(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get workflow bottlenecks analysis
     * @return Map containing bottleneck analysis data
     */
    Map<String, Object> getWorkflowBottlenecks();

    /**
     * Get user activity summary
     * @param days Number of days to include in the summary
     * @return List of user activity data
     */
    List<Map<String, Object>> getUserActivitySummary(int days);

    /**
     * Get workflow performance metrics
     * @param startDate Optional start date for filtering
     * @param endDate Optional end date for filtering
     * @return Map containing performance metrics
     */
    Map<String, Object> getWorkflowPerformanceMetrics(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Export audit logs as CSV
     * @param startDate Optional start date for filtering
     * @param endDate Optional end date for filtering
     * @param entityType Optional entity type for filtering
     * @param action Optional action for filtering
     * @return byte array containing CSV data
     */
    byte[] exportAuditLogsAsCsv(LocalDateTime startDate, LocalDateTime endDate, String entityType, String action);

    /**
     * Export workflow report as CSV
     * @param startDate Optional start date for filtering
     * @param endDate Optional end date for filtering
     * @param state Optional workflow state for filtering
     * @return byte array containing CSV data
     */
    byte[] exportWorkflowReportAsCsv(LocalDateTime startDate, LocalDateTime endDate, String state);

    /**
     * Get user role assignments for management
     * @return List of UserRoleAssignmentDto objects
     */
    List<UserRoleAssignmentDto> getUserRoleAssignments();

    /**
     * Update user roles
     * @param userId User ID
     * @param roleIds List of role IDs to assign
     * @return Updated UserRoleAssignmentDto
     */
    UserRoleAssignmentDto updateUserRoles(Long userId, List<Long> roleIds);
}