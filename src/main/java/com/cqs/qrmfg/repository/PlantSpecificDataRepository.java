package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.PlantSpecificData;
import com.cqs.qrmfg.model.PlantSpecificDataId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlantSpecificDataRepository extends JpaRepository<PlantSpecificData, PlantSpecificDataId> {
    
    // Find by individual components
    List<PlantSpecificData> findByPlantCode(String plantCode);
    List<PlantSpecificData> findByMaterialCode(String materialCode);
    
    // Find by combinations
    List<PlantSpecificData> findByPlantCodeAndMaterialCode(String plantCode, String materialCode);
    
    // Find by status
    List<PlantSpecificData> findByCompletionStatus(String completionStatus);
    List<PlantSpecificData> findByPlantCodeAndCompletionStatus(String plantCode, String completionStatus);
    
    // Find by CQS sync status
    List<PlantSpecificData> findByCqsSyncStatus(String cqsSyncStatus);
    List<PlantSpecificData> findByMaterialCodeAndCqsSyncStatus(String materialCode, String cqsSyncStatus);
    
    // Find by workflow
    List<PlantSpecificData> findByWorkflowId(Long workflowId);
    
    // Find active records
    List<PlantSpecificData> findByIsActiveTrue();
    List<PlantSpecificData> findByPlantCodeAndIsActiveTrue(String plantCode);
    List<PlantSpecificData> findByMaterialCodeAndIsActiveTrue(String materialCode);
    
    // Find by completion percentage range
    @Query("SELECT p FROM PlantSpecificData p WHERE p.completionPercentage >= :minPercentage AND p.completionPercentage <= :maxPercentage")
    List<PlantSpecificData> findByCompletionPercentageRange(@Param("minPercentage") Integer minPercentage, 
                                                           @Param("maxPercentage") Integer maxPercentage);
    
    // Find recently updated
    List<PlantSpecificData> findByUpdatedAtAfter(LocalDateTime after);
    List<PlantSpecificData> findByPlantCodeAndUpdatedAtAfter(String plantCode, LocalDateTime after);
    
    // Find submitted records
    List<PlantSpecificData> findBySubmittedAtIsNotNull();
    List<PlantSpecificData> findByPlantCodeAndSubmittedAtIsNotNull(String plantCode);
    
    // Count queries
    long countByPlantCode(String plantCode);
    long countByMaterialCode(String materialCode);
    long countByCompletionStatus(String completionStatus);
    long countByPlantCodeAndCompletionStatus(String plantCode, String completionStatus);
    
    // Statistics queries
    @Query("SELECT AVG(p.completionPercentage) FROM PlantSpecificData p WHERE p.plantCode = :plantCode")
    Double getAverageCompletionByPlant(@Param("plantCode") String plantCode);
    
    @Query("SELECT AVG(p.completionPercentage) FROM PlantSpecificData p WHERE p.materialCode = :materialCode")
    Double getAverageCompletionByMaterial(@Param("materialCode") String materialCode);
    
    @Query("SELECT p.plantCode, AVG(p.completionPercentage) FROM PlantSpecificData p GROUP BY p.plantCode")
    List<Object[]> getCompletionStatsByPlant();
    
    @Query("SELECT p.materialCode, AVG(p.completionPercentage) FROM PlantSpecificData p GROUP BY p.materialCode")
    List<Object[]> getCompletionStatsByMaterial();
    
    // Find records needing CQS sync
    @Query("SELECT p FROM PlantSpecificData p WHERE p.cqsSyncStatus = 'PENDING' OR p.lastCqsSync IS NULL OR p.lastCqsSync < :syncThreshold")
    List<PlantSpecificData> findRecordsNeedingCqsSync(@Param("syncThreshold") LocalDateTime syncThreshold);
    
    // Find incomplete records
    @Query("SELECT p FROM PlantSpecificData p WHERE p.completionPercentage < 100 AND p.completionStatus != 'SUBMITTED'")
    List<PlantSpecificData> findIncompleteRecords();
    
    @Query("SELECT p FROM PlantSpecificData p WHERE p.plantCode = :plantCode AND p.completionPercentage < 100 AND p.completionStatus != 'SUBMITTED'")
    List<PlantSpecificData> findIncompleteRecordsByPlant(@Param("plantCode") String plantCode);
    
    // Find records by date range
    @Query("SELECT p FROM PlantSpecificData p WHERE p.createdAt >= :startDate AND p.createdAt <= :endDate")
    List<PlantSpecificData> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                                  @Param("endDate") LocalDateTime endDate);
    
    // Custom query for dashboard statistics
    @Query("SELECT " +
           "COUNT(*) as total, " +
           "SUM(CASE WHEN p.completionStatus = 'COMPLETED' THEN 1 ELSE 0 END) as completed, " +
           "SUM(CASE WHEN p.completionStatus = 'SUBMITTED' THEN 1 ELSE 0 END) as submitted, " +
           "SUM(CASE WHEN p.completionStatus = 'IN_PROGRESS' THEN 1 ELSE 0 END) as inProgress, " +
           "SUM(CASE WHEN p.completionStatus = 'DRAFT' THEN 1 ELSE 0 END) as draft, " +
           "AVG(p.completionPercentage) as avgCompletion " +
           "FROM PlantSpecificData p WHERE p.plantCode = :plantCode")
    Object[] getPlantStatistics(@Param("plantCode") String plantCode);
    
    // Exists queries
    boolean existsByWorkflowId(Long workflowId);
    
    // Delete queries
    void deleteByWorkflowId(Long workflowId);

}