package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.annotation.RequireRole;
import com.cqs.qrmfg.enums.RoleType;
import com.cqs.qrmfg.model.Role;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.model.QrmfgLocationMaster;
import com.cqs.qrmfg.service.RoleService;
import com.cqs.qrmfg.service.UserService;
import com.cqs.qrmfg.repository.QrmfgLocationMasterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin")
@RequireRole(RoleType.ADMIN)
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private RoleService roleService;
    
    @Autowired
    private QrmfgLocationMasterRepository locationRepository;
    
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        logger.info("AdminController: Getting all users");
        try {
            // Test 1: Try to call userService.findAll()
            logger.info("AdminController: Calling userService.findAll()");
            List<User> users = userService.findAll();
            logger.info("AdminController: Successfully retrieved {} users from service", users.size());
            
            // Test 2: Create simplified user data
            List<Map<String, Object>> simplifiedUsers = new ArrayList<>();
            for (User user : users) {
                try {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("username", user.getUsername());
                    userMap.put("email", user.getEmail());
                    userMap.put("enabled", user.isEnabled());
                    userMap.put("status", user.getStatus());
                    userMap.put("createdAt", user.getCreatedAt());
                    
                    // Add role information manually to avoid circular reference
                    List<Map<String, Object>> userRoles = new ArrayList<>();
                    if (user.getRoles() != null) {
                        for (Role role : user.getRoles()) {
                            Map<String, Object> roleMap = new HashMap<>();
                            roleMap.put("id", role.getId());
                            roleMap.put("name", role.getName());
                            roleMap.put("description", role.getDescription());
                            roleMap.put("roleType", role.getRoleType());
                            roleMap.put("enabled", role.isEnabled());
                            userRoles.add(roleMap);
                        }
                    }
                    userMap.put("roles", userRoles);
                    
                    // Add plant assignment information
                    if (user.getAssignedPlants() != null && !user.getAssignedPlants().trim().isEmpty()) {
                        // Parse assigned plants from comma-separated string
                        List<String> plantAssignments = new ArrayList<>();
                        String[] plants = user.getAssignedPlants().split(",");
                        for (String plant : plants) {
                            String trimmedPlant = plant.trim();
                            if (!trimmedPlant.isEmpty()) {
                                plantAssignments.add(trimmedPlant);
                            }
                        }
                        userMap.put("assignedPlants", plantAssignments);
                        userMap.put("primaryPlant", user.getPrimaryPlant());
                    } else {
                        userMap.put("assignedPlants", new ArrayList<>());
                        userMap.put("primaryPlant", null);
                    }
                    
                    simplifiedUsers.add(userMap);
                    logger.debug("AdminController: Processed user {}", user.getUsername());
                } catch (Exception userEx) {
                    logger.error("AdminController: Error processing user {}: {}", user.getId(), userEx.getMessage());
                    // Skip this user and continue
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("users", simplifiedUsers);
            response.put("total", simplifiedUsers.size());
            response.put("status", "success");
            
            logger.info("AdminController: Successfully created response with {} users", simplifiedUsers.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("AdminController: Error in getAllUsers - Exception type: {}, Message: {}, Stack trace:", 
                e.getClass().getSimpleName(), e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to retrieve users: " + e.getMessage());
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            errorResponse.put("stackTrace", e.getStackTrace()[0].toString());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/roles")
    public ResponseEntity<Map<String, Object>> getAllRoles() {
        logger.info("AdminController: Getting all roles");
        try {
            List<Role> roles = roleService.findAll();
            Map<String, Object> response = new HashMap<>();
            response.put("roles", roles);
            response.put("total", roles.size());
            response.put("status", "success");
            
            logger.info("AdminController: Retrieved {} roles", roles.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("AdminController: Error retrieving roles", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to retrieve roles");
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, Object> userRequest) {
        logger.info("AdminController: Creating new user");
        try {
            // Extract user details from request
            String username = (String) userRequest.get("username");
            String email = (String) userRequest.get("email");
            String password = (String) userRequest.get("password");
            
            if (username == null || email == null || password == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "Username, email, and password are required");
                errorResponse.put("status", "error");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Check if user already exists
            if (userService.existsByUsername(username)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "Username already exists");
                errorResponse.put("status", "error");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            if (userService.existsByEmail(email)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "Email already exists");
                errorResponse.put("status", "error");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Create new user object
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(password); // Note: This will be encoded by the service
            user.setEnabled(true);
            user.setStatus("ACTIVE");
            
            User savedUser = userService.save(user);
            logger.info("AdminController: Created user with ID: {}", savedUser.getId());
            
            // Handle role assignments if provided
            @SuppressWarnings("unchecked")
            List<Object> roleIdsRaw = (List<Object>) userRequest.get("roles");
            if (roleIdsRaw != null && !roleIdsRaw.isEmpty()) {
                try {
                    // Convert to Long, handling both Integer and Long types
                    List<Long> roleIds = new ArrayList<>();
                    for (Object roleIdObj : roleIdsRaw) {
                        if (roleIdObj instanceof Integer) {
                            roleIds.add(((Integer) roleIdObj).longValue());
                        } else if (roleIdObj instanceof Long) {
                            roleIds.add((Long) roleIdObj);
                        } else if (roleIdObj instanceof String) {
                            roleIds.add(Long.parseLong((String) roleIdObj));
                        }
                    }
                    
                    // Assign roles immediately after user creation
                    logger.info("AdminController: Attempting to assign roles {} to user {}", roleIds, savedUser.getId());
                    userService.updateUserRoles(savedUser.getId(), roleIds);
                    
                    // Refresh the user to get updated role information
                    savedUser = userService.findById(savedUser.getId());
                    
                    if (savedUser != null && savedUser.getRoles() != null) {
                        logger.info("AdminController: Successfully assigned {} roles to user {}", savedUser.getRoles().size(), savedUser.getId());
                    } else {
                        logger.warn("AdminController: Role assignment may have failed - user has no roles after assignment");
                    }
                } catch (Exception roleError) {
                    logger.error("AdminController: Error assigning roles to new user {}", savedUser.getId(), roleError);
                    // Don't fail the entire operation, but return error message
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("message", "User created but role assignment failed: " + roleError.getMessage());
                    errorResponse.put("status", "warning");
                    errorResponse.put("userId", savedUser.getId());
                    return ResponseEntity.ok(errorResponse);
                }
            }
            
            // Handle plant assignments if provided
            @SuppressWarnings("unchecked")
            List<String> assignedPlants = (List<String>) userRequest.get("assignedPlants");
            String primaryPlant = (String) userRequest.get("primaryPlant");
            
            if (assignedPlants != null && !assignedPlants.isEmpty()) {
                try {
                    // Validate that all plant codes exist
                    for (String plantCode : assignedPlants) {
                        if (!locationRepository.existsByLocationCode(plantCode)) {
                            logger.warn("AdminController: Invalid plant code during user creation: {}", plantCode);
                        }
                    }
                    
                    // Update user's plant assignments
                    savedUser.setAssignedPlants(String.join(",", assignedPlants));
                    if (primaryPlant != null && !primaryPlant.trim().isEmpty()) {
                        if (locationRepository.existsByLocationCode(primaryPlant)) {
                            savedUser.setPrimaryPlant(primaryPlant);
                        } else if (!assignedPlants.isEmpty()) {
                            savedUser.setPrimaryPlant(assignedPlants.get(0)); // Use first assigned plant as primary
                        }
                    } else if (!assignedPlants.isEmpty()) {
                        savedUser.setPrimaryPlant(assignedPlants.get(0)); // Use first assigned plant as primary
                    }
                    
                    // Save the user with plant assignments
                    savedUser = userService.save(savedUser);
                    logger.info("AdminController: Assigned plants to user {}: {}", savedUser.getId(), assignedPlants);
                } catch (Exception plantError) {
                    logger.error("AdminController: Error assigning plants to new user {}", savedUser.getId(), plantError);
                    // Don't fail the entire operation, but log the error
                    logger.warn("AdminController: Plant assignment failed for user {}, continuing without plants", savedUser.getId());
                }
            }
            
            // Prepare response with complete user data
            Map<String, Object> response = new HashMap<>();
            
            // Create user response with role and plant information
            Map<String, Object> userResponse = new HashMap<>();
            userResponse.put("id", savedUser.getId());
            userResponse.put("username", savedUser.getUsername());
            userResponse.put("email", savedUser.getEmail());
            userResponse.put("enabled", savedUser.isEnabled());
            userResponse.put("status", savedUser.getStatus());
            userResponse.put("createdAt", savedUser.getCreatedAt());
            
            // Add role information
            List<Map<String, Object>> userRoles = new ArrayList<>();
            if (savedUser.getRoles() != null) {
                for (Role role : savedUser.getRoles()) {
                    Map<String, Object> roleMap = new HashMap<>();
                    roleMap.put("id", role.getId());
                    roleMap.put("name", role.getName());
                    roleMap.put("description", role.getDescription());
                    roleMap.put("roleType", role.getRoleType());
                    roleMap.put("enabled", role.isEnabled());
                    userRoles.add(roleMap);
                }
            }
            userResponse.put("roles", userRoles);
            
            // Add plant information
            if (savedUser.getAssignedPlants() != null && !savedUser.getAssignedPlants().trim().isEmpty()) {
                // Parse assigned plants from comma-separated string to array for frontend
                List<String> plantAssignments = new ArrayList<>();
                String[] plants = savedUser.getAssignedPlants().split(",");
                for (String plant : plants) {
                    String trimmedPlant = plant.trim();
                    if (!trimmedPlant.isEmpty()) {
                        plantAssignments.add(trimmedPlant);
                    }
                }
                userResponse.put("assignedPlants", plantAssignments);
                userResponse.put("primaryPlant", savedUser.getPrimaryPlant());
            } else {
                userResponse.put("assignedPlants", new ArrayList<>());
                userResponse.put("primaryPlant", null);
            }
            
            response.put("user", userResponse);
            response.put("status", "success");
            response.put("message", "User created successfully");
            
            logger.info("AdminController: Successfully created user with roles and plants: {}", savedUser.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("AdminController: Error creating user", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to create user: " + e.getMessage());
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PutMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Long userId, 
            @RequestBody Map<String, Object> userRequest) {
        logger.info("AdminController: Updating user with ID: {}", userId);
        try {
            // Check if user exists
            User existingUser = userService.findById(userId);
            if (existingUser == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "User not found");
                errorResponse.put("status", "error");
                return ResponseEntity.notFound().build();
            }
            
            // Extract user details from request
            String email = (String) userRequest.get("email");
            Boolean enabled = (Boolean) userRequest.get("enabled");
            String status = (String) userRequest.get("status");
            
            // Update user fields (username cannot be changed)
            if (email != null) {
                existingUser.setEmail(email);
            }
            if (enabled != null) {
                existingUser.setEnabled(enabled);
            }
            if (status != null) {
                existingUser.setStatus(status);
            }
            
            User updatedUser = userService.save(existingUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("user", updatedUser);
            response.put("status", "success");
            response.put("message", "User updated successfully");
            
            logger.info("AdminController: Updated user with ID: {}", userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("AdminController: Error updating user with ID: {}", userId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to update user: " + e.getMessage());
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long userId) {
        logger.info("AdminController: Deleting user with ID: {}", userId);
        try {
            // Check if user exists
            User existingUser = userService.findById(userId);
            if (existingUser == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "User not found");
                errorResponse.put("status", "error");
                return ResponseEntity.notFound().build();
            }
            
            userService.deleteById(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "User deleted successfully");
            response.put("deletedUserId", userId);
            
            logger.info("AdminController: Deleted user with ID: {}", userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("AdminController: Error deleting user with ID: {}", userId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to delete user: " + e.getMessage());
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/users/{username}/plants")
    public ResponseEntity<List<String>> getUserPlantAssignments(@PathVariable String username) {
        logger.info("AdminController: Getting plant assignments for user: {}", username);
        try {
            User user = userService.findByUsername(username);
            if (user == null) {
                logger.warn("AdminController: User not found: {}", username);
                return ResponseEntity.notFound().build();
            }
            
            // Parse assigned plants from the user's assignedPlants field
            List<String> plantAssignments = new ArrayList<>();
            if (user.getAssignedPlants() != null && !user.getAssignedPlants().trim().isEmpty()) {
                String[] plants = user.getAssignedPlants().split(",");
                for (String plant : plants) {
                    plantAssignments.add(plant.trim());
                }
            }
            
            logger.info("AdminController: Retrieved {} plant assignments for user {}", plantAssignments.size(), username);
            return ResponseEntity.ok(plantAssignments);
        } catch (Exception e) {
            logger.error("AdminController: Error getting plant assignments for user: {}", username, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PutMapping("/users/{username}/plants")
    public ResponseEntity<Map<String, Object>> updateUserPlantAssignments(
            @PathVariable String username, 
            @RequestBody Map<String, Object> plantData) {
        logger.info("AdminController: Updating plant assignments for user: {}", username);
        try {
            User user = userService.findByUsername(username);
            if (user == null) {
                logger.warn("AdminController: User not found: {}", username);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "User not found");
                errorResponse.put("status", "error");
return ResponseEntity.notFound().build();
            }
            
            @SuppressWarnings("unchecked")
            List<String> assignedPlants = (List<String>) plantData.get("assignedPlants");
            String primaryPlant = (String) plantData.get("primaryPlant");
            
            // Validate that all plant codes exist
            if (assignedPlants != null) {
                for (String plantCode : assignedPlants) {
                    if (!locationRepository.existsByLocationCode(plantCode)) {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("message", "Invalid plant code: " + plantCode);
                        errorResponse.put("status", "error");
                        return ResponseEntity.badRequest().body(errorResponse);
                    }
                }
                
                // Update user's plant assignments
                user.setAssignedPlants(String.join(",", assignedPlants));
            }
            
            if (primaryPlant != null && !primaryPlant.trim().isEmpty()) {
                if (!locationRepository.existsByLocationCode(primaryPlant)) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("message", "Invalid primary plant code: " + primaryPlant);
                    errorResponse.put("status", "error");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                user.setPrimaryPlant(primaryPlant);
            }
            
            userService.save(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Plant assignments updated successfully");
            response.put("status", "success");
            response.put("assignedPlants", assignedPlants);
            response.put("primaryPlant", primaryPlant);
            
            logger.info("AdminController: Updated plant assignments for user: {}", username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("AdminController: Error updating plant assignments for user: {}", username, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to update plant assignments");
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/plants")
    public ResponseEntity<Map<String, Object>> getAllPlants() {
        logger.info("AdminController: Getting all available plants");
        try {
            List<QrmfgLocationMaster> locations = locationRepository.findAllOrderByLocationCode();
            
            List<Map<String, Object>> plants = new ArrayList<>();
            for (QrmfgLocationMaster location : locations) {
                Map<String, Object> plant = new HashMap<>();
                plant.put("code", location.getLocationCode());
                plant.put("name", location.getDescription());
                plants.add(plant);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("plants", plants);
            response.put("total", plants.size());
            response.put("status", "success");
            
            logger.info("AdminController: Retrieved {} plants", plants.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("AdminController: Error retrieving plants", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to retrieve plants");
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<Map<String, Object>> assignRolesToUser(
            @PathVariable Long userId, 
            @RequestBody Map<String, Object> request) {
        logger.info("AdminController: Assigning roles to user {}", userId);
        try {
            @SuppressWarnings("unchecked")
            List<Object> roleIdsRaw = (List<Object>) request.get("roleIds");
            
            if (roleIdsRaw == null || roleIdsRaw.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Role IDs are required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Convert to Long, handling both Integer and Long types
            List<Long> roleIds = new ArrayList<>();
            for (Object roleIdObj : roleIdsRaw) {
                if (roleIdObj instanceof Integer) {
                    roleIds.add(((Integer) roleIdObj).longValue());
                } else if (roleIdObj instanceof Long) {
                    roleIds.add((Long) roleIdObj);
                } else if (roleIdObj instanceof String) {
                    try {
                        roleIds.add(Long.parseLong((String) roleIdObj));
                    } catch (NumberFormatException e) {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("status", "error");
                        errorResponse.put("message", "Invalid role ID format: " + roleIdObj);
                        return ResponseEntity.badRequest().body(errorResponse);
                    }
                } else {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Invalid role ID type: " + roleIdObj.getClass().getSimpleName());
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }
            
            // Check if user exists
            User user = userService.findById(userId);
            if (user == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "User not found");
                return ResponseEntity.notFound().build();
            }
            
            // Use the existing UserService method to update roles
            userService.updateUserRoles(userId, roleIds);
            
            // Get updated user to return role information
            User updatedUser = userService.findById(userId);
            List<Map<String, Object>> assignedRoles = new ArrayList<>();
            if (updatedUser != null && updatedUser.getRoles() != null) {
                for (Role role : updatedUser.getRoles()) {
                    Map<String, Object> roleMap = new HashMap<>();
                    roleMap.put("id", role.getId());
                    roleMap.put("name", role.getName());
                    roleMap.put("description", role.getDescription());
                    assignedRoles.add(roleMap);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Roles assigned successfully");
            response.put("userId", userId);
            response.put("assignedRoles", assignedRoles);
            
            logger.info("AdminController: Successfully assigned {} roles to user {}", assignedRoles.size(), userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("AdminController: Error assigning roles to user {}", userId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to assign roles: " + e.getMessage());
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PostMapping("/fix-admin-roles")
    public ResponseEntity<Map<String, Object>> fixAdminRoles() {
        logger.info("AdminController: Fixing admin user roles");
        try {
            // Find admin user
            User adminUser = userService.findByUsername("admin");
            if (adminUser == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "Admin user not found");
                errorResponse.put("status", "error");
                return ResponseEntity.notFound().build();
            }
            
            // Find ADMIN role
            Role adminRole = roleService.findByName("ADMIN").orElse(null);
            if (adminRole == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "ADMIN role not found");
                errorResponse.put("status", "error");
                return ResponseEntity.notFound().build();
            }
            
            // Assign ADMIN role to admin user
            List<Long> roleIds = Arrays.asList(adminRole.getId());
            userService.updateUserRoles(adminUser.getId(), roleIds);
            
            // Verify the assignment
            User updatedUser = userService.findById(adminUser.getId());
            List<String> assignedRoles = new ArrayList<>();
            if (updatedUser.getRoles() != null) {
                for (Role role : updatedUser.getRoles()) {
                    assignedRoles.add(role.getName());
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Admin roles fixed successfully");
            response.put("status", "success");
            response.put("userId", adminUser.getId());
            response.put("username", adminUser.getUsername());
            response.put("assignedRoles", assignedRoles);
            
            logger.info("AdminController: Successfully fixed admin user roles: {}", assignedRoles);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("AdminController: Error fixing admin roles", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to fix admin roles: " + e.getMessage());
            errorResponse.put("status", "error");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/test/user-roles")
    public ResponseEntity<Map<String, Object>> testUserRoles() {
        logger.info("AdminController: Testing user roles data");
        try {
            List<User> users = userService.findAll();
            Map<String, Object> testData = new HashMap<>();
            
            for (User user : users) {
                Map<String, Object> userData = new HashMap<>();
                userData.put("username", user.getUsername());
                userData.put("rolesCount", user.getRoles() != null ? user.getRoles().size() : 0);
                userData.put("rolesRaw", user.getRoles());
                
                List<String> roleNames = new ArrayList<>();
                if (user.getRoles() != null) {
                    for (Role role : user.getRoles()) {
                        roleNames.add(role.getName());
                    }
                }
                userData.put("roleNames", roleNames);
                
                testData.put(user.getUsername(), userData);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("testData", testData);
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("AdminController: Error in testUserRoles", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Test failed: " + e.getMessage());
            errorResponse.put("status", "error");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/users/{userId}/reset-password")
    public ResponseEntity<Map<String, Object>> resetUserPassword(
            @PathVariable Long userId, 
            @RequestBody Map<String, Object> request) {
        logger.info("AdminController: Resetting password for user {}", userId);
        try {
            String newPassword = (String) request.get("password");
            
            if (newPassword == null || newPassword.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "New password is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Get the user
            User user = userService.findById(userId);
            if (user == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "User not found");
                return ResponseEntity.notFound().build();
            }
            
            // Update the password using the service method (which will encode it)
            userService.updatePassword(user, newPassword);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Password reset successfully");
            response.put("userId", userId);
            response.put("username", user.getUsername());
            
            logger.info("AdminController: Successfully reset password for user {}", user.getUsername());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("AdminController: Error resetting password for user {}", userId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to reset password: " + e.getMessage());
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

}