import axios from 'axios';

import { apiCache, CACHE_CONFIGS } from '../services/apiCache';
import JWTValidator from '../services/jwtValidator';
import SecureTokenStorage from '../services/secureStorage';
import { securityMonitoring, SECURITY_EVENT_TYPES, SECURITY_SEVERITY } from '../services/securityMonitoring';

/**
 * Unified API Client with interceptors and standardized error handling
 * Replaces duplicate fetch and axios implementations with a single, secure client
 */
class APIClient {
  constructor() {
    this.client = axios.create({
      baseURL: process.env.REACT_APP_API_BASE_URL || '/qrmfg/api/v1',
      timeout: 30000, // 30 seconds as required
      headers: {
        'Content-Type': 'application/json'
      }
    });

    this.setupInterceptors();
  }

  /**
   * Sets up request and response interceptors
   */
  setupInterceptors() {
    // Request interceptor - automatically add Bearer token from SecureTokenStorage
    this.client.interceptors.request.use(
      config => {
        const token = SecureTokenStorage.getToken();
        if (token && JWTValidator.validateToken(token)) {
          config.headers.Authorization = `Bearer ${token}`;
        }

        // Log request in development (without sensitive data)
        if (process.env.NODE_ENV === 'development') {
          console.log(`[API] ${config.method?.toUpperCase()} ${config.url}`);
        }

        // Track API request start time for performance monitoring
        config.metadata = { startTime: Date.now() };

        return config;
      },
      error => {
        console.error('[API] Request interceptor error:', error);

        // Log security event for request interceptor error
        securityMonitoring.logSecurityEvent(
          'API_REQUEST_INTERCEPTOR_ERROR',
          { error: error.message },
          SECURITY_SEVERITY.MEDIUM
        );

        return Promise.reject(this.createStandardizedError(error));
      }
    );

    // Response interceptor - standardized error handling
    this.client.interceptors.response.use(
      response => {
        // Calculate request duration for performance monitoring
        const duration = response.config.metadata?.startTime ?
          Date.now() - response.config.metadata.startTime : 0;

        // Log successful response in development
        if (process.env.NODE_ENV === 'development') {
          console.log(
            `[API] Response ${response.status} for ${response.config.method?.toUpperCase()} ${response.config.url} (${duration}ms)`
          );
        }

        // Log API performance if request took too long
        if (duration > 5000) { // 5 seconds threshold
          securityMonitoring.logSecurityEvent(
            'SLOW_API_RESPONSE',
            {
              endpoint: response.config.url,
              method: response.config.method?.toUpperCase(),
              duration,
              status: response.status
            },
            SECURITY_SEVERITY.LOW
          );
        }

        // Return response data directly for easier consumption
        return response.data;
      },
      error => {
        // Calculate request duration for error monitoring
        const duration = error.config?.metadata?.startTime ?
          Date.now() - error.config.metadata.startTime : 0;

        // Log API security event for errors
        securityMonitoring.logApiSecurityEvent(
          error.config?.url || 'unknown',
          error.config?.method?.toUpperCase() || 'unknown',
          error.response?.status || 0,
          { duration, errorMessage: error.message }
        );

        return Promise.reject(this.handleError(error));
      }
    );
  }

  /**
   * Handles API errors with automatic 401 handling and retry logic
   * @param {Error} error - The axios error object
   * @returns {Object} Standardized error object
   */
  handleError(error) {
    // Handle 401 Unauthorized - automatic token cleanup and redirect
    if (error.response?.status === 401) {
      console.warn('[API] 401 Unauthorized - clearing tokens and redirecting to login');

      // Log unauthorized access attempt
      securityMonitoring.logAuthenticationEvent(
        SECURITY_EVENT_TYPES.UNAUTHORIZED_API_ACCESS,
        {
          endpoint: error.config?.url,
          method: error.config?.method?.toUpperCase(),
          userAgent: navigator.userAgent
        }
      );

      SecureTokenStorage.clearAll();

      // Only redirect if not already on login page
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/qrmfg/login';
      }

      return this.createStandardizedError(error, 'Authentication required. Please log in again.');
    }

