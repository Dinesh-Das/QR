package com.cqs.qrmfg.model;

public enum NotificationChannel {
    EMAIL("Email", "Email notification"),
    IN_APP("In-App", "In-application notification");

    private final String displayName;
    private final String description;

    NotificationChannel(String displayName, String description) {
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