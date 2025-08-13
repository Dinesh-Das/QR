package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.Query;
import com.cqs.qrmfg.model.QueryStatus;
import com.cqs.qrmfg.model.QueryTeam;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface QueryService {
    
    // Basic CRUD operations
    Query save(Query query);
    Query update(Query query);
    void delete(Long id);
    Optional<Query> findById(Long id);
    Optional<Query> findByIdWithWorkflow(Long id);
    List<Query> findAll();
    
    // Query creation
    Query createQuery(Long workflowId, String question, QueryTeam assignedTeam, String raisedBy);
    Query createQuery(Long workflowId, String question, Integer stepNumber, String fieldName, 
                     QueryTeam assignedTeam, String raisedBy);
    Query createQuery(Long workflowId, String question, Integer stepNumber, String fieldName, 
                     String stepTitle, QueryTeam assignedTeam, String raisedBy);
    Query createQuery(Long workflowId, String question, Integer stepNumber, String fieldName, 
                     String stepTitle, String originalQuestion, QueryTeam assignedTeam, String raisedBy);
    Query createQuery(String materialCode, String question, QueryTeam assignedTeam, String raisedBy);
    Query createQuery(String materialCode, String question, Integer stepNumber, String fieldName, 
                     QueryTeam assignedTeam, String raisedBy);
    Query createQuery(String materialCode, String question, Integer stepNumber, String fieldName, 
                     String stepTitle, QueryTeam assignedTeam, String raisedBy);
    Query createQuery(String materialCode, String question, Integer stepNumber, String fieldName, 
                     String stepTitle, String originalQuestion, QueryTeam assignedTeam, String raisedBy);
    
    // Query resolution
    Query resolveQuery(Long queryId, String response, String resolvedBy);
    Query resolveQuery(Long queryId, String response, String resolvedBy, String priorityLevel);
    void bulkResolveQueries(List<Long> queryIds, String response, String resolvedBy);
    
    // Query assignment and management
    Query assignToTeam(Long queryId, QueryTeam newTeam, String updatedBy);
    Query updatePriority(Long queryId, String priorityLevel, String updatedBy);
    Query updateCategory(Long queryId, String category, String updatedBy);
    
    // Query search and filtering
    List<Query> findByWorkflowId(Long workflowId);
    List<Query> findByMaterialCode(String materialCode);
    List<Query> findByStatus(QueryStatus status);
    List<Query> findByAssignedTeam(QueryTeam team);
    List<Query> findByRaisedBy(String username);
    List<Query> findByResolvedBy(String username);
    
    // Team-specific query inbox
    List<Query> findOpenQueriesForTeam(QueryTeam team);
    List<Query> findResolvedQueriesForTeam(QueryTeam team);
    List<Query> findQueriesForTeam(QueryTeam team, QueryStatus status);
    List<Query> findMyRaisedQueries(String username);
    List<Query> findMyResolvedQueries(String username);
    
    // Query filtering with criteria
    List<Query> findQueriesByWorkflowAndStatus(Long workflowId, QueryStatus status);
    List<Query> findQueriesByTeamAndStatus(QueryTeam team, QueryStatus status);
    List<Query> findQueriesByPriority(String priorityLevel);
    List<Query> findOverdueQueries();
    List<Query> findRecentQueries(int days);
    List<Query> findQueriesCreatedBetween(LocalDateTime start, LocalDateTime end);
    List<Query> findQueriesResolvedBetween(LocalDateTime start, LocalDateTime end);
    
    // SLA and metrics
    List<Query> findQueriesOverSLA(int slaHours);
    double getAverageResolutionTimeHours(QueryTeam team);
    double getAverageResolutionTimeHours(QueryTeam team, LocalDateTime start, LocalDateTime end);
    long countOpenQueriesByTeam(QueryTeam team);
    long countResolvedQueriesByTeam(QueryTeam team);
    long countOverdueQueries();
    long countOverdueQueriesByTeam(QueryTeam team);
    long countQueriesCreatedToday();
    long countQueriesResolvedToday();
    long countQueriesResolvedTodayByTeam(QueryTeam team);
    
    // Dashboard and reporting
    List<Query> findPendingQueriesForDashboard();
    List<Query> findHighPriorityQueries();
    List<Query> findQueriesNeedingAttention();
    
    // Validation and business rules
    void validateQueryCreation(Long workflowId, QueryTeam assignedTeam);
    void validateQueryResolution(Query query, String resolvedBy);
    boolean canUserResolveQuery(Query query, String username);
    boolean isQueryOverdue(Query query);
    boolean isQueryHighPriority(Query query);
    
    // Workflow integration
    void handleWorkflowStateChange(Long workflowId, String previousState, String newState);
    boolean hasWorkflowOpenQueries(Long workflowId);
    List<Query> searchQueriesWithContext(String materialCode, String projectCode, String plantCode, String team, String status, String priority, int minDaysOpen);
    
    // Role-based query history
    List<Query> findWorkflowQueriesResolvedByRole(Long workflowId, String role);
    List<Query> findMaterialQueriesResolvedByRole(String materialCode, String role);
    
    // Enhanced Query Status Management
    Query closeQuery(Long queryId, String reason, String closedBy);
    
    // Form Editing Permissions
    boolean canPlantEditForm(Long workflowId);
    Map<String, Object> getQueryStatusSummary(Long workflowId);
}