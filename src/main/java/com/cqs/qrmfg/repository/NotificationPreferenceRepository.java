package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    
    // Basic finders
    List<NotificationPreference> findByUsername(String username);
    List<NotificationPreference> findByNotificationType(String notificationType);
    List<NotificationPreference> findByChannel(String channel);
    List<NotificationPreference> findByEnabled(Boolean enabled);
    
    // Combined finders
    List<NotificationPreference> findByUsernameAndEnabled(String username, Boolean enabled);
    List<NotificationPreference> findByUsernameAndNotificationType(String username, String notificationType);
    List<NotificationPreference> findByUsernameAndChannel(String username, String channel);
    
    // Find specific preference
    NotificationPreference findByUsernameAndNotificationTypeAndChannel(String username, String notificationType, String channel);
    
    // Custom queries
    @Query("SELECT np FROM NotificationPreference np WHERE np.username = :username AND np.enabled = true")
    List<NotificationPreference> findActivePreferencesForUser(@Param("username") String username);
    
    @Query("SELECT np FROM NotificationPreference np WHERE np.notificationType = :type AND np.enabled = true")
    List<NotificationPreference> findActivePreferencesForType(@Param("type") String notificationType);
    
    @Query("SELECT np FROM NotificationPreference np WHERE np.username = :username AND np.notificationType = :type AND np.enabled = true")
    List<NotificationPreference> findActivePreferencesForUserAndType(
            @Param("username") String username, 
            @Param("type") String notificationType);
    
    // Existence checks
    boolean existsByUsernameAndNotificationTypeAndChannel(String username, String notificationType, String channel);
    
    // Bulk operations
    void deleteByUsername(String username);
    void deleteByUsernameAndNotificationType(String username, String notificationType);
    
    // Count queries
    long countByUsername(String username);
    long countByNotificationType(String notificationType);
    long countByEnabled(Boolean enabled);
}