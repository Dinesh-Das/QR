package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.Query;
import com.cqs.qrmfg.model.QueryStatus;
import com.cqs.qrmfg.model.QueryTeam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QueryRepository extends JpaRepository<Query, Long> {
    
    // Basic finders
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.workflow.id = :workflowId ORDER BY q.createdAt DESC")
    List<Query> findByWorkflow_Id(@Param("workflowId") Long workflowId);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.assignedTeam = :assignedTeam AND q.status = :status ORDER BY q.createdAt DESC")
    List<Query> findByAssignedTeamAndStatus(@Param("assignedTeam") QueryTeam assignedTeam, @Param("status") QueryStatus status);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.status = :status AND q.assignedTeam = :assignedTeam ORDER BY q.createdAt DESC")
    List<Query> findByStatusAndAssignedTeam(@Param("status") QueryStatus status, @Param("assignedTeam") QueryTeam assignedTeam);
    
    Page<Query> findByAssignedTeamOrderByCreatedAtDesc(QueryTeam assignedTeam, Pageable pageable);
    
    // Query inbox functionality
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.assignedTeam = :team AND q.status = :status ORDER BY q.createdAt DESC")
    List<Query> findByTeamAndStatus(@Param("team") QueryTeam team, @Param("status") QueryStatus status);
    
    // SLA tracking
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.status = 'OPEN' AND q.createdAt < :cutoffTime")
    List<Query> findOverdueQueries(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Dashboard queries
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(q) FROM Query q WHERE q.assignedTeam = :team AND q.status = 'OPEN'")
    Long countOpenQueriesByTeam(@Param("team") QueryTeam team);
    
    @org.springframework.data.jpa.repository.Query("SELECT q.assignedTeam, COUNT(q) FROM Query q WHERE q.status = 'OPEN' GROUP BY q.assignedTeam")
    List<Object[]> getOpenQueryCountsByTeam();
    
    // Query resolution metrics
    @org.springframework.data.jpa.repository.Query("SELECT AVG(EXTRACT(DAY FROM (q.resolvedAt - q.createdAt)) * 24 + EXTRACT(HOUR FROM (q.resolvedAt - q.createdAt))) FROM Query q WHERE q.status = 'RESOLVED' AND q.assignedTeam = :team")
    Double getAverageResolutionTimeByTeam(@Param("team") QueryTeam team);
    
    // Search and filtering
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN q.workflow w WHERE " +
           "(:status IS NULL OR q.status = :status) AND " +
           "(:team IS NULL OR q.assignedTeam = :team) AND " +
           "(:projectCode IS NULL OR w.projectCode = :projectCode) AND " +
           "(:materialCode IS NULL OR w.materialCode = :materialCode) " +
           "ORDER BY q.createdAt DESC")
    List<Query> findQueriesWithFilters(
        @Param("status") QueryStatus status,
        @Param("team") QueryTeam team,
        @Param("projectCode") String projectCode,
        @Param("materialCode") String materialCode);
    
    // Team workload analysis
    @org.springframework.data.jpa.repository.Query(value = "SELECT assigned_team, COUNT(*) as openCount, AVG(EXTRACT(DAY FROM (CURRENT_TIMESTAMP - created_at)) * 24 + EXTRACT(HOUR FROM (CURRENT_TIMESTAMP - created_at))) as avgAgeHours FROM QRMFG_QUERIES WHERE query_status = 'OPEN' GROUP BY assigned_team", nativeQuery = true)
    List<Object[]> getTeamWorkloadMetrics();
    
    // Additional methods needed by services
    Long countDistinctWorkflow_IdByStatus(QueryStatus status);
    Long countByStatus(QueryStatus status);
    Long countByStatusAndCreatedAtBefore(QueryStatus status, LocalDateTime dateTime);
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.createdAt BETWEEN :start AND :end ORDER BY q.createdAt DESC")
    List<Query> findByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.createdAt > :dateTime ORDER BY q.createdAt DESC")
    List<Query> findByCreatedAtAfter(@Param("dateTime") LocalDateTime dateTime);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.createdAt < :dateTime ORDER BY q.createdAt DESC")
    List<Query> findByCreatedAtBefore(@Param("dateTime") LocalDateTime dateTime);
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.status = :status AND q.resolvedAt BETWEEN :start AND :end ORDER BY q.resolvedAt DESC")
    List<Query> findByStatusAndResolvedAtBetween(@Param("status") QueryStatus status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.status = :status AND q.resolvedAt > :dateTime ORDER BY q.resolvedAt DESC")
    List<Query> findByStatusAndResolvedAtAfter(@Param("status") QueryStatus status, @Param("dateTime") LocalDateTime dateTime);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.status = :status AND q.resolvedAt < :dateTime ORDER BY q.resolvedAt DESC")
    List<Query> findByStatusAndResolvedAtBefore(@Param("status") QueryStatus status, @Param("dateTime") LocalDateTime dateTime);
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.status = :status ORDER BY q.createdAt DESC")
    List<Query> findByStatus(@Param("status") QueryStatus status);
    
    @org.springframework.data.jpa.repository.Query("SELECT q.assignedTeam, COUNT(q) FROM Query q WHERE q.status = 'OPEN' GROUP BY q.assignedTeam")
    List<Object[]> countOpenQueriesByTeamGrouped();
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.raisedBy = :raisedBy ORDER BY q.createdAt DESC")
    List<Query> findByRaisedBy(@Param("raisedBy") String raisedBy);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.resolvedBy = :resolvedBy ORDER BY q.resolvedAt DESC")
    List<Query> findByResolvedBy(@Param("resolvedBy") String resolvedBy);
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.workflow.materialCode = :materialCode ORDER BY q.createdAt DESC")
    List<Query> findByWorkflow_MaterialCode(@Param("materialCode") String materialCode);
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.assignedTeam = :assignedTeam ORDER BY q.createdAt DESC")
    List<Query> findByAssignedTeam(@Param("assignedTeam") QueryTeam assignedTeam);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.assignedTeam = :team AND q.status = 'OPEN' ORDER BY q.createdAt DESC")
    List<Query> findTeamInboxQueries(@Param("team") QueryTeam team);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.assignedTeam = :team AND q.status = 'RESOLVED' ORDER BY q.resolvedAt DESC")
    List<Query> findTeamResolvedQueries(@Param("team") QueryTeam team);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.raisedBy = :username ORDER BY q.createdAt DESC")
    List<Query> findQueriesRaisedByUser(@Param("username") String username);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.resolvedBy = :username ORDER BY q.resolvedAt DESC")
    List<Query> findQueriesResolvedByUser(@Param("username") String username);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.workflow.id = :workflowId AND q.status = :status ORDER BY q.createdAt DESC")
    List<Query> findByWorkflow_IdAndStatus(@Param("workflowId") Long workflowId, @Param("status") QueryStatus status);
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.priorityLevel = :priorityLevel ORDER BY q.createdAt DESC")
    List<Query> findByPriorityLevel(@Param("priorityLevel") String priorityLevel);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.createdAt >= :dateTime ORDER BY q.createdAt DESC")
    List<Query> findRecentQueries(@Param("dateTime") LocalDateTime dateTime);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.resolvedAt BETWEEN :start AND :end ORDER BY q.resolvedAt DESC")
    List<Query> findByResolvedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.status = 'OPEN' AND q.createdAt < :cutoffTime")
    List<Query> findQueriesOverSLA(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @org.springframework.data.jpa.repository.Query("SELECT AVG(EXTRACT(DAY FROM (q.resolvedAt - q.createdAt)) * 24 + EXTRACT(HOUR FROM (q.resolvedAt - q.createdAt))) FROM Query q WHERE q.status = 'RESOLVED' AND q.assignedTeam = :team")
    Double getAverageResolutionTimeHours(@Param("team") QueryTeam team);
    
    @org.springframework.data.jpa.repository.Query("SELECT AVG(EXTRACT(DAY FROM (q.resolvedAt - q.createdAt)) * 24 + EXTRACT(HOUR FROM (q.resolvedAt - q.createdAt))) FROM Query q WHERE q.status = 'RESOLVED' AND q.assignedTeam = :team AND q.resolvedAt BETWEEN :start AND :end")
    Double getAverageResolutionTimeHours(@Param("team") QueryTeam team, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    Long countByAssignedTeamAndStatus(QueryTeam team, QueryStatus status);
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(q) FROM Query q WHERE q.status = 'OPEN' AND q.createdAt < :cutoffTime")
    Long countOverdueQueries(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(q) FROM Query q WHERE q.status = 'OPEN' AND q.createdAt < :cutoffTime AND q.assignedTeam = :team")
    Long countOverdueQueriesByTeam(@Param("team") QueryTeam team, @Param("cutoffTime") LocalDateTime cutoffTime);
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(q) FROM Query q WHERE TRUNC(q.createdAt) = TRUNC(CURRENT_DATE)")
    Long countQueriesCreatedToday();
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(q) FROM Query q WHERE TRUNC(q.resolvedAt) = TRUNC(CURRENT_DATE) AND q.status = 'RESOLVED'")
    Long countQueriesResolvedToday();
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(q) FROM Query q WHERE TRUNC(q.resolvedAt) = TRUNC(CURRENT_DATE) AND q.status = 'RESOLVED' AND q.assignedTeam = :team")
    Long countQueriesResolvedTodayByTeam(@Param("team") QueryTeam team);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.status = 'OPEN' ORDER BY q.createdAt ASC")
    List<Query> findPendingQueriesForDashboard();
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.priorityLevel = 'HIGH' AND q.status = 'OPEN' ORDER BY q.createdAt ASC")
    List<Query> findHighPriorityQueries();
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.status = 'OPEN' AND q.createdAt < :cutoffTime ORDER BY q.createdAt ASC")
    List<Query> findQueriesNeedingAttention(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.workflow.id = :workflowId AND q.status = 'OPEN'")
    List<Query> findOpenQueriesByWorkflow(@Param("workflowId") Long workflowId);
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(q) > 0 FROM Query q WHERE q.workflow.id = :workflowId AND q.status = 'OPEN'")
    Boolean hasWorkflowOpenQueries(@Param("workflowId") Long workflowId);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.id = :id")
    java.util.Optional<Query> findByIdWithWorkflow(@Param("id") Long id);
    
    // Role-based query history methods
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.workflow.id = :workflowId AND q.assignedTeam = :team AND q.status = :status ORDER BY q.resolvedAt DESC")
    List<Query> findByWorkflow_IdAndAssignedTeamAndStatus(@Param("workflowId") Long workflowId, @Param("team") QueryTeam team, @Param("status") QueryStatus status);
    
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q JOIN FETCH q.workflow WHERE q.workflow.materialCode = :materialCode AND q.assignedTeam = :team AND q.status = :status ORDER BY q.resolvedAt DESC")
    List<Query> findByWorkflow_MaterialCodeAndAssignedTeamAndStatus(@Param("materialCode") String materialCode, @Param("team") QueryTeam team, @Param("status") QueryStatus status);
}