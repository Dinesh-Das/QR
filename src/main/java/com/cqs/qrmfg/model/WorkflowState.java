package com.cqs.qrmfg.model;
// Need to add more state, 
public enum WorkflowState {
    JVC_PENDING("JVC Extension Required", "JVC team needs to extend material to plant"),
    PLANT_PENDING("Plant Questionnaire", "Plant team needs to complete questionnaire"),
    CQS_PENDING("CQS Query Resolution", "CQS team needs to resolve pending queries"),
    TECH_PENDING("Technology Query Resolution", "Technology team needs to resolve pending queries"),
    COMPLETED("Workflow Completed", "All steps completed successfully");

    private final String displayName;
    private final String description;

    WorkflowState(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isQueryState() {
        return this == CQS_PENDING || this == TECH_PENDING || this == JVC_PENDING;
    }

    public boolean isTerminalState() {
        return this == COMPLETED;
    }

    public boolean canTransitionTo(WorkflowState newState) {
        switch (this) {
            case JVC_PENDING:
                // JVC can transition to PLANT_PENDING or other query states (for multi-query scenarios)
                return newState == PLANT_PENDING || newState == CQS_PENDING || newState == TECH_PENDING;
            case PLANT_PENDING:
                return newState == CQS_PENDING || newState == TECH_PENDING || newState == JVC_PENDING || newState == COMPLETED;
            case CQS_PENDING:
                // CQS can transition back to PLANT_PENDING or to other query states (for multi-query scenarios)
                return newState == PLANT_PENDING || newState == TECH_PENDING || newState == JVC_PENDING;
            case TECH_PENDING:
                // TECH can transition back to PLANT_PENDING or to other query states (for multi-query scenarios)
                return newState == PLANT_PENDING || newState == CQS_PENDING || newState == JVC_PENDING;
            case COMPLETED:
                return false; // Terminal state
            default:
                return false;
        }
    }
}