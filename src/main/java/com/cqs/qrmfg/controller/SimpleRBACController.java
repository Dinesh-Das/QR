package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.service.RBACAuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simplified RBAC Controller for frontend integration
 */
@RestController
@RequestMapping("/api/v1/rbac")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"}, maxAge = 3600, allowCredentials = "true")
public class SimpleRBACController {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleRBACController.class);
    
    @Autowired
    private RBACAuthorizationService rbacService;
    
    /**
     * Check if user has access to a specific screen/route
     */
    @GetMapping(value = "/screen-access", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkScreenAccess(
            @RequestParam String route, 
            Authentication auth) {
        
        logger.info("SimpleRBACController: Checking screen access for route: {}", route);
        
        try {
            boolean hasAccess = rbacService.hasScreenAccess(auth, route);
            
            Map<String, Object> response = new HashMap<>();
            response.put("hasAccess", hasAccess);
            response.put("route", route);
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("SimpleRBACController: Error checking screen access for route: {}", route, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("hasAccess", false);
            errorResponse.put("route", route);
            errorResponse.put("message", "Failed to check screen access");
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * Get list of accessible screens for current user
     */
    @GetMapping(value = "/accessible-screens", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAccessibleScreens(Authentication auth) {
        logger.info("SimpleRBACController: Getting accessible screens for user");
        
        try {
            List<String> screens = rbacService.getAccessibleScreens(auth);
            
            Map<String, Object> response = new HashMap<>();
            response.put("screens", screens);
            response.put("total", screens.size());
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("SimpleRBACController: Error getting accessible screens", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("screens", new ArrayList<>());
            errorResponse.put("total", 0);
            errorResponse.put("message", "Failed to get accessible screens");
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * Check if user has access to specific data type
     */
    @PostMapping("/data-access")
    public ResponseEntity<Map<String, Object>> checkDataAccess(
            @RequestBody Map<String, Object> request, 
            Authentication auth) {
        
        String dataType = (String) request.get("dataType");
        Map<String, Object> context = (Map<String, Object>) request.getOrDefault("context", new HashMap<>());
        
        logger.info("SimpleRBACController: Checking data access for type: {}", dataType);
        
        try {
            boolean hasAccess = rbacService.hasDataAccess(auth, dataType, context);
            
            Map<String, Object> response = new HashMap<>();
            response.put("hasAccess", hasAccess);
            response.put("dataType", dataType);
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("SimpleRBACController: Error checking data access for type: {}", dataType, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("hasAccess", false);
            errorResponse.put("dataType", dataType);
            errorResponse.put("message", "Failed to check data access");
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * Get user's access summary
     */
    @GetMapping("/user/access-summary")
    public ResponseEntity<Map<String, Object>> getUserAccessSummary(Authentication auth) {
        logger.info("SimpleRBACController: Getting access summary for user");
        
        try {
            Map<String, Object> summary = rbacService.generateUserAccessSummary(auth);
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", summary);
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("SimpleRBACController: Error getting access summary", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to get access summary");
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}