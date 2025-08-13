package com.cqs.qrmfg.util;

import com.cqs.qrmfg.dto.QuerySummaryDto;
import com.cqs.qrmfg.model.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class QueryMapper {

    public QuerySummaryDto toSummaryDto(Query query) {
        if (query == null) {
            return null;
        }

        // Safely extract workflow properties to handle lazy loading issues
        String materialCode = null;
        String materialName = null;
        String assignedPlant = null;
        
        try {
            if (query.getWorkflow() != null) {
                materialCode = query.getWorkflow().getMaterialCode();
                materialName = query.getWorkflow().getMaterialName();
                assignedPlant = query.getWorkflow().getAssignedPlant();
            }
        } catch (org.hibernate.LazyInitializationException e) {
            // Handle lazy loading exception gracefully
            materialCode = "N/A";
            materialName = "N/A";
            assignedPlant = "N/A";
        }

        // Safely extract workflow ID
        Long workflowId = null;
        try {
            if (query.getWorkflow() != null) {
                workflowId = query.getWorkflow().getId();
            }
        } catch (org.hibernate.LazyInitializationException e) {
            // Handle lazy loading exception gracefully
            workflowId = null;
        }

        return new QuerySummaryDto(
            query.getId(),
            workflowId,
            materialCode,
            materialName,
            assignedPlant,
            query.getStepNumber(),
            query.getFieldName(),
            query.getStepTitle(),
            query.getOriginalQuestion(),
            query.getQuestion(),
            query.getResponse(),
            query.getAssignedTeam(),
            query.getStatus(),
            query.getRaisedBy(),
            query.getResolvedBy(),
            query.getPriorityLevel(),
            query.getCreatedAt(),
            query.getResolvedAt(),
            query.getDaysOpen(),
            query.isOverdue(),
            query.isHighPriority()
        );
    }

    public List<QuerySummaryDto> toSummaryDtoList(List<Query> queries) {
        if (queries == null) {
            return null;
        }

        return queries.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }
}