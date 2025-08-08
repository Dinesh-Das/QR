# Implementation Plan

## Implementation Guidelines
- **No Mock Data**: Do not use mock data in implementations. Use real data from the database and APIs.
- **No Test Files**: Do not create automated test files. Manual testing will be performed for application flow validation.
- **Real Integration**: Focus on real integration with existing systems and databases.
- **No Random Files**: Do not create random fix files, debug files, or temporary files during development. Keep the codebase clean and organized.
- **Console Logging Only**: Use console.log() for debugging purposes only. Do not create separate debug utilities, logging frameworks, or debug files.
- **Minimal File Creation**: Only create files that are explicitly required for the feature implementation. Avoid creating helper files, utility files, or configuration files unless absolutely necessary.

- [x] 1. Set up core workflow entities and database schema
  - Create MaterialWorkflow entity with projectCode, materialCode, plantCode, blockId fields
  - Create WorkflowDocument entity for document management with reuse capabilities
  - Update Query entity to support JVC as query target team
  - Add workflow state enums (WorkflowState, QueryTeam including JVC, QueryStatus)
  - Configure Hibernate Envers for audit trail support including document tracking
  - Create database migration scripts for enhanced tables and reference tables
  - _Requirements: 1.1, 1.6, 8.1, 9.1_

- [x] 2. Implement workflow state management service
  - Create WorkflowService with state transition logic and validation
  - Implement finite state machine pattern for workflow states
  - Add business rules for valid state transitions based on user roles
  - Create unit tests for state transition scenarios
  - _Requirements: 1.3, 2.4, 3.3_

- [x] 3. Build query management system
  - Implement QueryService for creating, assigning, and resolving queries
  - Add query validation logic and SLA tracking
  - Create query search and filtering capabilities
  - Write unit tests for query lifecycle management
  - _Requirements: 2.3, 3.1, 3.2, 3.4_

- [x] 4. Create enhanced workflow REST API controllers
  - Implement WorkflowController with endpoints for workflow CRUD operations and document access
  - Add role-based security annotations for JVC, Plant, CQS, Tech access & Admin should have all access
  - Create QueryController with query management endpoints including JVC query resolution
  - Create ProjectController for dropdown data APIs (projects, materials, plants, blocks)
  - Create DocumentController for file upload, download, and reuse operations
  - Implement proper error handling and validation for all new endpoints
  - _Requirements: 1.1, 1.3, 2.1, 3.1, 8.1, 9.1_

- [x] 5. Build notification service infrastructure
  - Create NotificationService for email/Slack integration
  - Implement notification templates for different workflow events
  - Add asynchronous notification processing with proper error handling
  - Create configuration for notification preferences
  - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [x] 5.1. Implement document management service
  - Create DocumentService for file upload, storage, and retrieval
  - Implement document reuse logic for same project/material combinations
  - Add file validation (type, size) and secure storage in app/{projectCode}/{materialCode}/ structure
  - Create document access control and download logging functionality
  - Implement document metadata management and audit trail
  - _Requirements: 1.4, 1.5, 8.1, 8.4, 9.1, 9.3, 9.4_

- [x] 5.2. Build project and reference data service
  - Create ProjectService using QRMFG_PROJECT_ITEM_MASTER (project_code, item_code)
  - Implement API endpoints for projects and materials from new master tables
  - Add plant/block endpoints using QRMFG_LOCATION_MASTER and QRMFG_BLOCK_MASTER
  - Implement dependent dropdown logic: materials filtered by project code, blocks by plant code
  - Integrate with QRMFG_QUESTIONNAIRE_MASTER for questionnaire template management
  - Create caching mechanism for reference data to improve performance
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 6. Implement enhanced JPA repositories and data access layer
  - Create WorkflowRepository with custom query methods for dashboard data and project/material filtering
  - Implement QueryRepository with filtering and search capabilities including JVC queries
  - Add QuestionnaireResponseRepository for form data persistence
  - Create WorkflowDocumentRepository for document management and reuse queries
  - Add QrmfgProjectItemMasterRepository for project/material dropdown data from QRMFG_PROJECT_ITEM_MASTER table
  - Add QrmfgLocationMasterRepository for plant dropdown data from QRMFG_LOCATION_MASTER table
  - Add QrmfgBlockMasterRepository for block dropdown data from QRMFG_BLOCK_MASTER table
  - Add QRMFGQuestionnaireMasterRepository for questionnaire templates from QRMFG_QUESTIONNAIRE_MASTER
  - Create repository integration tests with test data including document scenarios
  - _Requirements: 4.1, 4.2, 7.1, 8.1, 9.1_

