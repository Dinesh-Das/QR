package com.cqs.qrmfg.config;

import com.cqs.qrmfg.service.impl.UserDetailsServiceImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Skip JWT validation for auth endpoints and public resources
        String requestPath = request.getRequestURI();
        logger.info("JwtAuthenticationFilter: Processing request: {}", requestPath);
        if (shouldSkipJwtValidation(requestPath)) {
            logger.info("JwtAuthenticationFilter: Skipping JWT validation for path: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }
        logger.info("JwtAuthenticationFilter: Applying JWT validation for path: {}", requestPath);
        
        final String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (ExpiredJwtException e) {
                logger.warn("JWT token has expired: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"JWT token has expired\",\"message\":\"Please login again\"}");
                return;
            } catch (MalformedJwtException e) {
                logger.warn("Invalid JWT token: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid JWT token\",\"message\":\"Please login again\"}");
                return;
            } catch (SignatureException e) {
                logger.warn("JWT signature validation failed: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"JWT signature validation failed\",\"message\":\"Please login again\"}");
                return;
            } catch (Exception e) {
                logger.error("Error processing JWT token: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Error processing JWT token\",\"message\":\"Please login again\"}");
                return;
            }
        }
        
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtUtil.validateToken(jwt, userDetails.getUsername())) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                logger.error("Error validating JWT token: {}", e.getMessage());
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Determine if JWT validation should be skipped for this request path
     */
    private boolean shouldSkipJwtValidation(String requestPath) {
        // Skip JWT validation for auth endpoints
        if (requestPath.startsWith("/qrmfg/api/v1/auth/") || 
            requestPath.startsWith("/api/v1/auth/")) {
            return true;
        }
        
        // Skip JWT validation for public endpoints
        if (requestPath.startsWith("/qrmfg/api/v1/public/") || 
            requestPath.startsWith("/api/v1/public/")) {
            return true;
        }
        
        // Skip JWT validation for static resources
        if (requestPath.startsWith("/qrmfg/static/") || 
            requestPath.startsWith("/static/") ||
            requestPath.startsWith("/css/") ||
            requestPath.startsWith("/js/") ||
            requestPath.matches(".*\\.(js|css|html|png|jpg|jpeg|gif|ico|woff|woff2|ttf|svg|json)$")) {
            return true;
        }
        
        // Skip JWT validation for frontend routes
        if (requestPath.equals("/qrmfg") || 
            requestPath.equals("/qrmfg/") ||
            requestPath.startsWith("/qrmfg/login") ||
            requestPath.startsWith("/qrmfg/dashboard") ||
            requestPath.startsWith("/qrmfg/admin") ||
            requestPath.equals("/") ||
            requestPath.equals("/favicon.ico") ||
            requestPath.equals("/manifest.json")) {
            return true;
        }
        
        return false;
    }
} 