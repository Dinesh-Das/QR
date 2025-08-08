# Design Document

## Overview

This design document outlines the comprehensive refactoring and improvement strategy for the QRMFG frontend application. Based on the audit findings of 47 JavaScript/JSX files, we will address critical issues through systematic refactoring while maintaining the existing React 18.2.0 + Ant Design 5.11.0 architecture. The design focuses on breaking down monolithic components, implementing secure authentication, standardizing the API layer, adding comprehensive error handling, and establishing robust development processes.

## Architecture

### Current Architecture Assessment

The current architecture follows a feature-based pattern with clear separation of concerns:

```
frontend/src/
├── api/                    # Dual API implementations (ISSUE)
├── components/            # Mixed component sizes (ISSUE)  
├── screens/               # Large monolithic screens (CRITICAL ISSUE)
├── services/              # Well-structured service layer
├── utils/                 # Utility functions
```

**Strengths:**
- Modern React 18 with functional components and hooks
- Clear folder structure with separation of concerns
- Excellent ErrorBoundary implementation
- Good activity tracking system
- Role-based access control

**Critical Issues:**
- Users.js (300+ lines) and PlantView.js (400+ lines) monolithic components
- Insecure JWT token storage in localStorage
- Duplicate API implementations (fetch + axios)
- Missing error boundaries on routes
- Code quality issues (unused imports, deprecated props)

### Target Architecture

The refactored architecture will maintain the same structure but with improved component organization:

```
frontend/src/
├── api/
│   └── client.js                    # Single unified API client
├── components/
│   ├── ErrorBoundaries/            # Specialized error boundaries
│   ├── User/                       # User management components
│   │   ├── UserTable.js
│   │   ├── UserModal.js
│   │   └── PlantAssignmentForm.js
│   ├── Plant/                      # Plant management components
│   │   ├── PlantDashboard.js
│   │   ├── WorkflowTable.js
│   │   └── FilterPanel.js
│   └── [existing components]
├── hooks/                          # Custom hooks extracted from components
│   ├── useUserManagement.js
│   ├── usePlantAssignment.js
│   ├── usePlantWorkflows.js
│   └── useWorkflowFilters.js
├── screens/                        # Refactored screen components
├── services/
│   ├── auth.js                     # Enhanced with secure storage
│   ├── secureStorage.js            # New secure token storage
│   ├── jwtValidator.js             # New JWT validation
│   └── [existing services]
└── constants/                      # New constants file
    └── index.js
```#
# Components and Interfaces

### 1. Component Refactoring Strategy

#### 1.1 Users.js Refactoring (300+ lines → 3 components < 150 lines each)

**Current Issues:**
- Mixed concerns: UI rendering, state management, API calls
- Complex state with multiple useState calls
- Difficult to test and maintain

**Refactored Structure:**

```javascript
// screens/Users.js (Main orchestrator - ~100 lines)
const Users = () => {
  const { users, loading, error, actions } = useUserManagement();
  const { plantAssignments } = usePlantAssignment();
  
  return (
    <UserErrorBoundary>
      <UserTable 
        users={users}
        loading={loading}
        onEdit={actions.editUser}
        onDelete={actions.deleteUser}
      />
      <UserModal 
        visible={modalState.visible}
        user={modalState.user}
        onSave={actions.saveUser}
        onCancel={actions.closeModal}
      />
      <PlantAssignmentForm
        assignments={plantAssignments}
        onAssign={actions.assignPlant}
      />
    </UserErrorBoundary>
  );
};

// hooks/useUserManagement.js (Business logic)
export const useUserManagement = () => {
  const [state, dispatch] = useReducer(userReducer, initialState);
  
  const actions = useMemo(() => ({
    editUser: useCallback((user) => { /* logic */ }, []),
    deleteUser: useCallback((id) => { /* logic */ }, []),
    saveUser: useCallback((userData) => { /* logic */ }, []),
    // ... other actions
  }), []);
  
  return { ...state, actions };
};

// components/User/UserTable.js (~120 lines)
const UserTable = React.memo(({ users, loading, onEdit, onDelete }) => {
  const columns = useMemo(() => [
    // column definitions
  ], [onEdit, onDelete]);
  
  return (
    <Table
      dataSource={users}
      columns={columns}
      loading={loading}
      rowKey="id"
      pagination={{ pageSize: 10 }}
    />
  );
});
```

