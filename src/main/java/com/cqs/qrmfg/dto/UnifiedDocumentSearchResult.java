package com.cqs.qrmfg.dto;

import com.cqs.qrmfg.enums.DocumentSource;

import java.util.List;
import java.util.Map;

/**
 * DTO for unified document search results across all document types
 * Provides comprehensive search results with source categorization and metadata
 */
public class UnifiedDocumentSearchResult {
    
    private List<DocumentSummary> allDocuments;
    private Map<DocumentSource, List<DocumentSummary>> documentsBySource;
    private SearchMetadata searchMetadata;
    
    public UnifiedDocumentSearchResult() {}
    
    public UnifiedDocumentSearchResult(List<DocumentSummary> allDocuments, 
                                     Map<DocumentSource, List<DocumentSummary>> documentsBySource,
                                     SearchMetadata searchMetadata) {
        this.allDocuments = allDocuments;
        this.documentsBySource = documentsBySource;
        this.searchMetadata = searchMetadata;
    }
    
    // Getters and setters
    public List<DocumentSummary> getAllDocuments() {
        return allDocuments;
    }
    
    public void setAllDocuments(List<DocumentSummary> allDocuments) {
        this.allDocuments = allDocuments;
    }
    
    public Map<DocumentSource, List<DocumentSummary>> getDocumentsBySource() {
        return documentsBySource;
    }
    
    public void setDocumentsBySource(Map<DocumentSource, List<DocumentSummary>> documentsBySource) {
        this.documentsBySource = documentsBySource;
    }
    
    public SearchMetadata getSearchMetadata() {
        return searchMetadata;
    }
    
    public void setSearchMetadata(SearchMetadata searchMetadata) {
        this.searchMetadata = searchMetadata;
    }
    
    /**
     * Convenience method to get workflow documents
     */
    public List<DocumentSummary> getWorkflowDocuments() {
        return documentsBySource.get(DocumentSource.WORKFLOW);
    }
    
    /**
     * Convenience method to get query documents
     */
    public List<DocumentSummary> getQueryDocuments() {
        return documentsBySource.get(DocumentSource.QUERY);
    }
    
    /**
     * Convenience method to get response documents
     */
    public List<DocumentSummary> getResponseDocuments() {
        return documentsBySource.get(DocumentSource.RESPONSE);
    }
    
    /**
     * Inner class for search metadata
     */
    public static class SearchMetadata {
        private String searchTerm;
        private String projectCode;
        private String materialCode;
        private List<DocumentSource> searchedSources;
        private int totalResults;
        private Map<DocumentSource, Integer> resultsBySource;
        private long searchTimeMs;
        
        public SearchMetadata() {}
        
        public SearchMetadata(String searchTerm, String projectCode, String materialCode,
                            List<DocumentSource> searchedSources, int totalResults,
                            Map<DocumentSource, Integer> resultsBySource, long searchTimeMs) {
            this.searchTerm = searchTerm;
            this.projectCode = projectCode;
            this.materialCode = materialCode;
            this.searchedSources = searchedSources;
            this.totalResults = totalResults;
            this.resultsBySource = resultsBySource;
            this.searchTimeMs = searchTimeMs;
        }
        
        // Getters and setters
        public String getSearchTerm() {
            return searchTerm;
        }
        
        public void setSearchTerm(String searchTerm) {
            this.searchTerm = searchTerm;
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
        
        public List<DocumentSource> getSearchedSources() {
            return searchedSources;
        }
        
        public void setSearchedSources(List<DocumentSource> searchedSources) {
            this.searchedSources = searchedSources;
        }
        
        public int getTotalResults() {
            return totalResults;
        }
        
        public void setTotalResults(int totalResults) {
            this.totalResults = totalResults;
        }
        
        public Map<DocumentSource, Integer> getResultsBySource() {
            return resultsBySource;
        }
        
        public void setResultsBySource(Map<DocumentSource, Integer> resultsBySource) {
            this.resultsBySource = resultsBySource;
        }
        
        public long getSearchTimeMs() {
            return searchTimeMs;
        }
        
        public void setSearchTimeMs(long searchTimeMs) {
            this.searchTimeMs = searchTimeMs;
        }
    }
}