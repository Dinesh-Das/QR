package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service for audit log operations
 */
public interface AuditLogService {

    /**
     * Get user activity summary
     * @param startDate Start date for filtering
     * @return List of user activity data
     */
    List<Map<String, Object>> getUserActivitySummary(LocalDateTime startDate);

    /**
     * Get filtered audit logs
     * @param startDate Optional start date for filtering
     * @param endDate Optional end date for filtering
     * @param entityType Optional entity type for filtering
     * @param action Optional action for filtering
     * @return List of audit log entries
     */
    List<Map<String, Object>> getFilteredAuditLogs(LocalDateTime startDate, LocalDateTime endDate, String entityType, String action);

    /**
     * Log an audit event
     * @param user User performing the action
     * @param action Action performed
     * @param entityType Type of entity affected
     * @param entityId ID of entity affected
     * @param details Additional details
     */
    void logAuditEvent(String user, String action, String entityType, String entityId, String details);

    // Add this method
    List<AuditLog> findAll();
    List<AuditLog> findByUserId(Long userId);
    void save(AuditLog log);
    List<AuditLog> findByAction(String action);
    List<AuditLog> findByEntityType(String entityType);
    List<AuditLog> findBySeverity(String severity);
    List<AuditLog> findByEventTimeBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
    AuditLog findById(Long id);
    void delete(Long id);
}