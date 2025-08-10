package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.model.NotificationPreference;
import com.cqs.qrmfg.repository.NotificationPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/notifications")
public class NotificationSetupController {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationSetupController.class);
    
    @Autowired
    private NotificationPreferenceRepository preferenceRepository;
    
    @RequestMapping(value = "/setup-plant-team", method = {RequestMethod.GET, RequestMethod.POST})
    public Map<String, Object> setupPlantTeamNotifications(
            @RequestParam String plantCode,
            @RequestParam String email) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Create notification preference for plant team
            NotificationPreference preference = new NotificationPreference();
            preference.setUsername("plant_team_" + plantCode.toLowerCase());
            preference.setNotificationType("TEAM_PLANT_" + plantCode);
            preference.setChannel("EMAIL");
            preference.setEmail(email);
            preference.setEnabled(true);
            preference.setCreatedBy("admin");
            preference.setUpdatedBy("admin");
            
            NotificationPreference saved = preferenceRepository.save(preference);
            
            response.put("success", true);
            response.put("message", "Plant team notification setup completed");
            response.put("plantCode", plantCode);
            response.put("email", email);
            response.put("preferenceId", saved.getId());
            
            logger.info("Created notification preference for plant {} with email {}", plantCode, email);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to setup plant team notifications: " + e.getMessage());
            logger.error("Failed to setup plant team notifications: {}", e.getMessage(), e);
        }
        
        return response;
    }
    
    @RequestMapping(value = "/setup-team", method = {RequestMethod.GET, RequestMethod.POST})
    public Map<String, Object> setupTeamNotifications(
            @RequestParam String teamType,
            @RequestParam String username,
            @RequestParam String email) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Create notification preference for team
            NotificationPreference preference = new NotificationPreference();
            preference.setUsername(username);
            preference.setNotificationType(teamType);
            preference.setChannel("EMAIL");
            preference.setEmail(email);
            preference.setEnabled(true);
            preference.setCreatedBy("admin");
            preference.setUpdatedBy("admin");
            
            NotificationPreference saved = preferenceRepository.save(preference);
            
            response.put("success", true);
            response.put("message", "Team notification setup completed");
            response.put("teamType", teamType);
            response.put("username", username);
            response.put("email", email);
            response.put("preferenceId", saved.getId());
            
            logger.info("Created notification preference for team {} user {} with email {}", teamType, username, email);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to setup team notifications: " + e.getMessage());
            logger.error("Failed to setup team notifications: {}", e.getMessage(), e);
        }
        
        return response;
    }
    
    @GetMapping("/preferences")
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
    
    @RequestMapping(value = "/setup-defaults", method = {RequestMethod.GET, RequestMethod.POST})
    public Map<String, Object> setupDefaultNotifications() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            int created = 0;
            
            // Setup role-based teams (NEW BUSINESS RULES)
            String[][] roleTeams = {
                {"ROLE_JVC", "jvc_role_team", "jvc@company.com"},
                {"ROLE_CQS", "cqs_role_team", "cqs@company.com"},
                {"ROLE_TECH", "tech_role_team", "tech@company.com"},
                {"ROLE_ADMIN", "admin_role_team", "admin@company.com"}
            };
            
            for (String[] team : roleTeams) {
                if (!preferenceRepository.existsByUsernameAndNotificationTypeAndChannel(team[1], team[0], "EMAIL")) {
                    NotificationPreference preference = new NotificationPreference();
                    preference.setUsername(team[1]);
                    preference.setNotificationType(team[0]);
                    preference.setChannel("EMAIL");
                    preference.setEmail(team[2]);
                    preference.setEnabled(true);
                    preference.setCreatedBy("system");
                    preference.setUpdatedBy("system");
                    
                    preferenceRepository.save(preference);
                    created++;
                }
            }
            
            // Setup plant-specific notifications for common plants
            String[] commonPlants = {"1001", "1002", "1103", "1104", "1116", "1102"};
            for (String plantCode : commonPlants) {
                String notificationType = "PLANT_" + plantCode;
                String username = "plant_team_" + plantCode.toLowerCase();
                String email = "plant" + plantCode + "@company.com";
                
                if (!preferenceRepository.existsByUsernameAndNotificationTypeAndChannel(username, notificationType, "EMAIL")) {
                    NotificationPreference preference = new NotificationPreference();
                    preference.setUsername(username);
                    preference.setNotificationType(notificationType);
                    preference.setChannel("EMAIL");
                    preference.setEmail(email);
                    preference.setEnabled(true);
                    preference.setCreatedBy("system");
                    preference.setUpdatedBy("system");
                    
                    preferenceRepository.save(preference);
                    created++;
                }
            }
            
            // Setup user-specific notifications for admin user
            String adminEmail = "admin@company.com";
            String[] adminNotificationTypes = {"WORKFLOW_CREATED", "WORKFLOW_COMPLETED", "WORKFLOW_STATE_CHANGED", "WORKFLOW_OVERDUE", 
                                             "QUERY_RAISED", "QUERY_RESOLVED", "QUERY_ASSIGNED", "QUERY_OVERDUE"};
            
            for (String notificationType : adminNotificationTypes) {
                if (!preferenceRepository.existsByUsernameAndNotificationTypeAndChannel("admin", notificationType, "EMAIL")) {
                    NotificationPreference preference = new NotificationPreference();
                    preference.setUsername("admin");
                    preference.setNotificationType(notificationType);
                    preference.setChannel("EMAIL");
                    preference.setEmail(adminEmail);
                    preference.setEnabled(true);
                    preference.setCreatedBy("system");
                    preference.setUpdatedBy("system");
                    
                    preferenceRepository.save(preference);
                    created++;
                }
            }
            
            response.put("success", true);
            response.put("message", "Default notification preferences created");
            response.put("created", created);
            response.put("note", "Created role-based, plant-specific, and admin user notifications");
            
            logger.info("Created {} default notification preferences", created);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to setup default notifications: " + e.getMessage());
            logger.error("Failed to setup default notifications: {}", e.getMessage(), e);
        }
        
        return response;
    }
    
    @RequestMapping(value = "/setup-plant-user", method = {RequestMethod.GET, RequestMethod.POST})
    public Map<String, Object> setupPlantUserNotifications(
            @RequestParam String plantCode,
            @RequestParam String username,
            @RequestParam String email) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Create notification preference for plant user
            NotificationPreference preference = new NotificationPreference();
            preference.setUsername(username);
            preference.setNotificationType("PLANT_" + plantCode);
            preference.setChannel("EMAIL");
            preference.setEmail(email);
            preference.setEnabled(true);
            preference.setCreatedBy("admin");
            preference.setUpdatedBy("admin");
            
            NotificationPreference saved = preferenceRepository.save(preference);
            
            response.put("success", true);
            response.put("message", "Plant user notification setup completed");
            response.put("plantCode", plantCode);
            response.put("username", username);
            response.put("email", email);
            response.put("preferenceId", saved.getId());
            
            logger.info("Created plant user notification preference for plant {} user {} with email {}", plantCode, username, email);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to setup plant user notifications: " + e.getMessage());
            logger.error("Failed to setup plant user notifications: {}", e.getMessage(), e);
        }
        
        return response;
    }
    
    @RequestMapping(value = "/setup-role-user", method = {RequestMethod.GET, RequestMethod.POST})
    public Map<String, Object> setupRoleUserNotifications(
            @RequestParam String role,
            @RequestParam String username,
            @RequestParam String email) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Create notification preference for role user
            NotificationPreference preference = new NotificationPreference();
            preference.setUsername(username);
            preference.setNotificationType("ROLE_" + role.toUpperCase());
            preference.setChannel("EMAIL");
            preference.setEmail(email);
            preference.setEnabled(true);
            preference.setCreatedBy("admin");
            preference.setUpdatedBy("admin");
            
            NotificationPreference saved = preferenceRepository.save(preference);
            
            response.put("success", true);
            response.put("message", "Role user notification setup completed");
            response.put("role", role);
            response.put("username", username);
            response.put("email", email);
            response.put("preferenceId", saved.getId());
            
            logger.info("Created role user notification preference for role {} user {} with email {}", role, username, email);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to setup role user notifications: " + e.getMessage());
            logger.error("Failed to setup role user notifications: {}", e.getMessage(), e);
        }
        
        return response;
    }
    
    @Autowired
    private com.cqs.qrmfg.service.DefaultNotificationPreferenceService defaultNotificationPreferenceService;
    
    @RequestMapping(value = "/fix-role-preferences", method = {RequestMethod.GET, RequestMethod.POST})
    public Map<String, Object> fixRoleBasedPreferences() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("Manually triggering role-based preference creation...");
            defaultNotificationPreferenceService.ensureDefaultPreferencesForAllUsers();
            
            response.put("success", true);
            response.put("message", "Role-based preferences have been created for all users");
            
            logger.info("Successfully completed role-based preference creation");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to create role-based preferences: " + e.getMessage());
            logger.error("Failed to create role-based preferences: {}", e.getMessage(), e);
        }
        
        return response;
    }
}