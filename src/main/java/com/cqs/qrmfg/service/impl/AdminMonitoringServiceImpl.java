package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.dto.QuerySlaReportDto;
import com.cqs.qrmfg.dto.UserRoleAssignmentDto;
import com.cqs.qrmfg.dto.WorkflowMonitoringDto;
import com.cqs.qrmfg.model.Workflow;
import com.cqs.qrmfg.model.Query;
import com.cqs.qrmfg.model.QueryStatus;
import com.cqs.qrmfg.model.WorkflowState;
import com.cqs.qrmfg.repository.DashboardRepository;
import com.cqs.qrmfg.repository.WorkflowRepository;
import com.cqs.qrmfg.repository.QueryRepository;
import com.cqs.qrmfg.service.AdminMonitoringService;
import com.cqs.qrmfg.service.AuditLogService;
import com.cqs.qrmfg.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminMonitoringServiceImpl implements AdminMonitoringService {

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private QueryRepository queryRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private UserService userService;

    @Override
    public WorkflowMonitoringDto getWorkflowMonitoringDashboard() {
        long totalWorkflows = workflowRepository.count();
        long activeWorkflows = workflowRepository.countByStateNot(WorkflowState.COMPLETED);
        long completedWorkflows = workflowRepository.countByState(WorkflowState.COMPLETED);
        
        // Calculate workflows with open queries
        long workflowsWithOpenQueries = queryRepository.countDistinctWorkflow_IdByStatus(QueryStatus.OPEN);
        
        // Calculate overdue workflows (more than 7 days old and not completed)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long overdueWorkflows = workflowRepository.countByStateNotAndCreatedAtBefore(
                WorkflowState.COMPLETED, sevenDaysAgo);
        
        // Get workflows by state
        Map<String, Long> workflowsByState = new HashMap<>();
        for (WorkflowState state : WorkflowState.values()) {
            workflowsByState.put(state.name(), workflowRepository.countByState(state));
        }
        
        // Get workflows by plant
        Map<String, Long> workflowsByPlant = new HashMap<>();
        List<Object[]> plantCounts = workflowRepository.countByPlantCodeGrouped();
        for (Object[] row : plantCounts) {
            workflowsByPlant.put((String) row[0], (Long) row[1]);
        }
        
        // Get recent activity (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        Map<String, Long> recentActivity = new HashMap<>();
        List<Object[]> activityCounts = workflowRepository.countByCreatedAtAfterGroupByDay(thirtyDaysAgo);
        for (Object[] row : activityCounts) {
            recentActivity.put(row[0].toString(), (Long) row[1]);
        }
        
        // Calculate average completion time in hours
        Double avgCompletionTime = workflowRepository.calculateAverageCompletionTimeHours();
        double averageCompletionTimeHours = avgCompletionTime != null ? avgCompletionTime : 0.0;
        
        // Query statistics
        long totalQueries = queryRepository.count();
        long openQueries = queryRepository.countByStatus(QueryStatus.OPEN);
        
        // Calculate overdue queries (more than 3 days old and not resolved)
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        long overdueQueries = queryRepository.countByStatusAndCreatedAtBefore(QueryStatus.OPEN, threeDaysAgo);
        
        return new WorkflowMonitoringDto(
                totalWorkflows,
                activeWorkflows,
                completedWorkflows,
                overdueWorkflows,
                workflowsWithOpenQueries,
                workflowsByState,
                workflowsByPlant,
                recentActivity,
                averageCompletionTimeHours,
                totalQueries,
                openQueries,
                overdueQueries
        );
    }

    @Override
    public Map<String, Long> getWorkflowStatusDistribution() {
        Map<String, Long> distribution = new HashMap<>();
        for (WorkflowState state : WorkflowState.values()) {
            distribution.put(state.name(), workflowRepository.countByState(state));
        }
        return distribution;
    }

    @Override
    public QuerySlaReportDto getQuerySlaReport(LocalDateTime startDate, LocalDateTime endDate) {
        // Apply date filters if provided
        List<Query> queries;
        if (startDate != null && endDate != null) {
            queries = queryRepository.findByCreatedAtBetween(startDate, endDate);
        } else if (startDate != null) {
            queries = queryRepository.findByCreatedAtAfter(startDate);
        } else if (endDate != null) {
            queries = queryRepository.findByCreatedAtBefore(endDate);
        } else {
            queries = queryRepository.findAll();
        }
        
        // Calculate metrics by team
        Map<String, List<Query>> queriesByTeam = queries.stream()
                .collect(Collectors.groupingBy(q -> q.getAssignedTeam().name()));
        
        Map<String, Double> averageResolutionTimesByTeam = new HashMap<>();
        Map<String, Long> totalQueriesByTeam = new HashMap<>();
        Map<String, Long> resolvedQueriesByTeam = new HashMap<>();
        Map<String, Long> overdueQueriesByTeam = new HashMap<>();
        Map<String, Double> slaComplianceByTeam = new HashMap<>();
        
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        
        for (Map.Entry<String, List<Query>> entry : queriesByTeam.entrySet()) {
            String team = entry.getKey();
            List<Query> teamQueries = entry.getValue();
            
            // Total queries for this team
            totalQueriesByTeam.put(team, (long) teamQueries.size());
            
            // Resolved queries for this team
            List<Query> resolvedQueries = teamQueries.stream()
                    .filter(q -> q.getStatus() == QueryStatus.RESOLVED)
                    .collect(Collectors.toList());
            resolvedQueriesByTeam.put(team, (long) resolvedQueries.size());
            
            // Overdue queries for this team
            long overdue = teamQueries.stream()
                    .filter(q -> q.getStatus() == QueryStatus.OPEN && q.getCreatedAt().isBefore(threeDaysAgo))
                    .count();
            overdueQueriesByTeam.put(team, overdue);
            
            // Average resolution time for this team
            if (!resolvedQueries.isEmpty()) {
                double avgHours = resolvedQueries.stream()
                        .mapToDouble(q -> {
                            if (q.getResolvedAt() != null) {
                                return Duration.between(q.getCreatedAt(), q.getResolvedAt()).toHours();
                            }
                            return 0;
                        })
                        .average()
                        .orElse(0);
                averageResolutionTimesByTeam.put(team, avgHours);
                
                // SLA compliance (% resolved within 3 days)
                long resolvedWithinSla = resolvedQueries.stream()
                        .filter(q -> {
                            if (q.getResolvedAt() != null) {
                                return Duration.between(q.getCreatedAt(), q.getResolvedAt()).toDays() <= 3;
                            }
                            return false;
                        })
                        .count();
                double compliance = (double) resolvedWithinSla / resolvedQueries.size() * 100;
                slaComplianceByTeam.put(team, compliance);
            } else {
                averageResolutionTimesByTeam.put(team, 0.0);
                slaComplianceByTeam.put(team, 0.0);
            }
        }
        
        // Calculate overall metrics
        double overallAverageResolutionTime = queries.stream()
                .filter(q -> q.getStatus() == QueryStatus.RESOLVED && q.getResolvedAt() != null)
                .mapToDouble(q -> Duration.between(q.getCreatedAt(), q.getResolvedAt()).toHours())
                .average()
                .orElse(0);
        
        long totalQueries = queries.size();
        long totalResolvedQueries = queries.stream()
                .filter(q -> q.getStatus() == QueryStatus.RESOLVED)
                .count();
        
        long totalOverdueQueries = queries.stream()
                .filter(q -> q.getStatus() == QueryStatus.OPEN && q.getCreatedAt().isBefore(threeDaysAgo))
                .count();
        
        double overallSlaCompliance = 0;
        if (totalResolvedQueries > 0) {
            long resolvedWithinSla = queries.stream()
                    .filter(q -> q.getStatus() == QueryStatus.RESOLVED && q.getResolvedAt() != null)
                    .filter(q -> Duration.between(q.getCreatedAt(), q.getResolvedAt()).toDays() <= 3)
                    .count();
            overallSlaCompliance = (double) resolvedWithinSla / totalResolvedQueries * 100;
        }
        
        return new QuerySlaReportDto(
                averageResolutionTimesByTeam,
                totalQueriesByTeam,
                resolvedQueriesByTeam,
                overdueQueriesByTeam,
                slaComplianceByTeam,
                overallAverageResolutionTime,
                totalQueries,
                totalResolvedQueries,
                totalOverdueQueries,
                overallSlaCompliance
        );
    }

    @Override
    public Map<String, Double> getAverageResolutionTimesByTeam(LocalDateTime startDate, LocalDateTime endDate) {
        List<Query> queries;
        if (startDate != null && endDate != null) {
            queries = queryRepository.findByStatusAndResolvedAtBetween(QueryStatus.RESOLVED, startDate, endDate);
        } else if (startDate != null) {
            queries = queryRepository.findByStatusAndResolvedAtAfter(QueryStatus.RESOLVED, startDate);
        } else if (endDate != null) {
            queries = queryRepository.findByStatusAndResolvedAtBefore(QueryStatus.RESOLVED, endDate);
        } else {
            queries = queryRepository.findByStatus(QueryStatus.RESOLVED);
        }
        
        return queries.stream()
                .filter(q -> q.getResolvedAt() != null)
                .collect(Collectors.groupingBy(
                        q -> q.getAssignedTeam().name(),
                        Collectors.averagingDouble(q -> 
                            Duration.between(q.getCreatedAt(), q.getResolvedAt()).toHours())
                ));
    }

    @Override
    public Map<String, Object> getWorkflowBottlenecks() {
        Map<String, Object> bottlenecks = new HashMap<>();
        
        // Average time spent in each state
        Map<String, Double> avgTimeInState = new HashMap<>();
        List<Object[]> timeInStateData = workflowRepository.calculateAverageTimeInEachStateGrouped();
        for (Object[] row : timeInStateData) {
            avgTimeInState.put(row[0].toString(), (Double) row[1]);
        }
        bottlenecks.put("averageTimeInState", avgTimeInState);
        
        // States with most overdue workflows
        Map<String, Long> overdueByState = new HashMap<>();
        List<Object[]> overdueStateData = workflowRepository.countOverdueWorkflowsByStateGrouped();
        for (Object[] row : overdueStateData) {
            overdueByState.put(row[0].toString(), (Long) row[1]);
        }
        bottlenecks.put("overdueByState", overdueByState);
        
        // Teams with most open queries
        Map<String, Long> openQueriesByTeam = new HashMap<>();
        List<Object[]> openQueryData = queryRepository.countOpenQueriesByTeamGrouped();
        for (Object[] row : openQueryData) {
            openQueriesByTeam.put(row[0].toString(), (Long) row[1]);
        }
        bottlenecks.put("openQueriesByTeam", openQueriesByTeam);
        
        // Plants with most delayed workflows
        Map<String, Long> delayedByPlant = new HashMap<>();
        List<Object[]> delayedPlantData = workflowRepository.countDelayedWorkflowsByPlantGrouped();
        for (Object[] row : delayedPlantData) {
            delayedByPlant.put((String) row[0], (Long) row[1]);
        }
        bottlenecks.put("delayedByPlant", delayedByPlant);
        
        return bottlenecks;
    }

    @Override
    public List<Map<String, Object>> getUserActivitySummary(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        return auditLogService.getUserActivitySummary(startDate);
    }

    @Override
    public Map<String, Object> getWorkflowPerformanceMetrics(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> metrics = new HashMap<>();
        
        // Apply date filters
        List<Workflow> workflows;
        if (startDate != null && endDate != null) {
            workflows = workflowRepository.findByCreatedAtBetween(startDate, endDate);
        } else if (startDate != null) {
            workflows = workflowRepository.findByCreatedAtAfter(startDate);
        } else if (endDate != null) {
            workflows = workflowRepository.findByCreatedAtBefore(endDate);
        } else {
            workflows = workflowRepository.findAll();
        }
        
        // Calculate completion rate
        long totalWorkflows = workflows.size();
        long completedWorkflows = workflows.stream()
                .filter(w -> w.getState() == WorkflowState.COMPLETED)
                .count();
        double completionRate = totalWorkflows > 0 ? (double) completedWorkflows / totalWorkflows * 100 : 0;
        metrics.put("completionRate", completionRate);
        
        // Calculate average completion time
        double avgCompletionTime = workflows.stream()
                .filter(w -> w.getState() == WorkflowState.COMPLETED)
                .mapToDouble(w -> {
                    // Assuming lastModified is updated when workflow is completed
                    return Duration.between(w.getCreatedAt(), w.getLastModified()).toHours();
                })
                .average()
                .orElse(0);
        metrics.put("averageCompletionTimeHours", avgCompletionTime);
        
        // Calculate query rate (queries per workflow)
        double queryRate = workflows.stream()
                .mapToLong(w -> w.getQueries().size())
                .average()
                .orElse(0);
        metrics.put("queriesPerWorkflow", queryRate);
        
        // Calculate workflow throughput by month
        Map<String, Long> throughputByMonth = workflows.stream()
                .filter(w -> w.getState() == WorkflowState.COMPLETED)
                .collect(Collectors.groupingBy(
                        w -> w.getLastModified().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        Collectors.counting()
                ));
        metrics.put("throughputByMonth", throughputByMonth);
        
        return metrics;
    }

    @Override
    public byte[] exportAuditLogsAsCsv(LocalDateTime startDate, LocalDateTime endDate, String entityType, String action) {
        List<Map<String, Object>> auditLogs = auditLogService.getFilteredAuditLogs(startDate, endDate, entityType, action);
        
        // Convert to CSV
        StringBuilder csv = new StringBuilder();
        csv.append("Timestamp,User,Action,Entity Type,Entity ID,Details\n");
        
        for (Map<String, Object> log : auditLogs) {
            csv.append(formatCsvField(log.get("timestamp")))
               .append(",").append(formatCsvField(log.get("user")))
               .append(",").append(formatCsvField(log.get("action")))
               .append(",").append(formatCsvField(log.get("entityType")))
               .append(",").append(formatCsvField(log.get("entityId")))
               .append(",").append(formatCsvField(log.get("details")))
               .append("\n");
        }
        
        return csv.toString().getBytes();
    }

    @Override
    public byte[] exportWorkflowReportAsCsv(LocalDateTime startDate, LocalDateTime endDate, String state) {
        List<Workflow> workflows;
        
        // Apply filters
        if (startDate != null && endDate != null) {
            if (state != null) {
                WorkflowState workflowState = WorkflowState.valueOf(state);
                workflows = workflowRepository.findByStateAndCreatedAtBetween(workflowState, startDate, endDate);
            } else {
                workflows = workflowRepository.findByCreatedAtBetween(startDate, endDate);
            }
        } else if (state != null) {
            WorkflowState workflowState = WorkflowState.valueOf(state);
            workflows = workflowRepository.findByState(workflowState);
        } else {
            workflows = workflowRepository.findAll();
        }
        
        // Convert to CSV
        StringBuilder csv = new StringBuilder();
        csv.append("Material ID,State,Assigned Plant,Initiated By,Created At,Last Modified,Open Queries,Total Queries\n");
        
        for (Workflow workflow : workflows) {
            long openQueries = workflow.getQueries().stream()
                    .filter(q -> q.getStatus() == QueryStatus.OPEN)
                    .count();
            
            csv.append(formatCsvField(workflow.getMaterialCode()))
               .append(",").append(formatCsvField(workflow.getState().name()))
               .append(",").append(formatCsvField(workflow.getAssignedPlant()))
               .append(",").append(formatCsvField(workflow.getInitiatedBy()))
               .append(",").append(formatCsvField(workflow.getCreatedAt()))
               .append(",").append(formatCsvField(workflow.getLastModified()))
               .append(",").append(openQueries)
               .append(",").append(workflow.getQueries().size())
               .append("\n");
        }
        
        return csv.toString().getBytes();
    }

    @Override
    public List<UserRoleAssignmentDto> getUserRoleAssignments() {
        // Cast the List<Object> to List<UserRoleAssignmentDto>
        List<Object> assignments = userService.getAllUserRoleAssignments();
        List<UserRoleAssignmentDto> result = new ArrayList<>();
        for (Object obj : assignments) {
            if (obj instanceof UserRoleAssignmentDto) {
                result.add((UserRoleAssignmentDto) obj);
            }
        }
        return result;
    }

    @Override
    public UserRoleAssignmentDto updateUserRoles(Long userId, List<Long> roleIds) {
        userService.updateUserRoles(userId, roleIds);
        // Return a simple DTO with updated info
        UserRoleAssignmentDto dto = new UserRoleAssignmentDto();
        dto.setUserId(userId);
        return dto;
    }
    
    /**
     * Helper method to format CSV fields properly
     */
    private String formatCsvField(Object field) {
        if (field == null) {
            return "";
        }
        String value = field.toString();
        // Escape quotes and wrap in quotes if contains comma
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}