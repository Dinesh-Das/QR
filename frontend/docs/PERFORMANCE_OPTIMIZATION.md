# QRMFG Frontend Performance Optimization Guide

## 🚀 Overview

This guide provides comprehensive performance optimization strategies for the QRMFG frontend application. Following these guidelines will ensure optimal user experience with fast load times, smooth interactions, and efficient resource usage.

## 📊 Performance Metrics & Goals

### Target Metrics

- **First Contentful Paint (FCP)**: < 1.5s
- **Largest Contentful Paint (LCP)**: < 2.5s
- **First Input Delay (FID)**: < 100ms
- **Cumulative Layout Shift (CLS)**: < 0.1
- **Time to Interactive (TTI)**: < 3.5s
- **Bundle Size**: < 2MB (gzipped)
- **Lighthouse Score**: > 90

### Measurement Tools

```javascript
// ✅ Performance monitoring setup
import { getCLS, getFID, getFCP, getLCP, getTTFB } from 'web-vitals';

const sendToAnalytics = (metric) => {
  // Send metrics to monitoring service
  analytics.track('web-vital', {
    name: metric.name,
    value: metric.value,
    id: metric.id,
    url: window.location.href
  });
};

// Measure Core Web Vitals
getCLS(sendToAnalytics);
getFID(sendToAnalytics);
getFCP(sendToAnalytics);
getLCP(sendToAnalytics);
getTTFB(sendToAnalytics);
```

## ⚛️ React Performance Optimization

### Component Memoization

```javascript
// ✅ React.memo for preventing unnecessary re-renders
const UserTable = React.memo(({ users, onEdit, onDelete }) => {
  const columns = useMemo(() => [
    {
      title: 'Username',
      dataIndex: 'username',
      sorter: (a, b) => a.username.localeCompare(b.username)
    },
    {
      title: 'Actions',
      render: (_, record) => (
        <Space>
          <Button onClick={() => onEdit(record)}>Edit</Button>
          <Button onClick={() => onDelete(record.id)}>Delete</Button>
        </Space>
      )
    }
  ], [onEdit, onDelete]);

  return <Table dataSource={users} columns={columns} rowKey="id" />;
});

// ✅ Custom comparison function for complex props
const ComplexComponent = React.memo(({ data, config }) => {
  // Component implementation
}, (prevProps, nextProps) => {
  // Custom comparison logic
  return (
    prevProps.data.length === nextProps.data.length &&
    prevProps.config.theme === nextProps.config.theme
  );
});
```

### useMemo and useCallback Optimization

```javascript
// ✅ Memoize expensive calculations
const DataProcessor = ({ rawData, filters }) => {
  // Expensive data processing
  const processedData = useMemo(() => {
    return rawData
      .filter(item => filters.every(filter => filter.test(item)))
      .map(item => ({
        ...item,
        displayName: `${item.firstName} ${item.lastName}`,
        formattedDate: formatDate(item.createdAt)
      }))
      .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  }, [rawData, filters]);

  // Memoize event handlers
  const handleItemClick = useCallback((item) => {
    analytics.track('item-clicked', { itemId: item.id });
    onItemSelect(item);
  }, [onItemSelect]);

  const handleBulkAction = useCallback((action, selectedItems) => {
    const itemIds = selectedItems.map(item => item.id);
    onBulkAction(action, itemIds);
  }, [onBulkAction]);

  return (
    <ItemList 
      items={processedData}
      onItemClick={handleItemClick}
      onBulkAction={handleBulkAction}
    />
  );
};
```

### Proper Key Usage

```javascript
// ✅ Use stable, unique keys
const UserList = ({ users }) => (
  <div>
    {users.map(user => (
      <UserCard 
        key={user.id} // Stable unique key
        user={user}
      />
    ))}
  </div>
);

// ❌ Avoid array indices as keys
const BadUserList = ({ users }) => (
  <div>
    {users.map((user, index) => (
      <UserCard 
        key={index} // Causes performance issues
        user={user}
      />
    ))}
  </div>
);

// ✅ Composite keys for complex scenarios
const NestedList = ({ categories }) => (
  <div>
    {categories.map(category => (
      <div key={category.id}>
        <h3>{category.name}</h3>
        {category.items.map(item => (
          <ItemCard 
            key={`${category.id}-${item.id}`} // Composite key
            item={item}
          />
        ))}
      </div>
    ))}
  </div>
);
```

### Virtual Scrolling for Large Lists

