package com.cqs.qrmfg.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service for tracking and analyzing user activity patterns
 */
public interface UserActivityService {
    
    /**
     * Record user activity event
     */
    void recordActivity(String username, String action, String component, String details);
    
    /**
     * Get user activity analytics for a specific time period
     */
    UserActivityAnalytics getActivityAnalytics(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Get most active users
     */
    List<UserActivitySummary> getMostActiveUsers(int limit);
    
    /**
     * Get most used features
     */
    List<FeatureUsageSummary> getMostUsedFeatures(int limit);
    
    /**
     * Get user activity patterns by time of day
     */
    Map<Integer, Long> getActivityPatternsByHour();
    
    /**
     * Get workflow usage patterns
     */
    WorkflowUsagePatterns getWorkflowUsagePatterns();
    
    /**
     * Get user performance metrics
     */
    UserPerformanceMetrics getUserPerformanceMetrics(String username);
    
    // Data classes for analytics
    class UserActivityAnalytics {
        private final long totalActivities;
        private final long uniqueUsers;
        private final double averageActivitiesPerUser;
        private final String peakActivityHour;
        private final Map<String, Long> activityByComponent;
        
        public UserActivityAnalytics(long totalActivities, long uniqueUsers, double averageActivitiesPerUser, 
                                   String peakActivityHour, Map<String, Long> activityByComponent) {
            this.totalActivities = totalActivities;
            this.uniqueUsers = uniqueUsers;
            this.averageActivitiesPerUser = averageActivitiesPerUser;
            this.peakActivityHour = peakActivityHour;
            this.activityByComponent = activityByComponent;
        }
        
        // Getters
        public long getTotalActivities() { return totalActivities; }
        public long getUniqueUsers() { return uniqueUsers; }
        public double getAverageActivitiesPerUser() { return averageActivitiesPerUser; }
        public String getPeakActivityHour() { return peakActivityHour; }
        public Map<String, Long> getActivityByComponent() { return activityByComponent; }
    }
    
    class UserActivitySummary {
        private final String username;
        private final long totalActivities;
        private final String mostUsedFeature;
        private final LocalDateTime lastActivity;
        
        public UserActivitySummary(String username, long totalActivities, String mostUsedFeature, LocalDateTime lastActivity) {
            this.username = username;
            this.totalActivities = totalActivities;
            this.mostUsedFeature = mostUsedFeature;
            this.lastActivity = lastActivity;
        }
        
        // Getters
        public String getUsername() { return username; }
        public long getTotalActivities() { return totalActivities; }
        public String getMostUsedFeature() { return mostUsedFeature; }
        public LocalDateTime getLastActivity() { return lastActivity; }
    }
    
    class FeatureUsageSummary {
        private final String feature;
        private final long usageCount;
        private final long uniqueUsers;
        private final double averageUsagePerUser;
        
        public FeatureUsageSummary(String feature, long usageCount, long uniqueUsers, double averageUsagePerUser) {
            this.feature = feature;
            this.usageCount = usageCount;
            this.uniqueUsers = uniqueUsers;
            this.averageUsagePerUser = averageUsagePerUser;
        }
        
        // Getters
        public String getFeature() { return feature; }
        public long getUsageCount() { return usageCount; }
        public long getUniqueUsers() { return uniqueUsers; }
        public double getAverageUsagePerUser() { return averageUsagePerUser; }
    }
    
    class WorkflowUsagePatterns {
        private final Map<String, Long> workflowsByState;
        private final Map<String, Long> workflowsByPlant;
        private final Map<String, Double> averageCompletionTimeByPlant;
        private final List<String> mostUsedProjectMaterialCombinations;
        
        public WorkflowUsagePatterns(Map<String, Long> workflowsByState, Map<String, Long> workflowsByPlant,
                                   Map<String, Double> averageCompletionTimeByPlant, List<String> mostUsedProjectMaterialCombinations) {
            this.workflowsByState = workflowsByState;
            this.workflowsByPlant = workflowsByPlant;
            this.averageCompletionTimeByPlant = averageCompletionTimeByPlant;
            this.mostUsedProjectMaterialCombinations = mostUsedProjectMaterialCombinations;
        }
        
        // Getters
        public Map<String, Long> getWorkflowsByState() { return workflowsByState; }
        public Map<String, Long> getWorkflowsByPlant() { return workflowsByPlant; }
        public Map<String, Double> getAverageCompletionTimeByPlant() { return averageCompletionTimeByPlant; }
        public List<String> getMostUsedProjectMaterialCombinations() { return mostUsedProjectMaterialCombinations; }
    }
    
    class UserPerformanceMetrics {
        private final String username;
        private final long workflowsInitiated;
        private final long workflowsCompleted;
        private final long queriesRaised;
        private final long queriesResolved;
        private final double averageWorkflowCompletionTime;
        private final double averageQueryResolutionTime;
        
        public UserPerformanceMetrics(String username, long workflowsInitiated, long workflowsCompleted,
                                    long queriesRaised, long queriesResolved, double averageWorkflowCompletionTime,
                                    double averageQueryResolutionTime) {
            this.username = username;
            this.workflowsInitiated = workflowsInitiated;
            this.workflowsCompleted = workflowsCompleted;
            this.queriesRaised = queriesRaised;
            this.queriesResolved = queriesResolved;
            this.averageWorkflowCompletionTime = averageWorkflowCompletionTime;
            this.averageQueryResolutionTime = averageQueryResolutionTime;
        }
        
        // Getters
        public String getUsername() { return username; }
        public long getWorkflowsInitiated() { return workflowsInitiated; }
        public long getWorkflowsCompleted() { return workflowsCompleted; }
        public long getQueriesRaised() { return queriesRaised; }
        public long getQueriesResolved() { return queriesResolved; }
        public double getAverageWorkflowCompletionTime() { return averageWorkflowCompletionTime; }
        public double getAverageQueryResolutionTime() { return averageQueryResolutionTime; }
    }
}