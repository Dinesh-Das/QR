package com.cqs.qrmfg.config;

import com.cqs.qrmfg.enums.RoleType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RBAC-specific security configuration properties.
 * Provides centralized configuration for role-based access control settings.
 */
@Configuration
@ConfigurationProperties(prefix = "rbac.security")
public class RBACSecurityConfiguration {
    
    /**
     * Whether RBAC is enabled system-wide
     */
    private boolean enabled = true;
    
    /**
     * Whether to enable comprehensive access logging
     */
    private boolean auditEnabled = true;
    
    /**
     * Whether to enable plant-based data filtering
     */
    private boolean plantFilteringEnabled = true;
    
    /**
     * Whether to enable method-level security annotations
     */
    private boolean methodSecurityEnabled = true;
    
    /**
     * Default role for new users
     */
    private String defaultRole = RoleType.PLANT_ROLE.getRoleName();
    
    /**
     * Session timeout in seconds for different roles
     */
    private Map<String, Integer> roleSessionTimeouts = new HashMap<>();
    
    /**
     * Maximum concurrent sessions per role
     */
    private Map<String, Integer> maxConcurrentSessions = new HashMap<>();
    
    /**
     * URL patterns that bypass RBAC validation
     */
    private List<String> bypassPatterns = Arrays.asList(
        "/qrmfg/api/v1/auth/**",
        "/qrmfg/api/v1/public/**",
        "/static/**",
        "/css/**",
        "/js/**",
        "/*.html",
        "/*.ico",
        "/*.png",
        "/*.jpg",
        "/*.gif"
    );
    
    /**
     * Role-specific URL access patterns
     */
    private Map<String, List<String>> roleUrlPatterns = new HashMap<>();
    
    /**
     * Whether to enable strict role validation
     */
    private boolean strictRoleValidation = true;
    
    /**
     * Whether to enable plant code validation for PLANT_ROLE users
     */
    private boolean plantCodeValidation = true;
    
    /**
     * Maximum number of failed access attempts before logging warning
     */
    private int maxFailedAttempts = 5;
    
    /**
     * Whether to enable role hierarchy (admin > other roles)
     */
    private boolean roleHierarchyEnabled = true;
    
    public RBACSecurityConfiguration() {
        initializeDefaults();
    }
    
    /**
     * Initialize default configuration values
     */
    private void initializeDefaults() {
        // Default session timeouts (in seconds)
        roleSessionTimeouts.put(RoleType.ADMIN.getRoleName(), 28800); // 8 hours
        roleSessionTimeouts.put(RoleType.TECH_ROLE.getRoleName(), 14400); // 4 hours
        roleSessionTimeouts.put(RoleType.JVC_ROLE.getRoleName(), 10800); // 3 hours
        roleSessionTimeouts.put(RoleType.CQS_ROLE.getRoleName(), 10800); // 3 hours
        roleSessionTimeouts.put(RoleType.PLANT_ROLE.getRoleName(), 7200); // 2 hours
        
        // Default max concurrent sessions
        maxConcurrentSessions.put(RoleType.ADMIN.getRoleName(), 5);
        maxConcurrentSessions.put(RoleType.TECH_ROLE.getRoleName(), 3);
        maxConcurrentSessions.put(RoleType.JVC_ROLE.getRoleName(), 2);
        maxConcurrentSessions.put(RoleType.CQS_ROLE.getRoleName(), 2);
        maxConcurrentSessions.put(RoleType.PLANT_ROLE.getRoleName(), 1);
        
        // Default role URL patterns
        roleUrlPatterns.put(RoleType.ADMIN.getRoleName(), Arrays.asList("/**"));
        roleUrlPatterns.put(RoleType.JVC_ROLE.getRoleName(), Arrays.asList("/qrmfg/api/v1/jvc/**", "/qrmfg/api/v1/workflows/**"));
        roleUrlPatterns.put(RoleType.CQS_ROLE.getRoleName(), Arrays.asList("/qrmfg/api/v1/cqs/**", "/qrmfg/api/v1/quality/**", "/qrmfg/api/v1/workflows/**"));
        roleUrlPatterns.put(RoleType.TECH_ROLE.getRoleName(), Arrays.asList("/qrmfg/api/v1/tech/**", "/qrmfg/api/v1/admin/users/**", "/qrmfg/api/v1/admin/audit/**"));
        roleUrlPatterns.put(RoleType.PLANT_ROLE.getRoleName(), Arrays.asList("/qrmfg/api/v1/plant/**", "/qrmfg/api/v1/operations/**", "/qrmfg/api/v1/workflows/**"));
    }
    
    // Getters and Setters
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isAuditEnabled() {
        return auditEnabled;
    }
    
    public void setAuditEnabled(boolean auditEnabled) {
        this.auditEnabled = auditEnabled;
    }
    
    public boolean isPlantFilteringEnabled() {
        return plantFilteringEnabled;
    }
    
    public void setPlantFilteringEnabled(boolean plantFilteringEnabled) {
        this.plantFilteringEnabled = plantFilteringEnabled;
    }
    
    public boolean isMethodSecurityEnabled() {
        return methodSecurityEnabled;
    }
    
