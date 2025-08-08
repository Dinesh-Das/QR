package com.cqs.qrmfg.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import com.cqs.qrmfg.enums.RoleType;

/**
 * Role entity for RBAC system
 */
@Entity
@Table(name = "QRMFG_ROLES")
public class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "roles_seq")
    @SequenceGenerator(name = "roles_seq", sequenceName = "QRMFG_ROLES_SEQ", allocationSize = 1)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String name;
    
    @Column(length = 200)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", length = 50)
    private RoleType roleType;
    
    @Column(nullable = false)
    private boolean enabled = true;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(length = 50)
    private String createdBy;
    
    @Column(length = 50)
    private String updatedBy;
    
    @ManyToMany(mappedBy = "roles")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Set<User> users = new HashSet<>();
    
    public Role() {}
    
    public Role(String name, String description) {
        this.name = name;
        this.description = description;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Role(String name, String description, RoleType roleType) {
        this.name = name;
        this.description = description;
        this.roleType = roleType;
        this.enabled = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public RoleType getRoleType() {
        return roleType;
    }
    
    public void setRoleType(RoleType roleType) {
        this.roleType = roleType;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
    
    public Set<User> getUsers() {
        return users;
    }
    
    public void setUsers(Set<User> users) {
        this.users = users;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role)) return false;
        Role role = (Role) o;
        return name != null && name.equals(role.name);
    }
    
    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
    
    // ========== RBAC UTILITY METHODS ==========
    
    /**
     * Sync role name with role type
     */
    public void syncRoleNameWithType() {
        if (roleType != null) {
            this.name = roleType.getRoleName();
        }
    }
    
    /**
     * Check if role name is valid for the role type
     */
    public boolean isRoleNameValid() {
        if (roleType == null) {
            return true; // No validation needed if no role type
        }
        return roleType.getRoleName().equals(name);
    }
    
    /**
     * Check if this role has higher or equal privilege than another role
     */
    public boolean hasHigherOrEqualPrivilege(Role other) {
        if (other == null || this.roleType == null || other.roleType == null) {
            return false;
        }
        return this.roleType.getHierarchyLevel() >= other.roleType.getHierarchyLevel();
    }
    
    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", roleType=" + roleType +
                ", enabled=" + enabled +
                '}';
    }
}