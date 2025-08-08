# Role-Based Access Control (RBAC) Implementation

This document describes the comprehensive RBAC system implemented for the frontend application, which integrates with the backend RBAC service.

## Overview

The RBAC system provides fine-grained access control based on user roles, supporting:
- Screen/route-based access control
- Data-type access control
- Plant-specific data filtering
- Component-level conditional rendering
- Dynamic access decisions

## Role Structure

The system supports the following roles (matching backend `RoleConstants.java`):

- **ADMIN**: Full system access with administrative privileges
- **JVC_USER**: Joint Venture Company user with workflow initiation rights
- **CQS_USER**: Corporate Quality Services user with query management rights
- **TECH_USER**: Technical user with system monitoring and audit access
- **PLANT_USER**: Plant-specific user with questionnaire completion rights
- **VIEWER**: Read-only access to assigned content

## Key Components

### 1. Enhanced Authentication Service (`services/auth.js`)

Extended with role-based functions:
```javascript
// Role checks
export const isAdmin = () => hasRole('ADMIN');
export const isJvcUser = () => hasRole('JVC_USER') || hasRole('JVC_ROLE');
export const isCqsUser = () => hasRole('CQS_USER') || hasRole('CQS_ROLE');
export const isTechUser = () => hasRole('TECH_USER') || hasRole('TECH_ROLE');
export const isPlantUser = () => hasRole('PLANT_USER') || hasRole('PLANT_ROLE');

// Screen access
export const hasScreenAccess = (screenRoute) => { /* ... */ };
export const getAccessibleScreens = () => { /* ... */ };

// Plant access
export const getUserPlantCodes = () => { /* ... */ };
export const getPrimaryPlantCode = () => { /* ... */ };
```

### 2. RBAC Service (`services/rbacService.js`)

Integrates with backend RBAC endpoints:
```javascript
class RBACService {
  static async getUserAccessSummary() { /* ... */ }
  static async hasScreenAccess(screenRoute) { /* ... */ }
  static async hasDataAccess(dataType, context) { /* ... */ }
  static async hasPlantDataAccess(dataType, plantCode, context) { /* ... */ }
  static async makeAccessDecision(resourceType, resourceId, action, context) { /* ... */ }
  static filterDataByPlantAccess(data, plantExtractor) { /* ... */ }
}
```

### 3. Role-Based Components (`components/RoleBasedComponent.js`)

Conditional rendering based on roles:
```javascript
// Generic component
<RoleBasedComponent roles={['ADMIN', 'TECH_USER']}>
  <AdminFeatures />
</RoleBasedComponent>

// Convenience components
<AdminOnly><AdminPanel /></AdminOnly>
<JvcOnly><JVCDashboard /></JvcOnly>
<CqsOnly><CQSDashboard /></CqsOnly>
<TechOnly><TechDashboard /></TechOnly>
<PlantOnly><PlantDashboard /></PlantOnly>
<NonViewerOnly><EditFeatures /></NonViewerOnly>
```

### 4. RBAC Hooks (`hooks/useRoleBasedAccess.js`)

React hooks for role-based logic:
```javascript
const {
  isAuthenticated,
  currentUser,
  primaryRole,
  userPlants,
  isAdmin,
  isJvcUser,
  // ... other role checks
  checkScreenAccess,
  checkDataAccess,
  filterByPlantAccess,
  accessSummary
} = useRoleBasedAccess();

// Specific hooks
const { hasAccess, loading } = useScreenAccess('/admin');
const { hasAccess } = useDataAccess('workflow');
const filteredData = usePlantDataFilter(data, item => item.plantCode);
```

### 5. Enhanced Protected Route (`components/ProtectedRoute.js`)

Route-level access control:
```javascript
<ProtectedRoute requiredRole="ADMIN">
  <AdminPanel />
</ProtectedRoute>
```

### 6. Role-Based Navigation (`components/Navigation.js`)

Dynamic navigation based on user roles:
- Fetches accessible screens from backend
- Shows role-appropriate menu items
- Displays user role information
- Provides admin indicators

## Screen Access Mapping

```javascript
const SCREEN_ACCESS = {
  [ROLES.ADMIN]: ['*'], // All screens
  [ROLES.JVC_USER]: ['/qrmfg', '/qrmfg/dashboard', '/qrmfg/jvc', '/qrmfg/workflows', '/qrmfg/reports'],
  [ROLES.CQS_USER]: ['/qrmfg', '/qrmfg/dashboard', '/qrmfg/cqs', '/qrmfg/workflows', '/qrmfg/reports'],
  [ROLES.TECH_USER]: ['/qrmfg', '/qrmfg/dashboard', '/qrmfg/tech', '/qrmfg/workflows', '/qrmfg/workflow-monitoring', '/qrmfg/auditlogs'],
  [ROLES.PLANT_USER]: ['/qrmfg', '/qrmfg/dashboard', '/qrmfg/plant', '/qrmfg/workflows'],
  [ROLES.VIEWER]: ['/qrmfg', '/qrmfg/dashboard']
};
```

