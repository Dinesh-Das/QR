package com.cqs.qrmfg.exception;

/**
 * Exception thrown when document access is denied
 */
public class DocumentAccessException extends DocumentException {
    
    private final Long documentId;
    private final String userId;
    private final String accessType;
    
    public DocumentAccessException(Long documentId, String userId, String accessType) {
        super("Access denied to document " + documentId + " for user " + userId + " (access type: " + accessType + ")");
        this.documentId = documentId;
        this.userId = userId;
        this.accessType = accessType;
    }
    
    public DocumentAccessException(Long documentId, String userId, String accessType, String reason) {
        super("Access denied to document " + documentId + " for user " + userId + " (access type: " + accessType + "): " + reason);
        this.documentId = documentId;
        this.userId = userId;
        this.accessType = accessType;
    }
    
    public Long getDocumentId() {
        return documentId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getAccessType() {
        return accessType;
    }
}