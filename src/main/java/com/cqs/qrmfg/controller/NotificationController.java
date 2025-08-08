package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.model.NotificationPreference;
import com.cqs.qrmfg.service.NotificationService;
import com.cqs.qrmfg.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("hasRole('USER')")
public class NotificationController {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private UserService userService;
    
    /**
     * Get notification preferences for the current user
     */
    @GetMapping("/preferences")
    public ResponseEntity<Map<String, Object>> getNotificationPreferences(Principal principal) {
        try {
            String username = principal.getName();
            Map<String, Object> preferences = userService.getNotificationPreferences(username);
            return ResponseEntity.ok(preferences);
        } catch (Exception e) {
            logger.error("Failed to get notification preferences: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Update notification preferences for the current user
     */
    @PutMapping("/preferences")
    public ResponseEntity<Void> updateNotificationPreferences(
            @RequestBody Map<String, Object> preferences, 
            Principal principal) {
        try {
            String username = principal.getName();
            userService.updateNotificationPreferences(username, preferences);
            logger.info("Updated notification preferences for user: {}", username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Failed to update notification preferences: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get notification preferences for a specific user (admin only)
     */
    @GetMapping("/preferences/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserNotificationPreferences(@PathVariable String username) {
        try {
            Map<String, Object> preferences = userService.getNotificationPreferences(username);
            return ResponseEntity.ok(preferences);
        } catch (Exception e) {
            logger.error("Failed to get notification preferences for user {}: {}", username, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Update notification preferences for a specific user (admin only)
     */
    @PutMapping("/preferences/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateUserNotificationPreferences(
            @PathVariable String username,
            @RequestBody Map<String, Object> preferences) {
        try {
            userService.updateNotificationPreferences(username, preferences);
            logger.info("Admin updated notification preferences for user: {}", username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Failed to update notification preferences for user {}: {}", username, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Test notification system (admin only)
     */
    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testNotification(
            @RequestBody Map<String, String> testRequest,
            Principal principal) {
        try {
            String recipient = testRequest.get("recipient");
            String subject = testRequest.getOrDefault("subject", "Test Notification");
            String message = testRequest.getOrDefault("message", "This is a test notification from the QRMFG system.");
            
            if (recipient == null) {
                recipient = principal.getName();
            }
            
            notificationService.notifyUser(recipient, subject, message);
            
            logger.info("Test notification sent to {} by admin {}", recipient, principal.getName());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test notification sent successfully");
            response.put("recipient", recipient);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to send test notification: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to send test notification: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * Get notification system status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getNotificationStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("enabled", notificationService.isNotificationEnabled());
            status.put("emailEnabled", notificationService.isEmailEnabled());
            status.put("slackEnabled", notificationService.isSlackEnabled());
            status.put("templatesAvailable", new HashMap<String, Object>() {{
                put("workflowCreated", notificationService.isTemplateAvailable("notifications/workflow-created"));
                put("workflowExtended", notificationService.isTemplateAvailable("notifications/workflow-extended"));
                put("workflowCompleted", notificationService.isTemplateAvailable("notifications/workflow-completed"));
                put("workflowStateChanged", notificationService.isTemplateAvailable("notifications/workflow-state-changed"));
                put("queryRaised", notificationService.isTemplateAvailable("notifications/query-raised"));
                put("queryResolved", notificationService.isTemplateAvailable("notifications/query-resolved"));
                put("queryAssigned", notificationService.isTemplateAvailable("notifications/query-assigned"));
                put("queryOverdue", notificationService.isTemplateAvailable("notifications/query-overdue"));
            }});
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Failed to get notification status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get failed notifications (admin only)
     */
    @GetMapping("/failed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Object>> getFailedNotifications() {
        try {
            // Convert NotificationResult to generic objects for JSON serialization
            List<Object> failedNotifications = notificationService.getFailedNotifications()
                .stream()
                .map(result -> {
                    Map<String, Object> notificationMap = new HashMap<>();
                    notificationMap.put("success", result.isSuccess());
                    notificationMap.put("message", result.getMessage());
                    notificationMap.put("timestamp", System.currentTimeMillis());
                    return notificationMap;
                })
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(failedNotifications);
        } catch (Exception e) {
            logger.error("Failed to get failed notifications: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Clear failed notifications (admin only)
     */
    @DeleteMapping("/failed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> clearFailedNotifications(Principal principal) {
        try {
            notificationService.clearFailedNotifications();
            logger.info("Failed notifications cleared by admin: {}", principal.getName());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Failed to clear failed notifications: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Send manual notification (admin only)
     */
    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> sendManualNotification(
            @RequestBody Map<String, Object> notificationRequest,
            Principal principal) {
        try {
            String type = (String) notificationRequest.get("type"); // "user", "team", "plant", "admin"
            String target = (String) notificationRequest.get("target");
            String subject = (String) notificationRequest.get("subject");
            String message = (String) notificationRequest.get("message");
            
            if (type == null || target == null || subject == null || message == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Missing required fields: type, target, subject, message");
                return ResponseEntity.badRequest().body(response);
            }
            
            switch (type.toLowerCase()) {
                case "user":
                    notificationService.notifyUser(target, subject, message);
                    break;
                case "team":
                    notificationService.notifyTeam(target, subject, message);
                    break;
                case "plant":
                    notificationService.notifyPlant(target, subject, message);
                    break;
                case "admin":
                    notificationService.notifyAdmins(subject, message);
                    break;
                default:
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Invalid notification type. Must be: user, team, plant, or admin");
                    return ResponseEntity.badRequest().body(response);
            }
            
            logger.info("Manual notification sent by admin {} to {} ({}): {}", 
                       principal.getName(), type, target, subject);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification sent successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to send manual notification: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to send notification: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}