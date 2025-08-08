package com.cqs.qrmfg.service;

import com.cqs.qrmfg.enums.RoleType;
import com.cqs.qrmfg.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;

/**
 * RBAC Authorization Service interface for making access control decisions.
 * Provides methods for screen access validation, data filtering, and audit logging.
 */
public interface RBACAuthorizationService {
    
    // ========== SCREEN ACCESS CONTROL ==========
    
    /**
     * Check if the authenticated user has access to a specific screen route
     * @param auth the authentication object containing user details
     * @param screenRoute the screen route to check access for
     * @return true if access is granted, false otherwise
     */
    boolean hasScreenAccess(Authentication auth, String screenRoute);
    
    /**
     * Get user by username
     * @param username the username to look up
     * @return user object if found, null otherwise
     */
    User getUserByUsername(String username);
    
    /**
     * Check if a user with specific role type has access to a screen route
     * @param roleType the user's role type
     * @param screenRoute the screen route to check access for
     * @return true if access is granted, false otherwise
     */
    boolean hasScreenAccess(RoleType roleType, String screenRoute);
    
    /**
     * Get all accessible screen routes for the authenticated user
     * @param auth the authentication object containing user details
     * @return list of accessible screen routes/patterns
     */
    List<String> getAccessibleScreens(Authentication auth);
    
    /**
     * Get all accessible screen routes for a specific role type
     * @param roleType the role type to get accessible screens for
     * @return list of accessible screen routes/patterns
     */
    List<String> getAccessibleScreens(RoleType roleType);
    
    // ========== DATA ACCESS CONTROL ==========
    
    /**
     * Check if the authenticated user has access to specific data
     * @param auth the authentication object containing user details
     * @param dataType the type of data being accessed (e.g., "Document", "Query")
     * @param context additional context information for access decision
     * @return true if access is granted, false otherwise
     */
    boolean hasDataAccess(Authentication auth, String dataType, Map<String, Object> context);
    
    /**
     * Check if the authenticated user has access to data with plant filtering
     * @param auth the authentication object containing user details
     * @param dataType the type of data being accessed
     * @param plantCode the plant code associated with the data
     * @param context additional context information
     * @return true if access is granted, false otherwise
     */
    boolean hasPlantDataAccess(Authentication auth, String dataType, String plantCode, Map<String, Object> context);
    
    /**
     * Check if the authenticated user has access to data with multiple plant codes
     * @param auth the authentication object containing user details
     * @param dataType the type of data being accessed
     * @param plantCodes list of plant codes associated with the data
     * @param context additional context information
     * @return true if access is granted, false otherwise
     */
    boolean hasMultiPlantDataAccess(Authentication auth, String dataType, List<String> plantCodes, Map<String, Object> context);
    
    // ========== DATA FILTERING ==========
    
    /**
     * Get a JPA Specification for filtering data based on user's access rights
     * @param auth the authentication object containing user details
     * @param entityType the entity type being queried
     * @param plantField the field name containing plant code in the entity
     * @return JPA Specification for filtering, or null if no filtering needed
     */
    <T> Specification<T> getDataFilterSpecification(Authentication auth, Class<T> entityType, String plantField);
    
    /**
     * Get a JPA Specification for filtering data with custom context
     * @param auth the authentication object containing user details
     * @param entityType the entity type being queried
     * @param plantField the field name containing plant code in the entity
     * @param context additional filtering context
     * @return JPA Specification for filtering, or null if no filtering needed
     */
    <T> Specification<T> getDataFilterSpecification(Authentication auth, Class<T> entityType, String plantField, Map<String, Object> context);
    
    /**
     * Filter a list of data objects based on user's plant access
     * @param auth the authentication object containing user details
     * @param data the list of data objects to filter
     * @param plantFieldExtractor function to extract plant code from data objects
     * @return filtered list containing only accessible data
     */
    <T> List<T> filterDataByPlantAccess(Authentication auth, List<T> data, java.util.function.Function<T, String> plantFieldExtractor);
    
    /**
     * Filter a list of data objects with multiple plant codes
     * @param auth the authentication object containing user details
     * @param data the list of data objects to filter
     * @param plantFieldExtractor function to extract plant codes from data objects
     * @return filtered list containing only accessible data
     */
    <T> List<T> filterDataByMultiPlantAccess(Authentication auth, List<T> data, java.util.function.Function<T, List<String>> plantFieldExtractor);
    
