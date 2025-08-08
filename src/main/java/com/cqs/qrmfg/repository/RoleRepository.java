package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.Role;
import com.cqs.qrmfg.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    boolean existsByName(String name);
    
    // Temporarily commented out methods that use roleType field
    // TODO: Uncomment these methods after adding role_type column to database
    
    // Optional<Role> findByRoleType(RoleType roleType);
    // boolean existsByRoleType(RoleType roleType);
    // List<Role> findAllByRoleType(RoleType roleType);
    // List<Role> findByRoleTypeAndEnabled(RoleType roleType, boolean enabled);
    
    /**
     * Find all enabled roles
     */
    @Query("SELECT r FROM Role r WHERE r.enabled = true")
    List<Role> findByEnabledTrue();
    
    /**
     * Get all users assigned to a specific role
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.id = :roleId")
    List<com.cqs.qrmfg.model.User> findUsersByRoleId(@Param("roleId") Long roleId);
    
    // Temporarily commented out methods that use roleType field
    // @Query("SELECT u FROM User u JOIN u.roles r WHERE r.roleType = :roleType")
    // List<com.cqs.qrmfg.model.User> findUsersByRoleType(@Param("roleType") RoleType roleType);
    
    // @Query("SELECT COUNT(r) > 0 FROM User u JOIN u.roles r WHERE u.id = :userId AND r.roleType = :roleType")
    // boolean userHasRoleType(@Param("userId") Long userId, @Param("roleType") RoleType roleType);
    
    // @Query("SELECT r FROM User u JOIN u.roles r WHERE u.id = :userId ORDER BY r.roleType")
    // List<Role> findUserRolesOrderedByHierarchy(@Param("userId") Long userId);
    
    // @Query("SELECT r.roleType, COUNT(r) FROM Role r GROUP BY r.roleType")
    // List<Object[]> countRolesByType();
} 