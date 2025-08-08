/**
 * APICache - TTL-based caching mechanism for API responses
 * Provides in-memory caching with automatic expiration and cache invalidation
 */
class APICache {
  constructor(defaultTTL = 5 * 60 * 1000) { // 5 minutes default TTL
    this.cache = new Map();
    this.defaultTTL = defaultTTL;
    this.metrics = {
      hits: 0,
      misses: 0,
      sets: 0,
      invalidations: 0
    };
    
    // Clean up expired entries every minute
    this.cleanupInterval = setInterval(() => {
      this.cleanup();
    }, 60 * 1000);
  }

  /**
   * Generate cache key from URL and options
   */
  generateKey(url, options = {}) {
    const { method = 'GET', params, data } = options;
    const keyParts = [method.toUpperCase(), url];
    
    if (params) {
      keyParts.push(JSON.stringify(params));
    }
    
    if (data && (method.toUpperCase() === 'POST' || method.toUpperCase() === 'PUT')) {
      keyParts.push(JSON.stringify(data));
    }
    
    return keyParts.join('|');
  }

  /**
   * Get cached response
   */
  get(key) {
    const item = this.cache.get(key);
    
    if (!item) {
      this.metrics.misses++;
      return null;
    }
    
    // Check if expired
    if (Date.now() > item.expiry) {
      this.cache.delete(key);
      this.metrics.misses++;
      return null;
    }
    
    this.metrics.hits++;
    return item.data;
  }

  /**
   * Set cached response with TTL
   */
  set(key, data, ttl = this.defaultTTL) {
    const expiry = Date.now() + ttl;
    
    this.cache.set(key, {
      data: this.deepClone(data), // Store a deep copy to prevent mutations
      expiry,
      createdAt: Date.now(),
      ttl
    });
    
    this.metrics.sets++;
  }

  /**
   * Check if key exists and is not expired
   */
  has(key) {
    const item = this.cache.get(key);
    
    if (!item) {
      return false;
    }
    
    if (Date.now() > item.expiry) {
      this.cache.delete(key);
      return false;
    }
    
    return true;
  }

  /**
   * Delete specific cache entry
   */
  delete(key) {
    const deleted = this.cache.delete(key);
    if (deleted) {
      this.metrics.invalidations++;
    }
    return deleted;
  }

  /**
   * Clear all cache entries
   */
  clear() {
    const size = this.cache.size;
    this.cache.clear();
    this.metrics.invalidations += size;
  }

  /**
   * Invalidate cache entries by pattern
   */
  invalidateByPattern(pattern) {
    const regex = new RegExp(pattern);
    let invalidated = 0;
    
    for (const key of this.cache.keys()) {
      if (regex.test(key)) {
        this.cache.delete(key);
        invalidated++;
      }
    }
    
    this.metrics.invalidations += invalidated;
    return invalidated;
  }

  /**
   * Invalidate cache entries by URL prefix
   */
  invalidateByPrefix(prefix) {
    let invalidated = 0;
    
    for (const key of this.cache.keys()) {
      if (key.includes(prefix)) {
        this.cache.delete(key);
        invalidated++;
      }
    }
    
    this.metrics.invalidations += invalidated;
    return invalidated;
  }

  /**
   * Clean up expired entries
   */
  cleanup() {
    const now = Date.now();
    let cleaned = 0;
    
    for (const [key, item] of this.cache.entries()) {
      if (now > item.expiry) {
        this.cache.delete(key);
        cleaned++;
      }
    }
    
    return cleaned;
  }

  /**
   * Get cache statistics
   */
  getStats() {
    const totalRequests = this.metrics.hits + this.metrics.misses;
    const hitRate = totalRequests > 0 ? (this.metrics.hits / totalRequests) * 100 : 0;
    
    return {
      ...this.metrics,
      totalRequests,
      hitRate: Math.round(hitRate * 100) / 100,
      cacheSize: this.cache.size,
      memoryUsage: this.getMemoryUsage()
    };
  }

