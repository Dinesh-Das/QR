import { BugOutlined, ReloadOutlined, HomeOutlined } from '@ant-design/icons';
import { Result, Button, Alert, Collapse, Typography } from 'antd';
import React from 'react';

const { Panel } = Collapse;
const { Paragraph, Text } = Typography;

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null,
      errorId: null
    };
  }

  static getDerivedStateFromError(_error) {
    // Update state so the next render will show the fallback UI
    return {
      hasError: true,
      errorId: `error_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    };
  }

  componentDidCatch(_error, errorInfo) {
    // Log the error
    console.error('ErrorBoundary caught an error:', _error, errorInfo);

    this.setState({
      error: _error,
      errorInfo
    });

    // Report error to monitoring service if available
    this.reportError(_error, errorInfo);
  }

  reportError = (error, errorInfo) => {
    const errorReport = {
      id: this.state.errorId,
      message: error.message,
      stack: error.stack,
      componentStack: errorInfo.componentStack,
      timestamp: new Date().toISOString(),
      userAgent: navigator.userAgent,
      url: window.location.href,
      userId: this.getUserId(),
      props: this.props.errorContext || {}
    };

    // Send to error reporting service
    try {
      // This would typically send to a service like Sentry, LogRocket, etc.
      if (window.errorReporter) {
        window.errorReporter.captureException(error, {
          extra: errorReport,
          tags: {
            component: 'ErrorBoundary',
            errorId: this.state.errorId
          }
        });
      }

      // Also log to console in development
      if (process.env.NODE_ENV === 'development') {
        console.group('🐛 Error Boundary Report');
        console.error('Error:', error);
        console.error('Error Info:', errorInfo);
        console.error('Full Report:', errorReport);
        console.groupEnd();
      }
    } catch (reportingError) {
      console.error('Failed to report error:', reportingError);
    }
  };

  getUserId = () => {
    try {
      // Try to get user ID from auth context or localStorage
      const user = JSON.parse(localStorage.getItem('user') || '{}');
      return user.id || 'anonymous';
    } catch {
      return 'anonymous';
    }
  };

  handleRetry = () => {
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null,
      errorId: null
    });
  };

  handleGoHome = () => {
    window.location.href = '/';
  };

  handleReload = () => {
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      const {
        title = 'Something went wrong',
        subtitle = 'An unexpected error occurred. Our team has been notified.',
        showDetails = process.env.NODE_ENV === 'development',
        showRetry = true,
        showGoHome = true,
        showReload = true,
        customActions = null
      } = this.props;

      return (
        <div style={{ padding: '50px 20px', minHeight: '400px' }}>
          <Result
            status="error"
            icon={<BugOutlined />}
            title={title}
            subTitle={
              <div>
                <div>{subtitle}</div>
                {this.state.errorId && (
                  <div style={{ marginTop: 8 }}>
                    <Text type="secondary" style={{ fontSize: '12px' }}>
                      Error ID: {this.state.errorId}
                    </Text>
                  </div>
                )}
              </div>
            }
            extra={[
              showRetry && (
                <Button
                  type="primary"
                  key="retry"
                  onClick={this.handleRetry}
                  icon={<ReloadOutlined />}
                >
                  Try Again
                </Button>
              ),
              showGoHome && (
                <Button key="home" onClick={this.handleGoHome} icon={<HomeOutlined />}>
                  Go Home
                </Button>
              ),
              showReload && (
                <Button key="reload" onClick={this.handleReload} icon={<ReloadOutlined />}>
                  Reload Page
                </Button>
              ),
              ...(customActions || [])
            ].filter(Boolean)}
          />

          {showDetails && this.state.error && (
            <div style={{ maxWidth: '800px', margin: '0 auto', marginTop: '20px' }}>
              <Alert
                message="Error Details (Development Mode)"
                type="warning"
                showIcon
                style={{ marginBottom: '16px' }}
              />

              <Collapse>
                <Panel header="Error Information" key="error">
                  <Paragraph>
                    <Text strong>Error Message:</Text>
                    <br />
                    <Text code>{this.state.error.message}</Text>
                  </Paragraph>

                  <Paragraph>
                    <Text strong>Error Stack:</Text>
                    <br />
                    <pre
                      style={{
                        background: '#f5f5f5',
                        padding: '10px',
                        borderRadius: '4px',
                        fontSize: '12px',
                        overflow: 'auto',
                        maxHeight: '200px'
                      }}
                    >
                      {this.state.error.stack}
                    </pre>
                  </Paragraph>
                </Panel>

                <Panel header="Component Stack" key="component">
                  <pre
                    style={{
                      background: '#f5f5f5',
                      padding: '10px',
                      borderRadius: '4px',
                      fontSize: '12px',
                      overflow: 'auto',
                      maxHeight: '200px'
                    }}
                  >
                    {this.state.errorInfo?.componentStack}
                  </pre>
                </Panel>

                <Panel header="Context Information" key="context">
                  <Paragraph>
                    <Text strong>URL:</Text> {window.location.href}
                  </Paragraph>
                  <Paragraph>
                    <Text strong>Timestamp:</Text> {new Date().toISOString()}
                  </Paragraph>
                  <Paragraph>
                    <Text strong>User Agent:</Text> {navigator.userAgent}
                  </Paragraph>
                  {this.props.errorContext && (
                    <Paragraph>
                      <Text strong>Additional Context:</Text>
                      <pre
                        style={{
                          background: '#f5f5f5',
                          padding: '10px',
                          borderRadius: '4px',
                          fontSize: '12px'
                        }}
                      >
                        {JSON.stringify(this.props.errorContext, null, 2)}
                      </pre>
                    </Paragraph>
                  )}
                </Panel>
              </Collapse>
            </div>
          )}
        </div>
      );
    }

    return this.props.children;
  }
}

// Higher-order component for wrapping components with error boundary
export const withErrorBoundary = (Component, errorBoundaryProps = {}) => {
  const WrappedComponent = props => (
    <ErrorBoundary {...errorBoundaryProps}>
      <Component {...props} />
    </ErrorBoundary>
  );

  WrappedComponent.displayName = `withErrorBoundary(${Component.displayName || Component.name})`;

  return WrappedComponent;
};

// Specialized error boundaries for different parts of the application
export const WorkflowErrorBoundary = ({ children, ...props }) => (
  <ErrorBoundary
    title="Workflow Error"
    subtitle="There was an issue with the workflow system. Please try again or contact support."
    errorContext={{ section: 'workflow' }}
    {...props}
  >
    {children}
  </ErrorBoundary>
);

export const QueryErrorBoundary = ({ children, ...props }) => (
  <ErrorBoundary
    title="Query Error"
    subtitle="There was an issue with the query system. Please try again or contact support."
    errorContext={{ section: 'query' }}
    {...props}
  >
    {children}
  </ErrorBoundary>
);

export const DashboardErrorBoundary = ({ children, ...props }) => (
  <ErrorBoundary
    title="Dashboard Error"
    subtitle="There was an issue loading the dashboard. Please refresh the page."
    errorContext={{ section: 'dashboard' }}
    {...props}
  >
    {children}
  </ErrorBoundary>
);

export const FormErrorBoundary = ({ children, ...props }) => (
  <ErrorBoundary
    title="Form Error"
    subtitle="There was an issue with the form. Please try again."
    errorContext={{ section: 'form' }}
    showGoHome={false}
    {...props}
  >
    {children}
  </ErrorBoundary>
);

export default ErrorBoundary;
