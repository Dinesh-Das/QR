-- Check the current structure of QRMFG_DOCUMENTS table
-- Run this to see what columns exist

-- Check if the table exists and show its structure
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns 
WHERE table_name = 'qrmfg_documents' 
ORDER BY ordinal_position;

-- Check if there are any documents in the table
SELECT COUNT(*) as total_documents FROM QRMFG_DOCUMENTS;

-- Check documents by source
SELECT 
    document_source,
    COUNT(*) as count
FROM QRMFG_DOCUMENTS 
GROUP BY document_source;

-- Check documents with workflow associations
SELECT 
    COUNT(*) as documents_with_workflow
FROM QRMFG_DOCUMENTS 
WHERE workflow_id IS NOT NULL;

-- Check documents with query associations
SELECT 
    COUNT(*) as documents_with_query
FROM QRMFG_DOCUMENTS 
WHERE query_id IS NOT NULL;

-- Show sample documents
SELECT 
    id,
    original_file_name,
    document_source,
    workflow_id,
    query_id,
    project_code,
    material_code
FROM QRMFG_DOCUMENTS 
ORDER BY uploaded_at DESC 
LIMIT 10;