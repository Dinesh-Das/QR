package com.cqs.qrmfg.util;

import com.cqs.qrmfg.dto.WorkflowSummaryDto;
import com.cqs.qrmfg.model.Workflow;
import com.cqs.qrmfg.repository.QrmfgProjectItemMasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class WorkflowMapper {

    @Autowired
    private QrmfgProjectItemMasterRepository projectItemRepository;

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
            workflow.isOverdue()
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
}