# Enhanced Document Management Implementation Tasks

## Phase 1: Document Reuse Enhancement

- [x] 1. Backend Document Reuse Infrastructure





  - [x] 1.1 Enhance WorkflowService for automatic document reuse


    - Update extendToMultiplePlantsSmartly method to include document reuse logic
    - Add document reuse tracking fields to Workflow entity
    - Implement efficient document lookup for same project/material combinations
    - Add logging for document reuse operations
    - _Requirements: 1.1, 1.2, 1.3_

  - [x] 1.2 Update SmartExtensionResult to include document information


    - Add DocumentReuseInfo class to SmartExtensionResponseDto
    - Update controller to populate document reuse information
    - Enhance response structure to show reused document details
    - Add document count and source information to results
    - _Requirements: 1.4, 6.1, 6.2, 6.3_

- [x] 2. Frontend Document Reuse Display





  - [x] 2.1 Enhance MaterialExtensionForm smart extension result modal


    - Add document reuse information section to result display
    - Show list of automatically reused documents with metadata
    - Display document reuse strategy and source information
    - Add visual indicators for automatic vs manual document attachment
    - _Requirements: 6.1, 6.2, 6.3, 6.4_



  - [x] 2.2 Update JVCView workflow creation handling





    - Modify handleInitiateWorkflow to process document reuse information
    - Add success messages that include document reuse details
    - Update workflow creation notifications to mention auto-reused documents
    - Ensure proper error handling for document reuse failures
    - _Requirements: 1.4, 1.5, 6.1_

## Phase 2: Query Document Attachment

- [x] 3. Database Schema and Entity Enhancement





  - [x] 3.1 Enhance existing QRMFG_DOCUMENTS table


    - Add query_id, response_id, and document_source columns to existing table
    - Add project_code and material_code columns for better reuse capabilities
    - Add document reuse tracking fields (is_reused, original_document_id, reuse_count)
    - Create database migration script for schema changes with proper indexes
    - _Requirements: 2.1, 3.1, 4.1, 5.1_

  - [x] 3.2 Update Document entity and related entities


    - Enhance Document JPA entity with new query relationships and metadata fields
    - Add DocumentSource enum and DocumentUploadContext/DocumentReuseContext classes
    - Update Query entity to include document relationships using existing Document entity
    - Add proper cascade operations and fetch strategies for document cleanup
    - _Requirements: 2.2, 3.2, 4.2_
-

- [x] 4. Repository and Service Layer Enhancement



  - [x] 4.1 Enhance DocumentRepository with unified queries


    - Add findByProjectCodeAndMaterialCode method for comprehensive document reuse
    - Implement findByQueryId and findByQueryIdAndResponseId methods
    - Add findByDocumentSource method for filtering by document type
    - Create methods for document access control validation across all document types
    - _Requirements: 2.3, 3.3, 4.3, 5.2_



  - [x] 4.2 Enhance DocumentService with unified functionality

    - Update uploadDocuments method to support DocumentUploadContext (workflow/query/response)
    - Enhance reuseDocuments method to support DocumentReuseContext
    - Update getReusableDocuments to return ALL documents for project/material combination
    - Add unified document search across all document types with source filtering

    - _Requirements: 2.1, 2.2, 3.1, 5.1, 5.3_

  - [x] 4.3 Maintain existing security and validation

    - Ensure existing file validation utilities work with all document types
    - Apply consistent security model across workflow, query, and response documents
    - Implement unified audit logging for all document operations
    - Maintain shared file storage management with proper cleanup
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [x] 5. Controller Layer Enhancement



  - [x] 5.1 Enhance DocumentController with unified endpoints


    - Update POST /documents/upload to support DocumentUploadContext parameter
    - Enhance GET /documents/reusable to return all document types for project/material
    - Add GET /queries/{queryId}/documents endpoint for query-related documents
    - Update POST /documents/reuse to support DocumentReuseContext
    - _Requirements: 2.4, 3.4, 4.4, 5.2_

  - [x] 5.2 Maintain comprehensive error handling



    - Ensure existing file validation works with all document contexts
    - Implement proper HTTP status codes for different error scenarios
    - Add comprehensive error messages for client consumption
    - Create proper exception handling for all document operations
    - _Requirements: 2.2, 3.2, 5.2_

- [x] 6. Frontend Query Document Components





  - [x] 6.1 Create QueryDocumentUpload component


    - Implement file selection and upload functionality with drag-and-drop
    - Add progress indicators and upload status feedback
    - Create file validation with user-friendly error messages
    - Support both query creation and response attachment contexts
    - _Requirements: 2.1, 3.1_

  - [x] 6.2 Create QueryDocumentList component


    - Display all documents associated with a query
    - Group documents by original query vs response attachments
    - Implement download functionality with proper error handling
    - Add document metadata display (size, type, upload date, uploader)
    - _Requirements: 3.1, 3.2, 3.3, 4.1_

  - [x] 6.3 Create UnifiedDocumentSearch component


    - Implement search across both workflow and query documents
    - Add tabbed interface for different document types
    - Support filtering and sorting of search results
    - Integrate with existing document management patterns
    - _Requirements: 7.1, 7.2, 7.3_

## Phase 3: Integration and Enhancement

- [x] 7. Query UI Integration





  - [x] 7.1 Update QueryRaisingModal component


    - Add document upload section to query creation form
    - Integrate QueryDocumentUpload component
    - Update form submission to handle file uploads
    - Add validation to ensure form can be submitted with or without documents
    - _Requirements: 2.1, 2.2, 2.3_

  - [x] 7.2 Update QueryResponseEditor component


    - Add document upload section to response form
    - Integrate QueryDocumentUpload for response attachments
    - Update response submission to handle file uploads
    - Show existing query documents for context while responding
    - _Requirements: 3.1, 3.2, 3.3_

  - [x] 7.3 Update QueryInbox component


    - Add document attachment indicators to query list
    - Show document count badges for queries with attachments
    - Update query details view to include QueryDocumentList
    - Add filtering option for queries with/without attachments
    - _Requirements: 2.4, 4.1, 4.4_
-

- [x] 8. Unified Document Management



  - [x] 8.1 Create unified document search API


    - Implement searchAllDocuments method in DocumentService
    - Add UnifiedDocumentSearchResult DTO for combined results
    - Create REST endpoint for unified document search
    - Add proper access control for cross-document-type searches
    - _Requirements: 7.1, 7.2, 7.3_

  - [x] 8.2 Enhance workflow document views


    - Add option to view related query documents from workflow details
    - Update workflow document lists to show document sources
    - Integrate query document information in workflow reports
    - Add unified document export functionality
    - _Requirements: 7.2, 7.3, 7.4_
-

- [x] 9. API Integration and Services




  - [x] 9.1 Update queryAPI service


    - Add uploadQueryDocuments method for file uploads
    - Implement uploadResponseDocuments for response attachments
    - Create getQueryDocuments method for retrieving document lists
    - Add downloadQueryDocument method for secure downloads
    - _Requirements: 2.3, 3.3, 4.3_

  - [x] 9.2 Enhance workflowAPI service


    - Update extendToMultiplePlantsSmartly to handle document reuse information
    - Add methods for retrieving unified document information
    - Implement proper error handling for document operations
    - Add retry functionality for failed document operations
    - _Requirements: 1.4, 6.1, 7.1_
