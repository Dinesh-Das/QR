import apiClient from '../api/client';

/**
 * Workflow API service providing comprehensive workflow management functionality
 * Migrated to use unified APIClient with standardized error handling
 *
 * @namespace workflowAPI
 */
export const workflowAPI = {
  // Dashboard endpoints
  /**
   * Get dashboard summary data
   * @returns {Promise<Object>} Dashboard summary data
   */
  getDashboardSummary: () => apiClient.get('/dashboard/summary'),

  /**
   * Get overdue workflows based on day threshold
   * @param {number} dayThreshold - Number of days to consider as overdue (default: 3)
   * @returns {Promise<Array>} Array of overdue workflows
   */
  getOverdueWorkflows: (dayThreshold = 3) =>
    apiClient.get(`/workflows/overdue?days=${dayThreshold}`),

  /**
   * Get recent workflow activity
   * @param {number} days - Number of days to look back (default: 7)
   * @returns {Promise<Array>} Array of recent workflows
   */
  getRecentActivity: (days = 7) => apiClient.get(`/workflows/recent/created?days=${days}`),

  /**
   * Get workflow counts grouped by state
   * @returns {Promise<Object>} Workflow counts by state
   */
  getWorkflowCountsByState: () => apiClient.get('/dashboard/counts-by-state'),

  /**
   * Get workflows for a specific plant
   * @param {string} plantName - Name of the plant
   * @returns {Promise<Array>} Array of workflows for the plant
   */
  getWorkflowsByPlant: plantName =>
    apiClient.get(`/workflows/plant/${encodeURIComponent(plantName)}`),

  // Workflow CRUD operations
  /**
   * Create a new workflow
   * @param {Object} workflowData - Workflow data to create
   * @returns {Promise<Object>} Created workflow object
   */
  createWorkflow: workflowData => apiClient.post('/workflows', workflowData),

  /**
   * Get workflow by ID
   * @param {string} id - Workflow ID
   * @returns {Promise<Object>} Workflow object
   */
  getWorkflow: id => apiClient.get(`/workflows/${id}`),

  /**
   * Update workflow by ID
   * @param {string} id - Workflow ID
   * @param {Object} workflowData - Updated workflow data
   * @returns {Promise<Object>} Updated workflow object
   */
  updateWorkflow: (id, workflowData) => apiClient.put(`/workflows/${id}`, workflowData),

  /**
   * Delete workflow by ID
   * @param {string} id - Workflow ID
   * @returns {Promise<void>} Promise that resolves when workflow is deleted
   */
  deleteWorkflow: id => apiClient.delete(`/workflows/${id}`),

  // Workflow state management
  /**
   * Transition workflow to a new state
   * @param {string} id - Workflow ID
   * @param {string} newState - New state to transition to
   * @param {string} comment - Comment for the transition
   * @returns {Promise<Object>} Updated workflow object
   */
  transitionWorkflowState: (id, newState, comment) =>
    apiClient.put(`/workflows/${id}/transition`, { newState, comment }),

  /**
   * Extend workflow deadline
   * @param {string} id - Workflow ID
   * @param {Object} extensionData - Extension data including new deadline
   * @returns {Promise<Object>} Updated workflow object
   */
  extendWorkflow: (id, extensionData) => apiClient.put(`/workflows/${id}/extend`, extensionData),

  /**
   * Complete workflow
   * @param {string} id - Workflow ID
   * @param {Object} completionData - Completion data
   * @returns {Promise<Object>} Completed workflow object
   */
  completeWorkflow: (id, completionData) =>
    apiClient.put(`/workflows/${id}/complete`, completionData),

  /**
   * Smart plant extension - only extend to plants that don't have workflows yet
   * Enhanced to handle document reuse information and provide detailed results
   * @param {Object} extensionData - Extension data with projectCode, materialCode, and plantCodes
   * @returns {Promise<Object>} Smart extension result with created, duplicate, failed workflows, and document reuse information
   */
  extendToMultiplePlantsSmartly: async (extensionData) => {
    try {
      const result = await apiClient.withRetry(
        () => apiClient.post('/workflows/extend-to-plants', extensionData),
        3, // maxRetries
        1000 // baseDelay in ms
      );

      // Enhanced result includes document reuse information
      return {
        ...result,
        documentReuse: result.documentReuse || {
          totalReusedDocuments: 0,
          reusedDocuments: [],
          reuseStrategy: 'NONE',
          sourceDescription: 'No documents available for reuse'
        }
      };
    } catch (error) {
      console.error('[WorkflowAPI] Smart extension failed:', error);
      throw {
        ...error,
        context: 'SMART_PLANT_EXTENSION',
        extensionData
      };
    }
  },

  // Workflow search and filtering
  /**
   * Search workflows with advanced filters
   * @param {Object} searchParams - Search parameters and filters
   * @returns {Promise<Array>} Array of matching workflows
   */
  searchWorkflows: searchParams => apiClient.post('/workflows/search', searchParams),

  /**
   * Get workflows by state
   * @param {string} state - Workflow state
   * @returns {Promise<Array>} Array of workflows in the specified state
   */
  getWorkflowsByState: state => apiClient.get(`/workflows/state/${state}`),

  /**
   * Get workflows initiated by a specific user
   * @param {string} username - Username of the initiator
   * @returns {Promise<Array>} Array of workflows initiated by the user
   */
  getWorkflowsByUser: username =>
    apiClient.get(`/workflows/initiated-by/${encodeURIComponent(username)}`),

  // Workflow validation
  canTransitionTo: (id, newState) => apiClient.get(`/workflows/${id}/can-transition/${newState}`),

  isReadyForCompletion: id => apiClient.get(`/workflows/${id}/ready-for-completion`),

  checkWorkflowExists: (projectCode, materialCode, plantCode) =>
    apiClient.get(
      `/workflows/check-exists?projectCode=${encodeURIComponent(projectCode)}&materialCode=${encodeURIComponent(materialCode)}&plantCode=${encodeURIComponent(plantCode)}`
    ),

  // Workflow statistics
  getWorkflowStats: timeRange => apiClient.get(`/workflows/stats?range=${timeRange}`),

  getCompletionRateByPlant: () => apiClient.get('/workflows/completion-rate-by-plant'),

  getWorkflowCompletionTrend: (months = 6) =>
    apiClient.get(`/workflows/completion-trend?months=${months}`),

  // State-based queries
  getPendingWorkflows: () => apiClient.get('/workflows/pending'),

  getWorkflowsWithOpenQueries: () => apiClient.get('/workflows/with-open-queries'),

  // Count endpoints
  getCountByState: state => apiClient.get(`/workflows/stats/count-by-state/${state}`),

  getOverdueCount: () => apiClient.get('/workflows/stats/overdue-count'),

  getWorkflowsWithOpenQueriesCount: () => apiClient.get('/workflows/stats/with-open-queries-count'),

  // Recent workflows
  getRecentlyCreated: (days = 7) => apiClient.get(`/workflows/recent/created?days=${days}`),

  getRecentlyCompleted: (days = 7) => apiClient.get(`/workflows/recent/completed?days=${days}`),

  // Questionnaire and draft management
  saveDraftResponses: (workflowId, draftData) =>
    apiClient.post(`/workflows/${workflowId}/draft-responses`, draftData),

  getDraftResponses: workflowId => apiClient.get(`/workflows/${workflowId}/draft-responses`),

  submitQuestionnaire: (workflowId, questionnaireData) =>
    apiClient.post(`/workflows/${workflowId}/submit-questionnaire`, questionnaireData),

  // Document management
  getWorkflowDocuments: workflowId => apiClient.get(`/workflows/${workflowId}/documents`),

  // Enhanced document management - get all related documents (workflow + query documents)
  getAllWorkflowRelatedDocuments: async (workflowId) => {
    try {
      return await apiClient.withRetry(
        () => apiClient.get(`/workflows/${workflowId}/documents/all`),
        2, // maxRetries for document operations
        500 // shorter delay for document operations
      );
    } catch (error) {
      console.error(`[WorkflowAPI] Failed to get all documents for workflow ${workflowId}:`, error);
      throw {
        ...error,
        context: 'GET_ALL_WORKFLOW_DOCUMENTS',
        workflowId
      };
    }
  },

  // Export workflow documents as ZIP with retry functionality
  exportWorkflowDocuments: async (workflowId, includeQueryDocuments = true) => {
    try {
      return await apiClient.withRetry(
        () => apiClient.download(`/workflows/${workflowId}/documents/export?includeQueryDocuments=${includeQueryDocuments}`),
        2, // maxRetries for download operations
        1000 // longer delay for file operations
      );
    } catch (error) {
      console.error(`[WorkflowAPI] Failed to export documents for workflow ${workflowId}:`, error);
      throw {
        ...error,
        context: 'EXPORT_WORKFLOW_DOCUMENTS',
         workflowId,
         includeQueryDocuments
      };
    }
  },

  // Get unified document search results across all document types
  searchUnifiedDocuments: async (searchParams) => {
    try {
      return await apiClient.withRetry(
        () => apiClient.post('/documents/search/unified', searchParams),
        2, // maxRetries
        500 // baseDelay
      );
    } catch (error) {
      console.error('[WorkflowAPI] Unified document search failed:', error);
      throw {
        ...error,
        context: 'UNIFIED_DOCUMENT_SEARCH',
         searchParams
      };
    }
  },

  // Get comprehensive document information for a project/material combination
  getUnifiedDocumentInfo: async (projectCode, materialCode) => {
    try {
      return await apiClient.withRetry(
        () => apiClient.get(
          `/documents/unified-info?projectCode=${encodeURIComponent(projectCode)}&materialCode=${encodeURIComponent(materialCode)}`
        ),
        2, // maxRetries
        500 // baseDelay
      );
    } catch (error) {
      console.error(`[WorkflowAPI] Failed to get unified document info for ${projectCode}/${materialCode}:`, error);
      throw {
        ...error,
        context: 'GET_UNIFIED_DOCUMENT_INFO',
         projectCode,
         materialCode
      };
    }
  },

  getReusableDocuments: async (projectCode, materialCode) => {
    try {
      return await apiClient.withRetry(
        () => apiClient.get(
          `/workflows/documents/reusable?projectCode=${encodeURIComponent(projectCode)}&materialCode=${encodeURIComponent(materialCode)}`
        ),
        2, // maxRetries
        500 // baseDelay
      );
    } catch (error) {
      console.error(`[WorkflowAPI] Failed to get reusable documents for ${projectCode}/${materialCode}:`, error);
      throw {
        ...error,
        context: 'GET_REUSABLE_DOCUMENTS',
         projectCode,
         materialCode
      };
    }
  },

  downloadDocument: async (documentId) => {
    try {
      return await apiClient.withRetry(
        () => apiClient.download(`/workflows/documents/${documentId}/download`),
        3, // more retries for downloads as they can be flaky
        1000 // longer delay for file operations
      );
    } catch (error) {
      console.error(`[WorkflowAPI] Failed to download document ${documentId}:`, error);
      throw {
        ...error,
        context: 'DOWNLOAD_DOCUMENT',
        documentId
      };
    }
  },

  uploadDocument: async (workflowId, file, metadata) => {
    try {
      return await apiClient.withRetry(
        () => apiClient.upload(`/workflows/${workflowId}/documents`, file, {
          metadata: JSON.stringify(metadata)
        }),
        2, // fewer retries for uploads to avoid duplicate uploads
        1000 // longer delay for file operations
      );
    } catch (error) {
      console.error(`[WorkflowAPI] Failed to upload document to workflow ${workflowId}:`, error);
      throw {
        ...error,
        context: 'UPLOAD_DOCUMENT',
         workflowId,
        fileName: file?.name || 'unknown'
      };
    }
  },

  // Bulk document operations with enhanced error handling
  uploadMultipleDocuments: async (workflowId, files, metadata = {}) => {
    const results = [];
    const errors = [];

    for (const file of files) {
      try {
        const result = await workflowAPI.uploadDocument(workflowId, file, metadata);
        results.push({ file: file.name, result, success: true });
      } catch (error) {
        console.error(`[WorkflowAPI] Failed to upload ${file.name}:`, error);
        errors.push({ file: file.name, error, success: false });
      }
    }

    return {
      successful: results,
      failed: errors,
      totalFiles: files.length,
      successCount: results.length,
      failureCount: errors.length
    };
  },

  // Retry failed document operations
  retryDocumentOperation: async (operationType, operationData, maxRetries = 3) => {
    let lastError;
    
    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        switch (operationType) {
          case 'UPLOAD':
            return await workflowAPI.uploadDocument(
              operationData.workflowId, 
              operationData.file, 
              operationData.metadata
            );
          case 'DOWNLOAD':
            return await workflowAPI.downloadDocument(operationData.documentId);
          case 'GET_DOCUMENTS':
            return await workflowAPI.getAllWorkflowRelatedDocuments(operationData.workflowId);
          default:
            throw new Error(`Unknown operation type: ${operationType}`);
        }
      } catch (error) {
        lastError = error;
        console.warn(`[WorkflowAPI] Retry attempt ${attempt}/${maxRetries} failed for ${operationType}:`, error);
        
        if (attempt < maxRetries) {
          // Exponential backoff
          const delay = Math.pow(2, attempt) * 1000;
          await new Promise(resolve => setTimeout(resolve, delay));
        }
      }
    }

    throw {
      ...lastError,
      context: 'RETRY_OPERATION_FAILED',
       operationType,
       maxRetries,
      finalAttempt: true
    };
  },

  // Material name management from ProjectItemMaster
  updateAllMaterialNamesFromProjectItemMaster: () =>
    apiClient.post('/workflows/update-material-names'),

  updateMaterialNameFromProjectItemMaster: workflowId =>
    apiClient.post(`/workflows/${workflowId}/update-material-name`),

  // Plant Questionnaire endpoints
  getQuestionnaireTemplate: ({ materialCode, plantCode, templateType = 'PLANT_QUESTIONNAIRE' }) =>
    apiClient.get(
      `/plant-questionnaire/template?materialCode=${encodeURIComponent(materialCode)}&plantCode=${encodeURIComponent(plantCode)}&templateType=${encodeURIComponent(templateType)}`
    ),

  getCqsData: ({ materialCode, plantCode }) =>
    apiClient.get(
      `/plant-questionnaire/cqs-data?materialCode=${encodeURIComponent(materialCode)}&plantCode=${encodeURIComponent(plantCode)}`
    ),

  // New unified questionnaire endpoint
  getQuestionnaireData: (workflowId) =>
    apiClient.get(`/api/v1/questionnaire/${workflowId}`),

  getQuestionnaireForEdit: (workflowId) =>
    apiClient.get(`/api/v1/questionnaire/${workflowId}/edit`),

  getPlantSpecificData: ({ plantCode, materialCode }) =>
    apiClient.get(
      `/plant-questionnaire/plant-data?plantCode=${encodeURIComponent(plantCode)}&materialCode=${encodeURIComponent(materialCode)}`
    ),

  getOrCreatePlantSpecificData: ({ plantCode, materialCode, workflowId }) =>
    apiClient.post(
      `/plant-questionnaire/plant-data/init?plantCode=${encodeURIComponent(plantCode)}&materialCode=${encodeURIComponent(materialCode)}&workflowId=${workflowId}`
    ),

  savePlantSpecificData: (plantSpecificData, modifiedBy = 'SYSTEM') =>
    apiClient.post(
      `/plant-questionnaire/plant-data/save?modifiedBy=${encodeURIComponent(modifiedBy)}`,
      plantSpecificData
    ),

  saveDraftPlantResponses: (workflowId, draftData) =>
    apiClient.post(`/plant-questionnaire/draft?workflowId=${workflowId}`, draftData),

  submitPlantQuestionnaire: (workflowId, submissionData) =>
    apiClient.post(`/plant-questionnaire/submit?workflowId=${workflowId}`, submissionData),

  getPlantQuestionnaireStats: ({ plantCode, materialCode }) => {
    const params = new URLSearchParams({ plantCode });
    if (materialCode) {
      params.append('materialCode', materialCode);
    }
    return apiClient.get(`/plant-questionnaire/stats?${params.toString()}`);
  },

  // Plant Dashboard with progress information
  getPlantDashboardData: plantCode =>
    apiClient.get(`/plant-questionnaire/dashboard?plantCode=${encodeURIComponent(plantCode)}`),

  // Initialize sample plant data for testing
  initializeSamplePlantData: plantCode =>
    apiClient.post(
      `/plant-questionnaire/init-sample-data?plantCode=${encodeURIComponent(plantCode)}`
    ),

  // Document management utility methods
  /**
   * Get document reuse statistics for a project/material combination
   * @param {string} projectCode - Project code
   * @param {string} materialCode - Material code
   * @returns {Promise<Object>} Document reuse statistics
   */
  getDocumentReuseStats: async (projectCode, materialCode) => {
    try {
      return await apiClient.withRetry(
        () => apiClient.get(
          `/workflows/documents/reuse-stats?projectCode=${encodeURIComponent(projectCode)}&materialCode=${encodeURIComponent(materialCode)}`
        ),
        2, // maxRetries
        500 // baseDelay
      );
    } catch (error) {
      console.error(`[WorkflowAPI] Failed to get document reuse stats for ${projectCode}/${materialCode}:`, error);
      throw {
        ...error,
        context: 'GET_DOCUMENT_REUSE_STATS',
         projectCode,
         materialCode
      };
    }
  },

  /**
   * Validate document reuse eligibility before workflow extension
   * @param {string} projectCode - Project code
   * @param {string} materialCode - Material code
   * @param {Array<string>} plantCodes - Plant codes to extend to
   * @returns {Promise<Object>} Document reuse validation result
   */
  validateDocumentReuse: async (projectCode, materialCode, plantCodes) => {
    try {
      return await apiClient.withRetry(
        () => apiClient.post('/workflows/documents/validate-reuse', {
          projectCode,
          materialCode,
          plantCodes
        }),
        2, // maxRetries
        500 // baseDelay
      );
    } catch (error) {
      console.error(`[WorkflowAPI] Failed to validate document reuse for ${projectCode}/${materialCode}:`, error);
      throw {
        ...error,
        context: 'VALIDATE_DOCUMENT_REUSE',
         projectCode,
         materialCode,
         plantCodes
      };
    }
  },

  /**
   * Get document access audit log
   * @param {string} workflowId - Workflow ID
   * @param {Object} options - Query options (dateRange, documentId, etc.)
   * @returns {Promise<Array>} Document access audit log
   */
  getDocumentAuditLog: async (workflowId, options = {}) => {
    try {
      const queryParams = new URLSearchParams(options).toString();
      return await apiClient.withRetry(
        () => apiClient.get(`/workflows/${workflowId}/documents/audit-log?${queryParams}`),
        2, // maxRetries
        500 // baseDelay
      );
    } catch (error) {
      console.error(`[WorkflowAPI] Failed to get document audit log for workflow ${workflowId}:`, error);
      throw {
        ...error,
        context: 'GET_DOCUMENT_AUDIT_LOG',
         workflowId,
         options
      };
    }
  },

  /**
   * Enhanced error handler for document operations
   * Provides user-friendly error messages and recovery suggestions
   * @param {Error} error - The error object
   * @param {string} operation - The operation that failed
   * @returns {Object} Enhanced error information
   */
  handleDocumentError: (error, operation) => {
    const enhancedError = {
      originalError: error,
       operation,
      timestamp: new Date().toISOString(),
      userMessage: 'An error occurred with document operation',
      technicalMessage: error.message,
      recoveryActions: [],
      retryable: false
    };

    // Determine user-friendly message and recovery actions based on error type
    switch (error.context) {
      case 'UPLOAD_DOCUMENT':
        enhancedError.userMessage = 'Failed to upload document. Please check file size and format.';
        enhancedError.recoveryActions = [
          'Verify file is under 25MB',
          'Check file format is supported (PDF, DOCX, XLSX, JPG, PNG)',
          'Try uploading again',
          'Contact support if problem persists'
        ];
        enhancedError.retryable = true;
        break;

      case 'DOWNLOAD_DOCUMENT':
        enhancedError.userMessage = 'Failed to download document. Please try again.';
        enhancedError.recoveryActions = [
          'Check your internet connection',
          'Try downloading again',
          'Contact support if document is corrupted'
        ];
        enhancedError.retryable = true;
        break;

      case 'GET_ALL_WORKFLOW_DOCUMENTS':
        enhancedError.userMessage = 'Failed to load workflow documents.';
        enhancedError.recoveryActions = [
          'Refresh the page',
          'Check your internet connection',
          'Contact support if problem persists'
        ];
        enhancedError.retryable = true;
        break;

      case 'SMART_PLANT_EXTENSION':
        enhancedError.userMessage = 'Failed to extend workflow to plants. Some documents may not have been reused.';
        enhancedError.recoveryActions = [
          'Check if workflows were created successfully',
          'Manually upload documents if needed',
          'Try the extension again',
          'Contact support for assistance'
        ];
        enhancedError.retryable = true;
        break;

      case 'UNIFIED_DOCUMENT_SEARCH':
        enhancedError.userMessage = 'Document search failed. Please try again.';
        enhancedError.recoveryActions = [
          'Simplify your search terms',
          'Try searching again',
          'Use individual document lists instead'
        ];
        enhancedError.retryable = true;
        break;

      default:
        if (error.type === 'NETWORK') {
          enhancedError.userMessage = 'Network error occurred. Please check your connection.';
          enhancedError.retryable = true;
        } else if (error.type === 'TIMEOUT') {
          enhancedError.userMessage = 'Operation timed out. Please try again.';
          enhancedError.retryable = true;
        } else if (error.status === 413) {
          enhancedError.userMessage = 'File is too large. Please use a smaller file.';
          enhancedError.retryable = false;
        } else if (error.status === 415) {
          enhancedError.userMessage = 'File type not supported. Please use PDF, DOCX, XLSX, JPG, or PNG.';
          enhancedError.retryable = false;
        }
        break;
    }

    return enhancedError;
  }
};
