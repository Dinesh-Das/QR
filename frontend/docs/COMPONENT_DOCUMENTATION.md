# Component Documentation Guide

## Overview

This document provides comprehensive documentation for all refactored components in the QRMFG frontend application. The components have been redesigned following modern React patterns with performance optimizations, accessibility features, and comprehensive error handling.

## Architecture Overview

The refactored architecture follows these principles:

- **Component Composition**: Large monolithic components have been broken down into smaller, focused components
- **Custom Hooks**: Business logic has been extracted into reusable custom hooks
- **Performance Optimization**: Components use React.memo, useMemo, and useCallback for optimal performance
- **Error Boundaries**: Comprehensive error handling with specialized error boundaries
- **Accessibility**: ARIA labels and keyboard navigation support
- **Type Safety**: PropTypes validation for all component props

## Component Categories

### 1. User Management Components

#### UserTable Component

**Location**: `src/components/User/UserTable.js`

**Purpose**: Displays users in a sortable, accessible table format with edit and delete actions.

**Key Features**:
- Performance optimized with React.memo
- Sortable columns
- Accessibility features (ARIA labels)
- Plant assignment display
- Loading states

**Props**:
```javascript
{
  users: Array<{
    id: string,
    username: string,
    email: string,
    roles: Array<string>,
    plantAssignments: Array<string>
  }>,
  loading: boolean,
  onEdit: Function,
  onDelete: Function,
  availablePlants: Array<{value: string, label: string}>
}
```

**Usage Example**:
```jsx
<UserTable
  users={users}
  loading={loading}
  onEdit={(user) => handleEdit(user)}
  onDelete={(userId) => handleDelete(userId)}
  availablePlants={plants}
/>
```

#### UserModal Component

**Location**: `src/components/User/UserModal.js`

**Purpose**: Modal dialog for creating and editing user information.

**Key Features**:
- Dual mode (create/edit)
- Form validation
- Plant assignment integration
- Error handling
- Loading states

**Props**:
```javascript
{
  visible: boolean,
  editingUser: Object|null,
  roles: Array<{value: string, label: string}>,
  onSave: Function,
  onCancel: Function,
  loading: boolean
}
```

#### PlantAssignmentForm Component

**Location**: `src/components/User/PlantAssignmentForm.js`

**Purpose**: Specialized form for managing user plant assignments.

**Key Features**:
- Multi-select plant assignment
- Validation rules
- Real-time updates
- Error handling

### 2. Plant Management Components

#### PlantDashboard Component

**Location**: `src/components/Plant/PlantDashboard.js`

**Purpose**: Dashboard view showing plant workflow statistics and summaries.

**Key Features**:
- Chart integration
- Real-time data updates
- Performance metrics
- Responsive design

#### WorkflowTable Component

**Location**: `src/components/Plant/WorkflowTable.js`

**Purpose**: Table displaying plant workflows with filtering and sorting capabilities.

**Key Features**:
- Advanced filtering
- Sorting and pagination
- Bulk operations
- Export functionality

#### FilterPanel Component

**Location**: `src/components/Plant/FilterPanel.js`

**Purpose**: Advanced filtering interface for workflow data.

**Key Features**:
- Multiple filter types
- Saved filter presets
- Real-time filtering
- Filter validation

### 3. Error Boundary Components

#### AppErrorBoundary

**Location**: `src/components/ErrorBoundaries/AppErrorBoundary.js`

**Purpose**: Application-level error boundary for catching and handling React errors.

**Features**:
- Error reporting to monitoring service
- User-friendly error messages
- Recovery options
- Error context collection

#### RouteErrorBoundary

**Location**: `src/components/ErrorBoundaries/RouteErrorBoundary.js`

**Purpose**: Route-specific error boundary with contextual error handling.

#### AsyncErrorBoundary

**Location**: `src/components/ErrorBoundaries/AsyncErrorBoundary.js`

**Purpose**: Error boundary for async operations with retry functionality.

## Custom Hooks Documentation

### useUserManagement Hook

**Location**: `src/hooks/useUserManagement.js`

**Purpose**: Centralized user management logic with CRUD operations.

**Returns**:
```javascript
{
  users: Array<Object>,
  loading: boolean,
  error: string|null,
  modalVisible: boolean,
  editingUser: Object|null,
  actions: {
    fetchUsers: Function,
    saveUser: Function,
    deleteUser: Function,
    openEditModal: Function,
    openCreateModal: Function,
    closeModal: Function,
    assignPlantToUser: Function,
    removePlantFromUser: Function
  }
}
```

### usePlantWorkflows Hook

**Location**: `src/hooks/usePlantWorkflows.js`

**Purpose**: Plant workflow data management with filtering and caching.

### useWorkflowFilters Hook

**Location**: `src/hooks/useWorkflowFilters.js`

**Purpose**: Advanced filtering logic for workflow data.

### usePlantAssignment Hook

**Location**: `src/hooks/usePlantAssignment.js`

**Purpose**: Plant assignment operations and validation.

## Integration Patterns

### Component Integration

```jsx
// Main screen component orchestrating child components
const Users = () => {
  const { users, loading, error, actions } = useUserManagement();
  const { plantAssignments } = usePlantAssignment();
  
  return (
    <UserErrorBoundary>
      <UserTable 
        users={users}
        loading={loading}
        onEdit={actions.openEditModal}
        onDelete={actions.deleteUser}
      />
      <UserModal 
        visible={modalState.visible}
        editingUser={modalState.user}
        onSave={actions.saveUser}
        onCancel={actions.closeModal}
      />
    </UserErrorBoundary>
  );
};
```

