import { useEffect, useCallback } from 'react';

import { trackUserActivity } from '../services/monitoringAPI';

/**
 * Custom hook for automatic user activity tracking
 */
export const useActivityTracking = (componentName, trackViews = true) => {
  // Track component view on mount
  useEffect(() => {
    if (trackViews) {
      trackUserActivity('view', componentName);
    }
  }, [componentName, trackViews]);

  // Function to track specific actions
  const trackAction = useCallback(
    (action, details = '') => {
      trackUserActivity(action, `${componentName}${details ? `:${details}` : ''}`);
    },
    [componentName]
  );

  // Function to track button clicks
  const trackClick = useCallback(
    buttonName => {
      trackUserActivity('click', `${componentName}:${buttonName}`);
    },
    [componentName]
  );

  // Function to track form submissions
  const trackSubmit = useCallback(
    formName => {
      trackUserActivity('submit', `${componentName}:${formName}`);
    },
    [componentName]
  );

  // Function to track downloads
  const trackDownload = useCallback(
    fileName => {
      trackUserActivity('download', `${componentName}:${fileName}`);
    },
    [componentName]
  );

  // Function to track searches
  const trackSearch = useCallback(
    searchTerm => {
      trackUserActivity('search', `${componentName}:${searchTerm}`);
    },
    [componentName]
  );

  // Function to track navigation
  const trackNavigation = useCallback(
    destination => {
      trackUserActivity('navigate', `${componentName}:${destination}`);
    },
    [componentName]
  );

  return {
    trackAction,
    trackClick,
    trackSubmit,
    trackDownload,
    trackSearch,
    trackNavigation
  };
};

/**
 * Higher-order component to automatically add activity tracking
 */
export const withActivityTracking = (WrappedComponent, componentName) => {
  return props => {
    const { trackAction, trackClick, trackSubmit, trackDownload, trackSearch, trackNavigation } =
      useActivityTracking(componentName);

    const enhancedProps = {
      ...props,
      trackAction,
      trackClick,
      trackSubmit,
      trackDownload,
      trackSearch,
      trackNavigation
    };

    return <WrappedComponent {...enhancedProps} />;
  };
};

/**
 * Activity tracking utilities
 */
export const ActivityTracker = {
  // Track workflow actions
  trackWorkflowAction: (action, workflowId, details = '') => {
    trackUserActivity(action, `workflow:${workflowId}${details ? `:${details}` : ''}`);
  },

  // Track query actions
  trackQueryAction: (action, queryId, details = '') => {
    trackUserActivity(action, `query:${queryId}${details ? `:${details}` : ''}`);
  },

  // Track document actions
  trackDocumentAction: (action, documentName, details = '') => {
    trackUserActivity(action, `document:${documentName}${details ? `:${details}` : ''}`);
  },

  // Track dashboard interactions
  trackDashboardAction: (action, section, details = '') => {
    trackUserActivity(action, `dashboard:${section}${details ? `:${details}` : ''}`);
  },

  // Track form interactions
  trackFormAction: (action, formName, fieldName = '', details = '') => {
    const actionDetails = fieldName
      ? `${formName}.${fieldName}${details ? `:${details}` : ''}`
      : `${formName}${details ? `:${details}` : ''}`;
    trackUserActivity(action, `form:${actionDetails}`);
  },

  // Track API calls (for performance monitoring)
  trackApiCall: (endpoint, method, duration) => {
    trackUserActivity('api_call', `api:${method} ${endpoint} (${duration}ms)`);
  },

  // Track errors
  trackError: (errorType, component, errorMessage) => {
    trackUserActivity('error', `${component}:${errorType}:${errorMessage}`);
  },

  // Track performance issues
  trackPerformanceIssue: (issueType, component, details) => {
    trackUserActivity('performance_issue', `${component}:${issueType}:${details}`);
  }
};

export default useActivityTracking;
