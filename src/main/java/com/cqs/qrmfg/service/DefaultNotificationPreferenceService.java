package com.cqs.qrmfg.service;

import com.cqs.qrmfg.config.DefaultNotificationPreferencesConfig;
import com.cqs.qrmfg.model.NotificationPreference;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.repository.NotificationPreferenceRepository;
import com.cqs.qrmfg.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class DefaultNotificationPreferenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultNotificationPreferenceService.class);
    
    @Autowired
    private NotificationPreferenceRepository preferenceRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DefaultNotificationPreferencesConfig defaultConfig;
    
    /**
     * Ensure all users have default notification preferences (all enabled)
     */
    public void ensureDefaultPreferencesForAllUsers() {
        logger.info("Ensuring default notification preferences for all users...");
        
        List<User> allUsers = userRepository.findAll();
        int usersProcessed = 0;
        int preferencesCreated = 0;
        
        for (User user : allUsers) {
            if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                int created = ensureRoleBasedPreferencesForUser(user.getUsername(), user.getEmail());
                if (created > 0) {
                    preferencesCreated += created;
                    usersProcessed++;
                }
            }
        }
        
        logger.info("Processed {} users, created {} default notification preferences", usersProcessed, preferencesCreated);
    }
    
    /**
     * Ensure role-based preferences exist for a user (even if they have other preferences)
     */
    public int ensureRoleBasedPreferencesForUser(String username, String emailAddress) {
        logger.info("Ensuring role-based notification preferences for user: {} with email: {}", username, emailAddress);
        
        int created = 0;
        List<String> userRoles = getUserRoles(username);
        
        // Create role-based preferences for user's roles if they don't exist
        for (String role : userRoles) {
            String roleNotificationType = "ROLE_" + role;
            if (!preferenceRepository.existsByUsernameAndNotificationTypeAndChannel(username, roleNotificationType, "EMAIL")) {
                if (createPreference(username, roleNotificationType, emailAddress)) {
                    created++;
                    logger.info("Created {} preference for user: {}", roleNotificationType, username);
                }
            } else {
                logger.debug("User {} already has {} preference", username, roleNotificationType);
            }
        }
        
        // Also ensure basic notification preferences exist
        String[] notificationTypes = {
            "WORKFLOW_CREATED", "WORKFLOW_COMPLETED", "WORKFLOW_STATE_CHANGED", 
            "WORKFLOW_OVERDUE", "QUERY_RAISED", "QUERY_RESOLVED", 
            "QUERY_ASSIGNED", "QUERY_OVERDUE"
        };
        
        for (String notificationType : notificationTypes) {
            if (!preferenceRepository.existsByUsernameAndNotificationTypeAndChannel(username, notificationType, "EMAIL")) {
                if (createPreference(username, notificationType, emailAddress)) {
                    created++;
                }
            }
        }
        
        logger.info("Created {} new notification preferences for user: {}", created, username);
        return created;
    }
    
    /**
     * Create default notification preferences for a newly created user
     * This method should be called when a new user is created in the system
     */
    public void createDefaultPreferencesForNewUser(String username, String emailAddress) {
        logger.info("Creating default notification preferences for new user: {}", username);
        
        try {
            int created = ensureDefaultPreferencesForUser(username, emailAddress);
            if (created > 0) {
                logger.info("Successfully created {} default notification preferences for new user: {}", created, username);
            } else {
                logger.info("User {} already has notification preferences, skipping creation", username);
            }
        } catch (Exception e) {
            logger.error("Failed to create default notification preferences for new user {}: {}", username, e.getMessage(), e);
        }
    }
    
    /**
     * Ensure a specific user has default notification preferences based on admin configuration
     */
    public int ensureDefaultPreferencesForUser(String username, String emailAddress) {
        logger.info("Creating default notification preferences for user: {} with email: {}", username, emailAddress);
        
        int created = 0;
        
        // Get user's assigned plants and roles
        List<String> userPlants = getUserAssignedPlants(username);
        List<String> userRoles = getUserRoles(username);
        
        // Get the primary role for determining default preferences
        String primaryRole = userRoles.isEmpty() ? null : userRoles.get(0);
        
        // Create notification preferences based on admin configuration
        Map<String, Boolean> defaultPreferences = defaultConfig.getPreferences();
        
        for (Map.Entry<String, Boolean> entry : defaultPreferences.entrySet()) {
            String notificationType = entry.getKey();
            
            // Check if this notification type should be enabled for the user's role
            boolean shouldEnable = primaryRole != null ? 
                defaultConfig.isEnabledByDefaultForRole(primaryRole, notificationType) :
                defaultConfig.isEnabledByDefault(notificationType);
            
            if (createPreferenceWithEnabledStatus(username, notificationType, emailAddress, shouldEnable)) {
                created++;
                logger.debug("Created {} preference for user: {} (enabled: {})", notificationType, username, shouldEnable);
            }
        }
        
        // Create plant-specific preferences for user's assigned plants (always enabled for assigned plants)
        for (String plantCode : userPlants) {
            if (createPreferenceWithEnabledStatus(username, "PLANT_" + plantCode, emailAddress, true)) {
                created++;
            }
        }
        
        // Create role-based preferences for user's roles (always enabled for role-based notifications)
        for (String role : userRoles) {
            if (createPreferenceWithEnabledStatus(username, "ROLE_" + role, emailAddress, true)) {
                created++;
                logger.info("Created ROLE_{} preference for user: {}", role, username);
            }
        }
        
        logger.info("Created {} default notification preferences for user: {} (primary role: {})", created, username, primaryRole);
        return created;
    }
    
    /**
     * Create a notification preference if it doesn't already exist (always enabled)
     */
    private boolean createPreference(String username, String notificationType, String emailAddress) {
        return createPreferenceWithEnabledStatus(username, notificationType, emailAddress, true);
    }
    
    /**
     * Create a notification preference with specific enabled status if it doesn't already exist
     */
    private boolean createPreferenceWithEnabledStatus(String username, String notificationType, String emailAddress, boolean enabled) {
        try {
            // Check if preference already exists to avoid duplicates
            if (preferenceRepository.existsByUsernameAndNotificationTypeAndChannel(username, notificationType, "EMAIL")) {
                return false;
            }
            
            NotificationPreference preference = new NotificationPreference();
            preference.setUsername(username);
            preference.setNotificationType(notificationType);
            preference.setChannel("EMAIL");
            preference.setEmail(emailAddress);
            preference.setEnabled(enabled);
            preference.setCreatedBy("system");
            preference.setUpdatedBy("system");
            
            preferenceRepository.save(preference);
            logger.debug("Created notification preference: {} -> {} ({}) - enabled: {}", username, notificationType, emailAddress, enabled);
            return true;
            
        } catch (Exception e) {
            logger.warn("Failed to create preference for user {} type {}: {}", username, notificationType, e.getMessage());
            return false;
        }
    }
    
    /**
     * Get user's assigned plants from the user table
     */
    private List<String> getUserAssignedPlants(String username) {
        try {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                return user.getAssignedPlantsList();
            }
        } catch (Exception e) {
            logger.warn("Failed to get assigned plants for user {}: {}", username, e.getMessage());
        }
        
        // Fallback to common plants if no user found or error
        return Arrays.asList("1001", "1002", "1103", "1104", "1116", "1102");
    }
    
    /**
     * Get user's roles from the user table
     */
    private List<String> getUserRoles(String username) {
        try {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null && user.getRoles() != null && !user.getRoles().isEmpty()) {
                List<String> roleTypes = new ArrayList<>();
                for (com.cqs.qrmfg.model.Role role : user.getRoles()) {
                    String roleName = role.getName();
                    if (roleName != null) {
                        // Convert role like "ROLE_CQS_USER" to "CQS"
                        if (roleName.startsWith("ROLE_") && roleName.endsWith("_USER")) {
                            String roleType = roleName.substring(5, roleName.length() - 5); // Remove "ROLE_" and "_USER"
                            roleTypes.add(roleType);
                        } else if (roleName.startsWith("ROLE_")) {
                            String roleType = roleName.substring(5); // Remove "ROLE_"
                            roleTypes.add(roleType);
                        } else {
                            roleTypes.add(roleName);
                        }
                    }
                }
                return roleTypes;
            }
        } catch (Exception e) {
            logger.warn("Failed to get roles for user {}: {}", username, e.getMessage());
        }
        
        return new ArrayList<>(); // Return empty list if no roles found
    }
}