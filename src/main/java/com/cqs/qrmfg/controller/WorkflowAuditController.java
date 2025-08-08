package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.dto.AuditHistoryDto;
import com.cqs.qrmfg.service.WorkflowAuditService;
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

/**
 * REST controller for workflow audit operations
 */
@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('USER')")
public class WorkflowAuditController {

    @Autowired
    private WorkflowAuditService workflowAuditService;

    /**
     * Get complete audit history for a workflow
     */
    @GetMapping("/workflow/{workflowId}")
    public ResponseEntity<List<AuditHistoryDto>> getWorkflowAuditHistory(@PathVariable Long workflowId) {
        List<AuditHistoryDto> history = workflowAuditService.getWorkflowAuditHistory(workflowId);
        return ResponseEntity.ok(history);
    }

    /**
     * Get complete audit trail including all related entities
     */
    @GetMapping("/workflow/{workflowId}/complete")
    public ResponseEntity<List<AuditHistoryDto>> getCompleteWorkflowAuditTrail(@PathVariable Long workflowId) {
        List<AuditHistoryDto> trail = workflowAuditService.getCompleteWorkflowAuditTrail(workflowId);
        return ResponseEntity.ok(trail);
    }

    /**
     * Get audit history for a specific query
     */
    @GetMapping("/query/{queryId}")
    public ResponseEntity<List<AuditHistoryDto>> getQueryAuditHistory(@PathVariable Long queryId) {
        List<AuditHistoryDto> history = workflowAuditService.getQueryAuditHistory(queryId);
        return ResponseEntity.ok(history);
    }

    /**
     * Get audit history for questionnaire responses
     */
    @GetMapping("/response/{responseId}")
    public ResponseEntity<List<AuditHistoryDto>> getQuestionnaireResponseAuditHistory(@PathVariable Long responseId) {
        List<AuditHistoryDto> history = workflowAuditService.getQuestionnaireResponseAuditHistory(responseId);
        return ResponseEntity.ok(history);
    }

    /**
     * Get recent audit activity across all workflows
     */
    @GetMapping("/recent")
    public ResponseEntity<List<AuditHistoryDto>> getRecentAuditActivity(
            @RequestParam(defaultValue = "7") int days) {
        List<AuditHistoryDto> activity = workflowAuditService.getRecentAuditActivity(days);
        return ResponseEntity.ok(activity);
    }

    /**
     * Get audit activity by user
     */
    @GetMapping("/by-user/{username}")
    public ResponseEntity<List<AuditHistoryDto>> getAuditActivityByUser(@PathVariable String username) {
        List<AuditHistoryDto> activity = workflowAuditService.getAuditActivityByUser(username);
        return ResponseEntity.ok(activity);
    }

    /**
     * Get audit activity by entity type
     */
    @GetMapping("/by-entity/{entityType}")
    public ResponseEntity<List<AuditHistoryDto>> getAuditActivityByEntityType(
            @PathVariable String entityType,
            @RequestParam(defaultValue = "7") int days) {
        List<AuditHistoryDto> activity = workflowAuditService.getAuditActivityByEntityType(entityType, days);
        return ResponseEntity.ok(activity);
    }

    /**
     * Search audit logs with filters
     */
    @PostMapping("/search")
    public ResponseEntity<List<AuditHistoryDto>> searchAuditLogs(@RequestBody Map<String, Object> searchParams) {
        List<AuditHistoryDto> results = workflowAuditService.searchAuditLogs(searchParams);
        return ResponseEntity.ok(results);
    }

    /**
     * Export audit logs for a workflow
     */
    @GetMapping("/export/{workflowId}")
    public ResponseEntity<String> exportAuditLogs(
            @PathVariable Long workflowId,
            @RequestParam(defaultValue = "csv") String format) {
        
        String exportData = workflowAuditService.exportAuditLogs(workflowId, format);
        
        HttpHeaders headers = new HttpHeaders();
        if ("csv".equals(format)) {
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "workflow_" + workflowId + "_audit.csv");
        } else {
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", "workflow_" + workflowId + "_audit.json");
        }
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(exportData);
    }

    /**
     * Get version history for questionnaire responses
     */
    @GetMapping("/workflow/{workflowId}/response-versions")
    public ResponseEntity<List<AuditHistoryDto>> getQuestionnaireResponseVersions(@PathVariable Long workflowId) {
        List<AuditHistoryDto> versions = workflowAuditService.getQuestionnaireResponseVersions(workflowId);
        return ResponseEntity.ok(versions);
    }

    /**
     * Get read-only view of completed workflow
     */
    @GetMapping("/workflow/{workflowId}/readonly")
    public ResponseEntity<Map<String, Object>> getReadOnlyWorkflowView(@PathVariable Long workflowId) {
        Map<String, Object> readOnlyView = workflowAuditService.getReadOnlyWorkflowView(workflowId);
        return ResponseEntity.ok(readOnlyView);
    }
}