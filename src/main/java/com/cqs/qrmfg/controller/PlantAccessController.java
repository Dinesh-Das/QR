package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.service.PlantAccessService;
import com.cqs.qrmfg.service.UserService;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.enums.RoleType;
import com.cqs.qrmfg.dto.*;
import com.cqs.qrmfg.exception.RBACException;
import com.cqs.qrmfg.exception.PlantAccessDeniedException;
import com.cqs.qrmfg.annotation.RequireRole;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for plant access management operations.
 * Provides endpoints for plant assignment, validation, and reporting.
 */
@RestController
@RequestMapping("/api/v1/admin/plant-access")
@PreAuthorize("hasRole('ADMIN')")
public class PlantAccessController {
    
    private static final Logger logger = LoggerFactory.getLogger(PlantAccessController.class);
    
    @Autowired
    private PlantAccessService plantAccessService;
    
    @Autowired
    private UserService userService;

    // ========== PLANT INFORMATION ENDPOINTS ==========

    /**
     * Get all available plant codes from QRMFG_LOCATIONS
     */
    @GetMapping("/plants")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<List<PlantInfoResponse>> getAvailablePlants() {
        logger.info("Fetching all available plant codes");
        
        try {
            List<String> plantCodes = plantAccessService.getAvailablePlantCodes();
            
            List<PlantInfoResponse> plantInfos = plantCodes.stream()
                    .map(plantCode -> {
                        String description = plantAccessService.getPlantDescription(plantCode);
                        return new PlantInfoResponse(plantCode, description);
                    })
                    .collect(Collectors.toList());
            
            logger.info("Successfully retrieved {} plant codes", plantInfos.size());
            return ResponseEntity.ok(plantInfos);
            
        } catch (Exception e) {
            logger.error("Error fetching available plants", e);
            throw new RBACException("PLANT_FETCH_ERROR", "Failed to fetch available plants: " + e.getMessage());
        }
    }

    /**
     * Get plant information by plant code
     */
    @GetMapping("/plants/{plantCode}")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<PlantInfoResponse> getPlantInfo(@PathVariable String plantCode) {
        logger.info("Fetching plant information for: {}", plantCode);
        
        try {
            if (!plantAccessService.plantExists(plantCode)) {
                logger.warn("Plant not found: {}", plantCode);
                return ResponseEntity.notFound().build();
            }
            
            String description = plantAccessService.getPlantDescription(plantCode);
            PlantInfoResponse response = new PlantInfoResponse(plantCode, description);
            
            logger.info("Successfully retrieved plant information for: {}", plantCode);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching plant information for: {}", plantCode, e);
            throw new RBACException("PLANT_INFO_ERROR", "Failed to fetch plant information: " + e.getMessage());
        }
    }

