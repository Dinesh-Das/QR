import apiClient from '../api/client';

/**
 * Query API service providing query management functionality
 * Migrated to use unified APIClient with standardized error handling
 *
 * @namespace queryAPI
 */
export const queryAPI = {
  // Query CRUD operations
  /**
   * Create a new query for a workflow
   * @param {string} workflowId - Workflow ID to create query for
   * @param {Object} queryData - Query data
   * @returns {Promise<Object>} Created query object
   */
  createQuery: (workflowId, queryData) =>
    apiClient.post(`/queries/workflow/${workflowId}`, queryData),

  /**
   * Get query by ID
   * @param {string} id - Query ID
   * @returns {Promise<Object>} Query object
   */
  getQuery: id => apiClient.get(`/queries/${id}`),

  /**
   * Update query by ID
   * @param {string} id - Query ID
   * @param {Object} queryData - Updated query data
   * @returns {Promise<Object>} Updated query object
   */
  updateQuery: (id, queryData) => apiClient.put(`/queries/${id}`, queryData),

  /**
   * Delete query by ID
   * @param {string} id - Query ID
   * @returns {Promise<void>} Promise that resolves when query is deleted
   */
  deleteQuery: id => apiClient.delete(`/queries/${id}`),

  // Query resolution
  /**
   * Resolve a query
   * @param {string} id - Query ID
   * @param {Object} resolutionData - Resolution data
   * @returns {Promise<Object>} Resolved query object
   */
  resolveQuery: (id, resolutionData) => apiClient.post(`/queries/${id}/resolve`, resolutionData),

  /**
   * Reopen a resolved query
   * @param {string} id - Query ID
   * @param {string} reason - Reason for reopening
   * @returns {Promise<Object>} Reopened query object
   */
  reopenQuery: (id, reason) => apiClient.post(`/queries/${id}/reopen`, { reason }),

  // Query assignment
  /**
   * Assign query to team/user
   * @param {string} id - Query ID
   * @param {Object} assignmentData - Assignment data
   * @returns {Promise<Object>} Assigned query object
   */
  assignQuery: (id, assignmentData) => apiClient.post(`/queries/${id}/assign`, assignmentData),

  /**
   * Reassign query to different team
   * @param {string} id - Query ID
   * @param {string} newTeam - New team to assign to
   * @param {string} reason - Reason for reassignment
   * @returns {Promise<Object>} Reassigned query object
   */
  reassignQuery: (id, newTeam, reason) =>
    apiClient.put(`/queries/${id}/assign`, { team: newTeam, reason }),

  // Query search and filtering
  /**
   * Get queries for a specific workflow
   * @param {string} workflowId - Workflow ID
   * @returns {Promise<Array>} Array of queries for the workflow
   */
  getQueriesByWorkflow: workflowId => apiClient.get(`/queries/workflow/${workflowId}`),

  /**
   * Get queries assigned to a team
   * @param {string} team - Team name
   * @returns {Promise<Array>} Array of queries for the team
   */
  getQueriesByTeam: team => apiClient.get(`/queries/team/${team}`),

  /**
   * Get queries by status
   * @param {string} status - Query status
   * @returns {Promise<Array>} Array of queries with the specified status
   */
  getQueriesByStatus: status => apiClient.get(`/queries/status/${status}`),

  /**
   * Get queries raised by current user
   * @param {string} username - Username (unused in current implementation)
   * @returns {Promise<Array>} Array of queries raised by user
   */
  getQueriesByUser: _username => apiClient.get(`/queries/my-raised`),

  /**
   * Search queries with parameters
   * @param {Object} searchParams - Search parameters
   * @returns {Promise<Array>} Array of matching queries
   */
  searchQueries: searchParams => {
    const queryString = new URLSearchParams(searchParams).toString();
    return apiClient.get(`/queries/search?${queryString}`);
  },

  // Query statistics
  /**
   * Get query statistics for time range
   * @param {number} timeRange - Number of days to look back
   * @returns {Promise<Object>} Query statistics
   */
  getQueryStats: timeRange => apiClient.get(`/queries/recent?days=${timeRange}`),

  /**
   * Get query counts by team
   * @param {string} team - Team name
   * @returns {Promise<number>} Number of open queries for team
   */
  getQueryCountsByTeam: team => apiClient.get(`/queries/stats/count-open/${team}`),

  /**
   * Get average resolution time by team
   * @param {string} team - Team name
   * @returns {Promise<number>} Average resolution time in hours
   */
  getAvgResolutionTimeByTeam: team => apiClient.get(`/queries/stats/avg-resolution-time/${team}`),

  /**
   * Get overdue queries
   * @param {number} dayThreshold - Days threshold for overdue (default: 3)
   * @returns {Promise<Array>} Array of overdue queries
   */
  getOverdueQueries: (_dayThreshold = 3) => apiClient.get('/queries/overdue'),

  // Team-specific statistics
  /**
   * Get overdue queries count by team
   * @param {string} team - Team name
   * @returns {Promise<number>} Number of overdue queries for team
   */
  getOverdueQueriesCountByTeam: team => apiClient.get(`/queries/stats/overdue-count/${team}`),

  /**
   * Get queries resolved today by team
   * @param {string} team - Team name
   * @returns {Promise<number>} Number of queries resolved today by team
   */
  getQueriesResolvedTodayByTeam: team => apiClient.get(`/queries/stats/resolved-today/${team}`),

  // Query SLA tracking
  /**
   * Get query SLA status
   * @param {string} id - Query ID
   * @returns {Promise<boolean>} True if query is overdue
   */
  getQuerySLAStatus: id => apiClient.get(`/queries/${id}/is-overdue`),

  /**
   * Get queries nearing SLA deadline
   * @param {number} hoursThreshold - Hours threshold (default: 24)
   * @returns {Promise<Array>} Array of queries needing attention
   */
  getQueriesNearingSLA: (_hoursThreshold = 24) => apiClient.get('/queries/needing-attention'),

  // Query comments/updates (not implemented in backend yet)
  /**
   * Add comment to query
   * @param {string} id - Query ID
   * @param {string} comment - Comment text
   * @returns {Promise<Object>} Added comment object
   */
  addQueryComment: (id, comment) => apiClient.post(`/queries/${id}/comments`, { comment }),

  /**
   * Get comments for query
   * @param {string} id - Query ID
   * @returns {Promise<Array>} Array of comments for the query
   */
  getQueryComments: id => apiClient.get(`/queries/${id}/comments`),

  // Query priority management
  /**
   * Update query priority
   * @param {string} id - Query ID
   * @param {string} priority - New priority level
   * @returns {Promise<Object>} Updated query object
   */
  updateQueryPriority: (id, priority) => apiClient.put(`/queries/${id}/priority`, { priority }),

  /**
   * Escalate query priority
   * @param {string} id - Query ID
   * @param {string} escalationReason - Reason for escalation
   * @returns {Promise<Object>} Escalated query object
   */
  escalateQuery: (id, _escalationReason) =>
    apiClient.put(`/queries/${id}/priority`, { priorityLevel: 'HIGH' }),

  // Query document management
  /**
   * Upload documents to a query
   * @param {string} queryId - Query ID
   * @param {Array<File>} files - Files to upload
   * @returns {Promise<Array>} Array of uploaded document objects
   */
  uploadQueryDocuments: (queryId, files) => {
    return apiClient.upload(`/queries/${queryId}/documents`, files);
  },

  /**
   * Upload documents to a query response
   * @param {string} queryId - Query ID
   * @param {string} responseId - Response ID
   * @param {Array<File>} files - Files to upload
   * @returns {Promise<Array>} Array of uploaded document objects
   */
  uploadResponseDocuments: (queryId, responseId, files) => {
    return apiClient.upload(`/queries/${queryId}/responses/${responseId}/documents`, files);
  },

  /**
   * Get all documents associated with a query
   * @param {string} queryId - Query ID
   * @returns {Promise<Array>} Array of document objects
   */
  getQueryDocuments: (queryId) => apiClient.get(`/queries/${queryId}/documents`),

  /**
   * Download a query document
   * @param {string} queryId - Query ID
   * @param {string} documentId - Document ID
   * @returns {Promise<Blob>} Document file blob
   */
  downloadQueryDocument: (queryId, documentId) => {
    return apiClient.download(`/queries/${queryId}/documents/${documentId}/download`);
  },

  /**
   * Delete a query document
   * @param {string} queryId - Query ID
   * @param {string} documentId - Document ID
   * @returns {Promise<void>} Promise that resolves when document is deleted
   */
  deleteQueryDocument: (queryId, documentId) => {
    return apiClient.delete(`/queries/${queryId}/documents/${documentId}`);
  }
};
