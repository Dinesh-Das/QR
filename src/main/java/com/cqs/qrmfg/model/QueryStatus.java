package com.cqs.qrmfg.model;

public enum QueryStatus {
    OPEN("Open", "Query is waiting for resolution"),
    RESOLVED("Resolved", "Query has been answered and resolved");

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
}