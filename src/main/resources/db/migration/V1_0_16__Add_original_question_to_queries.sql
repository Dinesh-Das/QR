-- Add original_question column to QRMFG_QUERIES table
ALTER TABLE QRMFG_QUERIES ADD original_question VARCHAR2(2000);

-- Add comment to the new column
COMMENT ON COLUMN QRMFG_QUERIES.original_question IS 'Original questionnaire question that prompted this query';

-- Commit the changes
COMMIT;