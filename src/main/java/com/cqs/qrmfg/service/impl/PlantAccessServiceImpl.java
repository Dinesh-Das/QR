package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.enums.RoleType;
import com.cqs.qrmfg.model.QrmfgLocationMaster;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.repository.QrmfgLocationMasterRepository;
import com.cqs.qrmfg.repository.UserRepository;
import com.cqs.qrmfg.service.PlantAccessService;
import com.cqs.qrmfg.util.RBACConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of PlantAccessService for managing plant access assignments and validation.
 */
@Service
@Transactional
public class PlantAccessServiceImpl implements PlantAccessService {
    
    private static final Logger logger = LoggerFactory.getLogger(PlantAccessServiceImpl.class);
    
    @Autowired
    private QrmfgLocationMasterRepository locationRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    @Transactional(readOnly = true)
    public List<String> getAvailablePlantCodes() {
        logger.debug("Fetching all available plant codes from QRMFG_LOCATIONS");
        
        try {
            List<QrmfgLocationMaster> locations = locationRepository.findAllOrderByLocationCode();
            List<String> plantCodes = locations.stream()
                .map(QrmfgLocationMaster::getLocationCode)
                .filter(code -> code != null && !code.trim().isEmpty())
                .map(RBACConstants::normalizePlantCode)
                .filter(code -> code != null)
                .collect(Collectors.toList());
            
            logger.debug("Found {} plant codes", plantCodes.size());
            return plantCodes;
        } catch (Exception e) {
            logger.error("Error fetching available plant codes", e);
            throw new RuntimeException("Failed to fetch available plant codes", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<String> getUserAssignedPlants(Long userId) {
        logger.debug("Fetching assigned plants for user ID: {}", userId);
        
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        
        User user = userOpt.get();
        List<String> assignedPlants = user.getAssignedPlantsList();
        
        logger.debug("User {} has {} assigned plants", userId, assignedPlants.size());
        return assignedPlants;
    }
    
    @Override
    public void assignPlantsToUser(Long userId, List<String> plantCodes) {
        logger.debug("Assigning plants to user ID: {}, plants: {}", userId, plantCodes);
        
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        
        User user = userOpt.get();
        
        // Validate plant codes if provided
        if (plantCodes != null && !plantCodes.isEmpty()) {
            List<String> invalidPlants = getInvalidPlantCodes(plantCodes);
            if (!invalidPlants.isEmpty()) {
                throw new IllegalArgumentException("Invalid plant codes: " + invalidPlants);
            }
            
            // Check maximum plants limit
            if (plantCodes.size() > RBACConstants.MAX_PLANTS_PER_USER) {
                throw new IllegalArgumentException(RBACConstants.ERROR_TOO_MANY_PLANTS);
            }
        }
        
        try {
            user.setAssignedPlantsList(plantCodes);
            userRepository.save(user);
            
            logger.info("Successfully assigned {} plants to user {}", 
                       plantCodes != null ? plantCodes.size() : 0, userId);
        } catch (Exception e) {
            logger.error("Error assigning plants to user {}", userId, e);
            throw new RuntimeException("Failed to assign plants to user", e);
        }
    }
    
    @Override
    public void removePlantsFromUser(Long userId, List<String> plantCodes) {
        logger.debug("Removing plants from user ID: {}, plants: {}", userId, plantCodes);
        
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        if (plantCodes == null || plantCodes.isEmpty()) {
            logger.debug("No plants to remove for user {}", userId);
            return;
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        
        User user = userOpt.get();
        
        try {
            // Normalize plant codes to remove
            List<String> normalizedPlantsToRemove = RBACConstants.normalizePlantCodes(plantCodes);
            
            for (String plantCode : normalizedPlantsToRemove) {
                user.removePlantAssignment(plantCode);
            }
            
            userRepository.save(user);
            
            logger.info("Successfully removed {} plants from user {}", plantCodes.size(), userId);
        } catch (Exception e) {
            logger.error("Error removing plants from user {}", userId, e);
            throw new RuntimeException("Failed to remove plants from user", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasPlantAccess(Long userId, String plantCode) {
        logger.debug("Checking plant access for user ID: {}, plant: {}", userId, plantCode);
        
        if (userId == null || plantCode == null || plantCode.trim().isEmpty()) {
            return false;
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            return false;
        }
        
        User user = userOpt.get();
        boolean hasAccess = user.hasPlantAccess(plantCode);
        
        logger.debug("User {} has access to plant {}: {}", userId, plantCode, hasAccess);
        return hasAccess;
    }
    
    @Override
    @Transactional(readOnly = true)
    public String getUserPrimaryPlant(Long userId) {
        logger.debug("Fetching primary plant for user ID: {}", userId);
        
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        
        User user = userOpt.get();
        String primaryPlant = user.getPrimaryPlant();
        
        logger.debug("User {} primary plant: {}", userId, primaryPlant);
        return primaryPlant;
    }
    
    @Override
    public void setPrimaryPlant(Long userId, String plantCode) {
        logger.debug("Setting primary plant for user ID: {}, plant: {}", userId, plantCode);
        
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        
        User user = userOpt.get();
        
        try {
            user.setPrimaryPlant(plantCode);
            userRepository.save(user);
            
            logger.info("Successfully set primary plant {} for user {}", plantCode, userId);
        } catch (Exception e) {
            logger.error("Error setting primary plant for user {}", userId, e);
            throw new RuntimeException("Failed to set primary plant", e);
        }
    }
    
    @Transactional(readOnly = true)
    public List<String> getInvalidPlantCodes(List<String> plantCodes) {
        logger.debug("Validating plant codes: {}", plantCodes);
        
        if (plantCodes == null || plantCodes.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> invalidPlants = new ArrayList<>();
        List<String> normalizedPlants = RBACConstants.normalizePlantCodes(plantCodes);
        
        for (String plantCode : normalizedPlants) {
            if (!plantExists(plantCode)) {
                invalidPlants.add(plantCode);
            }
        }
        
        logger.debug("Found {} invalid plant codes out of {}", invalidPlants.size(), plantCodes.size());
        return invalidPlants;
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean plantExists(String plantCode) {
        if (plantCode == null || plantCode.trim().isEmpty()) {
            return false;
        }
        
        String normalizedPlantCode = RBACConstants.normalizePlantCode(plantCode);
        if (!RBACConstants.isValidPlantCodeFormat(normalizedPlantCode)) {
            return false;
        }
        
        try {
            boolean exists = locationRepository.existsByLocationCode(normalizedPlantCode);
            logger.debug("Plant {} exists: {}", normalizedPlantCode, exists);
            return exists;
        } catch (Exception e) {
            logger.error("Error checking if plant exists: {}", plantCode, e);
            return false;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public String getPlantDescription(String plantCode) {
        if (plantCode == null || plantCode.trim().isEmpty()) {
            return null;
        }
        
        String normalizedPlantCode = RBACConstants.normalizePlantCode(plantCode);
        
        try {
            Optional<QrmfgLocationMaster> locationOpt = locationRepository.findByLocationCode(normalizedPlantCode);
            if (locationOpt.isPresent()) {
                String description = locationOpt.get().getDescription();
                logger.debug("Plant {} description: {}", normalizedPlantCode, description);
                return description;
            }
            
            logger.debug("Plant {} not found", normalizedPlantCode);
            return null;
        } catch (Exception e) {
            logger.error("Error fetching plant description for: {}", plantCode, e);
            return null;
        }
    }
    
    @Override
    public void addPlantToUser(Long userId, String plantCode) {
        logger.debug("Adding single plant to user ID: {}, plant: {}", userId, plantCode);
        
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        if (plantCode == null || plantCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Plant code cannot be null or empty");
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        
        User user = userOpt.get();
        
        // Validate plant exists
        if (!plantExists(plantCode)) {
            throw new IllegalArgumentException("Plant code does not exist: " + plantCode);
        }
        
        // Check if user already has maximum plants
        if (user.hasMaxPlantsAssigned()) {
            throw new IllegalArgumentException(RBACConstants.ERROR_TOO_MANY_PLANTS);
        }
        
        try {
            user.addPlantAssignment(plantCode);
            userRepository.save(user);
            
            logger.info("Successfully added plant {} to user {}", plantCode, userId);
        } catch (Exception e) {
            logger.error("Error adding plant to user {}", userId, e);
            throw new RuntimeException("Failed to add plant to user", e);
        }
    }
    
    @Override
    public void removePlantFromUser(Long userId, String plantCode) {
        logger.debug("Removing single plant from user ID: {}, plant: {}", userId, plantCode);
        
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        if (plantCode == null || plantCode.trim().isEmpty()) {
            return; // Nothing to remove
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        
        User user = userOpt.get();
        
        try {
            user.removePlantAssignment(plantCode);
            userRepository.save(user);
            
            logger.info("Successfully removed plant {} from user {}", plantCode, userId);
        } catch (Exception e) {
            logger.error("Error removing plant from user {}", userId, e);
            throw new RuntimeException("Failed to remove plant from user", e);
        }
    }
    
    @Override
    public void clearUserPlantAssignments(Long userId) {
        logger.debug("Clearing all plant assignments for user ID: {}", userId);
        
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        
        User user = userOpt.get();
        
        try {
            user.setAssignedPlantsList(null);
            user.setPrimaryPlant(null);
            userRepository.save(user);
            
            logger.info("Successfully cleared all plant assignments for user {}", userId);
        } catch (Exception e) {
            logger.error("Error clearing plant assignments for user {}", userId, e);
            throw new RuntimeException("Failed to clear plant assignments", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public String getUserEffectivePlant(Long userId) {
        logger.debug("Fetching effective plant for user ID: {}", userId);
        
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        
        User user = userOpt.get();
        String effectivePlant = user.getEffectivePlant();
        
        logger.debug("User {} effective plant: {}", userId, effectivePlant);
        return effectivePlant;
    }
    
    @Override
    @Transactional(readOnly = true)
    public void validateUserPlantAssignments(Long userId) {
        logger.debug("Validating plant assignments for user ID: {}", userId);
        
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        
        User user = userOpt.get();
        
        try {
            user.validatePlantAssignments();
            logger.debug("Plant assignments validation passed for user {}", userId);
        } catch (IllegalStateException e) {
            logger.error("Plant assignments validation failed for user {}: {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error validating plant assignments for user {}", userId, e);
            throw new RuntimeException("Failed to validate plant assignments", e);
        }
    }
    
    // ========== LEGACY INTERFACE METHODS (User object based) ==========
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasPlantAccess(User user, String plantCode) {
        if (user == null) {
            return false;
        }
        return user.hasPlantAccess(plantCode);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<String> getUserPlantCodes(User user) {
        if (user == null) {
            return new ArrayList<>();
        }
        return user.getAssignedPlantsList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public String getUserPrimaryPlant(User user) {
        if (user == null) {
            return null;
        }
        return user.getPrimaryPlant();
    }
    
    @Override
    public void assignPlantToUser(User user, String plantCode) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        if (plantCode == null || plantCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Plant code cannot be null or empty");
        }
        
        // Validate plant exists
        if (!plantExists(plantCode)) {
            throw new IllegalArgumentException("Plant code does not exist: " + plantCode);
        }
        
        try {
            user.addPlantAssignment(plantCode);
            userRepository.save(user);
            
            logger.info("Successfully assigned plant {} to user {}", plantCode, user.getId());
        } catch (Exception e) {
            logger.error("Error assigning plant to user {}", user.getId(), e);
            throw new RuntimeException("Failed to assign plant to user", e);
        }
    }
    
    @Override
    public void removePlantFromUser(User user, String plantCode) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        if (plantCode == null || plantCode.trim().isEmpty()) {
            return; // Nothing to remove
        }
        
        try {
            user.removePlantAssignment(plantCode);
            userRepository.save(user);
            
            logger.info("Successfully removed plant {} from user {}", plantCode, user.getId());
        } catch (Exception e) {
            logger.error("Error removing plant from user {}", user.getId(), e);
            throw new RuntimeException("Failed to remove plant from user", e);
        }
    }
    
    @Override
    public void setUserPrimaryPlant(User user, String plantCode) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        try {
            user.setPrimaryPlant(plantCode);
            userRepository.save(user);
            
            logger.info("Successfully set primary plant {} for user {}", plantCode, user.getId());
        } catch (Exception e) {
            logger.error("Error setting primary plant for user {}", user.getId(), e);
            throw new RuntimeException("Failed to set primary plant", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<User> getUsersForPlant(String plantCode) {
        logger.debug("Fetching users for plant: {}", plantCode);
        
        if (plantCode == null || plantCode.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String normalizedPlantCode = RBACConstants.normalizePlantCode(plantCode);
        
        try {
            List<User> allUsers = userRepository.findAll();
            List<User> usersWithPlant = allUsers.stream()
                .filter(user -> user.hasPlantAccess(normalizedPlantCode))
                .collect(Collectors.toList());
            
            logger.debug("Found {} users with access to plant {}", usersWithPlant.size(), plantCode);
            return usersWithPlant;
        } catch (Exception e) {
            logger.error("Error fetching users for plant {}", plantCode, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isValidPlantCode(String plantCode) {
        return RBACConstants.isValidPlantCodeFormat(plantCode) && plantExists(plantCode);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<String> getAllPlantCodes() {
        return getAvailablePlantCodes();
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean validatePlantCodes(List<String> plantCodes) {
        List<String> invalidPlants = getInvalidPlantCodes(plantCodes);
        return invalidPlants.isEmpty();
    }
    
    @Override
    public List<String> validatePlantCodesAndGetInvalid(List<String> plantCodes) {
        return getInvalidPlantCodes(plantCodes);
    }
   
   
    
}