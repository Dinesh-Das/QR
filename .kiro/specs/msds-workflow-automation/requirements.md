# Requirements Document

## Introduction

The MSDS Workflow Automation system is a comprehensive Spring Boot + React application that manages Material Safety Data Sheet (MSDS) workflows across multiple teams including JVC, Plant, CQS, and Technology teams. The system automates a manual process involving 4 different stakeholders for material safety data sheet completion. It provides role-based access control, dynamic questionnaires, query management, and workflow state tracking to streamline the MSDS completion process from initiation to completion with document reuse capabilities.

## Requirements

### Requirement 1

**User Story:** As a JVC team member, I want to initiate MSDS workflows for materials with comprehensive project and location details, so that I can start the safety documentation process and assign it to the appropriate plant team with all necessary context and documents.

#### Acceptance Criteria

1. WHEN a JVC user accesses the material extension screen THEN the system SHALL display a form with Project Code dropdown (populated via GET /api/projects), Material Code dropdown (dependent on project selection via GET /api/materials?project={projectCode}), Plant Code dropdown (from locations table), Block ID dropdown (filtered by plant code from blocks table), and Document Upload fields
2. WHEN a JVC user selects a project code THEN the system SHALL enable the Material Code dropdown and populate it with materials from the selected project
3. WHEN a JVC user selects a plant code THEN the system SHALL enable the Block ID dropdown and populate it with blocks from the selected plant
4. WHEN a JVC user uploads documents THEN the system SHALL store them in the directory structure app/{projectCode}/{materialCode}/ with proper file validation (PDF/DOCX/XLSX, max 25MB per file)
5. WHEN a JVC user selects the same project and material combination that has been previously extended THEN the system SHALL display existing documents with a "Reuse Documents" option (default checked) to avoid re-uploading
6. WHEN a JVC user submits a complete material extension form THEN the system SHALL create a new MaterialWorkflow entity with state JVC_PENDING and all selected metadata
7. WHEN a JVC user clicks "Extend to Plant" THEN the system SHALL transition the workflow state to PLANT_PENDING and notify the assigned plant team with project, material, plant, and block context
8. WHEN a JVC user views their dashboard THEN the system SHALL display a list of pending extensions with Material ID, Project Code, Plant Code, Block ID, Created Date, and SLA status

### Requirement 2

**User Story:** As a Plant team member, I want to complete multi-step questionnaires with the ability to raise queries, so that I can accurately fill out MSDS information while getting clarification when needed.

#### Acceptance Criteria

1. WHEN a Plant user accesses an assigned material THEN the system SHALL display a dynamic multi-step form with 10-20 fields grouped in logical sections
2. WHEN a Plant user is filling out the questionnaire THEN the system SHALL display a progress tracker showing current step (e.g., Step 3/10)
3. WHEN a Plant user clicks "Raise Query" on any step THEN the system SHALL open a modal allowing them to specify the field, target team (CQS/Tech/JVC), and question text
4. WHEN a Plant user submits a query THEN the system SHALL transition the workflow state to CQS_PENDING or JVC_PENDING or TECH_PENDING based on the selected team
5. WHEN a Plant user returns to a step with a resolved query THEN the system SHALL display the query response in an expandable card and auto-scroll to the queried step
6. WHEN a Plant user completes all questionnaire steps without pending queries THEN the system SHALL allow them to mark the material as completed
7. WHEN a Plant user saves progress THEN the system SHALL store draft responses and allow resumption later

### Requirement 3

**User Story:** As a CQS or Technology team member, I want to receive and resolve queries from plant teams, so that I can provide expert guidance and keep workflows moving forward.

#### Acceptance Criteria

1. WHEN a CQS/Tech user accesses their query inbox THEN the system SHALL display a filterable table with Material ID, Plant, Step, Question, and Days Open columns
2. WHEN a CQS/Tech user clicks on a query THEN the system SHALL display the original question, attachments, and material context
3. WHEN a CQS/Tech user submits a query response THEN the system SHALL transition the workflow state back to PLANT_PENDING and notify the plant team
4. WHEN a CQS/Tech user resolves a query THEN the system SHALL update the query status to RESOLVED and timestamp the resolution
5. IF a query remains unresolved for more than 3 days THEN the system SHALL display red urgency indicators

### Requirement 4

**User Story:** As any system user, I want to see a comprehensive dashboard with my pending tasks and material progress, so that I can prioritize my work and track overall system status.

