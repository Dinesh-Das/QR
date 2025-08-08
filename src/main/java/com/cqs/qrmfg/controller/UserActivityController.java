package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.service.UserActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST controller for user activity analytics and monitoring
 */
@RestController
@RequestMapping("/api/user-activity")
@PreAuthorize("hasRole('ADMIN')")
public class UserActivityController {

    @Autowired
    private UserActivityService userActivityService;

    /**
     * Get user activity analytics for a specific time period
     */
    @GetMapping("/analytics")
    public ResponseEntity<UserActivityService.UserActivityAnalytics> getActivityAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        UserActivityService.UserActivityAnalytics analytics = 
                userActivityService.getActivityAnalytics(startDate, endDate);
        return ResponseEntity.ok(analytics);
    }

    /**
     * Get most active users
     */
    @GetMapping("/most-active")
    public ResponseEntity<List<UserActivityService.UserActivitySummary>> getMostActiveUsers(
            @RequestParam(defaultValue = "10") int limit) {
        
        List<UserActivityService.UserActivitySummary> activeUsers = 
                userActivityService.getMostActiveUsers(limit);
        return ResponseEntity.ok(activeUsers);
    }

    /**
     * Get most used features
     */
    @GetMapping("/most-used-features")
    public ResponseEntity<List<UserActivityService.FeatureUsageSummary>> getMostUsedFeatures(
            @RequestParam(defaultValue = "10") int limit) {
        
        List<UserActivityService.FeatureUsageSummary> features = 
                userActivityService.getMostUsedFeatures(limit);
        return ResponseEntity.ok(features);
    }

    /**
     * Get activity patterns by hour of day
     */
    @GetMapping("/activity-patterns")
    public ResponseEntity<Map<Integer, Long>> getActivityPatternsByHour() {
        Map<Integer, Long> patterns = userActivityService.getActivityPatternsByHour();
        return ResponseEntity.ok(patterns);
    }

    /**
     * Get workflow usage patterns
     */
    @GetMapping("/workflow-patterns")
    public ResponseEntity<UserActivityService.WorkflowUsagePatterns> getWorkflowUsagePatterns() {
        UserActivityService.WorkflowUsagePatterns patterns = 
                userActivityService.getWorkflowUsagePatterns();
        return ResponseEntity.ok(patterns);
    }

    /**
     * Get user performance metrics for a specific user
     */
    @GetMapping("/user-performance/{username}")
    public ResponseEntity<UserActivityService.UserPerformanceMetrics> getUserPerformanceMetrics(
            @PathVariable String username) {
        
        UserActivityService.UserPerformanceMetrics metrics = 
                userActivityService.getUserPerformanceMetrics(username);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Record user activity (this endpoint is also available for regular users)
     */
    @PostMapping("/record")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> recordActivity(
            @RequestParam String username,
            @RequestParam String action,
            @RequestParam String component,
            @RequestParam(required = false) String details) {
        
        userActivityService.recordActivity(username, action, component, details);
        return ResponseEntity.ok().build();
    }
}