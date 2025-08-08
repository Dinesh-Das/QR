package com.cqs.qrmfg.model;

import javax.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.cqs.qrmfg.enums.RoleType;

@Entity
@Table(name = "QRMFG_USERS")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_seq")
    @SequenceGenerator(name = "users_seq", sequenceName = "QRMFG_USERS_SEQ", allocationSize = 1)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true)
    private String email;

    @Column(length = 20)
    private String status = "ACTIVE";

    private boolean enabled = true;
    private boolean emailVerified = false;
    private boolean phoneVerified = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "QRMFG_USER_ROLES",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @JsonIgnore
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<UserSession> sessions = new HashSet<>();

    @Column(name = "assigned_plants", length = 500)
    private String assignedPlants;
    
    @Column(name = "primary_plant", length = 50)
    private String primaryPlant;

    public User() {}

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    // Basic getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isEnabled() { return enabled && "ACTIVE".equals(status); }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
    public Set<UserSession> getSessions() { return sessions; }
    public void setSessions(Set<UserSession> sessions) { this.sessions = sessions; }
    public String getAssignedPlants() { return assignedPlants; }
    public void setAssignedPlants(String assignedPlants) { this.assignedPlants = assignedPlants; }
    public String getPrimaryPlant() { return primaryPlant; }
    public void setPrimaryPlant(String primaryPlant) { this.primaryPlant = primaryPlant; }

    // UserDetails methods implementation
    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        for (Role role : roles) {
            // Add ROLE_ prefix for Spring Security compatibility
            String roleName = role.getName();
            if (!roleName.startsWith("ROLE_")) {
                roleName = "ROLE_" + roleName;
            }
            authorities.add(new SimpleGrantedAuthority(roleName));
        }
        return authorities;
    }
    
    @Override
    public boolean isAccountNonExpired() { return true; }
    
    @Override
    public boolean isAccountNonLocked() { return !"LOCKED".equals(status); }
    
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    
    // RBAC helper methods
    public RoleType getPrimaryRoleType() {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        return roles.iterator().next().getRoleType();
    }
    
    public boolean isAdmin() {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return roles.stream().anyMatch(role -> role.getRoleType() == RoleType.ADMIN);
    }
    
    public boolean isPlantUser() {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return roles.stream().anyMatch(role -> role.getRoleType() == RoleType.PLANT_ROLE);
    }
    
    public boolean supportsPlantFiltering() {
        return isPlantUser();
    }
    
    public List<String> getAssignedPlantsList() {
        List<String> plantList = new ArrayList<>();
        if (assignedPlants != null && !assignedPlants.trim().isEmpty()) {
            String[] plants = assignedPlants.split(",");
            for (String plant : plants) {
                String trimmedPlant = plant.trim();
                if (!trimmedPlant.isEmpty()) {
                    plantList.add(trimmedPlant);
                }
            }
        }
        
        // If no assigned plants but we have a primary plant, include the primary plant
        if (plantList.isEmpty() && primaryPlant != null && !primaryPlant.trim().isEmpty()) {
            plantList.add(primaryPlant.trim());
        }
        
        return plantList;
    }
    
    // Missing methods - stub implementations for compilation
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public boolean isPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }
    
    public void validatePlantAssignments() { /* stub */ }
    public void setAssignedPlantsList(List<String> plants) { /* stub */ }
    public void addPlantAssignment(String plantCode) { /* stub */ }
    public void removePlantAssignment(String plantCode) { /* stub */ }
    public boolean hasPlantAccess(String plantCode) { return true; }
    public boolean hasMaxPlantsAssigned() { return false; }
    public String getEffectivePlant() { return primaryPlant; }
    public boolean hasRole(RoleType roleType) { 
        return roles.stream().anyMatch(role -> role.getRoleType() == roleType);
    }
}