package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.annotation.RequireRole;
import com.cqs.qrmfg.annotation.PlantDataFilter;
import com.cqs.qrmfg.enums.RoleType;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.model.Role;
import com.cqs.qrmfg.model.AuditLog;
import com.cqs.qrmfg.service.UserService;
import com.cqs.qrmfg.service.RoleService;
import com.cqs.qrmfg.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    @Autowired private UserService userService;
    @Autowired private RoleService roleService;
    @Autowired private AuditLogService auditLogService;

    @GetMapping("/users")
    @RequireRole({RoleType.ADMIN, RoleType.TECH_ROLE})
    public List<User> getUserReport() {
        return userService.findAll();
    }

    @GetMapping("/roles")
    @RequireRole({RoleType.ADMIN, RoleType.TECH_ROLE})
    public List<Role> getRoleReport() {
        return roleService.findAll();
    }

    @GetMapping("/activity")
    @RequireRole({RoleType.ADMIN, RoleType.TECH_ROLE})
    public List<AuditLog> getActivityReport() {
        return auditLogService.findAll();
    }

    // Analytics endpoints
    @GetMapping("/analytics/user-stats")
    @RequireRole({RoleType.ADMIN, RoleType.TECH_ROLE})
    public Map<String, Object> getUserStats() {
        List<User> users = userService.findAll();
        long active = users.stream().filter(User::isEnabled).count();
        long total = users.size();
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("active", active);
        return result;
    }

    @GetMapping("/analytics/role-distribution")
    public Map<String, Long> getRoleDistribution() {
        List<User> users = userService.findAll();
        Map<String, Long> dist = new HashMap<>();
        for (User u : users) {
            for (Role r : u.getRoles()) {
                dist.put(r.getName(), dist.getOrDefault(r.getName(), 0L) + 1);
            }
        }
        return dist;
    }

    @GetMapping("/analytics/activity-timeline")
    public Map<String, Long> getActivityTimeline() {
        List<AuditLog> logs = auditLogService.findAll();
        return logs.stream().collect(Collectors.groupingBy(
            l -> l.getEventTime().toLocalDate().toString(), Collectors.counting()
        ));
    }
} 