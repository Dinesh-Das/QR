package com.cqs.qrmfg.dto;

import java.util.List;

/**
 * DTO for document reuse requests
 */
public class DocumentReuseRequest {
    private Long workflowId;
    private List<Long> documentIds;
    private String reuseBy;
    private String projectCode;
    private String materialCode;
    private String plantCode;

    // Constructors
    public DocumentReuseRequest() {}

    public DocumentReuseRequest(Long workflowId, List<Long> documentIds, String reuseBy,
                               String projectCode, String materialCode, String plantCode) {
        this.workflowId = workflowId;
        this.documentIds = documentIds;
        this.reuseBy = reuseBy;
        this.projectCode = projectCode;
        this.materialCode = materialCode;
        this.plantCode = plantCode;
    }

    // Getters and Setters
    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
    }

    public List<Long> getDocumentIds() {
        return documentIds;
    }

    public void setDocumentIds(List<Long> documentIds) {
        this.documentIds = documentIds;
    }

    public String getReuseBy() {
        return reuseBy;
    }

    public void setReuseBy(String reuseBy) {
        this.reuseBy = reuseBy;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public String getPlantCode() {
        return plantCode;
    }

    public void setPlantCode(String plantCode) {
        this.plantCode = plantCode;
    }

}