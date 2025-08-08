-- V19__create_location_master.sql
-- Create Location Master table for plant/location management

-- Create the QRMFG_LOCATIONS table
CREATE TABLE QRMFG_LOCATIONS (
    location_code VARCHAR2(50) PRIMARY KEY,
    description VARCHAR2(200) NOT NULL,
    is_active NUMBER(1) DEFAULT 1 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR2(50) DEFAULT 'SYSTEM',
    updated_by VARCHAR2(50) DEFAULT 'SYSTEM'
);

-- Create indexes for better performance
CREATE INDEX idx_qrmfg_locations_active ON QRMFG_LOCATIONS(is_active);
CREATE INDEX idx_qrmfg_locations_desc ON QRMFG_LOCATIONS(description);

-- Insert sample location data
INSERT INTO QRMFG_LOCATIONS (location_code, description, is_active, created_at, updated_at, created_by, updated_by) 
VALUES ('PLANT001', 'Manufacturing Unit A - Mumbai', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM');

INSERT INTO QRMFG_LOCATIONS (location_code, description, is_active, created_at, updated_at, created_by, updated_by) 
VALUES ('PLANT002', 'Manufacturing Unit B - Delhi', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM');

INSERT INTO QRMFG_LOCATIONS (location_code, description, is_active, created_at, updated_at, created_by, updated_by) 
VALUES ('PLANT003', 'Manufacturing Unit C - Bangalore', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM');

INSERT INTO QRMFG_LOCATIONS (location_code, description, is_active, created_at, updated_at, created_by, updated_by) 
VALUES ('PLANT004', 'Manufacturing Unit D - Chennai', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM');

INSERT INTO QRMFG_LOCATIONS (location_code, description, is_active, created_at, updated_at, created_by, updated_by) 
VALUES ('PLANT005', 'Manufacturing Unit E - Pune', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM');

INSERT INTO QRMFG_LOCATIONS (location_code, description, is_active, created_at, updated_at, created_by, updated_by) 
VALUES ('PLANT006', 'Research & Development Center - Hyderabad', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM');

INSERT INTO QRMFG_LOCATIONS (location_code, description, is_active, created_at, updated_at, created_by, updated_by) 
VALUES ('PLANT007', 'Quality Control Lab - Kolkata', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM');

-- Add comments for documentation
COMMENT ON TABLE QRMFG_LOCATIONS IS 'Master table for plant/location codes and descriptions';
COMMENT ON COLUMN QRMFG_LOCATIONS.location_code IS 'Unique plant/location code identifier';
COMMENT ON COLUMN QRMFG_LOCATIONS.description IS 'Human-readable description of the plant/location';
COMMENT ON COLUMN QRMFG_LOCATIONS.is_active IS 'Flag to indicate if location is active (1) or inactive (0)';

COMMIT;