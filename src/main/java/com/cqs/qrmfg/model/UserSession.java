package com.cqs.qrmfg.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * User session entity for tracking user sessions
 */
@Entity
@Table(name = "QRMFG_USER_SESSIONS")
public class UserSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_sessions_seq")
    @SequenceGenerator(name = "user_sessions_seq", sequenceName = "QRMFG_USER_SESSIONS_SEQ", allocationSize = 1)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;
    
    @Column(name = "session_token", unique = true, nullable = false, length = 500)
    private String sessionToken;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;
    
    @Column(name = "is_active")
    private boolean isActive = true;
    
    public UserSession() {}
    
    public UserSession(User user, String sessionToken, String ipAddress, String userAgent, LocalDateTime expiresAt) {
        this.user = user;
        this.sessionToken = sessionToken;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
        this.lastAccessedAt = LocalDateTime.now();
        this.isActive = true;
    }
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (lastAccessedAt == null) {
            lastAccessedAt = LocalDateTime.now();
        }
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getSessionToken() {
        return sessionToken;
    }
    
    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }
    
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    /**
     * Check if session is expired
     */
    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }
    
    /**
     * Update last accessed time
     */
    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }
    
    /**
     * Deactivate session
     */
    public void deactivate() {
        this.isActive = false;
    }
}