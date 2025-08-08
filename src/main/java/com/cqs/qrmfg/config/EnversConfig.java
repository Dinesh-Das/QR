package com.cqs.qrmfg.config;

import org.hibernate.envers.RevisionListener;
import org.hibernate.envers.RevisionEntity;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.cqs.qrmfg.model.User;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "QRMFG_REVISIONS")
@RevisionEntity(EnversConfig.CustomRevisionListener.class)
public class EnversConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "revisions_seq")
    @SequenceGenerator(name = "revisions_seq", sequenceName = "QRMFG_REVISIONS_SEQ", allocationSize = 1)
    @org.hibernate.envers.RevisionNumber
    private Long id;

    @org.hibernate.envers.RevisionTimestamp
    private Long timestamp;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "revision_date")
    private LocalDateTime revisionDate;

    public EnversConfig() {}

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { 
        this.timestamp = timestamp;
        this.revisionDate = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp), 
            java.time.ZoneId.systemDefault()
        );
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public LocalDateTime getRevisionDate() { return revisionDate; }
    public void setRevisionDate(LocalDateTime revisionDate) { this.revisionDate = revisionDate; }

    public static class CustomRevisionListener implements RevisionListener {
        @Override
        public void newRevision(Object revisionEntity) {
            EnversConfig revision = (EnversConfig) revisionEntity;
            
            // Try to get current user from security context
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User) {
                User user = (User) auth.getPrincipal();
                revision.setUsername(user.getUsername());
            } else if (auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
                revision.setUsername(auth.getName());
            } else {
                revision.setUsername("SYSTEM");
            }
            
            // Set revision date
            revision.setRevisionDate(LocalDateTime.now());
        }
    }
}