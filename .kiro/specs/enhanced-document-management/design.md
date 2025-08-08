# Enhanced Document Management Design

## Overview

This design document outlines the implementation approach for enhancing the document management system with automatic document reuse and query document attachment functionality. The solution builds upon the existing document infrastructure while adding new capabilities for improved efficiency and communication.

## Architecture

### Database Schema Enhancements

#### Enhanced QRMFG_DOCUMENTS Table
```sql
-- Add columns to existing QRMFG_DOCUMENTS table for unified document management
ALTER TABLE QRMFG_DOCUMENTS ADD (
    -- Query-related fields
    query_id NUMBER,
    response_id NUMBER,
    document_source VARCHAR2(20) DEFAULT 'WORKFLOW', -- 'WORKFLOW', 'QUERY', 'RESPONSE'
    
    -- Document reuse tracking
    is_reused BOOLEAN DEFAULT FALSE,
    original_document_id NUMBER,
    reuse_count NUMBER DEFAULT 0,
    
    -- Enhanced metadata
    project_code VARCHAR2(50),
    material_code VARCHAR2(50),
    
    -- Constraints
    CONSTRAINT fk_doc_query FOREIGN KEY (query_id) REFERENCES QRMFG_QUERIES(id),
    CONSTRAINT fk_doc_response FOREIGN KEY (response_id) REFERENCES QRMFG_QUERY_RESPONSES(id),
    CONSTRAINT fk_doc_original FOREIGN KEY (original_document_id) REFERENCES QRMFG_DOCUMENTS(id),
    CONSTRAINT chk_doc_source CHECK (document_source IN ('WORKFLOW', 'QUERY', 'RESPONSE'))
);

-- Add indexes for efficient querying
CREATE INDEX idx_documents_project_material ON QRMFG_DOCUMENTS(project_code, material_code);
CREATE INDEX idx_documents_query ON QRMFG_DOCUMENTS(query_id);
CREATE INDEX idx_documents_source ON QRMFG_DOCUMENTS(document_source);
CREATE INDEX idx_documents_reuse ON QRMFG_DOCUMENTS(original_document_id);
```

#### Enhanced QRMFG_WORKFLOWS Table
```sql
-- Add document reuse tracking
ALTER TABLE QRMFG_WORKFLOWS ADD (
    auto_reused_document_count NUMBER DEFAULT 0,
    document_reuse_source VARCHAR2(100),
    document_reuse_strategy VARCHAR2(50) DEFAULT 'AUTOMATIC'
);
```

### Backend Components

#### Enhanced WorkflowService for Document Reuse
```java
@Service
public class WorkflowServiceImpl implements WorkflowService {
    
    @Autowired
    private DocumentService documentService;
    
    @Override
    @Transactional
    public SmartExtensionResult extendToMultiplePlantsSmartly(
            String projectCode, String materialCode, 
            List<String> plantCodes, String initiatedBy) {
        
        // Get reusable documents once for efficiency
        List<DocumentSummary> reusableDocuments = 
            documentService.getReusableDocuments(projectCode, materialCode);
        
        for (String plantCode : plantCodes) {
            // Create workflow
            Workflow workflow = initiateEnhancedWorkflow(projectCode, materialCode, plantCode, initiatedBy);
            
            // Auto-reuse documents
            if (!reusableDocuments.isEmpty()) {
                List<Long> documentIds = reusableDocuments.stream()
                    .map(DocumentSummary::getId)
                    .collect(Collectors.toList());
                
                documentService.reuseDocuments(workflow.getId(), documentIds, initiatedBy);
                
                // Update workflow with reuse information
                workflow.setAutoReusedDocumentCount(documentIds.size());
                workflow.setDocumentReuseStrategy("AUTOMATIC");
                workflowRepository.save(workflow);
            }
        }
        
        return new SmartExtensionResult(createdWorkflows, skippedPlants, 
                                      failedPlants, existingWorkflows, reusableDocuments);
    }
}
```

