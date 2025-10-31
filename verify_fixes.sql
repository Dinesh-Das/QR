-- Verification queries for Plant Questionnaire fixes

-- 1. Check current completion stats for materials showing 48/49 issue
SELECT 
    plant_code,
    material_code,
    total_fields,
    completed_fields,
    completion_percentage,
    completion_status,
    updated_at
FROM plant_specific_data 
WHERE completed_fields = 48 OR total_fields = 49
ORDER BY updated_at DESC;

-- 2. Check template field counts by step
SELECT 
    step_number,
    category,
    COUNT(*) as field_count,
    SUM(CASE WHEN responsible = 'CQS' THEN 1 ELSE 0 END) as cqs_fields,
    SUM(CASE WHEN responsible = 'Plant' THEN 1 ELSE 0 END) as plant_fields
FROM question_template 
WHERE is_active = true 
GROUP BY step_number, category 
ORDER BY step_number;

-- 3. Check for Process Safety Management fields
SELECT 
    step_number,
    category,
    field_name,
    question_text,
    responsible,
    is_active
FROM question_template 
WHERE category LIKE '%Process Safety%' OR category LIKE '%Safety Management%'
ORDER BY step_number, order_index;

-- 4. Check total template field count
SELECT 
    COUNT(*) as total_template_fields,
    SUM(CASE WHEN responsible = 'CQS' THEN 1 ELSE 0 END) as total_cqs_fields,
    SUM(CASE WHEN responsible = 'Plant' THEN 1 ELSE 0 END) as total_plant_fields
FROM question_template 
WHERE is_active = true;

-- 5. Check materials with completion issues
SELECT 
    psd.plant_code,
    psd.material_code,
    psd.total_fields,
    psd.completed_fields,
    psd.completion_percentage,
    w.state as workflow_state,
    w.material_name
FROM plant_specific_data psd
LEFT JOIN workflow w ON w.plant_code = psd.plant_code AND w.material_code = psd.material_code
WHERE psd.completion_percentage < 100 
  AND psd.completed_fields > 40  -- Focus on nearly complete ones
ORDER BY psd.completion_percentage DESC, psd.updated_at DESC
LIMIT 10;