```javascript
// ✅ Virtual scrolling for large datasets
import { FixedSizeList as List } from 'react-window';

const VirtualizedUserList = ({ users }) => {
  const Row = ({ index, style }) => (
    <div style={style}>
      <UserCard user={users[index]} />
    </div>
  );

  return (
    <List
      height={600}
      itemCount={users.length}
      itemSize={80}
      width="100%"
    >
      {Row}
    </List>
  );
};

// ✅ Virtual scrolling with dynamic heights
import { VariableSizeList as List } from 'react-window';

const DynamicVirtualList = ({ items }) => {
  const getItemSize = useCallback((index) => {
    // Calculate item height based on content
    return items[index].expanded ? 120 : 60;
  }, [items]);

  const Row = ({ index, style }) => (
    <div style={style}>
      <ItemCard item={items[index]} />
    </div>
  );

  return (
    <List
      height={600}
      itemCount={items.length}
      itemSize={getItemSize}
      width="100%"
    >
      {Row}
    </List>
  );
};
```

## 📦 Bundle Optimization

### Code Splitting

```javascript
// ✅ Route-based code splitting
import { lazy, Suspense } from 'react';
import { Routes, Route } from 'react-router-dom';

// Lazy load route components
const Users = lazy(() => import('./screens/Users'));
const PlantView = lazy(() => import('./screens/PlantView'));
const Dashboard = lazy(() => import('./screens/Dashboard'));
const Reports = lazy(() => import('./screens/Reports'));

const App = () => (
  <Suspense fallback={<PageSkeleton />}>
    <Routes>
      <Route path="/users" element={<Users />} />
      <Route path="/plant" element={<PlantView />} />
      <Route path="/dashboard" element={<Dashboard />} />
      <Route path="/reports" element={<Reports />} />
    </Routes>
  </Suspense>
);

// ✅ Component-based code splitting
const HeavyChart = lazy(() => import('./components/HeavyChart'));

const Dashboard = () => {
  const [showChart, setShowChart] = useState(false);

  return (
    <div>
      <h1>Dashboard</h1>
      <Button onClick={() => setShowChart(true)}>
        Load Chart
      </Button>
      
      {showChart && (
        <Suspense fallback={<Spin />}>
          <HeavyChart />
        </Suspense>
      )}
    </div>
  );
};
```

### Tree Shaking

```javascript
// ✅ Import only what you need
import { Button, Table, Modal } from 'antd';
import { debounce, throttle } from 'lodash';

// ❌ Avoid importing entire libraries
import * as antd from 'antd';
import _ from 'lodash';

// ✅ Use babel-plugin-import for automatic optimization
// .babelrc
{
  "plugins": [
    ["import", {
      "libraryName": "antd",
      "libraryDirectory": "es",
      "style": "css"
    }]
  ]
}

// ✅ Dynamic imports for conditional features
const loadAdvancedFeatures = async () => {
  if (user.hasAdvancedAccess) {
    const { AdvancedAnalytics } = await import('./AdvancedAnalytics');
    return AdvancedAnalytics;
  }
  return null;
};
```

### Bundle Analysis

```javascript
// package.json scripts for bundle analysis
{
  "scripts": {
    "analyze": "npm run build && npx webpack-bundle-analyzer build/static/js/*.js",
    "build:analyze": "ANALYZE=true npm run build"
  }
}

// webpack-bundle-analyzer configuration
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;

module.exports = {
  plugins: [
    process.env.ANALYZE && new BundleAnalyzerPlugin({
      analyzerMode: 'static',
      openAnalyzer: false,
      reportFilename: 'bundle-report.html'
    })
  ].filter(Boolean)
};
```

## 🌐 Network Optimization

### API Request Optimization

```javascript
// ✅ Request batching
const useBatchedRequests = () => {
  const batchQueue = useRef([]);
  const timeoutRef = useRef();

  const batchRequest = useCallback((request) => {
    batchQueue.current.push(request);

    // Clear existing timeout
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }

    // Batch requests after 50ms
    timeoutRef.current = setTimeout(async () => {
      const requests = [...batchQueue.current];
      batchQueue.current = [];

      try {
        const response = await apiClient.post('/batch', { requests });
        
        // Resolve individual promises
        requests.forEach((request, index) => {
          request.resolve(response.data[index]);
        });
      } catch (error) {
        requests.forEach(request => {
          request.reject(error);
        });
      }
    }, 50);

    return new Promise((resolve, reject) => {
      batchQueue.current[batchQueue.current.length - 1].resolve = resolve;
      batchQueue.current[batchQueue.current.length - 1].reject = reject;
    });
  }, []);

  return { batchRequest };
};
```

### Request Caching

