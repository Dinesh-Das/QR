package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.User;
import java.util.List;

/**
 * Service interface for Plant Access management
 */
public interface PlantAccessService {
    
    /**
     * Check if user has access to a specific plant
     */
    boolean hasPlantAccess(User user, String plantCode);
    
    /**
     * Get list of plants user has access to
     */
    List<String> getUserPlantCodes(User user);
    
    /**
     * Get user's primary plant
     */
    String getUserPrimaryPlant(User user);
    
    /**
     * Assign plant to user
     */
    void assignPlantToUser(User user, String plantCode);
    
    /**
     * Remove plant assignment from user
     */
    void removePlantFromUser(User user, String plantCode);
    
    /**
     * Set user's primary plant
     */
    void setUserPrimaryPlant(User user, String plantCode);
    
    /**
     * Get all users assigned to a specific plant
     */
    List<User> getUsersForPlant(String plantCode);
    
    /**
     * Validate plant code format
     */
    boolean isValidPlantCode(String plantCode);
    
    /**
     * Get all available plant codes
     */
    List<String> getAllPlantCodes();
    
    // ========== ADDITIONAL METHODS FOR CONTROLLER COMPATIBILITY ==========
    
    /**
     * Get available plant codes
     */
    List<String> getAvailablePlantCodes();
    
    /**
     * Get plant description
     */
    String getPlantDescription(String plantCode);
    
    /**
     * Check if plant exists
     */
    boolean plantExists(String plantCode);
    
    /**
     * Validate plant codes
     */
    boolean validatePlantCodes(List<String> plantCodes);
    
    /**
     * Validate plant codes and return invalid ones
     */
    List<String> validatePlantCodesAndGetInvalid(List<String> plantCodes);
    
    /**
     * Get user assigned plants by user ID
     */
    List<String> getUserAssignedPlants(Long userId);
    
    /**
     * Get user effective plant by user ID
     */
    String getUserEffectivePlant(Long userId);
    
    /**
     * Validate user plant assignments by user ID
     */
    void validateUserPlantAssignments(Long userId);
    
    /**
     * Assign plants to user by user ID
     */
    void assignPlantsToUser(Long userId, List<String> plantCodes);
    
    /**
     * Set primary plant by user ID
     */
    void setPrimaryPlant(Long userId, String plantCode);
    
    /**
     * Add plant to user by user ID
     */
    void addPlantToUser(Long userId, String plantCode);
    
    /**
     * Clear user plant assignments by user ID
     */
    void clearUserPlantAssignments(Long userId);
    
    /**
     * Remove plant from user by user ID
     */
    void removePlantFromUser(Long userId, String plantCode);
    
    /**
     * Remove plants from user by user ID
     */
    void removePlantsFromUser(Long userId, List<String> plantCodes);
    
    /**
     * Check if user has access to plant by user ID
     */
    boolean hasPlantAccess(Long userId, String plantCode);
    
    /**
     * Get user primary plant by user ID
     */
    String getUserPrimaryPlant(Long userId);
}