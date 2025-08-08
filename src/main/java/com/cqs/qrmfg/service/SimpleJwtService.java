package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Simple JWT service for RBAC token generation
 * Compatible with older JWT library versions
 */
@Service
public class SimpleJwtService {
    
    @Value("${jwt.secret:qrmfg-secret-key-for-jwt-token-generation-and-validation}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:86400}") // 24 hours in seconds
    private int jwtExpirationInSeconds;
    
    /**
     * Generate JWT token with RBAC information
     */
    public String generateTokenWithRBAC(User user) {
        Map<String, Object> claims = new HashMap<>();
        
        // Basic user information
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());
        claims.put("userId", user.getId());
        
        // Debug logging
        System.out.println("JWT Generation Debug for user: " + user.getUsername());
        System.out.println("User roles count: " + (user.getRoles() != null ? user.getRoles().size() : "null"));
        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> {
                System.out.println("Role: " + role.getName() + ", RoleType: " + role.getRoleType());
            });
        }
        System.out.println("Primary role type: " + user.getPrimaryRoleType());
        System.out.println("Is admin: " + user.isAdmin());
        
        // Role information
        if (user.getPrimaryRoleType() != null) {
            claims.put("primaryRoleType", user.getPrimaryRoleType().name());
            claims.put("roleType", user.getPrimaryRoleType().name());
        }
        
        // All roles
        List<String> roleNames = user.getRoles().stream()
            .map(role -> role.getName())
            .collect(Collectors.toList());
        claims.put("roles", roleNames);
        claims.put("authorities", roleNames);
        
        // Plant information for plant users
        if (user.supportsPlantFiltering()) {
            claims.put("plantCodes", user.getAssignedPlantsList());
            claims.put("assignedPlants", user.getAssignedPlantsList());
            claims.put("primaryPlant", user.getPrimaryPlant());
            claims.put("primaryPlantCode", user.getPrimaryPlant());
        }
        
        // Admin and role flags
        claims.put("isAdmin", user.isAdmin());
        claims.put("isPlantUser", user.isPlantUser());
        
        System.out.println("JWT Claims - roles: " + roleNames);
        System.out.println("JWT Claims - isAdmin: " + user.isAdmin());
        
        return createToken(claims, user.getUsername());
    }
    
    /**
     * Create JWT token with claims
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInSeconds * 1000L);
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, jwtSecret.getBytes())
            .compact();
    }
    
    /**
     * Extract username from JWT token
     */
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }
    
    /**
     * Extract all claims from JWT token
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
            .setSigningKey(jwtSecret.getBytes())
            .parseClaimsJws(token)
            .getBody();
    }
    
    /**
     * Check if JWT token is expired
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getClaimsFromToken(token).getExpiration();
        return expiration.before(new Date());
    }
    
    /**
     * Validate JWT token
     */
    public boolean validateToken(String token, String username) {
        try {
            String tokenUsername = getUsernameFromToken(token);
            return (tokenUsername.equals(username) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }
}