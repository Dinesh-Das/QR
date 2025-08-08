import { ExclamationCircleOutlined, ReloadOutlined } from '@ant-design/icons';
import { Alert, Button, Card } from 'antd';
import React from 'react';

import errorReportingService from '../../services/errorReporting';

/**
 * Component-specific error boundary for handling errors within individual components
 * Provides minimal UI disruption with inline error display
 */
export class ComponentErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      errorId: null
    };
  }

  static getDerivedStateFromError(_error) {
    return {
      hasError: true,
      errorId: `component_error_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    };
  }

  componentDidCatch(_error, errorInfo) {
    console.error(
      `ComponentErrorBoundary (${this.props.componentName}) caught an error:`,
      _error,
      errorInfo
    );

    this.setState({ error: _error });

    // Report to monitoring service with component-specific context
    this.reportError(_error, errorInfo, {
      level: 'component',
      componentName: this.props.componentName || 'unknown',
      component: 'ComponentErrorBoundary'
    });
  }

  reportError = async (error, errorInfo, context) => {
    try {
      await errorReportingService.reportComponentError(error, {
        errorBoundary: 'ComponentErrorBoundary',
        componentStack: errorInfo.componentStack,
        errorId: this.state.errorId,
        componentName: context.componentName,
        ...context,
        props: this.props.errorContext || {}
      });
    } catch (reportingError) {
      console.error('Failed to report component error:', reportingError);
    }
  };

  handleRetry = () => {
    this.setState({
      hasError: false,
      error: null,
      errorId: null
    });
  };

  render() {
    if (this.state.hasError) {
      const {
        componentName = 'Component',
        fallbackComponent: FallbackComponent,
        showRetry = true,
        inline = true
      } = this.props;

      // If a custom fallback component is provided, use it
      if (FallbackComponent) {
        return (
          <FallbackComponent
            error={this.state.error}
            errorId={this.state.errorId}
            componentName={componentName}
            onRetry={this.handleRetry}
          />
        );
      }

      // Inline error display (default)
      if (inline) {
        return (
          <Alert
            message={`${componentName} Error`}
            description={
              <div>
                <div>This component encountered an error and couldn't render properly.</div>
                {this.state.errorId && (
                  <div style={{ marginTop: 8, fontSize: '12px', opacity: 0.7 }}>
                    Error ID: {this.state.errorId}
                  </div>
                )}
              </div>
            }
            type="error"
            showIcon
            icon={<ExclamationCircleOutlined />}
            action={
              showRetry && (
                <Button
                  size="small"
                  type="primary"
                  ghost
                  onClick={this.handleRetry}
                  icon={<ReloadOutlined />}
                >
                  Retry
                </Button>
              )
            }
            style={{ margin: '16px 0' }}
          />
        );
      }

      // Card-based error display
      return (
        <Card style={{ margin: '16px 0' }} bodyStyle={{ textAlign: 'center', padding: '24px' }}>
          <ExclamationCircleOutlined
            style={{ fontSize: '48px', color: '#ff4d4f', marginBottom: '16px' }}
          />
          <h3>{componentName} Error</h3>
          <p style={{ color: '#666', marginBottom: '16px' }}>
            This component encountered an error and couldn't render properly.
          </p>
          {this.state.errorId && (
            <p style={{ fontSize: '12px', color: '#999', marginBottom: '16px' }}>
              Error ID: {this.state.errorId}
            </p>
          )}
          {showRetry && (
            <Button type="primary" onClick={this.handleRetry} icon={<ReloadOutlined />}>
              Try Again
            </Button>
          )}
        </Card>
      );
    }

    return this.props.children;
  }
}

/**
 * Higher-order component for wrapping individual components with error boundary
 * @param {React.Component} Component - The component to wrap
 * @param {string} componentName - Name of the component for error context
 * @param {Object} options - Additional options (fallbackComponent, showRetry, inline, etc.)
 */
export const withComponentErrorBoundary = (Component, componentName, options = {}) => {
  const WrappedComponent = props => (
    <ComponentErrorBoundary componentName={componentName} {...options}>
      <Component {...props} />
    </ComponentErrorBoundary>
  );

  WrappedComponent.displayName = `withComponentErrorBoundary(${Component.displayName || Component.name})`;

  return WrappedComponent;
};

export default ComponentErrorBoundary;
