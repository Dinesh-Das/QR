package com.cqs.qrmfg.dto;

import com.cqs.qrmfg.enums.RoleType;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * DTO for role assignment requests
 */
public class RoleAssignmentRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Role type is required")
    private RoleType roleType;
    
    private List<String> plantCodes;
    
    public RoleAssignmentRequest() {
    }
    
    public RoleAssignmentRequest(Long userId, RoleType roleType) {
        this.userId = userId;
        this.roleType = roleType;
    }
    
    public RoleAssignmentRequest(Long userId, RoleType roleType, List<String> plantCodes) {
        this.userId = userId;
        this.roleType = roleType;
        this.plantCodes = plantCodes;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public RoleType getRoleType() {
        return roleType;
    }
    
    public void setRoleType(RoleType roleType) {
        this.roleType = roleType;
    }
    
    public List<String> getPlantCodes() {
        return plantCodes;
    }
    
    public void setPlantCodes(List<String> plantCodes) {
        this.plantCodes = plantCodes;
    }
}