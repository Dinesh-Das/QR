package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.dto.WorkflowSummaryDto;
import com.cqs.qrmfg.model.WorkflowState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class DashboardRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * Get workflow counts by state
     * @return Map of state name to count
     */
    public Map<String, Long> getWorkflowCountsByState() {
        String sql = "SELECT workflow_state, COUNT(*) as count FROM QRMFG_WORKFLOWS GROUP BY workflow_state";
        
        Map<String, Long> counts = new HashMap<>();
        jdbcTemplate.query(sql, (rs, rowNum) -> {
            counts.put(rs.getString("workflow_state"), rs.getLong("count"));
            return null;
        });
        
        return counts;
    }
    
    /**
     * Get query counts by team
     * @return Map of team name to count
     */
    public Map<String, Long> getQueryCountsByTeam() {
        String sql = "SELECT assigned_team, COUNT(*) as count FROM qrmfg_queries WHERE query_status = 'OPEN' GROUP BY assigned_team";
        
        Map<String, Long> counts = new HashMap<>();
        jdbcTemplate.query(sql, (rs, rowNum) -> {
            counts.put(rs.getString("assigned_team"), rs.getLong("count"));
            return null;
        });
        
        return counts;
    }
    
    /**
     * Get overdue workflows
     * @param dayThreshold Number of days after which a workflow is considered overdue
     * @return List of overdue workflow summaries
     */
    public List<WorkflowSummaryDto> getOverdueWorkflows(int dayThreshold) {
        String sql = "SELECT w.id, w.material_code, w.material_name, w.workflow_state, w.assigned_plant, w.initiated_by, " +
                     "w.created_at, w.last_modified, w.extended_at, w.completed_at, " +
                     "CASE " +
                     "    WHEN w.workflow_state = 'JVC_PENDING' THEN TRUNC(EXTRACT(DAY FROM (SYSDATE - w.created_at))) " +
                     "    WHEN w.workflow_state = 'PLANT_PENDING' THEN TRUNC(EXTRACT(DAY FROM (SYSDATE - NVL(w.extended_at, w.created_at)))) " +
                     "    WHEN w.workflow_state IN ('CQS_PENDING', 'TECH_PENDING') THEN TRUNC(EXTRACT(DAY FROM (SYSDATE - w.last_modified))) " +
                     "    ELSE 0 " +
                     "END as days_pending, " +
                     "(SELECT COUNT(*) FROM qrmfg_queries q WHERE q.workflow_id = w.id) as total_queries, " +
                     "(SELECT COUNT(*) FROM qrmfg_queries q WHERE q.workflow_id = w.id AND q.query_status = 'OPEN') as open_queries " +
                     "FROM QRMFG_WORKFLOWS w " +
                     "WHERE w.workflow_state != 'COMPLETED' " +
                     "AND ( " +
                     "    (w.workflow_state = 'JVC_PENDING' AND w.created_at < (SYSDATE - INTERVAL ? DAY)) " +
                     "    OR (w.workflow_state = 'PLANT_PENDING' AND NVL(w.extended_at, w.created_at) < (SYSDATE - INTERVAL ? DAY)) " +
                     "    OR (w.workflow_state IN ('CQS_PENDING', 'TECH_PENDING') AND w.last_modified < (SYSDATE - INTERVAL ? DAY)) " +
                     ") " +
                     "ORDER BY days_pending DESC";
        
        return jdbcTemplate.query(sql, 
            new Object[]{dayThreshold, dayThreshold, dayThreshold}, 
            (rs, rowNum) -> mapWorkflowSummary(rs));
    }
    
    /**
     * Get workflows with open queries
     * @return List of workflow summaries with open queries
     */
    public List<WorkflowSummaryDto> getWorkflowsWithOpenQueries() {
        String sql = "SELECT w.id, w.material_code, w.material_name, w.workflow_state, w.assigned_plant, w.initiated_by, " +
                     "w.created_at, w.last_modified, w.extended_at, w.completed_at, " +
                     "CASE " +
                     "    WHEN w.workflow_state = 'JVC_PENDING' THEN TRUNC(EXTRACT(DAY FROM (SYSDATE - w.created_at))) " +
                     "    WHEN w.workflow_state = 'PLANT_PENDING' THEN TRUNC(EXTRACT(DAY FROM (SYSDATE - NVL(w.extended_at, w.created_at)))) " +
                     "    WHEN w.workflow_state IN ('CQS_PENDING', 'TECH_PENDING') THEN TRUNC(EXTRACT(DAY FROM (SYSDATE - w.last_modified))) " +
                     "    ELSE 0 " +
                     "END as days_pending, " +
                     "(SELECT COUNT(*) FROM qrmfg_queries q WHERE q.workflow_id = w.id) as total_queries, " +
                     "(SELECT COUNT(*) FROM qrmfg_queries q WHERE q.workflow_id = w.id AND q.query_status = 'OPEN') as open_queries " +
                     "FROM QRMFG_WORKFLOWS w " +
                     "WHERE EXISTS (SELECT 1 FROM qrmfg_queries q WHERE q.workflow_id = w.id AND q.query_status = 'OPEN') " +
                     "ORDER BY open_queries DESC";
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapWorkflowSummary(rs));
    }
    
    /**
     * Get recent activity across all workflows
     * @param days Number of days to look back
     * @return List of workflow summaries with recent activity
     */
    public List<WorkflowSummaryDto> getRecentActivity(int days) {
        String sql = "SELECT w.id, w.material_code, w.material_name, w.workflow_state, w.assigned_plant, w.initiated_by, " +
                     "w.created_at, w.last_modified, w.extended_at, w.completed_at, " +
                     "CASE " +
                     "    WHEN w.workflow_state = 'JVC_PENDING' THEN TRUNC(EXTRACT(DAY FROM (SYSDATE - w.created_at))) " +
                     "    WHEN w.workflow_state = 'PLANT_PENDING' THEN TRUNC(EXTRACT(DAY FROM (SYSDATE - NVL(w.extended_at, w.created_at)))) " +
                     "    WHEN w.workflow_state IN ('CQS_PENDING', 'TECH_PENDING') THEN TRUNC(EXTRACT(DAY FROM (SYSDATE - w.last_modified))) " +
                     "    ELSE 0 " +
                     "END as days_pending, " +
                     "(SELECT COUNT(*) FROM qrmfg_queries q WHERE q.workflow_id = w.id) as total_queries, " +
                     "(SELECT COUNT(*) FROM qrmfg_queries q WHERE q.workflow_id = w.id AND q.query_status = 'OPEN') as open_queries " +
                     "FROM QRMFG_WORKFLOWS w " +
                     "WHERE w.last_modified >= (SYSDATE - INTERVAL ? DAY) " +
                     "ORDER BY w.last_modified DESC";
        
        return jdbcTemplate.query(sql, new Object[]{days}, (rs, rowNum) -> mapWorkflowSummary(rs));
    }
    
    /**
     * Get workflows by plant with status summary
     * @param plantName Name of the plant
     * @return List of workflow summaries for the plant
     */
    public List<WorkflowSummaryDto> getWorkflowsByPlant(String plantName) {
        String sql = "SELECT w.id, w.material_code, w.material_name, w.workflow_state, w.assigned_plant, w.initiated_by, " +
                     "w.created_at, w.last_modified, w.extended_at, w.completed_at, " +
                     "CASE " +
                     "    WHEN w.workflow_state = 'JVC_PENDING' THEN TRUNC(EXTRACT(DAY FROM (SYSDATE - w.created_at))) " +
                     "    WHEN w.workflow_state = 'PLANT_PENDING' THEN TRUNC(EXTRACT(DAY FROM (SYSDATE - NVL(w.extended_at, w.created_at)))) " +
                     "    WHEN w.workflow_state IN ('CQS_PENDING', 'TECH_PENDING') THEN TRUNC(EXTRACT(DAY FROM (SYSDATE - w.last_modified))) " +
                     "    ELSE 0 " +
                     "END as days_pending, " +
                     "(SELECT COUNT(*) FROM qrmfg_queries q WHERE q.workflow_id = w.id) as total_queries, " +
                     "(SELECT COUNT(*) FROM qrmfg_queries q WHERE q.workflow_id = w.id AND q.query_status = 'OPEN') as open_queries " +
                     "FROM QRMFG_WORKFLOWS w " +
                     "WHERE w.assigned_plant = ? " +
                     "ORDER BY w.last_modified DESC";
        
        return jdbcTemplate.query(sql, new Object[]{plantName}, (rs, rowNum) -> mapWorkflowSummary(rs));
    }
    
    /**
     * Get dashboard summary statistics
     * @return Map of statistic name to value
     */
    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        try {
            // Total workflows - use safe query first
            Integer totalWorkflows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM QRMFG_WORKFLOWS", Integer.class);
            summary.put("totalWorkflows", totalWorkflows != null ? totalWorkflows : 0);
            
            // Active workflows
            Integer activeWorkflows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM QRMFG_WORKFLOWS WHERE workflow_state != 'COMPLETED'", Integer.class);
            summary.put("activeWorkflows", activeWorkflows != null ? activeWorkflows : 0);
            
            // Completed workflows
            Integer completedWorkflows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM QRMFG_WORKFLOWS WHERE workflow_state = 'COMPLETED'", Integer.class);
            summary.put("completedWorkflows", completedWorkflows != null ? completedWorkflows : 0);
            
        } catch (Exception e) {
            System.err.println("Error querying workflow counts: " + e.getMessage());
            summary.put("totalWorkflows", 0);
            summary.put("activeWorkflows", 0);
            summary.put("completedWorkflows", 0);
        }
        
        try {
            // Overdue workflows - fixed Oracle date arithmetic
            Integer overdueWorkflows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM QRMFG_WORKFLOWS w " +
                "WHERE w.workflow_state != 'COMPLETED' " +
                "AND w.created_at < (SYSDATE - INTERVAL '3' DAY)", Integer.class);
            summary.put("overdueWorkflows", overdueWorkflows != null ? overdueWorkflows : 0);
        } catch (Exception e) {
            System.err.println("Error querying overdue workflows: " + e.getMessage());
            summary.put("overdueWorkflows", 0);
        }
        
        try {
            // Query statistics
            Integer totalQueries = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM qrmfg_queries", Integer.class);
            summary.put("totalQueries", totalQueries != null ? totalQueries : 0);
            
            Integer openQueries = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM qrmfg_queries WHERE query_status = 'OPEN'", Integer.class);
            summary.put("openQueries", openQueries != null ? openQueries : 0);
            
            Integer resolvedQueries = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM qrmfg_queries WHERE query_status = 'RESOLVED'", Integer.class);
            summary.put("resolvedQueries", resolvedQueries != null ? resolvedQueries : 0);
            
        } catch (Exception e) {
            System.err.println("Error querying query statistics: " + e.getMessage());
            summary.put("totalQueries", 0);
            summary.put("openQueries", 0);
            summary.put("resolvedQueries", 0);
        }
        
        try {
            // Recent activity - fixed Oracle date arithmetic
            Integer recentWorkflows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM QRMFG_WORKFLOWS WHERE created_at >= (SYSDATE - INTERVAL '7' DAY)", Integer.class);
            summary.put("recentWorkflows", recentWorkflows != null ? recentWorkflows : 0);
            
            Integer recentCompletions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM QRMFG_WORKFLOWS " +
                "WHERE workflow_state = 'COMPLETED' " +
                "AND completed_at >= (SYSDATE - INTERVAL '7' DAY)", Integer.class);
            summary.put("recentCompletions", recentCompletions != null ? recentCompletions : 0);
            
            // Completed today
            Integer completedToday = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM QRMFG_WORKFLOWS " +
                "WHERE workflow_state = 'COMPLETED' " +
                "AND TRUNC(completed_at) = TRUNC(SYSDATE)", Integer.class);
            summary.put("completedToday", completedToday != null ? completedToday : 0);
            
        } catch (Exception e) {
            System.err.println("Error querying recent activity: " + e.getMessage());
            summary.put("recentWorkflows", 0);
            summary.put("recentCompletions", 0);
            summary.put("completedToday", 0);
        }
        
        // Default values for complex calculations
        summary.put("avgResolutionTimeHours", 0.0);
        
        return summary;
    }
    
    /**
     * Test database connection
     */
    public Integer testConnection() {
        return jdbcTemplate.queryForObject("SELECT 1 FROM DUAL", Integer.class);
    }
    
    /**
     * Check if required tables exist
     */
    public Map<String, Boolean> checkTablesExist() {
        Map<String, Boolean> tables = new HashMap<>();
        
        try {
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM QRMFG_WORKFLOWS WHERE ROWNUM <= 1", Integer.class);
            tables.put("QRMFG_WORKFLOWS", true);
        } catch (Exception e) {
            tables.put("QRMFG_WORKFLOWS", false);
        }
        
        try {
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM qrmfg_queries WHERE ROWNUM <= 1", Integer.class);
            tables.put("qrmfg_queries", true);
        } catch (Exception e) {
            tables.put("qrmfg_queries", false);
        }
        
        return tables;
    }
    
    // Helper method to map ResultSet to WorkflowSummaryDto
    private WorkflowSummaryDto mapWorkflowSummary(ResultSet rs) throws SQLException {
        WorkflowSummaryDto dto = new WorkflowSummaryDto();
        dto.setId(rs.getLong("id"));
        dto.setMaterialCode(rs.getString("material_code"));
        dto.setMaterialName(rs.getString("material_name"));
        dto.setCurrentState(WorkflowState.valueOf(rs.getString("workflow_state")));
        dto.setAssignedPlant(rs.getString("assigned_plant"));
        dto.setInitiatedBy(rs.getString("initiated_by"));
        dto.setDaysPending(rs.getInt("days_pending"));
        dto.setTotalQueries(rs.getLong("total_queries"));
        dto.setOpenQueries(rs.getLong("open_queries"));
        dto.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        dto.setLastModified(rs.getTimestamp("last_modified").toLocalDateTime());
        dto.setOverdue(dto.getDaysPending() > 3); // Business rule: overdue after 3 days
        return dto;
    }
}