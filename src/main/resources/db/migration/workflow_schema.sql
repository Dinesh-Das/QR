-- MSDS Workflow Database Schema
-- Oracle Database Migration Script

-- Create sequences for primary keys
CREATE SEQUENCE MATERIAL_WORKFLOW_SEQ START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE QUERY_SEQ START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE QUESTIONNAIRE_RESPONSE_SEQ START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE WORKFLOW_DOCUMENT_SEQ START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE QUESTIONNAIRE_MASTER_SEQ START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE REVINFO_SEQ START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE NOTIFICATION_PREF_SEQ START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SCREEN_ROLE_MAPPING_SEQ START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE RBAC_USER_SESSION_SEQ START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE RBAC_USER_SEQ START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE RBAC_AUDIT_LOG_SEQ START WITH 1 INCREMENT BY 1;

-- Questionnaire Master table
CREATE TABLE QRMFG_QUESTIONNAIRE_MASTER (
    id NUMBER PRIMARY KEY,
    sr_no NUMBER,
    checklist_text VARCHAR2(2000),
    comments VARCHAR2(2000),
    responsible VARCHAR2(100),
    question_id VARCHAR2(50),
    question_text VARCHAR2(1000),
    question_type VARCHAR2(50),
    step_number NUMBER,
    field_name VARCHAR2(100),
    is_required NUMBER(1) DEFAULT 0,
    options VARCHAR2(2000),
    validation_rules VARCHAR2(500),
    conditional_logic VARCHAR2(1000),
    depends_on_question_id VARCHAR2(50),
    help_text VARCHAR2(500),
    category VARCHAR2(100),
    order_index NUMBER,
    is_active NUMBER(1) DEFAULT 1,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR2(50),
    updated_by VARCHAR2(50)
);

-- Material Workflows table
CREATE TABLE QRMFG_MATERIAL_WORKFLOWS (
    id NUMBER PRIMARY KEY,
    project_code VARCHAR2(50) NOT NULL,
    material_code VARCHAR2(50) NOT NULL,
    plant_code VARCHAR2(50) NOT NULL,
    block_id VARCHAR2(50) NOT NULL,
    workflow_state VARCHAR2(20) NOT NULL,
    initiated_by VARCHAR2(100) NOT NULL,
    material_name VARCHAR2(200),
    material_description VARCHAR2(1000),
    safety_documents_path VARCHAR2(500),
    priority_level VARCHAR2(20) DEFAULT 'NORMAL',
    created_at TIMESTAMP NOT NULL,
    last_modified TIMESTAMP NOT NULL,
    extended_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_by VARCHAR2(50),
    updated_by VARCHAR2(50),
    CONSTRAINT uk_workflow_combination UNIQUE (project_code, material_code, plant_code, block_id)
);

-- Queries table
CREATE TABLE QRMFG_QUERIES (
    id NUMBER PRIMARY KEY,
    workflow_id NUMBER NOT NULL,
    question VARCHAR2(2000) NOT NULL,
    step_number NUMBER,
    field_name VARCHAR2(100),
    assigned_team VARCHAR2(20) NOT NULL,
    query_status VARCHAR2(20) DEFAULT 'OPEN' NOT NULL,
    response VARCHAR2(2000),
    raised_by VARCHAR2(100) NOT NULL,
    resolved_by VARCHAR2(100),
    priority_level VARCHAR2(20) DEFAULT 'NORMAL',
    query_category VARCHAR2(50),
    created_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP,
    last_modified TIMESTAMP NOT NULL,
    created_by VARCHAR2(50),
    updated_by VARCHAR2(50),
    CONSTRAINT fk_query_workflow FOREIGN KEY (workflow_id) REFERENCES QRMFG_MATERIAL_WORKFLOWS(id)
);

