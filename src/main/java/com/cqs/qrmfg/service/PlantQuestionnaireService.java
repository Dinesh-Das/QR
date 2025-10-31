package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.*;
import com.cqs.qrmfg.repository.*;
import com.cqs.qrmfg.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Objects;

@Service
public class PlantQuestionnaireService {

    /**
     * Known CQS auto-populated fields that should be marked as completed
     */
    private static final Set<String> KNOWN_CQS_FIELDS = new HashSet<String>() {{
        // Physical properties
        add("is_corrosive");
        add("highly_toxic");
        add("is_explosive");
        add("autoignition_temp");
        add("silica_content");
        add("dust_explosion");
        add("electrostatic_charge");
        add("compatibility_class");
        add("sap_compatibility");
        
        // Flammability
        add("flash_point_65");
        add("flash_point_21");
        add("petroleum_class");
        
        // Toxicity
        add("ld50_oral");
        add("ld50_dermal");
        add("lc50_inhalation");
        add("carcinogenic");
        add("mutagenic");
        add("endocrine_disruptor");
        add("reproductive_toxicant");
        add("env_toxic");
        add("narcotic_listed");
        add("hhrm_category");
        add("tlv_stel_values");
        
        // Process Safety Management
        add("psm_tier1_outdoor");
        add("psm_tier1_indoor");
        add("psm_tier2_outdoor");
        add("psm_tier2_indoor");
        
        // First Aid
        add("is_poisonous");
        add("antidote_specified");
        
        // PPE
        add("recommended_ppe");
        
        // Statutory
        add("cmvr_listed");
        add("msihc_listed");
        add("factories_act_listed");
        
        // Additional fields from logs
        add("swarf_analysis");
        add("spill_measures_provided");
    }};

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;
    
    @Autowired
    private QuestionTemplateRepository questionTemplateRepository;
    
    @Autowired
    private PlantSpecificDataRepository plantSpecificDataRepository;
    
    @Autowired
    private WorkflowRepository workflowRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private CqsIntegrationService cqsIntegrationService;

    @Transactional
    public void initializePlantQuestionnaire(Workflow workflow, String plantCode) {
        String materialCode = workflow.getMaterialCode();
        
        // Get all questions for this material
        List<Question> masterQuestions = questionRepository.findByMaterialCodeAndIsActiveTrueOrderByOrderIndexAsc(materialCode);
        
        // Create response entries for plant-specific questions
        List<Answer> responses = masterQuestions.stream()
            .filter(q -> shouldCreateResponseForPlant(q))
            .map(q -> createPlantResponse(workflow, q, plantCode, materialCode))
            .collect(Collectors.toList());
        
        answerRepository.saveAll(responses);
    }

    private boolean shouldCreateResponseForPlant(Question question) {
        String responsible = question.getResponsible();
        return "Plant".equalsIgnoreCase(responsible) || 
               "All Plants".equalsIgnoreCase(responsible) ||
               "Plant to fill data".equalsIgnoreCase(responsible);
    }

    private Answer createPlantResponse(Workflow workflow, 
                                     Question masterQuestion,
                                     String plantCode, String materialCode) {
        Answer response = new Answer();
        
        response.setWorkflow(workflow);
        response.setStepNumber(masterQuestion.getStepNumber());
        response.setFieldName(masterQuestion.getFieldName());
        response.setFieldType(masterQuestion.getQuestionType());
        response.setSectionName(masterQuestion.getCategory());
        response.setDisplayOrder(masterQuestion.getOrderIndex());
        response.setIsRequired(masterQuestion.getIsRequired());
        response.setPlantCode(plantCode);
        response.setMaterialCode(materialCode);
        response.setCreatedAt(LocalDateTime.now());
        response.setLastModified(LocalDateTime.now());
        response.setCreatedBy("SYSTEM");
        response.setModifiedBy("SYSTEM");
        response.setIsDraft(true);
        
        return response;
    }

    public List<Answer> getPlantResponses(Long workflowId, String plantCode) {
        return answerRepository.findByPlantCodeAndMaterialCode(plantCode, "");
    }

    public List<Answer> getPlantResponsesBySection(Long workflowId, String plantCode, 
                                                 String sectionName) {
        // Note: This method needs to be updated based on the new AnswerRepository methods
        return answerRepository.findByPlantCodeAndMaterialCode(plantCode, "");
    }

    @Transactional
    public void savePlantResponse(Long responseId, String value, String modifiedBy) {
        Answer response = answerRepository.findById(responseId)
            .orElseThrow(() -> new RuntimeException("Response not found: " + responseId));
        
        response.updateValue(value, modifiedBy);
        answerRepository.save(response);
    }

    @Transactional
    public void savePlantResponseDraft(Long responseId, String value, String modifiedBy) {
        Answer response = answerRepository.findById(responseId)
            .orElseThrow(() -> new RuntimeException("Response not found: " + responseId));
        
        response.saveDraft(value, modifiedBy);
        answerRepository.save(response);
    }

    public Map<String, List<Answer>> getPlantResponsesGroupedBySection(Long workflowId, 
                                                                      String plantCode) {
        List<Answer> responses = getPlantResponses(workflowId, plantCode);
        return responses.stream()
            .collect(Collectors.groupingBy(Answer::getSectionName));
    }

    public List<String> getAvailableSections(String materialCode) {
        return questionRepository.findDistinctCategoriesByMaterialCode(materialCode);
    }

    public List<Question> getCQSQuestions(String materialCode) {
        return questionRepository.findByMaterialCodeAndResponsible(materialCode, "CQS");
    }

    public List<Question> getPlantQuestions(String materialCode) {
        return questionRepository.findByMaterialCodeAndResponsible(materialCode, "Plant");
    }

    @Transactional
    public void validateAndSubmitPlantResponses(Long workflowId, String plantCode) {
        List<Answer> responses = getPlantResponses(workflowId, plantCode);
        
        for (Answer response : responses) {
            if (response.isRequiredAndEmpty()) {
                response.markInvalid("This field is required");
            } else {
                response.markValid();
            }
            response.setIsDraft(false);
        }
        
        answerRepository.saveAll(responses);
    }
    
