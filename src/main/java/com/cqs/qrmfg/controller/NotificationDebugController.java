package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.service.NotificationService;
import com.cqs.qrmfg.repository.NotificationPreferenceRepository;
import com.cqs.qrmfg.model.NotificationPreference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/debug/notifications")
public class NotificationDebugController {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationDebugController.class);
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private NotificationPreferenceRepository preferenceRepository;
    
    @GetMapping("/status")
    public Map<String, Object> getNotificationStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Check notification service status
            status.put("notificationEnabled", notificationService.isNotificationEnabled());
            status.put("emailEnabled", notificationService.isEmailEnabled());            
            // Check preferences count
            List<NotificationPreference> allPrefs = preferenceRepository.findAll();
            status.put("totalPreferences", allPrefs.size());
            
            // Check role-based preferences
            List<NotificationPreference> cqsPrefs = preferenceRepository.findActivePreferencesForType("ROLE_CQS");
            List<NotificationPreference> jvcPrefs = preferenceRepository.findActivePreferencesForType("ROLE_JVC");
            List<NotificationPreference> techPrefs = preferenceRepository.findActivePreferencesForType("ROLE_TECH");
            
            status.put("cqsRolePreferences", cqsPrefs.size());
            status.put("jvcRolePreferences", jvcPrefs.size());
            status.put("techRolePreferences", techPrefs.size());
            
            // Check plant preferences
            List<NotificationPreference> plant1001Prefs = preferenceRepository.findActivePreferencesForType("PLANT_1001");
            status.put("plant1001Preferences", plant1001Prefs.size());
            
            status.put("success", true);
            
        } catch (Exception e) {
            status.put("success", false);
            status.put("error", e.getMessage());
            logger.error("Failed to get notification status: {}", e.getMessage(), e);
        }
        
        return status;
    }
    
    @PostMapping("/test-workflow-created")
    public Map<String, Object> testWorkflowCreatedNotification() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Create a mock workflow object for testing
            com.cqs.qrmfg.model.Workflow mockWorkflow = new com.cqs.qrmfg.model.Workflow();
            mockWorkflow.setId(999L);
            mockWorkflow.setMaterialCode("TEST-001");
            mockWorkflow.setMaterialName("Test Material");
            mockWorkflow.setAssignedPlant("1001");
            mockWorkflow.setInitiatedBy("test_user");
            mockWorkflow.setState(com.cqs.qrmfg.model.WorkflowState.JVC_PENDING);
            mockWorkflow.setCreatedAt(java.time.LocalDateTime.now());
            
            // Test the notification
            notificationService.notifyWorkflowCreated(mockWorkflow);
            
            response.put("success", true);
            response.put("message", "Test workflow created notification sent");
            
            Map<String, Object> testWorkflow = new HashMap<>();
            testWorkflow.put("materialCode", mockWorkflow.getMaterialCode());
            testWorkflow.put("assignedPlant", mockWorkflow.getAssignedPlant());
            testWorkflow.put("initiatedBy", mockWorkflow.getInitiatedBy());
            response.put("testWorkflow", testWorkflow);
            
            logger.info("Test workflow created notification sent successfully");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to send test notification: " + e.getMessage());
            logger.error("Failed to send test workflow created notification: {}", e.getMessage(), e);
        }
        
        return response;
    }
    
    @PostMapping("/test-workflow-extended")
    public Map<String, Object> testWorkflowExtendedNotification() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Create a mock workflow object for testing
            com.cqs.qrmfg.model.Workflow mockWorkflow = new com.cqs.qrmfg.model.Workflow();
            mockWorkflow.setId(999L);
            mockWorkflow.setMaterialCode("TEST-002");
            mockWorkflow.setMaterialName("Test Material Extended");
            mockWorkflow.setAssignedPlant("1001");
            mockWorkflow.setInitiatedBy("jvc_user");
            mockWorkflow.setState(com.cqs.qrmfg.model.WorkflowState.PLANT_PENDING);
            mockWorkflow.setCreatedAt(java.time.LocalDateTime.now());
            
            // Test the notification
            notificationService.notifyWorkflowExtended(mockWorkflow, "jvc_user");
            
            response.put("success", true);
            response.put("message", "Test workflow extended notification sent");
            
            Map<String, Object> testWorkflow = new HashMap<>();
            testWorkflow.put("materialCode", mockWorkflow.getMaterialCode());
            testWorkflow.put("assignedPlant", mockWorkflow.getAssignedPlant());
            testWorkflow.put("extendedBy", "jvc_user");
            response.put("testWorkflow", testWorkflow);
            
            logger.info("Test workflow extended notification sent successfully");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to send test notification: " + e.getMessage());
            logger.error("Failed to send test workflow extended notification: {}", e.getMessage(), e);
        }
        
        return response;
    }
    
    @GetMapping("/preferences/all")
    public Map<String, Object> getAllPreferences() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<NotificationPreference> preferences = preferenceRepository.findAll();
            response.put("success", true);
            response.put("preferences", preferences);
            response.put("count", preferences.size());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to fetch preferences: " + e.getMessage());
        }
        
        return response;
    }
}