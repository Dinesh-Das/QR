package com.cqs.qrmfg.service;

import com.cqs.qrmfg.dto.DocumentSummary;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * Service interface for document export operations
 */
public interface DocumentExportService {
    
    /**
     * Export multiple documents as a ZIP file
     * @param documents List of documents to export
     * @param exportName Name for the export file
     * @return Resource containing the ZIP file
     */
    Resource exportDocumentsAsZip(List<DocumentSummary> documents, String exportName);
    
    /**
     * Export workflow documents with metadata as ZIP
     * @param workflowId Workflow ID
     * @param includeQueryDocuments Whether to include related query documents
     * @return Resource containing the ZIP file with documents and metadata
     */
    Resource exportWorkflowDocuments(Long workflowId, boolean includeQueryDocuments);
    
    /**
     * Create a document manifest/report for export
     * @param documents List of documents
     * @param format Format for the manifest (CSV, JSON, XML)
     * @return String containing the manifest content
     */
    String createDocumentManifest(List<DocumentSummary> documents, String format);
    
    /**
     * Export workflows data to Excel format
     * @param workflows List of workflow summaries to export
     * @param exportName Name for the export file
     * @return Resource containing the Excel file
     */
    Resource exportWorkflowsToExcel(List<com.cqs.qrmfg.dto.WorkflowSummaryDto> workflows, String exportName);
}