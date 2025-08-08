package com.cqs.qrmfg.exception;

import java.util.List;

/**
 * Exception thrown when a user attempts to access data for a plant
 * they are not assigned to, or when plant-based filtering fails.
 */
public class PlantAccessDeniedException extends RBACException {
    
    private final String requestedPlantCode;
    private final List<String> userAssignedPlants;
    
    /**
     * Constructor for single plant access denial
     * 
     * @param plantCode the plant code that was requested but denied
     */
    public PlantAccessDeniedException(String plantCode) {
        super("PLANT_ACCESS_DENIED", "Access denied to plant: " + plantCode);
        this.requestedPlantCode = plantCode;
        this.userAssignedPlants = null;
    }
    
    /**
     * Constructor with user's assigned plants context
     * 
     * @param plantCode the plant code that was requested but denied
     * @param userAssignedPlants list of plants the user is assigned to
     */
    public PlantAccessDeniedException(String plantCode, List<String> userAssignedPlants) {
        super("PLANT_ACCESS_DENIED", buildErrorMessage(plantCode, userAssignedPlants));
        this.requestedPlantCode = plantCode;
        this.userAssignedPlants = userAssignedPlants;
    }
    
    /**
     * Constructor with custom message
     * 
     * @param plantCode the plant code that was requested but denied
     * @param userAssignedPlants list of plants the user is assigned to
     * @param customMessage custom error message
     */
    public PlantAccessDeniedException(String plantCode, List<String> userAssignedPlants, String customMessage) {
        super("PLANT_ACCESS_DENIED", customMessage);
        this.requestedPlantCode = plantCode;
        this.userAssignedPlants = userAssignedPlants;
    }
    
    /**
     * Constructor for general plant filtering failures
     * 
     * @param message error message describing the filtering failure
     * @param isGeneralError flag to distinguish from single plant constructor
     */
    public PlantAccessDeniedException(String message, boolean isGeneralError) {
        super(isGeneralError ? "PLANT_FILTERING_FAILED" : "PLANT_ACCESS_DENIED", message);
        this.requestedPlantCode = null;
        this.userAssignedPlants = null;
    }
    
    /**
     * Build a descriptive error message including user's assigned plants
     */
    private static String buildErrorMessage(String plantCode, List<String> userAssignedPlants) {
        StringBuilder message = new StringBuilder();
        message.append("Access denied to plant: ").append(plantCode);
        
        if (userAssignedPlants != null && !userAssignedPlants.isEmpty()) {
            message.append(". You are assigned to: [")
                   .append(String.join(", ", userAssignedPlants))
                   .append("]");
        } else {
            message.append(". You have no plant assignments.");
        }
        
        return message.toString();
    }
    
    /**
     * Get the plant code that was requested but denied
     */
    public String getRequestedPlantCode() {
        return requestedPlantCode;
    }
    
    /**
     * Get the list of plants the user is assigned to
     */
    public List<String> getUserAssignedPlants() {
        return userAssignedPlants;
    }
}