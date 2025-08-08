/**
 * Error Reporting Service
 *
 * Centralized error reporting and monitoring integration
 * Handles error categorization, context collection, and reporting to monitoring services
 */

// Error severity levels
export const ERROR_SEVERITY = {
  LOW: 'low',
  MEDIUM: 'medium',
  HIGH: 'high',
  CRITICAL: 'critical'
};

// Error categories
export const ERROR_CATEGORY = {
  APPLICATION: 'application',
  ROUTE: 'route',
  ASYNC: 'async',
  COMPONENT: 'component',
  API: 'api',
  AUTHENTICATION: 'authentication',
  VALIDATION: 'validation',
  NETWORK: 'network',
  UNKNOWN: 'unknown'
};

// Error types for monitoring
export const ERROR_TYPE = {
  JAVASCRIPT: 'javascript',
  PROMISE_REJECTION: 'promise_rejection',
  NETWORK: 'network',
  API: 'api',
  COMPONENT: 'component'
};

class ErrorReportingService {
  constructor() {
    this.isEnabled = process.env.REACT_APP_ERROR_REPORTING_ENABLED !== 'false';
    this.apiEndpoint = process.env.REACT_APP_ERROR_REPORTING_ENDPOINT;
    this.environment = process.env.NODE_ENV || 'development';
    this.appVersion = process.env.REACT_APP_VERSION || '1.0.0';
    this.maxReportsPerSession = 50; // Prevent spam
    this.reportCount = 0;

    // Initialize error monitoring
    this.initializeGlobalErrorHandlers();
  }

  /**
   * Initialize global error handlers for unhandled errors
   */
  initializeGlobalErrorHandlers() {
    // Handle unhandled JavaScript errors
    window.addEventListener('error', event => {
      this.reportError(event.error || new Error(event.message), {
        category: ERROR_CATEGORY.APPLICATION,
        severity: ERROR_SEVERITY.HIGH,
        type: ERROR_TYPE.JAVASCRIPT,
        context: {
          filename: event.filename,
          lineno: event.lineno,
          colno: event.colno,
          source: 'global_error_handler'
        }
      });
    });

    // Handle unhandled promise rejections
    window.addEventListener('unhandledrejection', event => {
      this.reportError(event.reason || new Error('Unhandled Promise Rejection'), {
        category: ERROR_CATEGORY.APPLICATION,
        severity: ERROR_SEVERITY.HIGH,
        type: ERROR_TYPE.PROMISE_REJECTION,
        context: {
          source: 'unhandled_promise_rejection'
        }
      });
    });
  }

  /**
   * Get user context for error reporting
   */
  getUserContext() {
    try {
      // Get user information from secure storage
      const userStr = sessionStorage.getItem('user');
      const user = userStr ? JSON.parse(userStr) : null;

      return {
        userId: user?.id || 'anonymous',
        username: user?.username || 'anonymous',
        roles: user?.roles || [],
        sessionId: this.getSessionId(),
        isAuthenticated: !!user
      };
    } catch (error) {
      return {
        userId: 'anonymous',
        username: 'anonymous',
        roles: [],
        sessionId: this.getSessionId(),
        isAuthenticated: false
      };
    }
  }

