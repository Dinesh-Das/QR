package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.Workflow;
import com.cqs.qrmfg.model.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationSchedulerService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationSchedulerService.class);
    
    @Autowired
    private WorkflowService workflowService;
    
    @Autowired
    private QueryService queryService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private WorkflowNotificationIntegrationService integrationService;
    
    /**
     * Check for overdue workflows every hour and send notifications
     */
    @Scheduled(fixedRate = 3600000) // 1 hour = 3600000 ms
    @Transactional(readOnly = true)
    public void checkOverdueWorkflows() {
        logger.debug("Checking for overdue workflows...");
        
        try {
            List<Workflow> overdueWorkflows = workflowService.findOverdueWorkflows();
            
            for (Workflow workflow : overdueWorkflows) {
                logger.info("Found overdue workflow: {} ({} days pending)", 
                           workflow.getMaterialCode(), workflow.getDaysPending());
                
                // Use integration service for consistent notification handling
                integrationService.handleWorkflowOverdue(workflow);
            }
            
            if (!overdueWorkflows.isEmpty()) {
                logger.info("Processed {} overdue workflow notifications", overdueWorkflows.size());
            }
            
        } catch (Exception e) {
            logger.error("Error checking overdue workflows: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Check for overdue queries every 30 minutes and send notifications
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes = 1800000 ms
    @Transactional(readOnly = true)
    public void checkOverdueQueries() {
        logger.debug("Checking for overdue queries...");
        
        try {
            List<Query> overdueQueries = queryService.findOverdueQueries();
            
            for (Query query : overdueQueries) {
                logger.info("Found overdue query: {} for material {} ({} days open)", 
                           query.getId(), query.getWorkflow().getMaterialCode(), query.getDaysOpen());
                
                // Use integration service for consistent notification handling
                integrationService.handleQueryOverdue(query);
            }
            
            if (!overdueQueries.isEmpty()) {
                logger.info("Processed {} overdue query notifications", overdueQueries.size());
            }
            
        } catch (Exception e) {
            logger.error("Error checking overdue queries: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Send daily digest notifications at 9 AM
     */
    @Scheduled(cron = "0 0 9 * * MON-FRI") // 9 AM on weekdays
    @Transactional(readOnly = true)
    public void sendDailyDigest() {
        logger.info("Sending daily digest notifications...");
        
        try {
            // Get summary statistics
            long overdueWorkflowCount = workflowService.countOverdueWorkflows();
            long overdueQueryCount = queryService.countOverdueQueries();
            long todayQueriesCreated = queryService.countQueriesCreatedToday();
            long todayQueriesResolved = queryService.countQueriesResolvedToday();
            
            // Create digest message
            StringBuilder digest = new StringBuilder();
            digest.append("Daily MSDS Workflow Digest\n\n");
            digest.append(String.format("Overdue Workflows: %d\n", overdueWorkflowCount));
            digest.append(String.format("Overdue Queries: %d\n", overdueQueryCount));
            digest.append(String.format("Queries Created Today: %d\n", todayQueriesCreated));
            digest.append(String.format("Queries Resolved Today: %d\n", todayQueriesResolved));
            
            // Send to admins
            notificationService.notifyAdmins("Daily MSDS Workflow Digest", digest.toString());
            
            logger.info("Daily digest sent successfully");
            
        } catch (Exception e) {
            logger.error("Error sending daily digest: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Send weekly summary notifications on Monday at 8 AM
     */
    @Scheduled(cron = "0 0 8 * * MON") // 8 AM on Mondays
    @Transactional(readOnly = true)
    public void sendWeeklySummary() {
        logger.info("Sending weekly summary notifications...");
        
        try {
            // Get recent workflows and queries
            List<Workflow> recentWorkflows = workflowService.findRecentlyCreated(7);
            List<Workflow> completedWorkflows = workflowService.findRecentlyCompleted(7);
            List<Query> recentQueries = queryService.findRecentQueries(7);
            
            // Create summary message
            StringBuilder summary = new StringBuilder();
            summary.append("Weekly MSDS Workflow Summary\n\n");
            summary.append(String.format("New Workflows This Week: %d\n", recentWorkflows.size()));
            summary.append(String.format("Completed Workflows This Week: %d\n", completedWorkflows.size()));
            summary.append(String.format("Queries Raised This Week: %d\n", recentQueries.size()));
            
            // Add workflow details
            if (!recentWorkflows.isEmpty()) {
                summary.append("\nNew Workflows:\n");
                for (Workflow workflow : recentWorkflows) {
                    summary.append(String.format("- %s (%s) - %s\n", 
                                                 workflow.getMaterialCode(), 
                                                 workflow.getAssignedPlant(),
                                                 workflow.getState().getDisplayName()));
                }
            }
            
            if (!completedWorkflows.isEmpty()) {
                summary.append("\nCompleted Workflows:\n");
                for (Workflow workflow : completedWorkflows) {
                    summary.append(String.format("- %s (%s) - Completed\n", 
                                                 workflow.getMaterialCode(), 
                                                 workflow.getAssignedPlant()));
                }
            }
            
            // Send to admins
            notificationService.notifyAdmins("Weekly MSDS Workflow Summary", summary.toString());
            
            logger.info("Weekly summary sent successfully");
            
        } catch (Exception e) {
            logger.error("Error sending weekly summary: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Clean up old notification records monthly
     */
    @Scheduled(cron = "0 0 2 1 * *") // 2 AM on the 1st of each month
    public void cleanupNotifications() {
        logger.info("Cleaning up old notification records...");
        
        try {
            // Clear failed notifications older than 30 days
            notificationService.clearFailedNotifications();
            
            logger.info("Notification cleanup completed");
            
        } catch (Exception e) {
            logger.error("Error during notification cleanup: {}", e.getMessage(), e);
        }
    }
}