  /**
   * Get cache entries info
   */
  getCacheInfo() {
    const entries = [];
    const now = Date.now();
    
    for (const [key, item] of this.cache.entries()) {
      entries.push({
        key,
        size: this.getObjectSize(item.data),
        ttl: item.ttl,
        remainingTTL: Math.max(0, item.expiry - now),
        createdAt: new Date(item.createdAt).toISOString(),
        expiresAt: new Date(item.expiry).toISOString(),
        isExpired: now > item.expiry
      });
    }
    
    return entries.sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  }

  /**
   * Estimate memory usage (rough calculation)
   */
  getMemoryUsage() {
    let totalSize = 0;
    
    for (const [key, item] of this.cache.entries()) {
      totalSize += key.length * 2; // Approximate string size
      totalSize += this.getObjectSize(item.data);
      totalSize += 64; // Approximate overhead per entry
    }
    
    return {
      bytes: totalSize,
      kb: Math.round(totalSize / 1024 * 100) / 100,
      mb: Math.round(totalSize / (1024 * 1024) * 100) / 100
    };
  }

  /**
   * Deep clone object to prevent mutations
   */
  deepClone(obj) {
    if (obj === null || typeof obj !== 'object') {
      return obj;
    }
    
    if (obj instanceof Date) {
      return new Date(obj.getTime());
    }
    
    if (obj instanceof Array) {
      return obj.map(item => this.deepClone(item));
    }
    
    if (typeof obj === 'object') {
      const cloned = {};
      for (const key in obj) {
        if (obj.hasOwnProperty(key)) {
          cloned[key] = this.deepClone(obj[key]);
        }
      }
      return cloned;
    }
    
    return obj;
  }

  /**
   * Rough object size calculation
   */
  getObjectSize(obj) {
    try {
      return JSON.stringify(obj).length * 2; // Approximate size in bytes
    } catch (error) {
      return 0;
    }
  }

  /**
   * Destroy cache and cleanup
   */
  destroy() {
    if (this.cleanupInterval) {
      clearInterval(this.cleanupInterval);
      this.cleanupInterval = null;
    }
    this.clear();
  }
}

// Cache configurations for different types of data
export const CACHE_CONFIGS = {
  // User data - cache for 10 minutes
  USERS: {
    ttl: 10 * 60 * 1000,
    pattern: 'GET|/admin/users'
  },
  
  // Roles data - cache for 30 minutes (rarely changes)
  ROLES: {
    ttl: 30 * 60 * 1000,
    pattern: 'GET|/admin/roles'
  },
  
  // Plant data - cache for 15 minutes
  PLANTS: {
    ttl: 15 * 60 * 1000,
    pattern: 'GET|/master-data/plants'
  },
  
  // Workflow data - cache for 2 minutes (frequently updated)
  WORKFLOWS: {
    ttl: 2 * 60 * 1000,
    pattern: 'GET|.*workflows'
  },
  
  // Dashboard data - cache for 1 minute
  DASHBOARD: {
    ttl: 1 * 60 * 1000,
    pattern: 'GET|.*dashboard'
  },
  
  // Static data - cache for 1 hour
  STATIC: {
    ttl: 60 * 60 * 1000,
    pattern: 'GET|/static'
  }
};

// Create singleton instance
export const apiCache = new APICache();

// Cache invalidation strategies
export const invalidateCache = {
  // Invalidate user-related cache when user data changes
  users: () => {
    apiCache.invalidateByPattern('.*users.*');
  },
  
  // Invalidate workflow cache when workflow data changes
  workflows: () => {
    apiCache.invalidateByPattern('.*workflows.*');
    apiCache.invalidateByPattern('.*dashboard.*');
  },
  
  // Invalidate plant cache when plant assignments change
  plants: () => {
    apiCache.invalidateByPattern('.*plants.*');
    apiCache.invalidateByPattern('.*plant-assignments.*');
  },
  
  // Invalidate all cache
  all: () => {
    apiCache.clear();
  }
};

export default APICache;