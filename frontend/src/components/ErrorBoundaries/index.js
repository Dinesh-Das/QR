/**
 * Error Boundary Components
 *
 * Specialized error boundaries for different error contexts:
 * - AppErrorBoundary: Application-level errors (critical)
 * - RouteErrorBoundary: Route-specific errors with navigation context
 * - AsyncErrorBoundary: Async operation errors with retry functionality
 * - ComponentErrorBoundary: Component-specific errors with minimal UI disruption
 */

// Main error boundary components
export { default as AppErrorBoundary } from './AppErrorBoundary';
export { default as RouteErrorBoundary, withRouteErrorBoundary } from './RouteErrorBoundary';
export { default as AsyncErrorBoundary, withAsyncErrorBoundary } from './AsyncErrorBoundary';
export {
  default as ComponentErrorBoundary,
  withComponentErrorBoundary
} from './ComponentErrorBoundary';

// Re-export the original ErrorBoundary for backward compatibility
export { default as ErrorBoundary } from '../ErrorBoundary';

/**
 * Error reporting utility function
 * Centralized error reporting that can be used across all error boundaries
 */
export const reportError = (error, context = {}) => {
  const errorReport = {
    id: `error_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
    message: error.message,
    stack: error.stack,
    timestamp: new Date().toISOString(),
    userAgent: navigator.userAgent,
    url: window.location.href,
    context
  };

  try {
    // Send to error reporting service
    if (window.errorReporter) {
      window.errorReporter.captureException(error, {
        extra: errorReport,
        tags: context,
        level: context.level || 'error'
      });
    }

    // Log in development
    if (process.env.NODE_ENV === 'development') {
      console.group('🐛 Error Report');
      console.error('Error:', error);
      console.error('Context:', context);
      console.error('Full Report:', errorReport);
      console.groupEnd();
    }
  } catch (reportingError) {
    console.error('Failed to report error:', reportingError);
  }

  return errorReport;
};

/**
 * Error boundary configuration for different application sections
 */
export const ERROR_BOUNDARY_CONFIG = {
  app: {
    title: 'Application Error',
    subtitle:
      'Something went wrong with the application. Please refresh the page or contact support.',
    showRefresh: true,
    showGoHome: true
  },
  route: {
    title: 'Page Error',
    subtitle: 'There was an issue loading this page. Please try again or go back to the home page.',
    showRetry: true,
    showGoHome: true
  },
  async: {
    title: 'Loading Error',
    subtitle: 'Failed to load data. Please try again.',
    showRetry: true,
    maxRetries: 3
  },
  component: {
    title: 'Component Error',
    subtitle: "This component encountered an error and couldn't render properly.",
    showRetry: true,
    inline: true
  }
};
