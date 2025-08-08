package com.cqs.qrmfg.dto;

import java.util.List;

/**
 * Response DTO for smart plant extension operations
 * Provides detailed information about created, duplicate, and failed workflows
 */
public class SmartExtensionResponseDto {
    
    private boolean success;
    private String message;
    private ExtensionSummary summary;
    private ExtensionDetails details;
    
    public SmartExtensionResponseDto() {}
    
    public SmartExtensionResponseDto(boolean success, String message, ExtensionSummary summary, ExtensionDetails details) {
        this.success = success;
        this.message = message;
        this.summary = summary;
        this.details = details;
    }
    
    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public ExtensionSummary getSummary() { return summary; }
    public void setSummary(ExtensionSummary summary) { this.summary = summary; }
    
    public ExtensionDetails getDetails() { return details; }
    public void setDetails(ExtensionDetails details) { this.details = details; }
    
    // Inner classes for structured response
    public static class ExtensionSummary {
        private int totalRequested;
        private int created;
        private int skipped;
        private int failed;
        
        public ExtensionSummary() {}
        
        public ExtensionSummary(int totalRequested, int created, int skipped, int failed) {
            this.totalRequested = totalRequested;
            this.created = created;
            this.skipped = skipped;
            this.failed = failed;
        }
        
        // Getters and setters
        public int getTotalRequested() { return totalRequested; }
        public void setTotalRequested(int totalRequested) { this.totalRequested = totalRequested; }
        
        public int getCreated() { return created; }
        public void setCreated(int created) { this.created = created; }
        
        public int getSkipped() { return skipped; }
        public void setSkipped(int skipped) { this.skipped = skipped; }
        
        public int getFailed() { return failed; }
        public void setFailed(int failed) { this.failed = failed; }
    }
    
    public static class ExtensionDetails {
        private List<WorkflowSummaryDto> newlyCreatedWorkflows;
        private DuplicateInfo duplicateWorkflows;
        private List<String> failedPlants;
        private DocumentReuseInfo documentReuse;
        
        public ExtensionDetails() {}
        
        public ExtensionDetails(List<WorkflowSummaryDto> newlyCreatedWorkflows, 
                              DuplicateInfo duplicateWorkflows, List<String> failedPlants, 
                              DocumentReuseInfo documentReuse) {
            this.newlyCreatedWorkflows = newlyCreatedWorkflows;
            this.duplicateWorkflows = duplicateWorkflows;
            this.failedPlants = failedPlants;
            this.documentReuse = documentReuse;
        }
        
        // Getters and setters
        public List<WorkflowSummaryDto> getNewlyCreatedWorkflows() { return newlyCreatedWorkflows; }
        public void setNewlyCreatedWorkflows(List<WorkflowSummaryDto> newlyCreatedWorkflows) { 
            this.newlyCreatedWorkflows = newlyCreatedWorkflows; 
        }
        
        public DuplicateInfo getDuplicateWorkflows() { return duplicateWorkflows; }
        public void setDuplicateWorkflows(DuplicateInfo duplicateWorkflows) { 
            this.duplicateWorkflows = duplicateWorkflows; 
        }
        
        public List<String> getFailedPlants() { return failedPlants; }
        public void setFailedPlants(List<String> failedPlants) { this.failedPlants = failedPlants; }
        
        public DocumentReuseInfo getDocumentReuse() { return documentReuse; }
        public void setDocumentReuse(DocumentReuseInfo documentReuse) { this.documentReuse = documentReuse; }
    }
    
    public static class DocumentReuseInfo {
        private int totalReusedDocuments;
        private List<DocumentSummary> reusedDocuments;
        private String reuseStrategy;
        private String sourceDescription;
        private int documentsPerWorkflow;
        private DocumentReuseStatistics statistics;
        
        public DocumentReuseInfo() {}
        
        public DocumentReuseInfo(int totalReusedDocuments, List<DocumentSummary> reusedDocuments, String reuseStrategy) {
            this.totalReusedDocuments = totalReusedDocuments;
            this.reusedDocuments = reusedDocuments;
            this.reuseStrategy = reuseStrategy;
        }
        