#### Enhanced Document Entity
```java
@Entity
@Table(name = "QRMFG_DOCUMENTS")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    
    // Existing workflow relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id")
    private Workflow workflow;
    
    // New query relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id")
    private Query query;
    
    @Column(name = "response_id")
    private Long responseId;
    
    // Document source tracking
    @Enumerated(EnumType.STRING)
    @Column(name = "document_source", nullable = false)
    private DocumentSource documentSource = DocumentSource.WORKFLOW;
    
    // Enhanced metadata for better reuse
    @Column(name = "project_code", length = 50)
    private String projectCode;
    
    @Column(name = "material_code", length = 50)
    private String materialCode;
    
    // Document reuse tracking
    @Column(name = "is_reused")
    private Boolean isReused = false;
    
    @Column(name = "original_document_id")
    private Long originalDocumentId;
    
    @Column(name = "reuse_count")
    private Integer reuseCount = 0;
    
    // Existing fields...
    @Column(name = "file_name", nullable = false)
    private String fileName;
    
    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;
    
    // ... other existing fields
    
    // Getters and setters
}

public enum DocumentSource {
    WORKFLOW, QUERY, RESPONSE
}
```

#### Enhanced DocumentService
```java
@Service
public class DocumentServiceImpl implements DocumentService {
    
    // Enhanced reusable documents method
    @Override
    public List<DocumentSummary> getReusableDocuments(String projectCode, String materialCode) {
        // Get ALL documents (workflow, query, response) for the same project/material
        List<Document> documents = documentRepository.findByProjectCodeAndMaterialCode(
            projectCode, materialCode);
        
        return documents.stream()
            .map(this::convertToDocumentSummary)
            .collect(Collectors.toList());
    }
    
    // Unified document upload method
    @Override
    public List<DocumentSummary> uploadDocuments(
            List<MultipartFile> files, 
            String uploadedBy,
            DocumentUploadContext context) {
        
        List<DocumentSummary> uploadedDocs = new ArrayList<>();
        
        for (MultipartFile file : files) {
            Document document = new Document();
            
            // Set common fields
            document.setFileName(generateFileName(file));
            document.setOriginalFileName(file.getOriginalFilename());
            document.setUploadedBy(uploadedBy);
            document.setUploadedAt(LocalDateTime.now());
            
            // Set context-specific fields
            switch (context.getType()) {
                case WORKFLOW:
                    document.setWorkflow(context.getWorkflow());
                    document.setDocumentSource(DocumentSource.WORKFLOW);
                    document.setProjectCode(context.getWorkflow().getProjectCode());
                    document.setMaterialCode(context.getWorkflow().getMaterialCode());
                    break;
                    
                case QUERY:
                    document.setQuery(context.getQuery());
                    document.setDocumentSource(DocumentSource.QUERY);
                    // Get project/material from query's workflow
                    document.setProjectCode(context.getQuery().getWorkflow().getProjectCode());
                    document.setMaterialCode(context.getQuery().getWorkflow().getMaterialCode());
                    break;
                    
                case RESPONSE:
                    document.setQuery(context.getQuery());
                    document.setResponseId(context.getResponseId());
                    document.setDocumentSource(DocumentSource.RESPONSE);
                    document.setProjectCode(context.getQuery().getWorkflow().getProjectCode());
                    document.setMaterialCode(context.getQuery().getWorkflow().getMaterialCode());
                    break;
            }
            
            // Store file and save document
            String filePath = storeFile(file, document);
            document.setFilePath(filePath);
            
            Document savedDoc = documentRepository.save(document);
            uploadedDocs.add(convertToDocumentSummary(savedDoc));
        }
        
        return uploadedDocs;
    }
    
    // Enhanced document reuse method
    @Override
    public List<DocumentSummary> reuseDocuments(
            DocumentReuseContext context, 
            List<Long> documentIds, 
            String reuseBy) {
        
        List<DocumentSummary> reusedDocuments = new ArrayList<>();
        
        for (Long documentId : documentIds) {
            Document originalDocument = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
            
            Document reusedDocument = new Document();
            
            // Copy file information
            reusedDocument.setFileName(originalDocument.getFileName());
            reusedDocument.setOriginalFileName(originalDocument.getOriginalFileName());
            reusedDocument.setFilePath(originalDocument.getFilePath());
            reusedDocument.setFileType(originalDocument.getFileType());
            reusedDocument.setFileSize(originalDocument.getFileSize());
            
            // Set reuse information
            reusedDocument.setIsReused(true);
            reusedDocument.setOriginalDocumentId(documentId);
            reusedDocument.setUploadedBy(reuseBy);
            reusedDocument.setUploadedAt(LocalDateTime.now());
            
            // Set context-specific fields
            switch (context.getType()) {
                case WORKFLOW:
                    reusedDocument.setWorkflow(context.getWorkflow());
                    reusedDocument.setDocumentSource(DocumentSource.WORKFLOW);
                    reusedDocument.setProjectCode(context.getWorkflow().getProjectCode());
                    reusedDocument.setMaterialCode(context.getWorkflow().getMaterialCode());
                    break;
                    
                case QUERY:
                    reusedDocument.setQuery(context.getQuery());
                    reusedDocument.setDocumentSource(DocumentSource.QUERY);
                    reusedDocument.setProjectCode(context.getQuery().getWorkflow().getProjectCode());
                    reusedDocument.setMaterialCode(context.getQuery().getWorkflow().getMaterialCode());
                    break;
            }
            
            Document savedDoc = documentRepository.save(reusedDocument);
            
            // Update original document reuse count
            originalDocument.setReuseCount(originalDocument.getReuseCount() + 1);
            documentRepository.save(originalDocument);
            
            reusedDocuments.add(convertToDocumentSummary(savedDoc));
        }
        
        return reusedDocuments;
    }
}
```

