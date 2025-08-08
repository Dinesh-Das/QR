package com.cqs.qrmfg.config;

import com.cqs.qrmfg.enums.RoleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator for RBAC configuration properties.
 * Performs comprehensive validation of RBAC configuration at application startup.
 */
@Component
public class RBACConfigurationValidator {

    private static final Logger logger = LoggerFactory.getLogger(RBACConfigurationValidator.class);

    private final RBACConfigurationProperties rbacConfig;

    public RBACConfigurationValidator(RBACConfigurationProperties rbacConfig) {
        this.rbacConfig = rbacConfig;
    }

    /**
     * Validates RBAC configuration after application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration() {
        logger.info("Starting RBAC configuration validation...");

        try {
            validateSecurityConfiguration();
            validateAuditConfiguration();
            validatePlantConfiguration();
            validatePerformanceConfiguration();
            
            logger.info("RBAC configuration validation completed successfully");
        } catch (Exception e) {
            logger.error("RBAC configuration validation failed: {}", e.getMessage());
            throw new IllegalStateException("Invalid RBAC configuration", e);
        }
    }

    /**
     * Validates security-related configuration
     */
    private void validateSecurityConfiguration() {
        RBACConfigurationProperties.SecurityConfig security = rbacConfig.getSecurity();

        // Validate default role
        validateDefaultRole(security.getDefaultRole());

        // Validate role session timeouts
        validateRoleSessionTimeouts(security.getRoleSessionTimeouts());

        // Validate max concurrent sessions
        validateMaxConcurrentSessions(security.getMaxConcurrentSessions());

        // Validate role URL patterns
        validateRoleUrlPatterns(security.getRoleUrlPatterns());

        // Validate bypass patterns
        validateBypassPatterns(security.getBypassPatterns());

        logger.debug("Security configuration validation passed");
    }

    /**
     * Validates audit-related configuration
     */
    private void validateAuditConfiguration() {
        RBACConfigurationProperties.AuditConfig audit = rbacConfig.getAudit();

        if (audit.getRetentionDays() <= 0) {
            throw new IllegalArgumentException("Audit retention days must be positive");
        }

        if (audit.getBatchSize() <= 0) {
            throw new IllegalArgumentException("Audit batch size must be positive");
        }

        // Warn if audit is disabled but other audit features are enabled
        if (!audit.isEnabled() && (audit.isLogFailedAccess() || audit.isLogSuccessfulAccess())) {
            logger.warn("Audit is disabled but audit logging is enabled. Consider enabling audit or disabling audit logging.");
        }

        logger.debug("Audit configuration validation passed");
    }

    /**
     * Validates plant-related configuration
     */
    private void validatePlantConfiguration() {
        RBACConfigurationProperties.PlantConfig plant = rbacConfig.getPlant();

        if (plant.getMaxPlantsPerUser() <= 0) {
            throw new IllegalArgumentException("Max plants per user must be positive");
        }

        if (plant.getDefaultPlant() == null || plant.getDefaultPlant().trim().isEmpty()) {
            throw new IllegalArgumentException("Default plant cannot be null or empty");
        }

        // Validate default plant format (should be alphanumeric)
        if (!plant.getDefaultPlant().matches("^[A-Z0-9_]+$")) {
            logger.warn("Default plant '{}' does not follow recommended naming convention (uppercase alphanumeric with underscores)", 
                       plant.getDefaultPlant());
        }

        logger.debug("Plant configuration validation passed");
    }

    /**
     * Validates performance-related configuration
     */
    private void validatePerformanceConfiguration() {
        RBACConfigurationProperties.PerformanceConfig performance = rbacConfig.getPerformance();

        if (performance.getCacheTtlSeconds() <= 0) {
            throw new IllegalArgumentException("Cache TTL must be positive");
        }

        if (performance.getRoleCacheSize() <= 0) {
            throw new IllegalArgumentException("Role cache size must be positive");
        }

        if (performance.getPlantCacheSize() <= 0) {
            throw new IllegalArgumentException("Plant cache size must be positive");
        }

        if (performance.getScreenCacheSize() <= 0) {
            throw new IllegalArgumentException("Screen cache size must be positive");
        }

        // Warn about cache settings
        if (!performance.isCacheEnabled()) {
            logger.warn("RBAC caching is disabled. This may impact performance in production environments.");
        }

        logger.debug("Performance configuration validation passed");
    }

    /**
     * Validates the default role against available role types
     */
    private void validateDefaultRole(String defaultRole) {
        if (defaultRole == null || defaultRole.trim().isEmpty()) {
            throw new IllegalArgumentException("Default role cannot be null or empty");
        }

        Set<String> validRoles = Arrays.stream(RoleType.values())
                .map(RoleType::name)
                .collect(Collectors.toSet());

        if (!validRoles.contains(defaultRole)) {
            throw new IllegalArgumentException(
                String.format("Invalid default role '%s'. Valid roles are: %s", 
                             defaultRole, validRoles));
        }
    }

