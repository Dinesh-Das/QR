# Implementation Plan

- [x] 1. Create core RBAC enums and constants


  - Create RoleType enum with the five predefined roles (ADMIN, JVC_ROLE, CQS_ROLE, TECH_ROLE, PLANT_ROLE)
  - Create RBACConstants class for system-wide constants and default configurations
  - Add validation methods for role types and plant codes
  - _Requirements: 1.1, 1.4_

- [ ] 2. Enhance existing Role and User models
  - [x] 2.1 Add role type validation to Role entity

    - Add RoleType enum field to Role entity
    - Create validation annotations for role names
    - Add helper methods for role type checking
    - Write unit tests for role validation logic
    - _Requirements: 1.1, 1.2_

  - [x] 2.2 Enhance User model with plant access methods





    - Add utility methods for plant assignment management
    - Create validation for plant code format
    - Add methods to check plant access permissions
    - Write unit tests for plant access helper methods
    - _Requirements: 6.1, 6.2, 6.5_

- [x] 3. Create plant access management infrastructure





  - [x] 3.1 Create PlantAccessService interface and implementation


    - Define PlantAccessService interface with plant management methods
    - Implement PlantAccessServiceImpl with database operations
    - Add methods to fetch plant codes from QRMFG_LOCATIONS table
    - Create plant assignment and validation logic
    - Write unit tests for plant access service
    - _Requirements: 6.1, 6.2, 6.5_

  - [x] 3.2 Create plant data filtering framework


    - Create PlantDataFilter interface for data filtering operations
    - Implement PlantDataFilterImpl with JPA Specification support
    - Add methods to create plant-based query filters
    - Create utility methods for plant code extraction from entities
    - Write unit tests for data filtering logic
    - _Requirements: 6.2, 6.3_

- [x] 4. Enhance role management services



  - [x] 4.1 Create enhanced RoleService interface and implementation


    - Define RoleService interface with RBAC-specific methods
    - Implement RoleServiceImpl with role assignment logic
    - Add methods for role validation and plant assignment
    - Create role hierarchy checking methods
    - Write unit tests for role service operations
    - _Requirements: 1.1, 1.2, 7.1, 7.2_


  - [x] 4.2 Create RBAC authorization service

    - Create RBACAuthorizationService interface for access control decisions
    - Implement authorization logic for screen and data access
    - Add methods to generate user-specific access lists
    - Create plant-based data filtering integration
    - Write unit tests for authorization decisions
    - _Requirements: 2.1, 3.1, 4.1, 5.1, 6.2, 6.3_

- [x] 5. Create custom authorization annotations





  - Create @RequireRole annotation for method-level security
  - Create @PlantDataFilter annotation for automatic data filtering
  - Implement annotation processing logic with Spring AOP
  - Add annotation validation and error handling
  - Write unit tests for annotation-based security
  - _Requirements: 2.1, 3.1, 4.1, 5.1, 6.2_

- [x] 6. Enhance security filter chain





  - [x] 6.1 Create enhanced RBAC authorization filter


    - Extend existing ScreenRoleAccessFilter with RBAC logic
    - Add role-based access validation for all five roles
    - Implement plant-based access control for PLANT_ROLE users
    - Add comprehensive access logging
    - Write integration tests for filter behavior
    - _Requirements: 2.1, 3.1, 4.1, 5.1, 6.2, 6.3, 8.1, 8.2_

  - [x] 6.2 Update SecurityConfig with RBAC enhancements


    - Modify SecurityConfig to use enhanced authorization filter
    - Add role-based URL pattern matching
    - Configure method-level security for annotations
    - Add RBAC-specific security configurations
    - Write integration tests for security configuration
    - _Requirements: 10.1, 10.2, 10.3_


