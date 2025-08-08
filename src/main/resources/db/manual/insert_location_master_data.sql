-- Manual script to insert location master data
-- Run this script directly in your Oracle database if the table exists but is empty

-- First, check if the table exists and create it if it doesn't
BEGIN
    EXECUTE IMMEDIATE 'CREATE TABLE QRMFG_LOCATIONS (
        LOCATION_CODE VARCHAR2(50) PRIMARY KEY,
        DESCRIPTION VARCHAR2(200) NOT NULL
    )';
    DBMS_OUTPUT.PUT_LINE('Table QRMFG_LOCATIONS created successfully');
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -955 THEN -- Table already exists
            DBMS_OUTPUT.PUT_LINE('Table QRMFG_LOCATIONS already exists');
        ELSE
            RAISE;
        END IF;
END;
/

-- Clear existing data (optional)
DELETE FROM QRMFG_LOCATIONS;

-- Insert sample location data
INSERT INTO QRMFG_LOCATIONS (LOCATION_CODE, DESCRIPTION) VALUES ('PLANT001', 'Manufacturing Unit A - Mumbai');
INSERT INTO QRMFG_LOCATIONS (LOCATION_CODE, DESCRIPTION) VALUES ('PLANT002', 'Manufacturing Unit B - Delhi');
INSERT INTO QRMFG_LOCATIONS (LOCATION_CODE, DESCRIPTION) VALUES ('PLANT003', 'Manufacturing Unit C - Bangalore');
INSERT INTO QRMFG_LOCATIONS (LOCATION_CODE, DESCRIPTION) VALUES ('PLANT004', 'Manufacturing Unit D - Chennai');
INSERT INTO QRMFG_LOCATIONS (LOCATION_CODE, DESCRIPTION) VALUES ('PLANT005', 'Manufacturing Unit E - Pune');
INSERT INTO QRMFG_LOCATIONS (LOCATION_CODE, DESCRIPTION) VALUES ('PLANT006', 'Research & Development Center - Hyderabad');
INSERT INTO QRMFG_LOCATIONS (LOCATION_CODE, DESCRIPTION) VALUES ('PLANT007', 'Quality Control Lab - Kolkata');
INSERT INTO QRMFG_LOCATIONS (LOCATION_CODE, DESCRIPTION) VALUES ('PLANT008', 'Warehouse & Distribution Center - Ahmedabad');
INSERT INTO QRMFG_LOCATIONS (LOCATION_CODE, DESCRIPTION) VALUES ('PLANT009', 'Testing & Validation Lab - Gurgaon');
INSERT INTO QRMFG_LOCATIONS (LOCATION_CODE, DESCRIPTION) VALUES ('PLANT010', 'Corporate Office - Mumbai');

-- Commit the changes
COMMIT;

-- Verify the data was inserted
SELECT COUNT(*) as TOTAL_LOCATIONS FROM QRMFG_LOCATIONS;
SELECT * FROM QRMFG_LOCATIONS ORDER BY LOCATION_CODE;

DBMS_OUTPUT.PUT_LINE('Location Master data inserted successfully');