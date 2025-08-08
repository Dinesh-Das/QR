package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.DocumentAccessLog;
import com.cqs.qrmfg.model.DocumentAccessType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for DocumentAccessLog entity
 */
@Repository
public interface DocumentAccessLogRepository extends JpaRepository<DocumentAccessLog, Long> {

    /**
     * Find access logs by document ID
     */
    List<DocumentAccessLog> findByDocumentIdOrderByAccessTimeDesc(Long documentId);

    /**
     * Find access logs by user
     */
    List<DocumentAccessLog> findByAccessedByOrderByAccessTimeDesc(String accessedBy);

    /**
     * Find access logs by workflow ID
     */
    List<DocumentAccessLog> findByWorkflowIdOrderByAccessTimeDesc(Long workflowId);

    /**
     * Find unauthorized access attempts
     */
    List<DocumentAccessLog> findByAccessGrantedFalseOrderByAccessTimeDesc();

    /**
     * Count document downloads by document ID
     */
    @Query("SELECT COUNT(dal) FROM DocumentAccessLog dal WHERE dal.document.id = :documentId AND dal.accessType = :accessType AND dal.accessGranted = true")
    long countDocumentAccess(@Param("documentId") Long documentId, @Param("accessType") DocumentAccessType accessType);

    /**
     * Find recent access logs for a document
     */
    @Query("SELECT dal FROM DocumentAccessLog dal WHERE dal.document.id = :documentId AND dal.accessTime >= :since ORDER BY dal.accessTime DESC")
    List<DocumentAccessLog> findRecentAccessLogs(@Param("documentId") Long documentId, @Param("since") LocalDateTime since);

    /**
     * Find access logs by user and time range
     */
    @Query("SELECT dal FROM DocumentAccessLog dal WHERE dal.accessedBy = :user AND dal.accessTime BETWEEN :startTime AND :endTime ORDER BY dal.accessTime DESC")
    List<DocumentAccessLog> findByUserAndTimeRange(@Param("user") String user, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}