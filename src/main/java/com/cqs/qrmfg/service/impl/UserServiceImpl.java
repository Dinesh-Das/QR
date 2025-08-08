package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.dto.UserRoleAssignmentDto;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.model.Role;
import com.cqs.qrmfg.repository.UserRepository;
import com.cqs.qrmfg.repository.RoleRepository;
import com.cqs.qrmfg.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public List<UserRoleAssignmentDto> getAllUserRoleAssignmentDtos() {
        // Get all users using JPA
        List<User> users = userRepository.findAll();
        List<UserRoleAssignmentDto> assignments = new ArrayList<>();
        
        // Get all available roles
        List<Role> allRoles = roleRepository.findAll();
        List<UserRoleAssignmentDto.RoleDto> allRoleDtos = allRoles.stream()
                .map(this::convertToRoleDto)
                .collect(Collectors.toList());
        
        // Convert users to DTOs
        for (User user : users) {
            UserRoleAssignmentDto dto = new UserRoleAssignmentDto();
            dto.setUserId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setEmail(user.getEmail());
            
            // Convert assigned roles
            List<UserRoleAssignmentDto.RoleDto> assignedRoles = user.getRoles().stream()
                    .map(this::convertToRoleDto)
                    .collect(Collectors.toList());
            dto.setAssignedRoles(assignedRoles);
            
            // Set available roles (all roles not currently assigned)
            List<Long> assignedRoleIds = assignedRoles.stream()
                    .map(UserRoleAssignmentDto.RoleDto::getId)
                    .collect(Collectors.toList());
            
            List<UserRoleAssignmentDto.RoleDto> availableRoles = allRoleDtos.stream()
                    .filter(role -> !assignedRoleIds.contains(role.getId()))
                    .collect(Collectors.toList());
            
            dto.setAvailableRoles(availableRoles);
            assignments.add(dto);
        }
        
        return assignments;
    }
    
    private UserRoleAssignmentDto.RoleDto convertToRoleDto(Role role) {
        UserRoleAssignmentDto.RoleDto dto = new UserRoleAssignmentDto.RoleDto();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        return dto;
    }

    public UserRoleAssignmentDto updateUserRolesAndReturn(Long userId, List<Long> roleIds) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        
        User user = userOpt.get();
        
        // Clear existing roles
        user.getRoles().clear();
        
        // Add new roles
        if (roleIds != null && !roleIds.isEmpty()) {
            Set<Role> newRoles = new HashSet<>();
            for (Long roleId : roleIds) {
                Optional<Role> roleOpt = roleRepository.findById(roleId);
                if (roleOpt.isPresent()) {
                    newRoles.add(roleOpt.get());
                }
            }
            user.setRoles(newRoles);
        }
        
        // Save the user
        userRepository.save(user);
        
        // Return updated user
        return getUserById(userId);
    }


    public UserRoleAssignmentDto getUserById(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            return null;
        }
        
        User user = userOpt.get();
        UserRoleAssignmentDto dto = new UserRoleAssignmentDto();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        
        // Convert assigned roles
        List<UserRoleAssignmentDto.RoleDto> assignedRoles = user.getRoles().stream()
                .map(this::convertToRoleDto)
                .collect(Collectors.toList());
        dto.setAssignedRoles(assignedRoles);
        
        // Get all available roles
        List<Role> allRoles = roleRepository.findAll();
        List<Long> assignedRoleIds = assignedRoles.stream()
                .map(UserRoleAssignmentDto.RoleDto::getId)
                .collect(Collectors.toList());
        
        List<UserRoleAssignmentDto.RoleDto> availableRoles = allRoles.stream()
                .filter(role -> !assignedRoleIds.contains(role.getId()))
                .map(this::convertToRoleDto)
                .collect(Collectors.toList());
        
        dto.setAvailableRoles(availableRoles);
        return dto;
    }

    public UserRoleAssignmentDto getUserByUsername(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (!userOpt.isPresent()) {
            return null;
        }
        
        return getUserById(userOpt.get().getId());
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findByIdOptional(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public User save(User user) {
        // Set default values for required fields if not already set
        if (user.getStatus() == null) {
            user.setStatus("ACTIVE");
        }
        
        // Encode password if it's not already encoded
        if (user.getPassword() != null && 
            !user.getPassword().startsWith("$2a$") && 
            !user.getPassword().startsWith("$2b$") && 
            !user.getPassword().startsWith("$2y$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        // Set timestamps if this is a new user
        if (user.getId() == null) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
        } else {
            user.setUpdatedAt(java.time.LocalDateTime.now());
        }
        
        return userRepository.save(user);
    }

    public User update(User user) {
        // Check if user exists
        Optional<User> existingUserOpt = userRepository.findById(user.getId());
        if (!existingUserOpt.isPresent()) {
            throw new RuntimeException("User not found with id: " + user.getId());
        }
        
        User existingUser = existingUserOpt.get();
        
        // If password is null or empty, don't update it - keep the existing password
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            System.out.println("Updating user " + user.getId() + " without changing password");
            user.setPassword(existingUser.getPassword());
        } else {
            System.out.println("Updating user " + user.getId() + " including password change");
        }
        
        // Set updated timestamp
        user.setUpdatedAt(java.time.LocalDateTime.now());
        
        // Set default status if not provided
        if (user.getStatus() == null) {
            user.setStatus("ACTIVE");
        }
        
        return userRepository.save(user);
    }

    
    @Override
    public void deleteById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        
        // JPA will handle the cascade deletion of user roles through the relationship mapping
        userRepository.deleteById(id);
    }
    
    @Override
    public void delete(User user) {
        if (user != null && user.getId() != null) {
            deleteById(user.getId());
        }
    }

    @Override
    public Map<String, Object> getNotificationPreferences(String username) {
        return new HashMap<>(); // TODO: implement actual logic
    }

    @Override
    public void updateNotificationPreferences(String username, Map<String, Object> preferences) {
        // TODO: implement actual logic
    }
    
    @Override
    public List<String> getUserAssignedPlants(String username) {
        try {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (!userOpt.isPresent()) {
                return new ArrayList<>();
            }
            
            String assignedPlantsJson = userOpt.get().getAssignedPlants();
            if (assignedPlantsJson == null || assignedPlantsJson.trim().isEmpty()) {
                return new ArrayList<>();
            }
            
            // Simple JSON parsing for plant codes array
            String cleanJson = assignedPlantsJson.replace("[", "").replace("]", "").replace("\"", "");
            if (cleanJson.trim().isEmpty()) {
                return new ArrayList<>();
            }
            
            return java.util.Arrays.asList(cleanJson.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        } catch (Exception e) {
            // If column doesn't exist or other error, return empty list
            System.out.println("Warning: Could not load assigned plants for user " + username + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    @Override
    public String getUserPrimaryPlant(String username) {
        try {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (!userOpt.isPresent()) {
                return null;
            }
            
            String primaryPlant = userOpt.get().getPrimaryPlant();
            if (primaryPlant != null && !primaryPlant.trim().isEmpty()) {
                return primaryPlant;
            }
            
            // Fallback to first assigned plant
            List<String> assignedPlants = getUserAssignedPlants(username);
            return assignedPlants.isEmpty() ? null : assignedPlants.get(0);
        } catch (Exception e) {
            // If column doesn't exist or other error, return null
            System.out.println("Warning: Could not load primary plant for user " + username + ": " + e.getMessage());
            return null;
        }
    }
    
    @Override
    public void updateUserPlantAssignments(String username, List<String> plantCodes, String primaryPlant) {
        try {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (!userOpt.isPresent()) {
                throw new RuntimeException("User not found with username: " + username);
            }
            
            User user = userOpt.get();
            
            String assignedPlantsJson = null;
            if (plantCodes != null && !plantCodes.isEmpty()) {
                assignedPlantsJson = "[\"" + String.join("\",\"", plantCodes) + "\"]";
            }
            
            user.setAssignedPlants(assignedPlantsJson);
            user.setPrimaryPlant(primaryPlant);
            user.setUpdatedAt(java.time.LocalDateTime.now());
            
            userRepository.save(user);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update plant assignments for user " + username + ": " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isUserAssignedToPlant(String username, String plantCode) {
        List<String> assignedPlants = getUserAssignedPlants(username);
        return assignedPlants.contains(plantCode);
    }
    
    // ========== MISSING INTERFACE METHODS ==========
    
    @Override
    public User findByUsername(String username) {
        // Use JPA repository to properly load roles and other relationships
        Optional<User> userOpt = userRepository.findByUsername(username);
        return userOpt.orElse(null);
    }
    
    @Override
    public User findByEmail(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        return userOpt.orElse(null);
    }
    
    @Override
    public User findById(Long id) {
        Optional<User> userOpt = findByIdOptional(id);
        return userOpt.orElse(null);
    }
    

    
    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    @Override
    public User createUser(String username, String email, String password) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setEnabled(true);
        user.setStatus("ACTIVE");
        return save(user);
    }
    
    @Override
    public void updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        update(user);
    }
    
    @Override
    public List<User> findByRole(String roleName) {
        return userRepository.findByRoleName(roleName);
    }
    
    @Override
    public List<User> findByPlantCode(String plantCode) {
        return userRepository.findByPlantCode(plantCode);
    }
    
    @Override
    public List<Object> getAllUserRoleAssignments() {
        List<UserRoleAssignmentDto> assignments = getAllUserRoleAssignmentDtos();
        return new ArrayList<>(assignments);
    }
    
    @Override
    @Transactional
    public void updateUserRoles(Long userId, List<Long> roleIds) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        
        User user = userOpt.get();
        
        // Clear existing roles
        user.getRoles().clear();
        
        // Add new roles
        if (roleIds != null && !roleIds.isEmpty()) {
            Set<Role> newRoles = new HashSet<>();
            for (Long roleId : roleIds) {
                Optional<Role> roleOpt = roleRepository.findById(roleId);
                if (roleOpt.isPresent()) {
                    Role role = roleOpt.get();
                    newRoles.add(role);
                    System.out.println("Adding role to user " + userId + ": " + role.getName());
                } else {
                    System.out.println("Role not found with id: " + roleId);
                }
            }
            user.setRoles(newRoles);
            System.out.println("Total roles assigned to user " + userId + ": " + newRoles.size());
        }
        
        // Save the user
        User savedUser = userRepository.save(user);
        System.out.println("User saved with " + savedUser.getRoles().size() + " roles");
    }
    
    @Override
    public String encodePassword(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }

}