package com.cqs.qrmfg.config;

import com.cqs.qrmfg.model.Role;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.enums.RoleType;
import com.cqs.qrmfg.repository.RoleRepository;
import com.cqs.qrmfg.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Data initializer to create default roles and admin user
 */
@Component
public class DataInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting data initialization...");
        
        try {
            initializeRoles();
            initializeAdminUser();
            logger.info("Data initialization completed successfully");
        } catch (Exception e) {
            logger.error("Error during data initialization", e);
        }
    }
    
    private void initializeRoles() {
        logger.info("Initializing default roles...");
        
        for (RoleType roleType : RoleType.values()) {
            if (!roleRepository.existsByName(roleType.getRoleName())) {
                Role role = new Role();
                role.setName(roleType.getRoleName());
                role.setDescription(roleType.getDescription());
                role.setRoleType(roleType);
                role.setEnabled(true);
                role.setCreatedAt(LocalDateTime.now());
                role.setUpdatedAt(LocalDateTime.now());
                role.setCreatedBy("system");
                role.setUpdatedBy("system");
                
                roleRepository.save(role);
                logger.info("Created role: {}", roleType.getRoleName());
            } else {
                logger.debug("Role already exists: {}", roleType.getRoleName());
            }
        }
    }
    
    private void initializeAdminUser() {
        logger.info("Initializing admin user...");
        
        if (!userRepository.existsByUsername("admin")) {
            // Create admin user
            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setEmail("admin@qrmfg.com");
            adminUser.setPassword(passwordEncoder.encode("admin123"));
            adminUser.setEnabled(true);
            adminUser.setStatus("ACTIVE");
            adminUser.setCreatedAt(LocalDateTime.now());
            adminUser.setUpdatedAt(LocalDateTime.now());
            
            // Assign ADMIN role
            Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
            if (adminRole != null) {
                Set<Role> roles = new HashSet<>();
                roles.add(adminRole);
                adminUser.setRoles(roles);
            }
            
            userRepository.save(adminUser);
            logger.info("Created admin user with ADMIN role");
        } else {
            logger.debug("Admin user already exists");
            
            // Ensure admin user has ADMIN role and correct password
            User adminUser = userRepository.findByUsername("admin").orElse(null);
            if (adminUser != null) {
                boolean needsUpdate = false;
                
                // Check and update password
                String currentPassword = adminUser.getPassword();
                if (currentPassword == null || !currentPassword.startsWith("$2a$") || currentPassword.equals("admin")) {
                    adminUser.setPassword(passwordEncoder.encode("admin123"));
                    needsUpdate = true;
                    logger.info("Updated admin user password to use proper BCrypt encoding");
                }
                
                // Check and update role
                Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
                if (adminRole != null && !adminUser.getRoles().contains(adminRole)) {
                    adminUser.getRoles().add(adminRole);
                    needsUpdate = true;
                    logger.info("Added ADMIN role to existing admin user");
                }
                
                if (needsUpdate) {
                    userRepository.save(adminUser);
                }
            }
        }
        
        // Fix any existing users with plain text passwords
        fixPlainTextPasswords();
    }
    
    private void fixPlainTextPasswords() {
        logger.info("Checking for users with plain text passwords...");
        
        try {
            // Get all users
            Iterable<User> allUsers = userRepository.findAll();
            
            for (User user : allUsers) {
                String currentPassword = user.getPassword();
                boolean needsUpdate = false;
                
                // Check if password is not BCrypt encoded (BCrypt passwords start with $2a$, $2b$, or $2y$)
                if (currentPassword != null && 
                    !currentPassword.startsWith("$2a$") && 
                    !currentPassword.startsWith("$2b$") && 
                    !currentPassword.startsWith("$2y$")) {
                    
                    // This is likely a plain text password, encode it
                    String encodedPassword = passwordEncoder.encode(currentPassword);
                    user.setPassword(encodedPassword);
                    needsUpdate = true;
                    logger.info("Fixed plain text password for user: {} (kept original password)", user.getUsername());
                }
                
                // For users with null or empty passwords, set username as password (common dev pattern)
                if (currentPassword == null || currentPassword.trim().isEmpty()) {
                    String defaultPassword = passwordEncoder.encode(user.getUsername());
                    user.setPassword(defaultPassword);
                    needsUpdate = true;
                    logger.warn("Set username as password for user with null/empty password: {}", user.getUsername());
                }
                
                // Special handling for users that might have corrupted passwords
                // If password doesn't look right and user is not admin, reset to username
                if (!user.getUsername().equals("admin") && currentPassword != null && 
                    !currentPassword.startsWith("$2a$") && 
                    !currentPassword.startsWith("$2b$") && 
                    !currentPassword.startsWith("$2y$") &&
                    currentPassword.length() > 50) { // Likely corrupted
                    
                    String usernamePassword = passwordEncoder.encode(user.getUsername());
                    user.setPassword(usernamePassword);
                    needsUpdate = true;
                    logger.warn("Reset corrupted password to username for user: {}", user.getUsername());
                }
                
                if (needsUpdate) {
                    user.setUpdatedAt(LocalDateTime.now());
                    userRepository.save(user);
                }
            }
        } catch (Exception e) {
            logger.error("Error while fixing plain text passwords", e);
        }
    }
}