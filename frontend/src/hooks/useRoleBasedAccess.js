import { useState, useEffect, useCallback } from 'react';

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
  getUserPlantCodes,
  getPrimaryPlantCode,
  getCurrentUser
} from '../services/auth';
import RBACService from '../services/rbacService';

/**
 * Custom hook for role-based access control
 * Provides reactive access to user roles and permissions
 */
export const useRoleBasedAccess = () => {
  const [accessSummary, setAccessSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Load user access summary
  const loadAccessSummary = useCallback(async () => {
    if (!isAuthenticated()) {
      setAccessSummary(null);
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      
      const summary = await RBACService.getUserAccessSummary();
      setAccessSummary(summary);
    } catch (err) {
      console.warn('Failed to load access summary, using fallback:', err);
      setError(err);
      
      // Use fallback data
      const fallbackSummary = RBACService.getFallbackAccessSummary();
      setAccessSummary(fallbackSummary);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadAccessSummary();
  }, [loadAccessSummary]);

  // Check screen access
  const checkScreenAccess = useCallback(async (screenRoute) => {
    try {
      return await RBACService.hasScreenAccess(screenRoute);
    } catch (error) {
      console.warn('Screen access check failed, using fallback:', error);
      return RBACService.getFallbackScreenAccess(screenRoute);
    }
  }, []);

  // Check data access
  const checkDataAccess = useCallback(async (dataType, context = {}) => {
    try {
      return await RBACService.hasDataAccess(dataType, context);
    } catch (error) {
      console.warn('Data access check failed, using fallback:', error);
      return RBACService.getFallbackDataAccess(dataType, context);
    }
  }, []);

  // Check plant data access
  const checkPlantDataAccess = useCallback(async (dataType, plantCode, context = {}) => {
    try {
      return await RBACService.hasPlantDataAccess(dataType, plantCode, context);
    } catch (error) {
      console.warn('Plant data access check failed, using fallback:', error);
      return RBACService.getFallbackPlantDataAccess(dataType, plantCode, context);
    }
  }, []);

  // Make access decision
  const makeAccessDecision = useCallback(async (resourceType, resourceId, action, context = {}) => {
    try {
      return await RBACService.makeAccessDecision(resourceType, resourceId, action, context);
    } catch (error) {
      console.warn('Access decision failed, using fallback:', error);
      return RBACService.getFallbackAccessDecision(resourceType, resourceId, action, context);
    }
  }, []);

  // Filter data by plant access
  const filterByPlantAccess = useCallback((data, plantExtractor) => {
    return RBACService.filterDataByPlantAccess(data, plantExtractor);
  }, []);

  return {
    // Loading states
    loading,
    error,
    
    // User info
    isAuthenticated: isAuthenticated(),
    currentUser: getCurrentUser(),
    primaryRole: getPrimaryRoleType(),
    userPlants: getUserPlantCodes(),
    primaryPlant: getPrimaryPlantCode(),
    
    // Role checks
    isAdmin: isAdmin(),
    isJvcUser: isJvcUser(),
    isCqsUser: isCqsUser(),
    isTechUser: isTechUser(),
    isPlantUser: isPlantUser(),
    isViewer: isViewer(),
    
    // Access summary
    accessSummary,
    
    // Helper functions
    hasRole: (role) => hasRole(role),
    hasAnyRole: (roles) => roles.some(role => hasRole(role)),
    hasAllRoles: (roles) => roles.every(role => hasRole(role)),
    
    // Async access checks
    checkScreenAccess,
    checkDataAccess,
    checkPlantDataAccess,
    makeAccessDecision,
    
    // Data filtering
    filterByPlantAccess,
    
    // Refresh function
    refresh: loadAccessSummary
  };
};

/**
 * Hook for checking specific screen access
 */
export const useScreenAccess = (screenRoute) => {
  const [hasAccess, setHasAccess] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const checkAccess = async () => {
      if (!isAuthenticated() || !screenRoute) {
        setHasAccess(false);
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError(null);
        
        const access = await RBACService.hasScreenAccess(screenRoute);
        setHasAccess(access);
      } catch (err) {
        console.warn('Screen access check failed:', err);
        setError(err);
        
        // Use fallback
        const fallbackAccess = RBACService.getFallbackScreenAccess(screenRoute);
        setHasAccess(fallbackAccess);
      } finally {
        setLoading(false);
      }
    };

    checkAccess();
  }, [screenRoute]);

  return { hasAccess, loading, error };
};

/**
 * Hook for checking data access
 */
export const useDataAccess = (dataType, context = {}) => {
  const [hasAccess, setHasAccess] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const checkAccess = async () => {
      if (!isAuthenticated() || !dataType) {
        setHasAccess(false);
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError(null);
        
        const access = await RBACService.hasDataAccess(dataType, context);
        setHasAccess(access);
      } catch (err) {
        console.warn('Data access check failed:', err);
        setError(err);
        
        // Use fallback
        const fallbackAccess = RBACService.getFallbackDataAccess(dataType, context);
        setHasAccess(fallbackAccess);
      } finally {
        setLoading(false);
      }
    };

    checkAccess();
  }, [dataType, context]);

  return { hasAccess, loading, error };
};

/**
 * Hook for plant-specific data filtering
 */
export const usePlantDataFilter = (data, plantExtractor) => {
  const [filteredData, setFilteredData] = useState([]);

  useEffect(() => {
    if (!Array.isArray(data)) {
      setFilteredData([]);
      return;
    }

    const filtered = RBACService.filterDataByPlantAccess(data, plantExtractor);
    setFilteredData(filtered);
  }, [data, plantExtractor]);

  return filteredData;
};

export default useRoleBasedAccess;