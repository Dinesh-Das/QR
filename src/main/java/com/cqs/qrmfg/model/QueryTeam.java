package com.cqs.qrmfg.model;

public enum QueryTeam {
    CQS("CQS Team", "Chemical Quality and Safety team"),
    TECH("Technology Team", "Technology and Engineering team"),
    JVC("JVC Team", "Joint Venture Company team");

    private final String displayName;
    private final String description;

    QueryTeam(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public WorkflowState getCorrespondingWorkflowState() {
        switch (this) {
            case CQS:
                return WorkflowState.CQS_PENDING;
            case TECH:
                return WorkflowState.TECH_PENDING;
            case JVC:
                return WorkflowState.JVC_PENDING;
            default:
                throw new IllegalStateException("Unknown query team: " + this);
        }
    }
}