#### 1.2 PlantView.js Refactoring (400+ lines → 3 components < 150 lines each)

**Current Issues:**
- Massive component with workflow management, filtering, and dashboard logic
- Performance issues due to unnecessary re-renders
- Unused imports and deprecated props

**Refactored Structure:**

```javascript
// screens/PlantView.js (Main orchestrator - ~120 lines)
const PlantView = () => {
  const { workflows, loading, error } = usePlantWorkflows();
  const { filters, applyFilter, clearFilters } = useWorkflowFilters();
  
  return (
    <PlantErrorBoundary>
      <PlantDashboard 
        summary={workflows.summary}
        loading={loading}
      />
      <FilterPanel
        filters={filters}
        onApplyFilter={applyFilter}
        onClearFilters={clearFilters}
      />
      <WorkflowTable
        workflows={workflows.data}
        loading={loading}
        filters={filters}
      />
    </PlantErrorBoundary>
  );
};

// hooks/usePlantWorkflows.js
export const usePlantWorkflows = () => {
  const [workflows, setWorkflows] = useState({ data: [], summary: {} });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  
  const fetchWorkflows = useCallback(async () => {
    const controller = new AbortController();
    setLoading(true);
    
    try {
      const data = await workflowAPI.getPlantWorkflows(controller.signal);
      setWorkflows(data);
    } catch (err) {
      if (!controller.signal.aborted) {
        setError(err.message);
      }
    } finally {
      setLoading(false);
    }
    
    return () => controller.abort();
  }, []);
  
  return { workflows, loading, error, fetchWorkflows };
};
```### 2. Se
curity Enhancement Design

#### 2.1 Secure Token Storage Implementation

**Current Issue:** JWT tokens stored in plain localStorage, vulnerable to XSS attacks

**Solution:** Encrypted storage in sessionStorage with proper validation

```javascript
// services/secureStorage.js
import CryptoJS from 'crypto-js';

class SecureTokenStorage {
  private static readonly TOKEN_KEY = 'qrmfg_secure_token';
  private static readonly SECRET_KEY = process.env.REACT_APP_ENCRYPTION_KEY || 'default-key';
  
  static setToken(token: string): void {
    try {
      const encrypted = CryptoJS.AES.encrypt(token, this.SECRET_KEY).toString();
      sessionStorage.setItem(this.TOKEN_KEY, encrypted);
    } catch (error) {
      console.error('Failed to store token securely');
      throw new Error('Token storage failed');
    }
  }
  
  static getToken(): string | null {
    try {
      const encrypted = sessionStorage.getItem(this.TOKEN_KEY);
      if (!encrypted) return null;
      
      const decrypted = CryptoJS.AES.decrypt(encrypted, this.SECRET_KEY);
      return decrypted.toString(CryptoJS.enc.Utf8);
    } catch (error) {
      console.error('Failed to retrieve token');
      this.removeToken();
      return null;
    }
  }
  
  static removeToken(): void {
    sessionStorage.removeItem(this.TOKEN_KEY);
  }
  
  static hasToken(): boolean {
    return sessionStorage.getItem(this.TOKEN_KEY) !== null;
  }
}
```

#### 2.2 JWT Validation Implementation

**Current Issue:** Manual JWT parsing without validation

**Solution:** Comprehensive JWT validation with structure and expiry checks

```javascript
// services/jwtValidator.js
export class JWTValidator {
  static validateToken(token: string): boolean {
    if (!token) return false;
    
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return false;
      
      const payload = JSON.parse(atob(parts[1]));
      
      // Validate expiry
      if (payload.exp && payload.exp < Date.now() / 1000) {
        return false;
      }
      
      // Validate required claims
      return this.validateClaims(payload);
    } catch (error) {
      return false;
    }
  }
  
  private static validateClaims(payload: any): boolean {
    // Validate issuer
    if (payload.iss !== process.env.REACT_APP_JWT_ISSUER) {
      return false;
    }
    
    // Validate audience
    if (payload.aud !== process.env.REACT_APP_JWT_AUDIENCE) {
      return false;
    }
    
    // Validate required fields
    return !!(payload.sub && payload.username && payload.roles);
  }
  
  static getTokenPayload(token: string): any | null {
    if (!this.validateToken(token)) return null;
    
    try {
      const parts = token.split('.');
      return JSON.parse(atob(parts[1]));
    } catch {
      return null;
    }
  }
}
```### 
3. API Layer Standardization

