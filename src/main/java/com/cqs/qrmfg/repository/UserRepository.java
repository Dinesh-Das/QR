package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);
    
    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);
    
    /**
     * Find users by role name
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);
    
    /**
     * Find users assigned to a specific plant
     */
    @Query("SELECT u FROM User u WHERE u.assignedPlants LIKE %:plantCode%")
    List<User> findByPlantCode(@Param("plantCode") String plantCode);
    
    /**
     * Find users with primary plant
     */
    @Query("SELECT u FROM User u WHERE u.primaryPlant = :plantCode")
    List<User> findByPrimaryPlant(@Param("plantCode") String plantCode);
    
    /**
     * Find active users
     */
    @Query("SELECT u FROM User u WHERE u.enabled = true AND u.status = 'ACTIVE'")
    List<User> findActiveUsers();
    
    /**
     * Find users by status
     */
    List<User> findByStatus(String status);
    

}