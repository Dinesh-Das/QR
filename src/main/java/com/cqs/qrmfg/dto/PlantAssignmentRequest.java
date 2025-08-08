package com.cqs.qrmfg.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * DTO for plant assignment requests
 */
public class PlantAssignmentRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotEmpty(message = "Plant codes list cannot be empty")
    private List<String> plantCodes;
    
    private String primaryPlant;
    
    public PlantAssignmentRequest() {
    }
    
    public PlantAssignmentRequest(Long userId, List<String> plantCodes) {
        this.userId = userId;
        this.plantCodes = plantCodes;
    }
    
    public PlantAssignmentRequest(Long userId, List<String> plantCodes, String primaryPlant) {
        this.userId = userId;
        this.plantCodes = plantCodes;
        this.primaryPlant = primaryPlant;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public List<String> getPlantCodes() {
        return plantCodes;
    }
    
    public void setPlantCodes(List<String> plantCodes) {
        this.plantCodes = plantCodes;
    }
    
    public String getPrimaryPlant() {
        return primaryPlant;
    }
    
    public void setPrimaryPlant(String primaryPlant) {
        this.primaryPlant = primaryPlant;
    }
}