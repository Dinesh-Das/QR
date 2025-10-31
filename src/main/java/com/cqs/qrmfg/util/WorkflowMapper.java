package com.cqs.qrmfg.util;

import com.cqs.qrmfg.dto.WorkflowSummaryDto;
import com.cqs.qrmfg.model.Workflow;
import com.cqs.qrmfg.model.WorkflowState;
import com.cqs.qrmfg.repository.QrmfgProjectItemMasterRepository;
import com.cqs.qrmfg.service.PlantQuestionnaireService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class WorkflowMapper {

    @Autowired
    private QrmfgProjectItemMasterRepository projectItemRepository;

    @Autowired
    private PlantQuestionnaireService plantQuestionnaireService;

    public WorkflowSummaryDto toSummaryDto(Workflow workflow) {
        if (workflow == null) {
            return null;
        }

        // Fetch item description from project item master
        String itemDescription = null;
        if (workflow.getProjectCode() != null && workflow.getMaterialCode() != null) {
            itemDescription = projectItemRepository
                .findItemDescriptionByProjectCodeAndItemCode(workflow.getProjectCode(), workflow.getMaterialCode())
                .orElse(null);
        }

        // Determine completion status based on questionnaire submission
        String completionStatus = determineCompletionStatus(workflow);

        return new WorkflowSummaryDto(
            workflow.getId(),
            workflow.getProjectCode(),
            workflow.getMaterialCode(),
            workflow.getMaterialName(),
            workflow.getMaterialDescription(),
            itemDescription,
            workflow.getState(),
            workflow.getAssignedPlant(),
            workflow.getPlantCode(),
            workflow.getInitiatedBy(),
            workflow.getDaysPending(),
            workflow.getTotalQueriesCount(),
            workflow.getOpenQueriesCount(),
            workflow.getDocuments() != null ? workflow.getDocuments().size() : 0,
            workflow.getCreatedAt(),
            workflow.getLastModified(),
            workflow.isOverdue(),
            completionStatus
        );
    }

    public List<WorkflowSummaryDto> toSummaryDtoList(List<Workflow> workflows) {
        if (workflows == null) {
            return null;
        }

        return workflows.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Determine completion status based on questionnaire submission and workflow state
     */
    private String determineCompletionStatus(Workflow workflow) {
        try {
            // If workflow is completed, return COMPLETED
            if (workflow.getState() == WorkflowState.COMPLETED) {
                return "COMPLETED";
            }

            // Check plant questionnaire status if plant code and material code are available
            if (workflow.getPlantCode() != null && workflow.getMaterialCode() != null) {
                Map<String, Object> questionnaireStatus = plantQuestionnaireService
                    .getQuestionnaireStatus(workflow.getPlantCode(), workflow.getMaterialCode());
                
                if (questionnaireStatus != null) {
                    Boolean isSubmitted = (Boolean) questionnaireStatus.get("isSubmitted");
                    String qCompletionStatus = (String) questionnaireStatus.get("completionStatus");
                    
                    // If questionnaire is submitted or marked as completed, return COMPLETED
                    if (Boolean.TRUE.equals(isSubmitted) || "COMPLETED".equals(qCompletionStatus)) {
                        return "COMPLETED";
                    }
                    
                    // If questionnaire exists but not completed, check completion percentage
                    Integer completionPercentage = (Integer) questionnaireStatus.get("completionPercentage");
                    if (completionPercentage != null && completionPercentage > 0) {
                        return "IN_PROGRESS";
                    }
                }
            }

            // Fall back to workflow state mapping
            switch (workflow.getState()) {
                case JVC_PENDING:
                    return "JVC_PENDING";
                case PLANT_PENDING:
                    return "PLANT_PENDING";
                case CQS_PENDING:
                    return "CQS_PENDING";
                case TECH_PENDING:
                    return "TECH_PENDING";
                case COMPLETED:
                    return "COMPLETED";
                default:
                    return "DRAFT";
            }
        } catch (Exception e) {
            // If there's an error checking questionnaire status, fall back to workflow state
            System.err.println("WorkflowMapper: Error determining completion status for workflow " + 
                             workflow.getId() + ": " + e.getMessage());
            return workflow.getState().name();
        }
    }
}