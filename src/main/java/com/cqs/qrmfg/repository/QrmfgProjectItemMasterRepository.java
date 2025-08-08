package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.QrmfgProjectItemMaster;
import com.cqs.qrmfg.model.QrmfgProjectItemMaster.ProjectItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QrmfgProjectItemMasterRepository extends JpaRepository<QrmfgProjectItemMaster, ProjectItemId> {
    
    /**
     * Find all items for a specific project
     */
    List<QrmfgProjectItemMaster> findByProjectCode(String projectCode);
    
    /**
     * Find all projects for a specific item
     */
    List<QrmfgProjectItemMaster> findByItemCode(String itemCode);
    
    /**
     * Find specific project-item combination
     */
    Optional<QrmfgProjectItemMaster> findByProjectCodeAndItemCode(String projectCode, String itemCode);
    
    /**
     * Get all distinct project codes
     */
    @Query("SELECT DISTINCT p.projectCode FROM QrmfgProjectItemMaster p ORDER BY p.projectCode")
    List<String> findDistinctProjectCodes();
    
    /**
     * Get all distinct item codes
     */
    @Query("SELECT DISTINCT p.itemCode FROM QrmfgProjectItemMaster p ORDER BY p.itemCode")
    List<String> findDistinctItemCodes();
    
    /**
     * Get distinct item codes for a specific project
     */
    @Query("SELECT DISTINCT p.itemCode FROM QrmfgProjectItemMaster p WHERE p.projectCode = :projectCode ORDER BY p.itemCode")
    List<String> findDistinctItemCodesByProject(@Param("projectCode") String projectCode);
    
    /**
     * Get distinct project codes for a specific item
     */
    @Query("SELECT DISTINCT p.projectCode FROM QrmfgProjectItemMaster p WHERE p.itemCode = :itemCode ORDER BY p.projectCode")
    List<String> findDistinctProjectCodesByItem(@Param("itemCode") String itemCode);
    
    /**
     * Search project-item combinations by project code pattern
     */
    @Query("SELECT p FROM QrmfgProjectItemMaster p WHERE UPPER(p.projectCode) LIKE UPPER(CONCAT('%', :searchTerm, '%')) ORDER BY p.projectCode, p.itemCode")
    List<QrmfgProjectItemMaster> searchByProjectCode(@Param("searchTerm") String searchTerm);
    
    /**
     * Search project-item combinations by item code pattern
     */
    @Query("SELECT p FROM QrmfgProjectItemMaster p WHERE UPPER(p.itemCode) LIKE UPPER(CONCAT('%', :searchTerm, '%')) ORDER BY p.projectCode, p.itemCode")
    List<QrmfgProjectItemMaster> searchByItemCode(@Param("searchTerm") String searchTerm);
    
    /**
     * Count items per project
     */
    @Query("SELECT p.projectCode, COUNT(p.itemCode) FROM QrmfgProjectItemMaster p GROUP BY p.projectCode ORDER BY p.projectCode")
    List<Object[]> countItemsPerProject();
    
    /**
     * Count projects per item
     */
    @Query("SELECT p.itemCode, COUNT(p.projectCode) FROM QrmfgProjectItemMaster p GROUP BY p.itemCode ORDER BY p.itemCode")
    List<Object[]> countProjectsPerItem();
    
    /**
     * Check if project-item combination exists
     */
    boolean existsByProjectCodeAndItemCode(String projectCode, String itemCode);
    
    /**
     * Check if project exists
     */
    boolean existsByProjectCode(String projectCode);
    
    /**
     * Check if item exists
     */
    boolean existsByItemCode(String itemCode);
    
    /**
     * Count total project-item combinations
     */
    @Query("SELECT COUNT(p) FROM QrmfgProjectItemMaster p")
    Long countAllProjectItems();
    
    /**
     * Find projects with item count above threshold
     */
    @Query("SELECT p.projectCode FROM QrmfgProjectItemMaster p GROUP BY p.projectCode HAVING COUNT(p.itemCode) > :minItemCount ORDER BY p.projectCode")
    List<String> findProjectsWithItemCountAbove(@Param("minItemCount") Long minItemCount);
    
    /**
     * Find items used in multiple projects
     */
    @Query("SELECT p.itemCode FROM QrmfgProjectItemMaster p GROUP BY p.itemCode HAVING COUNT(p.projectCode) > 1 ORDER BY p.itemCode")
    List<String> findItemsInMultipleProjects();
    
    /**
     * Get item description by project code and item code
     */
    @Query("SELECT p.itemDescription FROM QrmfgProjectItemMaster p WHERE p.projectCode = :projectCode AND p.itemCode = :itemCode")
    Optional<String> findItemDescriptionByProjectCodeAndItemCode(@Param("projectCode") String projectCode, @Param("itemCode") String itemCode);
}