package com.cqs.qrmfg.exception;

/**
 * Exception thrown when document storage operations fail
 */
public class DocumentStorageException extends DocumentException {
    
    private final String operation;
    private final String fileName;
    
    public DocumentStorageException(String operation, String fileName, String message) {
        super("Document storage " + operation + " failed for '" + fileName + "': " + message);
        this.operation = operation;
        this.fileName = fileName;
    }
    
    public DocumentStorageException(String operation, String fileName, String message, Throwable cause) {
        super("Document storage " + operation + " failed for '" + fileName + "': " + message, cause);
        this.operation = operation;
        this.fileName = fileName;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public String getFileName() {
        return fileName;
    }
}