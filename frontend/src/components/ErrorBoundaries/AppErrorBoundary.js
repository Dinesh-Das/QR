import { BugOutlined, ReloadOutlined, HomeOutlined } from '@ant-design/icons';
import { Result, Button } from 'antd';
import React from 'react';

import errorReportingService, {
  ERROR_CATEGORY,
  ERROR_SEVERITY
} from '../../services/errorReporting';

/**
 * Application-level error boundary for catching critical application errors
 * This is the top-level error boundary that catches errors not handled by other boundaries
 */
export class AppErrorBoundary extends React.Component {
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
      errorId: `app_error_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    };
  }

  componentDidCatch(_error, errorInfo) {
    console.error('AppErrorBoundary caught an error:', _error, errorInfo);

    this.setState({ error: _error });

    // Report to monitoring service with application-level context
    this.reportError(_error, errorInfo, {
      level: 'application',
      critical: true,
      component: 'AppErrorBoundary'
    });
  }

  reportError = async (error, errorInfo, context) => {
    try {
      await errorReportingService.reportError(error, {
        category: ERROR_CATEGORY.APPLICATION,
        severity: ERROR_SEVERITY.CRITICAL,
        errorBoundary: 'AppErrorBoundary',
        componentStack: errorInfo.componentStack,
        errorId: this.state.errorId,
        ...context,
        props: this.props.errorContext || {}
      });
    } catch (reportingError) {
      console.error('Failed to report application error:', reportingError);
    }
  };

  handleRefreshPage = () => {
    window.location.reload();
  };

  handleGoHome = () => {
    window.location.href = '/qrmfg';
  };

  render() {
    if (this.state.hasError) {
      return (
        <div
          style={{
            padding: '50px 20px',
            minHeight: '100vh',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}
        >
          <Result
            status="500"
            icon={<BugOutlined style={{ color: '#ff4d4f' }} />}
            title="Application Error"
            subTitle={
              <div>
                <div>
                  Something went wrong with the application. Please refresh the page or contact
                  support.
                </div>
                {this.state.errorId && (
                  <div style={{ marginTop: 8, fontSize: '12px', color: '#666' }}>
                    Error ID: {this.state.errorId}
                  </div>
                )}
              </div>
            }
            extra={[
              <Button
                type="primary"
                key="refresh"
                onClick={this.handleRefreshPage}
                icon={<ReloadOutlined />}
                size="large"
              >
                Refresh Page
              </Button>,
              <Button key="home" onClick={this.handleGoHome} icon={<HomeOutlined />} size="large">
                Go Home
              </Button>
            ]}
          />
        </div>
      );
    }

    return this.props.children;
  }
}

export default AppErrorBoundary;
