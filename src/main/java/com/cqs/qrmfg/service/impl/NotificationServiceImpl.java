package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.config.NotificationConfig;
import com.cqs.qrmfg.dto.NotificationRequest;
import com.cqs.qrmfg.dto.NotificationResult;
import com.cqs.qrmfg.model.Workflow;
import com.cqs.qrmfg.config.NotificationWebSocketHandler;
import com.cqs.qrmfg.model.NotificationPreference;
import com.cqs.qrmfg.model.Query;
import com.cqs.qrmfg.model.WorkflowState;
import static com.cqs.qrmfg.model.WorkflowState.*;
import com.cqs.qrmfg.repository.NotificationPreferenceRepository;
import com.cqs.qrmfg.service.NotificationService;
import com.cqs.qrmfg.service.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);
    
    @Autowired
    private NotificationConfig notificationConfig;
    
    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    @Autowired(required = false)
    private TemplateEngine templateEngine;
    
    @Autowired
    private NotificationPreferenceRepository preferenceRepository;
    
    @Autowired
    private NotificationWebSocketHandler webSocketHandler;
    
    @Autowired
    private MetricsService metricsService;
    
    // In-memory storage for failed notifications (in production, use database or Redis)
    private final Map<String, NotificationRequest> failedNotifications = new ConcurrentHashMap<>();
    
    @Override
    public NotificationResult sendNotification(NotificationRequest request) {
        if (!notificationConfig.isEnabled()) {
            logger.debug("Notifications are disabled");
            return NotificationResult.failure("Notifications are disabled");
        }
        
        try {
            switch (request.getType().toUpperCase()) {
                case "EMAIL":
                    return sendEmailNotification(request);
                case "SLACK":
                    return sendSlackNotification(request);
                default:
                    return NotificationResult.failure("Unsupported notification type: " + request.getType());
            }
        } catch (Exception e) {
            logger.error("Failed to send notification: {}", e.getMessage(), e);
            failedNotifications.put(UUID.randomUUID().toString(), request);
            return NotificationResult.failure("Failed to send notification: " + e.getMessage());
        }
    }
    
    @Override
    @Async
    public CompletableFuture<NotificationResult> sendNotificationAsync(NotificationRequest request) {
        return CompletableFuture.completedFuture(sendNotification(request));
    }
    
    @Override
    public List<NotificationResult> sendBulkNotifications(List<NotificationRequest> requests) {
        return requests.stream()
                .map(this::sendNotification)
                .collect(Collectors.toList());
    }
    
    @Override
    public NotificationResult sendEmail(List<String> recipients, String subject, String message) {
        NotificationRequest request = new NotificationRequest("EMAIL", recipients, subject, message);
        return sendNotification(request);
    }
    
    @Override
    public NotificationResult sendEmailWithTemplate(List<String> recipients, String templateName, Object templateData) {
        NotificationRequest request = new NotificationRequest("EMAIL", recipients, templateName, 
                templateData instanceof Map ? (Map<String, Object>) templateData : 
                Collections.singletonMap("data", templateData));
        return sendNotification(request);
    }
    
    @Override
    @Async
    public CompletableFuture<NotificationResult> sendEmailAsync(List<String> recipients, String subject, String message) {
        return CompletableFuture.completedFuture(sendEmail(recipients, subject, message));
    }
    
    @Override
    public NotificationResult sendSlackMessage(String channel, String message) {
        NotificationRequest request = new NotificationRequest("SLACK", Collections.emptyList(), null, message);
        request.setChannel(channel);
        return sendNotification(request);
    }
    
    @Override
    public NotificationResult sendSlackMessage(List<String> users, String message) {
        NotificationRequest request = new NotificationRequest("SLACK", users, null, message);
        return sendNotification(request);
    }
    
    @Override
    @Async
    public CompletableFuture<NotificationResult> sendSlackMessageAsync(String channel, String message) {
        return CompletableFuture.completedFuture(sendSlackMessage(channel, message));
    }
    
    // Workflow-specific notification methods
    @Override
    public void notifyWorkflowCreated(Workflow workflow) {
        Map<String, Object> data = new HashMap<>();
        data.put("workflow", workflow);
        data.put("materialCode", workflow.getMaterialCode());
        data.put("materialName", workflow.getMaterialName());
        data.put("assignedPlant", workflow.getAssignedPlant());
        data.put("initiatedBy", workflow.getInitiatedBy());
        
        // Send real-time notification to JVC team
        sendRealTimeNotificationToTeam("TEAM_JVC", "workflow_created", 
                "New Workflow Created", 
                String.format("New MSDS workflow created for material %s", workflow.getMaterialCode()),
                workflow);
        
        // Notify JVC team with template
        List<NotificationPreference> jvcPreferences = preferenceRepository.findActivePreferencesForType("TEAM_JVC");
        for (NotificationPreference pref : jvcPreferences) {
            if ("EMAIL".equalsIgnoreCase(pref.getChannel())) {
                NotificationRequest request = new NotificationRequest();
                request.setType("EMAIL");
                request.setRecipients(Collections.singletonList(getRecipientAddress(pref.getUsername(), pref)));
                request.setSubject("New MSDS Workflow Created - " + workflow.getMaterialCode());
                request.setTemplateName("notifications/workflow-created");
                request.setTemplateData(data);
                sendNotificationAsync(request);
            }
        }
        
        // Notify assigned plant with template
        if (workflow.getAssignedPlant() != null) {
            // Send real-time notification to plant team
            sendRealTimeNotificationToTeam("TEAM_PLANT_" + workflow.getAssignedPlant(), "workflow_created", 
                    "New Material Assignment", 
                    String.format("Material %s has been assigned to your plant for MSDS workflow", workflow.getMaterialCode()),
                    workflow);
            
            List<NotificationPreference> plantPreferences = preferenceRepository.findActivePreferencesForType("TEAM_PLANT_" + workflow.getAssignedPlant());
            for (NotificationPreference pref : plantPreferences) {
                if ("EMAIL".equalsIgnoreCase(pref.getChannel())) {
                    NotificationRequest request = new NotificationRequest();
                    request.setType("EMAIL");
                    request.setRecipients(Collections.singletonList(getRecipientAddress(pref.getUsername(), pref)));
                    request.setSubject("New Material Assignment - " + workflow.getMaterialCode());
                    request.setTemplateName("notifications/workflow-created");
                    request.setTemplateData(data);
                    sendNotificationAsync(request);
                }
            }
        }
    }
    
    @Override
    public void notifyWorkflowExtended(Workflow workflow, String extendedBy) {
        Map<String, Object> data = new HashMap<>();
        data.put("workflow", workflow);
        data.put("extendedBy", extendedBy);
        
        // Send real-time notification to plant team
        sendRealTimeNotificationToTeam("TEAM_PLANT_" + workflow.getAssignedPlant(), "workflow_extended", 
                "Workflow Extended to Plant", 
                String.format("Material %s workflow has been extended to your plant. Please complete the questionnaire.", workflow.getMaterialCode()),
                workflow);
        
        // Notify plant team with template
        List<NotificationPreference> plantPreferences = preferenceRepository.findActivePreferencesForType("TEAM_PLANT_" + workflow.getAssignedPlant());
        for (NotificationPreference pref : plantPreferences) {
            if ("EMAIL".equalsIgnoreCase(pref.getChannel())) {
                NotificationRequest request = new NotificationRequest();
                request.setType("EMAIL");
                request.setRecipients(Collections.singletonList(getRecipientAddress(pref.getUsername(), pref)));
                request.setSubject("Workflow Extended to Plant - " + workflow.getMaterialCode());
                request.setTemplateName("notifications/workflow-extended");
                request.setTemplateData(data);
                sendNotificationAsync(request);
            }
        }
    }
    
    @Override
    public void notifyWorkflowCompleted(Workflow workflow, String completedBy) {
        Map<String, Object> data = new HashMap<>();
        data.put("workflow", workflow);
        data.put("completedBy", completedBy);
        
        // Notify workflow initiator
        List<NotificationPreference> initiatorPrefs = preferenceRepository.findActivePreferencesForUser(workflow.getInitiatedBy());
        for (NotificationPreference pref : initiatorPrefs) {
            if ("EMAIL".equalsIgnoreCase(pref.getChannel())) {
                NotificationRequest request = new NotificationRequest();
                request.setType("EMAIL");
                request.setRecipients(Collections.singletonList(getRecipientAddress(pref.getUsername(), pref)));
                request.setSubject("Workflow Completed - " + workflow.getMaterialCode());
                request.setTemplateName("notifications/workflow-completed");
                request.setTemplateData(data);
                sendNotificationAsync(request);
            }
        }
        
        // Notify plant team
        List<NotificationPreference> plantPreferences = preferenceRepository.findActivePreferencesForType("TEAM_PLANT_" + workflow.getAssignedPlant());
        for (NotificationPreference pref : plantPreferences) {
            if ("EMAIL".equalsIgnoreCase(pref.getChannel())) {
                NotificationRequest request = new NotificationRequest();
                request.setType("EMAIL");
                request.setRecipients(Collections.singletonList(getRecipientAddress(pref.getUsername(), pref)));
                request.setSubject("Workflow Completed - " + workflow.getMaterialCode());
                request.setTemplateName("notifications/workflow-completed");
                request.setTemplateData(data);
                sendNotificationAsync(request);
            }
        }
    }
    
    @Override
    public void notifyWorkflowStateChanged(Workflow workflow, WorkflowState previousState, String changedBy) {
        Map<String, Object> data = new HashMap<>();
        data.put("workflow", workflow);
        data.put("previousState", previousState);
        data.put("currentState", workflow.getState());
        data.put("changedBy", changedBy);
        
        // Notify relevant teams based on new state with template
        switch (workflow.getState()) {
            case PLANT_PENDING:
                List<NotificationPreference> plantPrefs = preferenceRepository.findActivePreferencesForType("TEAM_PLANT_" + workflow.getAssignedPlant());
                for (NotificationPreference pref : plantPrefs) {
                    if ("EMAIL".equalsIgnoreCase(pref.getChannel())) {
                        NotificationRequest request = new NotificationRequest();
                        request.setType("EMAIL");
                        request.setRecipients(Collections.singletonList(getRecipientAddress(pref.getUsername(), pref)));
                        request.setSubject("Action Required - " + workflow.getMaterialCode());
                        request.setTemplateName("notifications/workflow-state-changed");
                        request.setTemplateData(data);
                        sendNotificationAsync(request);
                    }
                }
                break;
            case CQS_PENDING:
                List<NotificationPreference> cqsPrefs = preferenceRepository.findActivePreferencesForType("TEAM_CQS");
                for (NotificationPreference pref : cqsPrefs) {
                    if ("EMAIL".equalsIgnoreCase(pref.getChannel())) {
                        NotificationRequest request = new NotificationRequest();
                        request.setType("EMAIL");
                        request.setRecipients(Collections.singletonList(getRecipientAddress(pref.getUsername(), pref)));
                        request.setSubject("Query Resolution Required - " + workflow.getMaterialCode());
                        request.setTemplateName("notifications/workflow-state-changed");
                        request.setTemplateData(data);
                        sendNotificationAsync(request);
                    }
                }
                break;
            case TECH_PENDING:
                List<NotificationPreference> techPrefs = preferenceRepository.findActivePreferencesForType("TEAM_TECH");
                for (NotificationPreference pref : techPrefs) {
                    if ("EMAIL".equalsIgnoreCase(pref.getChannel())) {
                        NotificationRequest request = new NotificationRequest();
                        request.setType("EMAIL");
                        request.setRecipients(Collections.singletonList(getRecipientAddress(pref.getUsername(), pref)));
                        request.setSubject("Query Resolution Required - " + workflow.getMaterialCode());
                        request.setTemplateName("notifications/workflow-state-changed");
                        request.setTemplateData(data);
                        sendNotificationAsync(request);
                    }
                }
                break;
            case COMPLETED:
                // Completion notifications are handled separately
                break;
        }
    }
    
    @Override
    public void notifyWorkflowOverdue(Workflow workflow) {
        Map<String, Object> data = new HashMap<>();
        data.put("workflow", workflow);
        
        // Notify based on current state with template
        String teamType = "";
        switch (workflow.getState()) {
            case JVC_PENDING:
                teamType = "TEAM_JVC";
                break;
            case PLANT_PENDING:
                teamType = "TEAM_PLANT_" + workflow.getAssignedPlant();
                break;
            case CQS_PENDING:
                teamType = "TEAM_CQS";
                break;
            case TECH_PENDING:
                teamType = "TEAM_TECH";
                break;
        }
        
        if (!teamType.isEmpty()) {
            List<NotificationPreference> teamPrefs = preferenceRepository.findActivePreferencesForType(teamType);
            for (NotificationPreference pref : teamPrefs) {
                if ("EMAIL".equalsIgnoreCase(pref.getChannel())) {
                    NotificationRequest request = new NotificationRequest();
                    request.setType("EMAIL");
                    request.setRecipients(Collections.singletonList(getRecipientAddress(pref.getUsername(), pref)));
                    request.setSubject("URGENT: Overdue Workflow - " + workflow.getMaterialCode());
                    request.setTemplateName("notifications/workflow-overdue");
                    request.setTemplateData(data);
                    sendNotificationAsync(request);
                }
            }
        }
        
        // Always notify admins for overdue workflows
        List<NotificationPreference> adminPrefs = preferenceRepository.findActivePreferencesForType("TEAM_ADMIN");
        for (NotificationPreference pref : adminPrefs) {
            if ("EMAIL".equalsIgnoreCase(pref.getChannel())) {
                NotificationRequest request = new NotificationRequest();
                request.setType("EMAIL");
                request.setRecipients(Collections.singletonList(getRecipientAddress(pref.getUsername(), pref)));
                request.setSubject("URGENT: Overdue Workflow Alert - " + workflow.getMaterialCode());
                request.setTemplateName("notifications/workflow-overdue");
                request.setTemplateData(data);
                sendNotificationAsync(request);
            }
        }
    }
    
    // Query-specific notification methods
    @Override
    public void notifyQueryRaised(Query query) {
        Map<String, Object> data = new HashMap<>();
        data.put("query", query);
        
        // Notify assigned team with template
        String teamType = "TEAM_" + query.getAssignedTeam().name();
        List<NotificationPreference> teamPrefs = preferenceRepository.findActivePreferencesForType(teamType);
        for (NotificationPreference pref : teamPrefs) {
            if ("EMAIL".equalsIgnoreCase(pref.getChannel())) {
                NotificationRequest request = new NotificationRequest();
                request.setType("EMAIL");
                request.setRecipients(Collections.singletonList(getRecipientAddress(pref.getUsername(), pref)));
                request.setSubject("New Query Assigned - " + query.getWorkflow().getMaterialCode());
                request.setTemplateName("notifications/query-raised");
                request.setTemplateData(data);
                sendNotificationAsync(request);
            }
        }
        
        // Notify query raiser with template
        List<NotificationPreference> raiserPrefs = preferenceRepository.findActivePreferencesForUser(query.getRaisedBy());
        for (NotificationPreference pref : raiserPrefs) {
            if ("EMAIL".equalsIgnoreCase(pref.getChannel())) {
                NotificationRequest request = new NotificationRequest();
                request.setType("EMAIL");
                request.setRecipients(Collections.singletonList(getRecipientAddress(pref.getUsername(), pref)));
                request.setSubject("Query Submitted - " + query.getWorkflow().getMaterialCode());
                request.setTemplateName("notifications/query-raised");
                request.setTemplateData(data);
                sendNotificationAsync(request);
            }
        }
    }
    
    @Override
    public void notifyQueryResolved(Query query) {
        Map<String, Object> data = new HashMap<>();
        data.put("query", query);
        
        // Notify query raiser with template
        List<NotificationPreference> raiserPrefs = preferenceRepository.findActivePreferencesForUser(query.getRaisedBy());
        for (NotificationPreference pref : raiserPrefs) {
            if ("EMAIL".equalsIgnoreCase(pref.getChannel())) {
                NotificationRequest request = new NotificationRequest();
                request.setType("EMAIL");
                request.setRecipients(Collections.singletonList(getRecipientAddress(pref.getUsername(), pref)));
                request.setSubject("Query Resolved - " + query.getWorkflow().getMaterialCode());
                request.setTemplateName("notifications/query-resolved");
                request.setTemplateData(data);
                sendNotificationAsync(request);
            }
        }
        
        // Notify plant team with template
        String plantTeamType = "TEAM_PLANT_" + query.getWorkflow().getAssignedPlant();
        List<NotificationPreference> plantPrefs = preferenceRepository.findActivePreferencesForType(plantTeamType);
        for (NotificationPreference pref : plantPrefs) {
            if ("EMAIL".equalsIgnoreCase(pref.getChannel())) {
                NotificationRequest request = new NotificationRequest();
                request.setType("EMAIL");
                request.setRecipients(Collections.singletonList(getRecipientAddress(pref.getUsername(), pref)));
                request.setSubject("Query Resolved - " + query.getWorkflow().getMaterialCode());
                request.setTemplateName("notifications/query-resolved");
                request.setTemplateData(data);
                sendNotificationAsync(request);
            }
        }
    }
    
    @Override
    public void notifyQueryAssigned(Query query, String assignedBy) {
        Map<String, Object> data = new HashMap<>();
        data.put("query", query);
        data.put("assignedBy", assignedBy);
        
        // Notify assigned team with template
        String teamType = "TEAM_" + query.getAssignedTeam().name();
        List<NotificationPreference> teamPrefs = preferenceRepository.findActivePreferencesForType(teamType);
        for (NotificationPreference pref : teamPrefs) {
            if ("EMAIL".equalsIgnoreCase(pref.getChannel())) {
                NotificationRequest request = new NotificationRequest();
                request.setType("EMAIL");
                request.setRecipients(Collections.singletonList(getRecipientAddress(pref.getUsername(), pref)));
                request.setSubject("Query Assigned - " + query.getWorkflow().getMaterialCode());
                request.setTemplateName("notifications/query-assigned");
                request.setTemplateData(data);
                sendNotificationAsync(request);
            }
        }
    }
    
    @Override
    public void notifyQueryOverdue(Query query) {
        Map<String, Object> data = new HashMap<>();
        data.put("query", query);
        
        // Notify assigned team with template
        String teamType = "TEAM_" + query.getAssignedTeam().name();
        List<NotificationPreference> teamPrefs = preferenceRepository.findActivePreferencesForType(teamType);
        for (NotificationPreference pref : teamPrefs) {
            if ("EMAIL".equalsIgnoreCase(pref.getChannel())) {
                NotificationRequest request = new NotificationRequest();
                request.setType("EMAIL");
                request.setRecipients(Collections.singletonList(getRecipientAddress(pref.getUsername(), pref)));
                request.setSubject("URGENT: Overdue Query - " + query.getWorkflow().getMaterialCode());
                request.setTemplateName("notifications/query-overdue");
                request.setTemplateData(data);
                sendNotificationAsync(request);
            }
        }
        
        // Notify admins with template
        List<NotificationPreference> adminPrefs = preferenceRepository.findActivePreferencesForType("TEAM_ADMIN");
        for (NotificationPreference pref : adminPrefs) {
            if ("EMAIL".equalsIgnoreCase(pref.getChannel())) {
                NotificationRequest request = new NotificationRequest();
                request.setType("EMAIL");
                request.setRecipients(Collections.singletonList(getRecipientAddress(pref.getUsername(), pref)));
                request.setSubject("URGENT: Overdue Query Alert - " + query.getWorkflow().getMaterialCode());
                request.setTemplateName("notifications/query-overdue");
                request.setTemplateData(data);
                sendNotificationAsync(request);
            }
        }
    }
    
    // User and team notification methods
    @Override
    public void notifyUser(String username, String subject, String message) {
        List<NotificationPreference> preferences = preferenceRepository.findActivePreferencesForUser(username);
        
        for (NotificationPreference pref : preferences) {
            NotificationRequest request = new NotificationRequest();
            request.setType(pref.getChannel());
            request.setRecipients(Collections.singletonList(getRecipientAddress(username, pref)));
            request.setSubject(subject);
            request.setMessage(message);
            
            sendNotificationAsync(request);
        }
    }
    
    @Override
    public void notifyTeam(String teamName, String subject, String message) {
        // Get team members from preferences
        List<NotificationPreference> teamPreferences = preferenceRepository.findActivePreferencesForType("TEAM_" + teamName);
        
        Map<String, List<String>> recipientsByChannel = teamPreferences.stream()
                .collect(Collectors.groupingBy(
                        NotificationPreference::getChannel,
                        Collectors.mapping(pref -> getRecipientAddress(pref.getUsername(), pref), Collectors.toList())
                ));
        
        for (Map.Entry<String, List<String>> entry : recipientsByChannel.entrySet()) {
            NotificationRequest request = new NotificationRequest(entry.getKey(), entry.getValue(), subject, message);
            sendNotificationAsync(request);
        }
    }
    
    @Override
    public void notifyPlant(String plantName, String subject, String message) {
        notifyTeam("PLANT_" + plantName, subject, message);
    }
    
    @Override
    public void notifyAdmins(String subject, String message) {
        notifyTeam("ADMIN", subject, message);
    }
    
    // Template management
    @Override
    public String renderTemplate(String templateName, Object data) {
        if (templateEngine == null) {
            logger.warn("Template engine not available, returning empty string");
            return "";
        }
        
        try {
            Context context = new Context();
            if (data instanceof Map) {
                context.setVariables((Map<String, Object>) data);
            } else {
                context.setVariable("data", data);
            }
            
            return templateEngine.process(templateName, context);
        } catch (Exception e) {
            logger.error("Failed to render template {}: {}", templateName, e.getMessage());
            return "";
        }
    }
    
    @Override
    public boolean isTemplateAvailable(String templateName) {
        // Simple check - in production, you might want to check if template file exists
        return templateEngine != null && templateName != null && !templateName.trim().isEmpty();
    }
    
    // Configuration and preferences
    @Override
    public boolean isNotificationEnabled() {
        return notificationConfig.isEnabled();
    }
    
    @Override
    public boolean isEmailEnabled() {
        return notificationConfig.isEnabled() && notificationConfig.getEmail().isEnabled() && mailSender != null;
    }
    
    @Override
    public boolean isSlackEnabled() {
        return notificationConfig.isEnabled() && notificationConfig.getSlack().isEnabled();
    }
    
    @Override
    public void updateNotificationPreferences(String username, String preferences) {
        // Implementation would parse preferences and update database
        logger.info("Updating notification preferences for user: {}", username);
    }
    
    // Retry and error handling
    @Override
    public NotificationResult retryFailedNotification(NotificationRequest request, int maxAttempts) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Thread.sleep(notificationConfig.getRetry().getDelayMillis() * attempt);
                NotificationResult result = sendNotification(request);
                if (result.isSuccess()) {
                    return result;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.warn("Retry attempt {} failed for notification: {}", attempt, e.getMessage());
            }
        }
        return NotificationResult.failure("All retry attempts failed");
    }
    
    @Override
    public List<NotificationResult> getFailedNotifications() {
        return failedNotifications.values().stream()
                .map(req -> NotificationResult.failure("Previously failed notification"))
                .collect(Collectors.toList());
    }
    
    @Override
    public void clearFailedNotifications() {
        failedNotifications.clear();
    }
    
    // Private helper methods
    private void sendRealTimeNotification(String username, String type, String title, String message, Object data) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", type);
            notification.put("title", title);
            notification.put("message", message);
            notification.put("timestamp", LocalDateTime.now().toString());
            notification.put("data", data);
            
            webSocketHandler.sendNotificationToUser(username, notification);
        } catch (Exception e) {
            logger.warn("Failed to send real-time notification to user {}: {}", username, e.getMessage());
        }
    }
    
    private void sendRealTimeNotificationToTeam(String teamType, String type, String title, String message, Object data) {
        try {
            List<NotificationPreference> teamPreferences = preferenceRepository.findActivePreferencesForType(teamType);
            List<String> usernames = teamPreferences.stream()
                    .map(NotificationPreference::getUsername)
                    .distinct()
                    .collect(Collectors.toList());
            
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", type);
            notification.put("title", title);
            notification.put("message", message);
            notification.put("timestamp", LocalDateTime.now().toString());
            notification.put("data", data);
            
            webSocketHandler.sendNotificationToUsers(usernames, notification);
        } catch (Exception e) {
            logger.warn("Failed to send real-time notification to team {}: {}", teamType, e.getMessage());
        }
    }
    
    private NotificationResult sendEmailNotification(NotificationRequest request) {
        if (!isEmailEnabled()) {
            return NotificationResult.failure("Email notifications are disabled");
        }
        
        try {
            String content = request.getMessage();
            if (request.getTemplateName() != null && isTemplateAvailable(request.getTemplateName())) {
                content = renderTemplate(request.getTemplateName(), request.getTemplateData());
            }
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(notificationConfig.getEmail().getFrom());
            message.setTo(request.getRecipients().toArray(new String[0]));
            message.setSubject(request.getSubject());
            message.setText(content);
            message.setSentDate(new Date());
            
            mailSender.send(message);
            
            logger.info("Email sent successfully to {} recipients", request.getRecipients().size());
            return NotificationResult.success("Email sent successfully", request.getRecipients());
            
        } catch (Exception e) {
            logger.error("Failed to send email: {}", e.getMessage(), e);
            return NotificationResult.failure("Failed to send email: " + e.getMessage(), request.getRecipients());
        }
    }
    
    private NotificationResult sendSlackNotification(NotificationRequest request) {
        if (!isSlackEnabled()) {
            return NotificationResult.failure("Slack notifications are disabled");
        }
        
        // Placeholder implementation - in production, integrate with Slack API
        logger.info("Slack notification would be sent: {}", request.getMessage());
        return NotificationResult.success("Slack notification sent (simulated)");
    }
    
    private String getRecipientAddress(String username, NotificationPreference preference) {
        switch (preference.getChannel().toUpperCase()) {
            case "EMAIL":
                return preference.getEmail() != null ? preference.getEmail() : username + "@company.com";
            case "SLACK":
                return preference.getSlackId() != null ? preference.getSlackId() : username;
            default:
                return username;
        }
    }
}