package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.service.AuditLogService;
import com.cqs.qrmfg.repository.AuditLogRepository;
import com.cqs.qrmfg.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.cqs.qrmfg.model.AuditLog;
import com.cqs.qrmfg.model.User;

@Service
@Transactional
public class AuditLogServiceImpl implements AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Override
    public List<Map<String, Object>> getUserActivitySummary(LocalDateTime startDate) {
        try {
            return auditLogRepository.getUserActivitySummary(startDate);
        } catch (Exception e) {
            // If there's an error (e.g., table doesn't exist), return empty list
            System.err.println("Warning: Could not get user activity summary: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Map<String, Object>> getFilteredAuditLogs(LocalDateTime startDate, LocalDateTime endDate, String entityType, String action) {
        try {
            return auditLogRepository.getFilteredAuditLogs(startDate, endDate, entityType, action);
        } catch (Exception e) {
            // If there's an error (e.g., table doesn't exist), return empty list
            System.err.println("Warning: Could not get filtered audit logs: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public void logAuditEvent(String username, String action, String entityType, String entityId, String details) {
        try {
            AuditLog auditLog = new AuditLog();
            
            // Find user by username
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                auditLog.setUser(userOpt.get());
            }
            
            auditLog.setAction(action);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setDetails(details);
            auditLog.setEventTime(LocalDateTime.now());
            
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            System.err.println("Warning: Could not log audit event: " + e.getMessage());
        }
    }

    @Override
    public List<AuditLog> findAll() {
        try {
            return auditLogRepository.findAll();
        } catch (Exception e) {
            // If table doesn't exist or other database error, return empty list
            System.err.println("Warning: Could not query audit logs - table may not exist: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<AuditLog> findByUserId(Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                return new ArrayList<>();
            }
            return auditLogRepository.findByUser(userOpt.get());
        } catch (Exception e) {
            System.err.println("Warning: Could not find audit logs by user ID: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public void save(AuditLog log) {
        try {
            // Ensure event time is set
            if (log.getEventTime() == null) {
                log.setEventTime(LocalDateTime.now());
            }
            
            auditLogRepository.save(log);
        } catch (Exception e) {
            System.err.println("Warning: Could not save audit log: " + e.getMessage());
        }
    }

    @Override
    public List<AuditLog> findByAction(String action) {
        try {
            return auditLogRepository.findByAction(action);
        } catch (Exception e) {
            System.err.println("Warning: Could not find audit logs by action: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<AuditLog> findByEntityType(String entityType) {
        try {
            return auditLogRepository.findByEntityType(entityType);
        } catch (Exception e) {
            System.err.println("Warning: Could not find audit logs by entity type: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<AuditLog> findBySeverity(String severity) {
        try {
            return auditLogRepository.findBySeverity(severity);
        } catch (Exception e) {
            System.err.println("Warning: Could not find audit logs by severity: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<AuditLog> findByEventTimeBetween(LocalDateTime start, LocalDateTime end) {
        try {
            return auditLogRepository.findByEventTimeBetween(start, end);
        } catch (Exception e) {
            System.err.println("Warning: Could not find audit logs by event time: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public AuditLog findById(Long id) {
        try {
            Optional<AuditLog> auditLogOpt = auditLogRepository.findById(id);
            return auditLogOpt.orElse(null);
        } catch (Exception e) {
            System.err.println("Warning: Could not find audit log by ID: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void delete(Long id) {
        try {
            auditLogRepository.deleteById(id);
        } catch (Exception e) {
            System.err.println("Warning: Could not delete audit log: " + e.getMessage());
        }
    }
}