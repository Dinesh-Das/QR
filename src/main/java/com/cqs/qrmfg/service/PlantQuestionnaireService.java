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
    private ObjectMapper objectMapper;

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
                    .map(this::convertTemplateToField)
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
            
            return templateDto;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load questionnaire template: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get CQS auto-populated data (mock implementation - replace with actual CQS integration)
     */
    public CqsDataDto getCqsData(String materialCode, String plantCode) {
        try {
            // Mock CQS data - replace with actual CQS service call
            Map<String, Object> cqsData = new HashMap<>();
            
            // Basic Information - CQS auto-populated fields
            cqsData.put("materialName", "Pending IMP");
            cqsData.put("materialType", "Pending IMP");
            cqsData.put("casNumber", "Pending IMP");
            
            // Physical Properties - CQS auto-populated fields
            cqsData.put("physicalState", "Pending IMP");
            cqsData.put("boilingPoint", "Pending IMP");
            cqsData.put("meltingPoint", "Pending IMP");
            
            // Hazard Classification - CQS auto-populated fields
            cqsData.put("hazardCategories", "Pending IMP");
            cqsData.put("signalWord", "Pending IMP");
            cqsData.put("hazardStatements", "Pending IMP");
            
            CqsDataDto cqsDto = new CqsDataDto(materialCode, plantCode, cqsData);
            cqsDto.setSyncStatus("PENDING_IMP");
            cqsDto.setSyncMessage("CQS integration pending implementation");
            cqsDto.setTotalFields(cqsData.size());
            cqsDto.setPopulatedFields(0); // None populated yet
            cqsDto.setCompletionPercentage(0.0);
            
            return cqsDto;
            
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
     * Submit plant questionnaire
     */
    @Transactional
    public void submitPlantQuestionnaire(String plantCode, String materialCode, String submittedBy) {
        try {
            PlantSpecificDataId id = new PlantSpecificDataId(plantCode, materialCode);
            PlantSpecificData plantData = plantSpecificDataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plant-specific data not found"));
            
            plantData.submit(submittedBy);
            plantSpecificDataRepository.save(plantData);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to submit plant questionnaire: " + e.getMessage(), e);
        }
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
    
    private QuestionnaireFieldDto convertTemplateToField(QuestionTemplate template) {
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
            field.setCqsValue("Pending IMP");
            field.setPlaceholder("Auto-populated by CQS (Pending Implementation)");
        } else {
            field.setDisabled(false);
            field.setPlaceholder(template.getHelpText());
        }
        
        // Parse options if available
        if (template.hasOptions()) {
            try {
                List<QuestionnaireFieldDto.OptionDto> options = parseOptions(template.getOptions());
                field.setOptions(options);
            } catch (Exception e) {
                // Log error and continue
                System.err.println("Failed to parse options for field " + template.getFieldName() + ": " + e.getMessage());
            }
        }
        
        field.setValidationRules(template.getValidationRules());
        field.setConditionalLogic(template.getConditionalLogic());
        field.setDependsOnField(template.getDependsOnQuestionId());
        
        return field;
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
            
            // Get all plant-specific data for this plant
            List<PlantSpecificData> plantDataList = plantSpecificDataRepository.findByPlantCode(plantCode);
            
            // Convert to dashboard format with progress information
            List<Map<String, Object>> workflowsWithProgress = plantDataList.stream()
                .map(plantData -> {
                    Map<String, Object> workflowInfo = new HashMap<>();
                    workflowInfo.put("materialCode", plantData.getMaterialCode());
                    workflowInfo.put("plantCode", plantData.getPlantCode());
                    workflowInfo.put("workflowId", plantData.getWorkflowId());
                    workflowInfo.put("completionStatus", plantData.getCompletionStatus());
                    workflowInfo.put("completionPercentage", plantData.getCompletionPercentage());
                    workflowInfo.put("totalFields", plantData.getTotalFields());
                    workflowInfo.put("completedFields", plantData.getCompletedFields());
                    workflowInfo.put("requiredFields", plantData.getRequiredFields());
                    workflowInfo.put("completedRequiredFields", plantData.getCompletedRequiredFields());
                    workflowInfo.put("lastModified", plantData.getUpdatedAt());
                    workflowInfo.put("submittedAt", plantData.getSubmittedAt());
                    workflowInfo.put("submittedBy", plantData.getSubmittedBy());
                    workflowInfo.put("isSubmitted", plantData.isSubmitted());
                    workflowInfo.put("isCompleted", plantData.isCompleted());
                    return workflowInfo;
                })
                .collect(Collectors.toList());
            
            dashboardData.put("workflows", workflowsWithProgress);
            dashboardData.put("totalWorkflows", workflowsWithProgress.size());
            
            // Calculate summary statistics
            long completedCount = workflowsWithProgress.stream()
                .mapToLong(w -> "COMPLETED".equals(w.get("completionStatus")) || "SUBMITTED".equals(w.get("completionStatus")) ? 1 : 0)
                .sum();
            
            long inProgressCount = workflowsWithProgress.stream()
                .mapToLong(w -> "IN_PROGRESS".equals(w.get("completionStatus")) ? 1 : 0)
                .sum();
            
            long draftCount = workflowsWithProgress.stream()
                .mapToLong(w -> "DRAFT".equals(w.get("completionStatus")) ? 1 : 0)
                .sum();
            
            double averageCompletion = workflowsWithProgress.stream()
                .mapToInt(w -> (Integer) w.getOrDefault("completionPercentage", 0))
                .average()
                .orElse(0.0);
            
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
     * Initialize sample plant data for testing (temporary method)
     */
    @Transactional
    public void initializeSamplePlantData(String plantCode) {
        try {
            // Check if data already exists
            List<PlantSpecificData> existingData = plantSpecificDataRepository.findByPlantCode(plantCode);
            if (!existingData.isEmpty()) {
                return; // Data already exists
            }

            // Create sample plant-specific data records
            String[] sampleMaterials = {"MAT001", "MAT002", "MAT003", "MAT004", "MAT005"};
            for (String materialCode : sampleMaterials) {
                PlantSpecificData plantData = new PlantSpecificData(plantCode, materialCode);
                    plantData.setWorkflowId(System.currentTimeMillis()); // Mock workflow ID
                    plantData.setCreatedBy("SYSTEM");
                    plantData.setUpdatedBy("SYSTEM");
                    
                    // Set random completion data
                    int totalFields = 87;
                    int completedFields = (int) (Math.random() * totalFields);
                    int requiredFields = 50;
                    int completedRequiredFields = Math.min(completedFields, requiredFields);
                    
                    plantData.updateCompletionStats(totalFields, completedFields, requiredFields, completedRequiredFields);
                    
                    // Set random status
                    String[] statuses = {"DRAFT", "IN_PROGRESS", "COMPLETED"};
                    String status = statuses[(int) (Math.random() * statuses.length)];
                    plantData.setCompletionStatus(status);
                    
                    // Initialize with empty JSON data
                    plantData.setCqsInputs("{}");
                    plantData.setPlantInputs("{}");
                    plantData.setCombinedData("{}");
                    
                    plantSpecificDataRepository.save(plantData);
                }
            
            
            System.out.println("Initialized sample plant data for plant: " + plantCode);
            
        } catch (Exception e) {
            System.err.println("Failed to initialize sample plant data: " + e.getMessage());
        }
    }
}