#### 3.1 Unified API Client Design

**Current Issue:** Duplicate implementations (fetch + axios) with inconsistent error handling

**Solution:** Single Axios-based client with interceptors and standardized error handling

```javascript
// api/client.js
import axios from 'axios';
import { SecureTokenStorage } from '../services/secureStorage';
import { JWTValidator } from '../services/jwtValidator';

class APIClient {
  constructor() {
    this.client = axios.create({
      baseURL: process.env.REACT_APP_API_BASE_URL || '/qrmfg/api/v1',
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json'
      }
    });
    
    this.setupInterceptors();
  }
  
  setupInterceptors() {
    // Request interceptor
    this.client.interceptors.request.use(
      (config) => {
        const token = SecureTokenStorage.getToken();
        if (token && JWTValidator.validateToken(token)) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );
    
    // Response interceptor
    this.client.interceptors.response.use(
      (response) => response.data,
      (error) => {
        return Promise.reject(this.handleError(error));
      }
    );
  }
  
  handleError(error) {
    // Handle 401 Unauthorized
    if (error.response?.status === 401) {
      SecureTokenStorage.removeToken();
      window.location.href = '/qrmfg/login';
      return;
    }
    
    // Standardized error format
    return {
      message: error.response?.data?.message || error.message || 'An error occurred',
      status: error.response?.status,
      code: error.response?.data?.code,
      timestamp: new Date().toISOString()
    };
  }
  
  // HTTP methods
  get(url, config = {}) {
    return this.client.get(url, config);
  }
  
  post(url, data, config = {}) {
    return this.client.post(url, data, config);
  }
  
  put(url, data, config = {}) {
    return this.client.put(url, data, config);
  }
  
  delete(url, config = {}) {
    return this.client.delete(url, config);
  }
}

export const apiClient = new APIClient();
```### 
4. Error Boundary Architecture

#### 4.1 Specialized Error Boundaries

**Current Issue:** Not all components wrapped with error boundaries

**Solution:** Hierarchical error boundary system with specialized handlers

```javascript
// components/ErrorBoundaries/AppErrorBoundary.js
export class AppErrorBoundary extends ErrorBoundary {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }
  
  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }
  
  componentDidCatch(error, errorInfo) {
    // Report to monitoring service
    this.reportError(error, errorInfo, { level: 'application' });
  }
  
  render() {
    if (this.state.hasError) {
      return (
        <Result
          status="500"
          title="Application Error"
          subTitle="Something went wrong. Please refresh the page or contact support."
          extra={[
            <Button type="primary" onClick={() => window.location.reload()}>
              Refresh Page
            </Button>,
            <Button onClick={() => window.location.href = '/qrmfg'}>
              Go Home
            </Button>
          ]}
        />
      );
    }
    
    return this.props.children;
  }
}

// components/ErrorBoundaries/RouteErrorBoundary.js
export const RouteErrorBoundary = ({ children, routeName }) => (
  <ErrorBoundary
    title={`${routeName} Error`}
    subtitle="There was an issue loading this page."
    errorContext={{ route: routeName, level: 'route' }}
    showGoHome={true}
    showRetry={false}
  >
    {children}
  </ErrorBoundary>
);

// components/ErrorBoundaries/AsyncErrorBoundary.js
export const AsyncErrorBoundary = ({ children, onRetry }) => (
  <ErrorBoundary
    title="Loading Error"
    subtitle="Failed to load data. Please try again."
    errorContext={{ level: 'async' }}
    showGoHome={false}
    showRetry={true}
    onRetry={onRetry}
  >
    {children}
  </ErrorBoundary>
);
```

## Data Models

### 1. Component State Models

#### User Management State
```typescript
interface UserState {
  users: User[];
  loading: boolean;
  error: string | null;
  modal: {
    visible: boolean;
    user: User | null;
    mode: 'create' | 'edit';
  };
  filters: UserFilters;
  pagination: PaginationConfig;
}

interface User {
  id: string;
  username: string;
  email: string;
  roles: Role[];
  plantAssignments: PlantAssignment[];
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}
```

