package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.AuditLog;
import com.cqs.qrmfg.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    List<AuditLog> findByUser(User user);
    List<AuditLog> findByAction(String action);
    List<AuditLog> findByEntityType(String entityType);
    List<AuditLog> findBySeverity(String severity);
    List<AuditLog> findByEventTimeBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT new map(" +
           "u.username as username, " +
           "COUNT(CASE WHEN a.action = 'CREATE_WORKFLOW' THEN 1 END) as workflowsCreated, " +
           "COUNT(CASE WHEN a.action = 'RESOLVE_QUERY' THEN 1 END) as queriesResolved, " +
           "COUNT(CASE WHEN a.action = 'RAISE_QUERY' THEN 1 END) as queriesRaised, " +
           "COUNT(CASE WHEN a.action = 'COMPLETE_WORKFLOW' THEN 1 END) as workflowsCompleted, " +
           "COUNT(a) as totalActions, " +
           "MAX(a.eventTime) as lastActivity) " +
           "FROM User u LEFT JOIN AuditLog a ON u.username = a.user.username AND a.eventTime >= :startDate " +
           "GROUP BY u.username " +
           "ORDER BY COUNT(a) DESC")
    List<Map<String, Object>> getUserActivitySummary(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT new map(" +
           "a.eventTime as timestamp, " +
           "a.user.username as user, " +
           "a.action as action, " +
           "a.entityType as entityType, " +
           "a.entityId as entityId, " +
           "a.details as details) " +
           "FROM AuditLog a " +
           "WHERE (:startDate IS NULL OR a.eventTime >= :startDate) " +
           "AND (:endDate IS NULL OR a.eventTime <= :endDate) " +
           "AND (:entityType IS NULL OR :entityType = '' OR a.entityType = :entityType) " +
           "AND (:action IS NULL OR :action = '' OR a.action = :action) " +
           "ORDER BY a.eventTime DESC")
    List<Map<String, Object>> getFilteredAuditLogs(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate,
                                                   @Param("entityType") String entityType,
                                                   @Param("action") String action);
} 