```javascript
// ✅ Intelligent caching with TTL
class APICache {
  constructor(defaultTTL = 5 * 60 * 1000) { // 5 minutes
    this.cache = new Map();
    this.defaultTTL = defaultTTL;
  }

  generateKey(url, params = {}) {
    const sortedParams = Object.keys(params)
      .sort()
      .reduce((result, key) => {
        result[key] = params[key];
        return result;
      }, {});
    
    return `${url}?${JSON.stringify(sortedParams)}`;
  }

  get(key) {
    const item = this.cache.get(key);
    
    if (!item) return null;
    
    if (Date.now() > item.expiry) {
      this.cache.delete(key);
      return null;
    }
    
    return item.data;
  }

  set(key, data, ttl = this.defaultTTL) {
    this.cache.set(key, {
      data,
      expiry: Date.now() + ttl
    });
  }

  invalidate(pattern) {
    for (const key of this.cache.keys()) {
      if (key.includes(pattern)) {
        this.cache.delete(key);
      }
    }
  }

  clear() {
    this.cache.clear();
  }
}

// Usage with React hook
const useApiCache = () => {
  const cache = useRef(new APICache());

  const cachedRequest = useCallback(async (url, params = {}, options = {}) => {
    const cacheKey = cache.current.generateKey(url, params);
    
    // Check cache first
    const cachedData = cache.current.get(cacheKey);
    if (cachedData && !options.skipCache) {
      return cachedData;
    }

    // Make request
    const response = await apiClient.get(url, { params });
    
    // Cache response
    cache.current.set(cacheKey, response.data, options.ttl);
    
    return response.data;
  }, []);

  return { cachedRequest, cache: cache.current };
};
```

### Request Deduplication

```javascript
// ✅ Prevent duplicate requests
const useRequestDeduplication = () => {
  const pendingRequests = useRef(new Map());

  const deduplicatedRequest = useCallback(async (key, requestFn) => {
    // Check if request is already pending
    if (pendingRequests.current.has(key)) {
      return pendingRequests.current.get(key);
    }

    // Create new request promise
    const requestPromise = requestFn()
      .finally(() => {
        // Clean up after request completes
        pendingRequests.current.delete(key);
      });

    // Store pending request
    pendingRequests.current.set(key, requestPromise);

    return requestPromise;
  }, []);

  return { deduplicatedRequest };
};

// Usage example
const useUserData = (userId) => {
  const { deduplicatedRequest } = useRequestDeduplication();
  const [userData, setUserData] = useState(null);

  useEffect(() => {
    const fetchUser = () => apiClient.get(`/users/${userId}`);
    
    deduplicatedRequest(`user-${userId}`, fetchUser)
      .then(response => setUserData(response.data))
      .catch(error => console.error('Failed to fetch user:', error));
  }, [userId, deduplicatedRequest]);

  return userData;
};
```

## 🖼️ Asset Optimization

### Image Optimization

```javascript
// ✅ Responsive images with lazy loading
const OptimizedImage = ({ 
  src, 
  alt, 
  width, 
  height, 
  className,
  loading = 'lazy' 
}) => {
  const [imageSrc, setImageSrc] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    const img = new Image();
    
    img.onload = () => {
      setImageSrc(src);
      setIsLoading(false);
    };
    
    img.onerror = () => {
      setError(true);
      setIsLoading(false);
    };
    
    img.src = src;
  }, [src]);

  if (error) {
    return (
      <div 
        className={`image-placeholder ${className}`}
        style={{ width, height }}
      >
        <span>Image not available</span>
      </div>
    );
  }

  return (
    <div className={`image-container ${className}`}>
      {isLoading && (
        <div 
          className="image-skeleton"
          style={{ width, height }}
        />
      )}
      {imageSrc && (
        <img
          src={imageSrc}
          alt={alt}
          width={width}
          height={height}
          loading={loading}
          style={{ display: isLoading ? 'none' : 'block' }}
        />
      )}
    </div>
  );
};

// ✅ WebP support with fallback
const WebPImage = ({ src, alt, ...props }) => {
  const webpSrc = src.replace(/\.(jpg|jpeg|png)$/i, '.webp');
  
  return (
    <picture>
      <source srcSet={webpSrc} type="image/webp" />
      <img src={src} alt={alt} {...props} />
    </picture>
  );
};
```

### Font Optimization

```css
/* ✅ Optimized font loading */
@font-face {
  font-family: 'CustomFont';
  src: url('./fonts/custom-font.woff2') format('woff2'),
       url('./fonts/custom-font.woff') format('woff');
  font-display: swap; /* Improves FCP */
  font-weight: 400;
  font-style: normal;
}

/* ✅ Preload critical fonts */
/* In HTML head */
<link 
  rel="preload" 
  href="/fonts/critical-font.woff2" 
  as="font" 
  type="font/woff2" 
  crossorigin
>
```