- [x] 7. Use the react Application in frontend\src\App.js and create React components for workflow UI
  - Build WorkflowDashboard component with pending tasks panel
  - Implement QueryWidget with tabbed interface for different query views
  - Create AuditTimeline component for displaying workflow history
  - Add responsive design support for mobile/tablet access
  - _Requirements: 4.1, 4.2, 4.3, 7.1, 8.1_

- [x] 8. Build enhanced JVC workflow initiation interface
  - Create comprehensive material extension form with Project Code, Material Code, Plant Code, Block ID dropdowns
  - Implement dependent dropdown logic (Material depends on Project, Block depends on Plant)
  - Add multi-file document upload with validation (PDF/DOCX/XLSX, max 25MB per file)
  - Implement document reuse detection and selection for same project/material combinations
  - Create pending extensions list with enhanced filtering and project/material context

- [x] 9. Develop enhanced plant questionnaire system
  - Create dynamic multi-step form component with progress tracking (Step X/10)
  - Implement query raising modal with team selection including JVC and field context
  - Add draft save functionality with auto-recovery
  - Build context panel displaying JVC-provided material data and project/plant/block details
  - Implement document download functionality for JVC-uploaded files
  - Add secure document access with proper logging
  - _Requirements: 2.1, 2.2, 2.3, 2.5, 2.7, 8.1, 8.2, 8.3_

- [x] 10. Implement enhanced query resolution interface for CQS/Tech/JVC teams
  - Extend CQSView, TechView, and JVCView with query inbox functionality
  - Create filterable query table with enhanced material context (project, material, plant, block)
  - Build query resolution form with rich text response editor
  - Add query status tracking and SLA indicators with proper team routing
  - Implement query history and resolution tracking across all teams
  - _Requirements: 3.1, 3.2, 3.3, 3.5_

- [x] 11. Build admin monitoring and reporting features
  - Extend existing admin view with workflow monitoring dashboard
  - Implement query SLA reports with average resolution times
  - Add user/role management capabilities for workflow permissions
  - Create exportable audit logs with CSV download functionality
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [x] 12. Integrate notification system with workflow events
  - Connect NotificationService to workflow state transitions
  - Implement real-time notifications for query assignments and resolutions
  - Add email templates for different notification types
  - Create notification preference management in user settings
  - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [x] 13. Add comprehensive error handling and validation
  - Implement global exception handlers for workflow-specific errors
  - Add client-side validation for all forms with proper error messages
  - Create retry mechanisms for failed API calls
  - Implement offline state detection and request queuing
  - _Requirements: 2.6, 3.4, 4.5_

- [x] 14. Implement audit logging and history tracking
  - Configure Hibernate Envers for automatic audit trail generation
  - Create audit log viewing components with timeline visualization
  - Add version history for questionnaire responses
  - Implement read-only views for completed workflows
  - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [x] 15. Set up monitoring and performance optimization
  - Add application metrics for workflow processing times
  - Implement database query optimization for dashboard queries
  - Create performance monitoring for notification system
  - Add user activity analytics for workflow usage patterns
  - _Requirements: 4.4, 5.2_

- [x] 16. Final integration and system testing
  - Integrate all workflow components with existing QRMFG portal including document storage
  - Perform end-to-end testing of complete MSDS workflow scenarios with document reuse
  - Validate role-based access control across all workflow functions and document access
  - Test notification delivery and workflow state synchronization
  - Validate project/material/plant/block dropdown dependencies and data integrity
  - Test document upload, storage, download, and reuse functionality across different workflows
  - _Requirements: All requirements integration testing_