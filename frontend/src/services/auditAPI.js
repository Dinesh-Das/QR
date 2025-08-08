import apiClient from '../api/client';

export const auditAPI = {
  // Workflow audit endpoints
  getWorkflowAuditHistory: workflowId => apiClient.get(`/audit/workflow/${workflowId}`),

  getQueryAuditHistory: queryId => apiClient.get(`/audit/query/${queryId}`),

  getQuestionnaireResponseAuditHistory: responseId =>
    apiClient.get(`/audit/response/${responseId}`),

  getCompleteWorkflowAuditTrail: workflowId =>
    apiClient.get(`/audit/workflow/${workflowId}/complete`),

  // Recent audit activity
  getRecentAuditActivity: (days = 7) => apiClient.get(`/audit/recent?days=${days}`),

  getAuditActivityByUser: username =>
    apiClient.get(`/audit/by-user/${encodeURIComponent(username)}`),

  getAuditActivityByEntityType: (entityType, days = 7) =>
    apiClient.get(`/audit/by-entity/${entityType}?days=${days}`),

  // Audit search and filtering
  searchAuditLogs: searchParams => apiClient.post('/audit/search', searchParams),

  // Export audit data
  exportAuditLogs: (workflowId, format = 'csv') =>
    apiClient.download(`/audit/export/${workflowId}?format=${format}`),

  // Read-only workflow view
  getReadOnlyWorkflowView: workflowId => apiClient.get(`/audit/workflow/${workflowId}/readonly`),

  // Version history for questionnaire responses
  getQuestionnaireResponseVersions: workflowId =>
    apiClient.get(`/audit/workflow/${workflowId}/response-versions`)
};
