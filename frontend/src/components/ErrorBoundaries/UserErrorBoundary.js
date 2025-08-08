import { Result, Button } from 'antd';
import PropTypes from 'prop-types';
import React from 'react';

class UserErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null, errorInfo: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    this.setState({
      error,
      errorInfo
    });

    // Report error to monitoring service
    this.reportError(error, errorInfo, { 
      level: 'component',
      component: 'UserManagement',
      context: this.props.context || 'user-management'
    });
  }

  reportError = (error, errorInfo, context) => {
    // In a real application, this would send to a monitoring service
    console.error('UserErrorBoundary caught an error:', {
      error: error.message,
      stack: error.stack,
      errorInfo,
      context,
      timestamp: new Date().toISOString()
    });

    // Example: Send to monitoring service
    // monitoringService.reportError({
    //   message: error.message,
    //   stack: error.stack,
    //   component: context.component,
    //   level: context.level,
    //   timestamp: new Date().toISOString()
    // });
  };

  handleRetry = () => {
    this.setState({ hasError: false, error: null, errorInfo: null });
    
    // Call retry callback if provided
    if (this.props.onRetry) {
      this.props.onRetry();
    }
  };

  handleGoHome = () => {
    window.location.href = '/qrmfg';
  };

  render() {
    if (this.state.hasError) {
      return (
        <Result
          status="error"
          title="User Management Error"
          subTitle="Something went wrong while managing users. Please try again or contact support if the problem persists."
          extra={[
            <Button type="primary" onClick={this.handleRetry} key="retry">
              Try Again
            </Button>,
            <Button onClick={this.handleGoHome} key="home">
              Go Home
            </Button>
          ]}
          style={{ padding: '50px 0' }}
        >
          {process.env.NODE_ENV === 'development' && (
            <div style={{ textAlign: 'left', marginTop: 20 }}>
              <details style={{ whiteSpace: 'pre-wrap' }}>
                <summary>Error Details (Development Only)</summary>
                <div style={{ marginTop: 10, fontSize: '12px', color: '#666' }}>
                  <strong>Error:</strong> {this.state.error?.message}
                  <br />
                  <strong>Stack:</strong>
                  <pre style={{ fontSize: '11px', marginTop: 5 }}>
                    {this.state.error?.stack}
                  </pre>
                  {this.state.errorInfo?.componentStack && (
                    <>
                      <strong>Component Stack:</strong>
                      <pre style={{ fontSize: '11px', marginTop: 5 }}>
                        {this.state.errorInfo.componentStack}
                      </pre>
                    </>
                  )}
                </div>
              </details>
            </div>
          )}
        </Result>
      );
    }

    return this.props.children;
  }
}

UserErrorBoundary.propTypes = {
  children: PropTypes.node.isRequired,
  context: PropTypes.string,
  onRetry: PropTypes.func
};

UserErrorBoundary.defaultProps = {
  context: 'user-management',
  onRetry: null
};

export default UserErrorBoundary;