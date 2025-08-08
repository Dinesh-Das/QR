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
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

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
        return userRepository.save(user);
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
        Map<String, Object> slack = new HashMap<>();
        Map<String, Object> general = new HashMap<>();
        
        // Initialize default values
        email.put("enabled", true);
        email.put("address", username + "@company.com");
        email.put("workflowCreated", true);
        email.put("workflowExtended", true);
        email.put("workflowCompleted", true);
        email.put("workflowStateChanged", true);
        email.put("workflowOverdue", true);
        email.put("queryRaised", true);
        email.put("queryResolved", true);
        email.put("queryAssigned", true);
        email.put("queryOverdue", true);
        
        slack.put("enabled", false);
        slack.put("userId", "");
        slack.put("workflowCreated", false);
        slack.put("workflowExtended", true);
        slack.put("workflowCompleted", true);
        slack.put("workflowStateChanged", false);
        slack.put("workflowOverdue", true);
        slack.put("queryRaised", true);
        slack.put("queryResolved", true);
        slack.put("queryAssigned", true);
        slack.put("queryOverdue", true);
        
        Map<String, Object> quietHours = new HashMap<>();
        quietHours.put("enabled", false);
        quietHours.put("start", "18:00");
        quietHours.put("end", "08:00");
        
        general.put("frequency", "immediate");
        general.put("quietHours", quietHours);
        
        // Override with actual preferences
        for (NotificationPreference pref : preferences) {
            if ("EMAIL".equalsIgnoreCase(pref.getChannel())) {
                email.put("enabled", pref.isEnabled());
                if (pref.getEmail() != null) {
                    email.put("address", pref.getEmail());
                }
                // Set specific notification type preferences based on preference data
                if (pref.getPreferenceData() != null) {
                    // Parse JSON preference data if needed
                }
            } else if ("SLACK".equalsIgnoreCase(pref.getChannel())) {
                slack.put("enabled", pref.isEnabled());
                if (pref.getSlackId() != null) {
                    slack.put("userId", pref.getSlackId());
                }
            }
        }
        
        result.put("email", email);
        result.put("slack", slack);
        result.put("general", general);
        
        return ResponseEntity.ok(result);
    }
    
    @PutMapping("/{username}/notification-preferences")
    public ResponseEntity<Map<String, Object>> updateNotificationPreferences(
            @PathVariable String username, 
            @RequestBody Map<String, Object> preferences) {
        
        try {
            // Clear existing preferences
            List<NotificationPreference> existingPrefs = notificationPreferenceRepository.findActivePreferencesForUser(username);
            for (NotificationPreference pref : existingPrefs) {
                pref.setEnabled(false);
                notificationPreferenceRepository.save(pref);
            }
            
            // Create new email preferences
            Map<String, Object> emailPrefs = (Map<String, Object>) preferences.get("email");
            if (emailPrefs != null && (Boolean) emailPrefs.get("enabled")) {
                NotificationPreference emailPref = new NotificationPreference();
                emailPref.setUsername(username);
                emailPref.setChannel("EMAIL");
                emailPref.setEnabled(true);
                emailPref.setEmail((String) emailPrefs.get("address"));
                emailPref.setNotificationType("USER_" + username);
                notificationPreferenceRepository.save(emailPref);
            }
            
            // Create new slack preferences
            Map<String, Object> slackPrefs = (Map<String, Object>) preferences.get("slack");
            if (slackPrefs != null && (Boolean) slackPrefs.get("enabled")) {
                NotificationPreference slackPref = new NotificationPreference();
                slackPref.setUsername(username);
                slackPref.setChannel("SLACK");
                slackPref.setEnabled(true);
                slackPref.setSlackId((String) slackPrefs.get("userId"));
                slackPref.setNotificationType("USER_" + username);
                notificationPreferenceRepository.save(slackPref);
            }
            
            // Update notification service preferences
            notificationService.updateNotificationPreferences(username, preferences.toString());
            
            return ResponseEntity.ok(preferences);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
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
} 