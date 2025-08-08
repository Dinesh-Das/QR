package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.service.UserActivityService;
import com.cqs.qrmfg.service.MetricsService;
import com.cqs.qrmfg.repository.WorkflowRepository;
import com.cqs.qrmfg.repository.QueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of UserActivityService for tracking user activity patterns
 */
@Service
public class UserActivityServiceImpl implements UserActivityService {

    private static final Logger logger = LoggerFactory.getLogger(UserActivityServiceImpl.class);

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private QueryRepository queryRepository;

    // In-memory storage for activity tracking (in production, this should be persisted)
    private final ConcurrentHashMap<String, List<ActivityEvent>> userActivities = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> featureUsage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Long> hourlyActivity = new ConcurrentHashMap<>();

    @Override
    public void recordActivity(String username, String action, String component, String details) {
        ActivityEvent event = new ActivityEvent(username, action, component, details, LocalDateTime.now());
        
        // Store activity event
        userActivities.computeIfAbsent(username, k -> new ArrayList<>()).add(event);
        
        // Update feature usage
        String featureKey = component + ":" + action;
        featureUsage.merge(featureKey, 1L, Long::sum);
        
        // Update hourly activity
        int hour = event.getTimestamp().getHour();
        hourlyActivity.merge(hour, 1L, Long::sum);
        
        // Record in metrics service
        metricsService.recordUserActivity(username, action, component);
        
        logger.debug("Recorded activity: {} performed {} on {} - {}", username, action, component, details);
    }

    @Override
    public UserActivityAnalytics getActivityAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        long totalActivities = userActivities.values().stream()
                .flatMap(List::stream)
                .filter(event -> event.getTimestamp().isAfter(startDate) && event.getTimestamp().isBefore(endDate))
                .count();

        long uniqueUsers = userActivities.entrySet().stream()
                .filter(entry -> entry.getValue().stream()
                        .anyMatch(event -> event.getTimestamp().isAfter(startDate) && event.getTimestamp().isBefore(endDate)))
                .count();

        double averageActivitiesPerUser = uniqueUsers > 0 ? (double) totalActivities / uniqueUsers : 0.0;

        // Find peak activity hour
        Map<Integer, Long> hourlyActivityInRange = new HashMap<>();
        userActivities.values().stream()
                .flatMap(List::stream)
                .filter(event -> event.getTimestamp().isAfter(startDate) && event.getTimestamp().isBefore(endDate))
                .forEach(event -> hourlyActivityInRange.merge(event.getTimestamp().getHour(), 1L, Long::sum));