## 🔄 State Management Optimization

### Efficient State Updates

```javascript
// ✅ Batch state updates
import { unstable_batchedUpdates } from 'react-dom';

const handleMultipleUpdates = () => {
  unstable_batchedUpdates(() => {
    setLoading(false);
    setData(newData);
    setError(null);
  });
};

// ✅ Use useReducer for complex state
const dataReducer = (state, action) => {
  switch (action.type) {
    case 'FETCH_START':
      return { ...state, loading: true, error: null };
    case 'FETCH_SUCCESS':
      return { 
        ...state, 
        loading: false, 
        data: action.payload,
        lastUpdated: Date.now()
      };
    case 'FETCH_ERROR':
      return { ...state, loading: false, error: action.payload };
    default:
      return state;
  }
};

const useDataFetching = () => {
  const [state, dispatch] = useReducer(dataReducer, {
    data: null,
    loading: false,
    error: null,
    lastUpdated: null
  });

  const fetchData = useCallback(async (url) => {
    dispatch({ type: 'FETCH_START' });
    
    try {
      const response = await apiClient.get(url);
      dispatch({ type: 'FETCH_SUCCESS', payload: response.data });
    } catch (error) {
      dispatch({ type: 'FETCH_ERROR', payload: error.message });
    }
  }, []);

  return { ...state, fetchData };
};
```

### State Normalization

```javascript
// ✅ Normalize complex state structures
const normalizeUsers = (users) => {
  const byId = {};
  const allIds = [];

  users.forEach(user => {
    byId[user.id] = user;
    allIds.push(user.id);
  });

  return { byId, allIds };
};

const useNormalizedUsers = () => {
  const [users, setUsers] = useState({ byId: {}, allIds: [] });

  const addUser = useCallback((user) => {
    setUsers(prev => ({
      byId: { ...prev.byId, [user.id]: user },
      allIds: prev.allIds.includes(user.id) 
        ? prev.allIds 
        : [...prev.allIds, user.id]
    }));
  }, []);

  const updateUser = useCallback((userId, updates) => {
    setUsers(prev => ({
      ...prev,
      byId: {
        ...prev.byId,
        [userId]: { ...prev.byId[userId], ...updates }
      }
    }));
  }, []);

  const removeUser = useCallback((userId) => {
    setUsers(prev => {
      const { [userId]: removed, ...restById } = prev.byId;
      return {
        byId: restById,
        allIds: prev.allIds.filter(id => id !== userId)
      };
    });
  }, []);

  return { users, addUser, updateUser, removeUser };
};
```

## ⏱️ Performance Monitoring

### Custom Performance Hooks

```javascript
// ✅ Performance monitoring hook
const usePerformanceMonitoring = (componentName) => {
  const renderStartTime = useRef();
  const renderCount = useRef(0);

  useEffect(() => {
    renderStartTime.current = performance.now();
    renderCount.current += 1;
  });

  useEffect(() => {
    const renderTime = performance.now() - renderStartTime.current;
    
    // Log slow renders
    if (renderTime > 16) { // 60fps threshold
      console.warn(`Slow render in ${componentName}: ${renderTime.toFixed(2)}ms`);
    }

    // Send metrics to monitoring service
    if (renderCount.current % 10 === 0) { // Sample every 10th render
      analytics.track('component-performance', {
        component: componentName,
        renderTime,
        renderCount: renderCount.current
      });
    }
  });

  const measureOperation = useCallback((operationName, operation) => {
    const startTime = performance.now();
    
    const result = operation();
    
    if (result && typeof result.then === 'function') {
      // Handle async operations
      return result.finally(() => {
        const duration = performance.now() - startTime;
        analytics.track('async-operation', {
          operation: operationName,
          duration,
          component: componentName
        });
      });
    } else {
      // Handle sync operations
      const duration = performance.now() - startTime;
      analytics.track('sync-operation', {
        operation: operationName,
        duration,
        component: componentName
      });
      return result;
    }
  }, [componentName]);

  return { measureOperation };
};

// Usage example
const UserTable = ({ users }) => {
  const { measureOperation } = usePerformanceMonitoring('UserTable');

  const processUsers = useCallback(() => {
    return measureOperation('process-users', () => {
      return users
        .filter(user => user.active)
        .sort((a, b) => a.name.localeCompare(b.name));
    });
  }, [users, measureOperation]);

  const processedUsers = useMemo(processUsers, [processUsers]);

  return <Table dataSource={processedUsers} />;
};
```

