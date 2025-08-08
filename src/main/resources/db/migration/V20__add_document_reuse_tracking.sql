-- Add document reuse tracking fields to QRMFG_WORKFLOWS table
-- Migration for Enhanced Document Management - Document Reuse Infrastructure

-- Add document reuse tracking columns to workflows
ALTER TABLE QRMFG_WORKFLOWS ADD (
    auto_reused_document_count NUMBER DEFAULT 0,
    document_reuse_source VARCHAR2(100),
    document_reuse_strategy VARCHAR2(50) DEFAULT 'AUTOMATIC'
);

-- Add comments for documentation
COMMENT ON COLUMN QRMFG_WORKFLOWS.auto_reused_document_count IS 'Number of documents automatically reused during workflow creation';
COMMENT ON COLUMN QRMFG_WORKFLOWS.document_reuse_source IS 'Description of document reuse sources (e.g., WORKFLOW_DOCS:5, QUERY_DOCS:2)';
COMMENT ON COLUMN QRMFG_WORKFLOWS.document_reuse_strategy IS 'Strategy used for document reuse (AUTOMATIC, MANUAL, NONE)';

-- Update existing workflows to have default values
UPDATE QRMFG_WORKFLOWS 
SET auto_reused_document_count = 0,
    document_reuse_strategy = 'AUTOMATIC'
WHERE auto_reused_document_count IS NULL 
   OR document_reuse_strategy IS NULL;

-- Add constraints
ALTER TABLE QRMFG_WORKFLOWS ADD CONSTRAINT chk_doc_reuse_strategy 
    CHECK (document_reuse_strategy IN ('AUTOMATIC', 'MANUAL', 'NONE'));

-- Create index for performance on document reuse queries
CREATE INDEX idx_workflows_doc_reuse ON QRMFG_WORKFLOWS(auto_reused_document_count, document_reuse_strategy);