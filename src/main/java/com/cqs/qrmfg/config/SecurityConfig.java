package com.cqs.qrmfg.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import com.cqs.qrmfg.model.User;

import org.springframework.beans.factory.annotation.Autowired;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import com.cqs.qrmfg.filter.RBACAuthorizationFilter;
import com.cqs.qrmfg.service.RBACAuthorizationService;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private RBACAuthorizationService rbacAuthorizationService;
    @Autowired
    private RBACSecurityConfiguration rbacSecurityConfiguration;
    @Autowired
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
    
    @Bean
    public org.springframework.security.authentication.dao.DaoAuthenticationProvider authenticationProvider() {
        org.springframework.security.authentication.dao.DaoAuthenticationProvider authProvider = new org.springframework.security.authentication.dao.DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Use allowedOriginPatterns instead of allowedOrigins for better compatibility
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:*",
            "http://127.0.0.1:*"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
        // Define role hierarchy: ADMIN > TECH_USER > JVC_USER = CQS_USER > PLANT_USER
        String hierarchy = "ROLE_ADMIN > ROLE_TECH_USER \n" +
                          "ROLE_TECH_USER > ROLE_JVC_USER \n" +
                          "ROLE_TECH_USER > ROLE_CQS_USER \n" +
                          "ROLE_JVC_USER > ROLE_PLANT_USER \n" +
                          "ROLE_CQS_USER > ROLE_PLANT_USER";
        roleHierarchy.setHierarchy(hierarchy);
        return roleHierarchy;
    }
    
    @Bean
    public DefaultWebSecurityExpressionHandler customWebSecurityExpressionHandler() {
        DefaultWebSecurityExpressionHandler expressionHandler = new DefaultWebSecurityExpressionHandler();
        if (rbacSecurityConfiguration.isRoleHierarchyEnabled()) {
            expressionHandler.setRoleHierarchy(roleHierarchy());
        }
        return expressionHandler;
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors().configurationSource(corsConfigurationSource())
            .and()
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests()
                // Allow authentication endpoints first (most specific)
                .antMatchers("/qrmfg/api/v1/auth/**", "/api/v1/auth/**").permitAll()
                // Allow public endpoints
                .antMatchers("/qrmfg/api/v1/public/**", "/api/v1/public/**").permitAll()
                // Allow health check endpoints
                .antMatchers("/qrmfg/api/v1/test/**", "/api/v1/test/**", "/api/health").permitAll()
                // Allow questionnaire endpoints
                .antMatchers("/qrmfg/api/v1/questionnaire/**", "/api/v1/questionnaire/**").permitAll()
                
                // Allow frontend static resources
                .antMatchers("/", "/index.html", "/static/**", "/css/**", "/js/**", 
                           "/favicon.ico", "/manifest.json", "/logo192.png", "/logo512.png", 
                           "/robots.txt", "/asset-manifest.json").permitAll()
                // Allow React app static resources with /qrmfg prefix
                .antMatchers("/qrmfg/static/**", "/qrmfg/favicon.ico", "/qrmfg/manifest.json", 
                           "/qrmfg/logo192.png", "/qrmfg/logo512.png", "/qrmfg/robots.txt", 
                           "/qrmfg/asset-manifest.json").permitAll()
                // Allow React app routes (for client-side routing)
                .antMatchers("/qrmfg", "/qrmfg/", "/qrmfg/dashboard", "/qrmfg/workflows", "/qrmfg/users", 
                           "/qrmfg/roles", "/qrmfg/login", "/qrmfg/admin/**").permitAll()
                
                // All API endpoints require authentication (except those explicitly permitted above)
                .antMatchers("/qrmfg/api/v1/**", "/api/v1/**").authenticated()
                // Allow all other requests (for frontend routing)
                .anyRequest().permitAll()
            .and()
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            
        // Temporarily disable RBAC filter to test admin endpoints
        // Add RBAC filter only if RBAC is enabled
        // if (rbacSecurityConfiguration.isEnabled()) {
        //     http.addFilterAfter(
        //         new RBACAuthorizationFilter(rbacAuthorizationService), 
        //         FilterSecurityInterceptor.class
        //     );
        // }
        
        return http.build();
    }


} 