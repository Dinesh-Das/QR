-- QR Analytics Database Schema Updates
-- Add quality score and revision tracking to workflows table

-- Add quality score column if it doesn't exist
ALTER TABLE workflows 
ADD COLUMN IF NOT EXISTS quality_score DECIMAL(5,2) DEFAULT NULL,
ADD COLUMN IF NOT EXISTS revision_count INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS target_completion_hours INT DEFAULT 24;

-- Add indexes for QR Analytics performance
CREATE INDEX IF NOT EXISTS idx_workflows_plant_created ON workflows(plant_code, created_date);
CREATE INDEX IF NOT EXISTS idx_workflows_status_plant ON workflows(status, plant_code);
CREATE INDEX IF NOT EXISTS idx_workflows_completed_date ON workflows(completed_date);
CREATE INDEX IF NOT EXISTS idx_workflows_quality_score ON workflows(quality_score);

-- Create plants table if it doesn't exist
CREATE TABLE IF NOT EXISTS plants (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plant_code VARCHAR(50) UNIQUE NOT NULL,
    plant_name VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create user_plants table for plant access control
CREATE TABLE IF NOT EXISTS user_plants (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    plant_id BIGINT NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (plant_id) REFERENCES plants(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_plant (user_id, plant_id)
);

-- Insert default plants if they don't exist
INSERT IGNORE INTO plants (plant_code, plant_name, location) VALUES
('PLANT_A', 'Plant A - Mumbai', 'Mumbai, Maharashtra'),
('PLANT_B', 'Plant B - Chennai', 'Chennai, Tamil Nadu'),
('PLANT_C', 'Plant C - Bangalore', 'Bangalore, Karnataka'),
('PLANT_D', 'Plant D - Pune', 'Pune, Maharashtra');

-- Create production_targets table for target tracking
CREATE TABLE IF NOT EXISTS production_targets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plant_code VARCHAR(50) NOT NULL,
    target_month DATE NOT NULL,
    target_production_count INT NOT NULL,
    target_quality_score DECIMAL(5,2) DEFAULT 95.0,
    target_cycle_time_hours INT DEFAULT 24,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_plant_month (plant_code, target_month)
);

-- Insert sample production targets
INSERT IGNORE INTO production_targets (plant_code, target_month, target_production_count, target_quality_score, target_cycle_time_hours) VALUES
('PLANT_A', '2025-08-01', 1200, 95.0, 24),
('PLANT_B', '2025-08-01', 1100, 94.0, 26),
('PLANT_C', '2025-08-01', 1000, 96.0, 22),
('PLANT_D', '2025-08-01', 900, 93.0, 28);

-- Create workflow_state_history table for detailed tracking
CREATE TABLE IF NOT EXISTS workflow_state_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workflow_id BIGINT NOT NULL,
    from_state VARCHAR(50),
    to_state VARCHAR(50) NOT NULL,
    state_entered TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    state_exited TIMESTAMP NULL,
    duration_hours DECIMAL(10,2) GENERATED ALWAYS AS (
        CASE 
            WHEN state_exited IS NOT NULL 
            THEN TIMESTAMPDIFF(MICROSECOND, state_entered, state_exited) / 3600000000
            ELSE NULL 
        END
    ) STORED,
    changed_by VARCHAR(255),
    comments TEXT,
    FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE,
    INDEX idx_workflow_state_history_workflow (workflow_id),
    INDEX idx_workflow_state_history_state (to_state, state_entered)
);

-- Create quality_assessments table for detailed quality tracking
CREATE TABLE IF NOT EXISTS quality_assessments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workflow_id BIGINT NOT NULL,
    assessment_type VARCHAR(50) NOT NULL, -- 'INITIAL', 'REVISION', 'FINAL'
    quality_score DECIMAL(5,2) NOT NULL,
    assessment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assessed_by VARCHAR(255),
    defects_found INT DEFAULT 0,
    defect_categories JSON,
    comments TEXT,
    FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE,
    INDEX idx_quality_assessments_workflow (workflow_id),
    INDEX idx_quality_assessments_date (assessment_date),
    INDEX idx_quality_assessments_score (quality_score)
);

-- Update existing workflows with sample quality scores
UPDATE workflows 
SET quality_score = CASE 
    WHEN status = 'COMPLETED' THEN 
        CASE 
            WHEN RAND() < 0.7 THEN 95 + (RAND() * 5)  -- 70% excellent (95-100)
            WHEN RAND() < 0.9 THEN 85 + (RAND() * 10) -- 20% good (85-95)
            ELSE 70 + (RAND() * 15)                   -- 10% fair/poor (70-85)
        END
    WHEN status IN ('JVC_PENDING', 'PLANT_PENDING', 'CQS_PENDING', 'TECH_PENDING') THEN NULL
    ELSE 90 + (RAND() * 10)
END
WHERE quality_score IS NULL;

-- Update revision counts based on workflow history (if available)
UPDATE workflows w
SET revision_count = (
    SELECT COUNT(*) 
    FROM workflow_state_history wsh 
    WHERE wsh.workflow_id = w.id 
    AND wsh.to_state LIKE '%_REVISION%'
)
WHERE revision_count = 0;

-- Create indexes for performance optimization
CREATE INDEX IF NOT EXISTS idx_production_targets_plant_month ON production_targets(plant_code, target_month);
CREATE INDEX IF NOT EXISTS idx_user_plants_user ON user_plants(user_id);
CREATE INDEX IF NOT EXISTS idx_user_plants_plant ON user_plants(plant_id);

-- Add triggers to automatically update workflow_state_history
DELIMITER //

CREATE TRIGGER IF NOT EXISTS workflow_status_change_trigger
AFTER UPDATE ON workflows
FOR EACH ROW
BEGIN
    IF OLD.status != NEW.status THEN
        -- Close previous state
        UPDATE workflow_state_history 
        SET state_exited = NOW()
        WHERE workflow_id = NEW.id 
        AND to_state = OLD.status 
        AND state_exited IS NULL;
        
        -- Insert new state
        INSERT INTO workflow_state_history (workflow_id, from_state, to_state, changed_by)
        VALUES (NEW.id, OLD.status, NEW.status, USER());
    END IF;
END//

DELIMITER ;

-- Create a view for easy analytics queries
CREATE OR REPLACE VIEW qr_analytics_summary AS
SELECT 
    w.id as workflow_id,
    w.plant_code,
    w.status,
    w.quality_score,
    w.revision_count,
    w.created_date,
    w.completed_date,
    TIMESTAMPDIFF(HOUR, w.created_date, COALESCE(w.completed_date, NOW())) as cycle_time_hours,
    CASE 
        WHEN w.quality_score >= 95 THEN 'Excellent'
        WHEN w.quality_score >= 85 THEN 'Good'
        WHEN w.quality_score >= 70 THEN 'Fair'
        ELSE 'Poor'
    END as quality_category,
    pt.target_production_count,
    pt.target_quality_score,
    pt.target_cycle_time_hours
FROM workflows w
LEFT JOIN production_targets pt ON w.plant_code = pt.plant_code 
    AND DATE_FORMAT(w.created_date, '%Y-%m-01') = pt.target_month;

-- Grant necessary permissions (adjust as needed for your setup)
-- GRANT SELECT ON qr_analytics_summary TO 'qrmfg_app_user'@'%';
-- GRANT SELECT, INSERT, UPDATE ON workflow_state_history TO 'qrmfg_app_user'@'%';
-- GRANT SELECT, INSERT, UPDATE ON quality_assessments TO 'qrmfg_app_user'@'%';