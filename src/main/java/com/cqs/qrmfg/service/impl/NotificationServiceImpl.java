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
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.internet.MimeMessage;

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
    
    @Autowired
    private com.cqs.qrmfg.repository.UserRepository userRepository;
    
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
    

    
    // Workflow-specific notification methods
    @Override
    @Async("notificationTaskExecutor")
    public void notifyWorkflowCreated(Workflow workflow) {
        Map<String, Object> data = new HashMap<>();
        data.put("workflow", workflow);
        data.put("materialCode", workflow.getMaterialCode());
        data.put("materialName", workflow.getMaterialName());
        data.put("assignedPlant", workflow.getAssignedPlant());
        data.put("initiatedBy", workflow.getInitiatedBy());
        
        // BUSINESS RULE: When JVC initiates workflow → Notify CQS Role team
        logger.info("Workflow created by JVC, notifying CQS role team for material: {}", workflow.getMaterialCode());
        
        // Send real-time notification to CQS team
        sendRealTimeNotificationToTeam("ROLE_CQS", "workflow_created", 
                "New Workflow Created - CQS Review Required", 
                String.format("New MSDS workflow created for material %s - CQS review required", workflow.getMaterialCode()),
                workflow);
        
        // Check for role-based CQS preferences
        List<NotificationPreference> cqsPreferences = preferenceRepository.findActivePreferencesForType("ROLE_CQS");
        logger.info("Found {} ROLE_CQS preferences", cqsPreferences.size());
        
        // Check for user-based workflow created preferences
        List<NotificationPreference> workflowCreatedPreferences = preferenceRepository.findActivePreferencesForType("WORKFLOW_CREATED");
        logger.info("Found {} WORKFLOW_CREATED preferences", workflowCreatedPreferences.size());
        
        // Log all preferences for debugging
        for (NotificationPreference pref : workflowCreatedPreferences) {
            logger.info("WORKFLOW_CREATED preference: user={}, email={}, enabled={}", 
                       pref.getUsername(), pref.getEmail(), pref.getEnabled());
        }
        
        // Combine both lists
        List<NotificationPreference> allPreferences = new ArrayList<>();
        allPreferences.addAll(cqsPreferences);
        allPreferences.addAll(workflowCreatedPreferences);
        
        logger.info("Total preferences for workflow created: {}", allPreferences.size());
        
        for (NotificationPreference pref : allPreferences) {
            if ("EMAIL".equalsIgnoreCase(pref.getChannel())) {
                logger.info("Sending workflow created notification to: {} ({})", pref.getUsername(), pref.getEmail());
                
                NotificationRequest request = new NotificationRequest();
                request.setType("EMAIL");
                request.setRecipients(Collections.singletonList(getRecipientAddress(pref.getUsername(), pref)));
                request.setSubject("New MSDS Workflow Created - CQS Review Required - " + workflow.getMaterialCode());
                request.setTemplateName("notifications/workflow-created");
                request.setTemplateData(data);
                
                // Send notification asynchronously to avoid blocking
                sendNotificationAsync(request).thenAccept(result -> {
                    if (result.isSuccess()) {
                        logger.info("Successfully sent workflow created notification to: {}", pref.getEmail());
                    } else {
                        logger.error("Failed to send workflow created notification to: {} - {}", pref.getEmail(), result.getMessage());
                    }
                }).exceptionally(throwable -> {
                    logger.error("Exception sending workflow created notification to: {} - {}", pref.getEmail(), throwable.getMessage());
                    return null;
                });
            }
        }
        
        // If no preferences found, log warning
        if (allPreferences.isEmpty()) {
            logger.warn("No notification preferences found for workflow created notifications. Workflow: {}", workflow.getMaterialCode());
        } else {
            logger.info("Processed workflow created notifications for {} recipients", allPreferences.size());
        }
    }
    
    // Track recent notifications to prevent duplicates
    private final Map<String, Long> recentNotifications = new ConcurrentHashMap<>();
    private static final long DUPLICATE_PREVENTION_WINDOW = 30000; // 30 seconds
    
    @Override
    @Async("notificationTaskExecutor")
    public void notifyWorkflowExtended(Workflow workflow, String extendedBy) {
        // Create a unique key for this notification
        String notificationKey = "WORKFLOW_EXTENDED_" + workflow.getId() + "_" + workflow.getAssignedPlant();
        long currentTime = System.currentTimeMillis();
        
        // Check if we've sent this notification recently
        Long lastSent = recentNotifications.get(notificationKey);
        if (lastSent != null && (currentTime - lastSent) < DUPLICATE_PREVENTION_WINDOW) {
            logger.info("Skipping duplicate workflow extended notification for workflow {} to plant {} (sent {} ms ago)", 
                       workflow.getId(), workflow.getAssignedPlant(), currentTime - lastSent);
            return;
        }
        
        // Record this notification
        recentNotifications.put(notificationKey, currentTime);
        
        // Clean up old entries (older than 5 minutes)
        recentNotifications.entrySet().removeIf(entry -> (currentTime - entry.getValue()) > 300000);
        
        Map<String, Object> data = new HashMap<>();
        data.put("workflow", workflow);
        data.put("extendedBy", extendedBy);
        
        // BUSINESS RULE: When JVC sends workflow to plant → Notify Plant Users (users with that plant assigned)
        logger.info("Workflow extended to plant {}, notifying plant users for material: {} (workflow ID: {})", 
                   workflow.getAssignedPlant(), workflow.getMaterialCode(), workflow.getId());
        
        // Send real-time notification to plant users
        sendRealTimeNotificationToTeam("PLANT_" + workflow.getAssignedPlant(), "workflow_extended", 
                "Workflow Extended to Your Plant", 
                String.format("Material %s workflow has been extended to your plant. Please complete the questionnaire.", workflow.getMaterialCode()),
                workflow);
        
        // Notify users with the assigned plant
        List<NotificationPreference> plantUserPreferences = preferenceRepository.findActivePreferencesForType("PLANT_" + workflow.getAssignedPlant());
        logger.info("Found {} plant-specific preferences for plant {}", plantUserPreferences.size(), workflow.getAssignedPlant());
        
        // If no plant-specific preferences found, look for users who have this plant assigned
        if (plantUserPreferences.isEmpty()) {
            logger.info("No plant-specific preferences found, looking for users assigned to plant {}", workflow.getAssignedPlant());
            plantUserPreferences = findUsersWithPlantAssignment(workflow.getAssignedPlant());
            logger.info("Found {} users with plant {} assignment", plantUserPreferences.size(), workflow.getAssignedPlant());
        }
        
        // Remove duplicates based on username and email
        Map<String, NotificationPreference> uniquePreferences = new HashMap<>();
        for (NotificationPreference pref : plantUserPreferences) {
            String key = pref.getUsername() + "_" + pref.getEmail();
            uniquePreferences.put(key, pref);
        }
        
        logger.info("Sending workflow extended notifications to {} unique recipients", uniquePreferences.size());
        
        for (NotificationPreference pref : uniquePreferences.values()) {
            if ("EMAIL".equalsIgnoreCase(pref.getChannel())) {
                logger.info("Sending workflow extended notification to: {} ({})", pref.getUsername(), pref.getEmail());
                
                NotificationRequest request = new NotificationRequest();
                request.setType("EMAIL");
                request.setRecipients(Collections.singletonList(getRecipientAddress(pref.getUsername(), pref)));
                request.setSubject("Workflow Extended to Your Plant - " + workflow.getMaterialCode());
                request.setTemplateName("notifications/workflow-extended");
                request.setTemplateData(data);
                
                // Send notification asynchronously to avoid blocking
                sendNotificationAsync(request).thenAccept(result -> {
                    if (result.isSuccess()) {
                        logger.info("Successfully sent workflow extended notification to: {}", pref.getEmail());
                    } else {
                        logger.error("Failed to send workflow extended notification to: {} - {}", pref.getEmail(), result.getMessage());
                    }
                }).exceptionally(throwable -> {
                    logger.error("Exception sending workflow extended notification to: {} - {}", pref.getEmail(), throwable.getMessage());
                    return null;
                });
            }
        }
        
        // If still no plant user preferences found, log warning
        if (uniquePreferences.isEmpty()) {
            logger.warn("No notification preferences found for plant {} users. Workflow: {}", workflow.getAssignedPlant(), workflow.getMaterialCode());
        } else {
            logger.info("Processed workflow extended notifications for {} unique recipients", uniquePreferences.size());
        }
    }
    
    @Override
    @Async("notificationTaskExecutor")
    public void notifyWorkflowCompleted(Workflow workflow, String completedBy) {
        Map<String, Object> data = new HashMap<>();
        data.put("workflow", workflow);
        data.put("completedBy", completedBy);
        
        // BUSINESS RULE: When workflow is completed → Notify CQS Role + JVC Role
        logger.info("Workflow completed, notifying CQS and JVC roles for material: {}", workflow.getMaterialCode());
        
        // Notify users with workflow completed preferences
        List<NotificationPreference> workflowCompletedPreferences = preferenceRepository.findActivePreferencesForType("WORKFLOW_COMPLETED");
        
        // Also check for legacy role-based preferences
        List<NotificationPreference> cqsPreferences = preferenceRepository.findActivePreferencesForType("ROLE_CQS");
        List<NotificationPreference> jvcPreferences = preferenceRepository.findActivePreferencesForType("ROLE_JVC");
        
        // Combine all preferences
        List<NotificationPreference> allPreferences = new ArrayList<>();
        allPreferences.addAll(workflowCompletedPreferences);
        allPreferences.addAll(cqsPreferences);
        allPreferences.addAll(jvcPreferences);
        
        for (NotificationPreference pref : allPreferences) {
            if ("EMAIL".equalsIgnoreCase(pref.getChannel())) {
                NotificationRequest request = new NotificationRequest();
                request.setType("EMAIL");
                request.setRecipients(Collections.singletonList(getRecipientAddress(pref.getUsername(), pref)));
                request.setSubject("Workflow Completed - " + workflow.getMaterialCode());
                request.setTemplateName("notifications/workflow-completed");
                request.setTemplateData(data);
                sendNotificationAsync(request);
                
                logger.info("Sent workflow completed notification to: {} ({})", pref.getUsername(), pref.getEmail());
            }
        }
        
        // Log if no preferences found
        if (allPreferences.isEmpty()) {
            logger.warn("No notification preferences found for workflow completed notifications. Workflow: {}", workflow.getMaterialCode());
        } else {
            logger.info("Sent workflow completed notifications to {} recipients", allPreferences.size());
        }
    }
    
    @Override
    @Async("notificationTaskExecutor")
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
    @Async("notificationTaskExecutor")
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
    @Async("notificationTaskExecutor")
    public void notifyQueryRaised(Query query) {
        // Create a unique key for this notification
        String notificationKey = "QUERY_RAISED_" + query.getId() + "_" + query.getWorkflow().getMaterialCode();
        long currentTime = System.currentTimeMillis();
        
        // Check if we've sent this notification recently
        Long lastSent = recentNotifications.get(notificationKey);
        if (lastSent != null && (currentTime - lastSent) < DUPLICATE_PREVENTION_WINDOW) {
            logger.info("Skipping duplicate query raised notification for query {} material {} (sent {} ms ago)", 
                       query.getId(), query.getWorkflow().getMaterialCode(), currentTime - lastSent);
            return;
        }
        
        // Record this notification
        recentNotifications.put(notificationKey, currentTime);
        
        // Clean up old entries (older than 5 minutes)
        recentNotifications.entrySet().removeIf(entry -> (currentTime - entry.getValue()) > 300000);
        
        logger.info("=== QUERY RAISED NOTIFICATION START ===");
        logger.info("Query ID: {}, Material: {}, Raised by: {}", 
                   query.getId(), query.getWorkflow().getMaterialCode(), query.getRaisedBy());
        
        Map<String, Object> data = new HashMap<>();
        data.put("query", query);
        
        // BUSINESS RULE: When plant raises query → Notify JVC/CQS/TECH Roles
        logger.info("Query raised by plant, notifying JVC/CQS/TECH roles for material: {}", query.getWorkflow().getMaterialCode());
        
        // Check for user-based query raised preferences
        List<NotificationPreference> queryRaisedPreferences = preferenceRepository.findActivePreferencesForType("QUERY_RAISED");
        logger.info("Found {} QUERY_RAISED preferences", queryRaisedPreferences.size());
        
        // Log each QUERY_RAISED preference
        for (NotificationPreference pref : queryRaisedPreferences) {
            logger.info("QUERY_RAISED preference: user={}, email={}, enabled={}", 
                       pref.getUsername(), pref.getEmail(), pref.getEnabled());
        }
        
        // Check for legacy role-based preferences
        List<NotificationPreference> jvcPrefs = preferenceRepository.findActivePreferencesForType("ROLE_JVC");
        List<NotificationPreference> cqsPrefs = preferenceRepository.findActivePreferencesForType("ROLE_CQS");
        List<NotificationPreference> techPrefs = preferenceRepository.findActivePreferencesForType("ROLE_TECH");
        
        logger.info("Found {} ROLE_JVC, {} ROLE_CQS, {} ROLE_TECH preferences", 
                   jvcPrefs.size(), cqsPrefs.size(), techPrefs.size());
        
        // Log each role-based preference
        for (NotificationPreference pref : cqsPrefs) {
            logger.info("ROLE_CQS preference: user={}, email={}, enabled={}", 
                       pref.getUsername(), pref.getEmail(), pref.getEnabled());
        }
        
        // Combine all preferences
        List<NotificationPreference> allPreferences = new ArrayList<>();
        allPreferences.addAll(queryRaisedPreferences);
        allPreferences.addAll(jvcPrefs);
        allPreferences.addAll(cqsPrefs);
        allPreferences.addAll(techPrefs);
        
        logger.info("Total combined preferences: {}", allPreferences.size());
        
        // Remove duplicates based on username and email (keep the first preference found)
        Map<String, NotificationPreference> uniquePreferences = new HashMap<>();
        for (NotificationPreference pref : allPreferences) {
            String key = pref.getUsername() + "_" + pref.getEmail();
            if (!uniquePreferences.containsKey(key)) {
                uniquePreferences.put(key, pref);
                logger.info("Added unique preference: {} -> {} (type: {})", key, pref.getEmail(), pref.getNotificationType());
            } else {
                logger.info("Skipping duplicate preference: {} -> {} (type: {})", key, pref.getEmail(), pref.getNotificationType());
            }
        }
        
        logger.info("Sending query raised notifications to {} unique recipients", uniquePreferences.size());
        
        for (NotificationPreference pref : uniquePreferences.values()) {
            if ("EMAIL".equalsIgnoreCase(pref.getChannel())) {
                // Additional per-recipient duplicate check
                String recipientKey = notificationKey + "_" + pref.getEmail();
                Long recipientLastSent = recentNotifications.get(recipientKey);
                if (recipientLastSent != null && (currentTime - recipientLastSent) < DUPLICATE_PREVENTION_WINDOW) {
                    logger.info("Skipping duplicate query raised notification to recipient {} (sent {} ms ago)", 
                               pref.getEmail(), currentTime - recipientLastSent);
                    continue;
                }
                
                logger.info("Attempting to send query raised notification to: {} ({})", pref.getUsername(), pref.getEmail());
                
                // Record this recipient notification
                recentNotifications.put(recipientKey, currentTime);
                
                NotificationRequest request = new NotificationRequest();
                request.setType("EMAIL");
                request.setRecipients(Collections.singletonList(getRecipientAddress(pref.getUsername(), pref)));
                request.setSubject("New Query from Plant - Review Required - " + query.getWorkflow().getMaterialCode());
                request.setTemplateName("notifications/query-raised");
                request.setTemplateData(data);
                
                // Send notification asynchronously to avoid blocking
                sendNotificationAsync(request).thenAccept(result -> {
                    if (result.isSuccess()) {
                        logger.info("✓ Successfully sent query raised notification to: {}", pref.getEmail());
                    } else {
                        logger.error("✗ Failed to send query raised notification to: {} - {}", pref.getEmail(), result.getMessage());
                    }
                }).exceptionally(throwable -> {
                    logger.error("✗ Exception sending query raised notification to: {} - {}", pref.getEmail(), throwable.getMessage());
                    return null;
                });
            } else {
                logger.info("Skipping non-email preference: {} (channel: {})", pref.getUsername(), pref.getChannel());
            }
        }
        
        // Log if no preferences found
        if (uniquePreferences.isEmpty()) {
            logger.warn("❌ No notification preferences found for query raised notifications. Query: {}", query.getId());
        } else {
            logger.info("✓ Processed query raised notifications for {} unique recipients", uniquePreferences.size());
        }
        
        logger.info("=== QUERY RAISED NOTIFICATION END ===");
    }
    
    @Override
    @Async("notificationTaskExecutor")
    public void notifyQueryResolved(Query query) {
        // Create a unique key for this notification
        String notificationKey = "QUERY_RESOLVED_" + query.getId() + "_" + query.getWorkflow().getMaterialCode();
        long currentTime = System.currentTimeMillis();
        
        // Check if we've sent this notification recently
        Long lastSent = recentNotifications.get(notificationKey);
        if (lastSent != null && (currentTime - lastSent) < DUPLICATE_PREVENTION_WINDOW) {
            logger.info("Skipping duplicate query resolved notification for query {} material {} (sent {} ms ago)", 
                       query.getId(), query.getWorkflow().getMaterialCode(), currentTime - lastSent);
            return;
        }
        
        // Record this notification
        recentNotifications.put(notificationKey, currentTime);
        
        // Clean up old entries (older than 5 minutes)
        recentNotifications.entrySet().removeIf(entry -> (currentTime - entry.getValue()) > 300000);
        
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
    @Async("notificationTaskExecutor")
    public void notifyQueryAssigned(Query query, String assignedBy) {
        Map<String, Object> data = new HashMap<>();
        data.put("query", query);
        data.put("assignedBy", assignedBy);
        
        logger.info("Query assigned, notifying users for material: {}", query.getWorkflow().getMaterialCode());
        
        // Check for user-based query assigned preferences
        List<NotificationPreference> queryAssignedPreferences = preferenceRepository.findActivePreferencesForType("QUERY_ASSIGNED");
        
        // Also check for legacy team-based preferences
        String teamType = "TEAM_" + query.getAssignedTeam().name();
        List<NotificationPreference> teamPrefs = preferenceRepository.findActivePreferencesForType(teamType);
        
        // Combine preferences
        List<NotificationPreference> allPreferences = new ArrayList<>();
        allPreferences.addAll(queryAssignedPreferences);
        allPreferences.addAll(teamPrefs);
        
        // Remove duplicates
        Map<String, NotificationPreference> uniquePreferences = new HashMap<>();
        for (NotificationPreference pref : allPreferences) {
            String key = pref.getUsername() + "_" + pref.getEmail();
            uniquePreferences.put(key, pref);
        }
        
        logger.info("Sending query assigned notifications to {} unique recipients", uniquePreferences.size());
        
        for (NotificationPreference pref : uniquePreferences.values()) {
            if ("EMAIL".equalsIgnoreCase(pref.getChannel())) {
                logger.info("Sending query assigned notification to: {} ({})", pref.getUsername(), pref.getEmail());
                
                NotificationRequest request = new NotificationRequest();
                request.setType("EMAIL");
                request.setRecipients(Collections.singletonList(getRecipientAddress(pref.getUsername(), pref)));
                request.setSubject("Query Assigned - " + query.getWorkflow().getMaterialCode());
                request.setTemplateName("notifications/query-assigned");
                request.setTemplateData(data);
                
                // Send notification asynchronously to avoid blocking
                sendNotificationAsync(request).thenAccept(result -> {
                    if (result.isSuccess()) {
                        logger.info("Successfully sent query assigned notification to: {}", pref.getEmail());
                    } else {
                        logger.error("Failed to send query assigned notification to: {} - {}", pref.getEmail(), result.getMessage());
                    }
                }).exceptionally(throwable -> {
                    logger.error("Exception sending query assigned notification to: {} - {}", pref.getEmail(), throwable.getMessage());
                    return null;
                });
            }
        }
        
        if (uniquePreferences.isEmpty()) {
            logger.warn("No notification preferences found for query assigned notifications. Query: {}", query.getId());
        }
    }
    
    @Override
    @Async("notificationTaskExecutor")
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
    @Async("notificationTaskExecutor")
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
    @Async("notificationTaskExecutor")
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
            boolean isHtmlContent = false;
            
            if (request.getTemplateName() != null && isTemplateAvailable(request.getTemplateName())) {
                content = renderTemplate(request.getTemplateName(), request.getTemplateData());
                isHtmlContent = true;
            }
            
            if (isHtmlContent && content != null && !content.trim().isEmpty()) {
                // Send HTML email using MimeMessage
                javax.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
                org.springframework.mail.javamail.MimeMessageHelper helper = 
                    new org.springframework.mail.javamail.MimeMessageHelper(mimeMessage, true, "UTF-8");
                
                helper.setFrom(notificationConfig.getEmail().getFrom());
                helper.setTo(request.getRecipients().toArray(new String[0]));
                helper.setSubject(request.getSubject());
                helper.setText(content, true); // true = HTML content
                helper.setSentDate(new Date());
                
                mailSender.send(mimeMessage);
            } else {
                // Send plain text email using SimpleMailMessage
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(notificationConfig.getEmail().getFrom());
                message.setTo(request.getRecipients().toArray(new String[0]));
                message.setSubject(request.getSubject());
                message.setText(content != null ? content : "");
                message.setSentDate(new Date());
                
                mailSender.send(message);
            }
            
            logger.info("Email sent successfully to {} recipients", request.getRecipients().size());
            return NotificationResult.success("Email sent successfully", request.getRecipients());
            
        } catch (Exception e) {
            logger.error("Failed to send email: {}", e.getMessage(), e);
            return NotificationResult.failure("Failed to send email: " + e.getMessage(), request.getRecipients());
        }
    }
    

    
    private String getRecipientAddress(String username, NotificationPreference preference) {
        switch (preference.getChannel().toUpperCase()) {
            case "EMAIL":
                return preference.getEmail() != null ? preference.getEmail() : username + "@company.com";
            default:
                return username;
        }
    }
    
    /**
     * Find users who have the specified plant assigned and create temporary notification preferences
     */
    private List<NotificationPreference> findUsersWithPlantAssignment(String plantCode) {
        List<NotificationPreference> tempPreferences = new ArrayList<>();
        
        try {
            // Find users who have this plant assigned
            List<com.cqs.qrmfg.model.User> usersWithPlant = userRepository.findByPlantCode(plantCode);
            
            for (com.cqs.qrmfg.model.User user : usersWithPlant) {
                if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                    // Create a temporary notification preference
                    NotificationPreference tempPref = new NotificationPreference();
                    tempPref.setUsername(user.getUsername());
                    tempPref.setNotificationType("PLANT_" + plantCode);
                    tempPref.setChannel("EMAIL");
                    tempPref.setEmail(user.getEmail());
                    tempPref.setEnabled(true);
                    
                    tempPreferences.add(tempPref);
                    logger.info("Found user {} with plant {} assignment, email: {}", 
                               user.getUsername(), plantCode, user.getEmail());
                }
            }
            
            // If no users found with plant assignment, check primary plant
            if (tempPreferences.isEmpty()) {
                List<com.cqs.qrmfg.model.User> usersWithPrimaryPlant = userRepository.findByPrimaryPlant(plantCode);
                
                for (com.cqs.qrmfg.model.User user : usersWithPrimaryPlant) {
                    if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                        // Create a temporary notification preference
                        NotificationPreference tempPref = new NotificationPreference();
                        tempPref.setUsername(user.getUsername());
                        tempPref.setNotificationType("PLANT_" + plantCode);
                        tempPref.setChannel("EMAIL");
                        tempPref.setEmail(user.getEmail());
                        tempPref.setEnabled(true);
                        
                        tempPreferences.add(tempPref);
                        logger.info("Found user {} with primary plant {} assignment, email: {}", 
                                   user.getUsername(), plantCode, user.getEmail());
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error finding users with plant {} assignment: {}", plantCode, e.getMessage(), e);
        }
        
        return tempPreferences;
    }
}