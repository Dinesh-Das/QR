package com.cqs.qrmfg.service;

import com.cqs.qrmfg.repository.WorkflowRepository;
import com.cqs.qrmfg.repository.QueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for periodic performance monitoring and metrics collection
 */
@Service
public class PerformanceMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringService.class);

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private QueryRepository queryRepository;

    /**
     * Update workflow metrics every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void updateWorkflowMetrics() {
        try {
            logger.debug("Updating workflow metrics...");
            
            // Count active workflows
            long activeWorkflows = workflowRepository.countByStateNot(
                com.cqs.qrmfg.model.WorkflowState.COMPLETED);
            metricsService.updateActiveWorkflowsCount(activeWorkflows);
            
            // Count pending queries
            long pendingQueries = queryRepository.countByStatus(
                com.cqs.qrmfg.model.QueryStatus.OPEN);
            metricsService.updatePendingQueriesCount(pendingQueries);
            
            logger.debug("Updated workflow metrics - Active: {}, Pending Queries: {}", 
                        activeWorkflows, pendingQueries);
            
        } catch (Exception e) {
            logger.error("Error updating workflow metrics", e);
        }
    }

    /**
     * Generate performance report every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void generatePerformanceReport() {
        try {
            logger.info("Generating hourly performance report...");
            
            MetricsService.WorkflowMetrics workflowMetrics = metricsService.getWorkflowMetrics();
            MetricsService.QueryMetrics queryMetrics = metricsService.getQueryMetrics();
            MetricsService.NotificationMetrics notificationMetrics = metricsService.getNotificationMetrics();
            MetricsService.UserActivityMetrics userActivityMetrics = metricsService.getUserActivityMetrics();
            
            logger.info("Performance Report:");
            logger.info("- Active Workflows: {}", workflowMetrics.getActiveWorkflows());
            logger.info("- Average Workflow Processing Time: {}ms", workflowMetrics.getAverageProcessingTime());
            logger.info("- Pending Queries: {}", queryMetrics.getPendingQueries());
            logger.info("- Average Query Resolution Time: {}ms", queryMetrics.getAverageResolutionTime());
            logger.info("- Notification Success Rate: {}%", 
                       notificationMetrics.getTotalNotifications() > 0 ? 
                       (notificationMetrics.getSuccessfulNotifications() * 100.0 / notificationMetrics.getTotalNotifications()) : 100);
            logger.info("- Active Users: {}", userActivityMetrics.getUniqueUsers());
            logger.info("- Most Active User: {}", userActivityMetrics.getMostActiveUser());
            
        } catch (Exception e) {
            logger.error("Error generating performance report", e);
        }
    }

    /**
     * Check for performance issues every 15 minutes
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void checkPerformanceIssues() {
        try {
            logger.debug("Checking for performance issues...");
            
            MetricsService.WorkflowMetrics workflowMetrics = metricsService.getWorkflowMetrics();
            MetricsService.QueryMetrics queryMetrics = metricsService.getQueryMetrics();
            MetricsService.NotificationMetrics notificationMetrics = metricsService.getNotificationMetrics();
            
            // Check for slow workflow processing
            if (workflowMetrics.getAverageProcessingTime() > 5000) { // > 5 seconds
                logger.warn("PERFORMANCE ALERT: Slow workflow processing detected - Average: {}ms", 
                           workflowMetrics.getAverageProcessingTime());
            }
            
            // Check for slow query resolution
            if (queryMetrics.getAverageResolutionTime() > 10000) { // > 10 seconds
                logger.warn("PERFORMANCE ALERT: Slow query resolution detected - Average: {}ms", 
                           queryMetrics.getAverageResolutionTime());
            }
            
            // Check notification failure rate
            if (notificationMetrics.getTotalNotifications() > 0) {
                double failureRate = (double) notificationMetrics.getFailedNotifications() / 
                                   notificationMetrics.getTotalNotifications();
                if (failureRate > 0.1) { // > 10% failure rate
                    logger.warn("PERFORMANCE ALERT: High notification failure rate - {}%", 
                               Math.round(failureRate * 100));
                }
            }
            
            // Check for high number of pending queries
            if (queryMetrics.getPendingQueries() > 50) {
                logger.warn("PERFORMANCE ALERT: High number of pending queries - {}", 
                           queryMetrics.getPendingQueries());
            }
            
        } catch (Exception e) {
            logger.error("Error checking performance issues", e);
        }
    }

    /**
     * Clean up old metrics data daily
     */
    @Scheduled(cron = "0 0 2 * * ?") // 2 AM daily
    public void cleanupOldMetrics() {
        try {
            logger.info("Cleaning up old metrics data...");
            
            // In a real implementation, this would clean up old metric data from database
            // For now, we'll just log the cleanup
            logger.info("Metrics cleanup completed");
            
        } catch (Exception e) {
            logger.error("Error cleaning up old metrics", e);
        }
    }
}