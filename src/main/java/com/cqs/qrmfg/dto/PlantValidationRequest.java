package com.cqs.qrmfg.dto;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * DTO for plant validation requests
 */
public class PlantValidationRequest {
    
    @NotEmpty(message = "Plant codes list cannot be empty")
    private List<String> plantCodes;
    
    public PlantValidationRequest() {
    }
    
    public PlantValidationRequest(List<String> plantCodes) {
        this.plantCodes = plantCodes;
    }
    
    public List<String> getPlantCodes() {
        return plantCodes;
    }
    
    public void setPlantCodes(List<String> plantCodes) {
        this.plantCodes = plantCodes;
    }
}