  /**
   * Get session ID for tracking
   */
  getSessionId() {
    let sessionId = sessionStorage.getItem('error_session_id');
    if (!sessionId) {
      sessionId = `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
      sessionStorage.setItem('error_session_id', sessionId);
    }
    return sessionId;
  }

  /**
   * Get system context for error reporting
   */
  getSystemContext() {
    return {
      userAgent: navigator.userAgent,
      url: window.location.href,
      pathname: window.location.pathname,
      search: window.location.search,
      referrer: document.referrer,
      timestamp: new Date().toISOString(),
      environment: this.environment,
      appVersion: this.appVersion,
      viewport: {
        width: window.innerWidth,
        height: window.innerHeight
      },
      screen: {
        width: window.screen.width,
        height: window.screen.height
      },
      connection: this.getConnectionInfo()
    };
  }

  /**
   * Get network connection information
   */
  getConnectionInfo() {
    if ('connection' in navigator) {
      const connection = navigator.connection;
      return {
        effectiveType: connection.effectiveType,
        downlink: connection.downlink,
        rtt: connection.rtt,
        saveData: connection.saveData
      };
    }
    return null;
  }

  /**
   * Categorize error based on error message and context
   */
  categorizeError(error, context = {}) {
    const message = error.message?.toLowerCase() || '';
    const stack = error.stack?.toLowerCase() || '';

    // API errors
    if (
      message.includes('fetch') ||
      message.includes('network') ||
      message.includes('api') ||
      context.type === ERROR_TYPE.API
    ) {
      return {
        category: ERROR_CATEGORY.API,
        severity: ERROR_SEVERITY.MEDIUM,
        type: ERROR_TYPE.API
      };
    }

    // Network errors
    if (
      message.includes('network') ||
      message.includes('connection') ||
      message.includes('timeout')
    ) {
      return {
        category: ERROR_CATEGORY.NETWORK,
        severity: ERROR_SEVERITY.MEDIUM,
        type: ERROR_TYPE.NETWORK
      };
    }

    // Authentication errors
    if (
      message.includes('auth') ||
      message.includes('token') ||
      message.includes('unauthorized') ||
      message.includes('forbidden')
    ) {
      return {
        category: ERROR_CATEGORY.AUTHENTICATION,
        severity: ERROR_SEVERITY.HIGH,
        type: ERROR_TYPE.API
      };
    }

    // Component errors
    if (stack.includes('react') || context.category === ERROR_CATEGORY.COMPONENT) {
      return {
        category: ERROR_CATEGORY.COMPONENT,
        severity: ERROR_SEVERITY.MEDIUM,
        type: ERROR_TYPE.COMPONENT
      };
    }

    // Route errors
    if (context.category === ERROR_CATEGORY.ROUTE) {
      return {
        category: ERROR_CATEGORY.ROUTE,
        severity: ERROR_SEVERITY.MEDIUM,
        type: ERROR_TYPE.COMPONENT
      };
    }

    // Default categorization
    return {
      category: context.category || ERROR_CATEGORY.UNKNOWN,
      severity: context.severity || ERROR_SEVERITY.MEDIUM,
      type: context.type || ERROR_TYPE.JAVASCRIPT
    };
  }

  /**
   * Sanitize error data to remove sensitive information
   */
  sanitizeErrorData(errorData) {
    const sanitized = { ...errorData };

    // Remove sensitive patterns from message and stack
    const sensitivePatterns = [
      /password[=:]\s*[^\s&]+/gi,
      /token[=:]\s*[^\s&]+/gi,
      /key[=:]\s*[^\s&]+/gi,
      /secret[=:]\s*[^\s&]+/gi,
      /authorization:\s*bearer\s+[^\s&]+/gi
    ];

    sensitivePatterns.forEach(pattern => {
      if (sanitized.message) {
        sanitized.message = sanitized.message.replace(pattern, '[REDACTED]');
      }
      if (sanitized.stack) {
        sanitized.stack = sanitized.stack.replace(pattern, '[REDACTED]');
      }
    });

    // Remove sensitive context data
    if (sanitized.context) {
      delete sanitized.context.password;
      delete sanitized.context.token;
      delete sanitized.context.apiKey;
      delete sanitized.context.secret;
    }

    return sanitized;
  }

  /**
   * Create comprehensive error report
   */
  createErrorReport(error, context = {}) {
    const errorId = `error_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    const categorization = this.categorizeError(error, context);

    const errorReport = {
      id: errorId,
      message: error.message || 'Unknown error',
      stack: error.stack || '',
      name: error.name || 'Error',

      // Categorization
      category: categorization.category,
      severity: categorization.severity,
      type: categorization.type,

      // Context
      userContext: this.getUserContext(),
      systemContext: this.getSystemContext(),
      errorContext: context,

      // Metadata
      timestamp: new Date().toISOString(),
      reportCount: ++this.reportCount,

      // Additional debugging info
      componentStack: context.componentStack || '',
      errorBoundary: context.errorBoundary || null,
      retryCount: context.retryCount || 0,

      // Performance context
      performance: this.getPerformanceContext()
    };

    return this.sanitizeErrorData(errorReport);
  }

  /**
   * Get performance context for error reporting
   */
  getPerformanceContext() {
    try {
      const navigation = performance.getEntriesByType('navigation')[0];
      return {
        loadTime: navigation ? navigation.loadEventEnd - navigation.loadEventStart : null,
        domContentLoaded: navigation
          ? navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart
          : null,
        memoryUsage: performance.memory
          ? {
              used: performance.memory.usedJSHeapSize,
              total: performance.memory.totalJSHeapSize,
              limit: performance.memory.jsHeapSizeLimit
            }
          : null
      };
    } catch (error) {
      return null;
    }
  }

  /**
   * Send error report to monitoring service
   */
  async sendToMonitoringService(errorReport) {
    if (!this.apiEndpoint) {
      return false;
    }

    try {
      const response = await fetch(this.apiEndpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(errorReport)
      });

      return response.ok;
    } catch (error) {
      console.error('Failed to send error report to monitoring service:', error);
      return false;
    }
  }

