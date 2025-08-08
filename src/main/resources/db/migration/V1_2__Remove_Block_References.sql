-- Remove block_id column from QRMFG_WORKFLOWS table
-- This migration removes all block-related functionality from the system

-- First, drop the unique constraint that includes block_id
ALTER TABLE QRMFG_WORKFLOWS DROP CONSTRAINT uk_workflow_combination;

-- Drop the block_id column
ALTER TABLE QRMFG_WORKFLOWS DROP COLUMN block_id;

-- Create new unique constraint without block_id
ALTER TABLE QRMFG_WORKFLOWS ADD CONSTRAINT uk_workflow_combination_no_block 
    UNIQUE (project_code, material_code, plant_code);

-- Drop block master table if it exists
DROP TABLE IF EXISTS qrmfg_block_master;

-- Drop block master sequence if it exists
DROP SEQUENCE IF EXISTS qrmfg_block_master_seq;

-- Add comment to document the change
COMMENT ON TABLE QRMFG_WORKFLOWS IS 'Material workflows table - block functionality removed in v1.2';