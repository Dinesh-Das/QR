package com.cqs.qrmfg.dto;

import com.cqs.qrmfg.enums.RoleType;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for role response data
 */
public class RoleResponse {
    
    private Long id;
    private String name;
    private String description;
    private RoleType roleType;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> permissions;
    private int userCount;
    
    public RoleResponse() {
    }
    
    public RoleResponse(Long id, String name, String description, RoleType roleType, boolean enabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.roleType = roleType;
        this.enabled = enabled;
    }
    
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
    
    public List<String> getPermissions() {
        return permissions;
    }
    
    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
    
    public int getUserCount() {
        return userCount;
    }
    
    public void setUserCount(int userCount) {
        this.userCount = userCount;
    }
}