    // ========== USER ACCESS INFORMATION ==========
    
    /**
     * Get the primary role type for the authenticated user
     * @param auth the authentication object containing user details
     * @return the primary role type, or null if not found
     */
    RoleType getUserPrimaryRoleType(Authentication auth);
    
    /**
     * Get all role types for the authenticated user
     * @param auth the authentication object containing user details
     * @return list of role types assigned to the user
     */
    List<RoleType> getUserRoleTypes(Authentication auth);
    
    /**
     * Get the plant codes assigned to the authenticated user
     * @param auth the authentication object containing user details
     * @return list of assigned plant codes, empty if none assigned
     */
    List<String> getUserPlantCodes(Authentication auth);
    
    /**
     * Get the primary plant code for the authenticated user
     * @param auth the authentication object containing user details
     * @return the primary plant code, or null if not set
     */
    String getUserPrimaryPlant(Authentication auth);
    
    /**
     * Check if the authenticated user is an admin
     * @param auth the authentication object containing user details
     * @return true if user has admin role, false otherwise
     */
    boolean isUserAdmin(Authentication auth);
    
    /**
     * Check if the authenticated user is a plant user
     * @param auth the authentication object containing user details
     * @return true if user has plant role, false otherwise
     */
    boolean isUserPlantUser(Authentication auth);
    
    // ========== ACCESS DECISION UTILITIES ==========
    
    /**
     * Make an access decision for a specific resource
     * @param auth the authentication object containing user details
     * @param resourceType the type of resource being accessed
     * @param resourceId the ID of the specific resource
     * @param action the action being performed (READ, WRITE, DELETE, etc.)
     * @param context additional context for the decision
     * @return access decision result with details
     */
    AccessDecision makeAccessDecision(Authentication auth, String resourceType, String resourceId, String action, Map<String, Object> context);
    
    /**
     * Generate user-specific access summary
     * @param auth the authentication object containing user details
     * @return map containing user's access capabilities and restrictions
     */
    Map<String, Object> generateUserAccessSummary(Authentication auth);
    
    // ========== AUDIT AND LOGGING ==========
    
    /**
     * Log an access attempt (both successful and denied)
     * @param auth the authentication object containing user details
     * @param resource the resource being accessed
     * @param action the action being performed
     * @param granted whether access was granted
     * @param context additional context information
     */
    void logAccessAttempt(Authentication auth, String resource, String action, boolean granted, Map<String, Object> context);
    
    /**
     * Log a screen access attempt
     * @param auth the authentication object containing user details
     * @param screenRoute the screen route being accessed
     * @param granted whether access was granted
     * @param context additional context information
     */
    void logScreenAccess(Authentication auth, String screenRoute, boolean granted, Map<String, Object> context);
    
    /**
     * Log a data access attempt
     * @param auth the authentication object containing user details
     * @param dataType the type of data being accessed
     * @param dataId the ID of the specific data item
     * @param granted whether access was granted
     * @param context additional context information
     */
    void logDataAccess(Authentication auth, String dataType, String dataId, boolean granted, Map<String, Object> context);

    // ========== INNER CLASSES ==========
    
    /**
     * Access decision result containing decision and reasoning
     */
    class AccessDecision {
        private final boolean granted;
        private final String reason;
        private final Map<String, Object> details;
        
        public AccessDecision(boolean granted, String reason) {
            this.granted = granted;
            this.reason = reason;
            this.details = new java.util.HashMap<>();
        }
        
        public AccessDecision(boolean granted, String reason, Map<String, Object> details) {
            this.granted = granted;
            this.reason = reason;
            this.details = details != null ? new java.util.HashMap<>(details) : new java.util.HashMap<>();
        }
        
        public boolean isGranted() { return granted; }
        public String getReason() { return reason; }
        public Map<String, Object> getDetails() { return details; }
        
        public void addDetail(String key, Object value) {
            details.put(key, value);
        }
        
        @Override
        public String toString() {
            return "AccessDecision{granted=" + granted + ", reason='" + reason + "', details=" + details + "}";
        }
    }
}