    public void setMethodSecurityEnabled(boolean methodSecurityEnabled) {
        this.methodSecurityEnabled = methodSecurityEnabled;
    }
    
    public String getDefaultRole() {
        return defaultRole;
    }
    
    public void setDefaultRole(String defaultRole) {
        this.defaultRole = defaultRole;
    }
    
    public Map<String, Integer> getRoleSessionTimeouts() {
        return roleSessionTimeouts;
    }
    
    public void setRoleSessionTimeouts(Map<String, Integer> roleSessionTimeouts) {
        this.roleSessionTimeouts = roleSessionTimeouts;
    }
    
    public Map<String, Integer> getMaxConcurrentSessions() {
        return maxConcurrentSessions;
    }
    
    public void setMaxConcurrentSessions(Map<String, Integer> maxConcurrentSessions) {
        this.maxConcurrentSessions = maxConcurrentSessions;
    }
    
    public List<String> getBypassPatterns() {
        return bypassPatterns;
    }
    
    public void setBypassPatterns(List<String> bypassPatterns) {
        this.bypassPatterns = bypassPatterns;
    }
    
    public Map<String, List<String>> getRoleUrlPatterns() {
        return roleUrlPatterns;
    }
    
    public void setRoleUrlPatterns(Map<String, List<String>> roleUrlPatterns) {
        this.roleUrlPatterns = roleUrlPatterns;
    }
    
    public boolean isStrictRoleValidation() {
        return strictRoleValidation;
    }
    
    public void setStrictRoleValidation(boolean strictRoleValidation) {
        this.strictRoleValidation = strictRoleValidation;
    }
    
    public boolean isPlantCodeValidation() {
        return plantCodeValidation;
    }
    
    public void setPlantCodeValidation(boolean plantCodeValidation) {
        this.plantCodeValidation = plantCodeValidation;
    }
    
    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }
    
    public void setMaxFailedAttempts(int maxFailedAttempts) {
        this.maxFailedAttempts = maxFailedAttempts;
    }
    
    public boolean isRoleHierarchyEnabled() {
        return roleHierarchyEnabled;
    }
    
    public void setRoleHierarchyEnabled(boolean roleHierarchyEnabled) {
        this.roleHierarchyEnabled = roleHierarchyEnabled;
    }
    
    /**
     * Get session timeout for a specific role
     * @param roleName the role name
     * @return session timeout in seconds, or default if not configured
     */
    public int getSessionTimeoutForRole(String roleName) {
        return roleSessionTimeouts.getOrDefault(roleName, 3600); // Default 1 hour
    }
    
    /**
     * Get max concurrent sessions for a specific role
     * @param roleName the role name
     * @return max concurrent sessions, or default if not configured
     */
    public int getMaxConcurrentSessionsForRole(String roleName) {
        return maxConcurrentSessions.getOrDefault(roleName, 1); // Default 1 session
    }
    
    /**
     * Get URL patterns for a specific role
     * @param roleName the role name
     * @return list of URL patterns, or empty list if not configured
     */
    public List<String> getUrlPatternsForRole(String roleName) {
        return roleUrlPatterns.getOrDefault(roleName, Arrays.asList());
    }
    
    /**
     * Check if a URL pattern should bypass RBAC validation
     * @param url the URL to check
     * @return true if should bypass, false otherwise
     */
    public boolean shouldBypassRBAC(String url) {
        if (url == null || !enabled) {
            return true;
        }
        
        return bypassPatterns.stream().anyMatch(pattern -> {
            if (pattern.endsWith("/**")) {
                return url.startsWith(pattern.substring(0, pattern.length() - 3));
            } else if (pattern.startsWith("*.")) {
                return url.endsWith(pattern.substring(1));
            } else {
                return url.equals(pattern) || url.matches(pattern);
            }
        });
    }
    
    /**
     * Validate configuration on startup
     */
    public void validateConfiguration() {
        if (defaultRole != null && !RoleType.isValidRoleName(defaultRole)) {
            throw new IllegalArgumentException("Invalid default role: " + defaultRole);
        }
        
        // Validate role session timeouts
        for (String roleName : roleSessionTimeouts.keySet()) {
            if (!RoleType.isValidRoleName(roleName)) {
                throw new IllegalArgumentException("Invalid role name in session timeouts: " + roleName);
            }
            
            Integer timeout = roleSessionTimeouts.get(roleName);
            if (timeout != null && timeout <= 0) {
                throw new IllegalArgumentException("Invalid session timeout for role " + roleName + ": " + timeout);
            }
        }
        
        // Validate max concurrent sessions
        for (String roleName : maxConcurrentSessions.keySet()) {
            if (!RoleType.isValidRoleName(roleName)) {
                throw new IllegalArgumentException("Invalid role name in max concurrent sessions: " + roleName);
            }
            
            Integer maxSessions = maxConcurrentSessions.get(roleName);
            if (maxSessions != null && maxSessions <= 0) {
                throw new IllegalArgumentException("Invalid max concurrent sessions for role " + roleName + ": " + maxSessions);
            }
        }
        
        if (maxFailedAttempts <= 0) {
            throw new IllegalArgumentException("Max failed attempts must be positive: " + maxFailedAttempts);
        }
    }
}