package com.cqs.qrmfg.util;

import com.cqs.qrmfg.enums.RoleType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Constants and utility methods for RBAC (Role-Based Access Control)
 */
public final class RBACConstants {
    
    // Prevent instantiation
    private RBACConstants() {}
    
    // Error messages
    public static final String ERROR_INVALID_ROLE_TYPE = "Invalid role type provided";
    public static final String ERROR_USER_NOT_FOUND = "User not found";
    public static final String ERROR_ROLE_NOT_FOUND = "Role not found";
    public static final String ERROR_INVALID_PLANT_CODE = "Invalid plant code";
    public static final String ERROR_PLANT_ASSIGNMENT_REQUIRED = "Plant assignment required for this role";
    
    // Validation constants
    public static final int MAX_ROLE_NAME_LENGTH = 50;
    public static final int MAX_ROLE_DESCRIPTION_LENGTH = 200;
    
    // Screen route mappings
    private static final Map<RoleType, Set<String>> ROLE_SCREEN_ACCESS = createRoleScreenAccessMap();
    
    /**
     * Check if a role can access a specific screen
     */
    public static boolean canAccessScreen(RoleType roleType, String screenRoute) {
        if (roleType == null || screenRoute == null) {
            return false;
        }
        
        // Admin has access to all screens
        if (roleType == RoleType.ADMIN) {
            return true;
        }
        
        Set<String> allowedScreens = ROLE_SCREEN_ACCESS.get(roleType);
        return allowedScreens != null && allowedScreens.contains(screenRoute);
    }
    
    /**
     * Get list of accessible screens for a role
     */
    public static List<String> getAccessibleScreens(RoleType roleType) {
        if (roleType == null) {
            return new ArrayList<>();
        }
        
        Set<String> screens = ROLE_SCREEN_ACCESS.get(roleType);
        return screens != null ? new ArrayList<>(screens) : new ArrayList<>();
    }
    
    /**
     * Normalize plant code for consistent comparison
     */
    public static String normalizePlantCode(String plantCode) {
        return plantCode != null ? plantCode.trim().toUpperCase() : null;
    }
    
    // Plant management constants
    public static final int MAX_PLANTS_PER_USER = 50;
    public static final String PLANT_CODE_PATTERN = "^[A-Z0-9]{4,10}$";
    public static final String ERROR_TOO_MANY_PLANTS = "Too many plants assigned to user";
    
    /**
     * Check if a role can access a specific data type
     */
    public static boolean canAccessDataType(RoleType roleType, String dataType) {
        return hasDataTypeAccess(roleType, dataType);
    }
    
    /**
     * Check if a role can access a specific resource with action
     */
    public static boolean canAccessResource(RoleType roleType, String resourceType, String action) {
        if (roleType == null || resourceType == null || action == null) {
            return false;
        }
        
        // Admin has access to all resources
        if (roleType == RoleType.ADMIN) {
            return true;
        }
        
        // Basic resource access check
        return canAccessDataType(roleType, resourceType);
    }
    
    /**
     * Check if a role has data access to a specific type
     */
    public static boolean hasDataTypeAccess(RoleType roleType, String dataType) {
        if (roleType == null || dataType == null) {
            return false;
        }
        
        // Admin has access to all data
        if (roleType == RoleType.ADMIN) {
            return true;
        }
        
        // Define data access rules
        switch (roleType) {
            case JVC_ROLE:
                return isJvcDataType(dataType);
            case CQS_ROLE:
                return isCqsDataType(dataType);
            case TECH_ROLE:
                return isTechDataType(dataType);
            case PLANT_ROLE:
                return isPlantDataType(dataType);
            default:
                return false;
        }
    }
    
    /**
     * Check if a role requires plant assignment
     */
    public static boolean requiresPlantAssignment(RoleType roleType) {
        return roleType == RoleType.PLANT_ROLE;
    }
    
    /**
     * Check if a role supports plant filtering
     */
    public static boolean supportsPlantFiltering(RoleType roleType) {
        return roleType == RoleType.PLANT_ROLE;
    }
    
