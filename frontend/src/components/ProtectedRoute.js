import { Result, Button } from 'antd';
import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';


import { isAuthenticated, hasScreenAccess } from '../services/auth';

const ProtectedRoute = ({ children, requiredRole = null, fallbackPath = '/qrmfg' }) => {
  const location = useLocation();

  // Check if user is authenticated
  if (!isAuthenticated()) {
    return <Navigate to="/qrmfg/login" replace state={{ from: location }} />;
  }

  // Check role-based access if required
  if (requiredRole || location.pathname !== '/qrmfg/login') {
    const hasAccess = hasScreenAccess(location.pathname);
    
    if (!hasAccess) {
      return (
        <Result
          status="403"
          title="403"
          subTitle="Sorry, you are not authorized to access this page."
          extra={
            <Button type="primary" onClick={() => window.location.href = fallbackPath}>
              Back Home
            </Button>
          }
        />
      );
    }
  }

  return children;
};

export default ProtectedRoute;
