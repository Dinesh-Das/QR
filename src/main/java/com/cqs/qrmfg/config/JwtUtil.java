package com.cqs.qrmfg.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.model.Role;
import java.util.Date;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JwtUtil {
    @Value("${jwt.secret:qrmfg-secret-key-for-jwt-token-generation-and-validation}")
    private String secret;

    @Value("${jwt.expiration:604800000}")
    private long jwtExpirationInMs;

    public String generateToken(User user) {
        // Extract roles from user
        String[] roles = user.getRoles().stream()
                .map(Role::getName)
                .toArray(String[]::new);
        
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("username", user.getUsername())
                .claim("roles", roles)
                .claim("authorities", roles) // Alternative role claim
                .claim("email", user.getEmail())
                .claim("userId", user.getId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationInMs))
                .signWith(SignatureAlgorithm.HS512, secret.getBytes())
                .compact();
    }
    
    // Keep the old method for backward compatibility
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .claim("username", username)
                .claim("roles", new String[]{"USER"}) // Default role
                .claim("authorities", new String[]{"USER"}) // Alternative role claim
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationInMs))
                .signWith(SignatureAlgorithm.HS512, secret.getBytes())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secret.getBytes())
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
} 