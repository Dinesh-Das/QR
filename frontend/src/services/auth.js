import axios from 'axios';

import JWTValidator from './jwtValidator';
import SecureTokenStorage from './secureStorage';
import { securityMonitoring, SECURITY_EVENT_TYPES, SECURITY_SEVERITY } from './securityMonitoring';

// Configure axios base URL
const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || '/qrmfg/api/v1';
axios.defaults.baseURL = API_BASE_URL;

/**
 * Gets the current JWT token if valid, otherwise returns null
 * @returns {string|null} Valid JWT token or null
 */
export const getToken = () => {
  try {
    const token = SecureTokenStorage.getToken();
    if (!token) {
      return null;
    }

    // Validate token before returning
    if (!JWTValidator.validateToken(token)) {
      // Log security event for invalid token
      securityMonitoring.logAuthenticationEvent(
        SECURITY_EVENT_TYPES.TOKEN_EXPIRED,
        { reason: 'Token validation failed during retrieval' }
      );

      // Token is invalid, clean up and redirect
      handleInvalidToken();
      return null;
    }

    return token;
  } catch (error) {
    console.error('Error retrieving token:', error.message);

    // Log security event for token retrieval error
    securityMonitoring.logSecurityEvent(
      'TOKEN_RETRIEVAL_ERROR',
      { error: error.message },
      SECURITY_SEVERITY.MEDIUM
    );

    handleInvalidToken();
    return null;
  }
};

/**
 * Sets a JWT token after validation
 * @param {string} token - The JWT token to store
 * @throws {Error} If token is invalid
 */
export const setToken = token => {
  if (!token || typeof token !== 'string') {
    const error = new Error('Token must be a non-empty string');

    // Log security event for invalid token format
    securityMonitoring.logSecurityEvent(
      'INVALID_TOKEN_FORMAT',
      { tokenType: typeof token, tokenLength: token ? token.length : 0 },
      SECURITY_SEVERITY.HIGH
    );

    throw error;
  }

  // Validate token before storing
  if (!JWTValidator.validateToken(token)) {
    console.error('Token validation failed');

    securityMonitoring.logAuthenticationEvent(
      'INVALID_TOKEN_PROVIDED',
      {
        tokenLength: token.length
      }
    );

    throw new Error('Invalid JWT token provided');
  }

  try {
    SecureTokenStorage.setToken(token);

    // Log successful token storage
    const userInfo = JWTValidator.getUserInfo(token);
    securityMonitoring.logAuthenticationEvent(
      SECURITY_EVENT_TYPES.LOGIN_SUCCESS,
      {
        username: userInfo?.username,
        roles: userInfo?.roles,
        tokenExpiry: userInfo?.exp
      }
    );

  } catch (error) {
    console.error('Error storing token:', error.message);

    // Log security event for token storage failure
    securityMonitoring.logSecurityEvent(
      'TOKEN_STORAGE_FAILURE',
      { error: error.message },
      SECURITY_SEVERITY.HIGH
    );

    throw new Error('Failed to store authentication token');
  }
};

/**
 * Removes the current JWT token
 */
export const removeToken = () => {
  try {
    const currentUser = getCurrentUser();
    SecureTokenStorage.removeToken();

    // Log logout event
    securityMonitoring.logAuthenticationEvent(
      SECURITY_EVENT_TYPES.LOGOUT,
      { username: currentUser }
    );

  } catch (error) {
    console.error('Error removing token:', error.message);

    // Log security event for token removal error
    securityMonitoring.logSecurityEvent(
      'TOKEN_REMOVAL_ERROR',
      { error: error.message },
      SECURITY_SEVERITY.MEDIUM
    );
  }
};

/**
 * Checks if user is authenticated with a valid token
 * @returns {boolean} True if authenticated with valid token
 */
export const isAuthenticated = () => {
  const token = getToken();
  return !!token;
};

/**
 * Gets the refresh token if it exists
 * @returns {string|null} Refresh token or null
 */
export const getRefreshToken = () => {
  try {
    return SecureTokenStorage.getRefreshToken();
  } catch (error) {
    console.error('Error retrieving refresh token:', error.message);
    return null;
  }
};

/**
 * Sets the refresh token
 * @param {string} refreshToken - The refresh token to store
 */
export const setRefreshToken = refreshToken => {
  if (!refreshToken || typeof refreshToken !== 'string') {
    throw new Error('Refresh token must be a non-empty string');
  }

  try {
    SecureTokenStorage.setRefreshToken(refreshToken);
  } catch (error) {
    console.error('Error storing refresh token:', error.message);
    throw new Error('Failed to store refresh token');
  }
};

