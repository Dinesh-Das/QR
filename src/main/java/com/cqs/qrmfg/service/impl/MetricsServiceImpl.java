package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.config.MonitoringConfiguration;
import com.cqs.qrmfg.model.WorkflowState;
import com.cqs.qrmfg.model.QueryTeam;
import com.cqs.qrmfg.service.MetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of MetricsService for tracking application metrics
 */
@Service
public class MetricsServiceImpl implements MetricsService {

    private static final Logger logger = LoggerFactory.getLogger(MetricsServiceImpl.class);

    @Autowired
    private Timer workflowProcessingTimer;

    @Autowired
    private Timer queryResolutionTimer;

    @Autowired
    private Timer databaseQueryTimer;

    @Autowired
    private Timer notificationProcessingTimer;

    @Autowired
    private Counter workflowStateTransitionCounter;

    @Autowired
    private Counter queryCreatedCounter;

    @Autowired
    private Counter queryResolvedCounter;

    @Autowired
    private Counter documentUploadCounter;

    @Autowired
    private Counter documentDownloadCounter;

    @Autowired
    private Counter notificationSentCounter;

    @Autowired
    private Counter notificationFailureCounter;

    @Autowired
    private Counter userActivityCounter;

    @Autowired
    private MonitoringConfiguration monitoringConfiguration;

    // Additional tracking for detailed metrics
    private final ConcurrentHashMap<String, AtomicLong> userActivityMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> featureUsageMap = new ConcurrentHashMap<>();
    private final AtomicLong totalWorkflowProcessingTime = new AtomicLong(0);
    private final AtomicLong workflowProcessingCount = new AtomicLong(0);
    private final AtomicLong totalQueryResolutionTime = new AtomicLong(0);
    private final AtomicLong queryResolutionCount = new AtomicLong(0);
    private final AtomicLong totalNotificationProcessingTime = new AtomicLong(0);
    private final AtomicLong notificationProcessingCount = new AtomicLong(0);

    @Override
    public void recordWorkflowProcessingTime(long durationMs, WorkflowState fromState, WorkflowState toState) {
        workflowProcessingTimer.record(Duration.ofMillis(durationMs));
        totalWorkflowProcessingTime.addAndGet(durationMs);
        workflowProcessingCount.incrementAndGet();
        
        logger.debug("Recorded workflow processing time: {}ms for transition {} -> {}", 
                    durationMs, fromState, toState);
    }

    @Override
    public void recordQueryResolutionTime(long durationMs, QueryTeam team) {
        queryResolutionTimer.record(Duration.ofMillis(durationMs));
        totalQueryResolutionTime.addAndGet(durationMs);
        queryResolutionCount.incrementAndGet();
        
        logger.debug("Recorded query resolution time: {}ms for team {}", durationMs, team);
    }

    @Override
    public void recordDatabaseQueryTime(long durationMs, String queryType) {
        databaseQueryTimer.record(Duration.ofMillis(durationMs));
        logger.debug("Recorded database query time: {}ms for type {}", durationMs, queryType);
    }

    @Override
    public void recordNotificationProcessingTime(long durationMs, String notificationType) {
        notificationProcessingTimer.record(Duration.ofMillis(durationMs));
        totalNotificationProcessingTime.addAndGet(durationMs);
        notificationProcessingCount.incrementAndGet();
        
        logger.debug("Recorded notification processing time: {}ms for type {}", durationMs, notificationType);
    }

    @Override
    public void incrementWorkflowStateTransition(WorkflowState fromState, WorkflowState toState) {
        workflowStateTransitionCounter.increment();
        
        // Track state-specific counters
        String transitionKey = fromState + "_TO_" + toState;
        monitoringConfiguration.getWorkflowStateCounters()
                .computeIfAbsent(transitionKey, k -> new AtomicLong(0))
                .incrementAndGet();
        
        logger.debug("Incremented workflow state transition: {} -> {}", fromState, toState);
    }

    @Override
    public void incrementQueryCreated(QueryTeam team) {
        queryCreatedCounter.increment();
        monitoringConfiguration.getTotalQueries().incrementAndGet();
        
        logger.debug("Incremented query created for team: {}", team);
    }

    @Override
    public void incrementQueryResolved(QueryTeam team) {
        queryResolvedCounter.increment();
        monitoringConfiguration.getResolvedQueries().incrementAndGet();
        
        logger.debug("Incremented query resolved for team: {}", team);
    }