    /**
     * Validates role session timeout configuration
     */
    private void validateRoleSessionTimeouts(Map<String, Integer> roleSessionTimeouts) {
        Set<String> validRoles = Arrays.stream(RoleType.values())
                .map(RoleType::name)
                .collect(Collectors.toSet());

        for (Map.Entry<String, Integer> entry : roleSessionTimeouts.entrySet()) {
            String role = entry.getKey();
            Integer timeout = entry.getValue();

            if (!validRoles.contains(role)) {
                logger.warn("Unknown role '{}' in session timeout configuration", role);
            }

            if (timeout <= 0) {
                throw new IllegalArgumentException(
                    String.format("Session timeout for role '%s' must be positive", role));
            }

            if (timeout < 300) { // 5 minutes minimum
                logger.warn("Session timeout for role '{}' is very short ({}s). Consider increasing for better user experience.", 
                           role, timeout);
            }
        }
    }

    /**
     * Validates max concurrent sessions configuration
     */
    private void validateMaxConcurrentSessions(Map<String, Integer> maxConcurrentSessions) {
        Set<String> validRoles = Arrays.stream(RoleType.values())
                .map(RoleType::name)
                .collect(Collectors.toSet());

        for (Map.Entry<String, Integer> entry : maxConcurrentSessions.entrySet()) {
            String role = entry.getKey();
            Integer maxSessions = entry.getValue();

            if (!validRoles.contains(role)) {
                logger.warn("Unknown role '{}' in max concurrent sessions configuration", role);
            }

            if (maxSessions <= 0) {
                throw new IllegalArgumentException(
                    String.format("Max concurrent sessions for role '%s' must be positive", role));
            }

            if (maxSessions > 50) {
                logger.warn("Max concurrent sessions for role '{}' is very high ({}). This may impact system performance.", 
                           role, maxSessions);
            }
        }
    }

    /**
     * Validates role URL patterns configuration
     */
    private void validateRoleUrlPatterns(Map<String, List<String>> roleUrlPatterns) {
        Set<String> validRoles = Arrays.stream(RoleType.values())
                .map(RoleType::name)
                .collect(Collectors.toSet());

        for (Map.Entry<String, List<String>> entry : roleUrlPatterns.entrySet()) {
            String role = entry.getKey();
            List<String> patterns = entry.getValue();

            if (!validRoles.contains(role)) {
                logger.warn("Unknown role '{}' in URL patterns configuration", role);
            }

            if (patterns == null || patterns.isEmpty()) {
                logger.warn("No URL patterns defined for role '{}'", role);
                continue;
            }

            // Validate URL patterns
            for (String pattern : patterns) {
                if (pattern == null || pattern.trim().isEmpty()) {
                    throw new IllegalArgumentException(
                        String.format("Empty URL pattern found for role '%s'", role));
                }

                if (!pattern.startsWith("/")) {
                    logger.warn("URL pattern '{}' for role '{}' should start with '/'", pattern, role);
                }
            }
        }

        // Check if ADMIN role has unrestricted access
        if (roleUrlPatterns.containsKey("ADMIN")) {
            List<String> adminPatterns = roleUrlPatterns.get("ADMIN");
            if (!adminPatterns.contains("/**")) {
                logger.warn("ADMIN role does not have unrestricted access pattern '/**'. This may limit admin functionality.");
            }
        }
    }

    /**
     * Validates bypass patterns configuration
     */
    private void validateBypassPatterns(List<String> bypassPatterns) {
        if (bypassPatterns == null) {
            logger.warn("Bypass patterns list is null");
            return;
        }

        for (String pattern : bypassPatterns) {
            if (pattern == null || pattern.trim().isEmpty()) {
                throw new IllegalArgumentException("Empty bypass pattern found");
            }

            if (!pattern.startsWith("/")) {
                logger.warn("Bypass pattern '{}' should start with '/'", pattern);
            }
        }

        // Check for essential bypass patterns
        boolean hasAuthBypass = bypassPatterns.stream()
                .anyMatch(pattern -> pattern.contains("/auth/"));
        
        if (!hasAuthBypass) {
            logger.warn("No authentication bypass pattern found. Users may not be able to log in.");
        }
    }

    /**
     * Gets a summary of the current RBAC configuration
     */
    public String getConfigurationSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("RBAC Configuration Summary:\n");
        summary.append("- Security enabled: ").append(rbacConfig.getSecurity().isEnabled()).append("\n");
        summary.append("- Audit enabled: ").append(rbacConfig.getAudit().isEnabled()).append("\n");
        summary.append("- Plant filtering enabled: ").append(rbacConfig.getSecurity().isPlantFilteringEnabled()).append("\n");
        summary.append("- Method security enabled: ").append(rbacConfig.getSecurity().isMethodSecurityEnabled()).append("\n");
        summary.append("- Default role: ").append(rbacConfig.getSecurity().getDefaultRole()).append("\n");
        summary.append("- Cache enabled: ").append(rbacConfig.getPerformance().isCacheEnabled()).append("\n");
        summary.append("- Max plants per user: ").append(rbacConfig.getPlant().getMaxPlantsPerUser()).append("\n");
        summary.append("- Audit retention days: ").append(rbacConfig.getAudit().getRetentionDays()).append("\n");
        
        return summary.toString();
    }
}