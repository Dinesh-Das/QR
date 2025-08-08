package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for User management
 */
public interface UserService {
    
    /**
     * Find user by username
     */
    User findByUsername(String username);
    
    /**
     * Find user by email
     */
    User findByEmail(String email);
    
    /**
     * Find user by ID
     */
    User findById(Long id);
    
    /**
     * Save user
     */
    User save(User user);
    
    /**
     * Find all users
     */
    List<User> findAll();
    
    /**
     * Delete user
     */
    void delete(User user);
    
    /**
     * Delete user by ID
     */
    void deleteById(Long id);
    
    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);
    
    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);
    
    /**
     * Create new user with encoded password
     */
    User createUser(String username, String email, String password);
    
    /**
     * Update user password
     */
    void updatePassword(User user, String newPassword);
    
    /**
     * Find users by role
     */
    List<User> findByRole(String roleName);
    
    /**
     * Find users assigned to a specific plant
     */
    List<User> findByPlantCode(String plantCode);
    
    /**
     * Get all user role assignments
     */
    List<Object> getAllUserRoleAssignments();
    
    /**
     * Update user roles
     */
    void updateUserRoles(Long userId, List<Long> roleIds);
    
    /**
     * Get notification preferences for user
     */
    Map<String, Object> getNotificationPreferences(String username);
    
    /**
     * Update notification preferences for user
     */
    void updateNotificationPreferences(String username, Map<String, Object> preferences);
    
    /**
     * Get user assigned plants by username
     */
    List<String> getUserAssignedPlants(String username);
    
    /**
     * Get user primary plant by username
     */
    String getUserPrimaryPlant(String username);
    
    /**
     * Update user plant assignments
     */
    void updateUserPlantAssignments(String username, List<String> plantCodes, String primaryPlant);
    
    /**
     * Check if user is assigned to plant
     */
    boolean isUserAssignedToPlant(String username, String plantCode);
    
    /**
     * Encode password using BCrypt
     */
    String encodePassword(String plainPassword);
}