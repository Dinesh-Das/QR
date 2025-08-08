package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.enums.DocumentSource;
import com.cqs.qrmfg.model.Document;
import com.cqs.qrmfg.model.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    List<Document> findByWorkflowOrderByUploadedAtDesc(Workflow workflow);
    
    List<Document> findByWorkflowAndFileType(Workflow workflow, String fileType);
    
    /**
     * Find ALL documents for comprehensive document reuse across all sources (workflow, query, response)
     * This method supports the enhanced document reuse functionality by returning documents from any source
     * for the same project/material combination.
     */
    @Query("SELECT d FROM Document d WHERE d.projectCode = :projectCode AND d.materialCode = :materialCode ORDER BY d.uploadedAt DESC")
    List<Document> findByProjectCodeAndMaterialCode(@Param("projectCode") String projectCode, @Param("materialCode") String materialCode);

    /**
     * Find reusable documents for same project and material combination (legacy method for backward compatibility)
     */
    @Query("SELECT d FROM Document d JOIN FETCH d.workflow w WHERE w.projectCode = :projectCode AND w.materialCode = :materialCode")
    List<Document> findReusableDocuments(@Param("projectCode") String projectCode, @Param("materialCode") String materialCode);

    /**
     * Find documents by workflow project and material codes
     */
    @Query("SELECT d FROM Document d JOIN FETCH d.workflow w WHERE w.projectCode = :projectCode AND w.materialCode = :materialCode AND d.workflow.id != :excludeWorkflowId")
    List<Document> findReusableDocumentsExcludingWorkflow(
        @Param("projectCode") String projectCode, 
        @Param("materialCode") String materialCode,
        @Param("excludeWorkflowId") Long excludeWorkflowId);

    /**
     * Find documents uploaded by specific user
     */
    List<Document> findByUploadedByOrderByUploadedAtDesc(String uploadedBy);

    /**
     * Find documents uploaded after specific date
     */
    List<Document> findByUploadedAtAfterOrderByUploadedAtDesc(LocalDateTime uploadedAfter);

    /**
     * Find documents by file type
     */
    List<Document> findByFileTypeOrderByUploadedAtDesc(String fileType);

    /**
     * Find reused documents
     */
    List<Document> findByIsReusedTrueOrderByUploadedAtDesc();

    /**
     * Find original documents (not reused)
     */
    List<Document> findByIsReusedFalseOrderByUploadedAtDesc();

    /**
     * Find documents by original document reference
     */
    List<Document> findByOriginalDocumentOrderByUploadedAtDesc(Document originalDocument);

    /**
     * Count documents by workflow
     */
    long countByWorkflow(Workflow workflow);

    /**
     * Count documents by file type
     */
    long countByFileType(String fileType);

    /**
     * Count reused documents
     */
    long countByIsReusedTrue();

    /**
     * Find documents by file name pattern
     */
    @Query("SELECT d FROM Document d WHERE d.fileName LIKE %:pattern% OR d.originalFileName LIKE %:pattern% ORDER BY d.uploadedAt DESC")
    List<Document> findByFileNameContaining(@Param("pattern") String pattern);

    /**
     * Find documents by size range
     */
    @Query("SELECT d FROM Document d WHERE d.fileSize BETWEEN :minSize AND :maxSize ORDER BY d.uploadedAt DESC")
    List<Document> findByFileSizeBetween(@Param("minSize") Long minSize, @Param("maxSize") Long maxSize);

    /**
     * Find large documents (over specified size)
     */
    @Query("SELECT d FROM Document d WHERE d.fileSize > :sizeThreshold ORDER BY d.fileSize DESC")
    List<Document> findLargeDocuments(@Param("sizeThreshold") Long sizeThreshold);

    /**
     * Get total storage used by workflow
     */
    @Query("SELECT COALESCE(SUM(d.fileSize), 0) FROM Document d WHERE d.workflow = :workflow")
    Long getTotalStorageByWorkflow(@Param("workflow") Workflow workflow);

    /**
     * Get total storage used by project and material
     */
    @Query("SELECT COALESCE(SUM(d.fileSize), 0) FROM Document d JOIN d.workflow w WHERE w.projectCode = :projectCode AND w.materialCode = :materialCode")
    Long getTotalStorageByProjectAndMaterial(@Param("projectCode") String projectCode, @Param("materialCode") String materialCode);

    /**
     * Find documents for bulk operations
     */
    @Query("SELECT d FROM Document d WHERE d.workflow.id IN :workflowIds ORDER BY d.workflow.id, d.uploadedAt DESC")
    List<Document> findByWorkflowIds(@Param("workflowIds") List<Long> workflowIds);

    /**
     * Document usage trends
     */
    @Query(value = "SELECT DATE(uploaded_at), file_type, COUNT(*) FROM QRMFG_DOCUMENTS WHERE uploaded_at >= :startDate GROUP BY DATE(uploaded_at), file_type ORDER BY DATE(uploaded_at), file_type", nativeQuery = true)
    List<Object[]> getDocumentUploadTrend(@Param("startDate") LocalDateTime startDate);

    /**
     * Check if document exists by file path
     */
    boolean existsByFilePath(String filePath);

    /**
     * Find document by file path
     */
    Optional<Document> findByFilePath(String filePath);

    /**
     * Delete documents by workflow
     */
    void deleteByWorkflow(Workflow workflow);

    /**
     * Find documents that can be reused for a specific workflow
     */
    @Query("SELECT d FROM Document d JOIN d.workflow w WHERE " +
           "w.projectCode = :projectCode AND w.materialCode = :materialCode AND " +
           "d.workflow.id != :excludeWorkflowId AND d.isReused = false " +
           "ORDER BY d.uploadedAt DESC")
    List<Document> findReusableCandidates(
        @Param("projectCode") String projectCode,
        @Param("materialCode") String materialCode, 
        @Param("excludeWorkflowId") Long excludeWorkflowId);
    
    // Additional methods needed by DocumentServiceImpl
    List<Document> findByWorkflowId(Long workflowId);
    long countByWorkflowId(Long workflowId);

    // ========== NEW UNIFIED QUERY METHODS FOR ENHANCED DOCUMENT MANAGEMENT ==========

    /**
     * Find documents by query ID for query document attachment functionality
     */
    @Query("SELECT d FROM Document d WHERE d.query.id = :queryId ORDER BY d.uploadedAt DESC")
    List<Document> findByQueryId(@Param("queryId") Long queryId);

    /**
     * Find documents by query ID and response ID for response document attachment
     */
    @Query("SELECT d FROM Document d WHERE d.query.id = :queryId AND d.responseId = :responseId ORDER BY d.uploadedAt DESC")
    List<Document> findByQueryIdAndResponseId(@Param("queryId") Long queryId, @Param("responseId") Long responseId);

    /**
     * Find documents by document source for filtering by document type
     */
    List<Document> findByDocumentSourceOrderByUploadedAtDesc(DocumentSource documentSource);

    /**
     * Find documents by document source and project/material for comprehensive filtering
     */
    @Query("SELECT d FROM Document d WHERE d.documentSource = :documentSource AND d.projectCode = :projectCode AND d.materialCode = :materialCode ORDER BY d.uploadedAt DESC")
    List<Document> findByDocumentSourceAndProjectCodeAndMaterialCode(
        @Param("documentSource") DocumentSource documentSource,
        @Param("projectCode") String projectCode, 
        @Param("materialCode") String materialCode);

    /**
     * Document access control validation - check if user has access to workflow documents
     */
    @Query("SELECT COUNT(d) > 0 FROM Document d JOIN d.workflow w WHERE d.id = :documentId AND w.createdBy = :userId")
    boolean hasWorkflowDocumentAccess(@Param("documentId") Long documentId, @Param("userId") String userId);

    /**
     * Document access control validation - check if user has access to query documents
     */
    @Query("SELECT COUNT(d) > 0 FROM Document d JOIN d.query q JOIN q.workflow w WHERE d.id = :documentId AND (q.raisedBy = :userId OR w.createdBy = :userId)")
    boolean hasQueryDocumentAccess(@Param("documentId") Long documentId, @Param("userId") String userId);

    /**
     * Document access control validation - unified method for all document types
     */
    @Query("SELECT COUNT(d) > 0 FROM Document d LEFT JOIN d.workflow w LEFT JOIN d.query q LEFT JOIN q.workflow qw " +
           "WHERE d.id = :documentId AND " +
           "((d.documentSource = 'WORKFLOW' AND w.createdBy = :userId) OR " +
           "(d.documentSource IN ('QUERY', 'RESPONSE') AND (q.raisedBy = :userId OR qw.createdBy = :userId)))")
    boolean hasDocumentAccess(@Param("documentId") Long documentId, @Param("userId") String userId);

    /**
     * Find document with eager loading of workflow and query relationships
     */
    @Query("SELECT d FROM Document d " +
           "LEFT JOIN FETCH d.workflow w " +
           "LEFT JOIN FETCH d.query q " +
           "LEFT JOIN FETCH q.workflow qw " +
           "WHERE d.id = :documentId")
    Optional<Document> findByIdWithRelationships(@Param("documentId") Long documentId);

    /**
     * Find documents by multiple document sources for unified search
     */
    @Query("SELECT d FROM Document d WHERE d.documentSource IN :sources ORDER BY d.uploadedAt DESC")
    List<Document> findByDocumentSourceIn(@Param("sources") List<DocumentSource> sources);

    /**
     * Find documents by project/material and document sources for filtered comprehensive search
     */
    @Query("SELECT d FROM Document d WHERE d.projectCode = :projectCode AND d.materialCode = :materialCode AND d.documentSource IN :sources ORDER BY d.uploadedAt DESC")
    List<Document> findByProjectCodeAndMaterialCodeAndDocumentSourceIn(
        @Param("projectCode") String projectCode,
        @Param("materialCode") String materialCode,
        @Param("sources") List<DocumentSource> sources);

    /**
     * Count documents by query ID
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.query.id = :queryId")
    long countByQueryId(@Param("queryId") Long queryId);

    /**
     * Count documents by document source
     */
    long countByDocumentSource(DocumentSource documentSource);

    /**
     * Count documents by project/material and document source
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.projectCode = :projectCode AND d.materialCode = :materialCode AND d.documentSource = :documentSource")
    long countByProjectCodeAndMaterialCodeAndDocumentSource(
        @Param("projectCode") String projectCode,
        @Param("materialCode") String materialCode,
        @Param("documentSource") DocumentSource documentSource);

    /**
     * Find all documents related to a query (both query and response documents)
     */
    @Query("SELECT d FROM Document d WHERE d.query.id = :queryId ORDER BY d.documentSource, d.uploadedAt DESC")
    List<Document> findAllByQueryId(@Param("queryId") Long queryId);

    /**
     * Find documents by workflow with enhanced metadata
     */
    @Query("SELECT d FROM Document d WHERE d.workflow.id = :workflowId ORDER BY d.uploadedAt DESC")
    List<Document> findByWorkflowIdOrderByUploadedAtDesc(@Param("workflowId") Long workflowId);

    /**
     * Find documents for unified search across all types with text search
     */
    @Query("SELECT d FROM Document d WHERE " +
           "(d.fileName LIKE %:searchTerm% OR d.originalFileName LIKE %:searchTerm%) AND " +
           "d.projectCode = :projectCode AND d.materialCode = :materialCode " +
           "ORDER BY d.uploadedAt DESC")
    List<Document> searchDocumentsByProjectAndMaterial(
        @Param("searchTerm") String searchTerm,
        @Param("projectCode") String projectCode,
        @Param("materialCode") String materialCode);

    /**
     * Find documents for unified search with source filtering
     */
    @Query("SELECT d FROM Document d WHERE " +
           "(d.fileName LIKE %:searchTerm% OR d.originalFileName LIKE %:searchTerm%) AND " +
           "d.projectCode = :projectCode AND d.materialCode = :materialCode AND " +
           "d.documentSource IN :sources " +
           "ORDER BY d.uploadedAt DESC")
    List<Document> searchDocumentsByProjectAndMaterialAndSources(
        @Param("searchTerm") String searchTerm,
        @Param("projectCode") String projectCode,
        @Param("materialCode") String materialCode,
        @Param("sources") List<DocumentSource> sources);
}