#### Plant Workflow State
```typescript
interface PlantWorkflowState {
  workflows: {
    data: Workflow[];
    summary: WorkflowSummary;
  };
  loading: boolean;
  error: string | null;
  filters: WorkflowFilters;
  selectedWorkflow: Workflow | null;
}

interface Workflow {
  id: string;
  projectCode: string;
  materialCode: string;
  plantCode: string;
  state: WorkflowState;
  assignedTo: string;
  dueDate: string;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
}
```## 
Error Handling

### 1. Error Classification

#### Error Types
```typescript
enum ErrorType {
  AUTHENTICATION = 'AUTHENTICATION',
  AUTHORIZATION = 'AUTHORIZATION',
  VALIDATION = 'VALIDATION',
  NETWORK = 'NETWORK',
  SERVER = 'SERVER',
  CLIENT = 'CLIENT',
  UNKNOWN = 'UNKNOWN'
}

interface ApplicationError {
  type: ErrorType;
  message: string;
  code: string;
  context: ErrorContext;
  timestamp: string;
  stack?: string;
}
```

### 2. Error Handling Strategy

#### Component Level
- **Error Boundaries**: Catch React component errors
- **Try-Catch**: Handle async operations
- **Validation**: Client-side form validation

#### Service Level
- **API Interceptors**: Handle HTTP errors
- **Retry Logic**: Automatic retry for network errors
- **Fallback Values**: Provide default values for failed requests

#### Application Level
- **Global Error Handler**: Catch unhandled errors
- **Error Reporting**: Send errors to monitoring service
- **User Notifications**: Display user-friendly error messages

### 3. Error Recovery Mechanisms

```javascript
// Automatic retry for network errors
const withRetry = async (fn, maxRetries = 3) => {
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await fn();
    } catch (error) {
      if (i === maxRetries - 1 || !isRetryableError(error)) {
        throw error;
      }
      await delay(Math.pow(2, i) * 1000); // Exponential backoff
    }
  }
};

// Graceful degradation
const withFallback = async (primaryFn, fallbackFn) => {
  try {
    return await primaryFn();
  } catch (error) {
    console.warn('Primary function failed, using fallback:', error);
    return await fallbackFn();
  }
};
```

## Testing Strategy

### 1. Testing Pyramid

#### Unit Tests (70%)
- **Components**: Test rendering, props, user interactions
- **Hooks**: Test custom hook logic and state management
- **Services**: Test API calls, error handling, data transformation
- **Utilities**: Test helper functions and validators

#### Integration Tests (20%)
- **Component Integration**: Test component interactions
- **API Integration**: Test service layer with mocked APIs
- **User Workflows**: Test complete user journeys

#### End-to-End Tests (10%)
- **Critical Paths**: Test login, user management, workflow creation
- **Cross-browser**: Test in Chrome, Firefox, Safari
- **Mobile**: Test responsive design and mobile interactions### 2.
 Testing Implementation

#### Component Testing Example
```javascript
// UserTable.test.js
describe('UserTable Component', () => {
  const mockUsers = [
    { id: '1', username: 'john', email: 'john@example.com', roles: ['USER'] }
  ];
  
  it('should display loading state while fetching users', () => {
    render(<UserTable users={[]} loading={true} />);
    expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
  });
  
  it('should display users in table format', () => {
    render(<UserTable users={mockUsers} loading={false} />);
    expect(screen.getByText('john')).toBeInTheDocument();
    expect(screen.getByText('john@example.com')).toBeInTheDocument();
  });
  
  it('should call onEdit when edit button is clicked', () => {
    const mockOnEdit = jest.fn();
    render(<UserTable users={mockUsers} onEdit={mockOnEdit} />);
    
    fireEvent.click(screen.getByTestId('edit-button-1'));
    expect(mockOnEdit).toHaveBeenCalledWith(mockUsers[0]);
  });
});
```

#### Hook Testing Example
```javascript
// useUserManagement.test.js
describe('useUserManagement Hook', () => {
  it('should fetch users on mount', async () => {
    const mockUsers = [{ id: '1', username: 'john' }];
    jest.spyOn(userAPI, 'getUsers').mockResolvedValue(mockUsers);
    
    const { result } = renderHook(() => useUserManagement());
    
    await waitFor(() => {
      expect(result.current.users).toEqual(mockUsers);
      expect(result.current.loading).toBe(false);
    });
  });
  
  it('should handle API errors gracefully', async () => {
    jest.spyOn(userAPI, 'getUsers').mockRejectedValue(new Error('API Error'));
    
    const { result } = renderHook(() => useUserManagement());
    
    await waitFor(() => {
      expect(result.current.error).toBe('API Error');
      expect(result.current.loading).toBe(false);
    });
  });
});
```

