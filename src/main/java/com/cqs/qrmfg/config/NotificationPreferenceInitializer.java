package com.cqs.qrmfg.config;

import com.cqs.qrmfg.service.DefaultNotificationPreferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100) // Run after other initializers
public class NotificationPreferenceInitializer implements ApplicationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationPreferenceInitializer.class);
    
    @Autowired
    private DefaultNotificationPreferenceService defaultNotificationPreferenceService;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("Initializing default notification preferences for all users...");
        
        try {
            defaultNotificationPreferenceService.ensureDefaultPreferencesForAllUsers();
            logger.info("Default notification preferences initialization completed successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize default notification preferences: {}", e.getMessage(), e);
        }
    }
}