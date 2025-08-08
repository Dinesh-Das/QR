package com.cqs.qrmfg.dto;

/**
 * DTO for plant dropdown options
 */
public class PlantOption {
    private String value; // location_code (e.g., "1001")
    private String label; // location_code (same as value for display)

    public PlantOption() {}

    public PlantOption(String value, String label) {
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