        String peakActivityHour = hourlyActivityInRange.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey() + ":00")
                .orElse("N/A");

        // Activity by component
        Map<String, Long> activityByComponent = userActivities.values().stream()
                .flatMap(List::stream)
                .filter(event -> event.getTimestamp().isAfter(startDate) && event.getTimestamp().isBefore(endDate))
                .collect(Collectors.groupingBy(ActivityEvent::getComponent, Collectors.counting()));

        return new UserActivityAnalytics(totalActivities, uniqueUsers, averageActivitiesPerUser, 
                                       peakActivityHour, activityByComponent);
    }

    @Override
    public List<UserActivitySummary> getMostActiveUsers(int limit) {
        return userActivities.entrySet().stream()
                .map(entry -> {
                    String username = entry.getKey();
                    List<ActivityEvent> activities = entry.getValue();
                    long totalActivities = activities.size();
                    
                    String mostUsedFeature = activities.stream()
                            .collect(Collectors.groupingBy(event -> event.getComponent() + ":" + event.getAction(), 
                                   Collectors.counting()))
                            .entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse("N/A");
                    
                    LocalDateTime lastActivity = activities.stream()
                            .map(ActivityEvent::getTimestamp)
                            .max(LocalDateTime::compareTo)
                            .orElse(LocalDateTime.now());
                    
                    return new UserActivitySummary(username, totalActivities, mostUsedFeature, lastActivity);
                })
                .sorted((a, b) -> Long.compare(b.getTotalActivities(), a.getTotalActivities()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<FeatureUsageSummary> getMostUsedFeatures(int limit) {
        Map<String, Set<String>> featureUsers = new HashMap<>();
        
        // Calculate unique users per feature
        userActivities.forEach((username, activities) -> {
            activities.forEach(activity -> {
                String featureKey = activity.getComponent() + ":" + activity.getAction();
                featureUsers.computeIfAbsent(featureKey, k -> new HashSet<>()).add(username);
            });
        });
        
        return featureUsage.entrySet().stream()
                .map(entry -> {
                    String feature = entry.getKey();
                    long usageCount = entry.getValue();
                    long uniqueUsers = featureUsers.getOrDefault(feature, Collections.emptySet()).size();
                    double averageUsagePerUser = uniqueUsers > 0 ? (double) usageCount / uniqueUsers : 0.0;
                    
                    return new FeatureUsageSummary(feature, usageCount, uniqueUsers, averageUsagePerUser);
                })
                .sorted((a, b) -> Long.compare(b.getUsageCount(), a.getUsageCount()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public Map<Integer, Long> getActivityPatternsByHour() {
        return new HashMap<>(hourlyActivity);
    }

    @Override
    public WorkflowUsagePatterns getWorkflowUsagePatterns() {
        try {
            // Get workflow statistics from repository
            List<Object[]> stateStats = workflowRepository.countByPlantCodeGrouped();
            Map<String, Long> workflowsByState = stateStats.stream()
                    .collect(Collectors.toMap(
                            row -> row[0].toString(),
                            row -> ((Number) row[1]).longValue()
                    ));

            List<Object[]> plantStats = workflowRepository.countByPlantCodeGrouped();
            Map<String, Long> workflowsByPlant = plantStats.stream()
                    .collect(Collectors.toMap(
                            row -> row[0].toString(),
                            row -> ((Number) row[1]).longValue()
                    ));

            // Calculate average completion time by plant (mock data for now)
            Map<String, Double> averageCompletionTimeByPlant = workflowsByPlant.keySet().stream()
                    .collect(Collectors.toMap(
                            plant -> plant,
                            plant -> 24.0 + Math.random() * 48.0 // Mock: 24-72 hours
                    ));

            // Get most used project-material combinations
            List<Object[]> combinations = workflowRepository.countByPlantCodeGrouped(); // Placeholder for missing method
            List<String> mostUsedProjectMaterialCombinations = combinations.stream()
                    .limit(10)
                    .map(row -> row[0] + " - " + row[1])
                    .collect(Collectors.toList());

            return new WorkflowUsagePatterns(workflowsByState, workflowsByPlant, 
                                           averageCompletionTimeByPlant, mostUsedProjectMaterialCombinations);
        } catch (Exception e) {
            logger.error("Error getting workflow usage patterns", e);
            return new WorkflowUsagePatterns(Collections.emptyMap(), Collections.emptyMap(), 
                                           Collections.emptyMap(), Collections.emptyList());
        }
    }

    @Override
    public UserPerformanceMetrics getUserPerformanceMetrics(String username) {
        try {
            // Get user workflow statistics
            long workflowsInitiated = workflowRepository.findByInitiatedByWithQueries(username).size();
            long workflowsCompleted = workflowRepository.findByInitiatedByWithQueries(username).stream()
                    .filter(w -> "COMPLETED".equals(w.getState().toString()))
                    .count();

            // Get user query statistics
            long queriesRaised = queryRepository.findByRaisedBy(username).size();
            long queriesResolved = queryRepository.findByResolvedBy(username).size();

            // Calculate average times (mock data for now)
            double averageWorkflowCompletionTime = 48.0 + Math.random() * 24.0; // Mock: 48-72 hours
            double averageQueryResolutionTime = 4.0 + Math.random() * 8.0; // Mock: 4-12 hours

            return new UserPerformanceMetrics(username, workflowsInitiated, workflowsCompleted,
                                            queriesRaised, queriesResolved, averageWorkflowCompletionTime,
                                            averageQueryResolutionTime);
        } catch (Exception e) {
            logger.error("Error getting user performance metrics for {}", username, e);
            return new UserPerformanceMetrics(username, 0, 0, 0, 0, 0.0, 0.0);
        }
    }

    // Internal class for activity events
    private static class ActivityEvent {
        private final String username;
        private final String action;
        private final String component;
        private final String details;
        private final LocalDateTime timestamp;

        public ActivityEvent(String username, String action, String component, String details, LocalDateTime timestamp) {
            this.username = username;
            this.action = action;
            this.component = component;
            this.details = details;
            this.timestamp = timestamp;
        }

        // Getters
        public String getUsername() { return username; }
        public String getAction() { return action; }
        public String getComponent() { return component; }
        public String getDetails() { return details; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}