### Frontend Components

#### Enhanced SmartExtensionResult Display
```javascript
const SmartExtensionResultModal = ({ result }) => {
  return (
    <Modal title="Smart Plant Extension Results" width={900}>
      {/* Existing summary and workflow sections */}
      
      {/* New Document Reuse Section */}
      {result.details.documentReuse && (
        <Card title="📎 Document Reuse Information" size="small">
          <Alert
            message={`${result.details.documentReuse.totalReusedDocuments} documents automatically reused`}
            description={result.details.documentReuse.reuseStrategy}
            type="success"
            showIcon
          />
          <List
            dataSource={result.details.documentReuse.reusedDocuments}
            renderItem={doc => (
              <List.Item>
                <List.Item.Meta
                  title={doc.originalFileName}
                  description={`${doc.fileType} • ${formatFileSize(doc.fileSize)} • From workflow ${doc.sourceWorkflowId}`}
                />
              </List.Item>
            )}
          />
        </Card>
      )}
    </Modal>
  );
};
```

#### QueryDocumentUpload Component
```javascript
const QueryDocumentUpload = ({ 
  queryId, 
  responseId, 
  onUploadComplete, 
  maxFiles = 5 
}) => {
  const [fileList, setFileList] = useState([]);
  const [uploading, setUploading] = useState(false);
  
  const handleUpload = async () => {
    setUploading(true);
    try {
      const formData = new FormData();
      fileList.forEach(file => {
        formData.append('files', file.originFileObj);
      });
      
      const endpoint = responseId 
        ? `/queries/${queryId}/responses/${responseId}/documents`
        : `/queries/${queryId}/documents`;
        
      const result = await queryAPI.uploadDocuments(endpoint, formData);
      onUploadComplete(result);
      setFileList([]);
    } catch (error) {
      message.error('Upload failed: ' + error.message);
    } finally {
      setUploading(false);
    }
  };
  
  return (
    <Upload.Dragger
      multiple
      fileList={fileList}
      onChange={({ fileList }) => setFileList(fileList)}
      beforeUpload={() => false} // Prevent auto upload
    >
      <p className="ant-upload-drag-icon">
        <InboxOutlined />
      </p>
      <p className="ant-upload-text">Click or drag files to upload</p>
      <p className="ant-upload-hint">
        Support PDF, DOCX, XLSX, JPG, PNG (max 25MB each)
      </p>
    </Upload.Dragger>
  );
};
```

#### UnifiedDocumentSearch Component
```javascript
const UnifiedDocumentSearch = ({ projectCode, materialCode }) => {
  const [searchResults, setSearchResults] = useState({
    workflowDocuments: [],
    queryDocuments: []
  });
  
  const handleSearch = async (searchTerm) => {
    const results = await documentAPI.searchAllDocuments(
      searchTerm, projectCode, materialCode
    );
    setSearchResults(results);
  };
  
  return (
    <div>
      <Search onSearch={handleSearch} placeholder="Search all documents..." />
      
      <Tabs>
        <TabPane tab={`Workflow Documents (${searchResults.workflowDocuments.length})`} key="workflow">
          <DocumentList documents={searchResults.workflowDocuments} type="workflow" />
        </TabPane>
        <TabPane tab={`Query Documents (${searchResults.queryDocuments.length})`} key="query">
          <DocumentList documents={searchResults.queryDocuments} type="query" />
        </TabPane>
      </Tabs>
    </div>
  );
};
```