    /**
     * Validate plant codes
     */
    @PostMapping("/plants/validate")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<Map<String, Object>> validatePlantCodes(@Valid @RequestBody PlantValidationRequest request) {
        logger.info("Validating plant codes: {}", request.getPlantCodes());
        
        try {
            List<String> invalidPlants = plantAccessService.validatePlantCodesAndGetInvalid(request.getPlantCodes());
            List<String> validPlants = request.getPlantCodes().stream()
                    .filter(plantCode -> !invalidPlants.contains(plantCode))
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("validPlants", validPlants);
            response.put("invalidPlants", invalidPlants);
            response.put("allValid", invalidPlants.isEmpty());
            
            logger.info("Plant validation completed - valid: {}, invalid: {}", 
                    validPlants.size(), invalidPlants.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error validating plant codes", e);
            throw new RBACException("PLANT_VALIDATION_ERROR", "Failed to validate plant codes: " + e.getMessage());
        }
    }

    // ========== USER PLANT ASSIGNMENT ENDPOINTS ==========

    /**
     * Get plant assignments for a specific user
     */
    @GetMapping("/user/{userId}")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<PlantAccessResponse> getUserPlantAssignments(@PathVariable Long userId) {
        logger.info("Fetching plant assignments for user: {}", userId);
        
        try {
            User user = userService.findById(userId);
            if (user == null) {
                logger.warn("User not found: {}", userId);
                return ResponseEntity.notFound().build();
            }
            List<String> assignedPlants = plantAccessService.getUserAssignedPlants(userId);
            String primaryPlant = plantAccessService.getUserPrimaryPlant(userId);
            String effectivePlant = plantAccessService.getUserEffectivePlant(userId);
            
            PlantAccessResponse response = new PlantAccessResponse(
                userId, user.getUsername(), assignedPlants, primaryPlant);
            response.setEffectivePlant(effectivePlant);
            response.setHasPlantAccess(!assignedPlants.isEmpty());
            
            logger.info("Successfully retrieved plant assignments for user: {}", userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching user plant assignments", e);
            throw new RBACException("USER_PLANT_FETCH_ERROR", "Failed to fetch user plant assignments: " + e.getMessage());
        }
    }

    /**
     * Assign plants to a user
     */
    @PostMapping("/user/{userId}/assign")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<PlantAccessResponse> assignPlantsToUser(
            @PathVariable Long userId, 
            @Valid @RequestBody PlantAssignmentRequest request) {
        
        logger.info("Assigning plants {} to user {}", request.getPlantCodes(), userId);
        
        try {
            // Validate user exists
            User user = userService.findById(userId);
            if (user == null) {
                logger.warn("User not found: {}", userId);
                return ResponseEntity.notFound().build();
            }
            
            // Validate plant codes
            List<String> invalidPlants = plantAccessService.validatePlantCodesAndGetInvalid(request.getPlantCodes());
            if (!invalidPlants.isEmpty()) {
                throw new RBACException("INVALID_PLANT_CODES", 
                    "Invalid plant codes: " + String.join(", ", invalidPlants));
            }
            
            // Validate user plant assignments
            plantAccessService.validateUserPlantAssignments(userId);
            
            // Assign plants
            plantAccessService.assignPlantsToUser(userId, request.getPlantCodes());
            
            // Set primary plant if provided
            if (request.getPrimaryPlant() != null) {
                plantAccessService.setPrimaryPlant(userId, request.getPrimaryPlant());
            }
            
            // Return updated assignments
            PlantAccessResponse response = getUserPlantAssignments(userId).getBody();
            
            logger.info("Successfully assigned plants to user {}", userId);
            return ResponseEntity.ok(response);
            
        } catch (RBACException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error assigning plants to user", e);
            throw new RBACException("PLANT_ASSIGNMENT_ERROR", "Failed to assign plants: " + e.getMessage());
        }
    }

    /**
     * Add a single plant to user's assignments
     */
    @PostMapping("/user/{userId}/add-plant")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<PlantAccessResponse> addPlantToUser(
            @PathVariable Long userId, 
            @RequestParam String plantCode) {
        
        logger.info("Adding plant {} to user {}", plantCode, userId);
        
        try {
            // Validate user exists
            if (userService.findById(userId) == null) {
                logger.warn("User not found: {}", userId);
                return ResponseEntity.notFound().build();
            }
            
            // Validate plant code
            if (!plantAccessService.plantExists(plantCode)) {
                throw new RBACException("INVALID_PLANT_CODE", "Plant code does not exist: " + plantCode);
            }
            
            plantAccessService.addPlantToUser(userId, plantCode);
            
            PlantAccessResponse response = getUserPlantAssignments(userId).getBody();
            
            logger.info("Successfully added plant {} to user {}", plantCode, userId);
            return ResponseEntity.ok(response);
            
        } catch (RBACException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error adding plant to user", e);
            throw new RBACException("PLANT_ADD_ERROR", "Failed to add plant: " + e.getMessage());
        }
    }

    /**
     * Remove a single plant from user's assignments
     */
    @DeleteMapping("/user/{userId}/remove-plant")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<PlantAccessResponse> removePlantFromUser(
            @PathVariable Long userId, 
            @RequestParam String plantCode) {
        
        logger.info("Removing plant {} from user {}", plantCode, userId);
        
        try {
            // Validate user exists
            if (userService.findById(userId) == null) {
                logger.warn("User not found: {}", userId);
                return ResponseEntity.notFound().build();
            }
            
            plantAccessService.removePlantFromUser(userId, plantCode);
            
            PlantAccessResponse response = getUserPlantAssignments(userId).getBody();
            
            logger.info("Successfully removed plant {} from user {}", plantCode, userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error removing plant from user", e);
            throw new RBACException("PLANT_REMOVE_ERROR", "Failed to remove plant: " + e.getMessage());
        }
    }

    /**
     * Clear all plant assignments for a user
     */
    @DeleteMapping("/user/{userId}/clear")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<PlantAccessResponse> clearUserPlantAssignments(@PathVariable Long userId) {
        logger.info("Clearing all plant assignments for user: {}", userId);
        
        try {
            // Validate user exists
            if (userService.findById(userId) == null) {
                logger.warn("User not found: {}", userId);
                return ResponseEntity.notFound().build();
            }
            
            plantAccessService.clearUserPlantAssignments(userId);
            
            PlantAccessResponse response = getUserPlantAssignments(userId).getBody();
            
            logger.info("Successfully cleared plant assignments for user: {}", userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error clearing user plant assignments", e);
            throw new RBACException("PLANT_CLEAR_ERROR", "Failed to clear plant assignments: " + e.getMessage());
        }
    }

    // ========== PRIMARY PLANT MANAGEMENT ==========

    /**
     * Set primary plant for a user
     */
    @PutMapping("/user/{userId}/primary-plant")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<PlantAccessResponse> setPrimaryPlant(
            @PathVariable Long userId, 
            @RequestParam String plantCode) {
        
        logger.info("Setting primary plant {} for user {}", plantCode, userId);
        
        try {
            // Validate user exists
            if (userService.findById(userId) == null) {
                logger.warn("User not found: {}", userId);
                return ResponseEntity.notFound().build();
            }
            
            // Validate user has access to the plant
            if (!plantAccessService.hasPlantAccess(userId, plantCode)) {
                throw new PlantAccessDeniedException(plantCode, 
                    plantAccessService.getUserAssignedPlants(userId));
            }
            
            plantAccessService.setPrimaryPlant(userId, plantCode);
            
            PlantAccessResponse response = getUserPlantAssignments(userId).getBody();
            
            logger.info("Successfully set primary plant {} for user {}", plantCode, userId);
            return ResponseEntity.ok(response);
            
        } catch (RBACException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error setting primary plant", e);
            throw new RBACException("PRIMARY_PLANT_ERROR", "Failed to set primary plant: " + e.getMessage());
        }
    }

    // ========== ACCESS VALIDATION ENDPOINTS ==========

    /**
     * Check if a user has access to a specific plant
     */
    @GetMapping("/user/{userId}/has-access/{plantCode}")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<Map<String, Object>> checkPlantAccess(
            @PathVariable Long userId, 
            @PathVariable String plantCode) {
        
        logger.info("Checking plant access for user {} to plant {}", userId, plantCode);
        
        try {
            boolean hasAccess = plantAccessService.hasPlantAccess(userId, plantCode);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("plantCode", plantCode);
            response.put("hasAccess", hasAccess);
            response.put("assignedPlants", plantAccessService.getUserAssignedPlants(userId));
            
            logger.info("Plant access check completed - user {} {} access to plant {}", 
                    userId, hasAccess ? "has" : "does not have", plantCode);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error checking plant access", e);
            throw new RBACException("ACCESS_CHECK_ERROR", "Failed to check plant access: " + e.getMessage());
        }
    }

    /**
     * Validate user plant assignments based on their roles
     */
    @PostMapping("/user/{userId}/validate")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<Map<String, Object>> validateUserPlantAssignments(@PathVariable Long userId) {
        logger.info("Validating plant assignments for user: {}", userId);
        
        try {
            // Validate user exists
            if (userService.findById(userId) == null) {
                logger.warn("User not found: {}", userId);
                return ResponseEntity.notFound().build();
            }
            
            plantAccessService.validateUserPlantAssignments(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("valid", true);
            response.put("message", "Plant assignments are valid for user's roles");
            response.put("assignedPlants", plantAccessService.getUserAssignedPlants(userId));
            
            logger.info("Plant assignments validation passed for user: {}", userId);
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("valid", false);
            response.put("message", e.getMessage());
            response.put("assignedPlants", plantAccessService.getUserAssignedPlants(userId));
            
            logger.warn("Plant assignments validation failed for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            logger.error("Error validating user plant assignments", e);
            throw new RBACException("VALIDATION_ERROR", "Failed to validate plant assignments: " + e.getMessage());
        }
    }

    // ========== REPORTING ENDPOINTS ==========

    /**
     * Get plant access report - all users with their plant assignments
     */
    @GetMapping("/report")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<List<PlantAccessResponse>> getPlantAccessReport() {
        logger.info("Generating plant access report");
        
        try {
            List<User> allUsers = userService.findAll();
            
            List<PlantAccessResponse> report = allUsers.stream()
                    .map(user -> {
                        List<String> assignedPlants = plantAccessService.getUserAssignedPlants(user.getId());
                        String primaryPlant = plantAccessService.getUserPrimaryPlant(user.getId());
                        String effectivePlant = plantAccessService.getUserEffectivePlant(user.getId());
                        
                        PlantAccessResponse response = new PlantAccessResponse(
                            user.getId(), user.getUsername(), assignedPlants, primaryPlant);
                        response.setEffectivePlant(effectivePlant);
                        response.setHasPlantAccess(!assignedPlants.isEmpty());
                        
                        return response;
                    })
                    .collect(Collectors.toList());
            
            logger.info("Successfully generated plant access report for {} users", report.size());
            return ResponseEntity.ok(report);
            
        } catch (Exception e) {
            logger.error("Error generating plant access report", e);
            throw new RBACException("REPORT_ERROR", "Failed to generate plant access report: " + e.getMessage());
        }
    }

    /**
     * Get plant usage statistics
     */
    @GetMapping("/statistics")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<Map<String, Object>> getPlantAccessStatistics() {
        logger.info("Fetching plant access statistics");
        
        try {
            List<String> allPlants = plantAccessService.getAvailablePlantCodes();
            List<User> allUsers = userService.findAll();
            
            // Count users per plant
            Map<String, Long> plantUserCounts = allPlants.stream()
                    .collect(Collectors.toMap(
                        plantCode -> plantCode,
                        plantCode -> allUsers.stream()
                                .filter(user -> plantAccessService.hasPlantAccess(user.getId(), plantCode))
                                .count()
                    ));
            
            // Count users with plant access
            long usersWithPlantAccess = allUsers.stream()
                    .filter(user -> !plantAccessService.getUserAssignedPlants(user.getId()).isEmpty())
                    .count();
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalPlants", allPlants.size());
            statistics.put("totalUsers", allUsers.size());
            statistics.put("usersWithPlantAccess", usersWithPlantAccess);
            statistics.put("usersWithoutPlantAccess", allUsers.size() - usersWithPlantAccess);
            statistics.put("plantUserCounts", plantUserCounts);
            
            logger.info("Successfully generated plant access statistics");
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("Error fetching plant access statistics", e);
            throw new RBACException("STATISTICS_ERROR", "Failed to fetch plant access statistics: " + e.getMessage());
        }
    }
}