    @Override
    public void incrementDocumentUpload(String fileType) {
        documentUploadCounter.increment();
        monitoringConfiguration.getDocumentUploads().incrementAndGet();
        
        logger.debug("Incremented document upload for type: {}", fileType);
    }

    @Override
    public void incrementDocumentDownload(String fileType) {
        documentDownloadCounter.increment();
        monitoringConfiguration.getDocumentDownloads().incrementAndGet();
        
        logger.debug("Incremented document download for type: {}", fileType);
    }

    @Override
    public void incrementNotificationSent(String notificationType) {
        notificationSentCounter.increment();
        monitoringConfiguration.getNotificationsSent().incrementAndGet();
        
        logger.debug("Incremented notification sent for type: {}", notificationType);
    }

    @Override
    public void incrementNotificationFailure(String notificationType, String errorType) {
        notificationFailureCounter.increment();
        monitoringConfiguration.getNotificationFailures().incrementAndGet();
        
        logger.warn("Incremented notification failure for type: {}, error: {}", notificationType, errorType);
    }

    @Override
    public void recordUserActivity(String username, String action, String component) {
        userActivityCounter.increment();
        
        // Track user-specific activity
        userActivityMap.computeIfAbsent(username, k -> new AtomicLong(0)).incrementAndGet();
        
        // Track feature usage
        String featureKey = component + ":" + action;
        featureUsageMap.computeIfAbsent(featureKey, k -> new AtomicLong(0)).incrementAndGet();
        
        logger.debug("Recorded user activity: {} performed {} on {}", username, action, component);
    }

    @Override
    public void updateActiveWorkflowsCount(long count) {
        monitoringConfiguration.getActiveWorkflows().set(count);
        logger.debug("Updated active workflows count: {}", count);
    }

    @Override
    public void updatePendingQueriesCount(long count) {
        // This is calculated dynamically in the gauge
        logger.debug("Pending queries count updated: {}", count);
    }

    @Override
    public WorkflowMetrics getWorkflowMetrics() {
        long totalWorkflows = workflowProcessingCount.get();
        long activeWorkflows = monitoringConfiguration.getActiveWorkflows().get();
        double averageProcessingTime = totalWorkflows > 0 ? 
                (double) totalWorkflowProcessingTime.get() / totalWorkflows : 0.0;
        long stateTransitions = (long) workflowStateTransitionCounter.count();
        
        return new WorkflowMetrics(totalWorkflows, activeWorkflows, averageProcessingTime, stateTransitions);
    }

    @Override
    public QueryMetrics getQueryMetrics() {
        long totalQueries = monitoringConfiguration.getTotalQueries().get();
        long resolvedQueries = monitoringConfiguration.getResolvedQueries().get();
        long pendingQueries = totalQueries - resolvedQueries;
        double averageResolutionTime = queryResolutionCount.get() > 0 ? 
                (double) totalQueryResolutionTime.get() / queryResolutionCount.get() : 0.0;
        
        return new QueryMetrics(totalQueries, pendingQueries, resolvedQueries, averageResolutionTime);
    }

    @Override
    public NotificationMetrics getNotificationMetrics() {
        long totalNotifications = monitoringConfiguration.getNotificationsSent().get() + 
                                 monitoringConfiguration.getNotificationFailures().get();
        long successfulNotifications = monitoringConfiguration.getNotificationsSent().get();
        long failedNotifications = monitoringConfiguration.getNotificationFailures().get();
        double averageProcessingTime = notificationProcessingCount.get() > 0 ? 
                (double) totalNotificationProcessingTime.get() / notificationProcessingCount.get() : 0.0;
        
        return new NotificationMetrics(totalNotifications, successfulNotifications, failedNotifications, averageProcessingTime);
    }

    @Override
    public UserActivityMetrics getUserActivityMetrics() {
        long totalActivities = (long) userActivityCounter.count();
        long uniqueUsers = userActivityMap.size();
        
        // Find most active user
        String mostActiveUser = userActivityMap.entrySet().stream()
                .max((e1, e2) -> Long.compare(e1.getValue().get(), e2.getValue().get()))
                .map(entry -> entry.getKey())
                .orElse("N/A");
        
        // Find most used feature
        String mostUsedFeature = featureUsageMap.entrySet().stream()
                .max((e1, e2) -> Long.compare(e1.getValue().get(), e2.getValue().get()))
                .map(entry -> entry.getKey())
                .orElse("N/A");
        
        return new UserActivityMetrics(totalActivities, uniqueUsers, mostActiveUser, mostUsedFeature);
    }
}