## Data Models

### Enhanced SmartExtensionResult
```java
public class SmartExtensionResult {
    private List<Workflow> createdWorkflows;
    private List<String> skippedPlants;
    private List<String> failedPlants;
    private List<Workflow> existingWorkflows;
    private DocumentReuseInfo documentReuse;
    
    public static class DocumentReuseInfo {
        private int totalReusedDocuments;
        private List<DocumentSummary> reusedDocuments;
        private String reuseStrategy;
        private String sourceDescription;
    }
}
```

### Enhanced DocumentSummary DTO
```java
public class DocumentSummary {
    private Long id;
    private String fileName;
    private String originalFileName;
    private String fileType;
    private Long fileSize;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private String downloadUrl;
    
    // Enhanced fields for unified document management
    private DocumentSource documentSource; // WORKFLOW, QUERY, RESPONSE
    private String projectCode;
    private String materialCode;
    
    // Context-specific IDs
    private Long workflowId;
    private Long queryId;
    private Long responseId;
    
    // Reuse information
    private Boolean isReused;
    private Long originalDocumentId;
    private Integer reuseCount;
    
    // Source description for UI display
    private String sourceDescription; // "Workflow", "Query", "Response to Query #123"
}
```

### DocumentUploadContext and DocumentReuseContext
```java
public class DocumentUploadContext {
    private DocumentContextType type; // WORKFLOW, QUERY, RESPONSE
    private Workflow workflow;
    private Query query;
    private Long responseId;
    
    // Factory methods
    public static DocumentUploadContext forWorkflow(Workflow workflow) { ... }
    public static DocumentUploadContext forQuery(Query query) { ... }
    public static DocumentUploadContext forResponse(Query query, Long responseId) { ... }
}

public class DocumentReuseContext {
    private DocumentContextType type; // WORKFLOW, QUERY
    private Workflow workflow;
    private Query query;
    
    // Factory methods
    public static DocumentReuseContext forWorkflow(Workflow workflow) { ... }
    public static DocumentReuseContext forQuery(Query query) { ... }
}
```

## API Endpoints

### Enhanced Workflow Endpoints
```
POST   /qrmfg/api/v1/workflows/extend-to-plants
       // Enhanced to include document reuse information in response

GET    /qrmfg/api/v1/workflows/{id}/documents/all
       // Returns both workflow and related query documents
```

### Enhanced Document Endpoints
```
POST   /qrmfg/api/v1/documents/upload
       // Unified upload endpoint with context parameter (workflow/query/response)

GET    /qrmfg/api/v1/documents/reusable
       // Enhanced to return ALL documents for project/material (workflow, query, response)

GET    /qrmfg/api/v1/documents/search
       // Enhanced to search across all document types with source filtering

POST   /qrmfg/api/v1/documents/reuse
       // Enhanced to support reusing documents to any context (workflow/query)

GET    /qrmfg/api/v1/queries/{queryId}/documents
       // Get all documents related to a query (query + response documents)

GET    /qrmfg/api/v1/workflows/{workflowId}/documents/all
       // Get all documents related to a workflow (workflow + related query documents)
```

## Security Considerations

### Unified Access Control
- Consistent permission model across workflow and query documents
- Role-based access with proper inheritance
- Audit logging for all document operations

### File Security
- Shared file validation and virus scanning
- Consistent encryption and storage policies
- Unified quota management

## Integration Points

### Document Reuse Integration
- Automatic detection of reusable documents during workflow extension
- Seamless integration with existing document upload flows
- Consistent user experience across manual and automatic document attachment

### Query System Integration
- Natural extension of existing query functionality
- Consistent UI patterns with workflow document management
- Shared notification and audit systems

## Performance Considerations

### Document Reuse Optimization
- Cache reusable document queries for frequently used materials
- Batch document reuse operations for multiple workflows
- Optimize database queries for document lookup

### Query Document Performance
- Efficient pagination for large document lists
- Lazy loading of document metadata
- Optimized file serving for downloads

## Testing Strategy

### Comprehensive Test Coverage
- Unit tests for all new service methods
- Integration tests for document reuse workflows
- End-to-end tests for query document functionality
- Performance tests for large document operations
- Security tests for access control validation