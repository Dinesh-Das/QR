import { useCallback, useEffect, useState } from 'react';

import apiClient from '../api/client';

/**
 * Custom hook for API cache management
 * Provides easy access to cache statistics and control functions
 */
export const useApiCache = () => {
  const [cacheStats, setCacheStats] = useState(null);
  const [isLoading, setIsLoading] = useState(false);

  /**
   * Load current cache statistics
   */
  const loadCacheStats = useCallback(async () => {
    setIsLoading(true);
    try {
      const stats = apiClient.getCacheStats();
      setCacheStats(stats);
    } catch (error) {
      console.error('Failed to load cache stats:', error);
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Clear all cache entries
   */
  const clearCache = useCallback(() => {
    apiClient.clearCache();
    loadCacheStats(); // Refresh stats after clearing
  }, [loadCacheStats]);

  /**
   * Invalidate cache by pattern
   */
  const invalidateByPattern = useCallback((pattern) => {
    const invalidated = apiClient.invalidateCache(pattern);
    loadCacheStats(); // Refresh stats after invalidation
    return invalidated;
  }, [loadCacheStats]);

  /**
   * Invalidate cache by URL prefix
   */
  const invalidateByPrefix = useCallback((prefix) => {
    const invalidated = apiClient.invalidateCacheByPrefix(prefix);
    loadCacheStats(); // Refresh stats after invalidation
    return invalidated;
  }, [loadCacheStats]);

  /**
   * Get detailed cache information
   */
  const getCacheInfo = useCallback(() => {
    return apiClient.getCacheInfo();
  }, []);

  /**
   * Check if cache is performing well
   */
  const isCacheHealthy = useCallback(() => {
    if (!cacheStats) return null;
    
    const { hitRate, totalRequests } = cacheStats;
    
    // Consider cache healthy if hit rate > 30% and we have enough requests
    return totalRequests > 10 && hitRate > 30;
  }, [cacheStats]);

  /**
   * Get cache performance metrics
   */
  const getPerformanceMetrics = useCallback(() => {
    if (!cacheStats) return null;
    
    const { hitRate, totalRequests, memoryUsage, cacheSize } = cacheStats;
    
    return {
      efficiency: hitRate > 50 ? 'excellent' : hitRate > 30 ? 'good' : hitRate > 10 ? 'fair' : 'poor',
      memoryEfficiency: memoryUsage?.mb < 5 ? 'excellent' : memoryUsage?.mb < 10 ? 'good' : 'fair',
      utilization: cacheSize > 0 ? 'active' : 'unused',
      recommendations: getCacheRecommendations(hitRate, totalRequests, memoryUsage?.mb || 0)
    };
  }, [cacheStats]);

  /**
   * Get cache optimization recommendations
   */
  const getCacheRecommendations = useCallback((hitRate, totalRequests, memoryMB) => {
    const recommendations = [];
    
    if (hitRate < 30 && totalRequests > 20) {
      recommendations.push('Consider increasing cache TTL for frequently accessed data');
    }
    
    if (memoryMB > 10) {
      recommendations.push('Cache memory usage is high, consider reducing TTL or clearing old entries');
    }
    
    if (totalRequests < 10) {
      recommendations.push('Not enough requests to evaluate cache performance');
    }
    
    if (hitRate > 70) {
      recommendations.push('Cache is performing excellently!');
    }
    
    return recommendations;
  }, []);

  // Load cache stats on mount
  useEffect(() => {
    loadCacheStats();
    
    // Auto-refresh stats every 30 seconds
    const interval = setInterval(loadCacheStats, 30000);
    
    return () => clearInterval(interval);
  }, [loadCacheStats]);

  return {
    cacheStats,
    isLoading,
    loadCacheStats,
    clearCache,
    invalidateByPattern,
    invalidateByPrefix,
    getCacheInfo,
    isCacheHealthy,
    getPerformanceMetrics,
    
    // Convenience methods for common cache operations
    invalidateUsers: () => invalidateByPattern('.*users.*'),
    invalidateWorkflows: () => invalidateByPattern('.*workflows.*'),
    invalidatePlants: () => invalidateByPattern('.*plants.*'),
    invalidateDashboard: () => invalidateByPattern('.*dashboard.*'),
  };
};

/**
 * Hook for making cached API requests with automatic cache management
 */
export const useCachedApi = (url, options = {}) => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const {
    dependencies = [],
    cacheTTL,
    useCache = true,
    onSuccess,
    onError
  } = options;

  /**
   * Fetch data with caching
   */
  const fetchData = useCallback(async (signal) => {
    if (!url) return;

    setLoading(true);
    setError(null);

    try {
      const response = await apiClient.get(url, {
        useCache,
        cacheTTL,
        signal
      });

      setData(response);
      
      if (onSuccess) {
        onSuccess(response);
      }
    } catch (err) {
      if (!signal?.aborted) {
        setError(err);
        
        if (onError) {
          onError(err);
        }
      }
    } finally {
      if (!signal?.aborted) {
        setLoading(false);
      }
    }
  }, [url, useCache, cacheTTL, onSuccess, onError]);

  /**
   * Refresh data (bypass cache)
   */
  const refresh = useCallback(async () => {
    const controller = new AbortController();
    
    try {
      const response = await apiClient.get(url, {
        useCache: false,
        signal: controller.signal
      });

      setData(response);
      
      // Update cache with fresh data
      if (useCache) {
        const cacheKey = apiClient.generateKey ? 
          apiClient.generateKey(url, { method: 'GET' }) : 
          `GET|${url}`;
        
        // This would require exposing the cache instance
        // For now, the next request will cache the fresh data
      }
      
      if (onSuccess) {
        onSuccess(response);
      }
    } catch (err) {
      if (!controller.signal.aborted) {
        setError(err);
        
        if (onError) {
          onError(err);
        }
      }
    }
  }, [url, useCache, onSuccess, onError]);

  // Fetch data when dependencies change
  useEffect(() => {
    const controller = new AbortController();
    fetchData(controller.signal);
    
    return () => {
      controller.abort();
    };
  }, [fetchData, ...dependencies]);

  return {
    data,
    loading,
    error,
    refresh,
    refetch: fetchData
  };
};

export default useApiCache;