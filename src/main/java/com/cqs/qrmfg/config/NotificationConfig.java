package com.cqs.qrmfg.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "notification")
public class NotificationConfig {
    
    private boolean enabled = true;
    private Email email = new Email();
    private Slack slack = new Slack();
    private Template template = new Template();
    private Retry retry = new Retry();
    
    public static class Email {
        private boolean enabled = true;
        private String host = "localhost";
        private int port = 587;
        private String username;
        private String password;
        private String from = "noreply@qrmfg.com";
        private String fromName = "QRMFG Workflow System";
        private boolean startTlsEnabled = true;
        private boolean authEnabled = true;
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        
        public String getFromName() { return fromName; }
        public void setFromName(String fromName) { this.fromName = fromName; }
        
        public boolean isStartTlsEnabled() { return startTlsEnabled; }
        public void setStartTlsEnabled(boolean startTlsEnabled) { this.startTlsEnabled = startTlsEnabled; }
        
        public boolean isAuthEnabled() { return authEnabled; }
        public void setAuthEnabled(boolean authEnabled) { this.authEnabled = authEnabled; }
    }
    
    public static class Slack {
        private boolean enabled = false;
        private String botToken;
        private String webhookUrl;
        private String defaultChannel = "#workflow-notifications";
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getBotToken() { return botToken; }
        public void setBotToken(String botToken) { this.botToken = botToken; }
        
        public String getWebhookUrl() { return webhookUrl; }
        public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
        
        public String getDefaultChannel() { return defaultChannel; }
        public void setDefaultChannel(String defaultChannel) { this.defaultChannel = defaultChannel; }
    }
    
    public static class Template {
        private String basePath = "classpath:/templates/notifications/";
        private String defaultEngine = "thymeleaf";
        
        // Getters and setters
        public String getBasePath() { return basePath; }
        public void setBasePath(String basePath) { this.basePath = basePath; }
        
        public String getDefaultEngine() { return defaultEngine; }
        public void setDefaultEngine(String defaultEngine) { this.defaultEngine = defaultEngine; }
    }
    
    public static class Retry {
        private int maxAttempts = 3;
        private long delayMillis = 5000;
        private double backoffMultiplier = 2.0;
        
        // Getters and setters
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        
        public long getDelayMillis() { return delayMillis; }
        public void setDelayMillis(long delayMillis) { this.delayMillis = delayMillis; }
        
        public double getBackoffMultiplier() { return backoffMultiplier; }
        public void setBackoffMultiplier(double backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }
    }
    
    // Main getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public Email getEmail() { return email; }
    public void setEmail(Email email) { this.email = email; }
    
    public Slack getSlack() { return slack; }
    public void setSlack(Slack slack) { this.slack = slack; }
    
    public Template getTemplate() { return template; }
    public void setTemplate(Template template) { this.template = template; }
    
    public Retry getRetry() { return retry; }
    public void setRetry(Retry retry) { this.retry = retry; }
}