package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.config.DefaultNotificationPreferencesConfig;
import com.cqs.qrmfg.service.DefaultNotificationPreferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/notification-defaults")
public class NotificationDefaultsController {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationDefaultsController.class);
    
    @Autowired
    private DefaultNotificationPreferencesConfig defaultConfig;
    
    @Autowired
    private DefaultNotificationPreferenceService defaultNotificationPreferenceService;
    
    /**
     * Get current default notification preferences configuration
     */
    @GetMapping("/config")
    public Map<String, Object> getDefaultConfig() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            response.put("success", true);
            response.put("generalDefaults", defaultConfig.getPreferences());
            response.put("roleBasedDefaults", defaultConfig.getRoleBasedPreferences());
            response.put("message", "Default notification preferences configuration retrieved successfully");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to retrieve default configuration: " + e.getMessage());
            logger.error("Failed to retrieve default configuration: {}", e.getMessage(), e);
        }
        
        return response;
    }
    
    /**
     * Update default preferences for a specific role
     */
    @PostMapping("/update-role-defaults")
    public Map<String, Object> updateRoleDefaults(
            @RequestParam String role,
            @RequestBody Map<String, Boolean> preferences) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Update the role-based preferences
            Map<String, Map<String, Boolean>> roleBasedPrefs = defaultConfig.getRoleBasedPreferences();
            roleBasedPrefs.put(role.toUpperCase(), preferences);
            
            response.put("success", true);
            response.put("message", "Default preferences updated for role: " + role);
            response.put("role", role);
            response.put("updatedPreferences", preferences);
            
            logger.info("Updated default notification preferences for role {}: {}", role, preferences);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to update role defaults: " + e.getMessage());
            logger.error("Failed to update role defaults for {}: {}", role, e.getMessage(), e);
        }
        
        return response;
    }
    
    /**
     * Update general default preferences
     */
    @PostMapping("/update-general-defaults")
    public Map<String, Object> updateGeneralDefaults(@RequestBody Map<String, Boolean> preferences) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Update the general preferences
            defaultConfig.getPreferences().putAll(preferences);
            
            response.put("success", true);
            response.put("message", "General default preferences updated successfully");
            response.put("updatedPreferences", preferences);
            
            logger.info("Updated general default notification preferences: {}", preferences);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to update general defaults: " + e.getMessage());
            logger.error("Failed to update general defaults: {}", e.getMessage(), e);
        }
        
        return response;
    }
    
    /**
     * Create default preferences for a specific user (admin can trigger this manually)
     */
    @PostMapping("/create-for-user")
    public Map<String, Object> createDefaultsForUser(
            @RequestParam String username,
            @RequestParam String email) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            defaultNotificationPreferenceService.createDefaultPreferencesForNewUser(username, email);
            
            response.put("success", true);
            response.put("message", "Default notification preferences created for user: " + username);
            response.put("username", username);
            response.put("email", email);
            
            logger.info("Admin manually created default notification preferences for user: {}", username);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to create defaults for user: " + e.getMessage());
            logger.error("Failed to create defaults for user {}: {}", username, e.getMessage(), e);
        }
        
        return response;
    }
    
    /**
     * Preview what default preferences would be created for a user with a specific role
     */
    @GetMapping("/preview")
    public Map<String, Object> previewDefaultsForRole(@RequestParam String role) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String upperRole = role.toUpperCase();
            logger.info("Preview request for role: {} (uppercase: {})", role, upperRole);
            
            // Handle role name variations (JVC_USER -> JVC)
            String normalizedRole = upperRole;
            if (upperRole.endsWith("_USER")) {
                normalizedRole = upperRole.substring(0, upperRole.length() - 5);
                logger.info("Normalized role from {} to {}", upperRole, normalizedRole);
            }
            
            // Debug: Log all available role-based preferences
            Map<String, Map<String, Boolean>> allRolePrefs = defaultConfig.getRoleBasedPreferences();
            logger.info("Available role-based preferences: {}", allRolePrefs.keySet());
            
            // Try both the original role and normalized role
            Map<String, Boolean> rolePreferences = defaultConfig.getPreferencesForRole(normalizedRole);
            if (rolePreferences.isEmpty() || rolePreferences.equals(defaultConfig.getPreferences())) {
                // If normalized role didn't work, try the original
                rolePreferences = defaultConfig.getPreferencesForRole(upperRole);
            }
            
            logger.info("Retrieved preferences for role {} (normalized: {}): {}", upperRole, normalizedRole, rolePreferences);
            
            response.put("success", true);
            response.put("role", role);
            response.put("defaultPreferences", rolePreferences);
            response.put("message", "Preview of default preferences for role: " + role);
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("requestedRole", role);
            debugInfo.put("upperRole", upperRole);
            debugInfo.put("availableRoles", allRolePrefs.keySet());
            debugInfo.put("foundRoleSpecificPrefs", allRolePrefs.containsKey(upperRole));
            response.put("debug", debugInfo);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to preview defaults: " + e.getMessage());
            logger.error("Failed to preview defaults for role {}: {}", role, e.getMessage(), e);
        }
        
        return response;
    }
}