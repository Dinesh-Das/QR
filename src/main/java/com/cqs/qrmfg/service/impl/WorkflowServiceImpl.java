package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.exception.InvalidWorkflowStateException;
import com.cqs.qrmfg.exception.WorkflowException;
import com.cqs.qrmfg.exception.WorkflowNotFoundException;
import com.cqs.qrmfg.model.Workflow;
import com.cqs.qrmfg.model.QueryStatus;
import com.cqs.qrmfg.model.WorkflowState;
import com.cqs.qrmfg.repository.WorkflowRepository;
import com.cqs.qrmfg.repository.QrmfgProjectItemMasterRepository;
import com.cqs.qrmfg.service.NotificationService;
import com.cqs.qrmfg.service.WorkflowService;
import com.cqs.qrmfg.service.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class WorkflowServiceImpl implements WorkflowService {
    
    private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceImpl.class);
    
    @Autowired
    private WorkflowRepository workflowRepository;
    
    @Autowired
    private QrmfgProjectItemMasterRepository projectItemMasterRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private MetricsService metricsService;
    
    @Autowired
    private com.cqs.qrmfg.service.PlantQuestionnaireService plantQuestionnaireService;
    
    @Autowired
    private com.cqs.qrmfg.service.DocumentService documentService;
    
    /**
     * Helper method to fetch material name from QrmfgProjectItemMaster
     */
    private String fetchMaterialNameFromProjectItemMaster(String projectCode, String materialCode) {
        if (projectCode == null || materialCode == null) {
            return null;
        }
        
        try {
            Optional<String> itemDescription = projectItemMasterRepository
                .findItemDescriptionByProjectCodeAndItemCode(projectCode, materialCode);
            return itemDescription.orElse(null);
        } catch (Exception e) {
            logger.warn("Failed to fetch material name for project: {}, material: {} - {}", 
                       projectCode, materialCode, e.getMessage());
            return null;
        }
    }
    
    // Basic CRUD operations
    @Override
    public Workflow save(Workflow workflow) {
        logger.debug("Saving workflow for material: {}", workflow.getMaterialCode());
        return workflowRepository.save(workflow);
    }
    
    @Override
    public Workflow update(Workflow workflow) {
        if (workflow.getId() == null) {
            throw new WorkflowException("Cannot update workflow without ID");
        }
        
        Optional<Workflow> existingOpt = workflowRepository.findById(workflow.getId());
        if (!existingOpt.isPresent()) {
            throw new WorkflowNotFoundException(workflow.getId());
        }
        
        logger.debug("Updating workflow for material: {}", workflow.getMaterialCode());
        return workflowRepository.save(workflow);
    }
    
    @Override
    public void delete(Long id) {
        if (!workflowRepository.existsById(id)) {
            throw new WorkflowNotFoundException(id);
        }
        logger.debug("Deleting workflow with ID: {}", id);
        workflowRepository.deleteById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Workflow> findById(Long id) {
        return workflowRepository.findById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Workflow> findByMaterialCode(String materialCode) {
        // If you want to keep returning Optional, use stream().findFirst()
        return workflowRepository.findByMaterialCode(materialCode).stream().findFirst();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Workflow> findAll() {
        List<Workflow> workflows = workflowRepository.findAllWithQueries();
        
        // Initialize documents collection for each workflow within the same transaction
        for (Workflow workflow : workflows) {
            workflow.getDocuments().size(); // This will trigger lazy loading within transaction
        }
        
        return workflows;
    }
    
    // Workflow creation
    @Override
    public Workflow initiateWorkflow(String materialCode, String materialName, String materialDescription,
                                           String assignedPlant, String initiatedBy) {
        // Check if workflow already exists
        if (workflowRepository.existsByMaterialCode(materialCode)) {
            throw new WorkflowException("Workflow already exists for material: " + materialCode);
        }
        
        // Create workflow using enhanced constructor and map to legacy fields
        Workflow workflow = new Workflow(materialCode, materialCode, assignedPlant, initiatedBy);
        workflow.setMaterialName(materialName);
        workflow.setMaterialDescription(materialDescription);
        
        logger.info("Initiating workflow for material: {} by user: {}", materialCode, initiatedBy);
        Workflow savedWorkflow = workflowRepository.save(workflow);
        
        // Send notification for workflow creation
        try {
            notificationService.notifyWorkflowCreated(savedWorkflow);
        } catch (Exception e) {
            logger.warn("Failed to send workflow creation notification for material {}: {}", 
                       materialCode, e.getMessage());
        }
        
        return savedWorkflow;
    }
    
    @Override
    public Workflow initiateWorkflow(String materialCode, String assignedPlant, String initiatedBy) {
        return initiateWorkflow(materialCode, null, null, assignedPlant, initiatedBy);
    }

    @Override
    public Workflow initiateEnhancedWorkflow(String projectCode, String materialCode, String plantCode, 
                                                   String initiatedBy) {
        // Check if workflow already exists for this combination
        if (workflowRepository.existsByProjectCodeAndMaterialCodeAndPlantCode(
                projectCode, materialCode, plantCode)) {
            throw new WorkflowException(String.format(
                "Workflow already exists for project: %s, material: %s, plant: %s", 
                projectCode, materialCode, plantCode));
        }
        
        Workflow workflow = new Workflow(projectCode, materialCode, plantCode, initiatedBy);
        
        // Fetch and set material name from QrmfgProjectItemMaster
        String materialName = fetchMaterialNameFromProjectItemMaster(projectCode, materialCode);
        if (materialName != null) {
            workflow.setMaterialName(materialName);
            logger.debug("Set material name from ProjectItemMaster: {} for project: {}, material: {}", 
                        materialName, projectCode, materialCode);
        } else {
            logger.warn("Could not fetch material name from ProjectItemMaster for project: {}, material: {}", 
                       projectCode, materialCode);
        }
        
        logger.info("Initiating enhanced workflow for project: {}, material: {}, plant: {} by user: {}", 
                   projectCode, materialCode, plantCode, initiatedBy);
        Workflow savedWorkflow = workflowRepository.save(workflow);
        
        // Send notification for workflow creation
        try {
            notificationService.notifyWorkflowCreated(savedWorkflow);
        } catch (Exception e) {
            logger.warn("Failed to send workflow creation notification for project {}, material {}: {}", 
                       projectCode, materialCode, e.getMessage());
        }
        
        return savedWorkflow;
    }
    
    /**
     * Update material names for existing workflows from QrmfgProjectItemMaster
     */
    @Override
    public void updateMaterialNamesFromProjectItemMaster() {
        logger.info("Starting bulk update of material names from ProjectItemMaster");
        
        List<Workflow> workflows = workflowRepository.findAll();
        int updatedCount = 0;
        int skippedCount = 0;
        int notFoundCount = 0;
        
        logger.info("Found {} workflows to process", workflows.size());
        
        for (Workflow workflow : workflows) {
            String currentMaterialName = workflow.getMaterialName();
            String projectCode = workflow.getProjectCode();
            String materialCode = workflow.getMaterialCode();
            
            logger.debug("Processing workflow {}: project={}, material={}, currentName={}", 
                        workflow.getId(), projectCode, materialCode, currentMaterialName);
            
            String fetchedMaterialName = fetchMaterialNameFromProjectItemMaster(projectCode, materialCode);
            
            if (fetchedMaterialName != null) {
                if (!fetchedMaterialName.equals(currentMaterialName)) {
                    workflow.setMaterialName(fetchedMaterialName);
                    workflowRepository.save(workflow);
                    updatedCount++;
                    
                    logger.info("Updated material name for workflow {}: '{}' -> '{}'", 
                               workflow.getId(), currentMaterialName, fetchedMaterialName);
                } else {
                    skippedCount++;
                    logger.debug("Skipped workflow {} - material name already correct: '{}'", 
                                workflow.getId(), currentMaterialName);
                }
            } else {
                notFoundCount++;
                logger.warn("No material name found in ProjectItemMaster for workflow {}: project={}, material={}", 
                           workflow.getId(), projectCode, materialCode);
            }
        }
        
        logger.info("Completed bulk update of material names. Updated: {}, Skipped: {}, Not Found: {}, Total: {}", 
                   updatedCount, skippedCount, notFoundCount, workflows.size());
    }
    
    /**
     * Update material name for a specific workflow from QrmfgProjectItemMaster
     */
    @Override
    public Workflow updateMaterialNameFromProjectItemMaster(Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        
        String fetchedMaterialName = fetchMaterialNameFromProjectItemMaster(
            workflow.getProjectCode(), workflow.getMaterialCode());
        
        if (fetchedMaterialName != null) {
            String oldName = workflow.getMaterialName();
            workflow.setMaterialName(fetchedMaterialName);
            Workflow savedWorkflow = workflowRepository.save(workflow);
            
            logger.info("Updated material name for workflow {}: {} -> {}", 
                       workflowId, oldName, fetchedMaterialName);
            
            return savedWorkflow;
        } else {
            logger.warn("Could not fetch material name from ProjectItemMaster for workflow {}", workflowId);
            return workflow;
        }
    }
    
    // State transition operations
    @Override
    public Workflow transitionToState(Long workflowId, WorkflowState newState, String updatedBy) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        
        return performStateTransition(workflow, newState, updatedBy);
    }
    
    @Override
    public Workflow transitionToState(String materialCode, WorkflowState newState, String updatedBy) {
        Workflow workflow = workflowRepository.findByMaterialCode(materialCode)
            .stream().findFirst().orElseThrow(() -> WorkflowNotFoundException.forMaterialCode(materialCode));
        
        return performStateTransition(workflow, newState, updatedBy);
    }
    
    private Workflow performStateTransition(Workflow workflow, WorkflowState newState, String updatedBy) {
        WorkflowState currentState = workflow.getState();
        
        logger.info("Transitioning workflow {} from {} to {} by user: {}", 
                   workflow.getMaterialCode(), currentState, newState, updatedBy);
        
        // Validate transition
        validateStateTransition(workflow, newState);
        
        // Perform transition
        workflow.transitionTo(newState, updatedBy);
        
        Workflow savedWorkflow = workflowRepository.save(workflow);
        
        // Send notification for state change - this is the core integration point
        try {
            notificationService.notifyWorkflowStateChanged(savedWorkflow, currentState, updatedBy);
            
            // Send specific notifications based on the new state
            switch (newState) {
                case PLANT_PENDING:
                    // Additional notification when workflow moves to plant
                    if (currentState == WorkflowState.JVC_PENDING) {
                        notificationService.notifyWorkflowExtended(savedWorkflow, updatedBy);
                    }
                    break;
                case COMPLETED:
                    // Additional notification when workflow completes
                    notificationService.notifyWorkflowCompleted(savedWorkflow, updatedBy);
                    break;
                default:
                    // State change notification already sent above
                    break;
            }
        } catch (Exception e) {
            logger.warn("Failed to send workflow state change notification for material {}: {}", 
                       workflow.getMaterialCode(), e.getMessage());
        }
        
        return savedWorkflow;
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean canTransitionTo(Long workflowId, WorkflowState newState) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        
        return workflow.canTransitionTo(newState);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean canTransitionTo(String materialCode, WorkflowState newState) {
        Workflow workflow = workflowRepository.findByMaterialCode(materialCode)
            .stream().findFirst().orElseThrow(() -> WorkflowNotFoundException.forMaterialCode(materialCode));
        
        return workflow.canTransitionTo(newState);
    }
    
    // Specific workflow actions
    @Override
    public Workflow extendToPlant(Long workflowId, String updatedBy) {
        Workflow workflow = transitionToState(workflowId, WorkflowState.PLANT_PENDING, updatedBy);
        
        // Create PlantSpecificData record so the workflow appears in plant dashboard
        try {
            plantQuestionnaireService.getOrCreatePlantSpecificData(
                workflow.getPlantCode(), 
                workflow.getMaterialCode(), 
                workflow.getId()
            );
            logger.info("Created PlantSpecificData for workflow {} extended to plant {}", 
                       workflow.getId(), workflow.getPlantCode());
        } catch (Exception e) {
            logger.error("Failed to create PlantSpecificData for workflow {} extended to plant {}: {}", 
                        workflow.getId(), workflow.getPlantCode(), e.getMessage());
        }
        
        // Send specific notification for workflow extension
        try {
            notificationService.notifyWorkflowExtended(workflow, updatedBy);
        } catch (Exception e) {
            logger.warn("Failed to send workflow extension notification for material {}: {}", 
                       workflow.getMaterialCode(), e.getMessage());
        }
        
        return workflow;
    }
    
    @Override
    public Workflow extendToPlant(String materialCode, String updatedBy) {
        Workflow workflow = transitionToState(materialCode, WorkflowState.PLANT_PENDING, updatedBy);
        
        // Create PlantSpecificData record so the workflow appears in plant dashboard
        try {
            plantQuestionnaireService.getOrCreatePlantSpecificData(
                workflow.getPlantCode(), 
                workflow.getMaterialCode(), 
                workflow.getId()
            );
            logger.info("Created PlantSpecificData for workflow {} extended to plant {}", 
                       workflow.getId(), workflow.getPlantCode());
        } catch (Exception e) {
            logger.error("Failed to create PlantSpecificData for workflow {} extended to plant {}: {}", 
                        workflow.getId(), workflow.getPlantCode(), e.getMessage());
        }
        
        // Send specific notification for workflow extension
        try {
            notificationService.notifyWorkflowExtended(workflow, updatedBy);
        } catch (Exception e) {
            logger.warn("Failed to send workflow extension notification for material {}: {}", 
                       workflow.getMaterialCode(), e.getMessage());
        }
        
        return workflow;
    }
    
    @Override
    public Workflow completeWorkflow(Long workflowId, String updatedBy) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        
        validateWorkflowCompletion(workflow);
        Workflow completedWorkflow = transitionToState(workflowId, WorkflowState.COMPLETED, updatedBy);
        
        // Send specific notification for workflow completion
        try {
            notificationService.notifyWorkflowCompleted(completedWorkflow, updatedBy);
        } catch (Exception e) {
            logger.warn("Failed to send workflow completion notification for material {}: {}", 
                       workflow.getMaterialCode(), e.getMessage());
        }
        
        return completedWorkflow;
    }
    
    @Override
    public Workflow completeWorkflow(String materialCode, String updatedBy) {
        Workflow workflow = workflowRepository.findByMaterialCode(materialCode)
            .stream().findFirst().orElseThrow(() -> WorkflowNotFoundException.forMaterialCode(materialCode));
        
        validateWorkflowCompletion(workflow);
        Workflow completedWorkflow = transitionToState(materialCode, WorkflowState.COMPLETED, updatedBy);
        
        // Send specific notification for workflow completion
        try {
            notificationService.notifyWorkflowCompleted(completedWorkflow, updatedBy);
        } catch (Exception e) {
            logger.warn("Failed to send workflow completion notification for material {}: {}", 
                       workflow.getMaterialCode(), e.getMessage());
        }
        
        return completedWorkflow;
    }
    
    @Override
    public Workflow moveToQueryState(Long workflowId, WorkflowState queryState, String updatedBy) {
        if (!queryState.isQueryState()) {
            throw new InvalidWorkflowStateException("State " + queryState + " is not a query state");
        }
        return transitionToState(workflowId, queryState, updatedBy);
    }
    
    @Override
    public Workflow returnFromQueryState(Long workflowId, String updatedBy) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        
        // Only transition if currently in a query state
        WorkflowState currentState = workflow.getState();
        if (currentState == WorkflowState.CQS_PENDING || 
            currentState == WorkflowState.TECH_PENDING || 
            currentState == WorkflowState.JVC_PENDING) {
            
            logger.info("Returning workflow {} from query state {} to PLANT_PENDING", 
                       workflowId, currentState);
            return transitionToState(workflowId, WorkflowState.PLANT_PENDING, updatedBy);
        } else {
            logger.debug("Workflow {} is already in state {}, no transition needed", 
                        workflowId, currentState);
            return workflow;
        }
    }
    
    // Query-based operations
    @Override
    @Transactional(readOnly = true)
    public List<Workflow> findByState(WorkflowState state) {
        List<Workflow> workflows = workflowRepository.findByStateWithQueries(state);
        
        // Initialize documents collection for each workflow within the same transaction
        for (Workflow workflow : workflows) {
            workflow.getDocuments().size(); // This will trigger lazy loading within transaction
        }
        
        return workflows;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Workflow> findByPlantCode(String plantCode) {
        List<Workflow> workflows = workflowRepository.findByPlantCodeWithQueries(plantCode);
        
        // Initialize documents collection for each workflow within the same transaction
        for (Workflow workflow : workflows) {
            workflow.getDocuments().size(); // This will trigger lazy loading within transaction
        }
        
        return workflows;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Workflow> findByInitiatedBy(String username) {
        List<Workflow> workflows = workflowRepository.findByInitiatedByWithQueries(username);
        
        // Initialize documents collection for each workflow within the same transaction
        for (Workflow workflow : workflows) {
            workflow.getDocuments().size(); // This will trigger lazy loading within transaction
        }
        
        return workflows;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Workflow> findPendingWorkflows() {
        return workflowRepository.findPendingWorkflows();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Workflow> findOverdueWorkflows() {
        return workflowRepository.findOverdueWorkflows();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Workflow> findWorkflowsWithOpenQueries() {
        return workflowRepository.findWorkflowsWithOpenQueries();
    }
    
    // Dashboard and reporting
    @Override
    @Transactional(readOnly = true)
    public long countByState(WorkflowState state) {
        return workflowRepository.countByState(state);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countOverdueWorkflows() {
        return workflowRepository.countOverdueWorkflows();
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countWorkflowsWithOpenQueries() {
        return workflowRepository.countWorkflowsWithOpenQueries();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Workflow> findRecentlyCreated(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return workflowRepository.findByCreatedAtAfter(cutoffDate);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Workflow> findRecentlyCompleted(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return workflowRepository.findByCompletedAtAfter(cutoffDate);
    }
    
    // Validation and business rules
    @Override
    public void validateStateTransition(Workflow workflow, WorkflowState newState) {
        if (!workflow.canTransitionTo(newState)) {
            throw new InvalidWorkflowStateException(
                String.format("Invalid state transition for workflow %s: %s -> %s", workflow.getMaterialCode(), workflow.getState(), newState)
            );
        }
        
        // Additional business rule validations
        if (newState == WorkflowState.COMPLETED) {
            validateWorkflowCompletion(workflow);
        }
        
        // Validate query state transitions
        if (newState.isQueryState()) {
            if (workflow.getState() != WorkflowState.PLANT_PENDING) {
                throw new InvalidWorkflowStateException(
                    "Can only move to query states from PLANT_PENDING state");
            }
        }
    }
    
    @Override
    public void validateWorkflowCompletion(Workflow workflow) {
        // Check if there are any open queries
        if (workflow.hasOpenQueries()) {
            throw new WorkflowException(
                String.format("Cannot complete workflow %s: %d open queries remaining", 
                             workflow.getMaterialCode(), workflow.getOpenQueriesCount()));
        }
        
        // Check if workflow is in a valid state for completion
        if (workflow.getState() != WorkflowState.PLANT_PENDING) {
            throw new InvalidWorkflowStateException(
                "Workflow can only be completed from PLANT_PENDING state");
        }
        
        // Additional business rules can be added here
        // For example: check if all required questionnaire fields are filled
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isWorkflowReadyForCompletion(Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        
        return isWorkflowReadyForCompletion(workflow);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isWorkflowReadyForCompletion(String materialCode) {
        Workflow workflow = workflowRepository.findByMaterialCode(materialCode)
            .stream().findFirst().orElseThrow(() -> WorkflowNotFoundException.forMaterialCode(materialCode));
        
        return isWorkflowReadyForCompletion(workflow);
    }
    
    private boolean isWorkflowReadyForCompletion(Workflow workflow) {
        try {
            validateWorkflowCompletion(workflow);
            return true;
        } catch (WorkflowException e) {
            return false;
        }
    }
    
    // Duplicate checking methods
    @Override
    @Transactional(readOnly = true)
    public Optional<Workflow> findExistingWorkflow(String projectCode, String materialCode, String plantCode) {
        Optional<Workflow> workflow = workflowRepository.findByProjectCodeAndMaterialCodeAndPlantCodeWithQueries(
            projectCode, materialCode, plantCode);
        
        // Initialize documents collection within transaction if workflow exists
        if (workflow.isPresent()) {
            workflow.get().getDocuments().size(); // Trigger lazy loading
        }
        
        return workflow;
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean workflowExists(String projectCode, String materialCode, String plantCode) {
        return workflowRepository.existsByProjectCodeAndMaterialCodeAndPlantCode(
            projectCode, materialCode, plantCode);
    }
    
    @Override
    @Transactional
    public WorkflowService.SmartExtensionResult extendToMultiplePlantsSmartly(String projectCode, String materialCode, 
                                                                             List<String> plantCodes, String initiatedBy) {
        logger.info("Smart plant extension requested for project: {}, material: {}, plants: {}, by user: {}", 
                   projectCode, materialCode, plantCodes, initiatedBy);
        
        List<Workflow> createdWorkflows = new ArrayList<>();
        List<String> skippedPlants = new ArrayList<>();
        List<String> failedPlants = new ArrayList<>();
        List<Workflow> existingWorkflows = new ArrayList<>();
        
        // Get reusable documents once for efficiency - this includes ALL documents from any source
        List<com.cqs.qrmfg.dto.DocumentSummary> reusableDocuments = null;
        try {
            reusableDocuments = documentService.getReusableDocuments(projectCode, materialCode);
            logger.info("Found {} reusable documents for project: {}, material: {}", 
                       reusableDocuments.size(), projectCode, materialCode);
        } catch (Exception e) {
            logger.warn("Failed to retrieve reusable documents for project: {}, material: {} - {}", 
                       projectCode, materialCode, e.getMessage());
            reusableDocuments = new ArrayList<>();
        }
        
        for (String plantCode : plantCodes) {
            try {
                // Check if workflow already exists for this combination
                Optional<Workflow> existingWorkflow = findExistingWorkflow(projectCode, materialCode, plantCode);
                if (existingWorkflow.isPresent()) {
                    logger.debug("Workflow already exists for project: {}, material: {}, plant: {} - skipping", 
                               projectCode, materialCode, plantCode);
                    skippedPlants.add(plantCode);
                    existingWorkflows.add(existingWorkflow.get());
                    continue;
                }
                
                // Create new workflow for this plant
                Workflow workflow = initiateEnhancedWorkflow(projectCode, materialCode, plantCode, initiatedBy);
                
                // Automatically reuse documents from ALL sources (workflow, query, response)
                if (!reusableDocuments.isEmpty()) {
                    try {
                        List<Long> documentIds = reusableDocuments.stream()
                            .map(com.cqs.qrmfg.dto.DocumentSummary::getId)
                            .collect(java.util.stream.Collectors.toList());
                        
                        // Reuse documents and get the reused document summaries
                        List<com.cqs.qrmfg.dto.DocumentSummary> reusedDocuments = 
                            documentService.reuseDocuments(workflow.getId(), documentIds, initiatedBy);
                        
                        // Update workflow with document reuse information
                        workflow.setAutoReusedDocumentCount(reusedDocuments.size());
                        workflow.setDocumentReuseStrategy("AUTOMATIC");
                        
                        // Create source description based on original document sources
                        String sourceDescription = createDocumentReuseSourceDescription(reusableDocuments);
                        workflow.setDocumentReuseSource(sourceDescription);
                        
                        // Save the updated workflow
                        workflowRepository.save(workflow);
                        
                        logger.info("Auto-reused {} documents for workflow {} (project: {}, material: {}, plant: {}) - Sources: {}", 
                                   reusedDocuments.size(), workflow.getId(), projectCode, materialCode, plantCode, sourceDescription);
                        
                        // Log detailed document reuse information
                        for (com.cqs.qrmfg.dto.DocumentSummary doc : reusableDocuments) {
                            logger.debug("Reused document: {} (ID: {}) from project: {}, material: {}", 
                                        doc.getOriginalFileName(), doc.getId(), doc.getProjectCode(), doc.getMaterialCode());
                        }
                        
                    } catch (Exception docError) {
                        logger.warn("Failed to auto-reuse documents for workflow {} - continuing without documents: {}", 
                                   workflow.getId(), docError.getMessage());
                        
                        // Reset document reuse fields on failure
                        workflow.setAutoReusedDocumentCount(0);
                        workflow.setDocumentReuseSource("FAILED");
                        workflow.setDocumentReuseStrategy("AUTOMATIC");
                        workflowRepository.save(workflow);
                    }
                } else {
                    logger.debug("No reusable documents found for project: {}, material: {}", projectCode, materialCode);
                    
                    // Set document reuse information to indicate no documents were available
                    workflow.setAutoReusedDocumentCount(0);
                    workflow.setDocumentReuseSource("NO_DOCUMENTS_AVAILABLE");
                    workflow.setDocumentReuseStrategy("AUTOMATIC");
                    workflowRepository.save(workflow);
                }
                
                createdWorkflows.add(workflow);
                
                logger.info("Successfully created workflow for project: {}, material: {}, plant: {}", 
                           projectCode, materialCode, plantCode);
                
            } catch (Exception e) {
                logger.error("Failed to create workflow for project: {}, material: {}, plant: {} - Error: {}", 
                           projectCode, materialCode, plantCode, e.getMessage());
                failedPlants.add(plantCode);
            }
        }
        
        // Log comprehensive summary
        logger.info("Smart plant extension completed - Created: {}, Skipped: {}, Failed: {}, Total reusable documents: {}", 
                   createdWorkflows.size(), skippedPlants.size(), failedPlants.size(), 
                   reusableDocuments != null ? reusableDocuments.size() : 0);
        
        if (!skippedPlants.isEmpty()) {
            logger.info("Skipped plants (already have workflows): {}", skippedPlants);
        }
        
        if (!failedPlants.isEmpty()) {
            logger.warn("Failed plants (errors occurred): {}", failedPlants);
        }
        
        // Log document reuse statistics
        int totalDocumentsReused = createdWorkflows.stream()
            .mapToInt(w -> w.getAutoReusedDocumentCount() != null ? w.getAutoReusedDocumentCount() : 0)
            .sum();
        
        if (totalDocumentsReused > 0) {
            logger.info("Total documents reused across all created workflows: {}", totalDocumentsReused);
        }
        
        return new WorkflowService.SmartExtensionResult(createdWorkflows, skippedPlants, failedPlants, existingWorkflows, reusableDocuments);
    }
    
    /**
     * Creates a description of document reuse sources for logging and tracking
     */
    private String createDocumentReuseSourceDescription(List<com.cqs.qrmfg.dto.DocumentSummary> reusableDocuments) {
        if (reusableDocuments.isEmpty()) {
            return "NO_DOCUMENTS";
        }
        
        // Count documents by source type (this will be enhanced when document source tracking is implemented)
        long workflowDocs = reusableDocuments.size(); // For now, all are workflow documents
        
        return String.format("WORKFLOW_DOCS:%d", workflowDocs);
    }
}