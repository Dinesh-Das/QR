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

@Service
public class PlantQuestionnaireService {

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
                
                // Sort templates within step by order index
                List<QuestionTemplate> sortedTemplates = stepTemplates.stream()
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
            PlantSpecificDataId id = new PlantSpecificDataId(
                dataDto.getPlantCode(), 
                dataDto.getMaterialCode()
            );
            
            PlantSpecificData plantData = plantSpecificDataRepository.findById(id)
                .orElse(new PlantSpecificData(dataDto.getPlantCode(), dataDto.getMaterialCode()));
            
            // Update CQS inputs if provided
            if (dataDto.getCqsInputs() != null) {
                String cqsJson = objectMapper.writeValueAsString(dataDto.getCqsInputs());
                plantData.updateCqsData(cqsJson, modifiedBy);
            }
            
            // Update plant inputs if provided
            if (dataDto.getPlantInputs() != null) {
                String plantJson = objectMapper.writeValueAsString(dataDto.getPlantInputs());
                plantData.updatePlantData(plantJson, modifiedBy);
            }
            
            // Update completion statistics
            if (dataDto.getTotalFields() != null) {
                plantData.updateCompletionStats(
                    dataDto.getTotalFields(),
                    dataDto.getCompletedFields() != null ? dataDto.getCompletedFields() : 0,
                    dataDto.getRequiredFields() != null ? dataDto.getRequiredFields() : 0,
                    dataDto.getCompletedRequiredFields() != null ? dataDto.getCompletedRequiredFields() : 0
                );
            }
            
            plantData.setWorkflowId(dataDto.getWorkflowId());
            plantData.setUpdatedBy(modifiedBy);
            
