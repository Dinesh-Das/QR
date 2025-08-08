package com.cqs.qrmfg.dto;

/**
 * DTO for plant information response
 */
public class PlantInfoResponse {
    
    private String plantCode;
    private String description;
    private boolean active;
    private int userCount;
    
    public PlantInfoResponse() {
    }
    
    public PlantInfoResponse(String plantCode, String description) {
        this.plantCode = plantCode;
        this.description = description;
        this.active = true;
    }
    
    public PlantInfoResponse(String plantCode, String description, boolean active, int userCount) {
        this.plantCode = plantCode;
        this.description = description;
        this.active = active;
        this.userCount = userCount;
    }
    
    public String getPlantCode() {
        return plantCode;
    }
    
    public void setPlantCode(String plantCode) {
        this.plantCode = plantCode;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public int getUserCount() {
        return userCount;
    }
    
    public void setUserCount(int userCount) {
        this.userCount = userCount;
    }
}