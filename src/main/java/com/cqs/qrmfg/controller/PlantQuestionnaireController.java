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
            
            // CRITICAL DEBUG: Log field names and sample values received from frontend
            if (responses != null && !responses.isEmpty()) {
                System.out.println("PlantQuestionnaireController: Field names received: " + responses.keySet());
                System.out.println("PlantQuestionnaireController: Sample field values: " + 
                                 responses.entrySet().stream().limit(5).collect(Collectors.toMap(
                                     Map.Entry::getKey, Map.Entry::getValue)));
            }
            
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
     * Debug completion calculation - shows detailed field-by-field analysis
     */
    @GetMapping("/debug-completion/{plantCode}/{materialCode}")
    public ResponseEntity<Map<String, Object>> debugCompletion(
            @PathVariable String plantCode, 
            @PathVariable String materialCode) {
        try {
            System.out.println("PlantQuestionnaireController: Debug completion for " + plantCode + "/" + materialCode);
            
            // Force recalculation first
            plantQuestionnaireService.recalculateCompletionStats(materialCode, plantCode);
            
            // Get updated stats
            Map<String, Object> stats = plantQuestionnaireService.getCompletionStats(materialCode, plantCode);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Completion debug completed - check server logs for detailed field analysis");
            response.put("stats", stats);
            response.put("plantCode", plantCode);
            response.put("materialCode", materialCode);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("PlantQuestionnaireController: Debug completion failed: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Debug completion failed");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Debug endpoint to compare frontend field names with backend template
     */
    @GetMapping("/debug-fields/{plantCode}/{materialCode}")
    public ResponseEntity<Map<String, Object>> debugFieldMatching(
            @PathVariable String plantCode, 
            @PathVariable String materialCode) {
        try {
            // Get template for field comparison
            QuestionnaireTemplateDto template = plantQuestionnaireService.getQuestionnaireTemplate(
                materialCode, plantCode, "PLANT_QUESTIONNAIRE");
            
            // Get plant data for inspection
            PlantSpecificDataDto plantData = plantQuestionnaireService.getPlantSpecificData(plantCode, materialCode);
            
            Map<String, Object> response = new HashMap<>();
            
            // Template field analysis
            if (template != null) {
                List<String> allTemplateFields = new ArrayList<>();
                List<String> plantTemplateFields = new ArrayList<>();
                List<String> cqsTemplateFields = new ArrayList<>();
                
                for (QuestionnaireStepDto step : template.getSteps()) {
                    for (QuestionnaireFieldDto field : step.getFields()) {
                        allTemplateFields.add(field.getName());
                        if (field.isCqsAutoPopulated()) {
                            cqsTemplateFields.add(field.getName());
                        } else {
                            plantTemplateFields.add(field.getName());
                        }
                    }
                }
                
                Map<String, Object> templateInfo = new HashMap<>();
                templateInfo.put("totalFields", allTemplateFields.size());
                templateInfo.put("plantFields", plantTemplateFields.size());
                templateInfo.put("cqsFields", cqsTemplateFields.size());
                templateInfo.put("allFieldNames", allTemplateFields);
                templateInfo.put("plantFieldNames", plantTemplateFields);
                templateInfo.put("cqsFieldNames", cqsTemplateFields);
                response.put("template", templateInfo);
            }
            
            // Plant data analysis
            if (plantData != null && plantData.getPlantInputs() != null) {
                Set<String> plantInputKeys = plantData.getPlantInputs().keySet();
                Map<String, Object> plantDataInfo = new HashMap<>();
                plantDataInfo.put("inputFieldCount", plantInputKeys.size());
                plantDataInfo.put("inputFieldNames", new ArrayList<>(plantInputKeys));
                
                // Show sample values for debugging
                Map<String, Object> sampleValues = new HashMap<>();
                plantData.getPlantInputs().entrySet().stream()
                    .limit(10)
                    .forEach(entry -> sampleValues.put(entry.getKey(), entry.getValue()));
                plantDataInfo.put("sampleValues", sampleValues);
                
                response.put("plantData", plantDataInfo);
                
                // Field matching analysis
                if (template != null) {
                    List<String> matchedFields = new ArrayList<>();
                    List<String> unmatchedPlantInputs = new ArrayList<>();
                    List<String> unmatchedTemplateFields = new ArrayList<>();
                    
                    // Check which plant inputs match template fields
                    for (String inputKey : plantInputKeys) {
                        boolean found = false;
                        for (QuestionnaireStepDto step : template.getSteps()) {
                            for (QuestionnaireFieldDto field : step.getFields()) {
                                if (field.getName().equals(inputKey)) {
                                    matchedFields.add(inputKey);
                                    found = true;
                                    break;
                                }
                            }
                            if (found) break;
                        }
                        if (!found) {
                            unmatchedPlantInputs.add(inputKey);
                        }
                    }
                    
                    // Check which template fields don't have plant inputs
                    for (QuestionnaireStepDto step : template.getSteps()) {
                        for (QuestionnaireFieldDto field : step.getFields()) {
                            if (!field.isCqsAutoPopulated() && !plantInputKeys.contains(field.getName())) {
                                unmatchedTemplateFields.add(field.getName());
                            }
                        }
                    }
                    
                    Map<String, Object> matchingInfo = new HashMap<>();
                    matchingInfo.put("matchedFields", matchedFields);
                    matchingInfo.put("matchedCount", matchedFields.size());
                    matchingInfo.put("unmatchedPlantInputs", unmatchedPlantInputs);
                    matchingInfo.put("unmatchedTemplateFields", unmatchedTemplateFields);
                    matchingInfo.put("unmatchedTemplateCount", unmatchedTemplateFields.size());
                    response.put("fieldMatching", matchingInfo);
                }
            }
            
            response.put("timestamp", LocalDateTime.now());
            response.put("materialCode", materialCode);
            response.put("plantCode", plantCode);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to debug field matching");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Test validation endpoint to debug completion calculation
     */
    @GetMapping("/test-validation/{plantCode}/{materialCode}")
    public ResponseEntity<Map<String, Object>> testValidation(
            @PathVariable String plantCode, 
            @PathVariable String materialCode) {
        try {
            // Force recalculation first
            plantQuestionnaireService.recalculateCompletionStats(materialCode, plantCode);
            
            // Get validation result
            PlantQuestionnaireService.ValidationResult validation = 
                plantQuestionnaireService.validateQuestionnaireCompletion(plantCode, materialCode);
            
            // Get plant data for inspection
            PlantSpecificDataDto plantData = plantQuestionnaireService.getPlantSpecificData(plantCode, materialCode);
            
            // Get template for field comparison
            QuestionnaireTemplateDto template = plantQuestionnaireService.getQuestionnaireTemplate(
                materialCode, plantCode, "PLANT_QUESTIONNAIRE");
            
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> validationInfo = new HashMap<>();
            validationInfo.put("isValid", validation.isValid());
            validationInfo.put("message", validation.getMessage());
            validationInfo.put("completionPercentage", validation.getCompletionPercentage());
            validationInfo.put("missingFieldsCount", validation.getMissingFields().size());
            validationInfo.put("missingFields", validation.getMissingFields());
            response.put("validation", validationInfo);
            
            if (plantData != null) {
                Map<String, Object> plantDataInfo = new HashMap<>();
                plantDataInfo.put("totalFields", plantData.getTotalFields() != null ? plantData.getTotalFields() : 0);
                plantDataInfo.put("completedFields", plantData.getCompletedFields() != null ? plantData.getCompletedFields() : 0);
                plantDataInfo.put("plantInputsSize", plantData.getPlantInputs() != null ? plantData.getPlantInputs().size() : 0);
                plantDataInfo.put("cqsInputsSize", plantData.getCqsInputs() != null ? plantData.getCqsInputs().size() : 0);
                plantDataInfo.put("completionPercentage", plantData.getCompletionPercentage() != null ? plantData.getCompletionPercentage() : 0);
                
                // Show actual plant input keys vs template field names
                if (plantData.getPlantInputs() != null) {
                    plantDataInfo.put("plantInputKeys", new ArrayList<>(plantData.getPlantInputs().keySet()));
                }
                response.put("plantData", plantDataInfo);
            }
            
            // Show template field names for comparison
            if (template != null) {
                List<String> templateFieldNames = new ArrayList<>();
                List<String> plantFieldNames = new ArrayList<>();
                List<String> cqsFieldNames = new ArrayList<>();
                
                for (QuestionnaireStepDto step : template.getSteps()) {
                    for (QuestionnaireFieldDto field : step.getFields()) {
                        templateFieldNames.add(field.getName());
                        if (field.isCqsAutoPopulated()) {
                            cqsFieldNames.add(field.getName());
                        } else {
                            plantFieldNames.add(field.getName());
                        }
                    }
                }
                
                Map<String, Object> templateInfo = new HashMap<>();
                templateInfo.put("totalFields", templateFieldNames.size());
                templateInfo.put("plantFields", plantFieldNames.size());
                templateInfo.put("cqsFields", cqsFieldNames.size());
                templateInfo.put("plantFieldNames", plantFieldNames);
                templateInfo.put("cqsFieldNames", cqsFieldNames);
                response.put("template", templateInfo);
            }
            
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to test validation");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("plantCode", plantCode);
            errorResponse.put("materialCode", materialCode);
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    

    
    /**
     * Debug endpoint to check plant-specific data status
     */
    @GetMapping("/debug/{plantCode}/{materialCode}")
    public ResponseEntity<Map<String, Object>> debugPlantData(
            @PathVariable String plantCode, 
            @PathVariable String materialCode) {
        try {
            Map<String, Object> debugInfo = new HashMap<>();
            
            // Get plant-specific data
            PlantSpecificDataDto plantData = plantQuestionnaireService.getPlantSpecificData(plantCode, materialCode);
            
            debugInfo.put("plantCode", plantCode);
            debugInfo.put("materialCode", materialCode);
            debugInfo.put("dataExists", plantData != null);
            
            if (plantData != null) {
                debugInfo.put("cqsInputsEmpty", plantData.getCqsInputs() == null || plantData.getCqsInputs().isEmpty());
                debugInfo.put("plantInputsEmpty", plantData.getPlantInputs() == null || plantData.getPlantInputs().isEmpty());
                debugInfo.put("cqsSyncStatus", plantData.getCqsSyncStatus());
                debugInfo.put("completionStatus", plantData.getCompletionStatus());
                debugInfo.put("completionPercentage", plantData.getCompletionPercentage());
                debugInfo.put("totalFields", plantData.getTotalFields());
                debugInfo.put("completedFields", plantData.getCompletedFields());
                debugInfo.put("lastUpdated", plantData.getUpdatedAt());
                debugInfo.put("updatedBy", plantData.getUpdatedBy());
                
                if (plantData.getCqsInputs() != null) {
                    debugInfo.put("cqsInputsSize", plantData.getCqsInputs().size());
                }
                
                if (plantData.getPlantInputs() != null) {
                    debugInfo.put("plantInputsSize", plantData.getPlantInputs().size());
                    debugInfo.put("plantInputsSample", plantData.getPlantInputs().entrySet().stream()
                        .limit(5)
                        .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue
                        )));
                }
            }
            
            // Check if CQS data exists
            try {
                CqsDataDto cqsData = plantQuestionnaireService.getCqsData(materialCode, plantCode);
                debugInfo.put("cqsDataExists", cqsData != null && cqsData.getCqsData() != null);
                if (cqsData != null) {
                    debugInfo.put("cqsDataSize", cqsData.getCqsData() != null ? cqsData.getCqsData().size() : 0);
                    debugInfo.put("cqsSyncStatus", cqsData.getSyncStatus());
                }
            } catch (Exception e) {
                debugInfo.put("cqsDataError", e.getMessage());
            }
            
            // Test completion validation
            try {
                PlantQuestionnaireService.ValidationResult validation = 
                    plantQuestionnaireService.validateQuestionnaireCompletion(plantCode, materialCode);
                Map<String, Object> validationMap = new HashMap<>();
                validationMap.put("isValid", validation.isValid());
                validationMap.put("message", validation.getMessage());
                validationMap.put("completionPercentage", validation.getCompletionPercentage());
                validationMap.put("missingFieldsCount", validation.getMissingFields().size());
                debugInfo.put("validationResult", validationMap);
            } catch (Exception e) {
                debugInfo.put("validationError", e.getMessage());
            }
            
            debugInfo.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get debug info");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("plantCode", plantCode);
            errorResponse.put("materialCode", materialCode);
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}

