-- Add step_title column to QRMFG_QUERIES table
ALTER TABLE QRMFG_QUERIES ADD step_title VARCHAR2(200);

-- Add comment to the new column
COMMENT ON COLUMN QRMFG_QUERIES.step_title IS 'Title of the questionnaire step for better context';

-- Update existing queries with default step titles based on step number
UPDATE QRMFG_QUERIES 
SET step_title = CASE 
    WHEN step_number = 0 THEN 'Material Information'
    WHEN step_number = 1 THEN 'Physical Properties'
    WHEN step_number = 2 THEN 'Chemical Properties'
    WHEN step_number = 3 THEN 'Safety Information'
    WHEN step_number = 4 THEN 'Regulatory Information'
    WHEN step_number = 5 THEN 'Environmental Information'
    WHEN step_number = 6 THEN 'Transportation Information'
    WHEN step_number = 7 THEN 'Additional Information'
    ELSE 'Step ' || step_number
END
WHERE step_number IS NOT NULL AND step_title IS NULL;

-- Commit the changes
COMMIT;