package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.dto.WorkflowMonitoringDto;
import com.cqs.qrmfg.dto.QuerySlaReportDto;
import com.cqs.qrmfg.dto.UserRoleAssignmentDto;
import com.cqs.qrmfg.service.AdminMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/monitoring")
@PreAuthorize("hasRole('ADMIN') or hasRole('TECH_USER')")
public class LegacyMonitoringController {

    @Autowired
    private AdminMonitoringService adminMonitoringService;

    /**
     * Get workflow monitoring dashboard data (Legacy)
     */
    @GetMapping("/dashboard")
    public ResponseEntity<WorkflowMonitoringDto> getWorkflowMonitoringDashboard() {
        WorkflowMonitoringDto dashboard = adminMonitoringService.getWorkflowMonitoringDashboard();
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Get workflow status distribution
     */
    @GetMapping("/workflow-status")
    public ResponseEntity<Map<String, Long>> getWorkflowStatusDistribution() {
        Map<String, Long> statusDistribution = adminMonitoringService.getWorkflowStatusDistribution();
        return ResponseEntity.ok(statusDistribution);
    }

    /**
     * Get query SLA reports
     */
    @GetMapping("/query-sla")
    public ResponseEntity<QuerySlaReportDto> getQuerySlaReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        QuerySlaReportDto report = adminMonitoringService.getQuerySlaReport(startDate, endDate);
        return ResponseEntity.ok(report);
    }

    /**
     * Get average resolution times by team
     */
    @GetMapping("/resolution-times")
    public ResponseEntity<Map<String, Double>> getAverageResolutionTimes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        Map<String, Double> resolutionTimes = adminMonitoringService.getAverageResolutionTimesByTeam(startDate, endDate);
        return ResponseEntity.ok(resolutionTimes);
    }

    /**
     * Get workflow bottlenecks analysis
     */
    @GetMapping("/bottlenecks")
    public ResponseEntity<Map<String, Object>> getWorkflowBottlenecks() {
        Map<String, Object> bottlenecks = adminMonitoringService.getWorkflowBottlenecks();
        return ResponseEntity.ok(bottlenecks);
    }

    /**
     * Get user activity summary
     */
    @GetMapping("/user-activity")
    public ResponseEntity<List<Map<String, Object>>> getUserActivitySummary(
            @RequestParam(defaultValue = "30") int days) {
        
        List<Map<String, Object>> userActivity = adminMonitoringService.getUserActivitySummary(days);
        return ResponseEntity.ok(userActivity);
    }

    /**
     * Get workflow performance metrics
     */
    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> getWorkflowPerformanceMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        Map<String, Object> metrics = adminMonitoringService.getWorkflowPerformanceMetrics(startDate, endDate);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Export audit logs as CSV
     */
    @GetMapping("/audit-logs/export")
    public ResponseEntity<byte[]> exportAuditLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action) {
        
        byte[] csvData = adminMonitoringService.exportAuditLogsAsCsv(startDate, endDate, entityType, action);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "audit-logs-" + System.currentTimeMillis() + ".csv");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
    }

    /**
     * Export workflow report as CSV
     */
    @GetMapping("/workflows/export")
    public ResponseEntity<byte[]> exportWorkflowReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String state) {
        
        byte[] csvData = adminMonitoringService.exportWorkflowReportAsCsv(startDate, endDate, state);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "workflow-report-" + System.currentTimeMillis() + ".csv");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
    }

    /**
     * Get user role assignments for management
     */
    @GetMapping("/user-roles")
    public ResponseEntity<List<UserRoleAssignmentDto>> getUserRoleAssignments() {
        List<UserRoleAssignmentDto> assignments = adminMonitoringService.getUserRoleAssignments();
        return ResponseEntity.ok(assignments);
    }

    /**
     * Update user role assignments
     */
    @PutMapping("/user-roles/{userId}")
    public ResponseEntity<UserRoleAssignmentDto> updateUserRoles(
            @PathVariable Long userId,
            @RequestBody List<Long> roleIds) {
        
        UserRoleAssignmentDto updated = adminMonitoringService.updateUserRoles(userId, roleIds);
        return ResponseEntity.ok(updated);
    }
}