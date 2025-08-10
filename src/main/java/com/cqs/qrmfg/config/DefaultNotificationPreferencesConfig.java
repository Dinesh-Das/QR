package com.cqs.qrmfg.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "app.notifications.defaults")
public class DefaultNotificationPreferencesConfig {
    
    private Map<String, Boolean> preferences = new HashMap<>();
    private Map<String, Map<String, Boolean>> roleBasedPreferences = new HashMap<>();
    
    public DefaultNotificationPreferencesConfig() {
        // Set default values
        initializeDefaults();
    }
    
    private void initializeDefaults() {
        // Default notification preferences (can be overridden in application.properties)
        preferences.put("WORKFLOW_CREATED", true);
        preferences.put("WORKFLOW_COMPLETED", true);
        preferences.put("WORKFLOW_STATE_CHANGED", false);
        preferences.put("WORKFLOW_OVERDUE", true);
        preferences.put("QUERY_RAISED", true);
        preferences.put("QUERY_RESOLVED", true);
        preferences.put("QUERY_ASSIGNED", true);
        preferences.put("QUERY_OVERDUE", true);
        
        // Role-based default preferences - these will be overridden by application.properties
        Map<String, Boolean> cqsDefaults = new HashMap<>();
        cqsDefaults.put("WORKFLOW_CREATED", true);
        cqsDefaults.put("WORKFLOW_COMPLETED", true);
        cqsDefaults.put("WORKFLOW_STATE_CHANGED", true);
        cqsDefaults.put("WORKFLOW_OVERDUE", true);
        cqsDefaults.put("QUERY_RAISED", true);
        cqsDefaults.put("QUERY_RESOLVED", true);
        cqsDefaults.put("QUERY_ASSIGNED", true);
        cqsDefaults.put("QUERY_OVERDUE", true);
        roleBasedPreferences.put("CQS", cqsDefaults);
        
        Map<String, Boolean> jvcDefaults = new HashMap<>();
        jvcDefaults.put("WORKFLOW_CREATED", false);
        jvcDefaults.put("WORKFLOW_COMPLETED", true);
        jvcDefaults.put("WORKFLOW_STATE_CHANGED", false);
        jvcDefaults.put("WORKFLOW_OVERDUE", true);
        jvcDefaults.put("QUERY_RAISED", false);
        jvcDefaults.put("QUERY_RESOLVED", false);
        jvcDefaults.put("QUERY_ASSIGNED", false);
        jvcDefaults.put("QUERY_OVERDUE", false);
        roleBasedPreferences.put("JVC", jvcDefaults);
        
        // Also add JVC_USER mapping for compatibility
        roleBasedPreferences.put("JVC_USER", jvcDefaults);
        
        Map<String, Boolean> plantDefaults = new HashMap<>();
        plantDefaults.put("WORKFLOW_CREATED", false);
        plantDefaults.put("WORKFLOW_COMPLETED", false);
        plantDefaults.put("WORKFLOW_STATE_CHANGED", true);
        plantDefaults.put("WORKFLOW_OVERDUE", false);
        plantDefaults.put("QUERY_RAISED", false);
        plantDefaults.put("QUERY_RESOLVED", true);
        plantDefaults.put("QUERY_ASSIGNED", false);
        plantDefaults.put("QUERY_OVERDUE", false);
        roleBasedPreferences.put("PLANT", plantDefaults);
        
        Map<String, Boolean> techDefaults = new HashMap<>();
        techDefaults.put("WORKFLOW_CREATED", false);
        techDefaults.put("WORKFLOW_COMPLETED", false);
        techDefaults.put("WORKFLOW_STATE_CHANGED", false);
        techDefaults.put("WORKFLOW_OVERDUE", false);
        techDefaults.put("QUERY_RAISED", true);
        techDefaults.put("QUERY_RESOLVED", false);
        techDefaults.put("QUERY_ASSIGNED", true);
        techDefaults.put("QUERY_OVERDUE", true);
        roleBasedPreferences.put("TECH", techDefaults);
    }
    
    public Map<String, Boolean> getPreferences() {
        return preferences;
    }
    
    public void setPreferences(Map<String, Boolean> preferences) {
        this.preferences = preferences;
    }
    
    public Map<String, Map<String, Boolean>> getRoleBasedPreferences() {
        return roleBasedPreferences;
    }
    
    public void setRoleBasedPreferences(Map<String, Map<String, Boolean>> roleBasedPreferences) {
        this.roleBasedPreferences = roleBasedPreferences;
    }
    
    /**
     * Get default preferences for a specific role
     */
    public Map<String, Boolean> getPreferencesForRole(String role) {
        return roleBasedPreferences.getOrDefault(role, preferences);
    }
    
    /**
     * Check if a notification type should be enabled by default for a role
     */
    public boolean isEnabledByDefaultForRole(String role, String notificationType) {
        Map<String, Boolean> rolePrefs = roleBasedPreferences.get(role);
        if (rolePrefs != null && rolePrefs.containsKey(notificationType)) {
            return rolePrefs.get(notificationType);
        }
        return preferences.getOrDefault(notificationType, false);
    }
    
    /**
     * Check if a notification type should be enabled by default (general)
     */
    public boolean isEnabledByDefault(String notificationType) {
        return preferences.getOrDefault(notificationType, false);
    }
}