### Error Boundary Integration

```jsx
// Wrapping components with appropriate error boundaries
<AppErrorBoundary>
  <Routes>
    <Route path="/users" element={
      <RouteErrorBoundary routeName="Users">
        <Users />
      </RouteErrorBoundary>
    } />
  </Routes>
</AppErrorBoundary>
```

### Hook Integration

```jsx
// Using multiple hooks together
const PlantView = () => {
  const { workflows, loading } = usePlantWorkflows();
  const { filters, applyFilter } = useWorkflowFilters();
  
  const filteredWorkflows = useMemo(() => 
    workflows.filter(workflow => filters.test(workflow)),
    [workflows, filters]
  );
  
  return (
    <PlantDashboard workflows={filteredWorkflows} />
  );
};
```

## Performance Considerations

### Memoization Strategy

1. **React.memo**: All functional components are wrapped with React.memo
2. **useMemo**: Expensive calculations and data transformations
3. **useCallback**: Event handlers passed to child components
4. **Proper Dependencies**: All hooks have correct dependency arrays

### Code Splitting

```jsx
// Lazy loading for route components
const Users = lazy(() => import('./screens/Users'));
const PlantView = lazy(() => import('./screens/PlantView'));

// Usage with Suspense
<Suspense fallback={<PageSkeleton />}>
  <Routes>
    <Route path="/users" element={<Users />} />
    <Route path="/plant" element={<PlantView />} />
  </Routes>
</Suspense>
```

## Testing Guidelines

### Component Testing

```javascript
// Example test for UserTable component
describe('UserTable Component', () => {
  const mockUsers = [
    { id: '1', username: 'john', email: 'john@example.com', roles: ['USER'] }
  ];
  
  it('should display users in table format', () => {
    render(<UserTable users={mockUsers} loading={false} />);
    expect(screen.getByText('john')).toBeInTheDocument();
  });
  
  it('should call onEdit when edit button is clicked', () => {
    const mockOnEdit = jest.fn();
    render(<UserTable users={mockUsers} onEdit={mockOnEdit} />);
    
    fireEvent.click(screen.getByTestId('edit-button-1'));
    expect(mockOnEdit).toHaveBeenCalledWith(mockUsers[0]);
  });
});
```

### Hook Testing

```javascript
// Example test for useUserManagement hook
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
});
```

## Accessibility Features

### ARIA Labels

All interactive elements include appropriate ARIA labels:

```jsx
<Button 
  aria-label={`Edit user ${user.username}`}
  onClick={() => onEdit(user)}
>
  <EditOutlined />
</Button>
```

### Keyboard Navigation

Components support keyboard navigation:

```jsx
<Table
  onRow={(record) => ({
    onKeyDown: (event) => {
      if (event.key === 'Enter') {
        onEdit(record);
      }
    },
    tabIndex: 0
  })}
/>
```

## Migration Guide

### From Legacy Components

1. **Identify Dependencies**: Check what data and functions the legacy component uses
2. **Extract Business Logic**: Move business logic to custom hooks
3. **Split UI Components**: Break down large components into smaller ones
4. **Add Error Boundaries**: Wrap components with appropriate error boundaries
5. **Optimize Performance**: Add memoization where appropriate
6. **Add Tests**: Write comprehensive tests for new components

### Example Migration

```jsx
// Before: Large monolithic component
const Users = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  // ... 300+ lines of mixed logic
};

// After: Orchestrator component with hooks and child components
const Users = () => {
  const { users, loading, actions } = useUserManagement();
  
  return (
    <UserErrorBoundary>
      <UserTable users={users} loading={loading} onEdit={actions.openEditModal} />
      <UserModal visible={modalVisible} onSave={actions.saveUser} />
    </UserErrorBoundary>
  );
};
```

## Best Practices

1. **Component Size**: Keep components under 150 lines
2. **Single Responsibility**: Each component should have one clear purpose
3. **Props Validation**: Always use PropTypes for prop validation
4. **Error Handling**: Wrap components with appropriate error boundaries
5. **Performance**: Use memoization for expensive operations
6. **Accessibility**: Include ARIA labels and keyboard support
7. **Testing**: Write tests for all components and hooks
8. **Documentation**: Document all props, methods, and usage examples

## Troubleshooting

### Common Issues

1. **Performance Issues**: Check for missing memoization or incorrect dependencies
2. **Error Boundaries Not Catching**: Ensure error boundaries are properly placed
3. **Props Not Updating**: Verify prop drilling and state management
4. **Memory Leaks**: Check for proper cleanup in useEffect hooks

### Debugging Tools

1. **React DevTools**: Use for component inspection and profiling
2. **ESLint**: Catches common React issues and enforces best practices
3. **Testing Library**: Provides utilities for testing component behavior
4. **Chrome DevTools**: Performance profiling and memory analysis

## Future Enhancements

1. **TypeScript Migration**: Convert components to TypeScript for better type safety
2. **Storybook Integration**: Add Storybook for component documentation and testing
3. **Automated Testing**: Increase test coverage and add visual regression tests
4. **Performance Monitoring**: Add performance metrics and monitoring
5. **Internationalization**: Add i18n support for multi-language applications