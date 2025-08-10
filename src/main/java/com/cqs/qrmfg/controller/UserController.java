package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.model.NotificationPreference;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.repository.NotificationPreferenceRepository;
import com.cqs.qrmfg.service.NotificationService;
import com.cqs.qrmfg.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    @Autowired
    private UserService userService;
    
    @Autowired
    private com.cqs.qrmfg.repository.UserRepository userRepository;
    
    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private com.cqs.qrmfg.config.DefaultNotificationPreferencesConfig defaultNotificationConfig;
    
    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    
    @Autowired
    private com.cqs.qrmfg.service.DefaultNotificationPreferenceService defaultNotificationPreferenceService;

    @GetMapping
    public List<User> getAllUsers() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }
    
    @PostMapping
    public User createUser(@RequestBody User user) {
        // Encode password before saving
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        // Set default values for required fields - BOTH enabled and status are needed!
        user.setEnabled(true);  // This is crucial for login to work
        if (user.getStatus() == null) {
            user.setStatus("ACTIVE");
        }
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(java.time.LocalDateTime.now());
        }
        if (user.getUpdatedAt() == null) {
            user.setUpdatedAt(java.time.LocalDateTime.now());
        }
        
        // Use JPA repository instead of JDBC service for consistency
        User savedUser = userRepository.save(user);
        
        // Create default notification preferences for the new user
        try {
            if (savedUser.getEmail() != null && !savedUser.getEmail().trim().isEmpty()) {
                defaultNotificationPreferenceService.createDefaultPreferencesForNewUser(
                    savedUser.getUsername(), 
                    savedUser.getEmail()
                );
            }
        } catch (Exception e) {
            // Log the error but don't fail user creation
            System.err.println("Warning: Failed to create default notification preferences for user " + 
                             savedUser.getUsername() + ": " + e.getMessage());
        }
        
        return savedUser;
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        // Find existing user
        User existingUser = userService.findById(id);
        if (existingUser == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Update fields
        existingUser.setUsername(user.getUsername());
        existingUser.setEmail(user.getEmail());
        existingUser.setEnabled(user.isEnabled());
        existingUser.setAssignedPlants(user.getAssignedPlants());
        existingUser.setPrimaryPlant(user.getPrimaryPlant());
        existingUser.setUpdatedAt(java.time.LocalDateTime.now());
        
        // Ensure status is set properly for login to work
        if (user.getStatus() != null) {
            existingUser.setStatus(user.getStatus());
        } else if (existingUser.getStatus() == null) {
            existingUser.setStatus("ACTIVE");
        }
        
        // Only update password if provided
        if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        

        
        return ResponseEntity.ok(userService.save(existingUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    // Notification preference endpoints
    @GetMapping("/{username}/notification-preferences")
    public ResponseEntity<Map<String, Object>> getNotificationPreferences(@PathVariable String username) {
        List<NotificationPreference> preferences = notificationPreferenceRepository.findActivePreferencesForUser(username);
        
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> email = new HashMap<>();
        Map<String, Object> general = new HashMap<>();
        
        // Get user's role to determine defaults
        Optional<User> userOpt = userRepository.findByUsername(username);
        String userRole = "GENERAL"; // fallback
        if (userOpt.isPresent() && userOpt.get().getRoles() != null && !userOpt.get().getRoles().isEmpty()) {
            String roleName = userOpt.get().getRoles().iterator().next().getName();
            // Convert role name to match config format (e.g., "ROLE_JVC_USER" -> "JVC")
            if (roleName.startsWith("ROLE_") && roleName.endsWith("_USER")) {
                userRole = roleName.substring(5, roleName.length() - 5);
            } else if (roleName.startsWith("ROLE_")) {
                userRole = roleName.substring(5);
            } else {
                userRole = roleName;
            }
        }
        
        // Get defaults from admin configuration
        Map<String, Boolean> roleDefaults = defaultNotificationConfig.getPreferencesForRole(userRole);
        
        // Initialize default values using admin configuration
        email.put("enabled", true);
        email.put("address", userOpt.isPresent() && userOpt.get().getEmail() != null ? 
                  userOpt.get().getEmail() : username + "@company.com");
        
        // Use admin-configured defaults instead of hardcoded values
        email.put("workflowCreated", roleDefaults.getOrDefault("WORKFLOW_CREATED", true));
        email.put("workflowExtended", roleDefaults.getOrDefault("WORKFLOW_EXTENDED", true));
        email.put("workflowCompleted", roleDefaults.getOrDefault("WORKFLOW_COMPLETED", true));
        email.put("workflowStateChanged", roleDefaults.getOrDefault("WORKFLOW_STATE_CHANGED", false));
        email.put("workflowOverdue", roleDefaults.getOrDefault("WORKFLOW_OVERDUE", true));
        email.put("queryRaised", roleDefaults.getOrDefault("QUERY_RAISED", true));
        email.put("queryResolved", roleDefaults.getOrDefault("QUERY_RESOLVED", true));
        email.put("queryAssigned", roleDefaults.getOrDefault("QUERY_ASSIGNED", true));
        email.put("queryOverdue", roleDefaults.getOrDefault("QUERY_OVERDUE", true));
        
        Map<String, Object> quietHours = new HashMap<>();
        quietHours.put("enabled", false);
        quietHours.put("start", "18:00");
        quietHours.put("end", "08:00");
        
        general.put("frequency", "immediate");
        general.put("quietHours", quietHours);
        
        // Override with actual preferences from database
        for (NotificationPreference pref : preferences) {
            if ("EMAIL".equalsIgnoreCase(pref.getChannel())) {
                email.put("enabled", pref.isEnabled());
                if (pref.getEmail() != null) {
                    email.put("address", pref.getEmail());
                }
                
                // Map specific notification types based on the preference type
                String notificationType = pref.getNotificationType();
                if (notificationType != null) {
                    switch (notificationType) {
                        case "WORKFLOW_CREATED":
                            email.put("workflowCreated", pref.isEnabled());
                            break;
                        case "WORKFLOW_COMPLETED":
                            email.put("workflowCompleted", pref.isEnabled());
                            break;
                        case "WORKFLOW_STATE_CHANGED":
                            email.put("workflowStateChanged", pref.isEnabled());
                            break;
                        case "WORKFLOW_OVERDUE":
                            email.put("workflowOverdue", pref.isEnabled());
                            break;
                        case "QUERY_RAISED":
                            email.put("queryRaised", pref.isEnabled());
                            break;
                        case "QUERY_RESOLVED":
                            email.put("queryResolved", pref.isEnabled());
                            break;
                        case "QUERY_ASSIGNED":
                            email.put("queryAssigned", pref.isEnabled());
                            break;
                        case "QUERY_OVERDUE":
                            email.put("queryOverdue", pref.isEnabled());
                            break;
                    }
                }
            }
        }
        
        result.put("email", email);
        result.put("general", general);
        
        return ResponseEntity.ok(result);
    }
    
    @PutMapping("/{username}/notification-preferences")
    public ResponseEntity<Map<String, Object>> updateNotificationPreferences(
            @PathVariable String username, 
            @RequestBody Map<String, Object> preferences) {
        
        try {
            // Get email preferences
            Map<String, Object> emailPrefs = (Map<String, Object>) preferences.get("email");
            if (emailPrefs == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Email preferences are required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            String emailAddress = (String) emailPrefs.get("address");
            if (emailAddress == null || emailAddress.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Email address is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Update or create notification preferences for each type
            String[] notificationTypes = {
                "WORKFLOW_CREATED", "WORKFLOW_COMPLETED", "WORKFLOW_STATE_CHANGED", 
                "WORKFLOW_OVERDUE", "QUERY_RAISED", "QUERY_RESOLVED", 
                "QUERY_ASSIGNED", "QUERY_OVERDUE"
            };
            
            for (String notificationType : notificationTypes) {
                // Get the preference setting from the request
                String prefKey = convertToFrontendKey(notificationType);
                Boolean isEnabled = (Boolean) emailPrefs.get(prefKey);
                if (isEnabled == null) isEnabled = false;
                
                // Find existing preference or create new one
                NotificationPreference pref = notificationPreferenceRepository
                    .findByUsernameAndNotificationTypeAndChannel(username, notificationType, "EMAIL");
                
                if (pref == null) {
                    pref = new NotificationPreference();
                    pref.setUsername(username);
                    pref.setNotificationType(notificationType);
                    pref.setChannel("EMAIL");
                    pref.setCreatedBy(username);
                }
                
                pref.setEnabled(isEnabled);
                pref.setEmail(emailAddress);
                pref.setUpdatedBy(username);
                
                notificationPreferenceRepository.save(pref);
            }
            
            return ResponseEntity.ok(preferences);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to update preferences: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @GetMapping("/{username}/notifications")
    public ResponseEntity<List<Map<String, Object>>> getUserNotifications(@PathVariable String username) {
        try {
            // This would typically fetch from a notifications table
            // For now, return empty list as placeholder
            List<Map<String, Object>> notifications = new java.util.ArrayList<>();
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PutMapping("/{username}/{notificationId}/read")
    public ResponseEntity<Void> markNotificationAsRead(
            @PathVariable String username,
            @PathVariable Long notificationId) {
        try {
            // This would typically update a notifications table
            // For now, just log the action
            System.out.println("Marked notification " + notificationId + " as read for user: " + username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PutMapping("/{username}/read-all")
    public ResponseEntity<Void> markAllNotificationsAsRead(@PathVariable String username) {
        try {
            // This would typically update all notifications for the user
            // For now, just log the action
            System.out.println("Marked all notifications as read for user: " + username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @DeleteMapping("/{username}/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable String username,
            @PathVariable Long notificationId) {
        try {
            // This would typically delete from a notifications table
            // For now, just log the action
            System.out.println("Deleted notification " + notificationId + " for user: " + username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @DeleteMapping("/{username}/clear")
    public ResponseEntity<Void> clearAllNotifications(@PathVariable String username) {
        try {
            // This would typically delete all notifications for the user
            // For now, just log the action
            System.out.println("Cleared all notifications for user: " + username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Plant assignment endpoints
    @GetMapping("/{username}/plants")
    public ResponseEntity<Map<String, Object>> getUserPlantAssignments(@PathVariable String username) {
        try {
            List<String> assignedPlants = userService.getUserAssignedPlants(username);
            String primaryPlant = userService.getUserPrimaryPlant(username);
            
            // If no plant assignments found, provide default for testing
            if (assignedPlants.isEmpty()) {
                assignedPlants = java.util.Arrays.asList("1102");
                primaryPlant = "1102";
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("assignedPlants", assignedPlants);
            result.put("primaryPlant", primaryPlant);
            result.put("effectivePlant", primaryPlant != null ? primaryPlant : (assignedPlants.isEmpty() ? null : assignedPlants.get(0)));
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Fallback response if database columns don't exist yet
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("assignedPlants", java.util.Arrays.asList("1102"));
            fallback.put("primaryPlant", "1102");
            fallback.put("effectivePlant", "1102");
            return ResponseEntity.ok(fallback);
        }
    }
    
    @PutMapping("/{username}/plants")
    public ResponseEntity<String> updateUserPlantAssignments(
            @PathVariable String username,
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> plantCodes = (List<String>) request.get("assignedPlants");
            String primaryPlant = (String) request.get("primaryPlant");
            
            userService.updateUserPlantAssignments(username, plantCodes, primaryPlant);
            return ResponseEntity.ok("Plant assignments updated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update plant assignments: " + e.getMessage());
        }
    }
    
    @GetMapping("/{username}/plants/{plantCode}/check")
    public ResponseEntity<Map<String, Boolean>> checkUserPlantAssignment(
            @PathVariable String username,
            @PathVariable String plantCode) {
        try {
            boolean isAssigned = userService.isUserAssignedToPlant(username, plantCode);
            Map<String, Boolean> result = new HashMap<>();
            result.put("isAssigned", isAssigned);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Convert backend notification type to frontend key
     * e.g., "WORKFLOW_CREATED" -> "workflowCreated"
     */
    private String convertToFrontendKey(String notificationType) {
        switch (notificationType) {
            case "WORKFLOW_CREATED": return "workflowCreated";
            case "WORKFLOW_COMPLETED": return "workflowCompleted";
            case "WORKFLOW_STATE_CHANGED": return "workflowStateChanged";
            case "WORKFLOW_OVERDUE": return "workflowOverdue";
            case "QUERY_RAISED": return "queryRaised";
            case "QUERY_RESOLVED": return "queryResolved";
            case "QUERY_ASSIGNED": return "queryAssigned";
            case "QUERY_OVERDUE": return "queryOverdue";
            default: return notificationType.toLowerCase();
        }
    }
} 