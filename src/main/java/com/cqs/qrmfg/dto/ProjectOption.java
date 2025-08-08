package com.cqs.qrmfg.dto;

/**
 * DTO for project dropdown options
 */
public class ProjectOption {
    private String value; // object_key (e.g., "SER-A-000210")
    private String label; // object_key (same as value for display)

    public ProjectOption() {}

    public ProjectOption(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}