package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.config.EnversConfig;
import com.cqs.qrmfg.dto.AuditHistoryDto;
import com.cqs.qrmfg.model.Workflow;
import com.cqs.qrmfg.model.Query;
import com.cqs.qrmfg.model.Answer;
import com.cqs.qrmfg.repository.WorkflowRepository;
import com.cqs.qrmfg.repository.QueryRepository;
import com.cqs.qrmfg.repository.AnswerRepository;
import com.cqs.qrmfg.service.WorkflowAuditService;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of WorkflowAuditService using Hibernate Envers
 */
@Service
@Transactional(readOnly = true)
public class WorkflowAuditServiceImpl implements WorkflowAuditService {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private QueryRepository queryRepository;

    @Autowired
    private AnswerRepository responseRepository;

    @Override
    public List<AuditHistoryDto> getWorkflowAuditHistory(Long workflowId) {
        AuditReader auditReader = AuditReaderFactory.get(entityManager);
        
        List<Object[]> revisions = auditReader.createQuery()
                .forRevisionsOfEntity(Workflow.class, false, true)
                .add(AuditEntity.id().eq(workflowId))
                .addOrder(AuditEntity.revisionNumber().desc())
                .getResultList();

        return revisions.stream()
                .map(this::mapWorkflowRevisionToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditHistoryDto> getCompleteWorkflowAuditTrail(Long workflowId) {
        List<AuditHistoryDto> auditTrail = new ArrayList<>();
        
        // Get workflow audit history
        auditTrail.addAll(getWorkflowAuditHistory(workflowId));
        
        // Get queries audit history
        Workflow workflow = workflowRepository.findById(workflowId).orElse(null);
        if (workflow != null) {
            for (Query query : workflow.getQueries()) {
                auditTrail.addAll(getQueryAuditHistory(query.getId()));
            }
            
            // Get questionnaire responses audit history
            for (Answer response : workflow.getResponses()) {
                auditTrail.addAll(getQuestionnaireResponseAuditHistory(response.getId()));
            }
        }
        
        // Sort by timestamp descending
        auditTrail.sort((a, b) -> {
            LocalDateTime timeA = a.getTimestamp() != null ? a.getTimestamp() : a.getRevisionDate();
            LocalDateTime timeB = b.getTimestamp() != null ? b.getTimestamp() : b.getRevisionDate();
            return timeB.compareTo(timeA);
        });
        
        return auditTrail;
    }

    @Override
    public List<AuditHistoryDto> getQueryAuditHistory(Long queryId) {
        AuditReader auditReader = AuditReaderFactory.get(entityManager);
        
        List<Object[]> revisions = auditReader.createQuery()
                .forRevisionsOfEntity(Query.class, false, true)
                .add(AuditEntity.id().eq(queryId))
                .addOrder(AuditEntity.revisionNumber().desc())
                .getResultList();

        return revisions.stream()
                .map(this::mapQueryRevisionToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditHistoryDto> getQuestionnaireResponseAuditHistory(Long responseId) {
        AuditReader auditReader = AuditReaderFactory.get(entityManager);
        
        List<Object[]> revisions = auditReader.createQuery()
                .forRevisionsOfEntity(Answer.class, false, true)
                .add(AuditEntity.id().eq(responseId))
                .addOrder(AuditEntity.revisionNumber().desc())
                .getResultList();

        return revisions.stream()
                .map(this::mapResponseRevisionToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditHistoryDto> getRecentAuditActivity(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        AuditReader auditReader = AuditReaderFactory.get(entityManager);
        
        List<AuditHistoryDto> recentActivity = new ArrayList<>();
        
        // Get recent workflow changes
        List<Object[]> workflowRevisions = auditReader.createQuery()
                .forRevisionsOfEntity(Workflow.class, false, true)
                .add(AuditEntity.revisionProperty("timestamp").ge(cutoffDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
                .addOrder(AuditEntity.revisionNumber().desc())
                .setMaxResults(100)
                .getResultList();
        
        recentActivity.addAll(workflowRevisions.stream()
                .map(this::mapWorkflowRevisionToDto)
                .collect(Collectors.toList()));
        
        // Get recent query changes
        List<Object[]> queryRevisions = auditReader.createQuery()
                .forRevisionsOfEntity(Query.class, false, true)
                .add(AuditEntity.revisionProperty("timestamp").ge(cutoffDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
                .addOrder(AuditEntity.revisionNumber().desc())
                .setMaxResults(100)
                .getResultList();
        
        recentActivity.addAll(queryRevisions.stream()
                .map(this::mapQueryRevisionToDto)
                .collect(Collectors.toList()));
        
        // Sort by timestamp descending
        recentActivity.sort((a, b) -> {
            LocalDateTime timeA = a.getTimestamp() != null ? a.getTimestamp() : a.getRevisionDate();
            LocalDateTime timeB = b.getTimestamp() != null ? b.getTimestamp() : b.getRevisionDate();
            return timeB.compareTo(timeA);
        });
        
        return recentActivity.stream().limit(50).collect(Collectors.toList());
    }

    @Override
    public List<AuditHistoryDto> getAuditActivityByUser(String username) {
        AuditReader auditReader = AuditReaderFactory.get(entityManager);
        
        List<AuditHistoryDto> userActivity = new ArrayList<>();
        
        // Get workflow changes by user
        List<Object[]> workflowRevisions = auditReader.createQuery()
                .forRevisionsOfEntity(Workflow.class, false, true)
                .add(AuditEntity.revisionProperty("username").eq(username))
                .addOrder(AuditEntity.revisionNumber().desc())
                .setMaxResults(100)
                .getResultList();
        
        userActivity.addAll(workflowRevisions.stream()
                .map(this::mapWorkflowRevisionToDto)
                .collect(Collectors.toList()));
        
        // Get query changes by user
        List<Object[]> queryRevisions = auditReader.createQuery()
                .forRevisionsOfEntity(Query.class, false, true)
                .add(AuditEntity.revisionProperty("username").eq(username))
                .addOrder(AuditEntity.revisionNumber().desc())
                .setMaxResults(100)
                .getResultList();
        
        userActivity.addAll(queryRevisions.stream()
                .map(this::mapQueryRevisionToDto)
                .collect(Collectors.toList()));
        
        // Sort by timestamp descending
        userActivity.sort((a, b) -> {
            LocalDateTime timeA = a.getTimestamp() != null ? a.getTimestamp() : a.getRevisionDate();
            LocalDateTime timeB = b.getTimestamp() != null ? b.getTimestamp() : b.getRevisionDate();
            return timeB.compareTo(timeA);
        });
        
        return userActivity;
    }

    @Override
    public List<AuditHistoryDto> getAuditActivityByEntityType(String entityType, int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        AuditReader auditReader = AuditReaderFactory.get(entityManager);
        
        List<AuditHistoryDto> entityActivity = new ArrayList<>();
        
        switch (entityType.toLowerCase()) {
            case "workflow":
            case "materialworkflow":
                List<Object[]> workflowRevisions = auditReader.createQuery()
                        .forRevisionsOfEntity(Workflow.class, false, true)
                        .add(AuditEntity.revisionProperty("timestamp").ge(cutoffDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
                        .addOrder(AuditEntity.revisionNumber().desc())
                        .getResultList();
                
                entityActivity.addAll(workflowRevisions.stream()
                        .map(this::mapWorkflowRevisionToDto)
                        .collect(Collectors.toList()));
                break;
                
            case "query":
                List<Object[]> queryRevisions = auditReader.createQuery()
                        .forRevisionsOfEntity(Query.class, false, true)
                        .add(AuditEntity.revisionProperty("timestamp").ge(cutoffDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
                        .addOrder(AuditEntity.revisionNumber().desc())
                        .getResultList();
                
                entityActivity.addAll(queryRevisions.stream()
                        .map(this::mapQueryRevisionToDto)
                        .collect(Collectors.toList()));
                break;
                
            case "response":
            case "questionnaireresponse":
                List<Object[]> responseRevisions = auditReader.createQuery()
                        .forRevisionsOfEntity(Answer.class, false, true)
                        .add(AuditEntity.revisionProperty("timestamp").ge(cutoffDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
                        .addOrder(AuditEntity.revisionNumber().desc())
                        .getResultList();
                
                entityActivity.addAll(responseRevisions.stream()
                        .map(this::mapResponseRevisionToDto)
                        .collect(Collectors.toList()));
                break;
        }
        
        return entityActivity;
    }

    @Override
    public List<AuditHistoryDto> searchAuditLogs(Map<String, Object> searchParams) {
        // Implementation for search functionality
        List<AuditHistoryDto> searchResults = new ArrayList<>();
        
        String entityType = (String) searchParams.get("entityType");
        String username = (String) searchParams.get("username");
        String action = (String) searchParams.get("action");
        LocalDateTime startDate = searchParams.get("startDate") != null ? 
            LocalDateTime.parse(searchParams.get("startDate").toString()) : null;
        LocalDateTime endDate = searchParams.get("endDate") != null ? 
            LocalDateTime.parse(searchParams.get("endDate").toString()) : null;
        
        AuditReader auditReader = AuditReaderFactory.get(entityManager);
        
        // Search workflows
        if (entityType == null || "workflow".equalsIgnoreCase(entityType)) {
            AuditQuery query = auditReader.createQuery()
                    .forRevisionsOfEntity(Workflow.class, false, true);
            
            if (username != null) {
                query.add(AuditEntity.revisionProperty("username").eq(username));
            }
            if (startDate != null) {
                query.add(AuditEntity.revisionProperty("timestamp").ge(startDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
            }
            if (endDate != null) {
                query.add(AuditEntity.revisionProperty("timestamp").le(endDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
            }
            
            List<Object[]> results = query.addOrder(AuditEntity.revisionNumber().desc()).getResultList();
            searchResults.addAll(results.stream()
                    .map(this::mapWorkflowRevisionToDto)
                    .collect(Collectors.toList()));
        }
        
        return searchResults;
    }

    @Override
    public String exportAuditLogs(Long workflowId, String format) {
        List<AuditHistoryDto> auditHistory = getCompleteWorkflowAuditTrail(workflowId);
        
        if ("csv".equals(format)) {
            return exportToCsv(auditHistory);
        } else {
            return exportToJson(auditHistory);
        }
    }

    @Override
    public List<AuditHistoryDto> getQuestionnaireResponseVersions(Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId).orElse(null);
        if (workflow == null) {
            return Collections.emptyList();
        }
        
        List<AuditHistoryDto> versions = new ArrayList<>();
        
        for (Answer response : workflow.getResponses()) {
            versions.addAll(getQuestionnaireResponseAuditHistory(response.getId()));
        }
        
        // Sort by timestamp descending
        versions.sort((a, b) -> {
            LocalDateTime timeA = a.getTimestamp() != null ? a.getTimestamp() : a.getRevisionDate();
            LocalDateTime timeB = b.getTimestamp() != null ? b.getTimestamp() : b.getRevisionDate();
            return timeB.compareTo(timeA);
        });
        
        return versions;
    }

    @Override
    public Map<String, Object> getReadOnlyWorkflowView(Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId).orElse(null);
        if (workflow == null) {
            return Collections.emptyMap();
        }
        
        Map<String, Object> readOnlyView = new HashMap<>();
        readOnlyView.put("id", workflow.getId());
        readOnlyView.put("materialCode", workflow.getMaterialCode());
        readOnlyView.put("state", workflow.getState());
        readOnlyView.put("assignedPlant", workflow.getAssignedPlant());
        readOnlyView.put("initiatedBy", workflow.getInitiatedBy());
        readOnlyView.put("materialName", workflow.getMaterialName());
        readOnlyView.put("materialDescription", workflow.getMaterialDescription());
        readOnlyView.put("createdAt", workflow.getCreatedAt());
        readOnlyView.put("lastModified", workflow.getLastModified());
        readOnlyView.put("completedAt", workflow.getCompletedAt());
        readOnlyView.put("isCompleted", workflow.getState().name().equals("COMPLETED"));
        readOnlyView.put("isReadOnly", true);
        
        // Add queries
        List<Map<String, Object>> queries = workflow.getQueries().stream()
                .map(this::mapQueryToReadOnlyView)
                .collect(Collectors.toList());
        readOnlyView.put("queries", queries);
        
        // Add responses
        List<Map<String, Object>> responses = workflow.getResponses().stream()
                .map(this::mapResponseToReadOnlyView)
                .collect(Collectors.toList());
        readOnlyView.put("responses", responses);
        
        // Add audit summary
        List<AuditHistoryDto> auditHistory = getWorkflowAuditHistory(workflowId);
        Map<String, Object> auditSummary = new HashMap<>();
        auditSummary.put("totalRevisions", auditHistory.size());
        auditSummary.put("lastModifiedBy", auditHistory.isEmpty() ? null : auditHistory.get(0).getUsername());
        auditSummary.put("lastModifiedAt", auditHistory.isEmpty() ? null : auditHistory.get(0).getTimestamp());
        readOnlyView.put("auditSummary", auditSummary);
        
        return readOnlyView;
    }

    // Helper methods for mapping revisions to DTOs
    private AuditHistoryDto mapWorkflowRevisionToDto(Object[] revision) {
        Workflow entity = (Workflow) revision[0];
        EnversConfig revInfo = (EnversConfig) revision[1];
        RevisionType revType = (RevisionType) revision[2];
        
        AuditHistoryDto dto = new AuditHistoryDto();
        dto.setId(entity.getId());
        dto.setRevisionId(revInfo.getId());
        dto.setRevisionType(revType.name());
        dto.setEntityType("Workflow");
        dto.setEntityId(entity.getId().toString());
        dto.setUsername(revInfo.getUsername());
        dto.setRevisionDate(revInfo.getRevisionDate());
        dto.setTimestamp(revInfo.getRevisionDate());
        dto.setMaterialCode(entity.getMaterialCode());
        dto.setWorkflowState(entity.getState().name());
        dto.setAction(mapRevisionTypeToAction(revType));
        dto.setDescription(generateWorkflowDescription(entity, revType));
        dto.setSeverity("INFO");
        dto.setCategory("WORKFLOW");
        
        return dto;
    }

    private AuditHistoryDto mapQueryRevisionToDto(Object[] revision) {
        Query entity = (Query) revision[0];
        EnversConfig revInfo = (EnversConfig) revision[1];
        RevisionType revType = (RevisionType) revision[2];
        
        AuditHistoryDto dto = new AuditHistoryDto();
        dto.setId(entity.getId());
        dto.setRevisionId(revInfo.getId());
        dto.setRevisionType(revType.name());
        dto.setEntityType("Query");
        dto.setEntityId(entity.getId().toString());
        dto.setUsername(revInfo.getUsername());
        dto.setRevisionDate(revInfo.getRevisionDate());
        dto.setTimestamp(revInfo.getRevisionDate());
        dto.setQueryStatus(entity.getStatus().name());
        dto.setAssignedTeam(entity.getAssignedTeam().name());
        dto.setAction(mapRevisionTypeToAction(revType));
        dto.setDescription(generateQueryDescription(entity, revType));
        dto.setSeverity("INFO");
        dto.setCategory("QUERY");
        
        return dto;
    }

    private AuditHistoryDto mapResponseRevisionToDto(Object[] revision) {
        Answer entity = (Answer) revision[0];
        EnversConfig revInfo = (EnversConfig) revision[1];
        RevisionType revType = (RevisionType) revision[2];
        
        AuditHistoryDto dto = new AuditHistoryDto();
        dto.setId(entity.getId());
        dto.setRevisionId(revInfo.getId());
        dto.setRevisionType(revType.name());
        dto.setEntityType("Answer");
        dto.setEntityId(entity.getId().toString());
        dto.setUsername(revInfo.getUsername());
        dto.setRevisionDate(revInfo.getRevisionDate());
        dto.setTimestamp(revInfo.getRevisionDate());
        dto.setStepNumber(entity.getStepNumber());
        dto.setFieldName(entity.getFieldName());
        dto.setFieldValue(entity.getFieldValue());
        dto.setAction(mapRevisionTypeToAction(revType));
        dto.setDescription(generateResponseDescription(entity, revType));
        dto.setSeverity("INFO");
        dto.setCategory("RESPONSE");
        
        return dto;
    }

    private String mapRevisionTypeToAction(RevisionType revType) {
        switch (revType) {
            case ADD: return "CREATE";
            case MOD: return "UPDATE";
            case DEL: return "DELETE";
            default: return "UNKNOWN";
        }
    }

    private String generateWorkflowDescription(Workflow workflow, RevisionType revType) {
        switch (revType) {
            case ADD:
                return "Created workflow for material " + workflow.getMaterialCode();
            case MOD:
                return "Updated workflow for material " + workflow.getMaterialCode();
            case DEL:
                return "Deleted workflow for material " + workflow.getMaterialCode();
            default:
                return "Modified workflow for material " + workflow.getMaterialCode();
        }
    }

    private String generateQueryDescription(Query query, RevisionType revType) {
        switch (revType) {
            case ADD:
                return "Created query for " + query.getAssignedTeam().name();
            case MOD:
                return query.getStatus().name().equals("RESOLVED") ? 
                    "Resolved query" : "Updated query";
            case DEL:
                return "Deleted query";
            default:
                return "Modified query";
        }
    }

    private String generateResponseDescription(Answer response, RevisionType revType) {
        switch (revType) {
            case ADD:
                return "Added response for " + response.getFieldName();
            case MOD:
                return "Updated response for " + response.getFieldName();
            case DEL:
                return "Deleted response for " + response.getFieldName();
            default:
                return "Modified response for " + response.getFieldName();
        }
    }

    private Map<String, Object> mapQueryToReadOnlyView(Query query) {
        Map<String, Object> queryView = new HashMap<>();
        queryView.put("id", query.getId());
        queryView.put("question", query.getQuestion());
        queryView.put("assignedTeam", query.getAssignedTeam().name());
        queryView.put("status", query.getStatus().name());
        queryView.put("response", query.getResponse());
        queryView.put("raisedBy", query.getRaisedBy());
        queryView.put("resolvedBy", query.getResolvedBy());
        queryView.put("createdAt", query.getCreatedAt());
        queryView.put("resolvedAt", query.getResolvedAt());
        queryView.put("isReadOnly", true);
        return queryView;
    }

    private Map<String, Object> mapResponseToReadOnlyView(Answer response) {
        Map<String, Object> responseView = new HashMap<>();
        responseView.put("id", response.getId());
        responseView.put("stepNumber", response.getStepNumber());
        responseView.put("fieldName", response.getFieldName());
        responseView.put("fieldValue", response.getFieldValue());
        responseView.put("fieldType", response.getFieldType());
        responseView.put("sectionName", response.getSectionName());
        responseView.put("lastModified", response.getLastModified());
        responseView.put("modifiedBy", response.getModifiedBy());
        responseView.put("isReadOnly", true);
        return responseView;
    }

    private String exportToCsv(List<AuditHistoryDto> auditHistory) {
        StringBuilder csv = new StringBuilder();
        csv.append("Timestamp,User,Action,Entity Type,Entity ID,Description\n");
        
        for (AuditHistoryDto entry : auditHistory) {
            csv.append(String.format("%s,%s,%s,%s,%s,\"%s\"\n",
                    entry.getTimestamp() != null ? entry.getTimestamp() : entry.getRevisionDate(),
                    entry.getUsername(),
                    entry.getAction(),
                    entry.getEntityType(),
                    entry.getEntityId(),
                    entry.getDescription().replace("\"", "\"\"")));
        }
        
        return csv.toString();
    }

    private String exportToJson(List<AuditHistoryDto> auditHistory) {
        // Simple JSON export - in production, use Jackson ObjectMapper
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        
        for (int i = 0; i < auditHistory.size(); i++) {
            AuditHistoryDto entry = auditHistory.get(i);
            json.append("  {\n");
            json.append(String.format("    \"timestamp\": \"%s\",\n", 
                    entry.getTimestamp() != null ? entry.getTimestamp() : entry.getRevisionDate()));
            json.append(String.format("    \"username\": \"%s\",\n", entry.getUsername()));
            json.append(String.format("    \"action\": \"%s\",\n", entry.getAction()));
            json.append(String.format("    \"entityType\": \"%s\",\n", entry.getEntityType()));
            json.append(String.format("    \"entityId\": \"%s\",\n", entry.getEntityId()));
            json.append(String.format("    \"description\": \"%s\"\n", 
                    entry.getDescription().replace("\"", "\\\"")));
            json.append("  }");
            if (i < auditHistory.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append("]");
        return json.toString();
    }
}