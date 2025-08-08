package com.cqs.qrmfg.dto;

import java.util.List;

/**
 * DTO for plant access response data
 */
public class PlantAccessResponse {
    
    private Long userId;
    private String username;
    private List<String> assignedPlants;
    private String primaryPlant;
    private String effectivePlant;
    private boolean hasPlantAccess;
    
    public PlantAccessResponse() {
    }
    
    public PlantAccessResponse(Long userId, String username, List<String> assignedPlants, String primaryPlant) {
        this.userId = userId;
        this.username = username;
        this.assignedPlants = assignedPlants;
        this.primaryPlant = primaryPlant;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public List<String> getAssignedPlants() {
        return assignedPlants;
    }
    
    public void setAssignedPlants(List<String> assignedPlants) {
        this.assignedPlants = assignedPlants;
    }
    
    public String getPrimaryPlant() {
        return primaryPlant;
    }
    
    public void setPrimaryPlant(String primaryPlant) {
        this.primaryPlant = primaryPlant;
    }
    
    public String getEffectivePlant() {
        return effectivePlant;
    }
    
    public void setEffectivePlant(String effectivePlant) {
        this.effectivePlant = effectivePlant;
    }
    
    public boolean isHasPlantAccess() {
        return hasPlantAccess;
    }
    
    public void setHasPlantAccess(boolean hasPlantAccess) {
        this.hasPlantAccess = hasPlantAccess;
    }
}