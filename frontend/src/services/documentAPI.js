import apiClient from '../api/client';
import { FILE_SIZE } from '../constants';

export const documentAPI = {
  // Document upload and management
  uploadDocuments: (files, projectCode, materialCode, workflowId) => {
    if (!workflowId) {
      throw new Error('workflowId is required for document upload');
    }

    return apiClient.upload('/documents/upload', files, {
      projectCode,
      materialCode,
      workflowId: workflowId.toString()
    });
  },

  // Document reuse functionality
  getReusableDocuments: (projectCode, materialCode, enhanced = true) =>
    apiClient.get(
      `/documents/reusable?projectCode=${encodeURIComponent(projectCode)}&materialCode=${encodeURIComponent(materialCode)}&enhanced=${enhanced}`
    ),

  reuseDocuments: (documentIds, workflowId, contextType = 'WORKFLOW') => {
    const params = new URLSearchParams();
    params.append('contextType', contextType);
    params.append('workflowId', workflowId.toString());
    
    console.log('=== DOCUMENT REUSE API CALL ===');
    console.log('Context Type:', contextType);
    console.log('Workflow ID:', workflowId);
    console.log('Document IDs:', documentIds);
    console.log('URL:', `/documents/reuse?${params.toString()}`);
    
    return apiClient.post(`/documents/reuse?${params.toString()}`, {
      workflowId,
      documentIds
    });
  },

  // Document access
  downloadDocument: (documentId, workflowId = null) => {
    const url = `/documents/${documentId}/download${workflowId ? `?workflowId=${workflowId}` : ''}`;
    return apiClient.download(url);
  },

  getDocumentInfo: (documentId, enhanced = false) =>
    apiClient.get(`/documents/${documentId}?enhanced=${enhanced}`),

  getWorkflowDocuments: workflowId => apiClient.get(`/documents/workflow/${workflowId}`),

  getDocumentAccessLogs: documentId => apiClient.get(`/documents/${documentId}/access-logs`),

  getDocumentCount: workflowId => apiClient.get(`/documents/workflow/${workflowId}/count`),

  // Document validation
  validateFile: file => {
    const validTypes = [
      'application/pdf',
      'application/msword',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      'application/vnd.ms-excel',
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    ];

    const maxSize = FILE_SIZE.MAX_UPLOAD_SIZE;

    return {
      isValidType: validTypes.includes(file.type),
      isValidSize: file.size <= maxSize,
      type: file.type,
      size: file.size
    };
  },

  // Server-side validation
  validateFileOnServer: file => {
    return apiClient.upload('/documents/validate', file);
  },

  // Delete document
  deleteDocument: documentId => apiClient.delete(`/documents/${documentId}`),

  // Unified document search
  searchAllDocuments: (searchTerm, projectCode = null, materialCode = null, filters = {}) => {
    const params = new URLSearchParams();
    
    if (searchTerm) params.append('searchTerm', searchTerm);
    if (projectCode) params.append('projectCode', projectCode);
    if (materialCode) params.append('materialCode', materialCode);
    
    // Add filter parameters
    if (filters.documentSource) params.append('documentSource', filters.documentSource);
    if (filters.fileType) params.append('fileType', filters.fileType);
    if (filters.uploadedBy) params.append('uploadedBy', filters.uploadedBy);
    if (filters.dateFrom) params.append('dateFrom', filters.dateFrom);
    if (filters.dateTo) params.append('dateTo', filters.dateTo);
    if (filters.sortBy) params.append('sortBy', filters.sortBy);
    if (filters.sortOrder) params.append('sortOrder', filters.sortOrder);
    
    return apiClient.get(`/documents/search?${params.toString()}`);
  },

  // Enhanced unified document search with comprehensive results and metadata
  searchAllDocumentsUnified: (searchTerm, projectCode, materialCode, sources = []) => {
    const params = new URLSearchParams();
    
    if (searchTerm) params.append('searchTerm', searchTerm);
    if (projectCode) params.append('projectCode', projectCode);
    if (materialCode) params.append('materialCode', materialCode);
    
    // Add source filters
    if (sources && sources.length > 0) {
      sources.forEach(source => params.append('sources', source));
    }
    
    return apiClient.get(`/documents/search/unified?${params.toString()}`);
  },

  // Get documents by source type
  getDocumentsBySource: (documentSource, projectCode = null, materialCode = null) => {
    const params = new URLSearchParams();
    params.append('documentSource', documentSource);
    
    if (projectCode) params.append('projectCode', projectCode);
    if (materialCode) params.append('materialCode', materialCode);
    
    return apiClient.get(`/documents/by-source?${params.toString()}`);
  }
};
