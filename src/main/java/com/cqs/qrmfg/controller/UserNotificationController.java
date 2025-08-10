package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.model.NotificationPreference;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.repository.NotificationPreferenceRepository;
import com.cqs.qrmfg.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserNotificationController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserNotificationController.class);
    
    @Autowired
    private NotificationPreferenceRepository preferenceRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private com.cqs.qrmfg.service.DefaultNotificationPreferenceService defaultNotificationPreferenceService;
    
    @GetMapping("/{username}/notification-preferences")
    public ResponseEntity<Map<String, Object>> getUserNotificationPreferences(
            @PathVariable String username,
            Authentication authentication) {
        
        try {
            // Get user email from user table
            String userEmail = getUserEmailFromDatabase(username);
            
            // Ensure user has default notification preferences (all enabled)
            defaultNotificationPreferenceService.ensureDefaultPreferencesForUser(username, userEmail);
            
            // Get user's preferences
            List<NotificationPreference> preferences = preferenceRepository.findActivePreferencesForUser(username);
            
            // Debug logging
            logger.info("Found {} notification preferences for user {}", preferences.size(), username);
            for (NotificationPreference pref : preferences) {
                logger.info("Preference: {} -> {} (enabled: {})", pref.getNotificationType(), pref.getEmail(), pref.getEnabled());
            }
            
            // Log the response values for debugging
            logger.info("Returning preferences - workflowCreated: {}, workflowExtended: {}, workflowCompleted: {}", 
                       hasPreference(preferences, "WORKFLOW_CREATED"),
                       hasPreferenceForPlants(preferences),
                       hasPreference(preferences, "WORKFLOW_COMPLETED"));
            
            // Convert to frontend format
            Map<String, Object> response = new HashMap<>();
            
            // Email preferences - check actual preferences or default to enabled
            Map<String, Object> emailPrefs = new HashMap<>();
            emailPrefs.put("enabled", true);
            emailPrefs.put("address", userEmail);
            emailPrefs.put("workflowCreated", hasPreference(preferences, "WORKFLOW_CREATED"));
            emailPrefs.put("workflowExtended", hasPreferenceForPlants(preferences));
            emailPrefs.put("workflowCompleted", hasPreference(preferences, "WORKFLOW_COMPLETED"));
            emailPrefs.put("workflowStateChanged", hasPreference(preferences, "WORKFLOW_STATE_CHANGED"));
            emailPrefs.put("workflowOverdue", hasPreference(preferences, "WORKFLOW_OVERDUE"));
            emailPrefs.put("queryRaised", hasPreference(preferences, "QUERY_RAISED"));
            emailPrefs.put("queryResolved", hasPreference(preferences, "QUERY_RESOLVED"));
            emailPrefs.put("queryAssigned", hasPreference(preferences, "QUERY_ASSIGNED"));
            emailPrefs.put("queryOverdue", hasPreference(preferences, "QUERY_OVERDUE"));
            
            
            // General preferences
            Map<String, Object> generalPrefs = new HashMap<>();
            generalPrefs.put("frequency", "immediate");
            Map<String, Object> quietHours = new HashMap<>();
            quietHours.put("enabled", false);
            quietHours.put("start", "18:00");
            quietHours.put("end", "08:00");
            generalPrefs.put("quietHours", quietHours);
            
            response.put("email", emailPrefs);
            response.put("general", generalPrefs);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get notification preferences for user {}: {}", username, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    

    
    @PostMapping("/notification-preferences/initialize-all")
    public ResponseEntity<Map<String, Object>> initializeAllUserPreferences() {
        
        try {
            logger.info("Manually initializing default notification preferences for all users");
            defaultNotificationPreferenceService.ensureDefaultPreferencesForAllUsers();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Default notification preferences initialized for all users");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to initialize notification preferences for all users: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to initialize notification preferences");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/notification-preferences/debug-all")
    public ResponseEntity<Map<String, Object>> debugAllNotificationPreferences() {
        
        try {
            List<NotificationPreference> allPrefs = preferenceRepository.findAll();
            List<NotificationPreference> workflowCreatedPrefs = preferenceRepository.findActivePreferencesForType("WORKFLOW_CREATED");
            List<NotificationPreference> roleCqsPrefs = preferenceRepository.findActivePreferencesForType("ROLE_CQS");
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalPreferences", allPrefs.size());
            response.put("workflowCreatedPreferences", workflowCreatedPrefs.size());
            response.put("roleCqsPreferences", roleCqsPrefs.size());
            response.put("allPreferences", allPrefs);
            response.put("workflowCreatedDetails", workflowCreatedPrefs);
            response.put("roleCqsDetails", roleCqsPrefs);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to debug notification preferences: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/{username}/notification-preferences/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupUserNotificationPreferences(
            @PathVariable String username,
            Authentication authentication) {
        
        try {
            logger.info("Cleaning up notification preferences for user: {}", username);
            
            cleanupDuplicatePreferences(username);
            
            List<NotificationPreference> preferences = preferenceRepository.findActivePreferencesForUser(username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification preferences cleaned up successfully");
            response.put("remainingPreferences", preferences.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to cleanup notification preferences for user {}: {}", username, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to cleanup notification preferences");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PutMapping("/{username}/notification-preferences")
    public ResponseEntity<Map<String, Object>> updateUserNotificationPreferences(
            @PathVariable String username,
            @RequestBody Map<String, Object> preferences,
            Authentication authentication) {
        
        try {
            logger.info("Updating notification preferences for user: {}", username);
            
            // Extract email preferences
            @SuppressWarnings("unchecked")
            Map<String, Object> emailPrefs = (Map<String, Object>) preferences.get("email");
            
            if (emailPrefs != null) {
                String emailAddress = (String) emailPrefs.get("address");
                Boolean enabled = (Boolean) emailPrefs.get("enabled");
                
                if (enabled && emailAddress != null && !emailAddress.trim().isEmpty()) {
                    // Update or create notification preferences based on user selections
                    updateUserPreferences(username, emailAddress, emailPrefs);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification preferences updated successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to update notification preferences for user {}: {}", username, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to update notification preferences");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Get user email from the user table
     */
    private String getUserEmailFromDatabase(String username) {
        logger.info("Looking up email for user: {}", username);
        
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String email = user.getEmail();
            logger.info("Found user {} with email: {}", username, email);
            
            if (email != null && !email.trim().isEmpty()) {
                return email;
            } else {
                logger.warn("User {} found but email is null or empty", username);
            }
        } else {
            logger.warn("User {} not found in database", username);
        }
        
        // Fallback to username@company.com if no email found
        logger.warn("No email found for user {}, using fallback: {}@company.com", username, username);
        return username + "@company.com";
    }
    
    private boolean hasPreference(List<NotificationPreference> preferences, String notificationType) {
        boolean hasIt = preferences.stream()
                .anyMatch(pref -> notificationType.equals(pref.getNotificationType()));
        logger.debug("Checking preference {}: {}", notificationType, hasIt);
        return hasIt;
    }
    
    private boolean hasPreferenceForPlants(List<NotificationPreference> preferences) {
        return preferences.stream()
                .anyMatch(pref -> pref.getNotificationType().startsWith("PLANT_"));
    }
    
    /**
     * Create default notification preferences for a user
     */
    private void createDefaultPreferencesForUser(String username, String emailAddress) {
        try {
            logger.info("Creating default notification preferences for user: {}", username);
            
            // Clean up any existing duplicates first
            cleanupDuplicatePreferences(username);
            
            // Get user's assigned plants
            List<String> userPlants = getUserAssignedPlants(username);
            
            // Create all notification types as enabled by default
            String[] notificationTypes = {
                "WORKFLOW_CREATED", "WORKFLOW_COMPLETED", "WORKFLOW_STATE_CHANGED", 
                "WORKFLOW_OVERDUE", "QUERY_RAISED", "QUERY_RESOLVED", 
                "QUERY_ASSIGNED", "QUERY_OVERDUE"
            };
            
            for (String notificationType : notificationTypes) {
                createPreference(username, notificationType, emailAddress);
            }
            
            // Create plant-specific preferences for user's assigned plants
            for (String plantCode : userPlants) {
                createPreference(username, "PLANT_" + plantCode, emailAddress);
            }
            
            logger.info("Created default notification preferences for user: {}", username);
            
        } catch (Exception e) {
            logger.error("Failed to create default preferences for user {}: {}", username, e.getMessage(), e);
        }
    }
    
    /**
     * Clean up duplicate notification preferences for a user
     */
    private void cleanupDuplicatePreferences(String username) {
        try {
            List<NotificationPreference> allPrefs = preferenceRepository.findByUsername(username);
            Map<String, List<NotificationPreference>> groupedByType = allPrefs.stream()
                .collect(Collectors.groupingBy(pref -> pref.getNotificationType() + "_" + pref.getChannel()));
            
            int deletedCount = 0;
            for (List<NotificationPreference> duplicates : groupedByType.values()) {
                if (duplicates.size() > 1) {
                    // Keep the first one, delete the rest
                    for (int i = 1; i < duplicates.size(); i++) {
                        preferenceRepository.delete(duplicates.get(i));
                        deletedCount++;
                    }
                }
            }
            
            if (deletedCount > 0) {
                logger.info("Cleaned up {} duplicate notification preferences for user: {}", deletedCount, username);
            }
            
        } catch (Exception e) {
            logger.warn("Failed to cleanup duplicate preferences for user {}: {}", username, e.getMessage());
        }
    }
    
    private void updateUserPreferences(String username, String emailAddress, Map<String, Object> emailPrefs) {
        // Clear existing preferences for this user
        preferenceRepository.deleteByUsername(username);
        
        // Get user's assigned plants for plant-specific notifications
        List<String> userPlants = getUserAssignedPlants(username);
        
        // Create new preferences based on selections
        if ((Boolean) emailPrefs.getOrDefault("workflowCreated", false)) {
            createPreference(username, "WORKFLOW_CREATED", emailAddress);
        }
        
        if ((Boolean) emailPrefs.getOrDefault("workflowExtended", false)) {
            // Create preferences for user's assigned plants
            for (String plantCode : userPlants) {
                createPreference(username, "PLANT_" + plantCode, emailAddress);
            }
        }
        
        if ((Boolean) emailPrefs.getOrDefault("workflowCompleted", false)) {
            createPreference(username, "WORKFLOW_COMPLETED", emailAddress);
        }
        
        if ((Boolean) emailPrefs.getOrDefault("workflowStateChanged", false)) {
            createPreference(username, "WORKFLOW_STATE_CHANGED", emailAddress);
        }
        
        if ((Boolean) emailPrefs.getOrDefault("workflowOverdue", false)) {
            createPreference(username, "WORKFLOW_OVERDUE", emailAddress);
        }
        
        if ((Boolean) emailPrefs.getOrDefault("queryRaised", false)) {
            logger.info("Creating QUERY_RAISED preference for user: {} with email: {}", username, emailAddress);
            createPreference(username, "QUERY_RAISED", emailAddress);
        } else {
            logger.info("queryRaised is false or missing for user: {}, value: {}", username, emailPrefs.get("queryRaised"));
        }
        
        if ((Boolean) emailPrefs.getOrDefault("queryResolved", false)) {
            createPreference(username, "QUERY_RESOLVED", emailAddress);
        }
        
        if ((Boolean) emailPrefs.getOrDefault("queryAssigned", false)) {
            createPreference(username, "QUERY_ASSIGNED", emailAddress);
        }
        
        if ((Boolean) emailPrefs.getOrDefault("queryOverdue", false)) {
            createPreference(username, "QUERY_OVERDUE", emailAddress);
        }
        
        logger.info("Updated notification preferences for user: {}", username);
    }
    
    /**
     * Get user's assigned plants from the user table
     */
    private List<String> getUserAssignedPlants(String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            return userOptional.get().getAssignedPlantsList();
        }
        
        // Fallback to common plants if no user found
        return Arrays.asList("1001", "1002", "1103", "1104", "1116");
    }
    
    private void createPreference(String username, String notificationType, String emailAddress) {
        try {
            // Check if preference already exists to avoid duplicates
            if (preferenceRepository.existsByUsernameAndNotificationTypeAndChannel(username, notificationType, "EMAIL")) {
                logger.debug("Preference already exists for user {} type {}, skipping", username, notificationType);
                return;
            }
            
            NotificationPreference preference = new NotificationPreference();
            preference.setUsername(username);
            preference.setNotificationType(notificationType);
            preference.setChannel("EMAIL");
            preference.setEmail(emailAddress);
            preference.setEnabled(true);
            preference.setCreatedBy(username);
            preference.setUpdatedBy(username);
            
            preferenceRepository.save(preference);
            logger.info("Created notification preference: {} -> {} ({})", username, notificationType, emailAddress);
            
        } catch (Exception e) {
            logger.warn("Failed to create preference for user {} type {}: {}", username, notificationType, e.getMessage());
        }
    }
}