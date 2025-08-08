package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.model.Role;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.enums.RoleType;
import com.cqs.qrmfg.repository.RoleRepository;
import com.cqs.qrmfg.repository.UserRepository;
import com.cqs.qrmfg.service.RoleService;
import com.cqs.qrmfg.util.RBACConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced RoleService implementation with RBAC-specific functionality
 * for role management, assignment, validation, and plant access control.
 */
@Service
@Transactional
public class RoleServiceImpl implements RoleService {
    
    private static final Logger logger = LoggerFactory.getLogger(RoleServiceImpl.class);
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    // ========== BASIC CRUD OPERATIONS ==========
    
    @Override
    public Role save(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        
        // Validate role before saving
        validateRole(role);
        
        // Sync role name with role type if needed
        if (role.getRoleType() != null) {
            role.syncRoleNameWithType();
        }
        
        logger.debug("Saving role: {}", role.getName());
        return roleRepository.save(role);
    }
    
    @Override
    public Role update(Role role) {
        if (role == null || role.getId() == null) {
            throw new IllegalArgumentException("Role and role ID cannot be null");
        }
        
        // Verify role exists
        if (!roleRepository.existsById(role.getId())) {
            throw new IllegalArgumentException("Role not found with ID: " + role.getId());
        }
        
        // Validate role before updating
        validateRole(role);
        
        // Sync role name with role type if needed
        if (role.getRoleType() != null) {
            role.syncRoleNameWithType();
        }
        
        logger.debug("Updating role: {}", role.getName());
        return roleRepository.save(role);
    }
    
