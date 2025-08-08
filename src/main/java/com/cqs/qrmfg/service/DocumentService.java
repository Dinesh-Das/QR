package com.cqs.qrmfg.service;

import com.cqs.qrmfg.dto.DocumentAccessLogDto;
import com.cqs.qrmfg.dto.DocumentReuseContext;
import com.cqs.qrmfg.dto.DocumentSummary;
import com.cqs.qrmfg.dto.DocumentUploadContext;
import com.cqs.qrmfg.enums.DocumentSource;
import com.cqs.qrmfg.model.DocumentAccessType;
import com.cqs.qrmfg.model.Document;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service interface for document management operations
 */
public interface DocumentService {

    /**
     * Upload documents for a workflow
     */
    List<DocumentSummary> uploadDocuments(MultipartFile[] files, String projectCode, String materialCode, Long workflowId, String uploadedBy);

    /**
     * Get documents for a workflow
     */
    List<DocumentSummary> getWorkflowDocuments(Long workflowId);

    /**
     * Get reusable documents for project and material combination
     */
    List<DocumentSummary> getReusableDocuments(String projectCode, String materialCode);

    /**
     * Reuse existing documents for a new workflow
     */
    List<DocumentSummary> reuseDocuments(Long workflowId, List<Long> documentIds, String reuseBy);

    /**
     * Download a document
     */
    Resource downloadDocument(Long documentId);

    /**
     * Get document by ID
     */
    Document getDocumentById(Long documentId);

    /**
     * Delete a document
     */
    void deleteDocument(Long documentId, String deletedBy);

    /**
     * Validate file type and size
     */
    boolean isValidFile(MultipartFile file);

    /**
     * Get document count for workflow
     */
    long getDocumentCount(Long workflowId);

    /**
     * Download document with access control and logging
     */
    Resource downloadDocumentSecure(Long documentId, String userId, String ipAddress, String userAgent, Long workflowId);

    /**
     * Check if user has access to document
     */
    boolean hasDocumentAccess(Long documentId, String userId, Long workflowId);

    /**
     * Log document access attempt
     */
    void logDocumentAccess(Long documentId, String userId, DocumentAccessType accessType, 
                          String ipAddress, String userAgent, Long workflowId, 
                          boolean accessGranted, String denialReason);

    /**
     * Get document access logs
     */
    List<DocumentAccessLogDto> getDocumentAccessLogs(Long documentId);

    /**
     * Get enhanced document metadata with access statistics
     */
    DocumentSummary getEnhancedDocumentSummary(Long documentId);

    /**
     * Get reusable documents with enhanced metadata
     */
    List<DocumentSummary> getReusableDocumentsEnhanced(String projectCode, String materialCode);

    /**
     * Get document storage information for debugging
     */
    java.util.Map<String, Object> getStorageInfo();

    // ========== NEW UNIFIED DOCUMENT MANAGEMENT METHODS ==========

    /**
     * Upload documents with unified context support (workflow/query/response)
     * This method replaces the existing uploadDocuments method with enhanced context support
     */
    List<DocumentSummary> uploadDocuments(MultipartFile[] files, DocumentUploadContext context, String uploadedBy);

    /**
     * Reuse documents with unified context support (workflow/query)
     * This method replaces the existing reuseDocuments method with enhanced context support
     */
    List<DocumentSummary> reuseDocuments(DocumentReuseContext context, List<Long> documentIds, String reuseBy);

    /**
     * Get ALL documents for project/material combination across all sources (workflow, query, response)
     * This method enhances the existing getReusableDocuments to return documents from all sources
     */
    List<DocumentSummary> getAllDocumentsForProjectMaterial(String projectCode, String materialCode);

    /**
     * Search documents across all types with source filtering
     * Provides unified document search functionality across workflow, query, and response documents
     */
    List<DocumentSummary> searchDocuments(String searchTerm, String projectCode, String materialCode, List<DocumentSource> sources);

    /**
     * Get documents by query ID (both query and response documents)
     */
    List<DocumentSummary> getQueryDocuments(Long queryId);

    /**
     * Get documents by query ID and response ID (response-specific documents)
     */
    List<DocumentSummary> getQueryResponseDocuments(Long queryId, Long responseId);

    /**
     * Get documents by document source for filtering
     */
    List<DocumentSummary> getDocumentsBySource(DocumentSource documentSource);

    /**
     * Get documents by document source and project/material for comprehensive filtering
     */
    List<DocumentSummary> getDocumentsBySourceAndProjectMaterial(DocumentSource documentSource, String projectCode, String materialCode);

    /**
     * Check if user has access to document (unified access control)
     */
    boolean hasUnifiedDocumentAccess(Long documentId, String userId);

    /**
     * Get document count by query ID
     */
    long getQueryDocumentCount(Long queryId);

    /**
     * Get document count by document source
     */
    long getDocumentCountBySource(DocumentSource documentSource);

    /**
     * Get all documents related to a query (both query and response documents)
     */
    List<DocumentSummary> getAllQueryRelatedDocuments(Long queryId);

    /**
     * Search all documents across all types with unified results
     * Provides comprehensive search functionality with source categorization and metadata
     */
    com.cqs.qrmfg.dto.UnifiedDocumentSearchResult searchAllDocuments(String searchTerm, String projectCode, String materialCode, List<DocumentSource> sources);
}