package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.QrmfgLocationMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QrmfgLocationMasterRepository extends JpaRepository<QrmfgLocationMaster, String> {
    
    /**
     * Find location by location code
     */
    Optional<QrmfgLocationMaster> findByLocationCode(String locationCode);
    
    /**
     * Find locations by description containing text (case insensitive)
     */
    @Query("SELECT l FROM QrmfgLocationMaster l WHERE UPPER(l.description) LIKE UPPER(CONCAT('%', :searchTerm, '%'))")
    List<QrmfgLocationMaster> findByDescriptionContainingIgnoreCase(@Param("searchTerm") String searchTerm);
    
    /**
     * Find all locations ordered by location code
     */
    @Query("SELECT l FROM QrmfgLocationMaster l ORDER BY l.locationCode")
    List<QrmfgLocationMaster> findAllOrderByLocationCode();
    
    /**
     * Search locations by location code or description
     */
    @Query("SELECT l FROM QrmfgLocationMaster l WHERE " +
           "UPPER(l.locationCode) LIKE UPPER(CONCAT('%', :searchTerm, '%')) OR " +
           "UPPER(l.description) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY l.locationCode")
    List<QrmfgLocationMaster> searchLocations(@Param("searchTerm") String searchTerm);
    
    /**
     * Check if location code exists
     */
    boolean existsByLocationCode(String locationCode);
    
    /**
     * Count total locations
     */
    @Query("SELECT COUNT(l) FROM QrmfgLocationMaster l")
    Long countAllLocations();
}