package com.cqs.qrmfg.service;

import com.cqs.qrmfg.dto.AuditHistoryDto;

import java.util.List;
import java.util.Map;

/**
 * Service for workflow audit operations
 */
public interface WorkflowAuditService {

    /**
     * Get complete audit history for a workflow
     * @param workflowId The workflow ID
     * @return List of audit history entries
     */
    List<AuditHistoryDto> getWorkflowAuditHistory(Long workflowId);

    /**
     * Get complete audit trail including all related entities (queries, responses)
     * @param workflowId The workflow ID
     * @return Complete audit trail
     */
    List<AuditHistoryDto> getCompleteWorkflowAuditTrail(Long workflowId);

    /**
     * Get audit history for a specific query
     * @param queryId The query ID
     * @return List of audit history entries
     */
    List<AuditHistoryDto> getQueryAuditHistory(Long queryId);

    /**
     * Get audit history for questionnaire responses
     * @param responseId The response ID
     * @return List of audit history entries
     */
    List<AuditHistoryDto> getQuestionnaireResponseAuditHistory(Long responseId);

    /**
     * Get recent audit activity across all workflows
     * @param days Number of days to look back
     * @return List of recent audit activities
     */
    List<AuditHistoryDto> getRecentAuditActivity(int days);

    /**
     * Get audit activity by user
     * @param username The username
     * @return List of audit activities by user
     */
    List<AuditHistoryDto> getAuditActivityByUser(String username);

    /**
     * Get audit activity by entity type
     * @param entityType The entity type
     * @param days Number of days to look back
     * @return List of audit activities by entity type
     */
    List<AuditHistoryDto> getAuditActivityByEntityType(String entityType, int days);

    /**
     * Search audit logs with filters
     * @param searchParams Search parameters
     * @return List of matching audit entries
     */
    List<AuditHistoryDto> searchAuditLogs(Map<String, Object> searchParams);

    /**
     * Export audit logs for a workflow
     * @param workflowId The workflow ID
     * @param format Export format (csv, json)
     * @return Exported data as string
     */
    String exportAuditLogs(Long workflowId, String format);

    /**
     * Get version history for questionnaire responses
     * @param workflowId The workflow ID
     * @return List of response version history
     */
    List<AuditHistoryDto> getQuestionnaireResponseVersions(Long workflowId);

    /**
     * Get read-only view of completed workflow
     * @param workflowId The workflow ID
     * @return Read-only workflow data
     */
    Map<String, Object> getReadOnlyWorkflowView(Long workflowId);
}