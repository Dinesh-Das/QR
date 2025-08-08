package com.cqs.qrmfg.exception;

/**
 * Exception thrown when a document is not found
 */
public class DocumentNotFoundException extends RuntimeException {
    
    public DocumentNotFoundException(String message) {
        super(message);
    }
    
    public DocumentNotFoundException(Long documentId) {
        super("Document not found with ID: " + documentId);
    }
}