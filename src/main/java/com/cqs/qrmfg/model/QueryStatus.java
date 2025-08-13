package com.cqs.qrmfg.model;

public enum QueryStatus {
    OPEN("Open", "Query is waiting for resolution"),
    RESOLVED("Resolved", "Query has been answered and resolved"),
    CLOSED("Closed", "Query has been closed without resolution");

    private final String displayName;
    private final String description;

    QueryStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isResolved() {
        return this == RESOLVED;
    }

    public boolean isActive() {
        return this == OPEN;
    }

    public boolean allowsPlantEditing() {
        // Plant can edit form even when queries are open
        return this != CLOSED;
    }
}