/**
 * Removes the refresh token
 */
export const removeRefreshToken = () => {
  try {
    SecureTokenStorage.removeRefreshToken();
  } catch (error) {
    console.error('Error removing refresh token:', error.message);
  }
};

/**
 * Handles invalid token by cleaning up and redirecting to login
 */
const handleInvalidToken = () => {
  try {
    const currentUser = getCurrentUser();

    // Log session timeout/invalid token event
    securityMonitoring.logAuthenticationEvent(
      SECURITY_EVENT_TYPES.SESSION_TIMEOUT,
      {
        username: currentUser,
        currentPath: window.location.pathname,
        reason: 'Invalid or expired token'
      }
    );

    SecureTokenStorage.clearAll();

    // Only redirect if not already on login page
    if (!window.location.pathname.includes('/login')) {
      window.location.href = '/qrmfg/login';
    }
  } catch (error) {
    console.error('Error handling invalid token:', error.message);

    // Log security event for token handling error
    securityMonitoring.logSecurityEvent(
      'TOKEN_HANDLING_ERROR',
      { error: error.message },
      SECURITY_SEVERITY.MEDIUM
    );
  }
};

/**
 * Refreshes the access token using the current token
 * @returns {string|null} New access token or null if refresh failed
 */
export const refreshAccessToken = async () => {
  const token = getToken();
  if (!token) {
    return null;
  }

  const currentUser = getCurrentUser();

  try {
    const response = await axios.post(
      '/auth/refresh',
      {},
      {
        headers: {
          Authorization: `Bearer ${token}`
        }
      }
    );

    if (response.data && response.data.token) {
      // Validate new token before storing
      if (JWTValidator.validateToken(response.data.token)) {
        setToken(response.data.token);

        // Log successful token refresh
        securityMonitoring.logAuthenticationEvent(
          SECURITY_EVENT_TYPES.TOKEN_REFRESH,
          { username: currentUser }
        );

        return response.data.token;
      } else {
        console.error('Received invalid token from refresh endpoint');

        // Log security event for invalid refreshed token
        securityMonitoring.logSecurityEvent(
          'INVALID_REFRESHED_TOKEN',
          { username: currentUser },
          SECURITY_SEVERITY.HIGH
        );

        handleInvalidToken();
        return null;
      }
    }
  } catch (err) {
    console.error('Token refresh failed:', err.message);

    // Log token refresh failure
    securityMonitoring.logAuthenticationEvent(
      'TOKEN_REFRESH_FAILED',
      {
        username: currentUser,
        error: err.message,
        statusCode: err.response?.status
      }
    );

    handleInvalidToken();
  }
  return null;
};

/**
 * Gets user roles from the current valid token
 * @returns {Array} Array of user roles
 */
export const getUserRoles = () => {
  const token = getToken();
  if (!token) {
    return [];
  }

  try {
    return JWTValidator.extractRoles(JWTValidator.getTokenPayload(token));
  } catch (error) {
    console.error('Error extracting user roles:', error.message);
    return [];
  }
};

/**
 * Gets the primary user role
 * @returns {string|null} Primary user role or null
 */
export const getUserRole = () => {
  const roles = getUserRoles();
  return roles && roles.length > 0 ? roles[0] : null;
};

/**
 * Gets the primary role type from backend role constants
 * @returns {string|null} Primary role type or null
 */
export const getPrimaryRoleType = () => {
  const token = getToken();
  if (!token) {
    return null;
  }

  try {
    const userInfo = JWTValidator.getUserInfo(token);
    return userInfo?.primaryRoleType || userInfo?.roleType || getUserRole();
  } catch (error) {
    console.error('Error getting primary role type:', error.message);
    return null;
  }
};

/**
 * Gets user's assigned plant codes
 * @returns {Array} Array of plant codes user has access to
 */
export const getUserPlantCodes = () => {
  const token = getToken();
  if (!token) {
    return [];
  }

  try {
    const userInfo = JWTValidator.getUserInfo(token);
    return userInfo?.plantCodes || userInfo?.assignedPlants || [];
  } catch (error) {
    console.error('Error getting user plant codes:', error.message);
    return [];
  }
};

/**
 * Gets user's primary plant code
 * @returns {string|null} Primary plant code or null
 */
