package com.cqs.qrmfg.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "QRMFG_NOTIFICATIONS")
public class NotificationPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notifications_seq")
    @SequenceGenerator(name = "notifications_seq", sequenceName = "QRMFG_NOTIFICATIONS_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "username", nullable = false, length = 100)
    private String username;
    
    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;
    
    @Column(name = "channel", nullable = false, length = 20)
    private String channel;
    
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
    
    @Column(name = "email", length = 255)
    private String email;
    
    @Column(name = "slack_id", length = 100)
    private String slackId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    public NotificationPreference() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public NotificationPreference(String username, String notificationType, String channel, Boolean enabled) {
        this();
        this.username = username;
        this.notificationType = notificationType;
        this.channel = channel;
        this.enabled = enabled;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSlackId() { return slackId; }
    public void setSlackId(String slackId) { this.slackId = slackId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public boolean isEnabled() {
        return getEnabled() != null && getEnabled();
    }

    public String getPreferenceData() {
        return null; // TODO: implement if preferenceData field is added
    }
}