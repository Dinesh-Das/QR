package com.cqs.qrmfg.filter;

import com.cqs.qrmfg.enums.RoleType;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.service.RBACAuthorizationService;

import com.cqs.qrmfg.util.RBACConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enhanced RBAC authorization filter that extends the existing screen role access functionality
 * with comprehensive role-based access control for all five roles and plant-based filtering.
 * 
 * This filter handles:
 * - Role-based screen access validation for all five roles (ADMIN, JVC_ROLE, CQS_ROLE, TECH_ROLE, PLANT_ROLE)
 * - Plant-based access control for PLANT_ROLE users
 * - Comprehensive access logging and audit trails
 * - Integration with existing screen role mapping system
 */
public class RBACAuthorizationFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(RBACAuthorizationFilter.class);
    
    private final RBACAuthorizationService rbacAuthorizationService;

    
    // API path patterns that require RBAC validation
    private static final String ADMIN_API_PREFIX_QRMFG = "/qrmfg/api/v1/admin/";
    private static final String ADMIN_API_PREFIX = "/api/v1/admin/";
    private static final String API_PREFIX_QRMFG = "/qrmfg/api/v1/";
    private static final String API_PREFIX = "/api/v1/";

    
    public RBACAuthorizationFilter(RBACAuthorizationService rbacAuthorizationService) {
        this.rbacAuthorizationService = rbacAuthorizationService;
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("Initializing RBAC Authorization Filter");
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestPath = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        
        logger.debug("Processing request: {} {}", method, requestPath);
        
        try {
            // Skip RBAC validation for non-API requests and public endpoints
            if (!requiresRBACValidation(requestPath)) {
                logger.debug("Skipping RBAC validation for path: {}", requestPath);
                chain.doFilter(request, response);
                return;
            }
            
            // Get authentication from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("No valid authentication found for protected resource: {}", requestPath);
                sendForbiddenResponse(httpResponse, "Authentication required");
                return;
            }
            
            // Perform RBAC authorization check
            boolean accessGranted = performRBACAuthorization(authentication, requestPath, method, httpRequest);
            
            if (accessGranted) {
                logger.debug("Access granted for user {} to {}", getUsernameFromAuth(authentication), requestPath);
                chain.doFilter(request, response);
            } else {
                logger.warn("Access denied for user {} to {}", getUsernameFromAuth(authentication), requestPath);
                sendForbiddenResponse(httpResponse, "Access denied: insufficient privileges");
            }
            
        } catch (Exception e) {
            logger.error("Error during RBAC authorization for path: {}", requestPath, e);
            sendInternalErrorResponse(httpResponse, "Authorization error");
        }
    }
    
    @Override
    public void destroy() {
        logger.info("Destroying RBAC Authorization Filter");
    }
    
    /**
     * Determine if a request path requires RBAC validation
     */
    private boolean requiresRBACValidation(String requestPath) {
        // Skip validation for public endpoints
        if (requestPath.startsWith("/qrmfg/api/v1/auth/") || 
            requestPath.startsWith("/api/v1/auth/") ||
            requestPath.startsWith("/qrmfg/api/v1/public/") ||
            requestPath.startsWith("/api/v1/public/") ||
            requestPath.startsWith("/api/health") ||
            requestPath.startsWith("/qrmfg/api/v1/test/") ||
            requestPath.startsWith("/api/v1/test/")) {
            return false;
        }
        
        // Skip validation for static resources
        if (requestPath.matches(".*\\.(js|css|html|png|jpg|jpeg|gif|ico|woff|woff2|ttf|svg|json)$")) {
            return false;
        }
        
        // Skip validation for specific files
        if (requestPath.endsWith("/manifest.json") || 
            requestPath.endsWith("/favicon.ico") ||
            requestPath.endsWith("/robots.txt")) {
            return false;
        }
        
        // Skip validation for frontend routes (handled by client-side routing)
        if (requestPath.equals("/") || 
            requestPath.equals("/index.html") ||
            requestPath.startsWith("/static/") ||
            requestPath.matches("^/(dashboard|workflows|users|roles|login|admin).*")) {
            return false;
        }
        
        // Require validation for all API endpoints
        return requestPath.startsWith(API_PREFIX_QRMFG) || requestPath.startsWith(API_PREFIX);
    }
    
    /**
     * Perform comprehensive RBAC authorization check
     */
    private boolean performRBACAuthorization(Authentication authentication, String requestPath, 
                                           String method, HttpServletRequest request) {
        
        User user = getUserFromAuth(authentication);
        if (user == null) {
            logger.warn("Could not extract user from authentication for path: {}", requestPath);
            return false;
        }
        
        RoleType primaryRole = user.getPrimaryRoleType();
        if (primaryRole == null) {
            logger.warn("User {} has no valid primary role for path: {}", user.getUsername(), requestPath);
            logAccessAttempt(authentication, requestPath, method, false, "No valid role");
            return false;
        }
        
        // Create context for access decision
        Map<String, Object> context = createRequestContext(request, method);
        

        
        // Admin users have full access
        if (primaryRole == RoleType.ADMIN) {
            logAccessAttempt(authentication, requestPath, method, true, "Admin access");
            return true;
        }
        
        // For admin API endpoints, perform enhanced role-based validation
        if (requestPath.startsWith(ADMIN_API_PREFIX_QRMFG) || requestPath.startsWith(ADMIN_API_PREFIX)) {
            return validateAdminAPIAccess(authentication, requestPath, method, context);
        }
        
        // For regular API endpoints, check basic role access
        return validateAPIAccess(authentication, requestPath, method, context);
    }
    
    /**
     * Validate access to admin API endpoints with enhanced RBAC logic
     */
    private boolean validateAdminAPIAccess(Authentication authentication, String requestPath, 
                                         String method, Map<String, Object> context) {
        
        User user = getUserFromAuth(authentication);
        RoleType primaryRole = user.getPrimaryRoleType();
        
        // Remove admin prefix for route matching (handle both patterns)
        String route = requestPath;
        if (route.startsWith(ADMIN_API_PREFIX_QRMFG)) {
            route = route.replaceFirst("^" + ADMIN_API_PREFIX_QRMFG.replace("/", "\\/"), "");
        } else if (route.startsWith(ADMIN_API_PREFIX)) {
            route = route.replaceFirst("^" + ADMIN_API_PREFIX.replace("/", "\\/"), "");
        }
        
        // Check screen access using RBAC service
        boolean hasScreenAccess = rbacAuthorizationService.hasScreenAccess(authentication, route);
        
        if (!hasScreenAccess) {
            logAccessAttempt(authentication, requestPath, method, false, "Screen access denied");
            return false;
        }
        
        // For PLANT_ROLE users, perform additional plant-based validation
        if (primaryRole == RoleType.PLANT_ROLE) {
            return validatePlantBasedAccess(authentication, requestPath, method, context);
        }
        
        // For other roles, grant access based on RBAC service validation
        logAccessAttempt(authentication, requestPath, method, true, "RBAC screen access granted");
        return true;
    }
    
    /**
     * Validate access to regular API endpoints
     */
    private boolean validateAPIAccess(Authentication authentication, String requestPath, 
                                    String method, Map<String, Object> context) {
        
        User user = getUserFromAuth(authentication);
        RoleType primaryRole = user.getPrimaryRoleType();
        
        // Extract data type from API path for data access validation
        String dataType = extractDataTypeFromPath(requestPath);
        
        if (dataType != null) {
            // Check data access permissions
            boolean hasDataAccess = rbacAuthorizationService.hasDataAccess(authentication, dataType, context);
            
            if (!hasDataAccess) {
                logAccessAttempt(authentication, requestPath, method, false, "Data access denied");
                return false;
            }
            
            // For PLANT_ROLE users, perform plant-based data filtering validation
            if (primaryRole == RoleType.PLANT_ROLE) {
                return validatePlantDataAccess(authentication, requestPath, dataType, method, context);
            }
        }
        
        logAccessAttempt(authentication, requestPath, method, true, "API access granted");
        return true;
    }
    
    /**
     * Validate plant-based access for PLANT_ROLE users
     */
    private boolean validatePlantBasedAccess(Authentication authentication, String requestPath, 
                                           String method, Map<String, Object> context) {
        
        User user = getUserFromAuth(authentication);
        
        // Extract plant code from request parameters or path
        String plantCode = extractPlantCodeFromRequest(requestPath, context);
        
        if (plantCode != null) {
            // Check if user has access to the specific plant
            boolean hasPlantAccess = user.hasPlantAccess(plantCode);
            
            if (!hasPlantAccess) {
                context.put("plantCode", plantCode);
                context.put("reason", "Plant access denied");
                logAccessAttempt(authentication, requestPath, method, false, "Plant access denied for: " + plantCode);
                return false;
            }
            
            context.put("plantCode", plantCode);
        }
        
        logAccessAttempt(authentication, requestPath, method, true, "Plant-based access granted");
        return true;
    }
    
    /**
     * Validate plant-based data access for PLANT_ROLE users
     */
    private boolean validatePlantDataAccess(Authentication authentication, String requestPath, 
                                          String dataType, String method, Map<String, Object> context) {
        
        String plantCode = extractPlantCodeFromRequest(requestPath, context);
        
        if (plantCode != null) {
            boolean hasPlantDataAccess = rbacAuthorizationService.hasPlantDataAccess(
                authentication, dataType, plantCode, context);
            
            if (!hasPlantDataAccess) {
                context.put("plantCode", plantCode);
                context.put("dataType", dataType);
                logAccessAttempt(authentication, requestPath, method, false, 
                    "Plant data access denied for: " + dataType + " at plant: " + plantCode);
                return false;
            }
        }
        
        logAccessAttempt(authentication, requestPath, method, true, "Plant data access granted");
        return true;
    }
    

    
    /**
     * Extract data type from API path
     */
    private String extractDataTypeFromPath(String requestPath) {
        // Extract data type from common API patterns
        if (requestPath.contains("/documents")) return "Document";
        if (requestPath.contains("/workflows")) return "Workflow";
        if (requestPath.contains("/queries")) return "Query";
        if (requestPath.contains("/users")) return "User";
        if (requestPath.contains("/roles")) return "Role";
        if (requestPath.contains("/projects")) return "Project";
        if (requestPath.contains("/reports")) return "Report";
        
        return null;
    }
    
    /**
     * Extract plant code from request path or parameters
     */
    private String extractPlantCodeFromRequest(String requestPath, Map<String, Object> context) {
        // Try to extract from context first (set by previous filters or controllers)
        Object plantCodeObj = context.get("plantCode");
        if (plantCodeObj instanceof String) {
            return RBACConstants.normalizePlantCode((String) plantCodeObj);
        }
        
        // Try to extract from path parameters (e.g., /api/v1/plant/PLANT001/data)
        String[] pathSegments = requestPath.split("/");
        for (int i = 0; i < pathSegments.length - 1; i++) {
            if ("plant".equals(pathSegments[i]) && i + 1 < pathSegments.length) {
                String potentialPlantCode = pathSegments[i + 1];
                if (RBACConstants.isValidPlantCodeFormat(potentialPlantCode)) {
                    return RBACConstants.normalizePlantCode(potentialPlantCode);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Create request context for access decisions
     */
    private Map<String, Object> createRequestContext(HttpServletRequest request, String method) {
        Map<String, Object> context = new HashMap<>();
        
        context.put("method", method);
        context.put("remoteAddr", request.getRemoteAddr());
        context.put("userAgent", request.getHeader("User-Agent"));
        context.put("timestamp", System.currentTimeMillis());
        
        // Add query parameters to context
        if (request.getQueryString() != null) {
            context.put("queryString", request.getQueryString());
        }
        
        // Add plant code from request parameters if present
        String plantParam = request.getParameter("plantCode");
        if (plantParam != null && RBACConstants.isValidPlantCodeFormat(plantParam)) {
            context.put("plantCode", RBACConstants.normalizePlantCode(plantParam));
        }
        
        return context;
    }
    
    /**
     * Get User entity from Authentication object
     */
    private User getUserFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.debug("Authentication or principal is null");
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        logger.debug("Principal type: {}, Principal: {}", principal.getClass().getName(), principal);
        
        if (principal instanceof User) {
            logger.debug("Principal is User instance");
            return (User) principal;
        }
        
        // Handle UserDetails from JWT authentication
        if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            logger.debug("Principal is UserDetails, username: {}", userDetails.getUsername());
            
            // If UserDetails is actually a User instance (common in Spring Security)
            if (userDetails instanceof User) {
                logger.debug("UserDetails is actually User instance");
                return (User) userDetails;
            }
            
            // Otherwise, try to get the User by username using the authorization service
            try {
                User user = rbacAuthorizationService.getUserByUsername(userDetails.getUsername());
                if (user != null) {
                    logger.debug("Successfully loaded user by username: {}", userDetails.getUsername());
                } else {
                    logger.warn("User not found by username: {}", userDetails.getUsername());
                }
                return user;
            } catch (Exception e) {
                logger.warn("Could not load user by username: {}", userDetails.getUsername(), e);
                return null;
            }
        }
        
        // Handle String principal (username)
        if (principal instanceof String) {
            String username = (String) principal;
            logger.debug("Principal is String username: {}", username);
            try {
                User user = rbacAuthorizationService.getUserByUsername(username);
                if (user != null) {
                    logger.debug("Successfully loaded user by username: {}", username);
                } else {
                    logger.warn("User not found by username: {}", username);
                }
                return user;
            } catch (Exception e) {
                logger.warn("Could not load user by username: {}", username, e);
                return null;
            }
        }
        
        logger.warn("Unknown principal type: {}", principal.getClass().getName());
        return null;
    }
    
    /**
     * Get username from Authentication object
     */
    private String getUsernameFromAuth(Authentication authentication) {
        if (authentication == null) {
            return "unknown";
        }
        
        User user = getUserFromAuth(authentication);
        if (user != null) {
            return user.getUsername();
        }
        
        return authentication.getName() != null ? authentication.getName() : "unknown";
    }
    
    /**
     * Log access attempt with comprehensive details
     */
    private void logAccessAttempt(Authentication authentication, String resource, String method, 
                                boolean granted, String reason) {
        
        Map<String, Object> logContext = new HashMap<>();
        logContext.put("method", method);
        logContext.put("reason", reason);
        logContext.put("timestamp", System.currentTimeMillis());
        
        rbacAuthorizationService.logAccessAttempt(authentication, resource, method, granted, logContext);
    }
    
    /**
     * Send forbidden response
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Access Denied\",\"message\":\"" + message + "\"}");
    }
    
    /**
     * Send internal error response
     */
    private void sendInternalErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Internal Error\",\"message\":\"" + message + "\"}");
    }
}