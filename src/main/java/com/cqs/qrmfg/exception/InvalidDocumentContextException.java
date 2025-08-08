package com.cqs.qrmfg.exception;

/**
 * Exception thrown when document context is invalid or missing
 */
public class InvalidDocumentContextException extends DocumentException {
    
    private final String contextType;
    private final String requiredFields;
    
    public InvalidDocumentContextException(String contextType, String requiredFields) {
        super("Invalid document context for type '" + contextType + "'. Required fields: " + requiredFields);
        this.contextType = contextType;
        this.requiredFields = requiredFields;
    }
    
    public InvalidDocumentContextException(String message) {
        super(message);
        this.contextType = null;
        this.requiredFields = null;
    }
    
    public String getContextType() {
        return contextType;
    }
    
    public String getRequiredFields() {
        return requiredFields;
    }
}