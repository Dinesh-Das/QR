package com.cqs.qrmfg.service;

import com.cqs.qrmfg.dto.NotificationRequest;
import com.cqs.qrmfg.dto.NotificationResult;
import com.cqs.qrmfg.model.Workflow;
import com.cqs.qrmfg.model.Query;
import com.cqs.qrmfg.model.WorkflowState;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface NotificationService {
    
    // Core notification methods
    NotificationResult sendNotification(NotificationRequest request);
    CompletableFuture<NotificationResult> sendNotificationAsync(NotificationRequest request);
    List<NotificationResult> sendBulkNotifications(List<NotificationRequest> requests);
    
    // Email-specific methods
    NotificationResult sendEmail(List<String> recipients, String subject, String message);
    NotificationResult sendEmailWithTemplate(List<String> recipients, String templateName, Object templateData);
    CompletableFuture<NotificationResult> sendEmailAsync(List<String> recipients, String subject, String message);
    
    // Slack-specific methods
    NotificationResult sendSlackMessage(String channel, String message);
    NotificationResult sendSlackMessage(List<String> users, String message);
    CompletableFuture<NotificationResult> sendSlackMessageAsync(String channel, String message);
    
    // Workflow-specific notification methods
    void notifyWorkflowCreated(Workflow workflow);
    void notifyWorkflowExtended(Workflow workflow, String extendedBy);
    void notifyWorkflowCompleted(Workflow workflow, String completedBy);
    void notifyWorkflowStateChanged(Workflow workflow, WorkflowState previousState, String changedBy);
    void notifyWorkflowOverdue(Workflow workflow);
    
    // Query-specific notification methods
    void notifyQueryRaised(Query query);
    void notifyQueryResolved(Query query);
    void notifyQueryAssigned(Query query, String assignedBy);
    void notifyQueryOverdue(Query query);
    
    // User and team notification methods
    void notifyUser(String username, String subject, String message);
    void notifyTeam(String teamName, String subject, String message);
    void notifyPlant(String plantName, String subject, String message);
    void notifyAdmins(String subject, String message);
    
    // Template management
    String renderTemplate(String templateName, Object data);
    boolean isTemplateAvailable(String templateName);
    
    // Configuration and preferences
    boolean isNotificationEnabled();
    boolean isEmailEnabled();
    boolean isSlackEnabled();
    void updateNotificationPreferences(String username, String preferences);
    
    // Retry and error handling
    NotificationResult retryFailedNotification(NotificationRequest request, int maxAttempts);
    List<NotificationResult> getFailedNotifications();
    void clearFailedNotifications();
}