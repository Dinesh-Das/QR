-- Simple version to add query document columns
-- Run this if the PostgreSQL version doesn't work

-- Add columns for query document support
ALTER TABLE QRMFG_DOCUMENTS ADD COLUMN IF NOT EXISTS query_id BIGINT;
ALTER TABLE QRMFG_DOCUMENTS ADD COLUMN IF NOT EXISTS response_id BIGINT;
ALTER TABLE QRMFG_DOCUMENTS ADD COLUMN IF NOT EXISTS document_source VARCHAR(20) DEFAULT 'WORKFLOW';
ALTER TABLE QRMFG_DOCUMENTS ADD COLUMN IF NOT EXISTS project_code VARCHAR(50);
ALTER TABLE QRMFG_DOCUMENTS ADD COLUMN IF NOT EXISTS material_code VARCHAR(50);

-- Add foreign key constraint
ALTER TABLE QRMFG_DOCUMENTS ADD CONSTRAINT IF NOT EXISTS fk_documents_query 
    FOREIGN KEY (query_id) REFERENCES QRMFG_QUERIES(id);

-- Update existing documents
UPDATE QRMFG_DOCUMENTS 
SET project_code = (SELECT project_code FROM QRMFG_WORKFLOWS WHERE id = QRMFG_DOCUMENTS.workflow_id),
    material_code = (SELECT material_code FROM QRMFG_WORKFLOWS WHERE id = QRMFG_DOCUMENTS.workflow_id)
WHERE project_code IS NULL OR material_code IS NULL;