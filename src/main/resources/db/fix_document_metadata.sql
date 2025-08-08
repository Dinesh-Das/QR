-- Fix document metadata for existing documents
-- This ensures that project_code and material_code are properly set

-- Update documents that have workflow_id but missing project/material codes
UPDATE QRMFG_DOCUMENTS 
SET 
    project_code = w.project_code,
    material_code = w.material_code,
    document_source = COALESCE(document_source, 'WORKFLOW')
FROM QRMFG_WORKFLOWS w 
WHERE QRMFG_DOCUMENTS.workflow_id = w.id 
  AND (QRMFG_DOCUMENTS.project_code IS NULL 
       OR QRMFG_DOCUMENTS.material_code IS NULL 
       OR QRMFG_DOCUMENTS.project_code = '' 
       OR QRMFG_DOCUMENTS.material_code = '');

-- Update documents that have query_id but missing project/material codes
UPDATE QRMFG_DOCUMENTS 
SET 
    project_code = w.project_code,
    material_code = w.material_code,
    document_source = CASE 
        WHEN QRMFG_DOCUMENTS.response_id IS NOT NULL THEN 'RESPONSE'
        ELSE 'QUERY'
    END
FROM QRMFG_QUERIES q
JOIN QRMFG_WORKFLOWS w ON q.workflow_id = w.id
WHERE QRMFG_DOCUMENTS.query_id = q.id 
  AND (QRMFG_DOCUMENTS.project_code IS NULL 
       OR QRMFG_DOCUMENTS.material_code IS NULL 
       OR QRMFG_DOCUMENTS.project_code = '' 
       OR QRMFG_DOCUMENTS.material_code = '');

-- Show the results
SELECT 
    'Documents with project/material codes' as description,
    COUNT(*) as count
FROM QRMFG_DOCUMENTS 
WHERE project_code IS NOT NULL 
  AND material_code IS NOT NULL 
  AND project_code != '' 
  AND material_code != '';

SELECT 
    'Documents missing project/material codes' as description,
    COUNT(*) as count
FROM QRMFG_DOCUMENTS 
WHERE project_code IS NULL 
   OR material_code IS NULL 
   OR project_code = '' 
   OR material_code = '';

-- Show sample documents with their metadata
SELECT 
    id,
    original_file_name,
    document_source,
    project_code,
    material_code,
    workflow_id,
    query_id,
    response_id
FROM QRMFG_DOCUMENTS 
ORDER BY uploaded_at DESC 
LIMIT 10;