            plantSpecificDataRepository.save(plantData);
            
        } catch (Exception e) {
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
            
            // Validate questionnaire completion before submission
            ValidationResult validation = validateQuestionnaireCompletion(plantCode, materialCode);
            
            if (!validation.isValid()) {
                result.put("success", false);
                result.put("message", "Cannot submit questionnaire: " + validation.getMessage());
                result.put("missingFields", validation.getMissingFields());
                result.put("completionPercentage", validation.getCompletionPercentage());
                return result;
            }
            
            // Submit the plant data
            plantData.submit(submittedBy);
            plantSpecificDataRepository.save(plantData);
            
            // Update workflow status to COMPLETED
            updateWorkflowStatusOnSubmission(plantCode, materialCode, submittedBy);
            
            result.put("success", true);
            result.put("message", "Questionnaire submitted successfully");
            result.put("submittedAt", plantData.getSubmittedAt());
            result.put("completionPercentage", plantData.getCompletionPercentage());
            
            return result;
            
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "Failed to submit plant questionnaire: " + e.getMessage());
            return errorResult;
        }
    }
    
    /**
     * Validate questionnaire completion before submission
     */
    @Transactional(readOnly = true)
    public ValidationResult validateQuestionnaireCompletion(String plantCode, String materialCode) {
        try {
            // Get the questionnaire template
            QuestionnaireTemplateDto template = getQuestionnaireTemplate(materialCode, plantCode, "PLANT_QUESTIONNAIRE");
            
            // Get plant-specific data
            PlantSpecificDataDto plantData = getPlantSpecificData(plantCode, materialCode);
            if (plantData == null) {
                return new ValidationResult(false, "Plant-specific data not found", new ArrayList<>(), 0);
            }
            
            Map<String, Object> plantInputs = plantData.getPlantInputs() != null ? plantData.getPlantInputs() : new HashMap<>();
            List<String> missingRequiredFields = new ArrayList<>();
            
            int totalFields = 0;
            int completedFields = 0;
            int requiredFields = 0;
            int completedRequiredFields = 0;
            
            // Validate each step and field
            for (QuestionnaireStepDto step : template.getSteps()) {
                for (QuestionnaireFieldDto field : step.getFields()) {
                    totalFields++;
                    
                    boolean isCompleted = false;
                    
                    if (field.isCqsAutoPopulated()) {
                        // CQS field is completed if it has a valid CQS value
                        isCompleted = field.getCqsValue() != null && 
                                     !field.getCqsValue().trim().isEmpty() && 
                                     !"Data not available".equals(field.getCqsValue());
                    } else {
                        // Plant field - check if user has provided a value
                        Object value = plantInputs.get(field.getName());
                        if (value != null) {
                            if (value instanceof String) {
                                isCompleted = !((String) value).trim().isEmpty();
                            } else if (value instanceof java.util.List) {
                                isCompleted = !((java.util.List<?>) value).isEmpty();
                            } else {
                                isCompleted = true;
                            }
                        }
                    }
                    
                    if (isCompleted) {
                        completedFields++;
                    }
                    
                    // Check required fields
                    if (field.isRequired()) {
                        requiredFields++;
                        if (isCompleted) {
                            completedRequiredFields++;
                        } else {
                            missingRequiredFields.add(field.getName() + " (" + field.getLabel() + ")");
                        }
                    }
                }
            }
            
            int completionPercentage = totalFields > 0 ? Math.round((float) completedFields / totalFields * 100) : 0;
            
            // Validation rules:
            // 1. All required fields must be completed
            // 2. At least 90% of total fields should be completed for submission
            boolean isValid = missingRequiredFields.isEmpty() && completionPercentage >= 90;
            
            String message;
            if (!missingRequiredFields.isEmpty()) {
                message = "Missing required fields: " + missingRequiredFields.size() + " field(s)";
            } else if (completionPercentage < 90) {
                message = "Questionnaire is only " + completionPercentage + "% complete. At least 90% completion required for submission.";
            } else {
                message = "Questionnaire is ready for submission";
            }
            
            return new ValidationResult(isValid, message, missingRequiredFields, completionPercentage);
            
        } catch (Exception e) {
            return new ValidationResult(false, "Validation failed: " + e.getMessage(), new ArrayList<>(), 0);
        }
    }
    
    /**
     * Update workflow status when questionnaire is submitted
     */
    @Transactional
    private void updateWorkflowStatusOnSubmission(String plantCode, String materialCode, String submittedBy) {
        try {
            // Find the workflow for this plant and material
            List<Workflow> workflows = workflowRepository.findByPlantCodeWithQueries(plantCode);
            
            Workflow targetWorkflow = workflows.stream()
                .filter(w -> materialCode.equals(w.getMaterialCode()))
                .findFirst()
                .orElse(null);
            
            if (targetWorkflow != null) {
                // Update workflow state to COMPLETED
                targetWorkflow.transitionTo(WorkflowState.COMPLETED, submittedBy);
                workflowRepository.save(targetWorkflow);
                
                System.out.println("PlantQuestionnaireService: Updated workflow " + targetWorkflow.getId() + 
                                 " status to COMPLETED for material " + materialCode + " at plant " + plantCode);
            } else {
                System.err.println("PlantQuestionnaireService: No workflow found for material " + materialCode + 
                           " at plant " + plantCode);
            }
            
        } catch (Exception e) {
            System.err.println("PlantQuestionnaireService: Failed to update workflow status: " + e.getMessage());
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
            boolean isSubmitted = plantData != null && plantData.getSubmittedAt() != null;
            
            // Also check if workflow is completed
            if (!isSubmitted) {
                List<Workflow> workflows = workflowRepository.findByPlantCodeWithQueries(plantCode);
                Workflow targetWorkflow = workflows.stream()
                    .filter(w -> materialCode.equals(w.getMaterialCode()))
                    .findFirst()
                    .orElse(null);
                
                if (targetWorkflow != null && targetWorkflow.getState() == WorkflowState.COMPLETED) {
                    isSubmitted = true;
                }
            }
            
            return isSubmitted;
        } catch (Exception e) {
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
            
            ValidationResult validation = validateQuestionnaireCompletion(plantCode, materialCode);
            
            status.put("exists", true);
            status.put("isSubmitted", isSubmitted);
            status.put("isWorkflowCompleted", isWorkflowCompleted);
            status.put("isReadOnly", isReadOnly);
            status.put("completionPercentage", plantData.getCompletionPercentage());
            status.put("submittedAt", plantData.getSubmittedAt());
            status.put("submittedBy", plantData.getSubmittedBy());
            status.put("canSubmit", validation.isValid() && !isReadOnly);
            status.put("validationMessage", validation.getMessage());
            status.put("missingRequiredFields", validation.getMissingFields().size());
            status.put("workflowState", targetWorkflow != null ? targetWorkflow.getState().name() : "UNKNOWN");
            
            return status;
            
        } catch (Exception e) {
            Map<String, Object> errorStatus = new HashMap<>();
            errorStatus.put("exists", false);
            errorStatus.put("error", e.getMessage());
            return errorStatus;
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
        
        // If we have templates, try to get description from the first template's comments
        if (!stepTemplates.isEmpty() && stepTemplates.get(0).getComments() != null) {
            return stepTemplates.get(0).getComments();
        }
        
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
                        
                        // Determine completion status based on both plant data and workflow state
                        String completionStatus;
                        if (workflow.getState() == WorkflowState.COMPLETED || plantData.getSubmittedAt() != null) {
                            completionStatus = "COMPLETED";
                        } else if (plantData.getCompletionPercentage() != null && plantData.getCompletionPercentage() > 0) {
                            completionStatus = "IN_PROGRESS";
                        } else {
                            completionStatus = "DRAFT";
                        }
                        workflowData.put("completionStatus", completionStatus);
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
            
            for (QuestionnaireStepDto step : template.getSteps()) {
                for (QuestionnaireFieldDto field : step.getFields()) {
                    totalFields++;
                    
                    if (field.isRequired()) {
                        requiredFields++;
                    }
                    
                    // Check if field is completed (has a value)
                    Object value = plantInputs.get(field.getName());
                    boolean isCompleted = false;
                    
                    if (field.isCqsAutoPopulated()) {
                        // CQS field is completed if it has a CQS value
                        isCompleted = field.getCqsValue() != null && 
                                     !field.getCqsValue().trim().isEmpty() && 
                                     !"Data not available".equals(field.getCqsValue());
                    } else {
                        // Plant field is completed if it has a value
                        if (value != null) {
                            if (value instanceof String) {
                                isCompleted = !((String) value).trim().isEmpty();
                            } else if (value instanceof java.util.List) {
                                isCompleted = !((java.util.List<?>) value).isEmpty();
                            } else {
                                isCompleted = true;
                            }
                        }
                    }
                    
                    if (isCompleted) {
                        completedFields++;
                        if (field.isRequired()) {
                            completedRequiredFields++;
                        }
                    }
                }
            }
            
            // Update plant-specific data with recalculated stats
            plantData.setTotalFields(totalFields);
            plantData.setCompletedFields(completedFields);
            plantData.setRequiredFields(requiredFields);
            plantData.setCompletedRequiredFields(completedRequiredFields);
            
            // Save the updated data
            savePlantSpecificData(plantData, "SYSTEM_RECALC");
            
            System.out.println("PlantQuestionnaireService: Recalculated completion stats for " + materialCode + 
                             " at plant " + plantCode + " - Total: " + totalFields + 
                             ", Completed: " + completedFields + ", Progress: " + plantData.getCompletionPercentage() + "%");
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to recalculate completion stats: " + e.getMessage(), e);
        }
    }
}