    /**
     * Normalize a list of plant codes
     */
    public static List<String> normalizePlantCodes(List<String> plantCodes) {
        if (plantCodes == null) {
            return new ArrayList<>();
        }
        
        return plantCodes.stream()
            .map(RBACConstants::normalizePlantCode)
            .filter(code -> code != null && !code.isEmpty())
            .distinct()
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Validate plant code format
     */
    public static boolean isValidPlantCodeFormat(String plantCode) {
        return plantCode != null && plantCode.matches(PLANT_CODE_PATTERN);
    }
    
    /**
     * Validate a list of plant codes
     */
    public static boolean areValidPlantCodes(List<String> plantCodes) {
        if (plantCodes == null) {
            return true;
        }
        
        return plantCodes.stream().allMatch(RBACConstants::isValidPlantCodeFormat);
    }
    
    /**
     * Check if a role type is valid
     */
    public static boolean isValidRoleType(RoleType roleType) {
        return roleType != null;
    }
    
    /**
     * Create the role screen access map (Java 8 compatible)
     */
    private static Map<RoleType, Set<String>> createRoleScreenAccessMap() {
        Map<RoleType, Set<String>> map = new HashMap<>();
        
        Set<String> adminScreens = new HashSet<>();
        adminScreens.add("/qrmfg");
        adminScreens.add("/qrmfg/dashboard");
        adminScreens.add("/qrmfg/admin");
        adminScreens.add("/qrmfg/jvc");
        adminScreens.add("/qrmfg/cqs");
        adminScreens.add("/qrmfg/tech");
        adminScreens.add("/qrmfg/plant");
        adminScreens.add("/qrmfg/workflows");
        adminScreens.add("/qrmfg/workflow-monitoring");
        adminScreens.add("/qrmfg/reports");
        adminScreens.add("/qrmfg/users");
        adminScreens.add("/qrmfg/roles");
        adminScreens.add("/qrmfg/sessions");
        adminScreens.add("/qrmfg/user-role-management");
        adminScreens.add("/qrmfg/auditlogs");
        adminScreens.add("/qrmfg/settings");
        map.put(RoleType.ADMIN, adminScreens);
        
        Set<String> jvcScreens = new HashSet<>();
        jvcScreens.add("/qrmfg");
        jvcScreens.add("/qrmfg/dashboard");
        jvcScreens.add("/qrmfg/jvc");
        jvcScreens.add("/qrmfg/workflows");
        jvcScreens.add("/qrmfg/reports");
        jvcScreens.add("/qrmfg/settings");
        map.put(RoleType.JVC_ROLE, jvcScreens);
        
        Set<String> cqsScreens = new HashSet<>();
        cqsScreens.add("/qrmfg");
        cqsScreens.add("/qrmfg/dashboard");
        cqsScreens.add("/qrmfg/cqs");
        cqsScreens.add("/qrmfg/workflows");
        cqsScreens.add("/qrmfg/reports");
        cqsScreens.add("/qrmfg/settings");
        map.put(RoleType.CQS_ROLE, cqsScreens);
        
        Set<String> techScreens = new HashSet<>();
        techScreens.add("/qrmfg");
        techScreens.add("/qrmfg/dashboard");
        techScreens.add("/qrmfg/tech");
        techScreens.add("/qrmfg/workflows");
        techScreens.add("/qrmfg/workflow-monitoring");
        techScreens.add("/qrmfg/reports");
        techScreens.add("/qrmfg/auditlogs");
        techScreens.add("/qrmfg/settings");
        map.put(RoleType.TECH_ROLE, techScreens);
        
        Set<String> plantScreens = new HashSet<>();
        plantScreens.add("/qrmfg");
        plantScreens.add("/qrmfg/dashboard");
        plantScreens.add("/qrmfg/plant");
        plantScreens.add("/qrmfg/workflows");
        plantScreens.add("/qrmfg/reports");
        plantScreens.add("/qrmfg/settings");
        map.put(RoleType.PLANT_ROLE, plantScreens);
        
        return map;
    }
    
    private static boolean isJvcDataType(String dataType) {
        return dataType != null && (
            dataType.toLowerCase().contains("jvc") ||
            dataType.toLowerCase().contains("workflow") ||
            dataType.toLowerCase().contains("document")
        );
    }
    
    private static boolean isCqsDataType(String dataType) {
        return dataType != null && (
            dataType.toLowerCase().contains("cqs") ||
            dataType.toLowerCase().contains("quality") ||
            dataType.toLowerCase().contains("query") ||
            dataType.toLowerCase().contains("workflow") ||
            dataType.toLowerCase().contains("document")
        );
    }
    
    private static boolean isTechDataType(String dataType) {
        return dataType != null && (
            dataType.toLowerCase().contains("tech") ||
            dataType.toLowerCase().contains("system") ||
            dataType.toLowerCase().contains("config") ||
            dataType.toLowerCase().contains("audit") ||
            dataType.toLowerCase().contains("workflow")
        );
    }
    
    private static boolean isPlantDataType(String dataType) {
        return dataType != null && (
            dataType.toLowerCase().contains("plant") ||
            dataType.toLowerCase().contains("operation") ||
            dataType.toLowerCase().contains("workflow") ||
            dataType.toLowerCase().contains("document") ||
            dataType.toLowerCase().contains("query")
        );
    }
}