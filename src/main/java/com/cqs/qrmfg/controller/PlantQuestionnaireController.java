package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.dto.*;
import com.cqs.qrmfg.service.PlantQuestionnaireService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
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
     * Save draft responses for plant questionnaire
     */
    @PostMapping("/draft")
    public ResponseEntity<String> saveDraftResponses(
            @RequestParam Long workflowId,
            @RequestBody Map<String, Object> draftData) {
        
        try {
            // Extract plant-specific data from draft
            String plantCode = (String) draftData.get("plantCode");
            String materialCode = (String) draftData.get("materialCode");
            String modifiedBy = (String) draftData.getOrDefault("modifiedBy", "SYSTEM");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> responses = (Map<String, Object>) draftData.get("responses");
            
            // Create or update plant-specific data
            PlantSpecificDataDto plantDataDto = plantQuestionnaireService.getOrCreatePlantSpecificData(
                plantCode, materialCode, workflowId);
            
            plantDataDto.setPlantInputs(responses);
            plantDataDto.setWorkflowId(workflowId);
            
            // Calculate completion stats
            if (responses != null) {
                int totalFields = responses.size();
                int completedFields = (int) responses.values().stream()
                    .filter(value -> value != null && !value.toString().trim().isEmpty())
                    .count();
                
                plantDataDto.setTotalFields(totalFields);
                plantDataDto.setCompletedFields(completedFields);
                // Note: Required fields calculation would need template information
            }
            
            plantQuestionnaireService.savePlantSpecificData(plantDataDto, modifiedBy);
            
            return ResponseEntity.ok("Draft saved successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to save draft: " + e.getMessage());
        }
    }

    /**
     * Submit plant questionnaire
     */
    @PostMapping("/submit")
    public ResponseEntity<String> submitQuestionnaire(
            @RequestParam Long workflowId,
            @RequestBody Map<String, Object> submissionData) {
        
        try {
            String plantCode = (String) submissionData.get("plantCode");
            String materialCode = (String) submissionData.get("materialCode");
            String submittedBy = (String) submissionData.getOrDefault("submittedBy", "SYSTEM");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> responses = (Map<String, Object>) submissionData.get("responses");
            
            // Save final responses
            PlantSpecificDataDto plantDataDto = plantQuestionnaireService.getOrCreatePlantSpecificData(
                plantCode, materialCode, workflowId);
            
            plantDataDto.setPlantInputs(responses);
            plantDataDto.setWorkflowId(workflowId);
            
            // Calculate final completion stats
            if (responses != null) {
                int totalFields = responses.size();
                int completedFields = (int) responses.values().stream()
                    .filter(value -> value != null && !value.toString().trim().isEmpty())
                    .count();
                
                plantDataDto.setTotalFields(totalFields);
                plantDataDto.setCompletedFields(completedFields);
            }
            
            plantQuestionnaireService.savePlantSpecificData(plantDataDto, submittedBy);
            
            // Submit the questionnaire
            plantQuestionnaireService.submitPlantQuestionnaire(plantCode, materialCode, submittedBy);
            
            return ResponseEntity.ok("Questionnaire submitted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to submit questionnaire: " + e.getMessage());
        }
    }

    /**
     * Get plant questionnaire statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPlantQuestionnaireStats(
            @RequestParam String plantCode,
            @RequestParam(required = false) String materialCode) {
        
        try {
            // This would be implemented based on your statistics requirements
            // For now, return a simple response
            Map<String, Object> stats = new HashMap<>();
            stats.put("message", "Statistics endpoint - implementation pending");
            stats.put("plantCode", plantCode);
            stats.put("materialCode", materialCode != null ? materialCode : "all");
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
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
            
            // Get questionnaire template
            QuestionnaireTemplateDto template = plantQuestionnaireService.getQuestionnaireTemplate(
                materialCode, plantCode, "PLANT_QUESTIONNAIRE");
            response.put("template", template);
            
            // Get CQS data
            CqsDataDto cqsData = plantQuestionnaireService.getCqsData(materialCode, plantCode);
            response.put("cqsData", cqsData);
            
            // Get or create plant-specific data
            PlantSpecificDataDto plantData = plantQuestionnaireService.getOrCreatePlantSpecificData(
                plantCode, materialCode, workflowId);
            response.put("plantData", plantData);
            
            // Add workflow information
            Map<String, Object> workflowInfo = new HashMap<>();
            workflowInfo.put("id", workflow.getId());
            workflowInfo.put("materialCode", materialCode);
            workflowInfo.put("plantCode", plantCode);
            workflowInfo.put("state", workflow.getState());
            workflowInfo.put("materialName", workflow.getMaterialName());
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
}

