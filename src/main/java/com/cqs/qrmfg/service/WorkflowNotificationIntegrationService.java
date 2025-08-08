package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.Workflow;
import com.cqs.qrmfg.model.Query;
import com.cqs.qrmfg.model.WorkflowState;
import static com.cqs.qrmfg.model.WorkflowState.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Service that handles integration between workflow events and notifications.
 * This ensures all workflow state changes and query events trigger appropriate notifications.
 */
@Service
public class WorkflowNotificationIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(WorkflowNotificationIntegrationService.class);
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * Handle workflow creation events
     */
    public void handleWorkflowCreated(Workflow workflow) {
        logger.info("Handling workflow created event for material: {}", workflow.getMaterialCode());
        
        try {
            notificationService.notifyWorkflowCreated(workflow);
        } catch (Exception e) {
            logger.error("Failed to send workflow created notification for material {}: {}", 
                        workflow.getMaterialCode(), e.getMessage(), e);
        }
    }
    
    /**
     * Handle workflow state transition events
     */
    public void handleWorkflowStateChanged(Workflow workflow, WorkflowState previousState, String changedBy) {
        logger.info("Handling workflow state change for material: {} from {} to {}", 
                   workflow.getMaterialCode(), previousState, workflow.getState());
        
        try {
            // Send general state change notification
            notificationService.notifyWorkflowStateChanged(workflow, previousState, changedBy);
            
            // Send specific notifications based on the new state
            switch (workflow.getState()) {
                case PLANT_PENDING:
                    if (previousState == WorkflowState.JVC_PENDING) {
                        notificationService.notifyWorkflowExtended(workflow, changedBy);
                    }
                    break;
                case COMPLETED:
                    notificationService.notifyWorkflowCompleted(workflow, changedBy);
                    break;
                default:
                    // General state change notification already sent
                    break;
            }
        } catch (Exception e) {
            logger.error("Failed to send workflow state change notification for material {}: {}", 
                        workflow.getMaterialCode(), e.getMessage(), e);
        }
    }
    
    /**
     * Handle query creation events
     */
    public void handleQueryCreated(Query query) {
        logger.info("Handling query created event for query ID: {} on material: {}", 
                   query.getId(), query.getWorkflow().getMaterialCode());
        
        try {
            // Notify the assigned team about the new query
            notificationService.notifyQueryRaised(query);
            
            // Also send assignment notification
            notificationService.notifyQueryAssigned(query, query.getRaisedBy());
        } catch (Exception e) {
            logger.error("Failed to send query created notification for query {}: {}", 
                        query.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Handle query resolution events
     */
    public void handleQueryResolved(Query query) {
        logger.info("Handling query resolved event for query ID: {} on material: {}", 
                   query.getId(), query.getWorkflow().getMaterialCode());
        
        try {
            notificationService.notifyQueryResolved(query);
        } catch (Exception e) {
            logger.error("Failed to send query resolved notification for query {}: {}", 
                        query.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Handle query assignment events
     */
    public void handleQueryAssigned(Query query, String assignedBy) {
        logger.info("Handling query assignment event for query ID: {} assigned to team: {}", 
                   query.getId(), query.getAssignedTeam());
        
        try {
            notificationService.notifyQueryAssigned(query, assignedBy);
        } catch (Exception e) {
            logger.error("Failed to send query assignment notification for query {}: {}", 
                        query.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Handle overdue workflow events (called by scheduler)
     */
    public void handleWorkflowOverdue(Workflow workflow) {
        logger.info("Handling overdue workflow event for material: {} ({} days pending)", 
                   workflow.getMaterialCode(), workflow.getDaysPending());
        
        try {
            notificationService.notifyWorkflowOverdue(workflow);
        } catch (Exception e) {
            logger.error("Failed to send overdue workflow notification for material {}: {}", 
                        workflow.getMaterialCode(), e.getMessage(), e);
        }
    }
    
    /**
     * Handle overdue query events (called by scheduler)
     */
    public void handleQueryOverdue(Query query) {
        logger.info("Handling overdue query event for query ID: {} ({} days open)", 
                   query.getId(), query.getDaysOpen());
        
        try {
            notificationService.notifyQueryOverdue(query);
        } catch (Exception e) {
            logger.error("Failed to send overdue query notification for query {}: {}", 
                        query.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Validate notification integration health
     */
    public boolean validateNotificationIntegration() {
        try {
            boolean emailEnabled = notificationService.isEmailEnabled();
            boolean slackEnabled = notificationService.isSlackEnabled();
            boolean notificationsEnabled = notificationService.isNotificationEnabled();
            
            logger.info("Notification integration status - Enabled: {}, Email: {}, Slack: {}", 
                       notificationsEnabled, emailEnabled, slackEnabled);
            
            return notificationsEnabled && (emailEnabled || slackEnabled);
        } catch (Exception e) {
            logger.error("Failed to validate notification integration: {}", e.getMessage(), e);
            return false;
        }
    }
}