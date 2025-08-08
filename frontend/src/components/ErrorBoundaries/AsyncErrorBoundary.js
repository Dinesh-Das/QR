import { SyncOutlined, ReloadOutlined, HomeOutlined } from '@ant-design/icons';
import { Result, Button, Spin } from 'antd';
import React from 'react';

import errorReportingService, {
  ERROR_CATEGORY,
  ERROR_SEVERITY
} from '../../services/errorReporting';

/**
 * Async operation error boundary with retry functionality
 * Handles errors from async operations like API calls, data loading, etc.
 */
export class AsyncErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      errorId: null,
      retryCount: 0,
      isRetrying: false
    };
  }

  static getDerivedStateFromError(_error) {
    return {
      hasError: true,
      errorId: `async_error_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    };
  }

  componentDidCatch(_error, errorInfo) {
    console.error('AsyncErrorBoundary caught an error:', _error, errorInfo);

    this.setState({ error: _error });

    // Report to monitoring service with async operation context
    this.reportError(_error, errorInfo, {
      level: 'async',
      operation: this.props.operation || 'unknown',
      retryCount: this.state.retryCount,
      component: 'AsyncErrorBoundary'
    });
  }

  reportError = async (error, errorInfo, context) => {
    try {
      await errorReportingService.reportError(error, {
        category: ERROR_CATEGORY.ASYNC,
        severity: ERROR_SEVERITY.MEDIUM,
        errorBoundary: 'AsyncErrorBoundary',
        componentStack: errorInfo.componentStack,
        errorId: this.state.errorId,
        operation: context.operation,
        retryCount: context.retryCount,
        ...context,
        props: this.props.errorContext || {}
      });
    } catch (reportingError) {
      console.error('Failed to report async error:', reportingError);
    }
  };

  handleRetry = async () => {
    const { onRetry, maxRetries = 3 } = this.props;

    if (this.state.retryCount >= maxRetries) {
      console.warn('Max retry attempts reached');
      return;
    }

    this.setState({
      isRetrying: true,
      retryCount: this.state.retryCount + 1
    });

    try {
      // Call the retry function if provided
      if (onRetry && typeof onRetry === 'function') {
        await onRetry();
      }

      // Reset error state on successful retry
      this.setState({
        hasError: false,
        error: null,
        errorId: null,
        isRetrying: false
      });
    } catch (retryError) {
      console.error('Retry failed:', retryError);
      this.setState({
        isRetrying: false,
        error: retryError
      });

      // Report retry failure
      this.reportError(
        retryError,
        { componentStack: '' },
        {
          level: 'async',
          operation: `${this.props.operation || 'unknown'}_retry`,
          retryCount: this.state.retryCount,
          component: 'AsyncErrorBoundary'
        }
      );
    }
  };

  handleGoHome = () => {
    window.location.href = '/qrmfg';
  };

  render() {
    if (this.state.hasError) {
      const { operation = 'operation', maxRetries = 3, showGoHome = false } = this.props;

      const canRetry = this.state.retryCount < maxRetries;

      if (this.state.isRetrying) {
        return (
          <div
            style={{
              padding: '50px 20px',
              textAlign: 'center',
              minHeight: '200px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexDirection: 'column'
            }}
          >
            <Spin size="large" />
            <div style={{ marginTop: 16, fontSize: '16px' }}>
              Retrying {operation}... (Attempt {this.state.retryCount + 1}/{maxRetries + 1})
            </div>
          </div>
        );
      }

      return (
        <div
          style={{
            padding: '50px 20px',
            minHeight: '300px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}
        >
          <Result
            status="warning"
            icon={<SyncOutlined style={{ color: '#faad14' }} />}
            title="Loading Error"
            subTitle={
              <div>
                <div>Failed to load data for {operation}. Please try again.</div>
                {this.state.retryCount > 0 && (
                  <div style={{ marginTop: 4, fontSize: '12px', color: '#666' }}>
                    Retry attempts: {this.state.retryCount}/{maxRetries}
                  </div>
                )}
                {this.state.errorId && (
                  <div style={{ marginTop: 8, fontSize: '12px', color: '#666' }}>
                    Error ID: {this.state.errorId}
                  </div>
                )}
              </div>
            }
            extra={[
              canRetry && (
                <Button
                  type="primary"
                  key="retry"
                  onClick={this.handleRetry}
                  icon={<ReloadOutlined />}
                  loading={this.state.isRetrying}
                >
                  Try Again
                </Button>
              ),
              showGoHome && (
                <Button key="home" onClick={this.handleGoHome} icon={<HomeOutlined />}>
                  Go Home
                </Button>
              )
            ].filter(Boolean)}
          />
        </div>
      );
    }

    return this.props.children;
  }
}

/**
 * Higher-order component for wrapping async operations with error boundary
 * @param {React.Component} Component - The component to wrap
 * @param {string} operation - Name of the async operation for error context
 * @param {Function} onRetry - Retry function to call on retry attempts
 * @param {Object} options - Additional options (maxRetries, showGoHome, etc.)
 */
export const withAsyncErrorBoundary = (Component, operation, onRetry, options = {}) => {
  const WrappedComponent = props => (
    <AsyncErrorBoundary operation={operation} onRetry={onRetry} {...options}>
      <Component {...props} />
    </AsyncErrorBoundary>
  );

  WrappedComponent.displayName = `withAsyncErrorBoundary(${Component.displayName || Component.name})`;

  return WrappedComponent;
};

export default AsyncErrorBoundary;