export const getPrimaryPlantCode = () => {
  const token = getToken();
  if (!token) {
    return null;
  }

  try {
    const userInfo = JWTValidator.getUserInfo(token);
    return userInfo?.primaryPlant || userInfo?.primaryPlantCode || null;
  } catch (error) {
    console.error('Error getting primary plant code:', error.message);
    return null;
  }
};

/**
 * Checks if the current user is an admin
 * @returns {boolean} True if user is admin
 */
export const isAdmin = () => {
  const token = getToken();
  if (!token) {
    return false;
  }

  try {
    return JWTValidator.isAdmin(token);
  } catch (error) {
    console.error('Error checking admin status:', error.message);
    return false;
  }
};

/**
 * Gets the current user identifier
 * @returns {string|null} User identifier or null
 */
export const getCurrentUser = () => {
  const token = getToken();
  if (!token) {
    return null;
  }

  try {
    const userInfo = JWTValidator.getUserInfo(token);
    return userInfo ? userInfo.username : null;
  } catch (error) {
    console.error('Error getting current user:', error.message);
    return null;
  }
};

/**
 * Gets the current user's full information
 * @returns {object|null} User information object or null
 */
export const getCurrentUserInfo = () => {
  const token = getToken();
  if (!token) {
    return null;
  }

  try {
    return JWTValidator.getUserInfo(token);
  } catch (error) {
    console.error('Error getting current user info:', error.message);
    return null;
  }
};

/**
 * Gets the JWT payload (deprecated - use getCurrentUserInfo instead)
 * @deprecated Use getCurrentUserInfo() for safer access to user data
 * @returns {object|null} JWT payload or null
 */
export const getCurrentUserPayload = () => {
  const token = getToken();
  if (!token) {
    return null;
  }

  try {
    return JWTValidator.getTokenPayload(token);
  } catch (error) {
    console.error('Error getting user payload:', error.message);
    return null;
  }
};

/**
 * Checks if the current user has a specific role
 * @param {string} role - Role to check for
 * @returns {boolean} True if user has the role
 */
export const hasRole = role => {
  const token = getToken();
  if (!token || !role) {
    return false;
  }

  try {
    return JWTValidator.hasRole(token, role);
  } catch (error) {
    console.error('Error checking user role:', error.message);
    return false;
  }
};

/**
 * Role-based access control functions
 */
export const isJvcUser = () => {
  if (isAdmin()) return true; // Admins have access to all role-specific screens
  const primaryRole = getPrimaryRoleType();
  return hasRole('JVC_USER') || hasRole('JVC_ROLE') || primaryRole === 'JVC_ROLE';
};

export const isCqsUser = () => {
  if (isAdmin()) return true; // Admins have access to all role-specific screens
  const primaryRole = getPrimaryRoleType();
  return hasRole('CQS_USER') || hasRole('CQS_ROLE') || primaryRole === 'CQS_ROLE';
};

export const isTechUser = () => {
  if (isAdmin()) return true; // Admins have access to all role-specific screens
  const primaryRole = getPrimaryRoleType();
  return hasRole('TECH_USER') || hasRole('TECH_ROLE') || primaryRole === 'TECH_ROLE';
};

export const isPlantUser = () => {
  if (isAdmin()) return true; // Admins have access to all role-specific screens
  const primaryRole = getPrimaryRoleType();
  return hasRole('PLANT_USER') || hasRole('PLANT_ROLE') || primaryRole === 'PLANT_ROLE';
};

export const isViewer = () => {
  const primaryRole = getPrimaryRoleType();
  return hasRole('VIEWER') || hasRole('VIEWER_ROLE') || primaryRole === 'VIEWER_ROLE';
};

/**
 * Checks if user has access to a specific screen/route
 * @param {string} screenRoute - The route to check access for
 * @returns {boolean} True if user has access
 */
