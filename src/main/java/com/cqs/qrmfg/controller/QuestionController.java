package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.model.Question;
import com.cqs.qrmfg.model.QuestionTemplate;
import com.cqs.qrmfg.model.Answer;
import com.cqs.qrmfg.service.MaterialQuestionnaireService;
import com.cqs.qrmfg.service.PlantQuestionnaireService;
import com.cqs.qrmfg.service.QuestionnaireInitializationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questionnaire")
public class QuestionController {

    @Autowired
    private MaterialQuestionnaireService materialQuestionnaireService;

    @Autowired
    private PlantQuestionnaireService plantService;

    @Autowired
    private QuestionnaireInitializationService initService;



    @PostMapping("/create-material/{materialCode}")
    public ResponseEntity<String> createMaterialQuestionnaire(@PathVariable String materialCode,
                                                             @RequestParam(required = false) Integer version) {
        try {
            if (version != null) {
                initService.initializeMaterialQuestionnaireFromVersion(materialCode, version);
            } else {
                initService.initializeMaterialQuestionnaire(materialCode);
            }
            return ResponseEntity.ok("Material questionnaire created successfully for: " + materialCode);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating material questionnaire: " + e.getMessage());
        }
    }

    @GetMapping("/template/questions")
    public ResponseEntity<List<QuestionTemplate>> getTemplateQuestions() {
        List<QuestionTemplate> templates = materialQuestionnaireService.getTemplateQuestions();
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/template/categories")
    public ResponseEntity<List<String>> getTemplateCategories() {
        List<String> categories = materialQuestionnaireService.getTemplateCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/template/statistics")
    public ResponseEntity<MaterialQuestionnaireService.TemplateStatistics> getTemplateStatistics() {
        MaterialQuestionnaireService.TemplateStatistics stats = initService.getTemplateStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/sections/{materialCode}")
    public ResponseEntity<List<String>> getAvailableSections(@PathVariable String materialCode) {
        List<String> sections = plantService.getAvailableSections(materialCode);
        return ResponseEntity.ok(sections);
    }

    @GetMapping("/cqs-questions/{materialCode}")
    public ResponseEntity<List<Question>> getCQSQuestions(@PathVariable String materialCode) {
        List<Question> questions = plantService.getCQSQuestions(materialCode);
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/plant-questions/{materialCode}")
    public ResponseEntity<List<Question>> getPlantQuestions(@PathVariable String materialCode) {
        List<Question> questions = plantService.getPlantQuestions(materialCode);
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/plant-responses/{workflowId}")
    public ResponseEntity<List<Answer>> getPlantResponses(
            @PathVariable Long workflowId,
            @RequestParam String plantCode) {
        List<Answer> responses = plantService.getPlantResponses(workflowId, plantCode);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/plant-responses/{workflowId}/grouped")
    public ResponseEntity<Map<String, List<Answer>>> getPlantResponsesGrouped(
            @PathVariable Long workflowId,
            @RequestParam String plantCode) {
        Map<String, List<Answer>> groupedResponses = 
            plantService.getPlantResponsesGroupedBySection(workflowId, plantCode);
        return ResponseEntity.ok(groupedResponses);
    }

    @GetMapping("/plant-responses/{workflowId}/section/{sectionName}")
    public ResponseEntity<List<Answer>> getPlantResponsesBySection(
            @PathVariable Long workflowId,
            @PathVariable String sectionName,
            @RequestParam String plantCode) {
        List<Answer> responses = 
            plantService.getPlantResponsesBySection(workflowId, plantCode, sectionName);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/plant-response/{responseId}")
    public ResponseEntity<String> savePlantResponse(
            @PathVariable Long responseId,
            @RequestBody Map<String, String> request) {
        try {
            String value = request.get("value");
            String modifiedBy = request.get("modifiedBy");
            plantService.savePlantResponse(responseId, value, modifiedBy);
            return ResponseEntity.ok("Response saved successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error saving response: " + e.getMessage());
        }
    }

    @PutMapping("/plant-response/{responseId}/draft")
    public ResponseEntity<String> savePlantResponseDraft(
            @PathVariable Long responseId,
            @RequestBody Map<String, String> request) {
        try {
            String value = request.get("value");
            String modifiedBy = request.get("modifiedBy");
            plantService.savePlantResponseDraft(responseId, value, modifiedBy);
            return ResponseEntity.ok("Draft saved successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error saving draft: " + e.getMessage());
        }
    }

    @PostMapping("/plant-responses/{workflowId}/submit")
    public ResponseEntity<String> submitPlantResponses(
            @PathVariable Long workflowId,
            @RequestParam String plantCode) {
        try {
            plantService.validateAndSubmitPlantResponses(workflowId, plantCode);
            return ResponseEntity.ok("Plant responses submitted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error submitting responses: " + e.getMessage());
        }
    }

}