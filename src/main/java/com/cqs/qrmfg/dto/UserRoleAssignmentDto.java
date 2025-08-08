package com.cqs.qrmfg.dto;

import java.util.List;

/**
 * DTO for user role assignments
 */
public class UserRoleAssignmentDto {
    private Long userId;
    private String username;
    private String email;
    private List<RoleDto> assignedRoles;
    private List<RoleDto> availableRoles;

    public UserRoleAssignmentDto() {
    }

    public UserRoleAssignmentDto(Long userId, String username, String email, List<RoleDto> assignedRoles, List<RoleDto> availableRoles) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.assignedRoles = assignedRoles;
        this.availableRoles = availableRoles;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<RoleDto> getAssignedRoles() {
        return assignedRoles;
    }

    public void setAssignedRoles(List<RoleDto> assignedRoles) {
        this.assignedRoles = assignedRoles;
    }

    public List<RoleDto> getAvailableRoles() {
        return availableRoles;
    }

    public void setAvailableRoles(List<RoleDto> availableRoles) {
        this.availableRoles = availableRoles;
    }

    /**
     * DTO for role information
     */
    public static class RoleDto {
        private Long id;
        private String name;
        private String description;

        public RoleDto() {
        }

        public RoleDto(Long id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
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
    }
}