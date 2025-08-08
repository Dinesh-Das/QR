package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.annotation.RequireRole;
import com.cqs.qrmfg.annotation.PlantDataFilter;
import com.cqs.qrmfg.enums.RoleType;
import com.cqs.qrmfg.repository.DashboardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    @Autowired
    private DashboardRepository dashboardRepository;

    @GetMapping("/summary")
    @RequireRole({RoleType.ADMIN, RoleType.JVC_ROLE, RoleType.CQS_ROLE, RoleType.TECH_ROLE, RoleType.PLANT_ROLE})
    @PlantDataFilter(entityField = "plantCode", filterType = PlantDataFilter.FilterType.AUTO)
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        try {
            Map<String, Object> summary = dashboardRepository.getDashboardSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            // Log the error and return a fallback response
            System.err.println("Error getting dashboard summary: " + e.getMessage());
            e.printStackTrace();
            
            // Return basic fallback data
            Map<String, Object> fallback = new java.util.HashMap<>();
            fallback.put("totalWorkflows", 0);
            fallback.put("activeWorkflows", 0);
            fallback.put("completedWorkflows", 0);
            fallback.put("overdueWorkflows", 0);
            fallback.put("totalQueries", 0);
            fallback.put("openQueries", 0);
            fallback.put("resolvedQueries", 0);
            fallback.put("avgResolutionTimeHours", 0.0);
            fallback.put("recentWorkflows", 0);
            fallback.put("recentCompletions", 0);
            fallback.put("completedToday", 0);
            
            return ResponseEntity.ok(fallback);
        }
    }

    @GetMapping("/counts-by-state")
    @RequireRole({RoleType.ADMIN, RoleType.JVC_ROLE, RoleType.CQS_ROLE, RoleType.TECH_ROLE, RoleType.PLANT_ROLE})
    @PlantDataFilter(entityField = "plantCode", filterType = PlantDataFilter.FilterType.AUTO)
    public ResponseEntity<Map<String, Long>> getWorkflowCountsByState() {
        Map<String, Long> counts = dashboardRepository.getWorkflowCountsByState();
        return ResponseEntity.ok(counts);
    }

    @GetMapping("/query-counts-by-team")
    @RequireRole({RoleType.ADMIN, RoleType.JVC_ROLE, RoleType.CQS_ROLE, RoleType.TECH_ROLE, RoleType.PLANT_ROLE})
    @PlantDataFilter(entityField = "plantCode", filterType = PlantDataFilter.FilterType.AUTO)
    public ResponseEntity<Map<String, Long>> getQueryCountsByTeam() {
        Map<String, Long> counts = dashboardRepository.getQueryCountsByTeam();
        return ResponseEntity.ok(counts);
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testDatabaseConnection() {
        Map<String, Object> result = new java.util.HashMap<>();
        try {
            // Test basic database connection
            Integer count = dashboardRepository.testConnection();
            result.put("connectionTest", "SUCCESS");
            result.put("dualResult", count);
            
            // Test if tables exist
            result.put("tablesExist", dashboardRepository.checkTablesExist());
            
        } catch (Exception e) {
            result.put("connectionTest", "FAILED");
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
}