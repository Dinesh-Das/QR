package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.dto.*;
import com.cqs.qrmfg.service.PlantQuestionnaireService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api/v1/plant-questionnaire")
public class PlantQuestionnaireController {

    @Autowired
    private PlantQuestionnaireService plantQuestionnaireService;
    
    @Autowired
    private com.cqs.qrmfg.service.WorkflowService workflowService;

    /**
     * Get questionnaire template from backend template table
     */
    @GetMapping("/template")
    public ResponseEntity<QuestionnaireTemplateDto> getQuestionnaireTemplate(
            @RequestParam String materialCode,
            @RequestParam String plantCode,
            @RequestParam(defaultValue = "PLANT_QUESTIONNAIRE") String templateType) {
        
        try {
            QuestionnaireTemplateDto template = plantQuestionnaireService.getQuestionnaireTemplate(
                materialCode, plantCode, templateType);
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get questionnaire template by workflow ID
     */
    @GetMapping("/workflow/{workflowId}/template")
    public ResponseEntity<QuestionnaireTemplateDto> getQuestionnaireTemplateByWorkflow(
            @PathVariable Long workflowId,
            @RequestParam(defaultValue = "PLANT_QUESTIONNAIRE") String templateType) {
        
        try {
            // Get workflow details
            Optional<com.cqs.qrmfg.model.Workflow> workflowOpt = workflowService.findById(workflowId);
            if (!workflowOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            com.cqs.qrmfg.model.Workflow workflow = workflowOpt.get();
            String materialCode = workflow.getMaterialCode();
            String plantCode = workflow.getPlantCode();
            
            QuestionnaireTemplateDto template = plantQuestionnaireService.getQuestionnaireTemplate(
                materialCode, plantCode, templateType);
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get CQS auto-populated data
     */
    @GetMapping("/cqs-data")
    public ResponseEntity<CqsDataDto> getCqsData(
            @RequestParam String materialCode,
            @RequestParam String plantCode) {
        
        try {
            CqsDataDto cqsData = plantQuestionnaireService.getCqsData(materialCode, plantCode);
            return ResponseEntity.ok(cqsData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get CQS auto-populated data by workflow ID
     */
    @GetMapping("/workflow/{workflowId}/cqs-data")
    public ResponseEntity<CqsDataDto> getCqsDataByWorkflow(@PathVariable Long workflowId) {
        
        try {
            // Get workflow details
            Optional<com.cqs.qrmfg.model.Workflow> workflowOpt = workflowService.findById(workflowId);
            if (!workflowOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            com.cqs.qrmfg.model.Workflow workflow = workflowOpt.get();
            String materialCode = workflow.getMaterialCode();
            String plantCode = workflow.getPlantCode();
            
            CqsDataDto cqsData = plantQuestionnaireService.getCqsData(materialCode, plantCode);
            return ResponseEntity.ok(cqsData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get plant-specific data by composite key
     */
    @GetMapping("/plant-data")
    public ResponseEntity<PlantSpecificDataDto> getPlantSpecificData(
            @RequestParam String plantCode,
            @RequestParam String materialCode) {
        
        try {
            PlantSpecificDataDto plantData = plantQuestionnaireService.getPlantSpecificData(
                plantCode, materialCode);
            
            if (plantData != null) {
                return ResponseEntity.ok(plantData);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get plant-specific data by workflow ID
     */
    @GetMapping("/workflow/{workflowId}/plant-data")
    public ResponseEntity<PlantSpecificDataDto> getPlantSpecificDataByWorkflow(@PathVariable Long workflowId) {
        
        try {
            // Get workflow details
            Optional<com.cqs.qrmfg.model.Workflow> workflowOpt = workflowService.findById(workflowId);
            if (!workflowOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            com.cqs.qrmfg.model.Workflow workflow = workflowOpt.get();
            String materialCode = workflow.getMaterialCode();
            String plantCode = workflow.getPlantCode();
            
            PlantSpecificDataDto plantData = plantQuestionnaireService.getPlantSpecificData(
                plantCode, materialCode);
            
            if (plantData != null) {
                return ResponseEntity.ok(plantData);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get or create plant-specific data record
     */
    @PostMapping("/plant-data/init")
    public ResponseEntity<PlantSpecificDataDto> getOrCreatePlantSpecificData(
            @RequestParam String plantCode,
            @RequestParam String materialCode,
            @RequestParam Long workflowId) {
        
        try {
            PlantSpecificDataDto plantData = plantQuestionnaireService.getOrCreatePlantSpecificData(
                plantCode, materialCode, workflowId);
            return ResponseEntity.ok(plantData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Save plant-specific data with composite key
     */
    @PostMapping("/plant-data/save")
    public ResponseEntity<String> savePlantSpecificData(
            @RequestBody PlantSpecificDataDto dataDto,
            @RequestParam(defaultValue = "SYSTEM") String modifiedBy) {
        
        try {
            plantQuestionnaireService.savePlantSpecificData(dataDto, modifiedBy);
            return ResponseEntity.ok("Plant-specific data saved successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to save plant-specific data: " + e.getMessage());
        }
    }

    /**
     * Save draft responses for plant questionnaire with change detection
     */
    @PostMapping("/draft")
    public ResponseEntity<Map<String, Object>> saveDraftResponses(
            @RequestParam Long workflowId,
            @RequestBody Map<String, Object> draftData) {
        
        try {
            // Extract plant-specific data from draft
            String plantCode = (String) draftData.get("plantCode");
            String materialCode = (String) draftData.get("materialCode");
            String modifiedBy = (String) draftData.getOrDefault("modifiedBy", "SYSTEM");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> responses = (Map<String, Object>) draftData.get("responses");
            
            System.out.println("PlantQuestionnaireController: Saving draft for " + plantCode + "/" + materialCode + 
                             " with " + (responses != null ? responses.size() : 0) + " responses");
            

            
            // Validate required parameters
            if (plantCode == null || materialCode == null) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "Plant code and material code are required");
                return ResponseEntity.badRequest().body(errorResult);
            }
            
            // CRITICAL: Check if questionnaire is read-only
            boolean isReadOnly = plantQuestionnaireService.isQuestionnaireReadOnly(plantCode, materialCode);
            if (isReadOnly) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "Cannot save draft - questionnaire has been submitted and is read-only");
                errorResult.put("isReadOnly", true);
                return ResponseEntity.badRequest().body(errorResult);
            }
            
            // Get existing plant-specific data to check for changes
            PlantSpecificDataDto existingData = plantQuestionnaireService.getPlantSpecificData(plantCode, materialCode);
            
            // Check if there are actual changes
            boolean hasChanges = plantQuestionnaireService.hasPlantInputChanges(existingData, responses);
            
            Map<String, Object> result = new HashMap<>();
            
            if (!hasChanges && existingData != null) {
                // No changes detected, skip save
                result.put("success", true);
                result.put("message", "No changes detected - draft not saved");
                result.put("hasChanges", false);
                result.put("timestamp", LocalDateTime.now());
                return ResponseEntity.ok(result);
            }
            
            // Create or update plant-specific data
            PlantSpecificDataDto plantDataDto = plantQuestionnaireService.getOrCreatePlantSpecificData(
                plantCode, materialCode, workflowId);
            
            // Set plant inputs
            if (responses != null && !responses.isEmpty()) {
                plantDataDto.setPlantInputs(responses);
                System.out.println("PlantQuestionnaireController: Setting plant inputs with " + responses.size() + " fields");
            }
            
            plantDataDto.setWorkflowId(workflowId);
            
            // Save the data first
            plantQuestionnaireService.savePlantSpecificData(plantDataDto, modifiedBy);
            
            // CRITICAL: Recalculate completion stats AFTER saving to ensure accurate counts
            plantQuestionnaireService.recalculateCompletionStats(materialCode, plantCode);
            
            // Get updated completion stats
            Map<String, Object> completionStats = plantQuestionnaireService.getCompletionStats(materialCode, plantCode);
            
            // Verify the save was successful
            PlantSpecificDataDto savedData = plantQuestionnaireService.getPlantSpecificData(plantCode, materialCode);
            boolean saveSuccessful = savedData != null && savedData.getPlantInputs() != null && 
                                   !savedData.getPlantInputs().isEmpty();
            
            result.put("success", saveSuccessful);
            result.put("message", saveSuccessful ? "Draft saved successfully" : "Draft save may have failed - please verify");
            result.put("hasChanges", true);
            result.put("savedFields", responses != null ? responses.size() : 0);
            result.put("timestamp", LocalDateTime.now());
            result.put("plantCode", plantCode);
            result.put("materialCode", materialCode);
            
            // Include updated completion stats
            result.putAll(completionStats);
            
            System.out.println("PlantQuestionnaireController: Draft save completed - success: " + saveSuccessful + 
                             ", completion: " + completionStats.get("completedFields") + "/" + completionStats.get("totalFields"));
            
            System.out.println("PlantQuestionnaireController: Draft save completed - success: " + saveSuccessful);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            System.err.println("PlantQuestionnaireController: Failed to save draft: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "Failed to save draft: " + e.getMessage());
            errorResult.put("timestamp", LocalDateTime.now());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    /**
     * Submit plant questionnaire with validation
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitQuestionnaire(
            @RequestParam Long workflowId,
            @RequestBody Map<String, Object> submissionData) {
        
        try {
            String plantCode = (String) submissionData.get("plantCode");
            String materialCode = (String) submissionData.get("materialCode");
            String submittedBy = (String) submissionData.getOrDefault("submittedBy", "SYSTEM");
            
            System.out.println("PlantQuestionnaireController: Submitting questionnaire for " + plantCode + "/" + materialCode);
            
            // CRITICAL: Check if questionnaire is already submitted
            boolean isReadOnly = plantQuestionnaireService.isQuestionnaireReadOnly(plantCode, materialCode);
            if (isReadOnly) {
                System.out.println("PlantQuestionnaireController: Questionnaire already submitted - returning error");
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "Cannot submit - questionnaire has already been submitted");
                errorResult.put("isReadOnly", true);
                
                // Include current status for frontend
                Map<String, Object> status = plantQuestionnaireService.getQuestionnaireStatus(plantCode, materialCode);
                errorResult.putAll(status);
                
                return ResponseEntity.badRequest().body(errorResult);
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> responses = (Map<String, Object>) submissionData.get("responses");
            
            // Get existing plant data to merge with new responses
            PlantSpecificDataDto plantDataDto = plantQuestionnaireService.getOrCreatePlantSpecificData(
                plantCode, materialCode, workflowId);
            
            if (responses != null && !responses.isEmpty()) {
                // CRITICAL FIX: Merge new responses with existing data instead of replacing
                Map<String, Object> existingInputs = plantDataDto.getPlantInputs();
                if (existingInputs == null) {
                    existingInputs = new HashMap<>();
                }
                
                // Merge new responses with existing data
                Map<String, Object> mergedInputs = new HashMap<>(existingInputs);
                mergedInputs.putAll(responses);
                
                plantDataDto.setPlantInputs(mergedInputs);
                System.out.println("PlantQuestionnaireController: Merging " + responses.size() + 
                                 " new responses with " + existingInputs.size() + 
                                 " existing responses = " + mergedInputs.size() + " total responses");
                
                // Log some sample merged data
                System.out.println("PlantQuestionnaireController: Sample merged data:");
                mergedInputs.entrySet().stream()
                    .limit(5)
                    .forEach(entry -> System.out.println("  " + entry.getKey() + " = " + entry.getValue()));
            }
            
            plantDataDto.setWorkflowId(workflowId);
            
            // Save the data first
            plantQuestionnaireService.savePlantSpecificData(plantDataDto, submittedBy);
            
            // CRITICAL: Recalculate completion stats AFTER saving to ensure accurate counts
            plantQuestionnaireService.recalculateCompletionStats(materialCode, plantCode);
            
            // Submit the questionnaire with validation
            Map<String, Object> result = plantQuestionnaireService.submitPlantQuestionnaire(plantCode, materialCode, submittedBy);
            
            System.out.println("PlantQuestionnaireController: Submission result - success: " + result.get("success") + 
                             ", message: " + result.get("message"));
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("PlantQuestionnaireController: Submission failed: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "Failed to submit questionnaire: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
    
    /**
     * Validate questionnaire completion
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateQuestionnaire(
            @RequestParam String plantCode,
            @RequestParam String materialCode) {
        
        try {
            PlantQuestionnaireService.ValidationResult validation = 
                plantQuestionnaireService.validateQuestionnaireCompletion(plantCode, materialCode);
            
            Map<String, Object> result = new HashMap<>();
            result.put("isValid", validation.isValid());
            result.put("message", validation.getMessage());
            result.put("missingFields", validation.getMissingFields());
            result.put("completionPercentage", validation.getCompletionPercentage());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("isValid", false);
            errorResult.put("message", "Validation failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
    
    /**
     * Get questionnaire status (submitted, read-only, etc.)
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getQuestionnaireStatus(
            @RequestParam String plantCode,
            @RequestParam String materialCode) {
        
        try {
            Map<String, Object> status = plantQuestionnaireService.getQuestionnaireStatus(plantCode, materialCode);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Failed to get questionnaire status: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
    
    /**
     * Check if questionnaire is read-only
     */
    @GetMapping("/readonly")
    public ResponseEntity<Map<String, Object>> isQuestionnaireReadOnly(
            @RequestParam String plantCode,
            @RequestParam String materialCode) {
        
        try {
            boolean isReadOnly = plantQuestionnaireService.isQuestionnaireReadOnly(plantCode, materialCode);
            
            Map<String, Object> result = new HashMap<>();
            result.put("isReadOnly", isReadOnly);
            result.put("plantCode", plantCode);
            result.put("materialCode", materialCode);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Failed to check read-only status: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    /**
     * Get plant questionnaire completion statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPlantQuestionnaireStats(
            @RequestParam String plantCode,
            @RequestParam String materialCode) {
        
        try {
            Map<String, Object> stats = plantQuestionnaireService.getCompletionStats(materialCode, plantCode);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get completion stats");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Get plant dashboard data with progress information
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getPlantDashboardData(
            @RequestParam String plantCode) {
        
        try {
            Map<String, Object> dashboardData = plantQuestionnaireService.getPlantDashboardData(plantCode);
            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get complete questionnaire data for a workflow (template + CQS data + plant data)
     */
    @GetMapping("/workflow/{workflowId}")
    public ResponseEntity<Map<String, Object>> getCompleteQuestionnaireByWorkflow(@PathVariable Long workflowId) {
        
        try {
            // Get workflow details
            Optional<com.cqs.qrmfg.model.Workflow> workflowOpt = workflowService.findById(workflowId);
            if (!workflowOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            com.cqs.qrmfg.model.Workflow workflow = workflowOpt.get();
            String materialCode = workflow.getMaterialCode();
            String plantCode = workflow.getPlantCode();
            
            Map<String, Object> response = new HashMap<>();
            
            // CRITICAL: Check if questionnaire is read-only first
            boolean isReadOnly = plantQuestionnaireService.isQuestionnaireReadOnly(plantCode, materialCode);
            response.put("isReadOnly", isReadOnly);
            
            // Get questionnaire template
            QuestionnaireTemplateDto template = plantQuestionnaireService.getQuestionnaireTemplate(
                materialCode, plantCode, "PLANT_QUESTIONNAIRE");
            response.put("template", template);
            
            // Get CQS data
            CqsDataDto cqsData = plantQuestionnaireService.getCqsData(materialCode, plantCode);
            response.put("cqsData", cqsData);
            
            // Get or create plant-specific data (but don't create if read-only)
            PlantSpecificDataDto plantData;
            if (isReadOnly) {
                plantData = plantQuestionnaireService.getPlantSpecificData(plantCode, materialCode);
            } else {
                plantData = plantQuestionnaireService.getOrCreatePlantSpecificData(
                    plantCode, materialCode, workflowId);
            }
            response.put("plantData", plantData);
            
            // Add workflow information
            Map<String, Object> workflowInfo = new HashMap<>();
            workflowInfo.put("id", workflow.getId());
            workflowInfo.put("materialCode", materialCode);
            workflowInfo.put("plantCode", plantCode);
            workflowInfo.put("state", workflow.getState());
            workflowInfo.put("materialName", workflow.getMaterialName());
            workflowInfo.put("isReadOnly", isReadOnly);
            response.put("workflow", workflowInfo);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to load questionnaire data");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Initialize sample plant data for testing
     */
    @PostMapping("/init-sample-data")
    public ResponseEntity<String> initializeSampleData(
            @RequestParam String plantCode) {
        
        try {
            plantQuestionnaireService.initializeSamplePlantData(plantCode);
            return ResponseEntity.ok("Sample data initialized for plant: " + plantCode);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to initialize sample data: " + e.getMessage());
        }
    }
    
    /**
     * Force recalculation of completion stats for a material
     */
    @PostMapping("/recalculate-progress/{plantCode}/{materialCode}")
    public ResponseEntity<Map<String, Object>> recalculateProgress(
            @PathVariable String plantCode, 
            @PathVariable String materialCode) {
        try {
            System.out.println("PlantQuestionnaireController: Recalculating progress for " + plantCode + "/" + materialCode);
            
            plantQuestionnaireService.recalculateCompletionStats(materialCode, plantCode);
            
            // Get updated stats to return
            Map<String, Object> stats = plantQuestionnaireService.getCompletionStats(materialCode, plantCode);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Completion stats recalculated successfully");
            response.put("materialCode", materialCode);
            response.put("plantCode", plantCode);
            response.put("timestamp", LocalDateTime.now());
            response.putAll(stats); // Include updated stats in response
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("PlantQuestionnaireController: Failed to recalculate progress: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to recalculate completion stats");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("materialCode", materialCode);
            errorResponse.put("plantCode", plantCode);
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Force sync CQS data for a specific plant and material
     */
    @PostMapping("/sync-cqs-data/{plantCode}/{materialCode}")
    public ResponseEntity<Map<String, Object>> syncCqsData(
            @PathVariable String plantCode, 
            @PathVariable String materialCode) {
        try {
            plantQuestionnaireService.forceSyncCqsData(plantCode, materialCode);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "CQS data synced successfully");
            response.put("materialCode", materialCode);
            response.put("plantCode", plantCode);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to sync CQS data");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("materialCode", materialCode);
            errorResponse.put("plantCode", plantCode);
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Force field name synchronization between frontend and backend
     */
    @PostMapping("/sync-field-names/{plantCode}/{materialCode}")
    public ResponseEntity<Map<String, Object>> syncFieldNames(
            @PathVariable String plantCode, 
            @PathVariable String materialCode) {
        try {
            // Force recalculation with enhanced field matching
            plantQuestionnaireService.recalculateCompletionStats(materialCode, plantCode);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Field names synchronized and completion stats recalculated");
            response.put("materialCode", materialCode);
            response.put("plantCode", plantCode);
            response.put("timestamp", LocalDateTime.now());
            
            // Get updated stats
            Map<String, Object> stats = plantQuestionnaireService.getCompletionStats(materialCode, plantCode);
            response.putAll(stats);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to sync field names");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Debug endpoint to fix completion status mismatch
     */
    @PostMapping("/fix-completion-status/{plantCode}/{materialCode}")
    public ResponseEntity<Map<String, Object>> fixCompletionStatus(
            @PathVariable String plantCode, 
            @PathVariable String materialCode) {
        try {
            System.out.println("PlantQuestionnaireController: Fixing completion status for " + plantCode + "/" + materialCode);
            
            // Step 1: Force recalculate completion stats
            plantQuestionnaireService.recalculateCompletionStats(materialCode, plantCode);
            
            // Step 2: Get current status
            Map<String, Object> currentStatus = plantQuestionnaireService.getQuestionnaireStatus(plantCode, materialCode);
            
            // Step 3: If submitted but completion percentage is low, force it to 100%
            boolean isSubmitted = (Boolean) currentStatus.getOrDefault("isSubmitted", false);
            Integer completionPercentage = (Integer) currentStatus.getOrDefault("completionPercentage", 0);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Completion status analysis completed");
            response.put("materialCode", materialCode);
            response.put("plantCode", plantCode);
            response.put("timestamp", LocalDateTime.now());
            response.put("wasSubmitted", isSubmitted);
            response.put("originalCompletionPercentage", completionPercentage);
            
            if (isSubmitted && completionPercentage < 100) {
                // Force completion to 100% for submitted questionnaires
                PlantSpecificDataDto plantData = plantQuestionnaireService.getPlantSpecificData(plantCode, materialCode);
                if (plantData != null) {
                    plantData.setCompletionPercentage(100);
                    plantData.setCompletionStatus("COMPLETED");
                    plantQuestionnaireService.savePlantSpecificData(plantData, "SYSTEM_FIX");
                    
                    response.put("action", "Forced completion percentage to 100% for submitted questionnaire");
                    response.put("newCompletionPercentage", 100);
                }
            } else if (isSubmitted) {
                response.put("action", "No fix needed - submitted questionnaire already at " + completionPercentage + "%");
            } else {
                response.put("action", "No fix needed - questionnaire not submitted yet");
            }
            
            // Get final stats
            Map<String, Object> finalStats = plantQuestionnaireService.getCompletionStats(materialCode, plantCode);
            response.put("finalStats", finalStats);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("PlantQuestionnaireController: Failed to fix completion status: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fix completion status");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("materialCode", materialCode);
            errorResponse.put("plantCode", plantCode);
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    

    /**
     * Compare frontend fields with backend template
     */
    @GetMapping("/compare-fields/{plantCode}/{materialCode}")
    public ResponseEntity<Map<String, Object>> compareFields(
            @PathVariable String plantCode, 
            @PathVariable String materialCode) {
        try {
            System.out.println("PlantQuestionnaireController: Comparing fields for " + plantCode + "/" + materialCode);
            
            // Get template
            QuestionnaireTemplateDto template = plantQuestionnaireService.getQuestionnaireTemplate(
                materialCode, plantCode, "PLANT_QUESTIONNAIRE");
            
            // Get plant data
            PlantSpecificDataDto plantData = plantQuestionnaireService.getPlantSpecificData(plantCode, materialCode);
            
            // Extract template field names
            List<String> templateFields = new ArrayList<>();
            List<String> cqsFields = new ArrayList<>();
            List<String> plantFields = new ArrayList<>();
            
            for (QuestionnaireStepDto step : template.getSteps()) {
                for (QuestionnaireFieldDto field : step.getFields()) {
                    templateFields.add(field.getName());
                    if (field.isCqsAutoPopulated()) {
                        cqsFields.add(field.getName());
                    } else {
                        plantFields.add(field.getName());
                    }
                }
            }
            
            // Extract frontend field names
            Set<String> frontendFields = plantData != null && plantData.getPlantInputs() != null ? 
                plantData.getPlantInputs().keySet() : new HashSet<>();
            
            Map<String, Object> response = new HashMap<>();
            response.put("templateFieldCount", templateFields.size());
            response.put("cqsFieldCount", cqsFields.size());
            response.put("plantFieldCount", plantFields.size());
            response.put("frontendFieldCount", frontendFields.size());
            
            response.put("templateFields", templateFields);
            response.put("cqsFields", cqsFields);
            response.put("plantTemplateFields", plantFields);
            response.put("frontendFields", new ArrayList<>(frontendFields));
            
            // Find mismatches
            List<String> frontendOnly = new ArrayList<>(frontendFields);
            frontendOnly.removeAll(templateFields);
            
            List<String> templateOnly = new ArrayList<>(templateFields);
            templateOnly.removeAll(frontendFields);
            
            response.put("frontendOnlyFields", frontendOnly);
            response.put("templateOnlyFields", templateOnly);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("PlantQuestionnaireController: Compare fields failed: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Compare fields failed");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Force completion recalculation with enhanced direct approach
     */
    @PostMapping("/force-recalc/{plantCode}/{materialCode}")
    public ResponseEntity<Map<String, Object>> forceRecalculation(
            @PathVariable String plantCode, 
            @PathVariable String materialCode) {
        try {
            System.out.println("PlantQuestionnaireController: FORCE RECALCULATION for " + plantCode + "/" + materialCode);
            
            // Force recalculation with new direct approach
            plantQuestionnaireService.recalculateCompletionStats(materialCode, plantCode);
            
            // Get updated stats
            Map<String, Object> stats = plantQuestionnaireService.getCompletionStats(materialCode, plantCode);
            
            // Test validation
            PlantQuestionnaireService.ValidationResult validation = 
                plantQuestionnaireService.validateQuestionnaireCompletion(plantCode, materialCode);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "FORCE RECALCULATION completed - check server logs for detailed analysis");
            response.put("stats", stats);
            Map<String, Object> validationMap = new HashMap<>();
            validationMap.put("isValid", validation.isValid());
            validationMap.put("message", validation.getMessage());
            validationMap.put("completionPercentage", validation.getCompletionPercentage());
            response.put("validation", validationMap);
            response.put("plantCode", plantCode);
            response.put("materialCode", materialCode);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("PlantQuestionnaireController: Force recalculation failed: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Force recalculation failed");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Fix workflow status for completed questionnaires (admin/debugging endpoint)
     */
    @PostMapping("/fix-workflow-status")
    public ResponseEntity<Map<String, Object>> fixWorkflowStatus(
            @RequestParam String plantCode,
            @RequestParam String materialCode,
            @RequestParam(defaultValue = "SYSTEM") String updatedBy) {
        
        try {
            Map<String, Object> result = plantQuestionnaireService.fixWorkflowStatusForCompletedQuestionnaire(
                plantCode, materialCode, updatedBy);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Fix workflow status failed");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}