- [x] 8. Create RBAC exception handling





  - Create custom RBAC exception classes (InsufficientRoleException, PlantAccessDeniedException)
  - Create RBACErrorResponse class for structured error responses
  - Implement global exception handler for RBAC exceptions
  - Add user-friendly error messages and guidance
  - Write unit tests for exception handling logic
  - _Requirements: 9.1, 9.2, 9.3_

- [ ] 9. Create role and plant management controllers










  - [x] 9.1 Create enhanced RoleController


    - Create REST endpoints for role management operations
    - Add endpoints for role assignment and plant assignment
    - Implement role validation and error handling
    - Add comprehensive input validation
    - Write unit tests for role controller endpoints
    - _Requirements: 1.1, 1.2, 6.1, 6.5_

  - [x] 9.2 Create PlantAccessController


    - Create REST endpoints for plant access management
    - Add endpoints for plant assignment and validation
    - Implement plant code fetching from QRMFG_LOCATIONS
    - Add plant access reporting endpoints
    - Write unit tests for plant access controller
    - _Requirements: 6.1, 6.2, 6.5_

- [x] 10. Enhance existing controllers with RBAC





  - [x] 10.1 Update DocumentController with role-based access


    - Add @RequireRole annotations to document endpoints
    - Implement plant-based document filtering for PLANT_ROLE users
    - Add role-specific document access validation
    - Update existing @PreAuthorize annotations to use new roles
    - Write integration tests for document access control
    - _Requirements: 2.1, 3.1, 4.1, 5.1, 6.2, 6.3_


  - [x] 10.2 Update ScreenRoleMappingController with RBAC

    - Enhance screen mapping endpoints with new role types
    - Add validation for role-screen mapping assignments
    - Implement role hierarchy checking in mappings
    - Add bulk role assignment capabilities
    - Write integration tests for screen mapping with RBAC
    - _Requirements: 1.1, 1.2, 7.1, 7.2_

- [ ] 12. Create comprehensive integration tests





  - [x] 12.1 Create role-based access integration tests


    - Test ADMIN access to all screens and data
    - Test JVC_ROLE access restricted to JVC screens only
    - Test CQS_ROLE access restricted to CQS screens only
    - Test TECH_ROLE access restricted to technical screens only
    - Test PLANT_ROLE access with plant-specific data filtering
    - _Requirements: 2.1, 3.1, 4.1, 5.1, 6.2, 6.3_

  - [x] 12.2 Create plant access integration tests


    - Test plant code assignment and validation
    - Test multi-plant user scenarios
    - Test plant data filtering across different entities
    - Test primary plant selection and usage
    - Test plant access audit logging
    - _Requirements: 6.1, 6.2, 6.3, 6.5, 8.1, 8.2_

- [-] 13. Create configuration and documentation


  - [x] 13.1 Create RBAC configuration properties



    - Add application.properties entries for RBAC settings
    - Create configuration classes for RBAC parameters
    - Add validation for configuration values
    - Create configuration documentation
    - _Requirements: 7.3, 10.1_

- [ ] 14. Performance optimization and caching
  - Implement role information caching in security context
  - Add plant assignment caching for PLANT_ROLE users
  - Optimize database queries for role and plant access checks
  - Create performance monitoring for RBAC operations
  - Write performance tests for RBAC system
  - _Requirements: 10.2, 10.4_

- [ ] 15. Final integration and testing
  - [ ] 15.1 End-to-end testing with all roles
    - Test complete user workflows for each role type
    - Validate role-based menu filtering in frontend integration
    - Test session management with role information
    - Verify audit logging across all operations
    - _Requirements: 2.1, 3.1, 4.1, 5.1, 6.2, 8.1, 10.3_

  - [ ] 15.2 Security validation and cleanup
    - Perform security testing for role escalation prevention
    - Validate all access control decisions
    - Test error handling and user feedback
    - Clean up any temporary code or debug statements
    - Create deployment checklist for RBAC system
    - _Requirements: 8.4, 9.1, 9.2, 9.3, 10.1_