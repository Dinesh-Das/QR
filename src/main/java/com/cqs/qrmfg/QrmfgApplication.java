// Legacy main application. New RBAC backend will be under com.cqs.qrmfg.rbac_app
package com.cqs.qrmfg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.model.Role;
import com.cqs.qrmfg.repository.UserRepository;
import com.cqs.qrmfg.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalDateTime;

import com.cqs.qrmfg.model.QrmfgLocationMaster;
import com.cqs.qrmfg.repository.QrmfgLocationMasterRepository;
import com.cqs.qrmfg.constants.RoleConstants;
import com.cqs.qrmfg.enums.RoleType;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// Security configuration will be implemented in com.cqs.qrmfg.config according to Spring Security and JWT best practices.
@SpringBootApplication
@EnableScheduling
public class QrmfgApplication {

	@Autowired
	private PasswordEncoder passwordEncoder;

	public static void main(String[] args) {
		SpringApplication.run(QrmfgApplication.class, args);
	}

	@Bean
	public CommandLineRunner ensureAdminUser(
        UserRepository userRepository,
        RoleRepository roleRepository,
        QrmfgLocationMasterRepository locationRepository,
        PasswordEncoder passwordEncoder) {
    return args -> {
        try {
            initializeRoleBasedAuth(userRepository, roleRepository, passwordEncoder);
            initializeLocationMaster(locationRepository);
        } catch (Exception e) {
            System.err.println("Error during initialization: " + e.getMessage());
            e.printStackTrace();
            // Don't fail the application startup for this
        }
    };
}

@Transactional
private void initializeRoleBasedAuth(
        UserRepository userRepository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder) {
    
    System.out.println("Starting Role-Based Authentication initialization...");
    
    // Initialize all required roles first
    initializeRoles(roleRepository);
    
    // Initialize admin user
    initializeAdminUser(userRepository, roleRepository, passwordEncoder);
    
    System.out.println("Role-Based Authentication initialization completed successfully");
        }
        
private void initializeRoles(RoleRepository roleRepository) {
    System.out.println("Initializing roles...");
    
    // Create all required roles
    createRoleIfNotExists(roleRepository, RoleConstants.ADMIN, "System administrator with full access");
    createRoleIfNotExists(roleRepository, RoleConstants.JVC_USER, "JVC User with specific module access");
    createRoleIfNotExists(roleRepository, RoleConstants.PLANT_USER, "Plant User with plant-specific access");
    createRoleIfNotExists(roleRepository, RoleConstants.CQS_USER, "CQS User with quality control access");
    createRoleIfNotExists(roleRepository, RoleConstants.TECH_USER, "Technical User with technical module access");
    createRoleIfNotExists(roleRepository, RoleConstants.VIEWER, "Read-only access to reports and dashboards");
    
    System.out.println("Role initialization completed");
}

private void initializeAdminUser(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
    String adminUsername = "admin";
    String adminPassword = "admin";
    String adminEmail = "admin@qrmfg.com";

    // Get or create admin user
    User admin = userRepository.findByUsername(adminUsername).orElse(null);
    
    if (admin == null) {
        System.out.println("Creating default admin user...");
        admin = new User();
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setEmail(adminEmail);
        admin.setStatus("ACTIVE");
        admin.setEnabled(true);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
    } else {
        System.out.println("Admin user already exists, updating role assignment...");
        admin.setUpdatedAt(LocalDateTime.now());
    }
    
    // Ensure ADMIN role is assigned
    Role adminRole = roleRepository.findByName(RoleConstants.ADMIN)
        .orElseThrow(() -> new RuntimeException("ADMIN role not found"));
    
    Set<Role> adminRoles = new HashSet<>();
    adminRoles.add(adminRole);
    admin.setRoles(adminRoles);
    
    userRepository.save(admin);
    System.out.println("Default admin user created/updated: username=admin, password=admin, roles=" + 
                      admin.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.joining(", ")));
}





private void createRoleIfNotExists(RoleRepository roleRepository, String roleName, String description) {
    Role role = roleRepository.findByName(roleName).orElse(null);
    
    if (role == null) {
        role = new Role();
        role.setName(roleName);
        role.setDescription(description);
        role.setEnabled(true);
        role.setCreatedAt(LocalDateTime.now());
        System.out.println("Created role: " + roleName);
    } else {
        System.out.println("Role already exists, updating: " + roleName);
    }
    
    // Always update the roleType and timestamp
    role.setUpdatedAt(LocalDateTime.now());
    
    // Set the roleType enum based on the role name
    try {
        RoleType roleType = RoleType.fromRoleName(roleName);
        role.setRoleType(roleType);
    } catch (Exception e) {
        System.err.println("Warning: Could not set roleType for role " + roleName + ": " + e.getMessage());
    }
    
    roleRepository.save(role);
}
        
private void initializeLocationMaster(QrmfgLocationMasterRepository locationRepository) {
    try {
        // Check if data already exists
        long count = locationRepository.count();
        if (count > 0) {
            System.out.println("Location Master data already exists. Count: " + count);
            return;
        }
        
        System.out.println("Initializing Location Master data...");
        
        // Create sample location data
        QrmfgLocationMaster[] locations = {
            new QrmfgLocationMaster("1102", "Manufacturing Unit 1102"),
            new QrmfgLocationMaster("1103", "Manufacturing Unit 1103"),
            new QrmfgLocationMaster("1104", "Manufacturing Unit 1104"),
            new QrmfgLocationMaster("1105", "Manufacturing Unit 1105"),
            new QrmfgLocationMaster("1106", "Manufacturing Unit 1106"),
            new QrmfgLocationMaster("1107", "Manufacturing Unit 1107"),
            new QrmfgLocationMaster("1108", "Manufacturing Unit 1108")
        };
        
        // Save all locations
        for (QrmfgLocationMaster location : locations) {
            locationRepository.save(location);
            System.out.println("Created location: " + location.getLocationCode() + " - " + location.getDescription());
        }
        
        System.out.println("Successfully initialized " + locations.length + " location master records");
        
    } catch (Exception e) {
        System.err.println("Failed to initialize Location Master data: " + e.getMessage());
        e.printStackTrace();
    }
}
}
