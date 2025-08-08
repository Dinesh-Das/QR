package com.cqs.qrmfg.enums;

/**
 * Enumeration of role types in the system
 * Maps to the role constants defined in RoleConstants.java
 */
public enum RoleType {
    ADMIN("ADMIN", "Administrator"),
    JVC_ROLE("JVC_USER", "JVC User"),
    CQS_ROLE("CQS_USER", "CQS User"),
    TECH_ROLE("TECH_USER", "Technical User"),
    PLANT_ROLE("PLANT_USER", "Plant User"),
    VIEWER_ROLE("VIEWER", "Viewer");
    
    private final String roleName;
    private final String displayName;
    
    RoleType(String roleName, String displayName) {
        this.roleName = roleName;
        this.displayName = displayName;
    }
    
    public String getRoleName() {
        return roleName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get RoleType from role name string
     */
    public static RoleType fromRoleName(String roleName) {
        if (roleName == null) {
            return null;
        }
        
        for (RoleType roleType : values()) {
            if (roleType.getRoleName().equalsIgnoreCase(roleName)) {
                return roleType;
            }
        }
        
        // Handle alternative role names
        switch (roleName.toUpperCase()) {
            case "JVC_USER":
            case "JVC":
                return JVC_ROLE;
            case "CQS_USER":
            case "CQS":
                return CQS_ROLE;
            case "TECH_USER":
            case "TECH":
                return TECH_ROLE;
            case "PLANT_USER":
            case "PLANT":
                return PLANT_ROLE;
            case "VIEWER":
                return VIEWER_ROLE;
            case "ADMIN":
                return ADMIN;
            default:
                return null;
        }
    }
    
    /**
     * Check if this role type has higher privilege than another
     */
    public boolean hasHigherPrivilegeThan(RoleType other) {
        if (other == null) {
            return true;
        }
        
        int thisLevel = getPrivilegeLevel();
        int otherLevel = other.getPrivilegeLevel();
        
        return thisLevel > otherLevel;
    }
    
    /**
     * Get privilege level for comparison (higher number = more privileges)
     */
    private int getPrivilegeLevel() {
        switch (this) {
            case VIEWER_ROLE:
                return 1;
            case PLANT_ROLE:
                return 2;
            case CQS_ROLE:
            case JVC_ROLE:
                return 3;
            case TECH_ROLE:
                return 4;
            case ADMIN:
                return 5;
            default:
                return 0;
        }
    }
    
    /**
     * Check if this role supports plant filtering
     */
    public boolean supportsPlantFiltering() {
        return this == PLANT_ROLE;
    }
    
    /**
     * Check if this role is an admin role
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }
    
    /**
     * Check if this role is a plant role
     */
    public boolean isPlantRole() {
        return this == PLANT_ROLE;
    }
    
    /**
     * Get hierarchy level for role comparison
     */
    public int getHierarchyLevel() {
        return getPrivilegeLevel();
    }
    
    /**
     * Get description for this role type
     */
    public String getDescription() {
        return displayName;
    }
    
    /**
     * Check if role type has higher or equal privilege than another
     */
    public static boolean hasHigherOrEqualPrivilege(RoleType roleType1, RoleType roleType2) {
        if (roleType1 == null || roleType2 == null) {
            return false;
        }
        return roleType1.getPrivilegeLevel() >= roleType2.getPrivilegeLevel();
    }
    
    /**
     * Check if role name is valid
     */
    public static boolean isValidRoleName(String roleName) {
        if (roleName == null) {
            return false;
        }
        
        for (RoleType roleType : values()) {
            if (roleType.getRoleName().equalsIgnoreCase(roleName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if this role requires plant assignment
     */
    public boolean requiresPlantAssignment() {
        return this == PLANT_ROLE;
    }
    
    /**
     * Check if this role is an admin role
     */
    public boolean isAdminRole() {
        return this == ADMIN;
    }
}