package com.cqs.qrmfg.dto;

import com.cqs.qrmfg.enums.RoleType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * DTO for role creation requests
 */
public class RoleCreateRequest {
    
    @NotNull(message = "Role type is required")
    private RoleType roleType;
    
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
    
    public RoleCreateRequest() {
    }
    
    public RoleCreateRequest(RoleType roleType, String description) {
        this.roleType = roleType;
        this.description = description;
    }
    
    public RoleType getRoleType() {
        return roleType;
    }
    
    public void setRoleType(RoleType roleType) {
        this.roleType = roleType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}