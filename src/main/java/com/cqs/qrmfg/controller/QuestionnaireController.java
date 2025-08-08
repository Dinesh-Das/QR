package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.dto.*;
import com.cqs.qrmfg.service.PlantQuestionnaireService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

/**
 * Simplified controller for direct questionnaire access
 */
@RestController
@RequestMapping("/api/v1/questionnaire")
public class QuestionnaireController {
    
    @Autowired
    private PlantQuestionnaireService plantQuestionnaireService;
    
    @Autowired
    private com.cqs.qrmfg.service.WorkflowService workflowService;
    
    /**
     * Test endpoint to verify controller is working
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("QuestionnaireController is working!");
    }
    
    /**
     * Simple hello endpoint with no dependencies
     */
    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Hello from QuestionnaireController!");
    }
    
    /**
     * Serve the questionnaire viewer HTML page
     */
    @GetMapping(value = "/{workflowId}/view", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getQuestionnaireViewerPage(@PathVariable Long workflowId) {
        try {
            ClassPathResource resource = new ClassPathResource("static/questionnaire-viewer.html");
            String html = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            
            // Replace placeholder with actual workflow ID
            html = html.replace("window.location.pathname.split('/').pop()", "'" + workflowId + "'");
            
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
        } catch (IOException e) {
            return ResponseEntity.status(500)
                .body("<html><body><h1>Error loading questionnaire viewer</h1><p>" + e.getMessage() + "</p></body></html>");
        }
    }
    
    /**
     * Get complete questionnaire data by workflow ID (simplified URL)
     * Returns filled form data in read-only mode for viewing
     */
    @GetMapping("/{workflowId}")
    public ResponseEntity<Map<String, Object>> getQuestionnaire(@PathVariable Long workflowId) {
        
        try {
            // Get current user authentication
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String currentUser = auth != null ? auth.getName() : "anonymous";
            
            // Get workflow details
            Optional<com.cqs.qrmfg.model.Workflow> workflowOpt = workflowService.findById(workflowId);
            if (!workflowOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            com.cqs.qrmfg.model.Workflow workflow = workflowOpt.get();
            String materialCode = workflow.getMaterialCode();
            String plantCode = workflow.getPlantCode();
            
            Map<String, Object> response = new HashMap<>();
            
            // Get questionnaire template (form structure)
            QuestionnaireTemplateDto template = plantQuestionnaireService.getQuestionnaireTemplate(
                materialCode, plantCode, "PLANT_QUESTIONNAIRE");
            response.put("template", template);
            
            // Get CQS auto-populated data (read-only)
            CqsDataDto cqsData = plantQuestionnaireService.getCqsData(materialCode, plantCode);
            response.put("cqsData", cqsData);
            
            // Get plant-specific data (this contains the actual filled form data)
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
            
            // Add access control information
            Map<String, Object> accessControl = new HashMap<>();
            accessControl.put("currentUser", currentUser);
            accessControl.put("isReadOnly", true); // Default to read-only for viewing
            accessControl.put("canEdit", false);   // Only plant users should be able to edit
            accessControl.put("viewMode", "QRMFG_VIEW"); // Indicates this is for QRMFG viewing
            response.put("accessControl", accessControl);
            
            // Add form completion status
            Map<String, Object> completionStatus = new HashMap<>();
            if (plantData != null) {
                completionStatus.put("totalFields", plantData.getTotalFields());
                completionStatus.put("completedFields", plantData.getCompletedFields());
                completionStatus.put("isComplete", plantData.getCompletedFields() >= plantData.getTotalFields());
                completionStatus.put("lastModified", plantData.getUpdatedAt());
                completionStatus.put("submittedBy", plantData.getCreatedBy());
            } else {
                completionStatus.put("totalFields", 0);
                completionStatus.put("completedFields", 0);
                completionStatus.put("isComplete", false);
                completionStatus.put("lastModified", null);
                completionStatus.put("submittedBy", null);
            }
            response.put("completionStatus", completionStatus);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to load questionnaire data");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Get questionnaire data for editing (plant users only)
     */
    @GetMapping("/{workflowId}/edit")
    public ResponseEntity<Map<String, Object>> getQuestionnaireForEdit(@PathVariable Long workflowId) {
        
        try {
            // Get current user authentication
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String currentUser = auth != null ? auth.getName() : "anonymous";
            
            // TODO: Add role-based access control here
            // For now, we'll allow editing but mark it clearly
            
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
            
            // Get plant-specific data
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
            
            // Add access control information for editing
            Map<String, Object> accessControl = new HashMap<>();
            accessControl.put("currentUser", currentUser);
            accessControl.put("isReadOnly", false); // Allow editing
            accessControl.put("canEdit", true);     // Plant users can edit
            accessControl.put("viewMode", "PLANT_EDIT"); // Indicates this is for plant editing
            response.put("accessControl", accessControl);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to load questionnaire data for editing");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}