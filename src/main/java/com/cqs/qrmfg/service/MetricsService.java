package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.WorkflowState;
import com.cqs.qrmfg.model.QueryTeam;

/**
 * Service for tracking application metrics and performance
 */
public interface MetricsService {
    
    /**
     * Record workflow processing time
     */
    void recordWorkflowProcessingTime(long durationMs, WorkflowState fromState, WorkflowState toState);
    
    /**
     * Record query resolution time
     */
    void recordQueryResolutionTime(long durationMs, QueryTeam team);
    
    /**
     * Record database query execution time
     */
    void recordDatabaseQueryTime(long durationMs, String queryType);
    
    /**
     * Record notification processing time
     */
    void recordNotificationProcessingTime(long durationMs, String notificationType);
    
    /**
     * Increment workflow state transition counter
     */
    void incrementWorkflowStateTransition(WorkflowState fromState, WorkflowState toState);
    
    /**
     * Increment query created counter
     */
    void incrementQueryCreated(QueryTeam team);
    
    /**
     * Increment query resolved counter
     */
    void incrementQueryResolved(QueryTeam team);
    
    /**
     * Increment document upload counter
     */
    void incrementDocumentUpload(String fileType);
    
    /**
     * Increment document download counter
     */
    void incrementDocumentDownload(String fileType);
    
    /**
     * Increment notification sent counter
     */
    void incrementNotificationSent(String notificationType);
    
    /**
     * Increment notification failure counter
     */
    void incrementNotificationFailure(String notificationType, String errorType);
    
    /**
     * Record user activity
     */
    void recordUserActivity(String username, String action, String component);
    
    /**
     * Update active workflows count
     */
    void updateActiveWorkflowsCount(long count);
    
    /**
     * Update pending queries count
     */
    void updatePendingQueriesCount(long count);
    
    /**
     * Get workflow processing metrics
     */
    WorkflowMetrics getWorkflowMetrics();
    
    /**
     * Get query resolution metrics
     */
    QueryMetrics getQueryMetrics();
    
    /**
     * Get notification metrics
     */
    NotificationMetrics getNotificationMetrics();
    
    /**
     * Get user activity metrics
     */
    UserActivityMetrics getUserActivityMetrics();
    
    // Metric data classes
    class WorkflowMetrics {
        private final long totalWorkflows;
        private final long activeWorkflows;
        private final double averageProcessingTime;
        private final long stateTransitions;
        
        public WorkflowMetrics(long totalWorkflows, long activeWorkflows, double averageProcessingTime, long stateTransitions) {
            this.totalWorkflows = totalWorkflows;
            this.activeWorkflows = activeWorkflows;
            this.averageProcessingTime = averageProcessingTime;
            this.stateTransitions = stateTransitions;
        }
        
        // Getters
        public long getTotalWorkflows() { return totalWorkflows; }
        public long getActiveWorkflows() { return activeWorkflows; }
        public double getAverageProcessingTime() { return averageProcessingTime; }
        public long getStateTransitions() { return stateTransitions; }
    }
    
    class QueryMetrics {
        private final long totalQueries;
        private final long pendingQueries;
        private final long resolvedQueries;
        private final double averageResolutionTime;
        
        public QueryMetrics(long totalQueries, long pendingQueries, long resolvedQueries, double averageResolutionTime) {
            this.totalQueries = totalQueries;
            this.pendingQueries = pendingQueries;
            this.resolvedQueries = resolvedQueries;
            this.averageResolutionTime = averageResolutionTime;
        }
        
        // Getters
        public long getTotalQueries() { return totalQueries; }
        public long getPendingQueries() { return pendingQueries; }
        public long getResolvedQueries() { return resolvedQueries; }
        public double getAverageResolutionTime() { return averageResolutionTime; }
    }
    
    class NotificationMetrics {
        private final long totalNotifications;
        private final long successfulNotifications;
        private final long failedNotifications;
        private final double averageProcessingTime;
        
        public NotificationMetrics(long totalNotifications, long successfulNotifications, long failedNotifications, double averageProcessingTime) {
            this.totalNotifications = totalNotifications;
            this.successfulNotifications = successfulNotifications;
            this.failedNotifications = failedNotifications;
            this.averageProcessingTime = averageProcessingTime;
        }
        
        // Getters
        public long getTotalNotifications() { return totalNotifications; }
        public long getSuccessfulNotifications() { return successfulNotifications; }
        public long getFailedNotifications() { return failedNotifications; }
        public double getAverageProcessingTime() { return averageProcessingTime; }
    }
    
    class UserActivityMetrics {
        private final long totalActivities;
        private final long uniqueUsers;
        private final String mostActiveUser;
        private final String mostUsedFeature;
        
        public UserActivityMetrics(long totalActivities, long uniqueUsers, String mostActiveUser, String mostUsedFeature) {
            this.totalActivities = totalActivities;
            this.uniqueUsers = uniqueUsers;
            this.mostActiveUser = mostActiveUser;
            this.mostUsedFeature = mostUsedFeature;
        }
        
        // Getters
        public long getTotalActivities() { return totalActivities; }
        public long getUniqueUsers() { return uniqueUsers; }
        public String getMostActiveUser() { return mostActiveUser; }
        public String getMostUsedFeature() { return mostUsedFeature; }
    }
}