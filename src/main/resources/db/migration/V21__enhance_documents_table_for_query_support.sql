-- V21__enhance_documents_table_for_query_support.sql
-- Enhanced Document Management - Add query support and document reuse tracking to QRMFG_DOCUMENTS table
-- This migration enhances the existing QRMFG_DOCUMENTS table to support query document attachments

-- First, ensure the QRMFG_DOCUMENTS table exists (rename from QRMFG_WORKFLOW_DOCUMENTS if needed)
-- Check if QRMFG_WORKFLOW_DOCUMENTS exists and QRMFG_DOCUMENTS doesn't
DECLARE
    workflow_docs_count NUMBER;
    documents_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO workflow_docs_count FROM user_tables WHERE table_name = 'QRMFG_WORKFLOW_DOCUMENTS';
    SELECT COUNT(*) INTO documents_count FROM user_tables WHERE table_name = 'QRMFG_DOCUMENTS';
    
    -- If QRMFG_WORKFLOW_DOCUMENTS exists but QRMFG_DOCUMENTS doesn't, rename it
    IF workflow_docs_count > 0 AND documents_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE QRMFG_WORKFLOW_DOCUMENTS RENAME TO QRMFG_DOCUMENTS';
        
        -- Update sequence name if it exists
        EXECUTE IMMEDIATE 'DROP SEQUENCE WORKFLOW_DOCUMENT_SEQ';
        EXECUTE IMMEDIATE 'CREATE SEQUENCE QRMFG_DOCUMENTS_SEQ START WITH 1 INCREMENT BY 1';
    END IF;
END;
/

-- Add new columns to support query documents and enhanced document reuse
ALTER TABLE QRMFG_DOCUMENTS ADD (
    -- Query-related fields for document attachment to queries and responses
    query_id NUMBER,
    response_id NUMBER,
    document_source VARCHAR2(20) DEFAULT 'WORKFLOW' NOT NULL,
    
    -- Enhanced metadata for better document reuse across all contexts
    project_code VARCHAR2(50),
    material_code VARCHAR2(50),
    
    -- Enhanced document reuse tracking
    reuse_count NUMBER DEFAULT 0 NOT NULL
);

-- Add foreign key constraints
ALTER TABLE QRMFG_DOCUMENTS ADD CONSTRAINT fk_doc_query 
    FOREIGN KEY (query_id) REFERENCES QRMFG_QUERIES(id);

-- Add check constraint for document_source
ALTER TABLE QRMFG_DOCUMENTS ADD CONSTRAINT chk_doc_source 
    CHECK (document_source IN ('WORKFLOW', 'QUERY', 'RESPONSE'));

-- Create indexes for efficient querying
CREATE INDEX idx_documents_project_material ON QRMFG_DOCUMENTS(project_code, material_code);
CREATE INDEX idx_documents_query ON QRMFG_DOCUMENTS(query_id);
CREATE INDEX idx_documents_response ON QRMFG_DOCUMENTS(response_id);
CREATE INDEX idx_documents_source ON QRMFG_DOCUMENTS(document_source);
CREATE INDEX idx_documents_reuse_tracking ON QRMFG_DOCUMENTS(original_document_id, reuse_count);

-- Update existing documents with project_code and material_code from their workflows
UPDATE QRMFG_DOCUMENTS d 
SET project_code = (
    SELECT w.project_code 
    FROM QRMFG_WORKFLOWS w 
    WHERE w.id = d.workflow_id
),
material_code = (
    SELECT w.material_code 
    FROM QRMFG_WORKFLOWS w 
    WHERE w.id = d.workflow_id
)
WHERE d.workflow_id IS NOT NULL;

-- Update existing reused documents to have proper reuse_count
UPDATE QRMFG_DOCUMENTS 
SET reuse_count = 0 
WHERE reuse_count IS NULL;

-- Update reuse_count for original documents that have been reused
UPDATE QRMFG_DOCUMENTS original
SET reuse_count = (
    SELECT COUNT(*) 
    FROM QRMFG_DOCUMENTS reused 
    WHERE reused.original_document_id = original.id
)
WHERE original.id IN (
    SELECT DISTINCT original_document_id 
    FROM QRMFG_DOCUMENTS 
    WHERE original_document_id IS NOT NULL
);

-- Add comments for documentation
COMMENT ON COLUMN QRMFG_DOCUMENTS.query_id IS 'Foreign key to QRMFG_QUERIES for query-attached documents';
COMMENT ON COLUMN QRMFG_DOCUMENTS.response_id IS 'ID of the query response this document is attached to (if applicable)';
COMMENT ON COLUMN QRMFG_DOCUMENTS.document_source IS 'Source of document attachment: WORKFLOW, QUERY, or RESPONSE';
COMMENT ON COLUMN QRMFG_DOCUMENTS.project_code IS 'Project code for efficient document reuse lookup';
COMMENT ON COLUMN QRMFG_DOCUMENTS.material_code IS 'Material code for efficient document reuse lookup';
COMMENT ON COLUMN QRMFG_DOCUMENTS.reuse_count IS 'Number of times this document has been reused in other contexts';

-- Update table comment
COMMENT ON TABLE QRMFG_DOCUMENTS IS 'Unified document storage for workflow, query, and response attachments with reuse tracking';

-- Create audit table for the enhanced QRMFG_DOCUMENTS table
CREATE TABLE QRMFG_DOCUMENTS_AUD (
    id NUMBER,
    rev NUMBER,
    revtype NUMBER,
    workflow_id NUMBER,
    query_id NUMBER,
    response_id NUMBER,
    document_source VARCHAR2(20),
    project_code VARCHAR2(50),
    material_code VARCHAR2(50),
    file_name VARCHAR2(255),
    original_file_name VARCHAR2(255),
    file_path VARCHAR2(500),
    file_type VARCHAR2(10),
    file_size NUMBER,
    uploaded_by VARCHAR2(100),
    uploaded_at TIMESTAMP,
    is_reused NUMBER(1),
    original_document_id NUMBER,
    reuse_count NUMBER,
    created_by VARCHAR2(50),
    updated_by VARCHAR2(50),
    last_modified TIMESTAMP,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_documents_aud_rev FOREIGN KEY (rev) REFERENCES QRMFG_REVINFO(id)
);

-- Add index for audit table performance
CREATE INDEX idx_documents_aud_rev ON QRMFG_DOCUMENTS_AUD(rev);
CREATE INDEX idx_documents_aud_id ON QRMFG_DOCUMENTS_AUD(id);

COMMIT;