    /**
     * Get questionnaire template from backend template table
     */
    public QuestionnaireTemplateDto getQuestionnaireTemplate(String materialCode, String plantCode, String templateType) {
        try {
            System.out.println("PlantQuestionnaireService: Loading template for material=" + materialCode + ", plant=" + plantCode);
            
            // Get CQS data for auto-population
            CqsDataDto cqsData = null;
            try {
                cqsData = getCqsData(materialCode, plantCode);
                System.out.println("PlantQuestionnaireService: CQS data loaded successfully");
            } catch (Exception e) {
                System.err.println("PlantQuestionnaireService: Failed to load CQS data: " + e.getMessage());
            }
            
            // Get all active template questions ordered by step and order index
            List<QuestionTemplate> templates = questionTemplateRepository.findByIsActiveTrueOrderByStepNumberAscOrderIndexAsc();
            
            System.out.println("PlantQuestionnaireService: Found " + templates.size() + " template questions");
            
            if (templates.isEmpty()) {
                System.err.println("PlantQuestionnaireService: No questionnaire template found in database");
                throw new RuntimeException("No questionnaire template found");
            }
            
            // Group templates by step number
            Map<Integer, List<QuestionTemplate>> stepGroups = templates.stream()
                .collect(Collectors.groupingBy(QuestionTemplate::getStepNumber));
            
            System.out.println("PlantQuestionnaireService: Step groups found: " + stepGroups.keySet());
            
            // Build template DTO
            QuestionnaireTemplateDto templateDto = new QuestionnaireTemplateDto();
            List<QuestionnaireStepDto> steps = new ArrayList<>();
            
            // Sort step numbers to ensure proper order
            List<Integer> sortedStepNumbers = stepGroups.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
            
            final CqsDataDto finalCqsData = cqsData; // Make it effectively final for lambda
            
            for (Integer stepNumber : sortedStepNumbers) {
                List<QuestionTemplate> stepTemplates = stepGroups.get(stepNumber);
                
                System.out.println("PlantQuestionnaireService: Processing step " + stepNumber + " with " + stepTemplates.size() + " templates");
                
                QuestionnaireStepDto stepDto = new QuestionnaireStepDto();
                stepDto.setStepNumber(stepNumber);
                stepDto.setTitle(getStepTitle(stepNumber, stepTemplates));
                stepDto.setDescription(getStepDescription(stepNumber, stepTemplates));
                
                // Sort templates within step by order index and filter out display-only questions
                List<QuestionTemplate> sortedTemplates = stepTemplates.stream()
                    .filter(template -> {
                        // Exclude display-only questions and questions with no responsible party
                        return !"DISPLAY".equalsIgnoreCase(template.getQuestionType()) && 
                               !"NONE".equalsIgnoreCase(template.getResponsible());
                    })
                    .sorted((t1, t2) -> {
                        Integer order1 = t1.getOrderIndex() != null ? t1.getOrderIndex() : t1.getSrNo();
                        Integer order2 = t2.getOrderIndex() != null ? t2.getOrderIndex() : t2.getSrNo();
                        return order1.compareTo(order2);
                    })
                    .collect(Collectors.toList());
                
                List<QuestionnaireFieldDto> fields = sortedTemplates.stream()
                    .map(template -> convertTemplateToField(template, finalCqsData))
                    .collect(Collectors.toList());
                
                stepDto.setFields(fields);
                steps.add(stepDto);
                
                System.out.println("PlantQuestionnaireService: Step " + stepNumber + " (" + stepDto.getTitle() + ") has " + fields.size() + " fields");
            }
            
            templateDto.setSteps(steps);
            templateDto.setMaterialCode(materialCode);
            templateDto.setPlantCode(plantCode);
            templateDto.setTemplateType(templateType);
            templateDto.setVersion(1);
            templateDto.setCreatedAt(LocalDateTime.now());
            
            // Update completion stats based on CQS auto-filled data
            updateCompletionStatsForTemplate(materialCode, plantCode, steps, finalCqsData);
            
            return templateDto;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load questionnaire template: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get CQS auto-populated data from QRMFG_AUTO_CQS table
     */
    public CqsDataDto getCqsData(String materialCode, String plantCode) {
        try {
            return cqsIntegrationService.getCqsData(materialCode, plantCode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load CQS data: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get or create plant-specific data record
     */
    @Transactional
    public PlantSpecificDataDto getOrCreatePlantSpecificData(String plantCode, String materialCode, Long workflowId) {
        try {
            PlantSpecificDataId id = new PlantSpecificDataId(plantCode, materialCode);
            Optional<PlantSpecificData> existing = plantSpecificDataRepository.findById(id);
            
            PlantSpecificData plantData;
            if (existing.isPresent()) {
                plantData = existing.get();
                
                // Check if CQS data needs to be synced
                if (plantData.getCqsInputs() == null || plantData.getCqsInputs().trim().isEmpty() || "{}".equals(plantData.getCqsInputs().trim())) {
                    System.out.println("PlantQuestionnaireService: CQS inputs empty for " + plantCode + "/" + materialCode + ", attempting sync");
                    syncCqsDataForPlantRecord(plantData, "SYSTEM");
                }
            } else {
                // Create new plant-specific data record
                plantData = new PlantSpecificData(plantCode, materialCode);
                plantData.setWorkflowId(workflowId);
                plantData.setCreatedBy("SYSTEM");
                plantData.setUpdatedBy("SYSTEM");
                
                // Initialize with empty CQS and plant data
                plantData.setCqsInputs("{}");
                plantData.setPlantInputs("{}");
                plantData.setCombinedData("{}");
                
                plantData = plantSpecificDataRepository.save(plantData);
                
                // Sync CQS data for new record
                System.out.println("PlantQuestionnaireService: New plant record created for " + plantCode + "/" + materialCode + ", syncing CQS data");
                syncCqsDataForPlantRecord(plantData, "SYSTEM");
            }
            
            return convertToDto(plantData);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get/create plant-specific data: " + e.getMessage(), e);
        }
    }
    
    /**
     * Save plant-specific data with composite key
     */
    @Transactional
    public void savePlantSpecificData(PlantSpecificDataDto dataDto, String modifiedBy) {
        try {
            System.out.println("PlantQuestionnaireService: Saving plant-specific data for " + 
                             dataDto.getPlantCode() + "/" + dataDto.getMaterialCode());
            
            PlantSpecificDataId id = new PlantSpecificDataId(
                dataDto.getPlantCode(), 
                dataDto.getMaterialCode()
            );
            
            PlantSpecificData plantData = plantSpecificDataRepository.findById(id)
                .orElse(new PlantSpecificData(dataDto.getPlantCode(), dataDto.getMaterialCode()));
            
            boolean dataUpdated = false;
            
            // Update CQS inputs if provided
            if (dataDto.getCqsInputs() != null) {
                String cqsJson = objectMapper.writeValueAsString(dataDto.getCqsInputs());
                plantData.updateCqsData(cqsJson, modifiedBy);
                dataUpdated = true;
                System.out.println("PlantQuestionnaireService: Updated CQS inputs");
            }
            
            // Update plant inputs if provided
            if (dataDto.getPlantInputs() != null && !dataDto.getPlantInputs().isEmpty()) {
                String plantJson = objectMapper.writeValueAsString(dataDto.getPlantInputs());
                plantData.updatePlantData(plantJson, modifiedBy);
                dataUpdated = true;
                System.out.println("PlantQuestionnaireService: Updated plant inputs with " + 
                                 dataDto.getPlantInputs().size() + " fields");
                
                // Log some sample data for debugging
                dataDto.getPlantInputs().entrySet().stream()
                    .limit(3)
                    .forEach(entry -> System.out.println("  " + entry.getKey() + " = " + entry.getValue()));
            } else {
                System.out.println("PlantQuestionnaireService: No plant inputs to update");
            }
            
            // Update completion statistics
            if (dataDto.getTotalFields() != null) {
                plantData.updateCompletionStats(
                    dataDto.getTotalFields(),
                    dataDto.getCompletedFields() != null ? dataDto.getCompletedFields() : 0,
                    dataDto.getRequiredFields() != null ? dataDto.getRequiredFields() : 0,
                    dataDto.getCompletedRequiredFields() != null ? dataDto.getCompletedRequiredFields() : 0
                );
                dataUpdated = true;
                System.out.println("PlantQuestionnaireService: Updated completion stats - " + 
                                 dataDto.getCompletedFields() + "/" + dataDto.getTotalFields() + " fields completed");
            }
            
            if (dataUpdated) {
                plantData.setWorkflowId(dataDto.getWorkflowId());
                plantData.setUpdatedBy(modifiedBy);
                
                PlantSpecificData savedData = plantSpecificDataRepository.save(plantData);
                System.out.println("PlantQuestionnaireService: Successfully saved plant-specific data. " +
                                 "Plant inputs length: " + (savedData.getPlantInputs() != null ? savedData.getPlantInputs().length() : 0));
            } else {
                System.out.println("PlantQuestionnaireService: No data to update");
            }
            
        } catch (Exception e) {
            System.err.println("PlantQuestionnaireService: Failed to save plant-specific data: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save plant-specific data: " + e.getMessage(), e);
        }
    }
    
    /**
     * Submit plant questionnaire with validation
     */
    @Transactional
    public Map<String, Object> submitPlantQuestionnaire(String plantCode, String materialCode, String submittedBy) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            PlantSpecificDataId id = new PlantSpecificDataId(plantCode, materialCode);
            PlantSpecificData plantData = plantSpecificDataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plant-specific data not found"));
            
            // CRITICAL FIX: Check if already submitted to prevent duplicate submissions
            if (plantData.isSubmitted()) {
                result.put("success", false);
                result.put("message", "Questionnaire has already been submitted");
                result.put("submittedAt", plantData.getSubmittedAt());
                result.put("submittedBy", plantData.getSubmittedBy());
                result.put("completionPercentage", plantData.getCompletionPercentage());
                result.put("isAlreadySubmitted", true);
                return result;
            }
            
            // Validate questionnaire completion before submission
            ValidationResult validation = validateQuestionnaireCompletion(plantCode, materialCode);
            
            if (!validation.isValid()) {
                result.put("success", false);
                result.put("message", "Cannot submit questionnaire: " + validation.getMessage());
                result.put("missingFields", validation.getMissingFields());
                result.put("completionPercentage", validation.getCompletionPercentage());
                return result;
            }
            
            // CRITICAL FIX: Preserve completion stats before submission
            Integer currentTotalFields = plantData.getTotalFields();
            Integer currentCompletedFields = plantData.getCompletedFields();
            Integer currentRequiredFields = plantData.getRequiredFields();
            Integer currentCompletedRequiredFields = plantData.getCompletedRequiredFields();
            Integer currentCompletionPercentage = plantData.getCompletionPercentage();
            
            System.out.println("PlantQuestionnaireService: BEFORE SUBMISSION - Completion: " + 
                             currentCompletedFields + "/" + currentTotalFields + " (" + currentCompletionPercentage + "%)");
            
            // Submit the plant data (this sets status to SUBMITTED and submittedAt timestamp)
            plantData.submit(submittedBy);
            
            // CRITICAL FIX: Ensure completion stats are preserved after submission
            if (currentTotalFields != null && currentCompletedFields != null) {
                plantData.setTotalFields(currentTotalFields);
                plantData.setCompletedFields(currentCompletedFields);
                plantData.setRequiredFields(currentRequiredFields);
                plantData.setCompletedRequiredFields(currentCompletedRequiredFields);
                plantData.setCompletionPercentage(currentCompletionPercentage);
            }
            
            // CRITICAL FIX: Force completion status to COMPLETED and 100% for submitted questionnaires
            plantData.setCompletionStatus("COMPLETED");
            plantData.setCompletionPercentage(100); // Force 100% completion for submitted questionnaires
            
            // Save with preserved stats
            PlantSpecificData savedData = plantSpecificDataRepository.save(plantData);
            
            System.out.println("PlantQuestionnaireService: AFTER SUBMISSION - Status: " + savedData.getCompletionStatus() + 
                             ", Completion: " + savedData.getCompletedFields() + "/" + savedData.getTotalFields() + 
                             " (" + savedData.getCompletionPercentage() + "%), Submitted: " + savedData.getSubmittedAt());
            
            // Update workflow status to COMPLETED
            updateWorkflowStatusOnSubmission(plantCode, materialCode, submittedBy);
            
            result.put("success", true);
            result.put("message", "Questionnaire submitted successfully");
            result.put("submittedAt", savedData.getSubmittedAt());
            result.put("submittedBy", savedData.getSubmittedBy());
            result.put("completionPercentage", savedData.getCompletionPercentage());
            result.put("completionStatus", savedData.getCompletionStatus());
            result.put("totalFields", savedData.getTotalFields());
            result.put("completedFields", savedData.getCompletedFields());
            result.put("isReadOnly", true);
            
            return result;
            
        } catch (Exception e) {
            System.err.println("PlantQuestionnaireService: Submission failed: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "Failed to submit plant questionnaire: " + e.getMessage());
            return errorResult;
        }
    }
    
    /**
     * Validate questionnaire completion before submission
     */
    @Transactional
    public ValidationResult validateQuestionnaireCompletion(String plantCode, String materialCode) {
        try {
            System.out.println("PlantQuestionnaireService: Validating completion for " + plantCode + "/" + materialCode);
            
            // CRITICAL FIX: First recalculate completion stats to ensure accuracy
            recalculateCompletionStats(materialCode, plantCode);
            
            // Get plant-specific data with updated stats
            PlantSpecificDataDto plantData = getPlantSpecificData(plantCode, materialCode);
            if (plantData == null) {
                return new ValidationResult(false, "Plant-specific data not found", new ArrayList<>(), 0);
            }
            
            // Use the recalculated completion stats instead of doing our own calculation
            int totalFields = plantData.getTotalFields() != null ? plantData.getTotalFields() : 0;
            int completedFields = plantData.getCompletedFields() != null ? plantData.getCompletedFields() : 0;
            int requiredFields = plantData.getRequiredFields() != null ? plantData.getRequiredFields() : 0;
            int completedRequiredFields = plantData.getCompletedRequiredFields() != null ? plantData.getCompletedRequiredFields() : 0;
            int completionPercentage = plantData.getCompletionPercentage() != null ? plantData.getCompletionPercentage() : 0;
            
            System.out.println("PlantQuestionnaireService: Using recalculated stats - " +
                             "Total: " + totalFields + ", Completed: " + completedFields + 
                             ", Required: " + requiredFields + ", Completed Required: " + completedRequiredFields +
                             ", Percentage: " + completionPercentage + "%");
            
            // Check for missing required fields only (completion stats are already calculated)
            List<String> missingRequiredFields = new ArrayList<>();
            
            // Only check required fields that are not completed
            if (completedRequiredFields < requiredFields) {
                // Get the questionnaire template to identify missing required fields
                QuestionnaireTemplateDto template = getQuestionnaireTemplate(materialCode, plantCode, "PLANT_QUESTIONNAIRE");
                Map<String, Object> plantInputs = plantData.getPlantInputs() != null ? plantData.getPlantInputs() : new HashMap<>();
                
                // Get CQS data for checking CQS fields
                CqsDataDto cqsData = null;
                try {
                    cqsData = getCqsData(materialCode, plantCode);
                } catch (Exception e) {
                    System.err.println("PlantQuestionnaireService: Failed to load CQS data for validation: " + e.getMessage());
                }
                
                // Only check required fields to identify which ones are missing
                for (QuestionnaireStepDto step : template.getSteps()) {
                    for (QuestionnaireFieldDto field : step.getFields()) {
                        if (field.isRequired()) {
                            boolean isCompleted = false;
                            
                            if (field.isCqsAutoPopulated()) {
                                // CQS field - CQS auto-populated fields are considered completed by default
                                isCompleted = true;
                                
                                // Only mark as incomplete if we have explicit "Data not available"
                                String cqsValue = getCqsValueForField(field.getName(), cqsData);
                                if (cqsValue != null && ("Data not available".equals(cqsValue) || 
                                                       "Not available".equalsIgnoreCase(cqsValue) ||
                                                       "N/A".equalsIgnoreCase(cqsValue))) {
                                    isCompleted = false;
                                }
                                
                                // Special handling for Process Safety fields - always completed
                                if ("Process Safety Management".equalsIgnoreCase(step.getTitle()) || 
                                    step.getTitle().toLowerCase().contains("process safety")) {
                                    isCompleted = true; // Process Safety fields are always auto-completed
                                }
                            } else {
                                // Plant field - check if user has provided a value
                                Object value = findFieldValue(plantInputs, field.getName(), field.getOrderIndex(), step.getStepNumber());
                                
                                if (value != null) {
                                    if (value instanceof String) {
                                        String strValue = ((String) value).trim();
                                        isCompleted = !strValue.isEmpty() && 
                                                     !"null".equalsIgnoreCase(strValue) && 
                                                     !"undefined".equalsIgnoreCase(strValue);
                                    } else {
                                        isCompleted = true; // Non-string values are considered complete
                                    }
                                }
                            }
                            
                            if (!isCompleted) {
                                missingRequiredFields.add(field.getName() + " (" + field.getLabel() + ")");
                            }
                        }
                    }
                }
            }
            
            // Validation rules:
            // 1. All required fields must be completed
            // 2. At least 80% of total fields should be completed for submission (reduced from 90% due to CQS field issues)
            boolean isValid = missingRequiredFields.isEmpty() && completionPercentage >= 80;
            
            String message;
            if (!missingRequiredFields.isEmpty()) {
                message = "Missing required fields: " + missingRequiredFields.size() + " field(s)";
            } else if (completionPercentage < 80) {
                message = "Questionnaire is only " + completionPercentage + "% complete. At least 80% completion required for submission. " +
                         "Total fields: " + completedFields + "/" + totalFields;
            } else {
                message = "Questionnaire is ready for submission";
            }
            
            return new ValidationResult(isValid, message, missingRequiredFields, completionPercentage);
            
        } catch (Exception e) {
            System.err.println("PlantQuestionnaireService: Validation failed: " + e.getMessage());
            e.printStackTrace();
            return new ValidationResult(false, "Validation failed: " + e.getMessage(), new ArrayList<>(), 0);
        }
    }
    
    /**
     * Update workflow status when questionnaire is submitted
     */
    @Transactional
    private void updateWorkflowStatusOnSubmission(String plantCode, String materialCode, String submittedBy) {
        try {
            System.out.println("PlantQuestionnaireService: Attempting to update workflow status for plant=" + 
                             plantCode + ", material=" + materialCode);
            
            // Try multiple approaches to find the workflow
            Workflow targetWorkflow = null;
            
            // Approach 1: Direct lookup by plant and material
            List<Workflow> directMatches = workflowRepository.findByPlantCodeAndMaterialCode(plantCode, materialCode);
            if (!directMatches.isEmpty()) {
                targetWorkflow = directMatches.get(0);
                System.out.println("PlantQuestionnaireService: Found workflow via direct lookup: " + targetWorkflow.getId());
            }
            
            // Approach 2: If not found, try by plant code with queries
            if (targetWorkflow == null) {
                List<Workflow> workflows = workflowRepository.findByPlantCodeWithQueries(plantCode);
                System.out.println("PlantQuestionnaireService: Found " + workflows.size() + " workflows for plant " + plantCode);
                
                targetWorkflow = workflows.stream()
                    .filter(w -> materialCode.equals(w.getMaterialCode()))
                    .findFirst()
                    .orElse(null);
                
                if (targetWorkflow != null) {
                    System.out.println("PlantQuestionnaireService: Found workflow via plant query: " + targetWorkflow.getId());
                }
            }
            
            // Approach 3: If still not found, try by material code
            if (targetWorkflow == null) {
                List<Workflow> materialWorkflows = workflowRepository.findByMaterialCode(materialCode);
                targetWorkflow = materialWorkflows.stream()
                    .filter(w -> plantCode.equals(w.getPlantCode()))
                    .findFirst()
                    .orElse(null);
                
                if (targetWorkflow != null) {
                    System.out.println("PlantQuestionnaireService: Found workflow via material query: " + targetWorkflow.getId());
                }
            }
            
            if (targetWorkflow != null) {
                System.out.println("PlantQuestionnaireService: Current workflow state: " + targetWorkflow.getState() + 
                                 ", attempting transition to COMPLETED");
                
                // Check if transition is allowed
                if (targetWorkflow.canTransitionTo(WorkflowState.COMPLETED)) {
                    // Update workflow state to COMPLETED
                    targetWorkflow.transitionTo(WorkflowState.COMPLETED, submittedBy);
                    Workflow savedWorkflow = workflowRepository.save(targetWorkflow);
                    
                    System.out.println("PlantQuestionnaireService: Successfully updated workflow " + savedWorkflow.getId() + 
                                     " status to " + savedWorkflow.getState() + " for material " + materialCode + 
                                     " at plant " + plantCode);
                } else {
                    System.err.println("PlantQuestionnaireService: Cannot transition from " + targetWorkflow.getState() + 
                                     " to COMPLETED for workflow " + targetWorkflow.getId());
                }
            } else {
                System.err.println("PlantQuestionnaireService: No workflow found for material " + materialCode + 
                           " at plant " + plantCode + " using any lookup method");
                
                // Debug: List all workflows for this plant
                List<Workflow> allPlantWorkflows = workflowRepository.findByPlantCodeWithQueries(plantCode);
                System.err.println("PlantQuestionnaireService: Available workflows for plant " + plantCode + ":");
                for (Workflow w : allPlantWorkflows) {
                    System.err.println("  - Workflow " + w.getId() + ": material=" + w.getMaterialCode() + 
                                     ", state=" + w.getState());
                }
            }
            
        } catch (Exception e) {
            System.err.println("PlantQuestionnaireService: Failed to update workflow status: " + e.getMessage());
            e.printStackTrace();
            // Don't throw exception to avoid breaking the submission process
        }
    }
    
    /**
     * Check if questionnaire is submitted and read-only
     */
    @Transactional(readOnly = true)
    public boolean isQuestionnaireReadOnly(String plantCode, String materialCode) {
        try {
            PlantSpecificDataDto plantData = getPlantSpecificData(plantCode, materialCode);
            
            // CRITICAL FIX: Primary check is submission timestamp
            boolean isSubmitted = plantData != null && plantData.getSubmittedAt() != null;
            
            if (isSubmitted) {
                System.out.println("PlantQuestionnaireService: Questionnaire is read-only - submitted at: " + plantData.getSubmittedAt());
                return true;
            }
            
            // Secondary check: workflow completion status
            List<Workflow> workflows = workflowRepository.findByPlantCodeWithQueries(plantCode);
            Workflow targetWorkflow = workflows.stream()
                .filter(w -> materialCode.equals(w.getMaterialCode()))
                .findFirst()
                .orElse(null);
            
            if (targetWorkflow != null && targetWorkflow.getState() == WorkflowState.COMPLETED) {
                System.out.println("PlantQuestionnaireService: Questionnaire is read-only - workflow completed");
                return true;
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("PlantQuestionnaireService: Error checking read-only status: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get questionnaire submission status
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getQuestionnaireStatus(String plantCode, String materialCode) {
        try {
            Map<String, Object> status = new HashMap<>();
            
            PlantSpecificDataDto plantData = getPlantSpecificData(plantCode, materialCode);
            if (plantData == null) {
                status.put("exists", false);
                status.put("isSubmitted", false);
                status.put("isReadOnly", false);
                status.put("completionPercentage", 0);
                status.put("completionStatus", "DRAFT");
                return status;
            }
            
            boolean isSubmitted = plantData.getSubmittedAt() != null;
            
            // Check workflow state
            List<Workflow> workflows = workflowRepository.findByPlantCodeWithQueries(plantCode);
            Workflow targetWorkflow = workflows.stream()
                .filter(w -> materialCode.equals(w.getMaterialCode()))
                .findFirst()
                .orElse(null);
            
            boolean isWorkflowCompleted = targetWorkflow != null && targetWorkflow.getState() == WorkflowState.COMPLETED;
            boolean isReadOnly = isSubmitted || isWorkflowCompleted;
            
            // CRITICAL FIX: Don't validate if already submitted (to avoid resetting status)
            ValidationResult validation = null;
            if (!isSubmitted) {
                validation = validateQuestionnaireCompletion(plantCode, materialCode);
            }
            
            status.put("exists", true);
            status.put("isSubmitted", isSubmitted);
            status.put("isWorkflowCompleted", isWorkflowCompleted);
            status.put("isReadOnly", isReadOnly);
            status.put("completionPercentage", plantData.getCompletionPercentage());
            status.put("completionStatus", plantData.getCompletionStatus());
            status.put("submittedAt", plantData.getSubmittedAt());
            status.put("submittedBy", plantData.getSubmittedBy());
            status.put("totalFields", plantData.getTotalFields());
            status.put("completedFields", plantData.getCompletedFields());
            status.put("requiredFields", plantData.getRequiredFields());
            status.put("completedRequiredFields", plantData.getCompletedRequiredFields());
            
            if (validation != null) {
                status.put("canSubmit", validation.isValid() && !isReadOnly);
                status.put("validationMessage", validation.getMessage());
                status.put("missingRequiredFields", validation.getMissingFields().size());
            } else {
                // Already submitted - no need to validate
                status.put("canSubmit", false);
                status.put("validationMessage", "Questionnaire has been submitted");
                status.put("missingRequiredFields", 0);
            }
            
            status.put("workflowState", targetWorkflow != null ? targetWorkflow.getState().name() : "UNKNOWN");
            
            return status;
            
        } catch (Exception e) {
            System.err.println("PlantQuestionnaireService: Error getting questionnaire status: " + e.getMessage());
            Map<String, Object> errorStatus = new HashMap<>();
            errorStatus.put("exists", false);
            errorStatus.put("error", e.getMessage());
            return errorStatus;
        }
    }
    
    /**
     * Manually update workflow status for completed questionnaires (for fixing data inconsistencies)
     */
    @Transactional
    public Map<String, Object> fixWorkflowStatusForCompletedQuestionnaire(String plantCode, String materialCode, String updatedBy) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Check if questionnaire is actually completed
            Map<String, Object> questionnaireStatus = getQuestionnaireStatus(plantCode, materialCode);
            Boolean isSubmitted = (Boolean) questionnaireStatus.get("isSubmitted");
            String completionStatus = (String) questionnaireStatus.get("completionStatus");
            
            if (!Boolean.TRUE.equals(isSubmitted) && !"COMPLETED".equals(completionStatus)) {
                result.put("success", false);
                result.put("message", "Questionnaire is not completed. Cannot update workflow status.");
                return result;
            }
            
            // Force update the workflow status
            updateWorkflowStatusOnSubmission(plantCode, materialCode, updatedBy);
            
            result.put("success", true);
            result.put("message", "Workflow status update attempted. Check logs for details.");
            return result;
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Failed to fix workflow status: " + e.getMessage());
            return result;
        }
    }

    /**
     * Validation result class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final List<String> missingFields;
        private final int completionPercentage;
        
        public ValidationResult(boolean valid, String message, List<String> missingFields, int completionPercentage) {
            this.valid = valid;
            this.message = message;
            this.missingFields = missingFields;
            this.completionPercentage = completionPercentage;
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public List<String> getMissingFields() { return missingFields; }
        public int getCompletionPercentage() { return completionPercentage; }
    }
    
    /**
     * Get plant-specific data by composite key
     */
    public PlantSpecificDataDto getPlantSpecificData(String plantCode, String materialCode) {
        try {
            PlantSpecificDataId id = new PlantSpecificDataId(plantCode, materialCode);
            PlantSpecificData plantData = plantSpecificDataRepository.findById(id)
                .orElse(null);
            
            return plantData != null ? convertToDto(plantData) : null;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get plant-specific data: " + e.getMessage(), e);
        }
    }
    
    // Helper methods
    private String getStepTitle(Integer stepNumber, List<QuestionTemplate> stepTemplates) {
        if (stepTemplates.isEmpty()) {
            return "Step " + stepNumber;
        }
        
        String category = stepTemplates.get(0).getCategory();
        return category != null ? category : "Step " + stepNumber;
    }
    
    private String getStepDescription(Integer stepNumber, List<QuestionTemplate> stepTemplates) {
        // Map step numbers to descriptions based on actual categories
        Map<Integer, String> stepDescriptions = new HashMap<>();
        stepDescriptions.put(1, "General information about MSDS availability and completeness");
        stepDescriptions.put(2, "Physical properties and handling requirements");
        stepDescriptions.put(3, "Flammability, explosivity and fire safety measures");
        stepDescriptions.put(4, "Toxicity assessment and exposure control");
        stepDescriptions.put(5, "Process safety management thresholds");
        stepDescriptions.put(6, "Reactivity hazards and compatibility");
        stepDescriptions.put(7, "Storage and handling procedures");
        stepDescriptions.put(8, "Personal protective equipment requirements");
        stepDescriptions.put(9, "Spill control measures and procedures");
        stepDescriptions.put(10, "First aid measures and emergency response");
        stepDescriptions.put(11, "Statutory compliance and regulatory requirements");
        stepDescriptions.put(12, "Additional inputs and gap analysis");
        
        // Use predefined descriptions instead of template comments to avoid confusion
        return stepDescriptions.getOrDefault(stepNumber, "Step " + stepNumber + " information");
    }
    
    private QuestionnaireFieldDto convertTemplateToField(QuestionTemplate template, CqsDataDto cqsData) {
        QuestionnaireFieldDto field = new QuestionnaireFieldDto();
        field.setName(template.getFieldName());
        field.setLabel(template.getQuestionText());
        field.setType(template.getQuestionType().toLowerCase());
        field.setRequired(template.getIsRequired() != null ? template.getIsRequired() : false);
        field.setHelpText(template.getHelpText());
        field.setOrderIndex(template.getOrderIndex());
        
        // Check if this is a CQS auto-populated field
        boolean isCqsField = template.isForCQS();
        field.setCqsAutoPopulated(isCqsField);
        
        if (isCqsField) {
            field.setDisabled(true);
            
            // Try to get CQS value for this field
            String cqsValue = getCqsValueForField(template.getFieldName(), cqsData);
            if (cqsValue != null && !cqsValue.trim().isEmpty()) {
                field.setCqsValue(cqsValue);
                field.setPlaceholder("Auto-populated by CQS");
                // For CQS fields with values, don't show options - show the value directly
                field.setOptions(null);
            } else {
                field.setCqsValue("Data not available");
                field.setPlaceholder("CQS data not available for this material");
                // Still parse options for fallback display
                if (template.hasOptions()) {
                    try {
                        List<QuestionnaireFieldDto.OptionDto> options = parseOptions(template.getOptions());
                        field.setOptions(options);
                    } catch (Exception e) {
                        System.err.println("Failed to parse options for field " + template.getFieldName() + ": " + e.getMessage());
                    }
                }
            }
        } else {
            field.setDisabled(false);
            field.setPlaceholder(template.getHelpText());
            
            // Parse options for non-CQS fields
            if (template.hasOptions()) {
                try {
                    List<QuestionnaireFieldDto.OptionDto> options = parseOptions(template.getOptions());
                    field.setOptions(options);
                } catch (Exception e) {
                    System.err.println("Failed to parse options for field " + template.getFieldName() + ": " + e.getMessage());
                }
            }
        }
        
        field.setValidationRules(template.getValidationRules());
        field.setConditionalLogic(template.getConditionalLogic());
        field.setDependsOnField(template.getDependsOnQuestionId());
        
        return field;
    }
    
    /**
     * Update completion statistics based on CQS auto-filled data
     */
    @Transactional
    private void updateCompletionStatsForTemplate(String materialCode, String plantCode, 
                                                List<QuestionnaireStepDto> steps, CqsDataDto cqsData) {
        try {
            // Calculate field statistics
            int totalFields = 0;
            int completedFields = 0;
            int requiredFields = 0;
            int completedRequiredFields = 0;
            
            for (QuestionnaireStepDto step : steps) {
                for (QuestionnaireFieldDto field : step.getFields()) {
                    totalFields++;
                    
                    if (field.isRequired()) {
                        requiredFields++;
                    }
                    
                    // Check if field is completed (has CQS value or is not a CQS field)
                    boolean isCompleted = false;
                    if (field.isCqsAutoPopulated()) {
                        // CQS field is completed if it has a value
                        String cqsValue = getCqsValueForField(field.getName(), cqsData);
                        isCompleted = (cqsValue != null && !cqsValue.trim().isEmpty() && 
                                     !"Data not available".equals(cqsValue));
                    } else {
                        // Non-CQS fields are not completed by default (plant needs to fill them)
                        isCompleted = false;
                    }
                    
                    if (isCompleted) {
                        completedFields++;
                        if (field.isRequired()) {
                            completedRequiredFields++;
                        }
                    }
                }
            }
            
            // Get or create plant-specific data
            PlantSpecificDataId id = new PlantSpecificDataId(plantCode, materialCode);
            PlantSpecificData plantData = plantSpecificDataRepository.findById(id)
                .orElseGet(() -> {
                    PlantSpecificData newData = new PlantSpecificData(plantCode, materialCode);
                    newData.setCreatedBy("SYSTEM");
                    return newData;
                });
            
            // Always update completion stats to override any incorrect existing data
            plantData.updateCompletionStats(totalFields, completedFields, requiredFields, completedRequiredFields);
            
            // Force update the updatedBy and updatedAt to indicate this was recalculated
            plantData.setUpdatedBy("SYSTEM_RECALC");
            plantData.setUpdatedAt(LocalDateTime.now());
            
            // Update CQS sync status if we have CQS data
            if (cqsData != null && cqsData.getCqsData() != null && !cqsData.getCqsData().isEmpty()) {
                plantData.setCqsSyncStatus("SYNCED");
                plantData.setLastCqsSync(LocalDateTime.now());
            }
            
            // Save the updated plant data
            plantSpecificDataRepository.save(plantData);
            
            System.out.println("PlantQuestionnaireService: Updated completion stats - " +
                             "Total: " + totalFields + ", Completed: " + completedFields + 
                             ", Required: " + requiredFields + ", Completed Required: " + completedRequiredFields +
                             ", Progress: " + plantData.getCompletionPercentage() + "%");
            
        } catch (Exception e) {
            System.err.println("Failed to update completion stats: " + e.getMessage());
            // Don't throw exception to avoid breaking template loading
        }
    }
    
    /**
     * Backward compatibility method
     */
    private QuestionnaireFieldDto convertTemplateToField(QuestionTemplate template) {
        return convertTemplateToField(template, null);
    }
    
    /**
     * Get CQS value for a specific field name
     */
    private String getCqsValueForField(String fieldName, CqsDataDto cqsData) {
        if (cqsData == null || cqsData.getCqsData() == null) {
            return null;
        }
        
        Object value = cqsData.getCqsData().get(fieldName);
        if (value == null) {
            return null;
        }
        
        // Convert boolean values to user-friendly text
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "Yes" : "No";
        }
        
        // Convert string values, handling common CQS values
        String stringValue = value.toString().toLowerCase();
        switch (stringValue) {
            case "true":
            case "yes":
            case "y":
                return "Yes";
            case "false":
            case "no":
            case "n":
                return "No";
            case "na":
            case "n/a":
            case "not_applicable":
                return "N/A";
            default:
                // Return the original value with proper capitalization
                return value.toString();
        }
    }
    
    private List<QuestionnaireFieldDto.OptionDto> parseOptions(String optionsJson) {
        try {
            if (optionsJson == null || optionsJson.trim().isEmpty()) {
                return new ArrayList<>();
            }
            
            // Simple parsing - enhance based on your JSON structure
            List<QuestionnaireFieldDto.OptionDto> options = new ArrayList<>();
            
            // For now, assume simple comma-separated values or JSON array
            if (optionsJson.startsWith("[")) {
                // JSON array format
                @SuppressWarnings("unchecked")
                List<Map<String, String>> optionMaps = objectMapper.readValue(optionsJson, List.class);
                for (Map<String, String> optionMap : optionMaps) {
                    options.add(new QuestionnaireFieldDto.OptionDto(
                        optionMap.get("value"), 
                        optionMap.get("label")
                    ));
                }
            } else {
                // Simple comma-separated format
                String[] parts = optionsJson.split(",");
                for (String part : parts) {
                    String trimmed = part.trim();
                    options.add(new QuestionnaireFieldDto.OptionDto(trimmed, trimmed));
                }
            }
            
            return options;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse options: " + optionsJson, e);
        }
    }
    
    /**
     * Sync CQS data for a specific plant record
     */
    @Transactional
    private void syncCqsDataForPlantRecord(PlantSpecificData plantData, String updatedBy) {
        try {
            String materialCode = plantData.getMaterialCode();
            
            // Get CQS data using the integration service
            CqsDataDto cqsData = cqsIntegrationService.getCqsData(materialCode, plantData.getPlantCode());
            
            if (cqsData != null && cqsData.getCqsData() != null && !cqsData.getCqsData().isEmpty()) {
                // Convert CQS data to JSON string
                String cqsJsonData = objectMapper.writeValueAsString(cqsData.getCqsData());
                
                // Update the plant record with CQS data
                plantData.updateCqsData(cqsJsonData, updatedBy);
                plantSpecificDataRepository.save(plantData);
                
                System.out.println("PlantQuestionnaireService: Successfully synced CQS data for " + 
                                 plantData.getPlantCode() + "/" + materialCode);
            } else {
                // Set status to indicate no CQS data available
                plantData.setCqsSyncStatus("NO_DATA");
                plantData.setLastCqsSync(LocalDateTime.now());
                plantData.setUpdatedBy(updatedBy);
                plantSpecificDataRepository.save(plantData);
                
                System.out.println("PlantQuestionnaireService: No CQS data available for " + 
                                 plantData.getPlantCode() + "/" + materialCode);
            }
            
        } catch (Exception e) {
            System.err.println("PlantQuestionnaireService: Failed to sync CQS data for " + 
                             plantData.getPlantCode() + "/" + plantData.getMaterialCode() + ": " + e.getMessage());
            
            // Set failed status
            plantData.setCqsSyncStatus("FAILED");
            plantData.setLastCqsSync(LocalDateTime.now());
            plantData.setUpdatedBy(updatedBy);
            plantSpecificDataRepository.save(plantData);
        }
    }
    
    /**
     * Force sync CQS data for a specific plant and material
     */
    @Transactional
    public void forceSyncCqsData(String plantCode, String materialCode) {
        try {
            PlantSpecificDataId id = new PlantSpecificDataId(plantCode, materialCode);
            Optional<PlantSpecificData> plantDataOpt = plantSpecificDataRepository.findById(id);
            
            if (plantDataOpt.isPresent()) {
                PlantSpecificData plantData = plantDataOpt.get();
                syncCqsDataForPlantRecord(plantData, "ADMIN");
                System.out.println("PlantQuestionnaireService: Force sync completed for " + plantCode + "/" + materialCode);
            } else {
                System.out.println("PlantQuestionnaireService: No plant-specific data found for " + plantCode + "/" + materialCode);
                throw new RuntimeException("Plant-specific data not found for " + plantCode + "/" + materialCode);
            }
            
        } catch (Exception e) {
            System.err.println("PlantQuestionnaireService: Force sync failed for " + plantCode + "/" + materialCode + ": " + e.getMessage());
            throw new RuntimeException("Failed to force sync CQS data: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if there are changes in plant inputs compared to existing data
     */
    public boolean hasPlantInputChanges(PlantSpecificDataDto existingData, Map<String, Object> newResponses) {
        try {
            // If no existing data, any new responses are changes
            if (existingData == null) {
                return newResponses != null && !newResponses.isEmpty();
            }
            
            // If no new responses, no changes
            if (newResponses == null || newResponses.isEmpty()) {
                return existingData.getPlantInputs() != null && !existingData.getPlantInputs().isEmpty();
            }
            
            // Get existing plant inputs
            Map<String, Object> existingInputs = existingData.getPlantInputs();
            if (existingInputs == null) {
                existingInputs = new HashMap<>();
            }
            
            // Compare field by field
            Set<String> allFields = new HashSet<>();
            allFields.addAll(existingInputs.keySet());
            allFields.addAll(newResponses.keySet());
            
            for (String field : allFields) {
                Object existingValue = existingInputs.get(field);
                Object newValue = newResponses.get(field);
                
                // Normalize null and empty values
                existingValue = normalizeValue(existingValue);
                newValue = normalizeValue(newValue);
                
                // Check if values are different
                if (!Objects.equals(existingValue, newValue)) {
                    System.out.println("PlantQuestionnaireService: Change detected in field '" + field + 
                                     "': '" + existingValue + "' -> '" + newValue + "'");
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("PlantQuestionnaireService: Error checking for changes: " + e.getMessage());
            // If we can't determine changes, assume there are changes to be safe
            return true;
        }
    }
    
    /**
     * Normalize values for comparison (treat null, empty string, and whitespace as equivalent)
     */
    private Object normalizeValue(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof String) {
            String strValue = ((String) value).trim();
            return strValue.isEmpty() ? null : strValue;
        }
        
        if (value instanceof List) {
            List<?> listValue = (List<?>) value;
            return listValue.isEmpty() ? null : value;
        }
        
        return value;
    }
    
    private PlantSpecificDataDto convertToDto(PlantSpecificData plantData) {
        try {
            PlantSpecificDataDto dto = new PlantSpecificDataDto();
            dto.setPlantCode(plantData.getPlantCode());
            dto.setMaterialCode(plantData.getMaterialCode());

            dto.setCompletionStatus(plantData.getCompletionStatus());
            dto.setCompletionPercentage(plantData.getCompletionPercentage());
            dto.setTotalFields(plantData.getTotalFields());
            dto.setCompletedFields(plantData.getCompletedFields());
            dto.setRequiredFields(plantData.getRequiredFields());
            dto.setCompletedRequiredFields(plantData.getCompletedRequiredFields());
            dto.setCqsSyncStatus(plantData.getCqsSyncStatus());
            dto.setLastCqsSync(plantData.getLastCqsSync());
            dto.setWorkflowId(plantData.getWorkflowId());
            dto.setCreatedAt(plantData.getCreatedAt());
            dto.setUpdatedAt(plantData.getUpdatedAt());
            dto.setCreatedBy(plantData.getCreatedBy());
            dto.setUpdatedBy(plantData.getUpdatedBy());
            dto.setSubmittedAt(plantData.getSubmittedAt());
            dto.setSubmittedBy(plantData.getSubmittedBy());
            dto.setVersion(plantData.getVersion());
            dto.setIsActive(plantData.getIsActive());
            
            // Parse JSON data
            if (plantData.getCqsInputs() != null && !plantData.getCqsInputs().trim().isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> cqsInputs = objectMapper.readValue(plantData.getCqsInputs(), Map.class);
                dto.setCqsInputs(cqsInputs);
            }
            
            if (plantData.getPlantInputs() != null && !plantData.getPlantInputs().trim().isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> plantInputs = objectMapper.readValue(plantData.getPlantInputs(), Map.class);
                dto.setPlantInputs(plantInputs);
            }
            
            if (plantData.getCombinedData() != null && !plantData.getCombinedData().trim().isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> combinedData = objectMapper.readValue(plantData.getCombinedData(), Map.class);
                dto.setCombinedData(combinedData);
            }
            
            return dto;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert PlantSpecificData to DTO: " + e.getMessage(), e);
        }
    }

    /**
     * Get plant dashboard data with progress information
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPlantDashboardData(String plantCode) {
        try {
            Map<String, Object> dashboardData = new HashMap<>();
            
            // Get all workflows for this plant
            List<Workflow> workflows = workflowRepository.findByPlantCodeWithQueries(plantCode);
            
            // Convert to dashboard format with progress information
            List<Map<String, Object>> workflowsWithProgress = workflows.stream()
                .map(workflow -> {
                    Map<String, Object> workflowData = new HashMap<>();
                    
                    // Get plant-specific data for this workflow
                    PlantSpecificDataDto plantData = getPlantSpecificData(plantCode, workflow.getMaterialCode());
                    
                    // CRITICAL FIX: Force recalculation of completion stats if data exists but stats are missing/incorrect
                    if (plantData != null && plantData.getPlantInputs() != null && !plantData.getPlantInputs().isEmpty()) {
                        // FORCE recalculation every time to ensure dashboard shows correct progress
                        // This ensures the dashboard always reflects the latest completion calculation logic
                        boolean needsRecalculation = true;
                        
                        if (needsRecalculation) {
                            System.out.println("PlantQuestionnaireService: Dashboard data - forcing recalculation for " + 
                                             plantCode + "/" + workflow.getMaterialCode() + 
                                             " (current: " + plantData.getCompletedFields() + "/" + plantData.getTotalFields() + 
                                             ", " + plantData.getCompletionPercentage() + "%)");
                            
                            try {
                                recalculateCompletionStats(workflow.getMaterialCode(), plantCode);
                                // Re-fetch the updated data immediately after recalculation
                                plantData = getPlantSpecificData(plantCode, workflow.getMaterialCode());
                            } catch (Exception e) {
                                System.err.println("PlantQuestionnaireService: Failed to recalculate stats for dashboard: " + e.getMessage());
                            }
                        }
                    }
                    
                    workflowData.put("workflowId", workflow.getId());
                    workflowData.put("materialCode", workflow.getMaterialCode());
                    workflowData.put("materialName", workflow.getMaterialName());
                    workflowData.put("plantCode", plantCode);
                    workflowData.put("itemDescription", workflow.getMaterialDescription());
                    workflowData.put("lastModified", workflow.getLastModified());
                    
                    if (plantData != null) {
                        workflowData.put("completionPercentage", plantData.getCompletionPercentage());
                        workflowData.put("totalFields", plantData.getTotalFields());
                        workflowData.put("completedFields", plantData.getCompletedFields());
                        workflowData.put("requiredFields", plantData.getRequiredFields());
                        workflowData.put("completedRequiredFields", plantData.getCompletedRequiredFields());
                        workflowData.put("isSubmitted", plantData.getSubmittedAt() != null);
                        workflowData.put("submittedAt", plantData.getSubmittedAt());
                        workflowData.put("submittedBy", plantData.getSubmittedBy());
                        workflowData.put("plantInputs", plantData.getPlantInputs());
                        workflowData.put("cqsSyncStatus", plantData.getCqsSyncStatus());
                        workflowData.put("lastCqsSync", plantData.getLastCqsSync());
                        
                        // CRITICAL FIX: Determine completion status based on submission status first
                        String completionStatus;
                        Integer displayCompletionPercentage = plantData.getCompletionPercentage();
                        
                        if (plantData.getSubmittedAt() != null) {
                            // If questionnaire is submitted, it's always COMPLETED with 100% regardless of calculated percentage
                            completionStatus = "COMPLETED";
                            displayCompletionPercentage = 100; // Force 100% for submitted questionnaires
                        } else if (workflow.getState() == WorkflowState.COMPLETED) {
                            // If workflow is completed but no submission timestamp, still mark as completed
                            completionStatus = "COMPLETED";
                            displayCompletionPercentage = 100; // Force 100% for completed workflows
                        } else if (plantData.getCompletionPercentage() != null && plantData.getCompletionPercentage() > 0) {
                            completionStatus = "IN_PROGRESS";
                        } else {
                            completionStatus = "DRAFT";
                        }
                        
                        workflowData.put("completionStatus", completionStatus);
                        workflowData.put("completionPercentage", displayCompletionPercentage); // Override with corrected percentage
                    } else {
                        // No plant data exists yet - initialize with defaults
                        workflowData.put("completionPercentage", 0);
                        workflowData.put("totalFields", 0);
                        workflowData.put("completedFields", 0);
                        workflowData.put("requiredFields", 0);
                        workflowData.put("completedRequiredFields", 0);
                        workflowData.put("isSubmitted", false);
                        workflowData.put("submittedAt", null);
                        workflowData.put("submittedBy", null);
                        workflowData.put("plantInputs", new HashMap<>());
                        workflowData.put("completionStatus", "DRAFT");
                        workflowData.put("cqsSyncStatus", "NOT_SYNCED");
                        workflowData.put("lastCqsSync", null);
                    }
                    
                    // Add open queries count
                    workflowData.put("openQueries", 0); // TODO: Implement query counting
                    
                    return workflowData;
                })
                .collect(Collectors.toList());
            
            // Calculate dashboard statistics
            int totalWorkflows = workflowsWithProgress.size();
            int completedCount = (int) workflowsWithProgress.stream()
                .filter(w -> "COMPLETED".equals(w.get("completionStatus")))
                .count();
            int inProgressCount = (int) workflowsWithProgress.stream()
                .filter(w -> "IN_PROGRESS".equals(w.get("completionStatus")))
                .count();
            int draftCount = (int) workflowsWithProgress.stream()
                .filter(w -> "DRAFT".equals(w.get("completionStatus")))
                .count();
            
            // Calculate average completion percentage
            double averageCompletion = workflowsWithProgress.stream()
                .mapToInt(w -> (Integer) w.getOrDefault("completionPercentage", 0))
                .average()
                .orElse(0.0);
            
            dashboardData.put("workflows", workflowsWithProgress);
            dashboardData.put("totalWorkflows", totalWorkflows);
            dashboardData.put("completedCount", completedCount);
            dashboardData.put("inProgressCount", inProgressCount);
            dashboardData.put("draftCount", draftCount);
            dashboardData.put("averageCompletion", Math.round(averageCompletion));
            dashboardData.put("plantCode", plantCode);
            dashboardData.put("lastUpdated", LocalDateTime.now());
            
            return dashboardData;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get plant dashboard data: " + e.getMessage(), e);
        }
    }
    
    /**
     * Initialize sample plant data for testing
     */
    @Transactional
    public void initializeSamplePlantData(String plantCode) {
        try {
            // Get all workflows for this plant
            List<Workflow> workflows = workflowRepository.findByPlantCodeWithQueries(plantCode);
            
            for (Workflow workflow : workflows) {
                String materialCode = workflow.getMaterialCode();
                
                // Check if plant-specific data already exists
                PlantSpecificDataDto existingData = getPlantSpecificData(plantCode, materialCode);
                if (existingData == null) {
                    // Create new plant-specific data with sample inputs
                    PlantSpecificDataDto plantData = getOrCreatePlantSpecificData(plantCode, materialCode, workflow.getId());
                    
                    // Add some sample plant inputs
                    Map<String, Object> sampleInputs = new HashMap<>();
                    sampleInputs.put("msds_available", "yes");
                    sampleInputs.put("missing_info", "no");
                    sampleInputs.put("sourcing_asked", "yes");
                    sampleInputs.put("cas_available", "yes");
                    sampleInputs.put("corrosive_storage", "proper_ventilation");
                    sampleInputs.put("toxic_powder_handling", "enclosed_system");
                    
                    plantData.setPlantInputs(sampleInputs);
                    
                    // Calculate completion stats
                    plantData.setTotalFields(87); // Total template fields
                    plantData.setCompletedFields(6); // Sample completed fields
                    plantData.setRequiredFields(50); // Estimated required fields
                    plantData.setCompletedRequiredFields(4); // Sample completed required fields
                    
                    savePlantSpecificData(plantData, "SYSTEM_SAMPLE");
                }
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize sample plant data: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get completion statistics for a specific material and plant
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCompletionStats(String materialCode, String plantCode) {
        try {
            PlantSpecificDataDto plantData = getPlantSpecificData(plantCode, materialCode);
            Map<String, Object> stats = new HashMap<>();
            
            if (plantData != null) {
                stats.put("totalFields", plantData.getTotalFields());
                stats.put("completedFields", plantData.getCompletedFields());
                stats.put("requiredFields", plantData.getRequiredFields());
                stats.put("completedRequiredFields", plantData.getCompletedRequiredFields());
                stats.put("completionPercentage", plantData.getCompletionPercentage());
                stats.put("completionStatus", plantData.getCompletionStatus());
                stats.put("isSubmitted", plantData.getSubmittedAt() != null);
                stats.put("submittedAt", plantData.getSubmittedAt());
                stats.put("submittedBy", plantData.getSubmittedBy());
                stats.put("lastUpdated", plantData.getUpdatedAt());
            } else {
                // Default values if no data exists
                stats.put("totalFields", 0);
                stats.put("completedFields", 0);
                stats.put("requiredFields", 0);
                stats.put("completedRequiredFields", 0);
                stats.put("completionPercentage", 0);
                stats.put("completionStatus", "DRAFT");
                stats.put("isSubmitted", false);
                stats.put("submittedAt", null);
                stats.put("submittedBy", null);
                stats.put("lastUpdated", null);
            }
            
            return stats;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get completion stats: " + e.getMessage(), e);
        }
    }

    /**
     * Recalculate completion statistics for a specific material and plant
     */
    @Transactional
    public void recalculateCompletionStats(String materialCode, String plantCode) {
        try {
            // Get the questionnaire template to get correct field counts
            QuestionnaireTemplateDto template = getQuestionnaireTemplate(materialCode, plantCode, "PLANT_QUESTIONNAIRE");
            
            // Get plant-specific data
            PlantSpecificDataDto plantData = getPlantSpecificData(plantCode, materialCode);
            if (plantData == null) {
                throw new RuntimeException("Plant-specific data not found for material " + materialCode + " and plant " + plantCode);
            }
            
            // Calculate field statistics from template
            int totalFields = 0;
            int requiredFields = 0;
            int completedFields = 0;
            int completedRequiredFields = 0;
            
            Map<String, Object> plantInputs = plantData.getPlantInputs() != null ? plantData.getPlantInputs() : new HashMap<>();
            
            // Get CQS data once for all CQS fields to improve performance
            CqsDataDto cqsData = null;
            try {
                cqsData = getCqsData(materialCode, plantCode);
                System.out.println("PlantQuestionnaireService: CQS data loaded successfully");
            } catch (Exception e) {
                System.err.println("PlantQuestionnaireService: Failed to load CQS data: " + e.getMessage());
            }

            int cqsFieldsCount = 0;
            int completedCqsFieldsCount = 0;
            int plantFieldsCount = 0;
            int completedPlantFieldsCount = 0;

            for (QuestionnaireStepDto step : template.getSteps()) {
                System.out.println("PlantQuestionnaireService: Processing step " + step.getStepNumber() + " (" + step.getTitle() + ") with " + step.getFields().size() + " fields");
                
                for (QuestionnaireFieldDto field : step.getFields()) {
                    totalFields++;
                    
                    if (field.isRequired()) {
                        requiredFields++;
                    }
                    
                    // Check if field is completed (has a value)
                    boolean isCompleted = false;
                    
                    if (field.isCqsAutoPopulated()) {
                        cqsFieldsCount++;
                        
                        // Check if CQS field has actual data
                        String cqsValue = getCqsValueForField(field.getName(), cqsData);
                        boolean hasCqsData = cqsValue != null && !cqsValue.trim().isEmpty() && 
                                           !"Data not available".equals(cqsValue) &&
                                           !"Not available".equalsIgnoreCase(cqsValue) &&
                                           !"N/A".equalsIgnoreCase(cqsValue);
                        
                        // Check if field has data in plant inputs (frontend sent it)
                        Object plantValue = plantInputs.get(field.getName());
                        boolean hasPlantData = plantValue != null && !plantValue.toString().trim().isEmpty() && 
                                             !"null".equalsIgnoreCase(plantValue.toString());
                        
                        // CQS field is completed if it has either CQS data or plant data
                        isCompleted = hasCqsData || hasPlantData;
                        
                        if (isCompleted) {
                            completedCqsFieldsCount++;
                        }
                        
                        if (hasPlantData) {
                            System.out.println("PlantQuestionnaireService: ✓ CQS field '" + field.getName() + 
                                             "' has plant data: '" + plantValue + "' - COMPLETED");
                        } else if (hasCqsData) {
                            System.out.println("PlantQuestionnaireService: ✓ CQS field '" + field.getName() + 
                                             "' has CQS data: '" + cqsValue + "' - COMPLETED");
                        } else {
                            System.out.println("PlantQuestionnaireService: ✗ CQS field '" + field.getName() + 
                                             "' has no data - NOT COMPLETED");
                        }
                    } else {
                        plantFieldsCount++;
                        // Plant field - try multiple field name variations
                        Object value = findFieldValue(plantInputs, field.getName(), field.getOrderIndex(), step.getStepNumber());
                        
                        // Plant field is completed if it has a value
                        if (value != null) {
                            if (value instanceof String) {
                                String strValue = ((String) value).trim();
                                isCompleted = !strValue.isEmpty() && 
                                             !"null".equalsIgnoreCase(strValue) && 
                                             !"undefined".equalsIgnoreCase(strValue);
                            } else if (value instanceof Boolean) {
                                isCompleted = true; // Boolean values are always considered complete
                            } else if (value instanceof Number) {
                                isCompleted = true; // Number values are always considered complete
                            } else if (value instanceof java.util.List) {
                                isCompleted = !((java.util.List<?>) value).isEmpty();
                            } else if (value instanceof Map) {
                                isCompleted = !((Map<?, ?>) value).isEmpty();
                            } else {
                                isCompleted = true; // Other non-null values considered complete
                            }
                        }
                        
                        if (isCompleted) {
                            completedPlantFieldsCount++;
                        }
                        
                        System.out.println("PlantQuestionnaireService: Plant field '" + field.getName() + 
                                         "' - value: '" + value + "', completed: " + isCompleted);
                    }
                    
                    if (isCompleted) {
                        completedFields++;
                        if (field.isRequired()) {
                            completedRequiredFields++;
                        }
                    }
                }
            }
            
            // CRITICAL FIX: Use frontend-based completion calculation
            System.out.println("PlantQuestionnaireService: FRONTEND-BASED APPROACH - Frontend sent " + plantInputs.size() + " fields");
            
            int frontendCqsFieldsCompleted = 0;
            int frontendPlantFieldsCompleted = 0;
            
            // Count ALL fields sent by frontend (excluding materialName)
            for (Map.Entry<String, Object> entry : plantInputs.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();
                
                // Skip materialName as it's not a questionnaire field
                if ("materialName".equals(fieldName)) {
                    continue;
                }
                
                // Check if field has a meaningful value
                boolean hasValue = false;
                if (value != null) {
                    if (value instanceof String) {
                        String strValue = ((String) value).trim();
                        hasValue = !strValue.isEmpty() && 
                                  !"null".equalsIgnoreCase(strValue) && 
                                  !"undefined".equalsIgnoreCase(strValue) &&
                                  !"Not Applicable".equalsIgnoreCase(strValue);
                    } else {
                        hasValue = true; // Non-string values are considered valid
                    }
                }
                
                if (hasValue) {
                    frontendPlantFieldsCompleted++;
                }
            }
            
            // CRITICAL FIX: Use template-based calculation as the source of truth
            // The template defines the actual 87 fields that should exist
            System.out.println("PlantQuestionnaireService: ===== TEMPLATE-BASED CALCULATION (FINAL) =====");
            System.out.println("PlantQuestionnaireService: Template total fields: " + totalFields);
            System.out.println("PlantQuestionnaireService: Template completed fields: " + completedFields);
            
            // The template-based calculation is already correct, so we keep those values
            // totalFields and completedFields are already calculated correctly above
            
            // Calculate completion percentage
            int completionPercentage = totalFields > 0 ? Math.round((float)completedFields * 100 / totalFields) : 0;
            
            System.out.println("PlantQuestionnaireService: ===== FINAL CALCULATION SUMMARY =====");
            System.out.println("PlantQuestionnaireService: Using TEMPLATE-BASED calculation as source of truth");
            System.out.println("PlantQuestionnaireService: Total fields: " + totalFields);
            System.out.println("PlantQuestionnaireService: Completed fields: " + completedFields);
            System.out.println("PlantQuestionnaireService: Completion percentage: " + completionPercentage + "%");
            System.out.println("PlantQuestionnaireService: Required fields: " + requiredFields);
            System.out.println("PlantQuestionnaireService: Completed required fields: " + completedRequiredFields);
            
            // Update plant-specific data with recalculated stats
            plantData.setTotalFields(totalFields);
            plantData.setCompletedFields(completedFields);
            plantData.setRequiredFields(requiredFields);
            plantData.setCompletedRequiredFields(completedRequiredFields);
            plantData.setCompletionPercentage(completionPercentage);
            
            // Save the updated data
            savePlantSpecificData(plantData, "SYSTEM_RECALC");
            
            System.out.println("PlantQuestionnaireService: Recalculated completion stats for " + materialCode + 
                             " at plant " + plantCode + " - Total: " + totalFields + 
                             ", Completed: " + completedFields + ", Progress: " + plantData.getCompletionPercentage() + "%" +
                             " (Required: " + completedRequiredFields + "/" + requiredFields + ")");
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to recalculate completion stats: " + e.getMessage(), e);
        }
    }
    
    /**
     * Normalize field names to ensure consistency between frontend and backend
     */
    private String normalizeFieldName(String fieldName) {
        if (fieldName == null) return null;
        
        // Convert camelCase to snake_case and lowercase
        return fieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
    /**
     * Find field value with enhanced matching
     */
    private Object findFieldValue(Map<String, Object> plantInputs, String templateFieldName, Integer orderIndex, Integer stepNumber) {
        if (plantInputs == null || templateFieldName == null) return null;
        
        // Try multiple field name variations
        String[] possibleKeys = {
            templateFieldName,
            templateFieldName.toLowerCase(),
            templateFieldName.toUpperCase(),
            normalizeFieldName(templateFieldName),
            templateFieldName.replace("_", ""),
            templateFieldName.replace("-", "_"),
            orderIndex != null ? "question_" + orderIndex : null,
            stepNumber != null ? "step_" + stepNumber + "_" + templateFieldName : null,
            // Try common variations
            templateFieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(),
            templateFieldName.replaceAll("_", "").toLowerCase()
        };
        
        for (String key : possibleKeys) {
            if (key != null && !key.isEmpty() && plantInputs.containsKey(key)) {
                Object value = plantInputs.get(key);
                if (value != null) {
                    return value;
                }
            }
        }
        
        return null;
    }
    


}