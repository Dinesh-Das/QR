import { ExclamationCircleOutlined, HomeOutlined, ReloadOutlined } from '@ant-design/icons';
import { Result, Button } from 'antd';
import React from 'react';

import errorReportingService, {
  ERROR_CATEGORY,
  ERROR_SEVERITY
} from '../../services/errorReporting';

/**
 * Route-specific error boundary for handling errors within specific routes
 * Provides route context and appropriate recovery options
 */
export class RouteErrorBoundary extends React.Component {
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
      errorId: `route_error_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    };
  }

  componentDidCatch(_error, errorInfo) {
    console.error(
      `RouteErrorBoundary (${this.props.routeName}) caught an error:`,
      _error,
      errorInfo
    );

    this.setState({ error: _error });

    // Report to monitoring service with route-specific context
    this.reportError(_error, errorInfo, {
      level: 'route',
      routeName: this.props.routeName,
      component: 'RouteErrorBoundary'
    });
  }

  reportError = async (error, errorInfo, context) => {
    try {
      await errorReportingService.reportError(error, {
        category: ERROR_CATEGORY.ROUTE,
        severity: ERROR_SEVERITY.HIGH,
        errorBoundary: 'RouteErrorBoundary',
        componentStack: errorInfo.componentStack,
        errorId: this.state.errorId,
        routeName: context.routeName,
        ...context,
        props: this.props.errorContext || {}
      });
    } catch (reportingError) {
      console.error('Failed to report route error:', reportingError);
    }
  };

  handleGoHome = () => {
    window.location.href = '/qrmfg';
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
      const { routeName = 'Page' } = this.props;

      return (
        <div
          style={{
            padding: '50px 20px',
            minHeight: '400px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}
        >
          <Result
            status="error"
            icon={<ExclamationCircleOutlined style={{ color: '#faad14' }} />}
            title={`${routeName} Error`}
            subTitle={
              <div>
                <div>
                  There was an issue loading this page. Please try again or go back to the home
                  page.
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
                key="retry"
                onClick={this.handleRetry}
                icon={<ReloadOutlined />}
              >
                Try Again
              </Button>,
              <Button key="home" onClick={this.handleGoHome} icon={<HomeOutlined />}>
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

/**
 * Higher-order component for wrapping routes with error boundary
 * @param {React.Component} Component - The route component to wrap
 * @param {string} routeName - Name of the route for error context
 * @param {Object} errorContext - Additional error context
 */
export const withRouteErrorBoundary = (Component, routeName, errorContext = {}) => {
  const WrappedComponent = props => (
    <RouteErrorBoundary routeName={routeName} errorContext={errorContext}>
      <Component {...props} />
    </RouteErrorBoundary>
  );

  WrappedComponent.displayName = `withRouteErrorBoundary(${Component.displayName || Component.name})`;

  return WrappedComponent;
};

export default RouteErrorBoundary;