## Performance Optimization

### 1. Component Optimization

#### Memoization Strategy
```javascript
// Memoize expensive calculations
const ExpensiveComponent = React.memo(({ data, filters }) => {
  const filteredData = useMemo(() => {
    return data.filter(item => 
      filters.every(filter => filter.test(item))
    );
  }, [data, filters]);
  
  const handleAction = useCallback((id) => {
    // Handle action
  }, []);
  
  return (
    <div>
      {filteredData.map(item => (
        <Item key={item.id} data={item} onAction={handleAction} />
      ))}
    </div>
  );
});
```

#### Lazy Loading Implementation
```javascript
// Lazy load route components
const Users = lazy(() => import('./screens/Users'));
const PlantView = lazy(() => import('./screens/PlantView'));
const Dashboard = lazy(() => import('./screens/Dashboard'));

// App.js with Suspense
<Suspense fallback={<PageSkeleton />}>
  <Routes>
    <Route path="/qrmfg/users" element={<Users />} />
    <Route path="/qrmfg/plant" element={<PlantView />} />
    <Route path="/qrmfg/dashboard" element={<Dashboard />} />
  </Routes>
</Suspense>
```#
## 2. Bundle Optimization

#### Import Optimization
```javascript
// Before (imports entire library)
import * as antd from 'antd';

// After (tree-shaking friendly)
import { Button, Table, Modal, Form } from 'antd';
```

#### Code Splitting Strategy
- **Route-based**: Split by main routes
- **Component-based**: Split large components
- **Vendor-based**: Separate vendor libraries

### 3. Caching Strategy

#### API Response Caching
```javascript
// Simple in-memory cache with TTL
class APICache {
  constructor(ttl = 5 * 60 * 1000) { // 5 minutes default
    this.cache = new Map();
    this.ttl = ttl;
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
  
  set(key, data) {
    this.cache.set(key, {
      data,
      expiry: Date.now() + this.ttl
    });
  }
}
```

## Migration Strategy

### 1. Phased Implementation

#### Phase 1: Security and API Standardization (Week 1)
1. Implement SecureTokenStorage and JWTValidator
2. Create unified APIClient
3. Update auth service to use secure storage
4. Migrate critical API calls to new client

#### Phase 2: Error Boundaries and Code Quality (Week 2)
1. Implement specialized error boundaries
2. Wrap all routes with RouteErrorBoundary
3. Run ESLint fixes for unused imports and deprecated props
4. Add useEffect cleanup functions

#### Phase 3: Component Refactoring - Users.js (Week 3)
1. Extract useUserManagement hook
2. Create UserTable, UserModal, PlantAssignmentForm components
3. Implement performance optimizations
4. Add comprehensive tests

#### Phase 4: Component Refactoring - PlantView.js (Week 4)
1. Extract usePlantWorkflows and useWorkflowFilters hooks
2. Create PlantDashboard, WorkflowTable, FilterPanel components
3. Implement lazy loading and memoization
4. Add comprehensive tests

#### Phase 5: Testing and Documentation (Week 5)
1. Complete test coverage for all refactored components
2. Update documentation and architecture guides
3. Performance testing and optimization
4. Final security audit

### 2. Risk Mitigation

#### Feature Flags
```javascript
// Feature flag for gradual rollout
const useRefactoredUsers = process.env.REACT_APP_USE_REFACTORED_USERS === 'true';

const Users = () => {
  return useRefactoredUsers ? <RefactoredUsers /> : <LegacyUsers />;
};
```

#### Rollback Strategy
- Keep original components until refactored versions are stable
- Use feature flags for gradual rollout
- Monitor error rates and performance metrics
- Automated rollback triggers for critical issues

### 3. Monitoring and Validation

#### Success Metrics
- Component size reduction: 60% (300+ lines → <150 lines)
- Security audit score: >90%
- Bundle size reduction: 15%
- Error rate reduction: 80%
- Test coverage: >85%
- Page load time improvement: 25%

This design provides a comprehensive roadmap for addressing all critical issues identified in the audit while maintaining system stability and improving overall code quality, security, and performance.