package com.cqs.qrmfg.dto;

import com.cqs.qrmfg.model.Query;
import com.cqs.qrmfg.model.Workflow;

/**
 * Context information for document reuse operations.
 * Provides the necessary context to determine where documents should be reused.
 */
public class DocumentReuseContext {
    
    public enum ContextType {
        WORKFLOW, QUERY
    }
    
    private final ContextType type;
    private final Workflow workflow;
    private final Query query;
    
    private DocumentReuseContext(ContextType type, Workflow workflow, Query query) {
        this.type = type;
        this.workflow = workflow;
        this.query = query;
    }
    
    /**
     * Create context for reusing documents in a workflow
     */
    public static DocumentReuseContext forWorkflow(Workflow workflow) {
        if (workflow == null) {
            throw new IllegalArgumentException("Workflow cannot be null");
        }
        return new DocumentReuseContext(ContextType.WORKFLOW, workflow, null);
    }
    
    /**
     * Create context for reusing documents in a query
     */
    public static DocumentReuseContext forQuery(Query query) {
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        return new DocumentReuseContext(ContextType.QUERY, null, query);
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
    
    /**
     * Get the project code from the appropriate context
     */
    public String getProjectCode() {
        switch (type) {
            case WORKFLOW:
                return workflow.getProjectCode();
            case QUERY:
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
            default:
                return "Unknown context";
        }
    }
    
    @Override
    public String toString() {
        return String.format("DocumentReuseContext{type=%s, description='%s'}", 
            type, getDescription());
    }
}