    // Handle 403 Forbidden - access denied
    if (error.response?.status === 403) {
      securityMonitoring.logAuthenticationEvent(
        SECURITY_EVENT_TYPES.ACCESS_DENIED,
        {
          endpoint: error.config?.url,
          method: error.config?.method?.toUpperCase(),
          userAgent: navigator.userAgent
        }
      );
    }

    // Handle 429 Rate Limiting
    if (error.response?.status === 429) {
      securityMonitoring.logSecurityEvent(
        SECURITY_EVENT_TYPES.RATE_LIMIT_EXCEEDED,
        {
          endpoint: error.config?.url,
          method: error.config?.method?.toUpperCase(),
          retryAfter: error.response?.headers?.['retry-after']
        },
        SECURITY_SEVERITY.HIGH
      );
    }

    // Check if this is a retryable network error (not 4xx/5xx errors)
    if (this.isRetryableError(error)) {
      // Note: Retry logic would be implemented at the service layer
      // to avoid infinite loops in interceptors
      console.warn('[API] Network error detected:', error.message);

      // Log network errors for monitoring
      securityMonitoring.logSecurityEvent(
        'NETWORK_ERROR',
        {
          endpoint: error.config?.url,
          method: error.config?.method?.toUpperCase(),
          errorCode: error.code,
          errorMessage: error.message
        },
        SECURITY_SEVERITY.LOW
      );
    }

    // Log error details in development
    if (process.env.NODE_ENV === 'development') {
      console.error('[API] Response error:', {
        url: error.config?.url,
        method: error.config?.method,
        status: error.response?.status,
        message: error.message,
        data: error.response?.data
      });
    }

