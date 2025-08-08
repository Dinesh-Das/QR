import apiClient from '../api/client';

/**
 * API service for monitoring and metrics endpoints
 */
export const monitoringAPI = {
  /**
   * Get comprehensive metrics dashboard data
   */
  getMetricsDashboard: async () => {
    try {
      return await apiClient.get('/metrics/dashboard');
    } catch (error) {
      console.error('Error fetching metrics dashboard:', error);
      throw error;
    }
  },

  /**
   * Get workflow-specific metrics
   */
  getWorkflowMetrics: async () => {
    try {
      return await apiClient.get('/metrics/workflow');
    } catch (error) {
      console.error('Error fetching workflow metrics:', error);
      throw error;
    }
  },

  /**
   * Get query-specific metrics
   */
  getQueryMetrics: async () => {
    try {
      return await apiClient.get('/metrics/query');
    } catch (error) {
      console.error('Error fetching query metrics:', error);
      throw error;
    }
  },

  /**
   * Get notification system metrics
   */
  getNotificationMetrics: async () => {
    try {
      return await apiClient.get('/metrics/notification');
    } catch (error) {
      console.error('Error fetching notification metrics:', error);
      throw error;
    }
  },

  /**
   * Get user activity metrics
   */
  getUserActivityMetrics: async () => {
    try {
      return await apiClient.get('/metrics/user-activity');
    } catch (error) {
      console.error('Error fetching user activity metrics:', error);
      throw error;
    }
  },

  /**
   * Get dashboard performance metrics
   */
  getDashboardPerformanceMetrics: async () => {
    try {
      return await apiClient.get('/metrics/performance/dashboard');
    } catch (error) {
      console.error('Error fetching dashboard performance metrics:', error);
      throw error;
    }
  },

  /**
   * Record user activity for analytics
   * Currently disabled until backend endpoints are implemented
   */
  recordUserActivity: async (username, action, component) => {
    // Activity tracking is disabled until backend endpoints are implemented
    // This prevents 404 errors in the console
    if (process.env.NODE_ENV === 'development') {
      console.debug(`Activity tracking (disabled): ${action} by ${username} on ${component}`);
    }
    return Promise.resolve();
  },

  /**
   * Get system health status
   */
  getSystemHealth: async () => {
    try {
      return await apiClient.get('/metrics/health');
    } catch (error) {
      console.error('Error fetching system health:', error);
      throw error;
    }
  },

  /**
   * Get user activity analytics for a specific time period
   */
  getUserActivityAnalytics: async (startDate, endDate) => {
    try {
      return await apiClient.get(
        `/user-activity/analytics?startDate=${encodeURIComponent(startDate)}&endDate=${encodeURIComponent(endDate)}`
      );
    } catch (error) {
      console.error('Error fetching user activity analytics:', error);
      throw error;
    }
  },

  /**
   * Get most active users
   */
  getMostActiveUsers: async (limit = 10) => {
    try {
      return await apiClient.get(`/user-activity/most-active?limit=${limit}`);
    } catch (error) {
      console.error('Error fetching most active users:', error);
      throw error;
    }
  },

  /**
   * Get most used features
   */
  getMostUsedFeatures: async (limit = 10) => {
    try {
      return await apiClient.get(`/user-activity/most-used-features?limit=${limit}`);
    } catch (error) {
      console.error('Error fetching most used features:', error);
      throw error;
    }
  },

  /**
   * Get workflow usage patterns
   */
  getWorkflowUsagePatterns: async () => {
    try {
      return await apiClient.get('/user-activity/workflow-patterns');
    } catch (error) {
      console.error('Error fetching workflow usage patterns:', error);
      throw error;
    }
  },

  /**
   * Get user performance metrics for a specific user
   */
  getUserPerformanceMetrics: async username => {
    try {
      return await apiClient.get(`/user-activity/user-performance/${encodeURIComponent(username)}`);
    } catch (error) {
      console.error('Error fetching user performance metrics:', error);
      throw error;
    }
  }
};

/**
 * Activity tracking utility to automatically record user actions
 * Currently disabled until backend endpoints are implemented
 */
export const trackUserActivity = (action, component) => {
  // Activity tracking is disabled until backend endpoints are implemented
  // This prevents 404 errors in the console
  if (process.env.NODE_ENV === 'development') {
    console.debug(`Activity tracking (disabled): ${action} on ${component}`);
  }
  return Promise.resolve();
};

/**
 * Higher-order component to automatically track component usage
 */
export const withActivityTracking = (WrappedComponent, componentName) => {
  const React = require('react');
  return props => {
    React.useEffect(() => {
      trackUserActivity('view', componentName);
    }, []);

    return React.createElement(WrappedComponent, props);
  };
};

export default monitoringAPI;
