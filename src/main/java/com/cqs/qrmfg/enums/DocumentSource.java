package com.cqs.qrmfg.enums;

/**
 * Enumeration representing the source of document attachment in the system.
 * Used to track where documents were originally attached for better management and reuse.
 */
public enum DocumentSource {
    /**
     * Document attached directly to a workflow during workflow creation or processing
     */
    WORKFLOW("Workflow"),
    
    /**
     * Document attached to a query when raising the query
     */
    QUERY("Query"),
    
    /**
     * Document attached to a query response when resolving the query
     */
    RESPONSE("Response");
    
    private final String displayName;
    
    DocumentSource(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get a user-friendly description for UI display
     */
    public String getDescription() {
        switch (this) {
            case WORKFLOW:
                return "Originally attached to workflow";
            case QUERY:
                return "Originally attached to query";
            case RESPONSE:
                return "Originally attached to query response";
            default:
                return displayName;
        }
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}