  /**
   * Send error to external monitoring services (Sentry, LogRocket, etc.)
   */
  sendToExternalServices(errorReport) {
    try {
      // Sentry integration
      if (window.Sentry) {
        window.Sentry.captureException(new Error(errorReport.message), {
          extra: errorReport,
          tags: {
            category: errorReport.category,
            severity: errorReport.severity,
            type: errorReport.type,
            errorId: errorReport.id
          },
          level: this.mapSeverityToSentryLevel(errorReport.severity),
          user: {
            id: errorReport.userContext.userId,
            username: errorReport.userContext.username
          }
        });
      }

      // LogRocket integration
      if (window.LogRocket) {
        window.LogRocket.captureException(new Error(errorReport.message));
      }

      // Custom error reporter
      if (window.errorReporter) {
        window.errorReporter.captureException(new Error(errorReport.message), {
          extra: errorReport,
          tags: {
            category: errorReport.category,
            severity: errorReport.severity,
            errorId: errorReport.id
          }
        });
      }
    } catch (error) {
      console.error('Failed to send to external monitoring services:', error);
    }
  }

  /**
   * Map severity to Sentry level
   */
  mapSeverityToSentryLevel(severity) {
    const mapping = {
      [ERROR_SEVERITY.LOW]: 'info',
      [ERROR_SEVERITY.MEDIUM]: 'warning',
      [ERROR_SEVERITY.HIGH]: 'error',
      [ERROR_SEVERITY.CRITICAL]: 'fatal'
    };
    return mapping[severity] || 'error';
  }

  /**
   * Main error reporting method
   */
  async reportError(error, context = {}) {
    // Check if reporting is enabled and within limits
    if (!this.isEnabled || this.reportCount >= this.maxReportsPerSession) {
      return null;
    }

    try {
      // Create comprehensive error report
      const errorReport = this.createErrorReport(error, context);

      // Log in development
      if (this.environment === 'development') {
        console.group(`🐛 Error Report (${errorReport.category})`);
        console.error('Error:', error);
        console.error('Context:', context);
        console.error('Full Report:', errorReport);
        console.groupEnd();
      }

      // Send to monitoring services
      const promises = [
        this.sendToMonitoringService(errorReport),
        this.sendToExternalServices(errorReport)
      ];

      await Promise.allSettled(promises);

      return errorReport;
    } catch (reportingError) {
      console.error('Failed to report error:', reportingError);
      return null;
    }
  }

  /**
   * Report API error with specific context
   */
  reportApiError(error, apiContext = {}) {
    return this.reportError(error, {
      category: ERROR_CATEGORY.API,
      severity: ERROR_SEVERITY.MEDIUM,
      type: ERROR_TYPE.API,
      ...apiContext
    });
  }

  /**
   * Report component error with specific context
   */
  reportComponentError(error, componentContext = {}) {
    return this.reportError(error, {
      category: ERROR_CATEGORY.COMPONENT,
      severity: ERROR_SEVERITY.MEDIUM,
      type: ERROR_TYPE.COMPONENT,
      ...componentContext
    });
  }

  /**
   * Report authentication error
   */
  reportAuthError(error, authContext = {}) {
    return this.reportError(error, {
      category: ERROR_CATEGORY.AUTHENTICATION,
      severity: ERROR_SEVERITY.HIGH,
      type: ERROR_TYPE.API,
      ...authContext
    });
  }

  /**
   * Get error statistics for dashboard
   */
  getErrorStats() {
    return {
      reportCount: this.reportCount,
      maxReports: this.maxReportsPerSession,
      isEnabled: this.isEnabled,
      environment: this.environment,
      sessionId: this.getSessionId()
    };
  }
}

// Create singleton instance
const errorReportingService = new ErrorReportingService();

// Export service and utilities
export default errorReportingService;
export { ErrorReportingService };

// Convenience methods
export const reportError = (error, context) => errorReportingService.reportError(error, context);
export const reportApiError = (error, context) =>
  errorReportingService.reportApiError(error, context);
export const reportComponentError = (error, context) =>
  errorReportingService.reportComponentError(error, context);
export const reportAuthError = (error, context) =>
  errorReportingService.reportAuthError(error, context);
