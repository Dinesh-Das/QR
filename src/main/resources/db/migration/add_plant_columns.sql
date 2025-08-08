-- Add plant assignment columns to QRMFG_USERS table
-- Run this script if you need to add the columns manually

-- Check if columns exist before adding them
BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE QRMFG_USERS ADD assigned_plants VARCHAR2(500)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN -- ORA-01430: column being added already exists in table
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE QRMFG_USERS ADD primary_plant VARCHAR2(50)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN -- ORA-01430: column being added already exists in table
            RAISE;
        END IF;
END;
/

-- Add some sample plant assignments for testing
-- Update existing users with plant assignments (optional)
UPDATE QRMFG_USERS 
SET assigned_plants = '["PLANT001"]', primary_plant = 'PLANT001' 
WHERE username = 'admin';

UPDATE QRMFG_USERS 
SET assigned_plants = '["PLANT001", "PLANT002"]', primary_plant = 'PLANT001' 
WHERE username LIKE 'plant%';

COMMIT;