    @Override
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Role ID cannot be null");
        }
        
        Optional<Role> roleOpt = roleRepository.findById(id);
        if (!roleOpt.isPresent()) {
            throw new IllegalArgumentException("Role not found with ID: " + id);
        }
        
        Role role = roleOpt.get();
        
        // Check if role is assigned to any users
        List<User> usersWithRole = getUsersByRole(id);
        if (!usersWithRole.isEmpty()) {
            throw new IllegalStateException("Cannot delete role that is assigned to users. " +
                "Users with this role: " + usersWithRole.size());
        }
        
        logger.debug("Deleting role: {}", role.getName());
        roleRepository.deleteById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return roleRepository.findById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        return roleRepository.findByName(name.trim());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Role> findAll() {
        return roleRepository.findAll();
    }
    
    // ========== RBAC-SPECIFIC ROLE MANAGEMENT ==========
    
    @Override
    public Role createRole(RoleType roleType, String description) {
        if (roleType == null) {
            throw new IllegalArgumentException(RBACConstants.ERROR_INVALID_ROLE_TYPE);
        }
        
        // Check if role already exists
        if (existsByRoleType(roleType)) {
            throw new IllegalArgumentException("Role already exists for type: " + roleType.getRoleName());
        }
        
        Role role = new Role(roleType.getRoleName(), description, roleType);
        role.setEnabled(true);
        
        logger.info("Creating new role: {} ({})", roleType.getRoleName(), roleType.getDescription());
        return save(role);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findByRoleType(RoleType roleType) {
        if (roleType == null) {
            return Optional.empty();
        }
        return roleRepository.findByName(roleType.getRoleName());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Role> findByRoleType(RoleType roleType, boolean includeDisabled) {
        if (roleType == null) {
            return Collections.emptyList();
        }
        
        Optional<Role> role = roleRepository.findByName(roleType.getRoleName());
        if (role.isPresent()) {
            if (includeDisabled || role.get().isEnabled()) {
                return Collections.singletonList(role.get());
            }
        }
        return Collections.emptyList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Role> findAllEnabled() {
        return findAll().stream()
            .filter(Role::isEnabled)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return false;
        }
        return roleRepository.existsByName(roleName.trim());
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByRoleType(RoleType roleType) {
        if (roleType == null) {
            return false;
        }
        return roleRepository.existsByName(roleType.getRoleName());
    }
    
    // ========== ROLE ASSIGNMENT OPERATIONS ==========
    
    @Override
    public void assignRoleToUser(Long userId, Long roleId) {
        if (userId == null || roleId == null) {
            throw new IllegalArgumentException("User ID and Role ID cannot be null");
        }
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException(RBACConstants.ERROR_USER_NOT_FOUND));
        
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException(RBACConstants.ERROR_ROLE_NOT_FOUND));
        
        // Validate role assignment
        validateRoleAssignment(userId, role.getRoleType());
        
        // Add role to user
        user.getRoles().add(role);
        userRepository.save(user);
        
        logger.info("Assigned role {} to user {}", role.getName(), user.getUsername());
    }
    
    @Override
    public void assignRoleToUser(Long userId, RoleType roleType) {
        if (roleType == null) {
            throw new IllegalArgumentException(RBACConstants.ERROR_INVALID_ROLE_TYPE);
        }
        
        Role role = findByRoleType(roleType)
            .orElseThrow(() -> new IllegalArgumentException("Role not found for type: " + roleType.getRoleName()));
        
        assignRoleToUser(userId, role.getId());
    }
    
    @Override
    public void removeRoleFromUser(Long userId, Long roleId) {
        if (userId == null || roleId == null) {
            throw new IllegalArgumentException("User ID and Role ID cannot be null");
        }
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException(RBACConstants.ERROR_USER_NOT_FOUND));
        
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException(RBACConstants.ERROR_ROLE_NOT_FOUND));
        
        // Remove role from user
        boolean removed = user.getRoles().removeIf(r -> r.getId().equals(roleId));
        
        if (removed) {
            userRepository.save(user);
            logger.info("Removed role {} from user {}", role.getName(), user.getUsername());
        }
    }
    
    @Override
    public void removeRoleFromUser(Long userId, RoleType roleType) {
        if (roleType == null) {
            throw new IllegalArgumentException(RBACConstants.ERROR_INVALID_ROLE_TYPE);
        }
        
        Role role = findByRoleType(roleType)
            .orElseThrow(() -> new IllegalArgumentException("Role not found for type: " + roleType.getRoleName()));
        
        removeRoleFromUser(userId, role.getId());
    }
    
    @Override
    public void replaceUserRole(Long userId, RoleType roleType) {
        if (userId == null || roleType == null) {
            throw new IllegalArgumentException("User ID and Role Type cannot be null");
        }
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException(RBACConstants.ERROR_USER_NOT_FOUND));
        
        Role newRole = findByRoleType(roleType)
            .orElseThrow(() -> new IllegalArgumentException("Role not found for type: " + roleType.getRoleName()));
        
        // Validate role assignment
        validateRoleAssignment(userId, roleType);
        
        // Clear existing roles and assign new role
        user.getRoles().clear();
        user.getRoles().add(newRole);
        userRepository.save(user);
        
        logger.info("Replaced user {} roles with {}", user.getUsername(), newRole.getName());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<User> getUsersByRole(Long roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }
        return roleRepository.findUsersByRoleId(roleId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<User> getUsersByRoleType(RoleType roleType) {
        if (roleType == null) {
            return Collections.emptyList();
        }
        // Temporary implementation: find role by name and get its users
        Optional<Role> role = roleRepository.findByName(roleType.getRoleName());
        if (role.isPresent()) {
            return roleRepository.findUsersByRoleId(role.get().getId());
        }
        return Collections.emptyList();
    }
    
    // ========== ROLE VALIDATION METHODS ==========
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasRole(Long userId, RoleType roleType) {
        if (userId == null || roleType == null) {
            return false;
        }
        // Temporary implementation: check if user has role by name
        Optional<Role> role = roleRepository.findByName(roleType.getRoleName());
        if (role.isPresent()) {
            List<User> usersWithRole = roleRepository.findUsersByRoleId(role.get().getId());
            return usersWithRole.stream().anyMatch(user -> user.getId().equals(userId));
        }
        return false;
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasAnyRole(Long userId, RoleType... roleTypes) {
        if (userId == null || roleTypes == null || roleTypes.length == 0) {
            return false;
        }
        
        for (RoleType roleType : roleTypes) {
            if (hasRole(userId, roleType)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasAllRoles(Long userId, RoleType... roleTypes) {
        if (userId == null || roleTypes == null || roleTypes.length == 0) {
            return false;
        }
        
        for (RoleType roleType : roleTypes) {
            if (!hasRole(userId, roleType)) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public void validateRoleAssignment(Long userId, RoleType roleType) {
        if (userId == null || roleType == null) {
            throw new IllegalArgumentException("User ID and Role Type cannot be null");
        }
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException(RBACConstants.ERROR_USER_NOT_FOUND));
        
        // Check if role type is valid
        if (!RBACConstants.isValidRoleType(roleType)) {
            throw new IllegalStateException(RBACConstants.ERROR_INVALID_ROLE_TYPE);
        }
        
        // For plant roles, validate plant assignments
        if (RBACConstants.requiresPlantAssignment(roleType)) {
            List<String> assignedPlants = user.getAssignedPlantsList();
            if (assignedPlants.isEmpty()) {
                throw new IllegalStateException(RBACConstants.ERROR_PLANT_ASSIGNMENT_REQUIRED);
            }
        }
    }
    
    @Override
    public void validateUserRoles(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException(RBACConstants.ERROR_USER_NOT_FOUND));
        
        // Validate each role assignment
        for (Role role : user.getRoles()) {
            if (role.getRoleType() != null) {
                validateRoleAssignment(userId, role.getRoleType());
            }
        }
        
        // Validate plant assignments
        user.validatePlantAssignments();
    }
    
    // ========== ROLE HIERARCHY METHODS ==========
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasHigherOrEqualPrivilege(Role role1, Role role2) {
        if (role1 == null || role2 == null) {
            return false;
        }
        return role1.hasHigherOrEqualPrivilege(role2);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasHigherOrEqualPrivilege(RoleType roleType1, RoleType roleType2) {
        return RoleType.hasHigherOrEqualPrivilege(roleType1, roleType2);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Role> getHighestPrivilegeRole(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        
        // Temporary implementation: get user and return first role (if any)
        // This is a simplified version without hierarchy ordering
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getRoles() != null && !user.getRoles().isEmpty()) {
                return Optional.of(user.getRoles().iterator().next());
            }
        } catch (Exception e) {
            logger.warn("Error getting user roles for userId: {}", userId, e);
        }
        return Optional.empty();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<RoleType> getPrimaryRoleType(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Optional.empty();
        }
        
        return Optional.ofNullable(user.getPrimaryRoleType());
    }
    
    // ========== PLANT ASSIGNMENT METHODS ==========
    
    @Override
    @Transactional(readOnly = true)
    public List<String> getUserPlantCodes(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Collections.emptyList();
        }
        
        return user.getAssignedPlantsList();
    }
    
    @Override
    public void assignPlantsToUser(Long userId, List<String> plantCodes) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException(RBACConstants.ERROR_USER_NOT_FOUND));
        
        // Validate plant codes
        if (plantCodes != null && !RBACConstants.areValidPlantCodes(plantCodes)) {
            throw new IllegalArgumentException(RBACConstants.ERROR_INVALID_PLANT_CODE);
        }
        
        // Check if user's role supports plant assignment
        if (plantCodes != null && !plantCodes.isEmpty() && !user.supportsPlantFiltering()) {
            throw new IllegalStateException("User's role does not support plant assignment");
        }
        
        user.setAssignedPlantsList(plantCodes);
        userRepository.save(user);
        
        logger.info("Assigned plants {} to user {}", plantCodes, user.getUsername());
    }
    
    @Override
    public void addPlantToUser(Long userId, String plantCode) {
        if (userId == null || plantCode == null) {
            throw new IllegalArgumentException("User ID and Plant Code cannot be null");
        }
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException(RBACConstants.ERROR_USER_NOT_FOUND));
        
        user.addPlantAssignment(plantCode);
        userRepository.save(user);
        
        logger.info("Added plant {} to user {}", plantCode, user.getUsername());
    }
    
    @Override
    public void removePlantFromUser(Long userId, String plantCode) {
        if (userId == null || plantCode == null) {
            throw new IllegalArgumentException("User ID and Plant Code cannot be null");
        }
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException(RBACConstants.ERROR_USER_NOT_FOUND));
        
        user.removePlantAssignment(plantCode);
        userRepository.save(user);
        
        logger.info("Removed plant {} from user {}", plantCode, user.getUsername());
    }
    
    @Override
    public void setPrimaryPlant(Long userId, String plantCode) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException(RBACConstants.ERROR_USER_NOT_FOUND));
        
        user.setPrimaryPlant(plantCode);
        userRepository.save(user);
        
        logger.info("Set primary plant {} for user {}", plantCode, user.getUsername());
    }
    
    @Override
    @Transactional(readOnly = true)
    public String getPrimaryPlant(Long userId) {
        if (userId == null) {
            return null;
        }
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }
        
        return user.getPrimaryPlant();
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasPlantAccess(Long userId, String plantCode) {
        if (userId == null || plantCode == null) {
            return false;
        }
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        
        return user.hasPlantAccess(plantCode);
    }
    
    @Override
    public void validatePlantAssignments(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException(RBACConstants.ERROR_USER_NOT_FOUND));
        
        user.validatePlantAssignments();
    }
    
    // ========== UTILITY METHODS ==========
    
    @Override
    public void initializeDefaultRoles() {
        logger.info("Initializing default RBAC roles...");
        
        for (RoleType roleType : RoleType.values()) {
            if (!existsByRoleType(roleType)) {
                createRole(roleType, roleType.getDescription());
                logger.info("Created default role: {}", roleType.getRoleName());
            } else {
                logger.debug("Role already exists: {}", roleType.getRoleName());
            }
        }
        
        logger.info("Default roles initialization completed");
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getRoleStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Total roles count
        long totalRoles = roleRepository.count();
        stats.put("totalRoles", totalRoles);
        
        // Enabled roles count
        long enabledRoles = roleRepository.findByEnabledTrue().size();
        stats.put("enabledRoles", enabledRoles);
        
        // Temporary implementation: count roles by checking each role type
        Map<String, Long> roleTypeStats = new HashMap<>();
        for (RoleType roleType : RoleType.values()) {
            long count = roleRepository.existsByName(roleType.getRoleName()) ? 1L : 0L;
            roleTypeStats.put(roleType.getRoleName(), count);
        }
        stats.put("rolesByType", roleTypeStats);
        
        // User counts by role type
        Map<String, Integer> usersByRole = new HashMap<>();
        for (RoleType roleType : RoleType.values()) {
            int userCount = getUsersByRoleType(roleType).size();
            usersByRole.put(roleType.getRoleName(), userCount);
        }
        stats.put("usersByRole", usersByRole);
        
        return stats;
    }
    
    @Override
    public void synchronizeRoleNames() {
        logger.info("Synchronizing role names with role types...");
        
        List<Role> allRoles = findAll();
        int updatedCount = 0;
        
        for (Role role : allRoles) {
            if (role.getRoleType() != null && !role.isRoleNameValid()) {
                String oldName = role.getName();
                role.syncRoleNameWithType();
                save(role);
                updatedCount++;
                logger.info("Updated role name from '{}' to '{}'", oldName, role.getName());
            }
        }
        
        logger.info("Synchronized {} role names", updatedCount);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Role> findByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Collections.emptyList();
        }
        
        return new ArrayList<>(user.getRoles());
    }
    
    // ========== PRIVATE HELPER METHODS ==========
    
    /**
     * Validate a role object before saving
     */
    private void validateRole(Role role) {
        if (role.getName() == null || role.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Role name cannot be null or empty");
        }
        
        if (role.getName().length() > RBACConstants.MAX_ROLE_NAME_LENGTH) {
            throw new IllegalArgumentException("Role name exceeds maximum length: " + RBACConstants.MAX_ROLE_NAME_LENGTH);
        }
        
        if (role.getDescription() != null && role.getDescription().length() > RBACConstants.MAX_ROLE_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Role description exceeds maximum length: " + RBACConstants.MAX_ROLE_DESCRIPTION_LENGTH);
        }
        
        if (role.getRoleType() != null && !RBACConstants.isValidRoleType(role.getRoleType())) {
            throw new IllegalArgumentException(RBACConstants.ERROR_INVALID_ROLE_TYPE);
        }
        
        // Check for duplicate role names (excluding current role if updating)
        Optional<Role> existingRole = findByName(role.getName());
        if (existingRole.isPresent() && !existingRole.get().getId().equals(role.getId())) {
            throw new IllegalArgumentException("Role name already exists: " + role.getName());
        }
        
        // Check for duplicate role types (excluding current role if updating)
        if (role.getRoleType() != null) {
            Optional<Role> existingRoleType = findByRoleType(role.getRoleType());
            if (existingRoleType.isPresent() && !existingRoleType.get().getId().equals(role.getId())) {
                throw new IllegalArgumentException("Role type already exists: " + role.getRoleType().getRoleName());
            }
        }
    }
} 