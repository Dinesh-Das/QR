-- Add query_id and response_id columns to QRMFG_DOCUMENTS table if they don't exist
-- This enables document attachment to queries and query responses

-- Check if query_id column exists, if not add it
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'qrmfg_documents' AND column_name = 'query_id') THEN
        ALTER TABLE QRMFG_DOCUMENTS ADD COLUMN query_id BIGINT;
        ALTER TABLE QRMFG_DOCUMENTS ADD CONSTRAINT fk_documents_query 
            FOREIGN KEY (query_id) REFERENCES QRMFG_QUERIES(id);
    END IF;
END $$;

-- Check if response_id column exists, if not add it
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'qrmfg_documents' AND column_name = 'response_id') THEN
        ALTER TABLE QRMFG_DOCUMENTS ADD COLUMN response_id BIGINT;
    END IF;
END $$;

-- Check if document_source column exists, if not add it
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'qrmfg_documents' AND column_name = 'document_source') THEN
        ALTER TABLE QRMFG_DOCUMENTS ADD COLUMN document_source VARCHAR(20) NOT NULL DEFAULT 'WORKFLOW';
    END IF;
END $$;

-- Check if project_code column exists, if not add it
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'qrmfg_documents' AND column_name = 'project_code') THEN
        ALTER TABLE QRMFG_DOCUMENTS ADD COLUMN project_code VARCHAR(50);
    END IF;
END $$;

-- Check if material_code column exists, if not add it
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'qrmfg_documents' AND column_name = 'material_code') THEN
        ALTER TABLE QRMFG_DOCUMENTS ADD COLUMN material_code VARCHAR(50);
    END IF;
END $$;

-- Update existing documents to have proper project_code and material_code from their workflows
UPDATE QRMFG_DOCUMENTS 
SET project_code = w.project_code, 
    material_code = w.material_code
FROM QRMFG_WORKFLOWS w 
WHERE QRMFG_DOCUMENTS.workflow_id = w.id 
  AND (QRMFG_DOCUMENTS.project_code IS NULL OR QRMFG_DOCUMENTS.material_code IS NULL);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_documents_query_id ON QRMFG_DOCUMENTS(query_id);
CREATE INDEX IF NOT EXISTS idx_documents_response_id ON QRMFG_DOCUMENTS(response_id);
CREATE INDEX IF NOT EXISTS idx_documents_source ON QRMFG_DOCUMENTS(document_source);
CREATE INDEX IF NOT EXISTS idx_documents_project_material ON QRMFG_DOCUMENTS(project_code, material_code);