## Data Access Control

### Plant-Based Filtering

For plant users, data is automatically filtered based on assigned plants:
```javascript
const filteredWorkflows = filterByPlantAccess(
  workflows, 
  workflow => workflow.plantCode
);
```

### Data Type Access

Different roles have access to different data types:
```javascript
const DATA_ACCESS = {
  [ROLES.ADMIN]: ['*'],
  [ROLES.JVC_USER]: ['workflow', 'document', 'query', 'jvc'],
  [ROLES.CQS_USER]: ['workflow', 'document', 'query', 'cqs'],
  [ROLES.TECH_USER]: ['workflow', 'document', 'query', 'audit', 'system'],
  [ROLES.PLANT_USER]: ['workflow', 'document', 'query', 'plant'],
  [ROLES.VIEWER]: ['workflow:read', 'document:read']
};
```

## Usage Examples

### 1. Role-Based Screen Components

```javascript
// JVC View with role protection
const JVCView = () => {
  return (
    <JvcOnly>
      <div>
        <Title>JVC Dashboard</Title>
        {/* JVC-specific content */}
      </div>
    </JvcOnly>
  );
};
```

### 2. Conditional Feature Rendering

```javascript
const Dashboard = () => {
  const { isAdmin, isTechUser } = useRoleBasedAccess();
  
  return (
    <div>
      <Title>Dashboard</Title>
      
      <AdminOnly>
        <AdminMetrics />
      </AdminOnly>
      
      {(isAdmin || isTechUser) && (
        <SystemMonitoring />
      )}
      
      <RoleBasedComponent roles={['JVC_USER', 'CQS_USER']}>
        <WorkflowMetrics />
      </RoleBasedComponent>
    </div>
  );
};
```

### 3. Dynamic Access Checks

```javascript
const WorkflowList = () => {
  const { checkDataAccess, filterByPlantAccess } = useRoleBasedAccess();
  const [workflows, setWorkflows] = useState([]);
  
  useEffect(() => {
    const loadWorkflows = async () => {
      const hasAccess = await checkDataAccess('workflow');
      if (hasAccess) {
        const data = await fetchWorkflows();
        const filtered = filterByPlantAccess(data, w => w.plantCode);
        setWorkflows(filtered);
      }
    };
    
    loadWorkflows();
  }, []);
  
  return <WorkflowTable data={workflows} />;
};
```

### 4. Plant-Specific Data Handling

```javascript
const PlantDashboard = () => {
  const { userPlants, primaryPlant, isPlantUser } = useRoleBasedAccess();
  
  if (!isPlantUser) {
    return <AccessDenied />;
  }
  
  return (
    <div>
      <Title>Plant Dashboard - {primaryPlant}</Title>
      <PlantSelector plants={userPlants} />
      {/* Plant-specific content */}
    </div>
  );
};
```

## Backend Integration

The frontend RBAC system integrates with backend endpoints:

- `GET /rbac/user/access-summary` - Get user's complete access summary
- `GET /rbac/screen-access?route={route}` - Check screen access
- `GET /rbac/accessible-screens` - Get list of accessible screens
- `POST /rbac/data-access` - Check data type access
- `POST /rbac/plant-data-access` - Check plant-specific data access
- `POST /rbac/access-decision` - Make comprehensive access decision

## Fallback Behavior

The system includes comprehensive fallback behavior when backend services are unavailable:
- Client-side role validation using JWT token
- Cached access rules
- Graceful degradation with logging

## Security Considerations

1. **Token Validation**: All role checks validate JWT token expiry and structure
2. **Backend Verification**: Critical access decisions are verified with backend
3. **Fallback Security**: Fallback behavior is restrictive (deny by default)
4. **Audit Logging**: Access attempts are logged for security monitoring
5. **Plant Isolation**: Plant users can only access their assigned plant data

## Testing

Use the `RBACDemo` component to test and demonstrate RBAC functionality:
```javascript
import RBACDemo from '../components/RBACDemo';

// Add to your routes for testing
<Route path="/rbac-demo" element={<RBACDemo />} />
```

## Migration Guide

To update existing components to use RBAC:

1. **Wrap role-specific screens**:
   ```javascript
   // Before
   const AdminPanel = () => <div>Admin content</div>;
   
   // After
   const AdminPanel = () => (
     <AdminOnly>
       <div>Admin content</div>
     </AdminOnly>
   );
   ```

2. **Replace manual role checks**:
   ```javascript
   // Before
   const isAdmin = localStorage.getItem('userRole') === 'ADMIN';
   
   // After
   const { isAdmin } = useRoleBasedAccess();
   ```

3. **Add plant filtering**:
   ```javascript
   // Before
   const workflows = await fetchWorkflows();
   
   // After
   const { filterByPlantAccess } = useRoleBasedAccess();
   const allWorkflows = await fetchWorkflows();
   const workflows = filterByPlantAccess(allWorkflows, w => w.plantCode);
   ```

This RBAC implementation provides a robust, scalable, and secure foundation for role-based access control in the frontend application.