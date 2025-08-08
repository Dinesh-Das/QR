package com.cqs.qrmfg.dto;

/**
 * DTO for material dropdown options
 */
public class MaterialOption {
    private String value; // r_object_key (e.g., "R31516J")
    private String label; // r_object_desc (e.g., "Axion CS 2455 (DBTO)")
    private String projectCode; // object_key (parent project)

    public MaterialOption() {}

    public MaterialOption(String value, String label, String projectCode) {
        this.value = value;
        this.label = label;
        this.projectCode = projectCode;
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

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }
}