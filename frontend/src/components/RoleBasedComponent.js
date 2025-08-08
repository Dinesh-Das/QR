import { Result, Spin } from 'antd';
import React from 'react';


import { 
  isAuthenticated, 
  isAdmin, 
  isJvcUser, 
  isCqsUser, 
  isTechUser, 
  isPlantUser, 
  isViewer,
  hasRole,
  getPrimaryRoleType,
  getCurrentUser,
  getToken
} from '../services/auth';
import JWTValidator from '../services/jwtValidator';

/**
 * Role-based component wrapper that conditionally renders content based on user roles
 */
const RoleBasedComponent = ({ 
  children, 
  roles = [], 
  requireAll = false, 
  fallback = null, 
  loading = false,
  showFallback = true 
}) => {
  
  if (loading) {
    return <Spin size="large" />;
  }

  if (!isAuthenticated()) {
    return showFallback ? (
      <Result
        status="warning"
        title="Authentication Required"
        subTitle="Please log in to access this content."
      />
    ) : null;
  }

  // If no roles specified, show content to all authenticated users
  if (!roles || roles.length === 0) {
    return children;
  }

  // Check role access - admins have access to everything
  const adminAccess = isAdmin();
  const roleAccess = requireAll 
    ? roles.every(role => hasRole(role))
    : roles.some(role => hasRole(role));
  
  const hasAccess = adminAccess || roleAccess;

  if (!hasAccess) {
    if (fallback) {
      return fallback;
    }
    
    return showFallback ? (
      <Result
        status="403"
        title="Access Denied"
        subTitle={`This content requires one of the following roles: ${roles.join(', ')} or ADMIN privileges`}
      />
    ) : null;
  }

  return children;
};

/**
 * Convenience components for specific roles
 */
export const AdminOnly = ({ children, fallback = null, showFallback = true }) => {
  if (!isAuthenticated()) {
    return showFallback ? (
      <Result
        status="warning"
        title="Authentication Required"
        subTitle="Please log in to access this content."
      />
    ) : null;
  }

  // Direct check using multiple methods
  const currentUser = getCurrentUser();
  const adminStatus = isAdmin();
  const hasAdminRole = hasRole('ADMIN');
  const hasAdminRoleLower = hasRole('admin');
  const hasRoleAdmin = hasRole('ROLE_ADMIN');
  
  // Also check the JWT token directly
  const token = getToken();
  let tokenIsAdmin = false;
  let tokenHasAdminRole = false;
  
  if (token) {
    try {
      const payload = JWTValidator.getTokenPayload(token);
      tokenIsAdmin = payload?.isAdmin === true;
      tokenHasAdminRole = payload?.roles?.includes('ADMIN') || payload?.authorities?.includes('ADMIN');
    } catch (error) {
      console.error('Error checking token in AdminOnly:', error);
    }
  }
  
  const hasAccess = adminStatus || hasAdminRole || hasAdminRoleLower || hasRoleAdmin || tokenIsAdmin || tokenHasAdminRole;
  
  // Enhanced debugging
  console.log('AdminOnly Enhanced Debug:', {
    currentUser,
    adminStatus,
    hasAdminRole,
    hasAdminRoleLower,
    hasRoleAdmin,
    tokenIsAdmin,
    tokenHasAdminRole,
    finalHasAccess: hasAccess,
    token: token ? 'present' : 'missing'
  });

  if (!hasAccess) {
    if (fallback) {
      return fallback;
    }
    
    return showFallback ? (
      <Result
        status="403"
        title="Access Denied"
        subTitle={`This content requires administrator privileges. Debug: user=${currentUser}, isAdmin=${adminStatus}, hasRole=${hasAdminRole}, tokenAdmin=${tokenIsAdmin}`}
      />
    ) : null;
  }

  return children;
};

export const JvcOnly = ({ children, fallback = null, showFallback = true }) => (
  <RoleBasedComponent 
    roles={['JVC_USER', 'JVC_ROLE', 'ADMIN']} 
    fallback={fallback} 
    showFallback={showFallback}
  >
    {children}
  </RoleBasedComponent>
);

export const CqsOnly = ({ children, fallback = null, showFallback = true }) => (
  <RoleBasedComponent 
    roles={['CQS_USER', 'CQS_ROLE', 'ADMIN']} 
    fallback={fallback} 
    showFallback={showFallback}
  >
    {children}
  </RoleBasedComponent>
);

export const TechOnly = ({ children, fallback = null, showFallback = true }) => (
  <RoleBasedComponent 
    roles={['TECH_USER', 'TECH_ROLE', 'ADMIN']} 
    fallback={fallback} 
    showFallback={showFallback}
  >
    {children}
  </RoleBasedComponent>
);

export const PlantOnly = ({ children, fallback = null, showFallback = true }) => (
  <RoleBasedComponent 
    roles={['PLANT_USER', 'PLANT_ROLE', 'ADMIN']} 
    fallback={fallback} 
    showFallback={showFallback}
  >
    {children}
  </RoleBasedComponent>
);

export const NonViewerOnly = ({ children, fallback = null, showFallback = true }) => {
  if (!isAuthenticated()) {
    return showFallback ? (
      <Result
        status="warning"
        title="Authentication Required"
        subTitle="Please log in to access this content."
      />
    ) : null;
  }

  if (isViewer() && !isAdmin()) {
    return fallback || (showFallback ? (
      <Result
        status="403"
        title="Access Denied"
        subTitle="Viewers cannot access this content."
      />
    ) : null);
  }

  return children;
};

/**
 * Hook for role-based conditional rendering
 */
export const useRoleAccess = () => {
  return {
    isAuthenticated: isAuthenticated(),
    isAdmin: isAdmin(),
    isJvcUser: isJvcUser(),
    isCqsUser: isCqsUser(),
    isTechUser: isTechUser(),
    isPlantUser: isPlantUser(),
    isViewer: isViewer(),
    primaryRole: getPrimaryRoleType(),
    hasRole: (role) => hasRole(role),
    hasAnyRole: (roles) => roles.some(role => hasRole(role)),
    hasAllRoles: (roles) => roles.every(role => hasRole(role))
  };
};

export default RoleBasedComponent;