export const hasScreenAccess = screenRoute => {
  if (isAdmin()) {
    return true; // Admin has access to all screens
  }

  const primaryRole = getPrimaryRoleType();
  if (!primaryRole) {
    return false;
  }

  // Define screen access rules based on role
  const screenAccessMap = {
    '/qrmfg': true, // Home accessible to all authenticated users
    '/qrmfg/dashboard': true, // Dashboard accessible to all
    '/qrmfg/login': true, // Login accessible to all

    // Admin-only screens
    '/qrmfg/admin': isAdmin(),
    '/qrmfg/users': isAdmin(),
    '/qrmfg/roles': isAdmin(),
    '/qrmfg/sessions': isAdmin(),
    '/qrmfg/user-role-management': isAdmin(),
    '/qrmfg/auditlogs': isAdmin() || isTechUser(),

    // Role-specific screens
    '/qrmfg/jvc': isJvcUser() || isAdmin(),
    '/qrmfg/cqs': isCqsUser() || isAdmin(),
    '/qrmfg/tech': isTechUser() || isAdmin(),
    '/qrmfg/plant': isPlantUser() || isAdmin(),

    // Shared screens
    '/qrmfg/workflows': isJvcUser() || isCqsUser() || isTechUser() || isPlantUser() || isAdmin(),
    '/qrmfg/workflow-monitoring': isTechUser() || isAdmin(),
    '/qrmfg/reports': !isViewer() || isAdmin(),
    '/qrmfg/analytics': true, // Analytics accessible to all authenticated users
    '/qrmfg/qr-analytics': true, // QR Analytics accessible to all authenticated users
    '/qrmfg/settings': true // Settings accessible to all authenticated users
  };

  return screenAccessMap[screenRoute] || false;
};

/**
 * Gets list of accessible screens for current user
 * @returns {Array} Array of accessible screen routes
 */
export const getAccessibleScreens = () => {
  if (isAdmin()) {
    return [
      '/qrmfg',
      '/qrmfg/dashboard',
      '/qrmfg/admin',
      '/qrmfg/jvc',
      '/qrmfg/cqs',
      '/qrmfg/tech',
      '/qrmfg/plant',
      '/qrmfg/workflows',
      '/qrmfg/workflow-monitoring',
      '/qrmfg/reports',
      '/qrmfg/analytics',
      '/qrmfg/qr-analytics',
      '/qrmfg/users',
      '/qrmfg/roles',
      '/qrmfg/sessions',
      '/qrmfg/user-role-management',
      '/qrmfg/auditlogs',
      '/qrmfg/settings'
    ];
  }

  const accessibleScreens = ['/qrmfg', '/qrmfg/dashboard', '/qrmfg/analytics', '/qrmfg/qr-analytics', '/qrmfg/settings'];

  if (isJvcUser()) {
    accessibleScreens.push('/qrmfg/jvc', '/qrmfg/workflows');
  }

  if (isCqsUser()) {
    accessibleScreens.push('/qrmfg/cqs', '/qrmfg/workflows');
  }

  if (isTechUser()) {
    accessibleScreens.push('/qrmfg/tech', '/qrmfg/workflows', '/qrmfg/workflow-monitoring', '/qrmfg/auditlogs');
  }

  if (isPlantUser()) {
    accessibleScreens.push('/qrmfg/plant', '/qrmfg/workflows');
  }

  if (!isViewer()) {
    accessibleScreens.push('/qrmfg/reports');
  }

  return [...new Set(accessibleScreens)]; // Remove duplicates
};

/**
 * Checks if the token will expire within specified seconds
 * @param {number} seconds - Seconds to check against (default 300 = 5 minutes)
 * @returns {boolean} True if token expires soon
 */
export const willTokenExpireSoon = (seconds = 300) => {
  const token = getToken();
  if (!token) {
    return true; // Consider no token as "expiring soon"
  }

  try {
    return JWTValidator.willExpireWithin(token, seconds);
  } catch (error) {
    console.error('Error checking token expiry:', error.message);
    return true; // Assume expiring soon on error
  }
};

// Request interceptor to add authentication token
axios.interceptors.request.use(
  config => {
    // Don't add token to login and refresh endpoints
    if (!config.url.includes('/auth/login') && !config.url.includes('/auth/refresh')) {
      const token = getToken();
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  error => {
    console.error('Request interceptor error:', error.message);
    return Promise.reject(error);
  }
);

// Response interceptor to handle 401 errors and token refresh
axios.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config;

    if (error.response && error.response.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      // Check if this is a token expiration error
      const errorData = error.response.data;
      if (
        errorData &&
        (errorData.error === 'JWT token has expired' || errorData.message === 'Please login again')
      ) {
        console.warn('JWT token expired, attempting refresh...');

        // Try to refresh the token
        const newToken = await refreshAccessToken();
        if (newToken) {
          originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
          return axios(originalRequest);
        }
      } else {
        // Other 401 errors (invalid token, etc.)
        console.warn('Authentication failed, redirecting to login');
        handleInvalidToken();
      }
    }

    return Promise.reject(error);
  }
);