#### Acceptance Criteria

1. WHEN any user accesses the dashboard THEN the system SHALL display a "Pending Tasks Panel" showing materials, current stage, action required by, and days pending
2. WHEN any user views the dashboard THEN the system SHALL display a "Query Widget" with tabs for "My Queries (Raised/Received)", "Unresolved", and "Resolved"
3. WHEN any user views material progress THEN the system SHALL display a visual pipeline showing JVC → Plant → [CQS/Tech] → Completion stages
4. WHEN a user has pending tasks THEN the system SHALL display badges on menu items (e.g., "Plant Tasks (3)")
5. IF any task is pending for more than 3 days THEN the system SHALL display color-coded urgency indicators

### Requirement 5

**User Story:** As an Admin user, I want to monitor workflows and generate reports, so that I can track system performance and identify bottlenecks.

#### Acceptance Criteria

1. WHEN an Admin accesses the monitoring dashboard THEN the system SHALL display workflow status across all materials and teams
2. WHEN an Admin requests query SLA reports THEN the system SHALL display average resolution times and pending counts
3. WHEN an Admin needs audit information THEN the system SHALL provide exportable CSV logs with full query history and timestamps
4. WHEN an Admin manages users THEN the system SHALL provide role assignment capabilities for JVC_USER, PLANT_USER, CQS_USER, TECH_USER, and ADMIN roles

### Requirement 6

**User Story:** As a system user, I want to receive notifications about workflow changes, so that I can respond promptly to new assignments and updates.

#### Acceptance Criteria

1. WHEN a workflow state changes THEN the system SHALL send email/Slack notifications to the newly assigned team
2. WHEN a query is raised THEN the system SHALL notify the assigned CQS/Tech team immediately
3. WHEN a query is resolved THEN the system SHALL notify the plant team that raised the query
4. WHEN a material workflow is completed THEN the system SHALL notify all stakeholders involved in the process

### Requirement 7

**User Story:** As a system user, I want to view complete audit logs and material history, so that I can track all changes and decisions made during the MSDS process.

#### Acceptance Criteria

1. WHEN a user views material details THEN the system SHALL display a timeline view of all material events with timestamps
2. WHEN any workflow state transition occurs THEN the system SHALL create an audit log entry with user, timestamp, and reason
3. WHEN questionnaire responses are modified THEN the system SHALL maintain version history of all changes
4. WHEN a user needs historical data THEN the system SHALL provide read-only views for completed materials

### Requirement 8

**User Story:** As a Plant team member, I want to access and download documents uploaded by JVC teams, so that I can review material specifications while completing questionnaires.

#### Acceptance Criteria

1. WHEN a Plant user accesses an assigned material workflow THEN the system SHALL display all documents uploaded by JVC team in a downloadable format
2. WHEN a Plant user clicks on a document THEN the system SHALL allow secure download with proper access logging
3. WHEN a Plant user views document details THEN the system SHALL display file name, upload date, file size, and uploader information
4. WHEN a Plant user attempts to access documents from unauthorized materials THEN the system SHALL deny access and log the attempt

### Requirement 9

**User Story:** As a JVC team member, I want to reuse previously uploaded documents for the same project and material combination, so that I can avoid redundant uploads and maintain consistency.

#### Acceptance Criteria

1. WHEN a JVC user selects a project and material combination that has existing documents THEN the system SHALL display a "Document Reuse" section showing available files
2. WHEN a JVC user views reusable documents THEN the system SHALL show file names, upload dates, and previous plant/block assignments
3. WHEN a JVC user selects "Reuse Documents" THEN the system SHALL copy document references to the new workflow without physical file duplication
4. WHEN a JVC user chooses to upload new documents instead of reusing THEN the system SHALL allow both options simultaneously

### Requirement 10

**User Story:** As a mobile user, I want to access critical workflow functions on tablets and mobile devices, so that I can work efficiently from the plant floor or remote locations.

#### Acceptance Criteria

1. WHEN a user accesses the system on a tablet THEN the system SHALL display a responsive interface optimized for touch interaction
2. WHEN a plant user needs to complete questionnaires on mobile THEN the system SHALL provide an accessible form interface with appropriate input controls
3. WHEN a user views dashboards on mobile THEN the system SHALL prioritize critical information and maintain usability
4. WHEN mobile users need to raise queries THEN the system SHALL provide simplified query creation workflows