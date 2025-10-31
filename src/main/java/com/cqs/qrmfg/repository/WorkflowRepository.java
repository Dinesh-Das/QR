package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.Workflow;
import com.cqs.qrmfg.model.WorkflowState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Long> {
    
    // Basic finders
    Optional<Workflow> findByProjectCodeAndMaterialCodeAndPlantCode(
        String projectCode, String materialCode, String plantCode);
    
    // Eager loading version for duplicate check
    @Query("SELECT DISTINCT w FROM Workflow w LEFT JOIN FETCH w.queries WHERE w.projectCode = :projectCode AND w.materialCode = :materialCode AND w.plantCode = :plantCode")
    Optional<Workflow> findByProjectCodeAndMaterialCodeAndPlantCodeWithQueries(
        @Param("projectCode") String projectCode, 
        @Param("materialCode") String materialCode, 
        @Param("plantCode") String plantCode);
    
    List<Workflow> findByState(WorkflowState state);
    
    List<Workflow> findByStateOrderByCreatedAtDesc(WorkflowState state);
    
    // Dashboard data queries
    @Query("SELECT w FROM Workflow w WHERE w.state != 'COMPLETED' ORDER BY w.createdAt DESC")
    List<Workflow> findPendingWorkflows();
    
    @Query("SELECT w FROM Workflow w WHERE w.state != 'COMPLETED' AND w.plantCode = :plantCode ORDER BY w.createdAt DESC")
    List<Workflow> findPendingWorkflowsByPlant(@Param("plantCode") String plantCode);
    
    @Query("SELECT w FROM Workflow w WHERE w.state != 'COMPLETED' AND w.initiatedBy = :username ORDER BY w.createdAt DESC")
    List<Workflow> findPendingWorkflowsByUser(@Param("username") String username);
    
    @Query(value = "SELECT * FROM QRMFG_WORKFLOWS WHERE workflow_state != 'COMPLETED' AND ((workflow_state = 'JVC_PENDING' AND created_at < CURRENT_TIMESTAMP - 3) OR (workflow_state = 'PLANT_PENDING' AND COALESCE(extended_at, created_at) < CURRENT_TIMESTAMP - 3) OR (workflow_state IN ('CQS_PENDING', 'TECH_PENDING') AND last_modified < CURRENT_TIMESTAMP - 3))", nativeQuery = true)
    List<Workflow> findOverdueWorkflows();
    
    @Query("SELECT DISTINCT w FROM Workflow w JOIN w.queries q WHERE q.status = 'OPEN'")
    List<Workflow> findWorkflowsWithOpenQueries();
    
    // Project/Material filtering for dashboard
    @Query("SELECT w FROM Workflow w WHERE w.projectCode = :projectCode AND w.materialCode = :materialCode ORDER BY w.createdAt DESC")
    List<Workflow> findByProjectAndMaterial(@Param("projectCode") String projectCode, @Param("materialCode") String materialCode);
    
    @Query("SELECT w FROM Workflow w WHERE w.projectCode = :projectCode ORDER BY w.createdAt DESC")
    List<Workflow> findByProjectCodeOrderByCreatedAt(@Param("projectCode") String projectCode);
    
    @Query("SELECT w FROM Workflow w WHERE w.materialCode = :materialCode ORDER BY w.createdAt DESC")
    List<Workflow> findByMaterialCodeOrderByCreatedAt(@Param("materialCode") String materialCode);
    
    // Count queries for dashboard
    long countByState(WorkflowState state);
    
    @Query(value = "SELECT COUNT(*) FROM QRMFG_WORKFLOWS WHERE workflow_state != 'COMPLETED' AND ((workflow_state = 'JVC_PENDING' AND (SYSDATE - created_at) > 3) OR (workflow_state = 'PLANT_PENDING' AND (SYSDATE - NVL(extended_at, created_at)) > 3) OR (workflow_state IN ('CQS_PENDING', 'TECH_PENDING') AND (SYSDATE - last_modified) > 3))", nativeQuery = true)
    long countOverdueWorkflows();
    
    @Query(value = "SELECT COUNT(DISTINCT w.id) FROM QRMFG_WORKFLOWS w JOIN QRMFG_QUERIES q ON w.id = q.workflow_id WHERE q.status = 'OPEN'", nativeQuery = true)
    long countWorkflowsWithOpenQueries();
    
    // Plant-specific queries
    @Query("SELECT w FROM Workflow w WHERE w.plantCode = :plantCode AND w.state = :state")
    List<Workflow> findByPlantAndState(@Param("plantCode") String plantCode, @Param("state") WorkflowState state);
    
    @Query("SELECT w FROM Workflow w WHERE w.plantCode = :plantCode AND w.state != 'COMPLETED'")
    List<Workflow> findPendingByPlant(@Param("plantCode") String plantCode);
    

    
    // User-specific queries
    @Query("SELECT w FROM Workflow w WHERE w.initiatedBy = :username AND w.state != 'COMPLETED'")
    List<Workflow> findPendingByInitiatedBy(@Param("username") String username);
    
    // Fetch workflows with queries eagerly loaded to avoid LazyInitializationException
    @Query("SELECT DISTINCT w FROM Workflow w LEFT JOIN FETCH w.queries WHERE w.initiatedBy = :username")
    List<Workflow> findByInitiatedByWithQueries(@Param("username") String username);
    
    @Query("SELECT DISTINCT w FROM Workflow w LEFT JOIN FETCH w.queries")
    List<Workflow> findAllWithQueries();
    
    @Query("SELECT DISTINCT w FROM Workflow w LEFT JOIN FETCH w.queries WHERE w.state = :state")
    List<Workflow> findByStateWithQueries(@Param("state") WorkflowState state);
    
    @Query("SELECT DISTINCT w FROM Workflow w LEFT JOIN FETCH w.queries WHERE w.plantCode = :plantCode")
    List<Workflow> findByPlantCodeWithQueries(@Param("plantCode") String plantCode);
    
    // Analytics and reporting queries
    @Query("SELECT w.plantCode, COUNT(w) FROM Workflow w GROUP BY w.plantCode")
    List<Object[]> countByPlantCodeGrouped();
    
    @Query(value = "SELECT TRUNC(created_at), COUNT(*) FROM QRMFG_WORKFLOWS WHERE created_at >= :startDate GROUP BY TRUNC(created_at) ORDER BY TRUNC(created_at)", nativeQuery = true)
    List<Object[]> countByCreatedAtAfterGroupByDay(@Param("startDate") LocalDateTime startDate);
    
    @Query(value = "SELECT AVG((last_modified - created_at) * 24) FROM QRMFG_WORKFLOWS WHERE workflow_state = 'COMPLETED'", nativeQuery = true)
    Double calculateAverageCompletionTimeHours();
    
    // Enhanced filtering
    @Query("SELECT w FROM Workflow w WHERE " +
           "(:projectCode IS NULL OR w.projectCode = :projectCode) AND " +
           "(:materialCode IS NULL OR w.materialCode = :materialCode) AND " +
           "(:plantCode IS NULL OR w.plantCode = :plantCode) AND " +
           "(:state IS NULL OR w.state = :state) AND " +
           "(:initiatedBy IS NULL OR w.initiatedBy = :initiatedBy) " +
           "ORDER BY w.createdAt DESC")
    List<Workflow> findWithFilters(
        @Param("projectCode") String projectCode,
        @Param("materialCode") String materialCode, 
        @Param("plantCode") String plantCode,
        @Param("state") WorkflowState state,
        @Param("initiatedBy") String initiatedBy);
    
    // Date range queries
    List<Workflow> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<Workflow> findByStateAndCreatedAtBetween(WorkflowState state, LocalDateTime start, LocalDateTime end);
    
    // Bulk operations support
    @Query("SELECT w FROM Workflow w WHERE w.id IN :workflowIds")
    List<Workflow> findByIds(@Param("workflowIds") List<Long> workflowIds);
    
    @Query("SELECT w FROM Workflow w WHERE w.projectCode IN :projectCodes AND w.state != 'COMPLETED'")
    List<Workflow> findPendingByProjectCodes(@Param("projectCodes") List<String> projectCodes);
    
    @Query("SELECT w FROM Workflow w WHERE w.materialCode IN :materialCodes AND w.state != 'COMPLETED'")
    List<Workflow> findPendingByMaterialCodes(@Param("materialCodes") List<String> materialCodes);
    
    // Additional methods needed by AdminMonitoringServiceImpl
    @Query("SELECT COUNT(w) FROM Workflow w WHERE w.state != :state")
    long countByStateNot(@Param("state") WorkflowState state);
    
    @Query("SELECT COUNT(w) FROM Workflow w WHERE w.state != :state AND w.createdAt < :cutoffDate")
    long countByStateNotAndCreatedAtBefore(@Param("state") WorkflowState state, @Param("cutoffDate") LocalDateTime cutoffDate);
    
    List<Workflow> findByCreatedAtAfter(LocalDateTime cutoffDate);
    List<Workflow> findByCreatedAtBefore(LocalDateTime cutoffDate);
    List<Workflow> findByCompletedAtAfter(LocalDateTime cutoffDate);
    
    // Analytics methods
    @Query(value = "SELECT workflow_state, AVG((last_modified - created_at) * 24) FROM QRMFG_WORKFLOWS WHERE workflow_state = 'COMPLETED' GROUP BY workflow_state", nativeQuery = true)
    List<Object[]> calculateAverageTimeInEachStateGrouped();
    
    @Query(value = "SELECT workflow_state, COUNT(*) FROM QRMFG_WORKFLOWS WHERE workflow_state != 'COMPLETED' AND (SYSDATE - created_at) > 3 GROUP BY workflow_state", nativeQuery = true)
    List<Object[]> countOverdueWorkflowsByStateGrouped();
    
    @Query(value = "SELECT plant_code, COUNT(*) FROM QRMFG_WORKFLOWS WHERE workflow_state != 'COMPLETED' AND (SYSDATE - created_at) > 3 GROUP BY plant_code", nativeQuery = true)
    List<Object[]> countDelayedWorkflowsByPlantGrouped();
    
    // Additional methods needed by WorkflowServiceImpl
    List<Workflow> findByMaterialCode(String materialCode);
    boolean existsByMaterialCode(String materialCode);
    boolean existsByProjectCodeAndMaterialCodeAndPlantCode(String projectCode, String materialCode, String plantCode);
    
    // Plant questionnaire specific methods
    List<Workflow> findByPlantCodeAndMaterialCode(String plantCode, String materialCode);
}