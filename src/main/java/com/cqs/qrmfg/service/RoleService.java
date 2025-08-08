package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.Role;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.enums.RoleType;
import java.util.List;
import java.util.Optional;
import java.util.Map;

/**
 * Service interface for Role management
 */
public interface RoleService {
    
    // ========== BASIC CRUD OPERATIONS ==========
    
    /**
     * Find role by name
     */
    Optional<Role> findByName(String name);
    
    /**
     * Find role by ID
     */
    Optional<Role> findById(Long id);
    
    /**
     * Save role
     */
    Role save(Role role);
    
    /**
     * Update role
     */
    Role update(Role role);
    
    /**
     * Delete role by ID
     */
    void delete(Long id);
    
    /**
     * Find all roles
     */
    List<Role> findAll();
    
    /**
     * Check if role exists by name
     */
    boolean existsByName(String name);
    
    // ========== RBAC-SPECIFIC ROLE MANAGEMENT ==========
    
    /**
     * Create role with role type
     */
    Role createRole(RoleType roleType, String description);
    
    /**
     * Find role by role type
     */
    Optional<Role> findByRoleType(RoleType roleType);
    
    /**
     * Find roles by role type with enabled filter
     */
    List<Role> findByRoleType(RoleType roleType, boolean includeDisabled);
    
    /**
     * Find all enabled roles
     */
    List<Role> findAllEnabled();
    
    /**
     * Check if role exists by role type
     */
    boolean existsByRoleType(RoleType roleType);
    
    // ========== ROLE ASSIGNMENT OPERATIONS ==========
    
    /**
     * Assign role to user by IDs
     */
    void assignRoleToUser(Long userId, Long roleId);
    
    /**
     * Assign role to user by role type
     */
    void assignRoleToUser(Long userId, RoleType roleType);
    
    /**
     * Remove role from user by IDs
     */
    void removeRoleFromUser(Long userId, Long roleId);
    
    /**
     * Remove role from user by role type
     */
    void removeRoleFromUser(Long userId, RoleType roleType);
    
    /**
     * Replace user's role with new role type
     */
    void replaceUserRole(Long userId, RoleType roleType);
    
    /**
     * Get users by role ID
     */
    List<User> getUsersByRole(Long roleId);
    
    /**
     * Get users by role type
     */
    List<User> getUsersByRoleType(RoleType roleType);
    
    // ========== ROLE VALIDATION METHODS ==========
    
    /**
     * Check if user has specific role type
     */
    boolean hasRole(Long userId, RoleType roleType);
    
    /**
     * Check if user has any of the specified roles
     */
    boolean hasAnyRole(Long userId, RoleType... roleTypes);
    
    /**
     * Check if user has all specified roles
     */
    boolean hasAllRoles(Long userId, RoleType... roleTypes);
    
    /**
     * Validate role assignment for user
     */
    void validateRoleAssignment(Long userId, RoleType roleType);
    
    /**
     * Validate all user roles
     */
    void validateUserRoles(Long userId);
    
    // ========== ROLE HIERARCHY METHODS ==========
    
    /**
     * Check if role1 has higher or equal privilege than role2
     */
    boolean hasHigherOrEqualPrivilege(Role role1, Role role2);
    
    /**
     * Check if roleType1 has higher or equal privilege than roleType2
     */
    boolean hasHigherOrEqualPrivilege(RoleType roleType1, RoleType roleType2);
    
    /**
     * Get user's highest privilege role
     */
    Optional<Role> getHighestPrivilegeRole(Long userId);
    
    /**
     * Get user's primary role type
     */
    Optional<RoleType> getPrimaryRoleType(Long userId);
    
    // ========== PLANT ASSIGNMENT METHODS ==========
    
    /**
     * Get user's assigned plant codes
     */
    List<String> getUserPlantCodes(Long userId);
    
    /**
     * Assign plants to user
     */
    void assignPlantsToUser(Long userId, List<String> plantCodes);
    
    /**
     * Add plant to user
     */
    void addPlantToUser(Long userId, String plantCode);
    
    /**
     * Remove plant from user
     */
    void removePlantFromUser(Long userId, String plantCode);
    
    /**
     * Set primary plant for user
     */
    void setPrimaryPlant(Long userId, String plantCode);
    
    /**
     * Get user's primary plant
     */
    String getPrimaryPlant(Long userId);
    
    /**
     * Check if user has access to plant
     */
    boolean hasPlantAccess(Long userId, String plantCode);
    
    /**
     * Validate user's plant assignments
     */
    void validatePlantAssignments(Long userId);
    
    // ========== UTILITY METHODS ==========
    
    /**
     * Initialize default roles
     */
    void initializeDefaultRoles();
    
    /**
     * Get role statistics
     */
    Map<String, Object> getRoleStatistics();
    
    /**
     * Synchronize role names with role types
     */
    void synchronizeRoleNames();
    
    /**
     * Find roles by user ID
     */
    List<Role> findByUserId(Long userId);
}