### Memory Leak Prevention

```javascript
// ✅ Proper cleanup in useEffect
const useApiSubscription = (endpoint) => {
  const [data, setData] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    let intervalId;

    const fetchData = async () => {
      try {
        const response = await apiClient.get(endpoint, {
          signal: controller.signal
        });
        setData(response.data);
      } catch (error) {
        if (!controller.signal.aborted) {
          console.error('Fetch error:', error);
        }
      }
    };

    // Initial fetch
    fetchData();

    // Set up polling
    intervalId = setInterval(fetchData, 30000);

    // Cleanup function
    return () => {
      controller.abort();
      if (intervalId) {
        clearInterval(intervalId);
      }
    };
  }, [endpoint]);

  return data;
};

// ✅ Event listener cleanup
const useWindowResize = (callback) => {
  useEffect(() => {
    const handleResize = () => callback(window.innerWidth, window.innerHeight);
    
    window.addEventListener('resize', handleResize);
    
    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, [callback]);
};
```

## 🎯 Performance Best Practices Summary

### Do's ✅

1. **Use React.memo** for components with stable props
2. **Memoize expensive calculations** with useMemo
3. **Memoize event handlers** with useCallback
4. **Use proper keys** for list items (stable, unique)
5. **Implement code splitting** for routes and heavy components
6. **Optimize images** with lazy loading and WebP format
7. **Cache API responses** with appropriate TTL
8. **Batch API requests** when possible
9. **Use virtual scrolling** for large lists
10. **Monitor performance** with Web Vitals

### Don'ts ❌

1. **Don't use array indices as keys** in dynamic lists
2. **Don't create objects/functions in render** without memoization
3. **Don't forget cleanup functions** in useEffect
4. **Don't over-memoize** simple components
5. **Don't ignore bundle size** - monitor and optimize
6. **Don't make unnecessary API calls** - implement caching
7. **Don't block the main thread** with heavy computations
8. **Don't ignore memory leaks** - clean up subscriptions
9. **Don't skip performance testing** in CI/CD
10. **Don't optimize prematurely** - measure first

## 📊 Performance Testing

### Automated Performance Testing

```javascript
// jest.config.js - Performance testing setup
module.exports = {
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: ['<rootDir>/src/setupTests.js'],
  testMatch: [
    '<rootDir>/src/**/__tests__/**/*.{js,jsx}',
    '<rootDir>/src/**/*.{test,spec}.{js,jsx}',
    '<rootDir>/src/**/*.perf.{test,spec}.{js,jsx}' // Performance tests
  ]
};

// Performance test example
describe('UserTable Performance', () => {
  it('should render 1000 users within performance budget', async () => {
    const users = generateMockUsers(1000);
    
    const startTime = performance.now();
    
    render(<UserTable users={users} />);
    
    await waitFor(() => {
      expect(screen.getAllByRole('row')).toHaveLength(1001); // +1 for header
    });
    
    const renderTime = performance.now() - startTime;
    
    // Should render within 100ms
    expect(renderTime).toBeLessThan(100);
  });

  it('should handle rapid prop changes efficiently', () => {
    const { rerender } = render(<UserTable users={[]} />);
    
    const startTime = performance.now();
    
    // Simulate rapid updates
    for (let i = 0; i < 100; i++) {
      rerender(<UserTable users={generateMockUsers(i)} />);
    }
    
    const totalTime = performance.now() - startTime;
    
    // Should handle 100 updates within 500ms
    expect(totalTime).toBeLessThan(500);
  });
});
```

### Lighthouse CI Integration

```yaml
# .github/workflows/performance.yml
name: Performance Testing

on:
  pull_request:
    branches: [main]

jobs:
  lighthouse:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
          cache: 'npm'
          
      - name: Install dependencies
        run: |
          cd frontend
          npm ci
          
      - name: Build application
        run: |
          cd frontend
          npm run build
          
      - name: Run Lighthouse CI
        run: |
          npm install -g @lhci/cli@0.12.x
          lhci autorun
        env:
          LHCI_GITHUB_APP_TOKEN: ${{ secrets.LHCI_GITHUB_APP_TOKEN }}
```

## 📞 Performance Support

For performance-related questions or issues:

- **Slack**: #frontend-performance channel
- **Email**: performance-team@qrmfg.com
- **Documentation**: [Performance Wiki](https://wiki.qrmfg.com/frontend/performance)
- **Monitoring**: [Performance Dashboard](https://monitoring.qrmfg.com/frontend)

Remember: Performance optimization is an ongoing process. Measure first, optimize second, and always validate improvements with real metrics.