        public DocumentReuseInfo(int totalReusedDocuments, List<DocumentSummary> reusedDocuments, 
                               String reuseStrategy, String sourceDescription, int documentsPerWorkflow,
                               DocumentReuseStatistics statistics) {
            this.totalReusedDocuments = totalReusedDocuments;
            this.reusedDocuments = reusedDocuments;
            this.reuseStrategy = reuseStrategy;
            this.sourceDescription = sourceDescription;
            this.documentsPerWorkflow = documentsPerWorkflow;
            this.statistics = statistics;
        }
        
        // Getters and setters
        public int getTotalReusedDocuments() { return totalReusedDocuments; }
        public void setTotalReusedDocuments(int totalReusedDocuments) { this.totalReusedDocuments = totalReusedDocuments; }
        
        public List<DocumentSummary> getReusedDocuments() { return reusedDocuments; }
        public void setReusedDocuments(List<DocumentSummary> reusedDocuments) { this.reusedDocuments = reusedDocuments; }
        
        public String getReuseStrategy() { return reuseStrategy; }
        public void setReuseStrategy(String reuseStrategy) { this.reuseStrategy = reuseStrategy; }
        
        public String getSourceDescription() { return sourceDescription; }
        public void setSourceDescription(String sourceDescription) { this.sourceDescription = sourceDescription; }
        
        public int getDocumentsPerWorkflow() { return documentsPerWorkflow; }
        public void setDocumentsPerWorkflow(int documentsPerWorkflow) { this.documentsPerWorkflow = documentsPerWorkflow; }
        
        public DocumentReuseStatistics getStatistics() { return statistics; }
        public void setStatistics(DocumentReuseStatistics statistics) { this.statistics = statistics; }
    }
    
    public static class DocumentReuseStatistics {
        private int workflowDocuments;
        private int queryDocuments;
        private int responseDocuments;
        private int totalUniqueDocuments;
        private int workflowsWithReusedDocuments;
        
        public DocumentReuseStatistics() {}
        
        public DocumentReuseStatistics(int workflowDocuments, int queryDocuments, int responseDocuments, 
                                     int totalUniqueDocuments, int workflowsWithReusedDocuments) {
            this.workflowDocuments = workflowDocuments;
            this.queryDocuments = queryDocuments;
            this.responseDocuments = responseDocuments;
            this.totalUniqueDocuments = totalUniqueDocuments;
            this.workflowsWithReusedDocuments = workflowsWithReusedDocuments;
        }
        
        // Getters and setters
        public int getWorkflowDocuments() { return workflowDocuments; }
        public void setWorkflowDocuments(int workflowDocuments) { this.workflowDocuments = workflowDocuments; }
        
        public int getQueryDocuments() { return queryDocuments; }
        public void setQueryDocuments(int queryDocuments) { this.queryDocuments = queryDocuments; }
        
        public int getResponseDocuments() { return responseDocuments; }
        public void setResponseDocuments(int responseDocuments) { this.responseDocuments = responseDocuments; }
        
        public int getTotalUniqueDocuments() { return totalUniqueDocuments; }
        public void setTotalUniqueDocuments(int totalUniqueDocuments) { this.totalUniqueDocuments = totalUniqueDocuments; }
        
        public int getWorkflowsWithReusedDocuments() { return workflowsWithReusedDocuments; }
        public void setWorkflowsWithReusedDocuments(int workflowsWithReusedDocuments) { this.workflowsWithReusedDocuments = workflowsWithReusedDocuments; }
    }
    
    public static class DuplicateInfo {
        private List<String> plants;
        private List<WorkflowSummaryDto> existingWorkflows;
        private String reason;
        
        public DuplicateInfo() {}
        
        public DuplicateInfo(List<String> plants, List<WorkflowSummaryDto> existingWorkflows, String reason) {
            this.plants = plants;
            this.existingWorkflows = existingWorkflows;
            this.reason = reason;
        }
        
        // Getters and setters
        public List<String> getPlants() { return plants; }
        public void setPlants(List<String> plants) { this.plants = plants; }
        
        public List<WorkflowSummaryDto> getExistingWorkflows() { return existingWorkflows; }
        public void setExistingWorkflows(List<WorkflowSummaryDto> existingWorkflows) { 
            this.existingWorkflows = existingWorkflows; 
        }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}