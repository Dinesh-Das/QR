package com.cqs.qrmfg.dto;

import com.cqs.qrmfg.model.Query;
import com.cqs.qrmfg.model.Workflow;

/**
 * Context information for document upload operations.
 * Provides the necessary context to determine where and how documents should be attached.
 */
public class DocumentUploadContext {
    
    public enum ContextType {
        WORKFLOW, QUERY, RESPONSE
    }
    
    private final ContextType type;
    private final Workflow workflow;
    private final Query query;
    private final Long responseId;
    
    private DocumentUploadContext(ContextType type, Workflow workflow, Query query, Long responseId) {
        this.type = type;
        this.workflow = workflow;
        this.query = query;
        this.responseId = responseId;
    }
    
    /**
     * Create context for workflow document upload
     */
    public static DocumentUploadContext forWorkflow(Workflow workflow) {
        if (workflow == null) {
            throw new IllegalArgumentException("Workflow cannot be null");
        }
        return new DocumentUploadContext(ContextType.WORKFLOW, workflow, null, null);
    }
    
    /**
     * Create context for query document upload
     */
    public static DocumentUploadContext forQuery(Query query) {
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        return new DocumentUploadContext(ContextType.QUERY, null, query, null);
    }
    
    /**
     * Create context for query response document upload
     */
    public static DocumentUploadContext forResponse(Query query, Long responseId) {
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        if (responseId == null) {
            throw new IllegalArgumentException("Response ID cannot be null");
        }
        return new DocumentUploadContext(ContextType.RESPONSE, null, query, responseId);
    }
    
    // Getters
    public ContextType getType() {
        return type;
    }
    
    public Workflow getWorkflow() {
        return workflow;
    }
    
    public Query getQuery() {
        return query;
    }
    
    public Long getResponseId() {
        return responseId;
    }
    
    /**
     * Get the project code from the appropriate context
     */
    public String getProjectCode() {
        switch (type) {
            case WORKFLOW:
                return workflow.getProjectCode();
            case QUERY:
            case RESPONSE:
                return query.getWorkflow().getProjectCode();
            default:
                return null;
        }
    }
    
    /**
     * Get the material code from the appropriate context
     */
    public String getMaterialCode() {
        switch (type) {
            case WORKFLOW:
                return workflow.getMaterialCode();
            case QUERY:
            case RESPONSE:
                return query.getWorkflow().getMaterialCode();
            default:
                return null;
        }
    }
    
    /**
     * Get a description of this context for logging/display purposes
     */
    public String getDescription() {
        switch (type) {
            case WORKFLOW:
                return String.format("Workflow %d (%s/%s)", 
                    workflow.getId(), workflow.getProjectCode(), workflow.getMaterialCode());
            case QUERY:
                return String.format("Query %d in Workflow %d", 
                    query.getId(), query.getWorkflow().getId());
            case RESPONSE:
                return String.format("Response %d to Query %d", 
                    responseId, query.getId());
            default:
                return "Unknown context";
        }
    }
    
    @Override
    public String toString() {
        return String.format("DocumentUploadContext{type=%s, description='%s'}", 
            type, getDescription());
    }
}