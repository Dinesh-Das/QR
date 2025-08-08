package com.cqs.qrmfg.exception;

public class WorkflowNotFoundException extends WorkflowException {
    public WorkflowNotFoundException(String materialCode) {
        super("Workflow not found for material: " + materialCode);
    }
    
    public WorkflowNotFoundException(Long workflowId) {
        super("Workflow not found with ID: " + workflowId);
    }
    
    public WorkflowNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static WorkflowNotFoundException forMaterialCode(String materialCode) {
        return new WorkflowNotFoundException(materialCode);
    }
}