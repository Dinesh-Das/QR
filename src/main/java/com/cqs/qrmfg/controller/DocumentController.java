package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.annotation.RequireRole;
import com.cqs.qrmfg.annotation.PlantDataFilter;
import com.cqs.qrmfg.dto.DocumentAccessLogDto;
import com.cqs.qrmfg.dto.DocumentReuseRequest;
import com.cqs.qrmfg.dto.DocumentReuseContext;
import com.cqs.qrmfg.dto.DocumentSummary;
import com.cqs.qrmfg.dto.DocumentUploadContext;
import com.cqs.qrmfg.dto.UnifiedDocumentSearchResult;
import com.cqs.qrmfg.enums.DocumentSource;
import com.cqs.qrmfg.enums.RoleType;
import com.cqs.qrmfg.exception.DocumentException;
import com.cqs.qrmfg.exception.DocumentNotFoundException;
import com.cqs.qrmfg.exception.DocumentValidationException;
import com.cqs.qrmfg.exception.DocumentAccessException;
import com.cqs.qrmfg.exception.DocumentStorageException;
import com.cqs.qrmfg.exception.InvalidDocumentContextException;
import com.cqs.qrmfg.exception.ErrorResponse;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.model.Document;
import com.cqs.qrmfg.model.Query;
import com.cqs.qrmfg.model.Workflow;
import com.cqs.qrmfg.repository.DocumentRepository;
import com.cqs.qrmfg.repository.QueryRepository;
import com.cqs.qrmfg.repository.WorkflowRepository;
import com.cqs.qrmfg.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for document management operations
 */
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private QueryRepository queryRepository;

    /**
     * Upload documents with unified context support (workflow/query/response) - JVC users only
     */
    @PostMapping("/upload")
    @RequireRole(RoleType.JVC_ROLE)
    public ResponseEntity<List<DocumentSummary>> uploadDocuments(
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(required = false) String contextType,
            @RequestParam(required = false) Long workflowId,
            @RequestParam(required = false) Long queryId,
            @RequestParam(required = false) Long responseId,
            // Legacy parameters for backward compatibility
            @RequestParam(required = false) String projectCode,
            @RequestParam(required = false) String materialCode,
            Authentication authentication) {
        
        System.out.println("=== UNIFIED DOCUMENT UPLOAD ENDPOINT REACHED ===");
        System.out.println("Files count: " + (files != null ? files.length : 0));
        System.out.println("Context Type: " + contextType);
        System.out.println("Workflow ID: " + workflowId);
        System.out.println("Query ID: " + queryId);
        System.out.println("Response ID: " + responseId);
        System.out.println("Legacy - Project Code: " + projectCode);
        System.out.println("Legacy - Material Code: " + materialCode);
        
        // Validate input parameters
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("No files provided for upload");
        }
        
        // Validate each file before processing
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new DocumentValidationException(file.getOriginalFilename(), "File is empty");
            }
            
            if (!documentService.isValidFile(file)) {
                throw new DocumentValidationException(file.getOriginalFilename(), 
                    "Invalid file type or size. Supported types: PDF, DOCX, XLSX, JPG, PNG. Maximum size: 25MB");
            }
        }
        
        String uploadedBy = getCurrentUsername(authentication);
        DocumentUploadContext context = createUploadContext(contextType, workflowId, queryId, responseId, projectCode, materialCode);
        
        List<DocumentSummary> uploadedDocuments = documentService.uploadDocuments(files, context, uploadedBy);
        
        System.out.println("Upload successful, returned " + uploadedDocuments.size() + " documents");
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadedDocuments);
    }

    /**
     * Download a document with access control and logging - All roles can access with plant filtering for PLANT_ROLE
     */
    @GetMapping("/{id}/download")
    @RequireRole({RoleType.ADMIN, RoleType.JVC_ROLE, RoleType.CQS_ROLE, RoleType.TECH_ROLE, RoleType.PLANT_ROLE})
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id,
            @RequestParam(required = false) Long workflowId,
            Authentication authentication,
            HttpServletRequest request) {
        
        System.out.println("=== DOCUMENT DOWNLOAD ENDPOINT REACHED ===");
        System.out.println("Document ID: " + id);
        System.out.println("Workflow ID: " + workflowId);
        System.out.println("User: " + getCurrentUsername(authentication));
        
        // Validate document ID
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid document ID: " + id);
        }
        
        Document document = documentService.getDocumentById(id);
        System.out.println("Found document: " + document.getOriginalFileName());
        System.out.println("File path: " + document.getFilePath());
        
        String userId = getCurrentUsername(authentication);
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        System.out.println("User ID: " + userId);
        System.out.println("IP Address: " + ipAddress);
        
        // Check access permissions before attempting download
        if (!documentService.hasDocumentAccess(id, userId, workflowId)) {
            throw new DocumentAccessException(id, userId, "download", 
                "User does not have permission to download this document");
        }
        
        Resource resource = documentService.downloadDocumentSecure(id, userId, ipAddress, userAgent, workflowId);
        
        // Validate resource before returning
        if (!resource.exists() || !resource.isReadable()) {
            throw new DocumentStorageException("download", document.getOriginalFileName(), 
                "Document file is not accessible or has been moved");
        }
        
        System.out.println("Resource obtained: " + resource.exists() + ", readable: " + resource.isReadable());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                       "attachment; filename=\"" + document.getOriginalFileName() + "\"")
                .body(resource);
    }

    /**
     * Get reusable documents for same project/material combination from ALL sources (workflow/query/response) - JVC users only
     */
    @GetMapping("/reusable")
    @RequireRole(RoleType.JVC_ROLE)
    @PlantDataFilter(entityField = "plantCode", filterType = PlantDataFilter.FilterType.LIST)
    public ResponseEntity<List<DocumentSummary>> getReusableDocuments(
            @RequestParam String projectCode, 
            @RequestParam String materialCode,
            @RequestParam(defaultValue = "false") boolean enhanced,
            @RequestParam(required = false) List<String> sources) {
        
        System.out.println("=== ENHANCED REUSABLE DOCUMENTS ENDPOINT REACHED ===");
        System.out.println("Project Code: " + projectCode);
        System.out.println("Material Code: " + materialCode);
        System.out.println("Enhanced: " + enhanced);
        System.out.println("Sources filter: " + sources);
        
        // Validate required parameters
        if (projectCode == null || projectCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Project code is required");
        }
        if (materialCode == null || materialCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Material code is required");
        }
        
        List<DocumentSummary> documents;
        
        if (sources != null && !sources.isEmpty()) {
            // Validate and filter by specific document sources
            List<DocumentSource> documentSources;
            try {
                documentSources = sources.stream()
                    .map(s -> DocumentSource.valueOf(s.toUpperCase()))
                    .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid document source. Valid sources: " + 
                    Arrays.toString(DocumentSource.values()));
            }
            documents = documentService.searchDocuments(null, projectCode, materialCode, documentSources);
        } else {
            // Get ALL documents from all sources
            documents = documentService.getAllDocumentsForProjectMaterial(projectCode, materialCode);
        }
        
        System.out.println("Found " + documents.size() + " reusable documents from all sources");
        return ResponseEntity.ok(documents);
    }

    /**
     * Reuse existing documents with unified context support (workflow/query) - JVC users only
     */
    @PostMapping("/reuse")
    @RequireRole(RoleType.JVC_ROLE)
    public ResponseEntity<List<DocumentSummary>> reuseDocuments(
            @Valid @RequestBody DocumentReuseRequest request,
            @RequestParam(required = false) String contextType,
            @RequestParam(required = false) Long workflowId,
            @RequestParam(required = false) Long queryId,
            Authentication authentication) {
        
        System.out.println("=== UNIFIED DOCUMENT REUSE ENDPOINT REACHED ===");
        System.out.println("Context Type: " + contextType);
        System.out.println("Workflow ID: " + workflowId);
        System.out.println("Query ID: " + queryId);
        System.out.println("Document IDs: " + request.getDocumentIds());
        
        // Validate request parameters
        if (request.getDocumentIds() == null || request.getDocumentIds().isEmpty()) {
            throw new IllegalArgumentException("No document IDs provided for reuse");
        }
        
        // Validate document IDs
        for (Long docId : request.getDocumentIds()) {
            if (docId == null || docId <= 0) {
                throw new IllegalArgumentException("Invalid document ID: " + docId);
            }
        }
        
        String reuseBy = getCurrentUsername(authentication);
        DocumentReuseContext context = createReuseContext(contextType, workflowId, queryId, request);
        
        // Verify all documents exist before attempting reuse
        for (Long docId : request.getDocumentIds()) {
            try {
                documentService.getDocumentById(docId);
            } catch (DocumentNotFoundException e) {
                throw new DocumentNotFoundException("Cannot reuse document with ID " + docId + ": " + e.getMessage());
            }
        }
        
        List<DocumentSummary> reusedDocuments = documentService.reuseDocuments(
            context, request.getDocumentIds(), reuseBy);
        
        System.out.println("Successfully reused " + reusedDocuments.size() + " documents");
        return ResponseEntity.ok(reusedDocuments);
    }

    /**
     * Get documents for a workflow - All roles can access with plant filtering for PLANT_ROLE
     */
    @GetMapping("/workflow/{workflowId}")
    @RequireRole({RoleType.ADMIN, RoleType.JVC_ROLE, RoleType.CQS_ROLE, RoleType.TECH_ROLE, RoleType.PLANT_ROLE})
    @PlantDataFilter(entityField = "plantCode", filterType = PlantDataFilter.FilterType.LIST)
    public ResponseEntity<List<DocumentSummary>> getWorkflowDocuments(@PathVariable Long workflowId) {
        // Validate workflow ID
        if (workflowId == null || workflowId <= 0) {
            throw new IllegalArgumentException("Invalid workflow ID: " + workflowId);
        }
        
        // Verify workflow exists
        workflowRepository.findById(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Workflow not found with ID: " + workflowId));
        
        List<DocumentSummary> documents = documentService.getWorkflowDocuments(workflowId);
        return ResponseEntity.ok(documents);
    }

    /**
     * Get document details by ID with optional enhanced metadata - All roles can access with plant filtering for PLANT_ROLE
     */
    @GetMapping("/{id}")
    @RequireRole({RoleType.ADMIN, RoleType.JVC_ROLE, RoleType.CQS_ROLE, RoleType.TECH_ROLE, RoleType.PLANT_ROLE})
    @PlantDataFilter(entityField = "plantCode", filterType = PlantDataFilter.FilterType.SINGLE)
    public ResponseEntity<DocumentSummary> getDocumentById(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean enhanced) {
        
        // Validate document ID
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid document ID: " + id);
        }
        
        DocumentSummary summary;
        if (enhanced) {
            summary = documentService.getEnhancedDocumentSummary(id);
        } else {
            Document document = documentService.getDocumentById(id);
            summary = new DocumentSummary(
                document.getId(),
                document.getFileName(),
                document.getOriginalFileName(),
                document.getFileType(),
                document.getFileSize(),
                document.getUploadedBy(),
                document.getUploadedAt(),
                document.getIsReused(),
                "/qrmfg/api/v1/documents/" + document.getId() + "/download"
            );
        }
        return ResponseEntity.ok(summary);
    }

    /**
     * Get document access logs - Admin and JVC users only
     */
    @GetMapping("/{id}/access-logs")
    @RequireRole({RoleType.ADMIN, RoleType.JVC_ROLE})
    public ResponseEntity<List<DocumentAccessLogDto>> getDocumentAccessLogs(@PathVariable Long id) {
        // Validate document ID
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid document ID: " + id);
        }
        
        // Verify document exists
        documentService.getDocumentById(id);
        
        List<DocumentAccessLogDto> accessLogs = documentService.getDocumentAccessLogs(id);
        return ResponseEntity.ok(accessLogs);
    }

    /**
     * Delete a document - JVC users and Admin only
     */
    @DeleteMapping("/{id}")
    @RequireRole({RoleType.ADMIN, RoleType.JVC_ROLE})
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id, Authentication authentication) {
        // Validate document ID
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid document ID: " + id);
        }
        
        // Verify document exists before attempting deletion
        Document document = documentService.getDocumentById(id);
        
        String deletedBy = getCurrentUsername(authentication);
        
        // Additional access control - users can only delete their own documents unless they're admin
        if (!document.getUploadedBy().equals(deletedBy) && 
            !authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
            throw new DocumentAccessException(id, deletedBy, "delete", 
                "You can only delete documents you uploaded");
        }
        
        documentService.deleteDocument(id, deletedBy);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get document count for a workflow - All roles can access with plant filtering for PLANT_ROLE
     */
    @GetMapping("/workflow/{workflowId}/count")
    @RequireRole({RoleType.ADMIN, RoleType.JVC_ROLE, RoleType.CQS_ROLE, RoleType.TECH_ROLE, RoleType.PLANT_ROLE})
    public ResponseEntity<Map<String, Long>> getDocumentCount(@PathVariable Long workflowId) {
        // Validate workflow ID
        if (workflowId == null || workflowId <= 0) {
            throw new IllegalArgumentException("Invalid workflow ID: " + workflowId);
        }
        
        // Verify workflow exists
        workflowRepository.findById(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Workflow not found with ID: " + workflowId));
        
        long count = documentService.getDocumentCount(workflowId);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all documents related to a query (both query and response documents) - All roles can access with plant filtering for PLANT_ROLE
     */
    @GetMapping("/queries/{queryId}")
    @RequireRole({RoleType.ADMIN, RoleType.JVC_ROLE, RoleType.CQS_ROLE, RoleType.TECH_ROLE, RoleType.PLANT_ROLE})
    @PlantDataFilter(entityField = "plantCode", filterType = PlantDataFilter.FilterType.LIST)
    public ResponseEntity<List<DocumentSummary>> getQueryDocuments(@PathVariable Long queryId) {
        System.out.println("=== GET QUERY DOCUMENTS ENDPOINT REACHED ===");
        System.out.println("Query ID: " + queryId);
        
        // Validate query ID
        if (queryId == null || queryId <= 0) {
            throw new IllegalArgumentException("Invalid query ID: " + queryId);
        }
        
        // Verify query exists
        Query query = queryRepository.findById(queryId)
            .orElseThrow(() -> new IllegalArgumentException("Query not found with ID: " + queryId));
        
        List<DocumentSummary> documents = documentService.getAllQueryRelatedDocuments(queryId);
        
        System.out.println("Found " + documents.size() + " documents for query " + queryId);
        return ResponseEntity.ok(documents);
    }

    /**
     * Get documents for a specific query response - All roles can access with plant filtering for PLANT_ROLE
     */
    @GetMapping("/queries/{queryId}/responses/{responseId}")
    @RequireRole({RoleType.ADMIN, RoleType.JVC_ROLE, RoleType.CQS_ROLE, RoleType.TECH_ROLE, RoleType.PLANT_ROLE})
    @PlantDataFilter(entityField = "plantCode", filterType = PlantDataFilter.FilterType.LIST)
    public ResponseEntity<List<DocumentSummary>> getQueryResponseDocuments(
            @PathVariable Long queryId, 
            @PathVariable Long responseId) {
        System.out.println("=== GET QUERY RESPONSE DOCUMENTS ENDPOINT REACHED ===");
        System.out.println("Query ID: " + queryId);
        System.out.println("Response ID: " + responseId);
        
        // Validate parameters
        if (queryId == null || queryId <= 0) {
            throw new IllegalArgumentException("Invalid query ID: " + queryId);
        }
        if (responseId == null || responseId <= 0) {
            throw new IllegalArgumentException("Invalid response ID: " + responseId);
        }
        
        // Verify query exists
        Query query = queryRepository.findById(queryId)
            .orElseThrow(() -> new IllegalArgumentException("Query not found with ID: " + queryId));
        
        List<DocumentSummary> documents = documentService.getQueryResponseDocuments(queryId, responseId);
        
        System.out.println("Found " + documents.size() + " documents for response " + responseId);
        return ResponseEntity.ok(documents);
    }

    /**
     * Get document count for a query - All roles can access with plant filtering for PLANT_ROLE
     */
    @GetMapping("/queries/{queryId}/count")
    @RequireRole({RoleType.ADMIN, RoleType.JVC_ROLE, RoleType.CQS_ROLE, RoleType.TECH_ROLE, RoleType.PLANT_ROLE})
    public ResponseEntity<Map<String, Long>> getQueryDocumentCount(@PathVariable Long queryId) {
        // Validate query ID
        if (queryId == null || queryId <= 0) {
            throw new IllegalArgumentException("Invalid query ID: " + queryId);
        }
        
        // Verify query exists
        queryRepository.findById(queryId)
            .orElseThrow(() -> new IllegalArgumentException("Query not found with ID: " + queryId));
        
        long count = documentService.getQueryDocumentCount(queryId);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    /**
     * Search documents across all types with filtering - All roles can access with plant filtering for PLANT_ROLE
     */
    @GetMapping("/search")
    @RequireRole({RoleType.ADMIN, RoleType.JVC_ROLE, RoleType.CQS_ROLE, RoleType.TECH_ROLE, RoleType.PLANT_ROLE})
    @PlantDataFilter(entityField = "plantCode", filterType = PlantDataFilter.FilterType.LIST)
    public ResponseEntity<List<DocumentSummary>> searchDocuments(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String projectCode,
            @RequestParam(required = false) String materialCode,
            @RequestParam(required = false) List<String> sources) {
        System.out.println("=== UNIFIED DOCUMENT SEARCH ENDPOINT REACHED ===");
        System.out.println("Search Term: " + searchTerm);
        System.out.println("Project Code: " + projectCode);
        System.out.println("Material Code: " + materialCode);
        System.out.println("Sources: " + sources);
        
        // Validate search parameters - at least one search criteria must be provided
        if ((searchTerm == null || searchTerm.trim().isEmpty()) && 
            (projectCode == null || projectCode.trim().isEmpty()) && 
            (materialCode == null || materialCode.trim().isEmpty()) && 
            (sources == null || sources.isEmpty())) {
            throw new IllegalArgumentException("At least one search parameter must be provided (searchTerm, projectCode, materialCode, or sources)");
        }
        
        List<DocumentSource> documentSources = null;
        if (sources != null && !sources.isEmpty()) {
            try {
                documentSources = sources.stream()
                    .map(s -> DocumentSource.valueOf(s.toUpperCase()))
                    .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid document source. Valid sources: " + 
                    Arrays.toString(DocumentSource.values()));
            }
        }
        
        List<DocumentSummary> documents = documentService.searchDocuments(
            searchTerm, projectCode, materialCode, documentSources);
        
        System.out.println("Found " + documents.size() + " documents matching search criteria");
        return ResponseEntity.ok(documents);
    }

    /**
     * Unified document search API with comprehensive results and metadata
     * Returns structured search results with source categorization and search metadata - All roles can access with plant filtering for PLANT_ROLE
     */
    @GetMapping("/search/unified")
    @RequireRole({RoleType.ADMIN, RoleType.JVC_ROLE, RoleType.CQS_ROLE, RoleType.TECH_ROLE, RoleType.PLANT_ROLE})
    @PlantDataFilter(entityField = "plantCode", filterType = PlantDataFilter.FilterType.AUTO)
    public ResponseEntity<com.cqs.qrmfg.dto.UnifiedDocumentSearchResult> searchAllDocuments(
            @RequestParam(required = false) String searchTerm,
            @RequestParam String projectCode,
            @RequestParam String materialCode,
            @RequestParam(required = false) List<String> sources) {
        System.out.println("=== UNIFIED DOCUMENT SEARCH API ENDPOINT REACHED ===");
        System.out.println("Search Term: " + searchTerm);
        System.out.println("Project Code: " + projectCode);
        System.out.println("Material Code: " + materialCode);
        System.out.println("Sources: " + sources);
        
        // Validate required parameters
        if (projectCode == null || projectCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Project code is required for unified search");
        }
        if (materialCode == null || materialCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Material code is required for unified search");
        }
        
        // Parse and validate document sources
        List<DocumentSource> documentSources = null;
        if (sources != null && !sources.isEmpty()) {
            try {
                documentSources = sources.stream()
                    .map(s -> DocumentSource.valueOf(s.toUpperCase()))
                    .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid document source. Valid sources: " + 
                    Arrays.toString(DocumentSource.values()));
            }
        }
        
        // Perform unified search
        com.cqs.qrmfg.dto.UnifiedDocumentSearchResult result = documentService.searchAllDocuments(
            searchTerm, projectCode, materialCode, documentSources);
        
        System.out.println("Unified search completed: " + result.getSearchMetadata().getTotalResults() + 
                          " total results in " + result.getSearchMetadata().getSearchTimeMs() + "ms");
        
        // Log results by source for debugging
        result.getDocumentsBySource().forEach((source, docs) -> 
            System.out.println("  " + source + ": " + docs.size() + " documents"));
        
        return ResponseEntity.ok(result);
    }

    /**
     * Validate file before upload - JVC users only
     */
    @PostMapping("/validate")
    @RequireRole(RoleType.JVC_ROLE)
    public ResponseEntity<Map<String, Object>> validateFile(@RequestParam("file") MultipartFile file) {
        // Validate input
        if (file == null) {
            throw new IllegalArgumentException("No file provided for validation");
        }
        
        if (file.isEmpty()) {
            throw new DocumentValidationException(file.getOriginalFilename(), "File is empty");
        }
        
        boolean isValid = documentService.isValidFile(file);
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("valid", isValid);
        response.put("fileName", file.getOriginalFilename());
        response.put("fileSize", file.getSize());
        response.put("fileType", getFileExtension(file.getOriginalFilename()));
        
        if (!isValid) {
            String errorMessage = "File validation failed. Check file type (PDF/DOCX/XLSX/JPG/PNG) and size (max 25MB)";
            response.put("error", errorMessage);
            response.put("validationDetails", getValidationDetails(file));
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get detailed validation information for a file
     */
    private Map<String, Object> getValidationDetails(MultipartFile file) {
        Map<String, Object> details = new java.util.HashMap<>();
        
        String fileName = file.getOriginalFilename();
        long fileSize = file.getSize();
        String fileType = getFileExtension(fileName);
        
        // Check file type
        List<String> allowedTypes = Arrays.asList("pdf", "docx", "xlsx", "jpg", "jpeg", "png");
        boolean validType = allowedTypes.contains(fileType.toLowerCase());
        details.put("validFileType", validType);
        details.put("allowedTypes", allowedTypes);
        
        // Check file size (25MB = 25 * 1024 * 1024 bytes)
        long maxSize = 25 * 1024 * 1024;
        boolean validSize = fileSize <= maxSize;
        details.put("validFileSize", validSize);
        details.put("maxSizeBytes", maxSize);
        details.put("maxSizeMB", 25);
        
        return details;
    }

    /**
     * Test endpoint to verify document storage configuration
     */
    @GetMapping("/test/storage-info")
    public ResponseEntity<Map<String, Object>> getStorageInfo() {
        Map<String, Object> info = documentService.getStorageInfo();
        return ResponseEntity.ok(info);
    }

    /**
     * Simple test endpoint for upload debugging
     */
    @PostMapping("/test/upload-debug")
    public ResponseEntity<Map<String, Object>> testUpload(
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "projectCode", required = false) String projectCode,
            @RequestParam(value = "materialCode", required = false) String materialCode,
            @RequestParam(value = "workflowId", required = false) String workflowId) {
        
        System.out.println("=== UPLOAD DEBUG TEST - Reached controller ===");
        System.out.println("Files: " + (files != null ? files.length : 0));
        System.out.println("Project: " + projectCode);
        System.out.println("Material: " + materialCode);
        System.out.println("Workflow: " + workflowId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Upload test endpoint reached");
        response.put("filesCount", files != null ? files.length : 0);
        response.put("projectCode", projectCode);
        response.put("materialCode", materialCode);
        response.put("workflowId", workflowId);
        
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                response.put("file" + i + "_name", files[i].getOriginalFilename());
                response.put("file" + i + "_size", files[i].getSize());
            }
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Simple GET endpoint to test controller accessibility
     */
    @GetMapping("/test/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        System.out.println("=== PING TEST ENDPOINT REACHED ===");
        Map<String, Object> response = new HashMap<>();
        response.put("message", "DocumentController is accessible");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * Simple POST endpoint to test POST requests
     */
    @PostMapping("/test/simple-post")
    public ResponseEntity<Map<String, Object>> simplePost(@RequestBody(required = false) Map<String, Object> body) {
        System.out.println("=== SIMPLE POST TEST ENDPOINT REACHED ===");
        System.out.println("Request body: " + body);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "POST request successful");
        response.put("receivedBody", body);
        return ResponseEntity.ok(response);
    }

    /**
     * Test endpoint to list all documents in storage
     */
    @GetMapping("/test/list-all")
    public ResponseEntity<List<Map<String, Object>>> listAllDocuments() {
        List<Document> allDocuments = documentRepository.findAll();
        List<Map<String, Object>> documentInfo = new java.util.ArrayList<>();
        
        for (Document doc : allDocuments) {
            Map<String, Object> info = new java.util.HashMap<>();
            info.put("id", doc.getId());
            info.put("originalFileName", doc.getOriginalFileName());
            info.put("fileName", doc.getFileName());
            info.put("filePath", doc.getFilePath());
            info.put("fileExists", java.nio.file.Files.exists(java.nio.file.Paths.get(doc.getFilePath())));
            info.put("workflowId", doc.getWorkflow().getId());
            info.put("projectCode", doc.getWorkflow().getProjectCode());
            info.put("materialCode", doc.getWorkflow().getMaterialCode());
            info.put("uploadedBy", doc.getUploadedBy());
            info.put("uploadedAt", doc.getUploadedAt());
            documentInfo.add(info);
        }
        
        return ResponseEntity.ok(documentInfo);
    }

    // Utility methods
    private String getCurrentUsername(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getUsername();
        }
        return "SYSTEM";
    }

    /**
     * Create DocumentUploadContext from request parameters
     */
    private DocumentUploadContext createUploadContext(String contextType, Long workflowId, Long queryId, 
                                                    Long responseId, String projectCode, String materialCode) {
        if (contextType == null) {
            // Legacy support - if no context type but workflowId provided, assume WORKFLOW
            if (workflowId != null) {
                contextType = "WORKFLOW";
            } else {
                throw new InvalidDocumentContextException("Context type is required");
            }
        }

        try {
            switch (contextType.toUpperCase()) {
                case "WORKFLOW":
                    if (workflowId == null) {
                        throw new InvalidDocumentContextException("WORKFLOW", "workflowId");
                    }
                    Workflow workflow = workflowRepository.findById(workflowId)
                        .orElseThrow(() -> new IllegalArgumentException("Workflow not found with ID: " + workflowId));
                    return DocumentUploadContext.forWorkflow(workflow);

                case "QUERY":
                    if (queryId == null) {
                        throw new InvalidDocumentContextException("QUERY", "queryId");
                    }
                    Query query = queryRepository.findById(queryId)
                        .orElseThrow(() -> new IllegalArgumentException("Query not found with ID: " + queryId));
                    return DocumentUploadContext.forQuery(query);

                case "RESPONSE":
                    if (queryId == null || responseId == null) {
                        throw new InvalidDocumentContextException("RESPONSE", "queryId, responseId");
                    }
                    Query responseQuery = queryRepository.findById(queryId)
                        .orElseThrow(() -> new IllegalArgumentException("Query not found with ID: " + queryId));
                    return DocumentUploadContext.forResponse(responseQuery, responseId);

                default:
                    throw new InvalidDocumentContextException("Invalid context type: " + contextType + 
                        ". Supported types: WORKFLOW, QUERY, RESPONSE");
            }
        } catch (Exception e) {
            if (e instanceof InvalidDocumentContextException || e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new InvalidDocumentContextException("Failed to create upload context: " + e.getMessage());
        }
    }

    /**
     * Create DocumentReuseContext from request parameters
     */
    private DocumentReuseContext createReuseContext(String contextType, Long workflowId, Long queryId, 
                                                  DocumentReuseRequest request) {
        if (contextType == null) {
            // Legacy support - if no context type but workflowId provided, assume WORKFLOW
            if (workflowId != null) {
                contextType = "WORKFLOW";
            } else {
                throw new InvalidDocumentContextException("Context type is required for document reuse");
            }
        }

        try {
            switch (contextType.toUpperCase()) {
                case "WORKFLOW":
                    if (workflowId == null) {
                        throw new InvalidDocumentContextException("WORKFLOW", "workflowId");
                    }
                    Workflow workflow = workflowRepository.findById(workflowId)
                        .orElseThrow(() -> new IllegalArgumentException("Workflow not found with ID: " + workflowId));
                    return DocumentReuseContext.forWorkflow(workflow);

                case "QUERY":
                    if (queryId == null) {
                        throw new InvalidDocumentContextException("QUERY", "queryId");
                    }
                    Query query = queryRepository.findById(queryId)
                        .orElseThrow(() -> new IllegalArgumentException("Query not found with ID: " + queryId));
                    return DocumentReuseContext.forQuery(query);

                default:
                    throw new InvalidDocumentContextException("Invalid context type for reuse: " + contextType + 
                        ". Supported types: WORKFLOW, QUERY");
            }
        } catch (Exception e) {
            if (e instanceof InvalidDocumentContextException || e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new InvalidDocumentContextException("Failed to create reuse context: " + e.getMessage());
        }
    }



    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    // Exception handlers with comprehensive error handling
    
    /**
     * Handle document validation errors with detailed information
     */
    @ExceptionHandler(DocumentValidationException.class)
    public ResponseEntity<ErrorResponse> handleDocumentValidation(DocumentValidationException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(java.time.LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Document Validation Error")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .build();
        
        System.err.println("Document validation error: " + ex.getMessage() + " for file: " + ex.getFileName());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle document access denied errors
     */
    @ExceptionHandler(DocumentAccessException.class)
    public ResponseEntity<ErrorResponse> handleDocumentAccess(DocumentAccessException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(java.time.LocalDateTime.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Document Access Denied")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .build();
        
        System.err.println("Document access denied: " + ex.getMessage() + " for document: " + ex.getDocumentId());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handle document storage errors
     */
    @ExceptionHandler(DocumentStorageException.class)
    public ResponseEntity<ErrorResponse> handleDocumentStorage(DocumentStorageException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(java.time.LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Document Storage Error")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .build();
        
        System.err.println("Document storage error: " + ex.getMessage() + " for operation: " + ex.getOperation());
        return ResponseEntity.internalServerError().body(errorResponse);
    }

    /**
     * Handle invalid document context errors
     */
    @ExceptionHandler(InvalidDocumentContextException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDocumentContext(InvalidDocumentContextException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(java.time.LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Invalid Document Context")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .build();
        
        System.err.println("Invalid document context: " + ex.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle document not found errors
     */
    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDocumentNotFound(DocumentNotFoundException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(java.time.LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Document Not Found")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .build();
        
        System.err.println("Document not found: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle general document errors
     */
    @ExceptionHandler(DocumentException.class)
    public ResponseEntity<ErrorResponse> handleDocumentException(DocumentException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(java.time.LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Document Error")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .build();
        
        System.err.println("Document error: " + ex.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle illegal argument errors with detailed validation information
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(java.time.LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Invalid Request Parameters")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .build();
        
        System.err.println("Invalid argument: " + ex.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            org.springframework.web.bind.MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        Map<String, String> fieldErrors = new java.util.HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            fieldErrors.put(error.getField(), error.getDefaultMessage()));
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(java.time.LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Error")
            .message("Request validation failed")
            .path(request.getRequestURI())
            .fieldErrors(fieldErrors)
            .build();
        
        System.err.println("Validation errors: " + fieldErrors);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle multipart file upload errors
     */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
            org.springframework.web.multipart.MaxUploadSizeExceededException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(java.time.LocalDateTime.now())
            .status(HttpStatus.PAYLOAD_TOO_LARGE.value())
            .error("File Too Large")
            .message("File size exceeds maximum allowed limit")
            .path(request.getRequestURI())
            .build();
        
        System.err.println("File size exceeded: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    /**
     * Handle security access denied errors
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(java.time.LocalDateTime.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Access Denied")
            .message("You do not have permission to perform this operation")
            .path(request.getRequestURI())
            .build();
        
        System.err.println("Access denied: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handle generic exceptions with proper logging and user-friendly messages
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(java.time.LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred while processing your request")
            .path(request.getRequestURI())
            .build();
        
        System.err.println("=== UNEXPECTED ERROR IN DOCUMENT CONTROLLER ===");
        System.err.println("Error type: " + ex.getClass().getSimpleName());
        System.err.println("Error message: " + ex.getMessage());
        System.err.println("Request URI: " + request.getRequestURI());
        ex.printStackTrace();
        
        return ResponseEntity.internalServerError().body(errorResponse);
    }
}