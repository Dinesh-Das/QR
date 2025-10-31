-- Test script to verify CQS input fix
-- Run these queries to check the current state and verify the fix

-- 1. Check CQS data availability
SELECT material_code, sync_status, 
       CASE WHEN narcotic_listed IS NOT NULL THEN 1 ELSE 0 END +
       CASE WHEN flash_point_65 IS NOT NULL THEN 1 ELSE 0 END +
       CASE WHEN is_corrosive IS NOT NULL THEN 1 ELSE 0 END +
       CASE WHEN highly_toxic IS NOT NULL THEN 1 ELSE 0 END +
       CASE WHEN recommended_ppe IS NOT NULL THEN 1 ELSE 0 END AS populated_fields
FROM QRMFG_AUTO_CQS 
ORDER BY material_code;

-- 2. Check plant-specific data CQS inputs status
SELECT plant_code, material_code, cqs_sync_status, last_cqs_sync,
       CASE 
           WHEN cqs_inputs IS NULL THEN 'NULL'
           WHEN LENGTH(TRIM(cqs_inputs)) = 0 THEN 'EMPTY'
           WHEN TRIM(cqs_inputs) = '{}' THEN 'EMPTY_JSON'
           ELSE 'POPULATED'
       END as cqs_inputs_status,
       LENGTH(cqs_inputs) as cqs_inputs_length
FROM QRMFG_PLANT_SPECIFIC_DATA 
ORDER BY plant_code, material_code;

-- 3. Find records that need fixing (have CQS data but empty inputs)
SELECT psd.plant_code, psd.material_code, psd.cqs_sync_status,
       cqs.sync_status as cqs_data_status,
       CASE 
           WHEN psd.cqs_inputs IS NULL THEN 'NULL'
           WHEN LENGTH(TRIM(psd.cqs_inputs)) = 0 THEN 'EMPTY'
           WHEN TRIM(psd.cqs_inputs) = '{}' THEN 'EMPTY_JSON'
           ELSE 'POPULATED'
       END as cqs_inputs_status
FROM QRMFG_PLANT_SPECIFIC_DATA psd
JOIN QRMFG_AUTO_CQS cqs ON psd.material_code = cqs.material_code
WHERE (psd.cqs_inputs IS NULL 
       OR LENGTH(TRIM(psd.cqs_inputs)) = 0 
       OR TRIM(psd.cqs_inputs) = '{}')
  AND cqs.sync_status = 'ACTIVE'
ORDER BY psd.plant_code, psd.material_code;

-- 4. Count summary
SELECT 
    'Total Plant Records' as description,
    COUNT(*) as count
FROM QRMFG_PLANT_SPECIFIC_DATA
UNION ALL
SELECT 
    'Records with CQS Data Available' as description,
    COUNT(*) as count
FROM QRMFG_PLANT_SPECIFIC_DATA psd
JOIN QRMFG_AUTO_CQS cqs ON psd.material_code = cqs.material_code
WHERE cqs.sync_status = 'ACTIVE'
UNION ALL
SELECT 
    'Records with Empty CQS Inputs' as description,
    COUNT(*) as count
FROM QRMFG_PLANT_SPECIFIC_DATA psd
WHERE (psd.cqs_inputs IS NULL 
       OR LENGTH(TRIM(psd.cqs_inputs)) = 0 
       OR TRIM(psd.cqs_inputs) = '{}')
UNION ALL
SELECT 
    'Records Needing Fix' as description,
    COUNT(*) as count
FROM QRMFG_PLANT_SPECIFIC_DATA psd
JOIN QRMFG_AUTO_CQS cqs ON psd.material_code = cqs.material_code
WHERE (psd.cqs_inputs IS NULL 
       OR LENGTH(TRIM(psd.cqs_inputs)) = 0 
       OR TRIM(psd.cqs_inputs) = '{}')
  AND cqs.sync_status = 'ACTIVE';

-- 5. Sample CQS inputs content (after fix)
SELECT plant_code, material_code, 
       SUBSTR(cqs_inputs, 1, 100) as cqs_inputs_sample,
       cqs_sync_status, last_cqs_sync
FROM QRMFG_PLANT_SPECIFIC_DATA 
WHERE cqs_inputs IS NOT NULL 
  AND LENGTH(TRIM(cqs_inputs)) > 2
  AND TRIM(cqs_inputs) != '{}'
ORDER BY plant_code, material_code;