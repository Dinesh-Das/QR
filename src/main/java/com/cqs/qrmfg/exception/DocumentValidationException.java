package com.cqs.qrmfg.exception;

/**
 * Exception thrown when document validation fails
 */
public class DocumentValidationException extends DocumentException {
    
    private final String fileName;
    private final String validationError;
    
    public DocumentValidationException(String fileName, String validationError) {
        super("Document validation failed for '" + fileName + "': " + validationError);
        this.fileName = fileName;
        this.validationError = validationError;
    }
    
    public DocumentValidationException(String fileName, String validationError, Throwable cause) {
        super("Document validation failed for '" + fileName + "': " + validationError, cause);
        this.fileName = fileName;
        this.validationError = validationError;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public String getValidationError() {
        return validationError;
    }
}