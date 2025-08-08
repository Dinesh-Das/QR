package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.service.SimpleJwtService;
import com.cqs.qrmfg.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Simple authentication controller with RBAC support
 */
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"}, maxAge = 3600, allowCredentials = "true")
public class SimpleAuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleAuthController.class);
    
    @Autowired(required = false)
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private SimpleJwtService jwtService;
    
    @Autowired
    private UserService userService;
    
    /**
     * Test endpoint to check if controller is reachable
     */
    @GetMapping(value = "/test", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> test() {
        logger.info("SimpleAuthController: Test endpoint reached!");
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Auth controller is working");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Enhanced login endpoint with RBAC information
     */
    @PostMapping(value = "/login", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        logger.info("SimpleAuthController: Login endpoint reached!");
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");
        
        logger.info("SimpleAuthController: Login attempt for user: {}", username);
        
        try {
            // Authenticate user (if authentication manager is available)
            if (authenticationManager != null) {
                Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
                );
            }
            
            // Get user details
            User user = userService.findByUsername(username);
            if (user == null) {
                logger.warn("SimpleAuthController: User not found: {}", username);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "Invalid username or password");
                errorResponse.put("status", "error");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Generate JWT token with RBAC information
            String token = jwtService.generateTokenWithRBAC(user);
            
            // Prepare response with RBAC data
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            
            // Role information
            if (user.getPrimaryRoleType() != null) {
                response.put("primaryRole", user.getPrimaryRoleType().name());
            }
            response.put("roles", user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toList()));
            
            // User type flags
            response.put("isAdmin", user.isAdmin());
            response.put("isPlantUser", user.isPlantUser());
            
            // Plant information for plant users
            if (user.supportsPlantFiltering()) {
                response.put("plantCodes", user.getAssignedPlantsList());
                response.put("primaryPlant", user.getPrimaryPlant());
            }
            
            response.put("status", "success");
            response.put("message", "Login successful");
            
            logger.info("SimpleAuthController: Login successful for user: {}", username);
            return ResponseEntity.ok(response);
            
        } catch (AuthenticationException e) {
            logger.warn("SimpleAuthController: Authentication failed for user: {}", username, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Invalid username or password");
            errorResponse.put("status", "error");
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("SimpleAuthController: Login error for user: {}", username, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Login failed: " + e.getMessage());
            errorResponse.put("status", "error");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Token validation endpoint
     */
    @PostMapping(value = "/validate", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        
        try {
            if (token == null || token.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("valid", false);
                errorResponse.put("message", "Token is required");
                errorResponse.put("status", "error");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            String username = jwtService.getUsernameFromToken(token);
            boolean isValid = jwtService.validateToken(token, username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", isValid);
            response.put("username", isValid ? username : null);
            response.put("status", "success");
            
            if (isValid) {
                // Add token claims for frontend
                try {
                    Map<String, Object> claims = jwtService.getClaimsFromToken(token);
                    response.put("roles", claims.get("roles"));
                    response.put("primaryRoleType", claims.get("primaryRoleType"));
                    response.put("plantCodes", claims.get("plantCodes"));
                    response.put("primaryPlant", claims.get("primaryPlant"));
                    response.put("isAdmin", claims.get("isAdmin"));
                    response.put("isPlantUser", claims.get("isPlantUser"));
                } catch (Exception e) {
                    logger.warn("Could not extract claims from token", e);
                }
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("SimpleAuthController: Token validation error", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("valid", false);
            errorResponse.put("message", "Token validation failed: " + e.getMessage());
            errorResponse.put("status", "error");
            return ResponseEntity.ok(errorResponse);
        }
    }
}