package com.cqs.qrmfg.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for RBAC (Role-Based Access Control) system.
 * This class binds all RBAC-related configuration from application.properties
 * and provides validation for configuration values.
 */
@Component
@ConfigurationProperties(prefix = "rbac")
@Validated
public class RBACConfigurationProperties {

    @NestedConfigurationProperty
    @Valid
    @NotNull
    private SecurityConfig security = new SecurityConfig();

    @NestedConfigurationProperty
    @Valid
    @NotNull
    private AuditConfig audit = new AuditConfig();

    @NestedConfigurationProperty
    @Valid
    @NotNull
    private PlantConfig plant = new PlantConfig();

    @NestedConfigurationProperty
    @Valid
    @NotNull
    private PerformanceConfig performance = new PerformanceConfig();

    // Getters and setters
    public SecurityConfig getSecurity() {
        return security;
    }

    public void setSecurity(SecurityConfig security) {
        this.security = security;
    }

    public AuditConfig getAudit() {
        return audit;
    }

    public void setAudit(AuditConfig audit) {
        this.audit = audit;
    }

    public PlantConfig getPlant() {
        return plant;
    }

    public void setPlant(PlantConfig plant) {
        this.plant = plant;
    }

    public PerformanceConfig getPerformance() {
        return performance;
    }

    public void setPerformance(PerformanceConfig performance) {
        this.performance = performance;
    }

    /**
     * Security-related RBAC configuration
     */
    public static class SecurityConfig {
        private boolean enabled = true;
        private boolean auditEnabled = true;
        private boolean plantFilteringEnabled = true;
        private boolean methodSecurityEnabled = true;
        
        @NotEmpty(message = "Default role cannot be empty")
        private String defaultRole = "PLANT_ROLE";
        
        private boolean strictRoleValidation = true;
        private boolean plantCodeValidation = true;
        
        @Min(value = 1, message = "Max failed attempts must be at least 1")
        @Max(value = 20, message = "Max failed attempts cannot exceed 20")
        private int maxFailedAttempts = 5;
        
        private boolean roleHierarchyEnabled = true;

        @Valid
        @NotNull
        private Map<String, Integer> roleSessionTimeouts = new HashMap<>();

        @Valid
        @NotNull
        private Map<String, Integer> maxConcurrentSessions = new HashMap<>();

        @NotNull
        private List<String> bypassPatterns = new ArrayList<>();

        @Valid
        @NotNull
        private Map<String, List<String>> roleUrlPatterns = new HashMap<>();

        // Getters and setters
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
    }

    /**
     * Audit-related RBAC configuration
     */
    public static class AuditConfig {
        private boolean enabled = true;
        private boolean logSuccessfulAccess = false;
        private boolean logFailedAccess = true;
        private boolean logScreenAccess = true;
        private boolean logDataAccess = true;
        private boolean logPlantAccess = true;
        
        @Min(value = 1, message = "Retention days must be at least 1")
        @Max(value = 3650, message = "Retention days cannot exceed 3650 (10 years)")
        private int retentionDays = 90;
        
        @Min(value = 1, message = "Batch size must be at least 1")
        @Max(value = 10000, message = "Batch size cannot exceed 10000")
        private int batchSize = 100;

        // Getters and setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isLogSuccessfulAccess() {
            return logSuccessfulAccess;
        }

        public void setLogSuccessfulAccess(boolean logSuccessfulAccess) {
            this.logSuccessfulAccess = logSuccessfulAccess;
        }

        public boolean isLogFailedAccess() {
            return logFailedAccess;
        }

        public void setLogFailedAccess(boolean logFailedAccess) {
            this.logFailedAccess = logFailedAccess;
        }

        public boolean isLogScreenAccess() {
            return logScreenAccess;
        }

        public void setLogScreenAccess(boolean logScreenAccess) {
            this.logScreenAccess = logScreenAccess;
        }

        public boolean isLogDataAccess() {
            return logDataAccess;
        }

        public void setLogDataAccess(boolean logDataAccess) {
            this.logDataAccess = logDataAccess;
        }

        public boolean isLogPlantAccess() {
            return logPlantAccess;
        }

        public void setLogPlantAccess(boolean logPlantAccess) {
            this.logPlantAccess = logPlantAccess;
        }

        public int getRetentionDays() {
            return retentionDays;
        }

        public void setRetentionDays(int retentionDays) {
            this.retentionDays = retentionDays;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }

    /**
     * Plant access related RBAC configuration
     */
    public static class PlantConfig {
        @Min(value = 1, message = "Max plants per user must be at least 1")
        @Max(value = 100, message = "Max plants per user cannot exceed 100")
        private int maxPlantsPerUser = 20;
        
        @NotEmpty(message = "Default plant cannot be empty")
        private String defaultPlant = "MAIN";
        
        private boolean validationEnabled = true;
        private boolean autoAssignPrimary = true;

        // Getters and setters
        public int getMaxPlantsPerUser() {
            return maxPlantsPerUser;
        }

        public void setMaxPlantsPerUser(int maxPlantsPerUser) {
            this.maxPlantsPerUser = maxPlantsPerUser;
        }

        public String getDefaultPlant() {
            return defaultPlant;
        }

        public void setDefaultPlant(String defaultPlant) {
            this.defaultPlant = defaultPlant;
        }

        public boolean isValidationEnabled() {
            return validationEnabled;
        }

        public void setValidationEnabled(boolean validationEnabled) {
            this.validationEnabled = validationEnabled;
        }

        public boolean isAutoAssignPrimary() {
            return autoAssignPrimary;
        }

        public void setAutoAssignPrimary(boolean autoAssignPrimary) {
            this.autoAssignPrimary = autoAssignPrimary;
        }
    }

    /**
     * Performance-related RBAC configuration
     */
    public static class PerformanceConfig {
        private boolean cacheEnabled = true;
        
        @Min(value = 60, message = "Cache TTL must be at least 60 seconds")
        @Max(value = 86400, message = "Cache TTL cannot exceed 86400 seconds (24 hours)")
        private int cacheTtlSeconds = 3600;
        
        @Min(value = 100, message = "Role cache size must be at least 100")
        @Max(value = 100000, message = "Role cache size cannot exceed 100000")
        private int roleCacheSize = 1000;
        
        @Min(value = 100, message = "Plant cache size must be at least 100")
        @Max(value = 100000, message = "Plant cache size cannot exceed 100000")
        private int plantCacheSize = 5000;
        
        @Min(value = 100, message = "Screen cache size must be at least 100")
        @Max(value = 100000, message = "Screen cache size cannot exceed 100000")
        private int screenCacheSize = 2000;

        // Getters and setters
        public boolean isCacheEnabled() {
            return cacheEnabled;
        }

        public void setCacheEnabled(boolean cacheEnabled) {
            this.cacheEnabled = cacheEnabled;
        }

        public int getCacheTtlSeconds() {
            return cacheTtlSeconds;
        }

        public void setCacheTtlSeconds(int cacheTtlSeconds) {
            this.cacheTtlSeconds = cacheTtlSeconds;
        }

        public int getRoleCacheSize() {
            return roleCacheSize;
        }

        public void setRoleCacheSize(int roleCacheSize) {
            this.roleCacheSize = roleCacheSize;
        }

        public int getPlantCacheSize() {
            return plantCacheSize;
        }

        public void setPlantCacheSize(int plantCacheSize) {
            this.plantCacheSize = plantCacheSize;
        }

        public int getScreenCacheSize() {
            return screenCacheSize;
        }

        public void setScreenCacheSize(int screenCacheSize) {
            this.screenCacheSize = screenCacheSize;
        }
    }
}