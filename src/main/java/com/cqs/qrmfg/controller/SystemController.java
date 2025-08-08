package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.service.UserService;
import com.cqs.qrmfg.service.UserSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {
    @Autowired private UserService userService;
    @Autowired private UserSessionService userSessionService;

    @GetMapping("/health")
    public Map<String, Object> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        health.put("uptime", rb.getUptime());
        health.put("startTime", rb.getStartTime());
        health.put("memory", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        // Add more checks as needed (e.g., DB status)
        return health;
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeUsers", userService.findAll().stream().filter(u -> u.isEnabled()).count());
        stats.put("totalUsers", userService.findAll().size());
        stats.put("activeSessions", userSessionService.findAll().stream().filter(s -> s.isActive()).count());
        stats.put("totalSessions", userSessionService.findAll().size());
        // Add more stats as needed
        return stats;
    }
} 