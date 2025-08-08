package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.model.Role;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.service.RoleService;
import com.cqs.qrmfg.enums.RoleType;
import com.cqs.qrmfg.dto.*;
import com.cqs.qrmfg.exception.RBACException;
import com.cqs.qrmfg.exception.InsufficientRoleException;
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
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Enhanced REST controller for role management operations with RBAC support.
 * Provides endpoints for role CRUD operations, role assignments, and plant assignments.
 */
@RestController
@RequestMapping("/api/v1/roles")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {
    
    private static final Logger logger = LoggerFactory.getLogger(RoleController.class);
    
    @Autowired
    private RoleService roleService;

    // ========== BASIC ROLE CRUD OPERATIONS ==========

    /**
     * Get all roles with optional filtering
     */
    @GetMapping
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<List<RoleResponse>> getAllRoles(
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) RoleType roleType) {
        
        logger.info("Fetching all roles - enabled: {}, roleType: {}", enabled, roleType);
        
        try {
            List<Role> roles;
            
            if (roleType != null) {
                roles = roleService.findByRoleType(roleType, enabled == null ? true : enabled);
            } else if (enabled != null && enabled) {
                roles = roleService.findAllEnabled();
            } else {
                roles = roleService.findAll();
            }
            
            List<RoleResponse> roleResponses = roles.stream()
                    .map(this::convertToRoleResponse)
                    .collect(Collectors.toList());
            
            logger.info("Successfully retrieved {} roles", roleResponses.size());
            return ResponseEntity.ok(roleResponses);
            
        } catch (Exception e) {
            logger.error("Error fetching roles", e);
            throw new RBACException("ROLE_FETCH_ERROR", "Failed to fetch roles: " + e.getMessage());
        }
    }

    /**
     * Get role by ID
     */
    @GetMapping("/{id}")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable Long id) {
        logger.info("Fetching role with ID: {}", id);
        
        try {
            Optional<Role> role = roleService.findById(id);
            
            if (role.isPresent()) {
                RoleResponse response = convertToRoleResponse(role.get());
                logger.info("Successfully retrieved role: {}", role.get().getName());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Role not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error fetching role with ID: {}", id, e);
            throw new RBACException("ROLE_FETCH_ERROR", "Failed to fetch role: " + e.getMessage());
        }
    }

    /**
     * Create a new role
     */
    @PostMapping
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleCreateRequest request) {
        logger.info("Creating new role with type: {}", request.getRoleType());
        
        try {
            // Check if role already exists
            if (roleService.existsByRoleType(request.getRoleType())) {
                logger.warn("Role already exists with type: {}", request.getRoleType());
                throw new RBACException("ROLE_ALREADY_EXISTS", 
                    "Role with type " + request.getRoleType() + " already exists");
            }
            
            Role createdRole = roleService.createRole(request.getRoleType(), request.getDescription());
            RoleResponse response = convertToRoleResponse(createdRole);
            
            logger.info("Successfully created role: {} with ID: {}", createdRole.getName(), createdRole.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (RBACException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error creating role", e);
            throw new RBACException("ROLE_CREATE_ERROR", "Failed to create role: " + e.getMessage());
        }
    }

    /**
     * Update an existing role
     */
    @PutMapping("/{id}")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<RoleResponse> updateRole(@PathVariable Long id, @Valid @RequestBody Role role) {
        logger.info("Updating role with ID: {}", id);
        
        try {
            if (!roleService.findById(id).isPresent()) {
                logger.warn("Role not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            role.setId(id);
            Role updatedRole = roleService.update(role);
            RoleResponse response = convertToRoleResponse(updatedRole);
            
            logger.info("Successfully updated role: {}", updatedRole.getName());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error updating role with ID: {}", id, e);
            throw new RBACException("ROLE_UPDATE_ERROR", "Failed to update role: " + e.getMessage());
        }
    }

    /**
     * Delete a role
     */
    @DeleteMapping("/{id}")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        logger.info("Deleting role with ID: {}", id);
        
        try {
            if (!roleService.findById(id).isPresent()) {
                logger.warn("Role not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            roleService.delete(id);
            logger.info("Successfully deleted role with ID: {}", id);
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            logger.error("Error deleting role with ID: {}", id, e);
            throw new RBACException("ROLE_DELETE_ERROR", "Failed to delete role: " + e.getMessage());
        }
    }

    // ========== ROLE ASSIGNMENT OPERATIONS ==========

    /**
     * Assign a role to a user
     */
    @PostMapping("/assign")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<UserRoleAssignmentDto> assignRoleToUser(@Valid @RequestBody RoleAssignmentRequest request) {
        logger.info("Assigning role {} to user {}", request.getRoleType(), request.getUserId());
        
        try {
            // Validate role assignment
            roleService.validateRoleAssignment(request.getUserId(), request.getRoleType());
            
            // Assign the role
            roleService.assignRoleToUser(request.getUserId(), request.getRoleType());
            
            // If plant codes are provided and role supports plant filtering, assign plants
            if (request.getPlantCodes() != null && !request.getPlantCodes().isEmpty() 
                && request.getRoleType().supportsPlantFiltering()) {
                roleService.assignPlantsToUser(request.getUserId(), request.getPlantCodes());
            }
            
            // Return updated user role assignment
            UserRoleAssignmentDto response = getUserRoleAssignment(request.getUserId());
            
            logger.info("Successfully assigned role {} to user {}", request.getRoleType(), request.getUserId());
            return ResponseEntity.ok(response);
            
        } catch (RBACException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error assigning role to user", e);
            throw new RBACException("ROLE_ASSIGNMENT_ERROR", "Failed to assign role: " + e.getMessage());
        }
    }

    /**
     * Remove a role from a user
     */
    @DeleteMapping("/assign")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<UserRoleAssignmentDto> removeRoleFromUser(@Valid @RequestBody RoleAssignmentRequest request) {
        logger.info("Removing role {} from user {}", request.getRoleType(), request.getUserId());
        
        try {
            roleService.removeRoleFromUser(request.getUserId(), request.getRoleType());
            
            UserRoleAssignmentDto response = getUserRoleAssignment(request.getUserId());
            
            logger.info("Successfully removed role {} from user {}", request.getRoleType(), request.getUserId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error removing role from user", e);
            throw new RBACException("ROLE_REMOVAL_ERROR", "Failed to remove role: " + e.getMessage());
        }
    }

    /**
     * Replace user's role with a new one
     */
    @PutMapping("/assign/replace")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<UserRoleAssignmentDto> replaceUserRole(@Valid @RequestBody RoleAssignmentRequest request) {
        logger.info("Replacing user {} role with {}", request.getUserId(), request.getRoleType());
        
        try {
            roleService.validateRoleAssignment(request.getUserId(), request.getRoleType());
            roleService.replaceUserRole(request.getUserId(), request.getRoleType());
            
            // If plant codes are provided and role supports plant filtering, assign plants
            if (request.getPlantCodes() != null && !request.getPlantCodes().isEmpty() 
                && request.getRoleType().supportsPlantFiltering()) {
                roleService.assignPlantsToUser(request.getUserId(), request.getPlantCodes());
            }
            
            UserRoleAssignmentDto response = getUserRoleAssignment(request.getUserId());
            
            logger.info("Successfully replaced user {} role with {}", request.getUserId(), request.getRoleType());
            return ResponseEntity.ok(response);
            
        } catch (RBACException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error replacing user role", e);
            throw new RBACException("ROLE_REPLACEMENT_ERROR", "Failed to replace role: " + e.getMessage());
        }
    }

    /**
     * Get user role assignments
     */
    @GetMapping("/assign/user/{userId}")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<UserRoleAssignmentDto> getUserRoleAssignments(@PathVariable Long userId) {
        logger.info("Fetching role assignments for user: {}", userId);
        
        try {
            UserRoleAssignmentDto response = getUserRoleAssignment(userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching user role assignments", e);
            throw new RBACException("USER_ROLES_FETCH_ERROR", "Failed to fetch user roles: " + e.getMessage());
        }
    }

    // ========== PLANT ASSIGNMENT OPERATIONS ==========

    /**
     * Assign plants to a user
     */
    @PostMapping("/plants/assign")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<Map<String, Object>> assignPlantsToUser(@Valid @RequestBody PlantAssignmentRequest request) {
        logger.info("Assigning plants {} to user {}", request.getPlantCodes(), request.getUserId());
        
        try {
            roleService.assignPlantsToUser(request.getUserId(), request.getPlantCodes());
            
            if (request.getPrimaryPlant() != null) {
                roleService.setPrimaryPlant(request.getUserId(), request.getPrimaryPlant());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", request.getUserId());
            response.put("assignedPlants", roleService.getUserPlantCodes(request.getUserId()));
            response.put("primaryPlant", roleService.getPrimaryPlant(request.getUserId()));
            
            logger.info("Successfully assigned plants to user {}", request.getUserId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error assigning plants to user", e);
            throw new RBACException("PLANT_ASSIGNMENT_ERROR", "Failed to assign plants: " + e.getMessage());
        }
    }

    /**
     * Get user plant assignments
     */
    @GetMapping("/plants/user/{userId}")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<Map<String, Object>> getUserPlantAssignments(@PathVariable Long userId) {
        logger.info("Fetching plant assignments for user: {}", userId);
        
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("assignedPlants", roleService.getUserPlantCodes(userId));
            response.put("primaryPlant", roleService.getPrimaryPlant(userId));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching user plant assignments", e);
            throw new RBACException("PLANT_FETCH_ERROR", "Failed to fetch plant assignments: " + e.getMessage());
        }
    }

    /**
     * Set primary plant for a user
     */
    @PutMapping("/plants/primary")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<Map<String, Object>> setPrimaryPlant(
            @RequestParam Long userId, 
            @RequestParam String plantCode) {
        
        logger.info("Setting primary plant {} for user {}", plantCode, userId);
        
        try {
            roleService.setPrimaryPlant(userId, plantCode);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("primaryPlant", plantCode);
            response.put("assignedPlants", roleService.getUserPlantCodes(userId));
            
            logger.info("Successfully set primary plant {} for user {}", plantCode, userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error setting primary plant", e);
            throw new RBACException("PRIMARY_PLANT_ERROR", "Failed to set primary plant: " + e.getMessage());
        }
    }

    // ========== ROLE STATISTICS AND UTILITIES ==========

    /**
     * Get role statistics
     */
    @GetMapping("/statistics")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<Map<String, Object>> getRoleStatistics() {
        logger.info("Fetching role statistics");
        
        try {
            Map<String, Object> statistics = roleService.getRoleStatistics();
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("Error fetching role statistics", e);
            throw new RBACException("STATISTICS_ERROR", "Failed to fetch statistics: " + e.getMessage());
        }
    }

    /**
     * Get users by role type
     */
    @GetMapping("/users/{roleType}")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<List<User>> getUsersByRoleType(@PathVariable RoleType roleType) {
        logger.info("Fetching users with role type: {}", roleType);
        
        try {
            List<User> users = roleService.getUsersByRoleType(roleType);
            logger.info("Found {} users with role type: {}", users.size(), roleType);
            return ResponseEntity.ok(users);
            
        } catch (Exception e) {
            logger.error("Error fetching users by role type", e);
            throw new RBACException("USERS_FETCH_ERROR", "Failed to fetch users: " + e.getMessage());
        }
    }

    /**
     * Initialize default roles
     */
    @PostMapping("/initialize")
    @RequireRole(RoleType.ADMIN)
    public ResponseEntity<Map<String, Object>> initializeDefaultRoles() {
        logger.info("Initializing default roles");
        
        try {
            roleService.initializeDefaultRoles();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Default roles initialized successfully");
            response.put("roles", roleService.findAll().stream()
                    .map(this::convertToRoleResponse)
                    .collect(Collectors.toList()));
            
            logger.info("Successfully initialized default roles");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error initializing default roles", e);
            throw new RBACException("INITIALIZATION_ERROR", "Failed to initialize roles: " + e.getMessage());
        }
    }

    // ========== HELPER METHODS ==========

    /**
     * Convert Role entity to RoleResponse DTO
     */
    private RoleResponse convertToRoleResponse(Role role) {
        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setName(role.getName());
        response.setDescription(role.getDescription());
        response.setRoleType(role.getRoleType());
        response.setEnabled(role.isEnabled());
        response.setCreatedAt(role.getCreatedAt());
        response.setUpdatedAt(role.getUpdatedAt());
        
        // Get user count for this role
        List<User> users = roleService.getUsersByRole(role.getId());
        response.setUserCount(users.size());
        
        return response;
    }

    /**
     * Get user role assignment information
     */
    private UserRoleAssignmentDto getUserRoleAssignment(Long userId) {
        // This would typically involve fetching user information and roles
        // For now, we'll create a basic response structure
        UserRoleAssignmentDto dto = new UserRoleAssignmentDto();
        dto.setUserId(userId);
        
        // Get assigned roles (this would need to be implemented based on your User-Role relationship)
        List<UserRoleAssignmentDto.RoleDto> assignedRoles = roleService.findAll().stream()
            .filter(role -> roleService.hasRole(userId, role.getRoleType()))
            .map(role -> new UserRoleAssignmentDto.RoleDto(role.getId(), role.getName(), role.getDescription()))
            .collect(Collectors.toList());
        
        dto.setAssignedRoles(assignedRoles);
        
        // Get available roles
        List<UserRoleAssignmentDto.RoleDto> availableRoles = roleService.findAllEnabled().stream()
            .map(role -> new UserRoleAssignmentDto.RoleDto(role.getId(), role.getName(), role.getDescription()))
            .collect(Collectors.toList());
        
        dto.setAvailableRoles(availableRoles);
        
        return dto;
    }
}