-- Questionnaire Responses table
CREATE TABLE QRMFG_QUESTIONNAIRE_RESPONSES (
    id NUMBER PRIMARY KEY,
    workflow_id NUMBER NOT NULL,
    step_number NUMBER NOT NULL,
    field_name VARCHAR2(100) NOT NULL,
    field_value VARCHAR2(2000),
    field_type VARCHAR2(50) DEFAULT 'TEXT',
    is_required NUMBER(1) DEFAULT 0,
    validation_status VARCHAR2(20) DEFAULT 'VALID',
    validation_message VARCHAR2(500),
    is_draft NUMBER(1) DEFAULT 0,
    section_name VARCHAR2(100),
    display_order NUMBER,
    last_modified TIMESTAMP NOT NULL,
    modified_by VARCHAR2(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR2(50),
    CONSTRAINT fk_response_workflow FOREIGN KEY (workflow_id) REFERENCES QRMFG_MATERIAL_WORKFLOWS(id)
);

-- Workflow Documents table
CREATE TABLE QRMFG_WORKFLOW_DOCUMENTS (
    id NUMBER PRIMARY KEY,
    workflow_id NUMBER NOT NULL,
    file_name VARCHAR2(255) NOT NULL,
    original_file_name VARCHAR2(255) NOT NULL,
    file_path VARCHAR2(500) NOT NULL,
    file_type VARCHAR2(10) NOT NULL,
    file_size NUMBER NOT NULL,
    uploaded_by VARCHAR2(100),
    uploaded_at TIMESTAMP NOT NULL,
    is_reused NUMBER(1) DEFAULT 0 NOT NULL,
    original_document_id NUMBER,
    created_by VARCHAR2(50),
    updated_by VARCHAR2(50),
    last_modified TIMESTAMP NOT NULL,
    CONSTRAINT fk_document_workflow FOREIGN KEY (workflow_id) REFERENCES QRMFG_MATERIAL_WORKFLOWS(id),
    CONSTRAINT fk_document_original FOREIGN KEY (original_document_id) REFERENCES QRMFG_WORKFLOW_DOCUMENTS(id)
);

-- Revision info table for Hibernate Envers
CREATE TABLE QRMFG_REVINFO (
    id NUMBER PRIMARY KEY,
    timestamp NUMBER NOT NULL,
    username VARCHAR2(100),
    revision_date TIMESTAMP
);

-- Notification Preferences table
CREATE TABLE QRMFG_NOTIFICATION_PREFERENCES (
    id NUMBER PRIMARY KEY,
    username VARCHAR2(100) NOT NULL,
    notification_type VARCHAR2(50) NOT NULL,
    channel VARCHAR2(20) NOT NULL,
    enabled NUMBER(1) DEFAULT 1 NOT NULL,
    email VARCHAR2(255),
    slack_id VARCHAR2(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR2(100),
    updated_by VARCHAR2(100)
);

-- Audit tables for Hibernate Envers
CREATE TABLE QRMFG_QUESTIONNAIRE_MASTER_AUD (
    id NUMBER,
    rev NUMBER,
    revtype NUMBER,
    sr_no NUMBER,
    checklist_text VARCHAR2(2000),
    comments VARCHAR2(2000),
    responsible VARCHAR2(100),
    question_id VARCHAR2(50),
    question_text VARCHAR2(1000),
    question_type VARCHAR2(50),
    step_number NUMBER,
    field_name VARCHAR2(100),
    is_required NUMBER(1),
    options VARCHAR2(2000),
    validation_rules VARCHAR2(500),
    conditional_logic VARCHAR2(1000),
    depends_on_question_id VARCHAR2(50),
    help_text VARCHAR2(500),
    category VARCHAR2(100),
    order_index NUMBER,
    is_active NUMBER(1),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR2(50),
    updated_by VARCHAR2(50),
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_questionnaire_master_aud_rev FOREIGN KEY (rev) REFERENCES QRMFG_REVINFO(id)
);

CREATE TABLE QRMFG_MATERIAL_WORKFLOWS_AUD (
    id NUMBER,
    rev NUMBER,
    revtype NUMBER,
    project_code VARCHAR2(50),
    material_code VARCHAR2(50),
    plant_code VARCHAR2(50),
    block_id VARCHAR2(50),
    workflow_state VARCHAR2(20),
    initiated_by VARCHAR2(100),
    material_name VARCHAR2(200),
    material_description VARCHAR2(1000),
    safety_documents_path VARCHAR2(500),
    priority_level VARCHAR2(20),
    created_at TIMESTAMP,
    last_modified TIMESTAMP,
    extended_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_by VARCHAR2(50),
    updated_by VARCHAR2(50),
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_workflow_aud_rev FOREIGN KEY (rev) REFERENCES QRMFG_REVINFO(id)
);

CREATE TABLE QRMFG_QUERIES_AUD (
    id NUMBER,
    rev NUMBER,
    revtype NUMBER,
    workflow_id NUMBER,
    question VARCHAR2(2000),
    step_number NUMBER,
    field_name VARCHAR2(100),
    assigned_team VARCHAR2(20),
    query_status VARCHAR2(20),
    response VARCHAR2(2000),
    raised_by VARCHAR2(100),
    resolved_by VARCHAR2(100),
    priority_level VARCHAR2(20),
    query_category VARCHAR2(50),
    created_at TIMESTAMP,
    resolved_at TIMESTAMP,
    last_modified TIMESTAMP,
    created_by VARCHAR2(50),
    updated_by VARCHAR2(50),
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_query_aud_rev FOREIGN KEY (rev) REFERENCES QRMFG_REVINFO(id)
);

CREATE TABLE QRMFG_QUESTIONNAIRE_RESPONSES_AUD (
    id NUMBER,
    rev NUMBER,
    revtype NUMBER,
    workflow_id NUMBER,
    step_number NUMBER,
    field_name VARCHAR2(100),
    field_value VARCHAR2(2000),
    field_type VARCHAR2(50),
    is_required NUMBER(1),
    validation_status VARCHAR2(20),
    validation_message VARCHAR2(500),
    is_draft NUMBER(1),
    section_name VARCHAR2(100),
    display_order NUMBER,
    last_modified TIMESTAMP,
    modified_by VARCHAR2(100),
    created_at TIMESTAMP,
    created_by VARCHAR2(50),
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_response_aud_rev FOREIGN KEY (rev) REFERENCES QRMFG_REVINFO(id)
);

CREATE TABLE QRMFG_WORKFLOW_DOCUMENTS_AUD (
    id NUMBER,
    rev NUMBER,
    revtype NUMBER,
    workflow_id NUMBER,
    file_name VARCHAR2(255),
    original_file_name VARCHAR2(255),
    file_path VARCHAR2(500),
    file_type VARCHAR2(10),
    file_size NUMBER,
    uploaded_by VARCHAR2(100),
    uploaded_at TIMESTAMP,
    is_reused NUMBER(1),
    original_document_id NUMBER,
    created_by VARCHAR2(50),
    updated_by VARCHAR2(50),
    last_modified TIMESTAMP,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_document_aud_rev FOREIGN KEY (rev) REFERENCES QRMFG_REVINFO(id)
);

-- Create indexes for better performance
CREATE INDEX idx_questionnaire_master_active ON qrmfg_questionnaire_master(is_active);
CREATE INDEX idx_questionnaire_master_step ON qrmfg_questionnaire_master(step_number);
CREATE INDEX idx_questionnaire_master_question_id ON qrmfg_questionnaire_master(question_id);
CREATE INDEX idx_questionnaire_master_category ON qrmfg_questionnaire_master(category);
CREATE INDEX idx_questionnaire_master_depends ON qrmfg_questionnaire_master(depends_on_question_id);

CREATE INDEX idx_material_workflow_state ON qrmfg_material_workflows(workflow_state);
CREATE INDEX idx_material_workflow_plant ON qrmfg_material_workflows(plant_code);
CREATE INDEX idx_material_workflow_project ON qrmfg_material_workflows(project_code);
CREATE INDEX idx_material_workflow_material ON qrmfg_material_workflows(material_code);
CREATE INDEX idx_material_workflow_created ON qrmfg_material_workflows(created_at);

CREATE INDEX idx_query_workflow ON qrmfg_queries(workflow_id);
CREATE INDEX idx_query_status ON qrmfg_queries(query_status);
CREATE INDEX idx_query_team ON qrmfg_queries(assigned_team);
CREATE INDEX idx_query_created ON qrmfg_queries(created_at);

CREATE INDEX idx_response_workflow ON qrmfg_questionnaire_responses(workflow_id);
CREATE INDEX idx_response_step ON qrmfg_questionnaire_responses(step_number);
CREATE INDEX idx_response_field ON qrmfg_questionnaire_responses(field_name);

CREATE INDEX idx_notification_pref_username ON qrmfg_notification_preferences(username);
CREATE INDEX idx_notification_pref_type ON qrmfg_notification_preferences(notification_type);
CREATE INDEX idx_notification_pref_enabled ON qrmfg_notification_preferences(enabled);

-- Create sequences for audit and user management
CREATE SEQUENCE AUDIT_LOG_SEQ START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE USER_SEQ START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE ROLE_SEQ START WITH 1 INCREMENT BY 1;

-- Audit Logs table for admin monitoring
CREATE TABLE QRMFG_AUDIT_LOGS (
    id NUMBER PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    username VARCHAR2(100) NOT NULL,
    action VARCHAR2(100) NOT NULL,
    entity_type VARCHAR2(50) NOT NULL,
    entity_id VARCHAR2(100),
    details VARCHAR2(2000),
    ip_address VARCHAR2(45),
    user_agent VARCHAR2(500),
    session_id VARCHAR2(100)
);

-- Users table for user management
CREATE TABLE QRMFG_USERS (
    id NUMBER PRIMARY KEY,
    username VARCHAR2(100) UNIQUE NOT NULL,
    email VARCHAR2(255) UNIQUE NOT NULL,
    first_name VARCHAR2(100),
    last_name VARCHAR2(100),
    department VARCHAR2(100),
    plant_assignment VARCHAR2(100),
    is_active NUMBER(1) DEFAULT 1 NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR2(100),
    updated_by VARCHAR2(100),
    last_login TIMESTAMP
);

-- Roles table for role-based access control
CREATE TABLE QRMFG_ROLES (
    id NUMBER PRIMARY KEY,
    name VARCHAR2(50) UNIQUE NOT NULL,
    description VARCHAR2(200),
    is_active NUMBER(1) DEFAULT 1 NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR2(100),
    updated_by VARCHAR2(100)
);

-- User-Role mapping table
CREATE TABLE QRMFG_USER_ROLES (
    user_id NUMBER NOT NULL,
    role_id NUMBER NOT NULL,
    assigned_at TIMESTAMP NOT NULL,
    assigned_by VARCHAR2(100),
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES QRMFG_USERS(id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES QRMFG_ROLES(id)
);

-- Screen Role Mapping table
CREATE TABLE QRMFG_SCREEN_ROLE_MAPPING (
    id NUMBER PRIMARY KEY,
    route VARCHAR2(255) NOT NULL,
    role_id NUMBER NOT NULL,
    CONSTRAINT fk_screen_role_mapping_role FOREIGN KEY (role_id) REFERENCES QRMFG_ROLES(id)
);

-- User Sessions table
CREATE TABLE QRMFG_USER_SESSIONS (
    id NUMBER PRIMARY KEY,
    user_id NUMBER NOT NULL,
    session_id VARCHAR2(255) UNIQUE NOT NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    last_activity_at TIMESTAMP,
    expires_at TIMESTAMP,
    ip_address VARCHAR2(50),
    user_agent VARCHAR2(500),
    status VARCHAR2(20) DEFAULT 'ACTIVE',
    active NUMBER(1) DEFAULT 1 NOT NULL,
    session_data VARCHAR2(1000),
    device_type VARCHAR2(20),
    location VARCHAR2(100),
    browser VARCHAR2(20),
    os VARCHAR2(20),
    CONSTRAINT fk_user_session_user FOREIGN KEY (user_id) REFERENCES QRMFG_USERS(id)
);

-- Create additional indexes for audit and user management
CREATE INDEX idx_audit_log_timestamp ON qrmfg_audit_logs(timestamp);
CREATE INDEX idx_audit_log_username ON qrmfg_audit_logs(username);
CREATE INDEX idx_audit_log_action ON qrmfg_audit_logs(action);
CREATE INDEX idx_audit_log_entity ON qrmfg_audit_logs(entity_type, entity_id);

CREATE INDEX idx_user_username ON qrmfg_users(username);
CREATE INDEX idx_user_email ON qrmfg_users(email);
CREATE INDEX idx_user_active ON qrmfg_users(is_active);
CREATE INDEX idx_user_plant ON qrmfg_users(plant_assignment);

CREATE INDEX idx_role_name ON qrmfg_roles(name);
CREATE INDEX idx_role_active ON qrmfg_roles(is_active);

CREATE INDEX idx_screen_role_mapping_route ON qrmfg_screen_role_mapping(route);
CREATE INDEX idx_screen_role_mapping_role ON qrmfg_screen_role_mapping(role_id);

CREATE INDEX idx_user_session_user ON qrmfg_user_sessions(user_id);
CREATE INDEX idx_user_session_id ON qrmfg_user_sessions(session_id);
CREATE INDEX idx_user_session_active ON qrmfg_user_sessions(active);
CREATE INDEX idx_user_session_expires ON qrmfg_user_sessions(expires_at);

-- Insert default roles for workflow management
INSERT INTO qrmfg_roles (id, name, description, is_active, created_at, updated_at, created_by) VALUES 
(ROLE_SEQ.NEXTVAL, 'ADMIN', 'System Administrator with full access', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM');

INSERT INTO qrmfg_roles (id, name, description, is_active, created_at, updated_at, created_by) VALUES 
(ROLE_SEQ.NEXTVAL, 'JVC_USER', 'JVC team member who can initiate workflows', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM');

INSERT INTO qrmfg_roles (id, name, description, is_active, created_at, updated_at, created_by) VALUES 
(ROLE_SEQ.NEXTVAL, 'PLANT_USER', 'Plant team member who completes questionnaires', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM');

INSERT INTO qrmfg_roles (id, name, description, is_active, created_at, updated_at, created_by) VALUES 
(ROLE_SEQ.NEXTVAL, 'CQS_USER', 'CQS team member who resolves queries', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM');

INSERT INTO qrmfg_roles (id, name, description, is_active, created_at, updated_at, created_by) VALUES 
(ROLE_SEQ.NEXTVAL, 'TECH_USER', 'Technology team member who resolves technical queries', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM');

-- Comments for documentation
COMMENT ON TABLE qrmfg_questionnaire_master IS 'Master template for questionnaire questions and structure';
COMMENT ON TABLE qrmfg_material_workflows IS 'Main workflow tracking table for MSDS materials';
COMMENT ON TABLE qrmfg_queries IS 'Queries raised during workflow processing';
COMMENT ON TABLE qrmfg_questionnaire_responses IS 'Plant team responses to questionnaire fields';
COMMENT ON TABLE qrmfg_audit_logs IS 'Audit trail for all system actions';
COMMENT ON TABLE qrmfg_users IS 'System users with workflow access';
COMMENT ON TABLE qrmfg_roles IS 'Role definitions for access control';
COMMENT ON TABLE qrmfg_user_roles IS 'User-role assignments';
COMMENT ON TABLE qrmfg_screen_role_mapping IS 'Screen/route access control by role';
COMMENT ON TABLE qrmfg_user_sessions IS 'Active user sessions for authentication';

COMMENT ON COLUMN qrmfg_questionnaire_master.question_type IS 'Question type: TEXT, SELECT, CHECKBOX, RADIO, TEXTAREA, NUMBER, DATE, BOOLEAN';
COMMENT ON COLUMN qrmfg_questionnaire_master.depends_on_question_id IS 'Question ID this question depends on for conditional display';
COMMENT ON COLUMN qrmfg_material_workflows.workflow_state IS 'Current state: JVC_PENDING, PLANT_PENDING, CQS_PENDING, TECH_PENDING, COMPLETED';
COMMENT ON COLUMN qrmfg_queries.assigned_team IS 'Team assigned to resolve query: CQS, TECH';
COMMENT ON COLUMN qrmfg_queries.query_status IS 'Query status: OPEN, RESOLVED';
COMMENT ON COLUMN qrmfg_questionnaire_responses.field_type IS 'Field type: TEXT, NUMBER, DATE, BOOLEAN, SELECT, TEXTAREA';
COMMENT ON COLUMN qrmfg_audit_logs.action IS 'Action performed: CREATE_WORKFLOW, RESOLVE_QUERY, RAISE_QUERY, COMPLETE_WORKFLOW, etc.';
COMMENT ON COLUMN qrmfg_roles.name IS 'Role name: ADMIN, JVC_USER, PLANT_USER, CQS_USER, TECH_USER';

-- Document Access Logs table for audit trail
CREATE SEQUENCE DOCUMENT_ACCESS_LOG_SEQ START WITH 1 INCREMENT BY 1;

CREATE TABLE QRMFG_DOCUMENT_ACCESS_LOGS (
    id NUMBER PRIMARY KEY,
    document_id NUMBER NOT NULL,
    accessed_by VARCHAR2(100) NOT NULL,
    access_type VARCHAR2(20) NOT NULL,
    access_time TIMESTAMP NOT NULL,
    ip_address VARCHAR2(45),
    user_agent VARCHAR2(500),
    workflow_id NUMBER,
    access_granted NUMBER(1) DEFAULT 1 NOT NULL,
    denial_reason VARCHAR2(255),
    CONSTRAINT fk_doc_access_document FOREIGN KEY (document_id) REFERENCES QRMFG_WORKFLOW_DOCUMENTS(id),
    CONSTRAINT fk_doc_access_workflow FOREIGN KEY (workflow_id) REFERENCES QRMFG_MATERIAL_WORKFLOWS(id)
);

-- Create indexes for better performance
CREATE INDEX idx_doc_access_document_id ON qrmfg_document_access_logs(document_id);
CREATE INDEX idx_doc_access_user ON qrmfg_document_access_logs(accessed_by);
CREATE INDEX idx_doc_access_time ON qrmfg_document_access_logs(access_time);
CREATE INDEX idx_doc_access_workflow ON qrmfg_document_access_logs(workflow_id);
CREATE INDEX idx_doc_access_type ON qrmfg_document_access_logs(access_type);

-- Add audit table for document access logs
CREATE TABLE QRMFG_DOCUMENT_ACCESS_LOGS_AUD (
    id NUMBER NOT NULL,
    rev NUMBER NOT NULL,
    revtype NUMBER(3),
    document_id NUMBER,
    accessed_by VARCHAR2(100),
    access_type VARCHAR2(20),
    access_time TIMESTAMP,
    ip_address VARCHAR2(45),
    user_agent VARCHAR2(500),
    workflow_id NUMBER,
    access_granted NUMBER(1),
    denial_reason VARCHAR2(255),
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_doc_access_log_aud_rev FOREIGN KEY (rev) REFERENCES QRMFG_REVINFO(id)
);

CREATE TABLE QRMFG_SCREEN_ROLE_MAPPING_AUD (
    id NUMBER,
    rev NUMBER,
    revtype NUMBER,
    route VARCHAR2(255),
    role_id NUMBER,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_screen_role_mapping_aud_rev FOREIGN KEY (rev) REFERENCES QRMFG_REVINFO(id)
);

CREATE TABLE QRMFG_USER_SESSIONS_AUD (
    id NUMBER,
    rev NUMBER,
    revtype NUMBER,
    user_id NUMBER,
    session_id VARCHAR2(255),
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    last_activity_at TIMESTAMP,
    expires_at TIMESTAMP,
    ip_address VARCHAR2(50),
    user_agent VARCHAR2(500),
    status VARCHAR2(20),
    active NUMBER(1),
    session_data VARCHAR2(1000),
    device_type VARCHAR2(20),
    location VARCHAR2(100),
    browser VARCHAR2(20),
    os VARCHAR2(20),
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_user_sessions_aud_rev FOREIGN KEY (rev) REFERENCES QRMFG_REVINFO(id)
);