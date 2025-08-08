package com.cqs.qrmfg.model;

public enum NotificationType {
    WORKFLOW_CREATED("Workflow Created", "A new workflow has been created"),
    WORKFLOW_EXTENDED("Workflow Extended", "Workflow has been extended to plant"),
    WORKFLOW_COMPLETED("Workflow Completed", "Workflow has been completed"),
    QUERY_RAISED("Query Raised", "A new query has been raised"),
    QUERY_RESOLVED("Query Resolved", "A query has been resolved"),
    QUERY_OVERDUE("Query Overdue", "A query is overdue for resolution"),
    WORKFLOW_OVERDUE("Workflow Overdue", "A workflow is overdue"),
    QUERY_ASSIGNED("Query Assigned", "A query has been assigned to a team"),
    WORKFLOW_STATE_CHANGED("Workflow State Changed", "Workflow state has changed");

    private final String displayName;
    private final String description;

    NotificationType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}