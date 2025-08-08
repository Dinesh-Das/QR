package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.annotation.RequireRole;
import com.cqs.qrmfg.annotation.PlantDataFilter;
import com.cqs.qrmfg.dto.DocumentSummary;
import com.cqs.qrmfg.dto.SmartExtensionResponseDto;
import com.cqs.qrmfg.dto.WorkflowCreateRequest;
import com.cqs.qrmfg.dto.WorkflowSummaryDto;
import com.cqs.qrmfg.enums.RoleType;
import com.cqs.qrmfg.exception.WorkflowException;
import com.cqs.qrmfg.exception.WorkflowNotFoundException;
import com.cqs.qrmfg.model.Workflow;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.model.WorkflowState;
import com.cqs.qrmfg.service.DocumentService;
import com.cqs.qrmfg.service.WorkflowService;
import com.cqs.qrmfg.util.WorkflowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowController.class);

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowMapper workflowMapper;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private com.cqs.qrmfg.service.DocumentExportService documentExportService;

    @Autowired
    private com.cqs.qrmfg.service.ExcelReportService excelReportService;

    // Basic CRUD operations
    @GetMapping
    @RequireRole({RoleType.ADMIN, RoleType.JVC_ROLE, RoleType.CQS_ROLE, RoleType.TECH_ROLE, RoleType.PLANT_ROLE})
    @PlantDataFilter(entityField = "plantCode", filterType = PlantDataFilter.FilterType.LIST)
    @Transactional(readOnly = true)
    public ResponseEntity<List<WorkflowSummaryDto>> getAllWorkflows() {
        List<Workflow> workflows = workflowService.findAll();
        List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
        return ResponseEntity.ok(workflowDtos);
    }

    @GetMapping("/{id}")
    @RequireRole({RoleType.ADMIN, RoleType.JVC_ROLE, RoleType.CQS_ROLE, RoleType.TECH_ROLE, RoleType.PLANT_ROLE})
    @PlantDataFilter(entityField = "plantCode", filterType = PlantDataFilter.FilterType.SINGLE, plantRoleOnly = true)
    @Transactional(readOnly = true)
    public ResponseEntity<WorkflowSummaryDto> getWorkflowById(@PathVariable Long id) {
        Optional<Workflow> workflow = workflowService.findById(id);
        if (workflow.isPresent()) {
            Workflow w = workflow.get();
            // Initialize collections within transaction
            w.getQueries().size();
            w.getDocuments().size();
            WorkflowSummaryDto dto = workflowMapper.toSummaryDto(w);
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/material/{materialCode}")
    @RequireRole({RoleType.ADMIN, RoleType.JVC_ROLE, RoleType.CQS_ROLE, RoleType.TECH_ROLE, RoleType.PLANT_ROLE})
    @PlantDataFilter(entityField = "plantCode", filterType = PlantDataFilter.FilterType.SINGLE)
    @Transactional(readOnly = true)
    public ResponseEntity<WorkflowSummaryDto> getWorkflowByMaterialCode(@PathVariable String materialCode) {
        Optional<Workflow> workflow = workflowService.findByMaterialCode(materialCode);
        if (workflow.isPresent()) {
            Workflow w = workflow.get();
            // Initialize collections within transaction
            w.getQueries().size();
            w.getDocuments().size();
            WorkflowSummaryDto dto = workflowMapper.toSummaryDto(w);
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.notFound().build();
    }

    // Workflow creation - JVC users only
    @PostMapping
    @PreAuthorize("hasRole('JVC_USER') or hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<WorkflowSummaryDto> createWorkflow(
            @Valid @RequestBody WorkflowCreateRequest request,
            Authentication authentication) {
        
        String initiatedBy = getCurrentUsername(authentication);
        
        Workflow workflow;
        
        // Support both legacy and enhanced workflow creation
        if (request.getProjectCode() != null && request.getMaterialCode() != null && 
            request.getPlantCode() != null) {
            // Enhanced workflow creation with project/material/plant structure
            workflow = workflowService.initiateEnhancedWorkflow(
                request.getProjectCode(),
                request.getMaterialCode(),
                request.getPlantCode(),
                initiatedBy
            );
        } else {
            // Legacy workflow creation for backward compatibility
            workflow = workflowService.initiateWorkflow(
                request.getMaterialCode(),
                request.getMaterialName(),
                request.getMaterialDescription(),
                request.getAssignedPlant(),
                initiatedBy
            );
        }
        
        if (request.getSafetyDocumentsPath() != null) {
            workflow.setSafetyDocumentsPath(request.getSafetyDocumentsPath());
        }
        
        Workflow savedWorkflow = workflowService.save(workflow);
        
        // Initialize collections within transaction to avoid LazyInitializationException
        savedWorkflow.getQueries().size();
        savedWorkflow.getDocuments().size();
        
        // Convert to DTO to avoid serialization issues
        WorkflowSummaryDto workflowDto = workflowMapper.toSummaryDto(savedWorkflow);
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowDto);
    }

    // Smart plant extension - only extend to plants that don't have workflows yet
    @PostMapping("/extend-to-plants")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<SmartExtensionResponseDto> extendToMultiplePlantsSmartly(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        try {
            String projectCode = (String) request.get("projectCode");
            String materialCode = (String) request.get("materialCode");
            @SuppressWarnings("unchecked")
            List<String> plantCodes = (List<String>) request.get("plantCodes");
            String initiatedBy = getCurrentUsername(authentication);
            
            // Validate required parameters
            if (projectCode == null || materialCode == null || plantCodes == null || plantCodes.isEmpty()) {
                SmartExtensionResponseDto errorResponse = new SmartExtensionResponseDto(
                    false, 
                    "Missing required parameters: projectCode, materialCode, and plantCodes are required",
                    new SmartExtensionResponseDto.ExtensionSummary(0, 0, 0, 0),
                    new SmartExtensionResponseDto.ExtensionDetails(Collections.emptyList(), 
                        new SmartExtensionResponseDto.DuplicateInfo(Collections.emptyList(), Collections.emptyList(), ""), 
                        Collections.emptyList(), null)
                );
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            logger.info("Smart plant extension requested by user: {} for project: {}, material: {}, plants: {}", 
                       initiatedBy, projectCode, materialCode, plantCodes);
            
            // Use the smart extension service
            WorkflowService.SmartExtensionResult result = workflowService.extendToMultiplePlantsSmartly(
                projectCode, materialCode, plantCodes, initiatedBy);
            
            // Convert created workflows to DTOs
            List<WorkflowSummaryDto> createdWorkflowDtos = result.getCreatedWorkflows().stream()
                .map(workflowMapper::toSummaryDto)
                .collect(java.util.stream.Collectors.toList());
            
            // Convert existing workflows to DTOs
            List<WorkflowSummaryDto> existingWorkflowDtos = result.getExistingWorkflows().stream()
                .map(workflowMapper::toSummaryDto)
                .collect(java.util.stream.Collectors.toList());
            
            // Calculate summary statistics
            int totalRequested = plantCodes.size();
            int created = result.getCreatedWorkflows().size();
            int skipped = result.getSkippedPlants().size();
            int failed = result.getFailedPlants().size();
            
            // Build detailed message
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("Smart plant extension completed: ");
            messageBuilder.append(String.format("%d total requested, ", totalRequested));
            messageBuilder.append(String.format("%d newly created, ", created));
            messageBuilder.append(String.format("%d skipped (duplicates), ", skipped));
            messageBuilder.append(String.format("%d failed", failed));
            
            // Create structured response
            SmartExtensionResponseDto.ExtensionSummary summary = new SmartExtensionResponseDto.ExtensionSummary(
                totalRequested, created, skipped, failed);
            
            SmartExtensionResponseDto.DuplicateInfo duplicateInfo = new SmartExtensionResponseDto.DuplicateInfo(
                result.getSkippedPlants(), 
                existingWorkflowDtos,
                "These workflows already exist for the specified project, material, and plant combinations"
            );
            
            // Get enhanced document reuse information from the service result
            SmartExtensionResponseDto.DocumentReuseInfo documentReuseInfo = null;
            try {
                List<com.cqs.qrmfg.dto.DocumentSummary> reusableDocuments = result.getReusableDocuments();
                
                if (!reusableDocuments.isEmpty() && created > 0) {
                    // Calculate document reuse statistics
                    int totalDocumentsReused = reusableDocuments.size() * created;
                    int documentsPerWorkflow = reusableDocuments.size();
                    
                    // Count workflows that actually received reused documents
                    int workflowsWithReusedDocuments = (int) result.getCreatedWorkflows().stream()
                        .filter(w -> w.getAutoReusedDocumentCount() != null && w.getAutoReusedDocumentCount() > 0)
                        .count();
                    
                    // Create statistics (for now, all documents are workflow documents - will be enhanced later)
                    SmartExtensionResponseDto.DocumentReuseStatistics statistics = 
                        new SmartExtensionResponseDto.DocumentReuseStatistics(
                            reusableDocuments.size(), // workflowDocuments
                            0, // queryDocuments (will be implemented in later phases)
                            0, // responseDocuments (will be implemented in later phases)
                            reusableDocuments.size(), // totalUniqueDocuments
                            workflowsWithReusedDocuments
                        );
                    
                    // Create source description
                    String sourceDescription = String.format(
                        "Documents automatically reused from %d existing sources for project/material combination", 
                        reusableDocuments.size());
                    
                    documentReuseInfo = new SmartExtensionResponseDto.DocumentReuseInfo(
                        totalDocumentsReused,
                        reusableDocuments,
                        "AUTOMATIC",
                        sourceDescription,
                        documentsPerWorkflow,
                        statistics
                    );
                    
                    logger.info("Document reuse info created: {} total documents reused across {} workflows", 
                               totalDocumentsReused, workflowsWithReusedDocuments);
                } else if (created > 0) {
                    // No documents available for reuse
                    SmartExtensionResponseDto.DocumentReuseStatistics statistics = 
                        new SmartExtensionResponseDto.DocumentReuseStatistics(0, 0, 0, 0, 0);
                    
                    documentReuseInfo = new SmartExtensionResponseDto.DocumentReuseInfo(
                        0,
                        java.util.Collections.emptyList(),
                        "AUTOMATIC",
                        "No documents available for reuse for this project/material combination",
                        0,
                        statistics
                    );
                }
            } catch (Exception docError) {
                logger.warn("Failed to get document reuse information: {}", docError.getMessage());
                
                // Create error state document reuse info
                if (created > 0) {
                    SmartExtensionResponseDto.DocumentReuseStatistics statistics = 
                        new SmartExtensionResponseDto.DocumentReuseStatistics(0, 0, 0, 0, 0);
                    
                    documentReuseInfo = new SmartExtensionResponseDto.DocumentReuseInfo(
                        0,
                        java.util.Collections.emptyList(),
                        "AUTOMATIC",
                        "Failed to retrieve document reuse information: " + docError.getMessage(),
                        0,
                        statistics
                    );
                }
            }
            
            SmartExtensionResponseDto.ExtensionDetails details = new SmartExtensionResponseDto.ExtensionDetails(
                createdWorkflowDtos, duplicateInfo, result.getFailedPlants(), documentReuseInfo);
            
            SmartExtensionResponseDto response = new SmartExtensionResponseDto(
                true, messageBuilder.toString(), summary, details);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during smart plant extension: {}", e.getMessage(), e);
            SmartExtensionResponseDto errorResponse = new SmartExtensionResponseDto(
                false, 
                "Smart plant extension failed: " + e.getMessage(),
                new SmartExtensionResponseDto.ExtensionSummary(0, 0, 0, 0),
                new SmartExtensionResponseDto.ExtensionDetails(Collections.emptyList(), 
                    new SmartExtensionResponseDto.DuplicateInfo(Collections.emptyList(), Collections.emptyList(), ""), 
                    Collections.emptyList(), null)
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // State transition operations
    @PutMapping("/{id}/extend")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<WorkflowSummaryDto> extendToPlant(
            @PathVariable Long id,
            Authentication authentication) {
        
        String updatedBy = getCurrentUsername(authentication);
        Workflow workflow = workflowService.extendToPlant(id, updatedBy);
        
        // Initialize collections within transaction
        workflow.getQueries().size();
        workflow.getDocuments().size();
        
        WorkflowSummaryDto dto = workflowMapper.toSummaryDto(workflow);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/material/{materialCode}/extend")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<WorkflowSummaryDto> extendToPlantByMaterialCode(
            @PathVariable String materialCode,
            Authentication authentication) {
        
        String updatedBy = getCurrentUsername(authentication);
        Workflow workflow = workflowService.extendToPlant(materialCode, updatedBy);
        
        // Initialize collections within transaction
        workflow.getQueries().size();
        workflow.getDocuments().size();
        
        WorkflowSummaryDto dto = workflowMapper.toSummaryDto(workflow);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('PLANT_USER') or hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<WorkflowSummaryDto> completeWorkflow(
            @PathVariable Long id,
            Authentication authentication) {
        
        String updatedBy = getCurrentUsername(authentication);
        Workflow workflow = workflowService.completeWorkflow(id, updatedBy);
        
        // Initialize collections within transaction
        workflow.getQueries().size();
        workflow.getDocuments().size();
        
        WorkflowSummaryDto dto = workflowMapper.toSummaryDto(workflow);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/material/{materialCode}/complete")
    @PreAuthorize("hasRole('PLANT_USER') or hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<WorkflowSummaryDto> completeWorkflowByMaterialCode(
            @PathVariable String materialCode,
            Authentication authentication) {
        
        String updatedBy = getCurrentUsername(authentication);
        Workflow workflow = workflowService.completeWorkflow(materialCode, updatedBy);
        
        // Initialize collections within transaction
        workflow.getQueries().size();
        workflow.getDocuments().size();
        
        WorkflowSummaryDto dto = workflowMapper.toSummaryDto(workflow);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}/transition")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<WorkflowSummaryDto> transitionToState(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        String newStateStr = request.get("newState");
        if (newStateStr == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            WorkflowState newState = WorkflowState.valueOf(newStateStr);
            String updatedBy = getCurrentUsername(authentication);
            Workflow workflow = workflowService.transitionToState(id, newState, updatedBy);
            
            // Initialize collections within transaction
            workflow.getQueries().size();
            workflow.getDocuments().size();
            
            WorkflowSummaryDto dto = workflowMapper.toSummaryDto(workflow);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Query-based operations
    @GetMapping("/state/{state}")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<WorkflowSummaryDto>> getWorkflowsByState(@PathVariable String state) {
        try {
            WorkflowState workflowState = WorkflowState.valueOf(state);
            List<Workflow> workflows = workflowService.findByState(workflowState);
            
            // Debug logging
            System.out.println("Found " + workflows.size() + " workflows for state: " + state);
            for (Workflow workflow : workflows) {
                System.out.println("Workflow: " + workflow.getId() + 
                    ", Project: " + workflow.getProjectCode() + 
                    ", Material: " + workflow.getMaterialCode() + 
                    ", Plant: " + workflow.getPlantCode() + 
                    ", Documents: " + (workflow.getDocuments() != null ? workflow.getDocuments().size() : 0));
            }
            
            List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
            
            // Debug the DTOs
            System.out.println("Mapped to " + workflowDtos.size() + " DTOs");
            for (WorkflowSummaryDto dto : workflowDtos) {
                System.out.println("DTO: " + dto.getId() + 
                    ", Project: " + dto.getProjectCode() + 
                    ", Material: " + dto.getMaterialCode() + 
                    ", Plant: " + dto.getPlantCode() + 
                    ", Documents: " + dto.getDocumentCount());
            }
            
            return ResponseEntity.ok(workflowDtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/plant/{plantName}")
    @PreAuthorize("hasRole('PLANT_USER') or hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<WorkflowSummaryDto>> getWorkflowsByPlant(@PathVariable String plantName) {
        List<Workflow> workflows = workflowService.findByPlantCode(plantName);
        List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
        return ResponseEntity.ok(workflowDtos);
    }

    @GetMapping("/initiated-by/{username}")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<WorkflowSummaryDto>> getWorkflowsByInitiatedBy(@PathVariable String username) {
        List<Workflow> workflows = workflowService.findByInitiatedBy(username);
        List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
        return ResponseEntity.ok(workflowDtos);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<WorkflowSummaryDto>> getPendingWorkflows() {
        List<Workflow> workflows = workflowService.findPendingWorkflows();
        List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
        return ResponseEntity.ok(workflowDtos);
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<WorkflowSummaryDto>> getOverdueWorkflows() {
        List<Workflow> workflows = workflowService.findOverdueWorkflows();
        List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
        return ResponseEntity.ok(workflowDtos);
    }

    @GetMapping("/with-open-queries")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<WorkflowSummaryDto>> getWorkflowsWithOpenQueries() {
        List<Workflow> workflows = workflowService.findWorkflowsWithOpenQueries();
        List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
        return ResponseEntity.ok(workflowDtos);
    }

    // Dashboard and reporting endpoints
    @GetMapping("/stats/count-by-state/{state}")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<Long> getCountByState(@PathVariable String state) {
        try {
            WorkflowState workflowState = WorkflowState.valueOf(state);
            long count = workflowService.countByState(workflowState);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/stats/overdue-count")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<Long> getOverdueCount() {
        long count = workflowService.countOverdueWorkflows();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/stats/with-open-queries-count")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<Long> getWorkflowsWithOpenQueriesCount() {
        long count = workflowService.countWorkflowsWithOpenQueries();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/recent/created")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<WorkflowSummaryDto>> getRecentlyCreated(@RequestParam(defaultValue = "7") int days) {
        List<Workflow> workflows = workflowService.findRecentlyCreated(days);
        List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
        return ResponseEntity.ok(workflowDtos);
    }

    @GetMapping("/recent/completed")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<WorkflowSummaryDto>> getRecentlyCompleted(@RequestParam(defaultValue = "7") int days) {
        List<Workflow> workflows = workflowService.findRecentlyCompleted(days);
        List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
        return ResponseEntity.ok(workflowDtos);
    }

    // Validation endpoints
    @GetMapping("/{id}/can-transition/{newState}")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<Boolean> canTransitionTo(@PathVariable Long id, @PathVariable String newState) {
        try {
            WorkflowState workflowState = WorkflowState.valueOf(newState);
            boolean canTransition = workflowService.canTransitionTo(id, workflowState);
            return ResponseEntity.ok(canTransition);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/ready-for-completion")
    @PreAuthorize("hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<Boolean> isReadyForCompletion(@PathVariable Long id) {
        boolean isReady = workflowService.isWorkflowReadyForCompletion(id);
        return ResponseEntity.ok(isReady);
    }

    @GetMapping("/check-exists")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> checkWorkflowExists(
            @RequestParam String projectCode,
            @RequestParam String materialCode,
            @RequestParam String plantCode) {
        
        try {
            logger.debug("Checking for existing workflow: project={}, material={}, plant={}", 
                        projectCode, materialCode, plantCode);
            
            Optional<Workflow> existingWorkflow = workflowService.findExistingWorkflow(
                projectCode, materialCode, plantCode);
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("exists", existingWorkflow.isPresent());
            
            if (existingWorkflow.isPresent()) {
                Workflow workflow = existingWorkflow.get();
                logger.debug("Found existing workflow with ID: {}", workflow.getId());
                
                // Collections should already be initialized by the service method
                WorkflowSummaryDto dto = workflowMapper.toSummaryDto(workflow);
                response.put("workflow", dto);
                
                logger.debug("Mapped workflow to DTO successfully");
            } else {
                logger.debug("No existing workflow found");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error checking for existing workflow: project={}, material={}, plant={}", 
                        projectCode, materialCode, plantCode, e);
            
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("error", "Failed to check for existing workflow");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Document access endpoints
    @GetMapping("/{id}/documents")
    @PreAuthorize("hasRole('PLANT_USER') or hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<DocumentSummary>> getWorkflowDocuments(@PathVariable Long id) {
        try {
            logger.info("Getting workflow documents for workflow {}", id);
            
            // Verify workflow exists
            Optional<Workflow> workflowOpt = workflowService.findById(id);
            if (!workflowOpt.isPresent()) {
                logger.error("Workflow not found: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            List<DocumentSummary> documents = documentService.getWorkflowDocuments(id);
            logger.info("Found {} documents for workflow {}", documents.size(), id);
            
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            logger.error("Failed to get workflow documents for {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/documents/reusable")
    @PreAuthorize("hasRole('PLANT_USER') or hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<DocumentSummary>> getReusableDocuments(
            @RequestParam String projectCode, 
            @RequestParam String materialCode) {
        List<DocumentSummary> documents = documentService.getReusableDocuments(projectCode, materialCode);
        return ResponseEntity.ok(documents);
    }

    /**
     * Get all documents related to a workflow including query documents
     * This endpoint provides unified document access for enhanced workflow document views
     */
    @GetMapping("/{id}/documents/all")
    @PreAuthorize("hasRole('PLANT_USER') or hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('ADMIN')")
    public ResponseEntity<com.cqs.qrmfg.dto.UnifiedDocumentSearchResult> getAllWorkflowRelatedDocuments(@PathVariable Long id) {
        try {
            // Get the workflow to extract project and material codes
            Workflow workflow = workflowService.findById(id)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found with ID: " + id));
            
            // Use the unified search to get all documents for this project/material
            com.cqs.qrmfg.dto.UnifiedDocumentSearchResult result = documentService.searchAllDocuments(
                null, // no search term - get all documents
                workflow.getProjectCode(),
                workflow.getMaterialCode(),
                java.util.Arrays.asList(com.cqs.qrmfg.enums.DocumentSource.WORKFLOW, 
                                       com.cqs.qrmfg.enums.DocumentSource.QUERY, 
                                       com.cqs.qrmfg.enums.DocumentSource.RESPONSE)
            );
            
            logger.debug("Found {} total documents for workflow {} (project: {}, material: {})", 
                        result.getSearchMetadata().getTotalResults(), id, 
                        workflow.getProjectCode(), workflow.getMaterialCode());
            
            return ResponseEntity.ok(result);
        } catch (WorkflowNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error retrieving all documents for workflow {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



    /**
     * Generate and download Excel report with questions and answers for a workflow
     */
    @GetMapping("/{id}/excel-report")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadExcelReport(@PathVariable Long id) {
        try {
            logger.info("Excel report requested for workflow: {}", id);
            
            Optional<Workflow> workflowOpt = workflowService.findById(id);
            if (!workflowOpt.isPresent()) {
                logger.error("Workflow not found: {}", id);
                return ResponseEntity.notFound().build();
            }

            Workflow workflow = workflowOpt.get();
            byte[] excelData = excelReportService.generateQuestionsAndAnswersReport(workflow);

            String filename = String.format("QRMFG_Report_Workflow_%d_%s.xlsx", 
                id, workflow.getMaterialCode().replaceAll("[^a-zA-Z0-9]", "_"));

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
            headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            logger.info("Excel report generated successfully for workflow: {}, size: {} bytes", id, excelData.length);
            return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);

        } catch (Exception e) {
            logger.error("Error generating Excel report for workflow {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Export all documents related to a workflow as a ZIP file
     * Includes workflow documents and optionally related query documents
     */
    @GetMapping("/{id}/documents/export")
    @PreAuthorize("hasRole('PLANT_USER') or hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('ADMIN')")
    public ResponseEntity<org.springframework.core.io.Resource> exportWorkflowDocuments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean includeQueryDocuments) {
        try {
            org.springframework.core.io.Resource zipResource = documentExportService.exportWorkflowDocuments(id, includeQueryDocuments);
            
            return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                       "attachment; filename=\"" + zipResource.getFilename() + "\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(zipResource);
                
        } catch (WorkflowNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error exporting documents for workflow {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Test endpoint to check workflow count
    @GetMapping("/test/count")
    public ResponseEntity<Map<String, Object>> getWorkflowCount() {
        try {
            List<Workflow> allWorkflows = workflowService.findAll();
            List<Workflow> jvcPendingWorkflows = workflowService.findByState(WorkflowState.JVC_PENDING);
            
            Map<String, Object> counts = new java.util.HashMap<>();
            counts.put("totalWorkflows", allWorkflows.size());
            counts.put("jvcPendingWorkflows", jvcPendingWorkflows.size());
            counts.put("sampleWorkflow", allWorkflows.isEmpty() ? null : allWorkflows.get(0));
            
            return ResponseEntity.ok(counts);
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Test endpoint to create sample data
    @PostMapping("/test/create-sample-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> createSampleData() {
        try {
            // Create a few sample workflows for testing
            Workflow workflow1 = new Workflow("SER-A-123456", "R12345A", "1001", "testuser");
            workflow1.setMaterialName("Test Material 1");
            workflow1.setMaterialDescription("Test material description 1");
            
            Workflow workflow2 = new Workflow("SER-B-789012", "R67890B", "1002", "testuser");
            workflow2.setMaterialName("Test Material 2");
            workflow2.setMaterialDescription("Test material description 2");
            
            Workflow workflow3 = new Workflow("SER-C-345678", "R34567C", "1003", "admin");
            workflow3.setMaterialName("Test Material 3");
            workflow3.setMaterialDescription("Test material description 3");
            
            workflowService.save(workflow1);
            workflowService.save(workflow2);
            workflowService.save(workflow3);
            
            return ResponseEntity.ok("Sample data created successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error creating sample data: " + e.getMessage());
        }
    }

    // Material name management endpoints
    @PostMapping("/update-material-names")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")  // Allow all users to trigger this
    public ResponseEntity<Map<String, String>> updateAllMaterialNamesFromProjectItemMaster(Authentication authentication) {
        try {
            String updatedBy = getCurrentUsername(authentication);
            logger.info("Starting bulk update of material names from ProjectItemMaster by user: {}", updatedBy);
            
            workflowService.updateMaterialNamesFromProjectItemMaster();
            
            Map<String, String> response = new java.util.HashMap<>();
            response.put("message", "Material names updated successfully from ProjectItemMaster");
            response.put("updatedBy", updatedBy);
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to update material names from ProjectItemMaster", e);
            Map<String, String> errorResponse = new java.util.HashMap<>();
            errorResponse.put("error", "Failed to update material names");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/{id}/update-material-name")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<WorkflowSummaryDto> updateMaterialNameFromProjectItemMaster(
            @PathVariable Long id, Authentication authentication) {
        try {
            String updatedBy = getCurrentUsername(authentication);
            logger.info("Updating material name for workflow {} from ProjectItemMaster by user: {}", id, updatedBy);
            
            Workflow workflow = workflowService.updateMaterialNameFromProjectItemMaster(id);
            WorkflowSummaryDto dto = workflowMapper.toSummaryDto(workflow);
            
            return ResponseEntity.ok(dto);
        } catch (WorkflowNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to update material name for workflow {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Utility method to get current username
    private String getCurrentUsername(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getUsername();
        }
        return "SYSTEM";
    }

    // Exception handlers
    @ExceptionHandler(WorkflowNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleWorkflowNotFound(WorkflowNotFoundException ex) {
        Map<String, String> errorResponse = new java.util.HashMap<>();
        errorResponse.put("error", "Workflow not found");
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(WorkflowException.class)
    public ResponseEntity<Map<String, String>> handleWorkflowException(WorkflowException ex) {
        Map<String, String> errorResponse = new java.util.HashMap<>();
        errorResponse.put("error", "Workflow error");
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> errorResponse = new java.util.HashMap<>();
        errorResponse.put("error", "Invalid argument");
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // ========== WORKFLOW DOCUMENT ENDPOINTS ==========

    /**
     * Download a workflow document - delegates to DocumentService
     */
    @GetMapping("/documents/{documentId}/download")
    @PreAuthorize("hasRole('PLANT_USER') or hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('ADMIN')")
    public ResponseEntity<org.springframework.core.io.Resource> downloadWorkflowDocument(
            @PathVariable Long documentId,
            @RequestParam(required = false) Long workflowId,
            Authentication authentication,
            javax.servlet.http.HttpServletRequest request) {
        
        try {
            logger.info("Downloading workflow document {} for user {}", documentId, getCurrentUsername(authentication));
            
            // Validate document ID
            if (documentId == null || documentId <= 0) {
                return ResponseEntity.badRequest().build();
            }
            
            // Get document info
            com.cqs.qrmfg.model.Document document = documentService.getDocumentById(documentId);
            if (document == null) {
                logger.error("Document not found: {}", documentId);
                return ResponseEntity.notFound().build();
            }
            
            logger.info("Found document: {} (Original: {}, Path: {})", 
                       document.getFileName(), document.getOriginalFileName(), document.getFilePath());
            
            String userId = getCurrentUsername(authentication);
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            
            // Download document with security checks
            org.springframework.core.io.Resource resource = 
                documentService.downloadDocumentSecure(documentId, userId, ipAddress, userAgent, workflowId);
            
            return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                       "attachment; filename=\"" + document.getOriginalFileName() + "\"")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, 
                       org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .body(resource);
                
        } catch (Exception e) {
            logger.error("Failed to download workflow document {}: {}", documentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get document count for a workflow
     */
    @GetMapping("/{workflowId}/documents/count")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<Long> getWorkflowDocumentCount(@PathVariable Long workflowId) {
        try {
            // Verify workflow exists
            Optional<Workflow> workflowOpt = workflowService.findById(workflowId);
            if (!workflowOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            long count = documentService.getDocumentCount(workflowId);
            return ResponseEntity.ok(count);
            
        } catch (Exception e) {
            logger.error("Failed to get document count for workflow {}: {}", workflowId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Debug endpoint to check document database state
     */
    @GetMapping("/{workflowId}/documents/debug")
    @PreAuthorize("hasRole('TECH_USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> debugWorkflowDocuments(@PathVariable Long workflowId) {
        try {
            Map<String, Object> debug = new java.util.HashMap<>();
            
            // Check workflow exists
            Optional<Workflow> workflowOpt = workflowService.findById(workflowId);
            debug.put("workflowExists", workflowOpt.isPresent());
            
            if (workflowOpt.isPresent()) {
                Workflow workflow = workflowOpt.get();
                debug.put("workflowId", workflow.getId());
                debug.put("projectCode", workflow.getProjectCode());
                debug.put("materialCode", workflow.getMaterialCode());
                debug.put("workflowState", workflow.getState().toString());
                
                // Get workflow documents
                List<DocumentSummary> workflowDocs = documentService.getWorkflowDocuments(workflowId);
                debug.put("workflowDocuments", workflowDocs.size());
                debug.put("workflowDocumentsList", workflowDocs);
                
                // Get reusable documents
                List<DocumentSummary> reusableDocs = documentService.getReusableDocuments(
                    workflow.getProjectCode(), workflow.getMaterialCode());
                debug.put("reusableDocuments", reusableDocs.size());
                debug.put("reusableDocumentsList", reusableDocs);
            }
            
            // Get document count
            long count = documentService.getDocumentCount(workflowId);
            debug.put("documentCount", count);
            
            return ResponseEntity.ok(debug);
            
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("error", e.getMessage());
            error.put("errorType", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Export workflows to Excel format
     */
    @GetMapping("/export/excel")
    @RequireRole({RoleType.ADMIN, RoleType.JVC_ROLE, RoleType.CQS_ROLE, RoleType.TECH_ROLE, RoleType.PLANT_ROLE})
    @PlantDataFilter(entityField = "plantCode", filterType = PlantDataFilter.FilterType.LIST)
    @Transactional(readOnly = true)
    public ResponseEntity<org.springframework.core.io.Resource> exportWorkflowsToExcel(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String plantCode,
            @RequestParam(required = false) String initiatedBy,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false) Boolean withOpenQueries) {
        
        try {
            logger.info("Exporting workflows to Excel with filters - state: {}, plantCode: {}, initiatedBy: {}, overdue: {}, withOpenQueries: {}", 
                       state, plantCode, initiatedBy, overdue, withOpenQueries);
            
            List<Workflow> workflows;
            String exportName = "workflows";
            
            // Apply filters based on parameters
            if (state != null) {
                try {
                    WorkflowState workflowState = WorkflowState.valueOf(state.toUpperCase());
                    workflows = workflowService.findByState(workflowState);
                    exportName += "_" + state.toLowerCase();
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().build();
                }
            } else if (plantCode != null) {
                workflows = workflowService.findByPlantCode(plantCode);
                exportName += "_plant_" + plantCode;
            } else if (initiatedBy != null) {
                workflows = workflowService.findByInitiatedBy(initiatedBy);
                exportName += "_initiated_by_" + initiatedBy;
            } else if (Boolean.TRUE.equals(overdue)) {
                workflows = workflowService.findOverdueWorkflows();
                exportName += "_overdue";
            } else if (Boolean.TRUE.equals(withOpenQueries)) {
                workflows = workflowService.findWorkflowsWithOpenQueries();
                exportName += "_with_open_queries";
            } else {
                // Default: get all workflows
                workflows = workflowService.findAll();
                exportName += "_all";
            }
            
            // Convert to DTOs
            List<WorkflowSummaryDto> workflowDtos = workflowMapper.toSummaryDtoList(workflows);
            
            logger.info("Exporting {} workflows to Excel", workflowDtos.size());
            
            // Generate Excel file
            org.springframework.core.io.Resource excelResource = documentExportService.exportWorkflowsToExcel(workflowDtos, exportName);
            
            return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                       "attachment; filename=\"" + excelResource.getFilename() + "\"")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, 
                       "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(excelResource);
                
        } catch (Exception e) {
            logger.error("Error exporting workflows to Excel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Export specific workflow by ID to Excel format
     */
    @GetMapping("/{id}/export/excel")
    @RequireRole({RoleType.ADMIN, RoleType.JVC_ROLE, RoleType.CQS_ROLE, RoleType.TECH_ROLE, RoleType.PLANT_ROLE})
    @PlantDataFilter(entityField = "plantCode", filterType = PlantDataFilter.FilterType.SINGLE, plantRoleOnly = true)
    @Transactional(readOnly = true)
    public ResponseEntity<org.springframework.core.io.Resource> exportSingleWorkflowToExcel(@PathVariable Long id) {
        try {
            logger.info("Exporting single workflow {} to Excel", id);
            
            Optional<Workflow> workflowOpt = workflowService.findById(id);
            if (!workflowOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Workflow workflow = workflowOpt.get();
            // Initialize collections within transaction
            workflow.getQueries().size();
            workflow.getDocuments().size();
            
            WorkflowSummaryDto workflowDto = workflowMapper.toSummaryDto(workflow);
            List<WorkflowSummaryDto> workflowList = java.util.Arrays.asList(workflowDto);
            
            String exportName = String.format("workflow_%s_%s_%s", 
                workflow.getProjectCode(), workflow.getMaterialCode(), workflow.getPlantCode());
            
            // Generate Excel file
            org.springframework.core.io.Resource excelResource = documentExportService.exportWorkflowsToExcel(workflowList, exportName);
            
            return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                       "attachment; filename=\"" + excelResource.getFilename() + "\"")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, 
                       "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(excelResource);
                
        } catch (Exception e) {
            logger.error("Error exporting workflow {} to Excel", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}