    return this.createStandardizedError(error);
  }

  /**
   * Creates a standardized error format
   * @param {Error} error - The original error
   * @param {string} customMessage - Optional custom message
   * @returns {Object} Standardized error object
   */
  createStandardizedError(error, customMessage = null) {
    const standardizedError = {
      message:
        customMessage ||
        error.response?.data?.message ||
        error.message ||
        'An unexpected error occurred',
      status: error.response?.status || null,
      code: error.response?.data?.code || error.code || 'UNKNOWN_ERROR',
      timestamp: new Date().toISOString(),
      type: this.getErrorType(error)
    };

    // Add additional context in development
    if (process.env.NODE_ENV === 'development') {
      standardizedError.originalError = error.message;
      standardizedError.url = error.config?.url;
      standardizedError.method = error.config?.method;
    }

    return standardizedError;
  }

  /**
   * Determines the error type for categorization
   * @param {Error} error - The error object
   * @returns {string} Error type
   */
  getErrorType(error) {
    if (error.response) {
      const status = error.response.status;
      if (status === 401) {
        return 'AUTHENTICATION';
      }
      if (status === 403) {
        return 'AUTHORIZATION';
      }
      if (status >= 400 && status < 500) {
        return 'CLIENT_ERROR';
      }
      if (status >= 500) {
        return 'SERVER_ERROR';
      }
    }

    if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
      return 'TIMEOUT';
    }

    if (error.code === 'ERR_NETWORK' || error.message.includes('Network Error')) {
      return 'NETWORK';
    }

    return 'UNKNOWN';
  }

  /**
   * Checks if an error is retryable (network errors, not 4xx/5xx)
   * @param {Error} error - The error object
   * @returns {boolean} True if error is retryable
   */
  isRetryableError(error) {
    // Don't retry 4xx/5xx HTTP errors
    if (error.response && error.response.status >= 400) {
      return false;
    }

    // Retry network errors, timeouts, and connection issues
    return (
      error.code === 'ERR_NETWORK' ||
      error.code === 'ECONNABORTED' ||
      error.code === 'ENOTFOUND' ||
      error.code === 'ECONNRESET' ||
      error.message.includes('Network Error') ||
      error.message.includes('timeout')
    );
  }

  /**
   * Implements retry logic for network errors
   * @param {Function} requestFn - Function that makes the request
   * @param {number} maxRetries - Maximum number of retries
   * @param {number} baseDelay - Base delay in milliseconds
   * @returns {Promise} Promise that resolves to response data
   */
  async withRetry(requestFn, maxRetries = 3, baseDelay = 1000) {
    let lastError;

    for (let attempt = 0; attempt <= maxRetries; attempt++) {
      try {
        return await requestFn();
      } catch (error) {
        lastError = error;

        // Don't retry if it's not a retryable error or if it's the last attempt
        if (!this.isRetryableError(error) || attempt === maxRetries) {
          throw error;
        }

        // Exponential backoff delay
        const delay = baseDelay * Math.pow(2, attempt);
        console.warn(`[API] Retry attempt ${attempt + 1}/${maxRetries} after ${delay}ms delay`);

        await new Promise(resolve => setTimeout(resolve, delay));
      }
    }

    throw lastError;
  }

  // HTTP method wrappers for consistent usage

  /**
   * GET request with caching support
   * @param {string} url - Request URL
   * @param {Object} config - Axios config options
   * @returns {Promise} Promise that resolves to response data
   */
  async get(url, config = {}) {
    const { useCache = true, cacheTTL, ...axiosConfig } = config;

    // Skip caching if explicitly disabled or if using AbortController
    if (!useCache || axiosConfig.signal) {
      return this.client.get(url, axiosConfig);
    }

    // Generate cache key
    const cacheKey = apiCache.generateKey(url, { method: 'GET', params: axiosConfig.params });

    // Try to get from cache first
    const cachedResponse = apiCache.get(cacheKey);
    if (cachedResponse) {
      if (process.env.NODE_ENV === 'development') {
        console.log(`[API Cache] Cache HIT for ${url}`);
      }
      return cachedResponse;
    }

    if (process.env.NODE_ENV === 'development') {
      console.log(`[API Cache] Cache MISS for ${url}`);
    }

    // Make the actual request
    const response = await this.client.get(url, axiosConfig);

    // Determine TTL based on URL pattern or use provided TTL
    const ttl = cacheTTL || this.getCacheTTL(url);

    // Cache the response if TTL is set
    if (ttl > 0) {
      apiCache.set(cacheKey, response, ttl);
    }

    return response;
  }

  /**
   * POST request with cache invalidation
   * @param {string} url - Request URL
   * @param {*} data - Request data
   * @param {Object} config - Axios config options
   * @returns {Promise} Promise that resolves to response data
   */
  async post(url, data, config = {}) {
    const response = await this.client.post(url, data, config);

    // Invalidate related cache entries after successful POST
    this.invalidateRelatedCache(url);

    return response;
  }

  /**
   * PUT request with cache invalidation
   * @param {string} url - Request URL
   * @param {*} data - Request data
   * @param {Object} config - Axios config options
   * @returns {Promise} Promise that resolves to response data
   */
  async put(url, data, config = {}) {
    const response = await this.client.put(url, data, config);

    // Invalidate related cache entries after successful PUT
    this.invalidateRelatedCache(url);

    return response;
  }

  /**
   * DELETE request with cache invalidation
   * @param {string} url - Request URL
   * @param {Object} config - Axios config options
   * @returns {Promise} Promise that resolves to response data
   */
  async delete(url, config = {}) {
    const response = await this.client.delete(url, config);

    // Invalidate related cache entries after successful DELETE
    this.invalidateRelatedCache(url);

    return response;
  }

  /**
   * PATCH request
   * @param {string} url - Request URL
   * @param {*} data - Request data
   * @param {Object} config - Axios config options
   * @returns {Promise} Promise that resolves to response data
   */
  async patch(url, data, config = {}) {
    return this.client.patch(url, data, config);
  }

  /**
   * File upload helper
   * @param {string} url - Upload URL
   * @param {File|File[]} files - File or array of files to upload
   * @param {Object} additionalData - Additional form data
   * @param {Function} onUploadProgress - Upload progress callback
   * @returns {Promise} Promise that resolves to response data
   */
  async upload(url, files, additionalData = {}, onUploadProgress = null) {
    const formData = new FormData();

    // Handle both single file and array of files
    if (Array.isArray(files)) {
      files.forEach(file => {
        formData.append('files', file);
      });
    } else {
      formData.append('files', files);
    }

    // Add additional form data
    Object.keys(additionalData).forEach(key => {
      formData.append(key, additionalData[key]);
    });

    const config = {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    };

    if (onUploadProgress) {
      config.onUploadProgress = onUploadProgress;
    }

    return this.client.post(url, formData, config);
  }

  /**
   * Download file helper
   * @param {string} url - Download URL
   * @param {Object} config - Additional config options
   * @returns {Promise} Promise that resolves to blob data
   */
  async download(url, config = {}) {
    const response = await this.client.get(url, {
      ...config,
      responseType: 'blob'
    });

    // Return the full response for download handling
    return response;
  }

  /**
   * Determine cache TTL based on URL pattern
   * @param {string} url - Request URL
   * @returns {number} TTL in milliseconds
   */
  getCacheTTL(url) {
    // Check each cache configuration pattern
    for (const [configName, config] of Object.entries(CACHE_CONFIGS)) {
      const pattern = new RegExp(config.pattern);
      if (pattern.test(`GET|${url}`)) {
        return config.ttl;
      }
    }

    // Default TTL for unmatched URLs (5 minutes)
    return 5 * 60 * 1000;
  }

  /**
   * Invalidate cache entries by pattern
   * @param {string} pattern - Regex pattern to match cache keys
   * @returns {number} Number of invalidated entries
   */
  invalidateCache(pattern) {
    return apiCache.invalidateByPattern(pattern);
  }

  /**
   * Invalidate cache entries by URL prefix
   * @param {string} prefix - URL prefix to match
   * @returns {number} Number of invalidated entries
   */
  invalidateCacheByPrefix(prefix) {
    return apiCache.invalidateByPrefix(prefix);
  }

  /**
   * Clear all cache entries
   */
  clearCache() {
    apiCache.clear();
  }

  /**
   * Get cache statistics
   * @returns {Object} Cache statistics
   */
  getCacheStats() {
    return apiCache.getStats();
  }

  /**
   * Get cache information for debugging
   * @returns {Array} Array of cache entries with metadata
   */
  getCacheInfo() {
    return apiCache.getCacheInfo();
  }

  /**
   * Invalidate cache entries related to a URL
   * @param {string} url - The URL that was modified
   */
  invalidateRelatedCache(url) {
    // Invalidate based on URL patterns
    if (url.includes('/users')) {
      apiCache.invalidateByPattern('.*users.*');
    }

    if (url.includes('/workflows')) {
      apiCache.invalidateByPattern('.*workflows.*');
      apiCache.invalidateByPattern('.*dashboard.*');
    }

    if (url.includes('/plants') || url.includes('/plant-assignments')) {
      apiCache.invalidateByPattern('.*plants.*');
      apiCache.invalidateByPattern('.*plant-assignments.*');
    }

    if (url.includes('/roles')) {
      apiCache.invalidateByPattern('.*roles.*');
    }

    if (url.includes('/dashboard')) {
      apiCache.invalidateByPattern('.*dashboard.*');
    }

    // Log cache invalidation in development
    if (process.env.NODE_ENV === 'development') {
      console.log(`[API Cache] Invalidated cache for URL pattern: ${url}`);
    }
  }
}

// Create and export a singleton instance
const apiClient = new APIClient();

export default apiClient;
export { APIClient };
