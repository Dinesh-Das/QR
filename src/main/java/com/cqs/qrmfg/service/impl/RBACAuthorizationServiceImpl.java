package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.service.RBACAuthorizationService;
import com.cqs.qrmfg.service.RoleService;
import com.cqs.qrmfg.service.PlantAccessService;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.enums.RoleType;
import com.cqs.qrmfg.util.RBACConstants;
import com.cqs.qrmfg.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.criteria.Predicate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of RBACAuthorizationService providing comprehensive access control
 * decisions for screens, data, and plant-based filtering.
 */
@Service
@Transactional
public class RBACAuthorizationServiceImpl implements RBACAuthorizationService {
    
    private static final Logger logger = LoggerFactory.getLogger(RBACAuthorizationServiceImpl.class);
    
    @Autowired
    private RoleService roleService;
    
    @Autowired
    private PlantAccessService plantAccessService;
    
    @Autowired
    private UserRepository userRepository;
    
    // ========== USER RETRIEVAL ==========
    
    @Override
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        
        return userRepository.findByUsername(username.trim()).orElse(null);
    }
        
    // ========== SCREEN ACCESS CONTROL ==========
    @Override
    @Transactional(readOnly = true)
    public boolean hasScreenAccess(Authentication auth, String screenRoute) {
        if (auth == null || screenRoute == null) {
            logger.debug("Screen access denied: null authentication or screen route");
            return false;
        }
        
        RoleType primaryRole = getUserPrimaryRoleType(auth);
        if (primaryRole == null) {
            logger.debug("Screen access denied: no valid role found for user {}", getUsernameFromAuth(auth));
            return false;
        }
        
        boolean hasAccess = hasScreenAccess(primaryRole, screenRoute);
        // Log access attempt
        logScreenAccess(auth, screenRoute, hasAccess, createContextMap("primaryRole", primaryRole));
        return hasAccess;
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasScreenAccess(RoleType roleType, String screenRoute) {
        if (roleType == null || screenRoute == null) {
            return false;
        }
        
        return RBACConstants.canAccessScreen(roleType, screenRoute);
    }
        
    @Override
    @Transactional(readOnly = true)
    public List<String> getAccessibleScreens(Authentication auth) {
        if (auth == null) {
            return Collections.emptyList();
        }
        
        RoleType primaryRole = getUserPrimaryRoleType(auth);
        if (primaryRole == null) {
            return Collections.emptyList();
        }
    
        return getAccessibleScreens(primaryRole);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAccessibleScreens(RoleType roleType) {
        if (roleType == null) {
            return Collections.emptyList();
        }
        
        return RBACConstants.getAccessibleScreens(roleType);
    }
    
    // ========== DATA ACCESS CONTROL ==========
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasDataAccess(Authentication auth, String dataType, Map<String, Object> context) {
        if (auth == null || dataType == null) {
            return false;
        }
        
        RoleType primaryRole = getUserPrimaryRoleType(auth);
        if (primaryRole == null) {
            return false;
        }
        
        // Admin has access to all data
        if (primaryRole == RoleType.ADMIN) {
            return true;
        }
        
        // Check role-based data access
        return RBACConstants.canAccessDataType(primaryRole, dataType);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasPlantDataAccess(Authentication auth, String dataType, String plantCode, Map<String, Object> context) {
        if (!hasDataAccess(auth, dataType, context)) {
            return false;
        }
        
        // If user is not a plant user, they have access to all plants
        if (!isUserPlantUser(auth)) {
            return true;
        }
        
        List<String> userPlants = getUserPlantCodes(auth);
        return userPlants.contains(plantCode);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasMultiPlantDataAccess(Authentication auth, String dataType, List<String> plantCodes, Map<String, Object> context) {
        if (!hasDataAccess(auth, dataType, context)) {
            return false;
        }
        
        if (plantCodes == null || plantCodes.isEmpty()) {
            return true;
        }
        
        // If user is not a plant user, they have access to all plants
        if (!isUserPlantUser(auth)) {
            return true;
        }
        
        List<String> userPlants = getUserPlantCodes(auth);
        return plantCodes.stream().anyMatch(userPlants::contains);
    }
    
    // ========== DATA FILTERING ==========
    
    @Override
    @Transactional(readOnly = true)
    public <T> Specification<T> getDataFilterSpecification(Authentication auth, Class<T> entityType, String plantField) {
        return getDataFilterSpecification(auth, entityType, plantField, Collections.emptyMap());
    }
    
    @Override
    @Transactional(readOnly = true)
    public <T> Specification<T> getDataFilterSpecification(Authentication auth, Class<T> entityType, String plantField, Map<String, Object> context) {
        if (auth == null || !isUserPlantUser(auth)) {
            return null; // No filtering needed for non-plant users
        }
        
        List<String> userPlants = getUserPlantCodes(auth);
        if (userPlants.isEmpty()) {
            // User has no plant access, return specification that matches nothing
            return (root, query, criteriaBuilder) -> criteriaBuilder.disjunction();
        }
        
        return (root, query, criteriaBuilder) -> {
            if (plantField == null || plantField.trim().isEmpty()) {
                return criteriaBuilder.conjunction(); // No filtering if no plant field specified
            }
            
            return root.get(plantField).in(userPlants);
        };
    }
    
    @Override
    @Transactional(readOnly = true)
    public <T> List<T> filterDataByPlantAccess(Authentication auth, List<T> data, Function<T, String> plantFieldExtractor) {
        if (data == null || data.isEmpty()) {
            return Collections.emptyList();
        }
        
        if (auth == null || !isUserPlantUser(auth)) {
            return new ArrayList<>(data); // No filtering for non-plant users
        }
        
        List<String> userPlants = getUserPlantCodes(auth);
        if (userPlants.isEmpty()) {
            return Collections.emptyList(); // No plant access
        }
        
        return data.stream()
                .filter(item -> {
                    String plantCode = plantFieldExtractor.apply(item);
                    return plantCode != null && userPlants.contains(plantCode);
                })
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public <T> List<T> filterDataByMultiPlantAccess(Authentication auth, List<T> data, Function<T, List<String>> plantFieldExtractor) {
        if (data == null || data.isEmpty()) {
            return Collections.emptyList();
        }
        
        if (auth == null || !isUserPlantUser(auth)) {
            return new ArrayList<>(data); // No filtering for non-plant users
        }
        
        List<String> userPlants = getUserPlantCodes(auth);
        if (userPlants.isEmpty()) {
            return Collections.emptyList(); // No plant access
        }
        
        return data.stream()
                .filter(item -> {
                    List<String> plantCodes = plantFieldExtractor.apply(item);
                    return plantCodes != null && plantCodes.stream().anyMatch(userPlants::contains);
                })
                .collect(Collectors.toList());
    }
    
    // ========== USER ACCESS INFORMATION ==========
    
    @Override
    @Transactional(readOnly = true)
    public RoleType getUserPrimaryRoleType(Authentication auth) {
        if (auth == null) {
            return null;
        }
        
        String username = getUsernameFromAuth(auth);
        if (username == null) {
            return null;
        }
        
        User user = getUserByUsername(username);
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return null;
        }
        
        // Return the first role as primary (could be enhanced with priority logic)
        return user.getRoles().iterator().next().getRoleType();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<RoleType> getUserRoleTypes(Authentication auth) {
        if (auth == null) {
            return Collections.emptyList();
        }
        
        String username = getUsernameFromAuth(auth);
        if (username == null) {
            return Collections.emptyList();
        }
        
        User user = getUserByUsername(username);
        if (user == null || user.getRoles() == null) {
            return Collections.emptyList();
        }
        
        return user.getRoles().stream()
                .map(role -> role.getRoleType())
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<String> getUserPlantCodes(Authentication auth) {
        if (auth == null) {
            return Collections.emptyList();
        }
        
        String username = getUsernameFromAuth(auth);
        if (username == null) {
            return Collections.emptyList();
        }
        
        User user = getUserByUsername(username);
        if (user == null) {
            return Collections.emptyList();
        }
        
        // Get assigned plants from user
        String assignedPlants = user.getAssignedPlants();
        if (assignedPlants == null || assignedPlants.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        return Arrays.asList(assignedPlants.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public String getUserPrimaryPlant(Authentication auth) {
        if (auth == null) {
            return null;
        }
        
        String username = getUsernameFromAuth(auth);
        if (username == null) {
            return null;
        }
        
        User user = getUserByUsername(username);
        if (user == null) {
            return null;
        }
        
        return user.getPrimaryPlant();
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isUserAdmin(Authentication auth) {
        RoleType primaryRole = getUserPrimaryRoleType(auth);
        return primaryRole == RoleType.ADMIN;
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isUserPlantUser(Authentication auth) {
        RoleType primaryRole = getUserPrimaryRoleType(auth);
        return primaryRole == RoleType.PLANT_ROLE;
    }
    
    // ========== ACCESS DECISION UTILITIES ==========
    
    @Override
    @Transactional(readOnly = true)
    public AccessDecision makeAccessDecision(Authentication auth, String resourceType, String resourceId, String action, Map<String, Object> context) {
        if (auth == null) {
            return new AccessDecision(false, "No authentication provided");
        }
        
        RoleType primaryRole = getUserPrimaryRoleType(auth);
        if (primaryRole == null) {
            return new AccessDecision(false, "No valid role found");
        }
        
        // Admin has access to everything
        if (primaryRole == RoleType.ADMIN) {
            return new AccessDecision(true, "Admin access granted");
        }
        
        // Check resource-specific access
        boolean hasAccess = RBACConstants.canAccessResource(primaryRole, resourceType, action);
        String reason = hasAccess ? "Access granted based on role" : "Access denied based on role";
        
        AccessDecision decision = new AccessDecision(hasAccess, reason);
        decision.addDetail("userRole", primaryRole);
        decision.addDetail("resourceType", resourceType);
        decision.addDetail("resourceId", resourceId);
        decision.addDetail("action", action);
        
        return decision;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> generateUserAccessSummary(Authentication auth) {
        Map<String, Object> summary = new HashMap<>();
        
        if (auth == null) {
            summary.put("error", "No authentication provided");
            return summary;
        }
        
        String username = getUsernameFromAuth(auth);
        RoleType primaryRole = getUserPrimaryRoleType(auth);
        List<RoleType> allRoles = getUserRoleTypes(auth);
        List<String> plantCodes = getUserPlantCodes(auth);
        String primaryPlant = getUserPrimaryPlant(auth);
        List<String> accessibleScreens = getAccessibleScreens(auth);
        
        summary.put("username", username);
        summary.put("primaryRole", primaryRole != null ? primaryRole.name() : null);
        summary.put("allRoles", allRoles.stream().filter(role -> role != null).map(RoleType::name).collect(Collectors.toList()));
        summary.put("isAdmin", isUserAdmin(auth));
        summary.put("isPlantUser", isUserPlantUser(auth));
        summary.put("assignedPlants", plantCodes);
        summary.put("primaryPlant", primaryPlant);
        summary.put("accessibleScreens", accessibleScreens);
        
        Map<String, Object> restrictions = new HashMap<>();
        restrictions.put("plantFiltering", isUserPlantUser(auth));
        restrictions.put("screenRestrictions", !isUserAdmin(auth));
        restrictions.put("dataRestrictions", !isUserAdmin(auth));
        summary.put("restrictions", restrictions);
        
        return summary;
    }
    
    // ========== AUDIT AND LOGGING ==========
    
    @Override
    public void logAccessAttempt(Authentication auth, String resource, String action, boolean granted, Map<String, Object> context) {
        String username = getUsernameFromAuth(auth);
        logger.info("Access attempt: user={}, resource={}, action={}, granted={}, context={}", 
                   username, resource, action, granted, context);
    }
    
    @Override
    public void logScreenAccess(Authentication auth, String screenRoute, boolean granted, Map<String, Object> context) {
        String username = getUsernameFromAuth(auth);
        logger.info("Screen access: user={}, screen={}, granted={}, context={}", 
                   username, screenRoute, granted, context);
    }
    
    @Override
    public void logDataAccess(Authentication auth, String dataType, String dataId, boolean granted, Map<String, Object> context) {
        String username = getUsernameFromAuth(auth);
        logger.info("Data access: user={}, dataType={}, dataId={}, granted={}, context={}", 
                   username, dataType, dataId, granted, context);
    }
    
    // ========== HELPER METHODS ==========
    
    private String getUsernameFromAuth(Authentication auth) {
        if (auth == null) {
            return null;
        }
        
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            return (String) principal;
        } else if (principal instanceof User) {
            return ((User) principal).getUsername();
        }
        
        return null;
    }
    
    private Map<String, Object> createContextMap(String key, Object value) {
        Map<String, Object> context = new HashMap<>();
        context.put(key, value);
        return context;
    }
}