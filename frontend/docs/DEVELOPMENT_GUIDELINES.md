# QRMFG Frontend Development Guidelines

## 🎯 Overview

This document outlines the development standards, patterns, and best practices for the QRMFG frontend application. These guidelines ensure consistency, maintainability, and quality across the codebase.

## 📁 Project Structure

### Directory Organization

```
src/
├── api/                    # API client and configuration
│   └── client.js          # Unified API client
├── components/             # Reusable UI components
│   ├── ErrorBoundaries/   # Error boundary components
│   ├── User/              # User-related components
│   ├── Plant/             # Plant-related components
│   └── common/            # Common/shared components
├── hooks/                 # Custom React hooks
│   ├── useUserManagement.js
│   ├── usePlantWorkflows.js
│   └── ...
├── screens/               # Page-level components
│   ├── Users.js
│   ├── PlantView.js
│   └── ...
├── services/              # Business logic and API services
│   ├── auth.js
│   ├── userAPI.js
│   └── ...
├── constants/             # Application constants
│   └── index.js
├── utils/                 # Utility functions
└── styles/                # Global styles and themes
```

### File Naming Conventions

- **Components**: PascalCase (e.g., `UserTable.js`, `PlantDashboard.js`)
- **Hooks**: camelCase with "use" prefix (e.g., `useUserManagement.js`)
- **Services**: camelCase (e.g., `userAPI.js`, `workflowAPI.js`)
- **Utilities**: camelCase (e.g., `dateUtils.js`, `validationUtils.js`)
- **Constants**: camelCase (e.g., `apiConstants.js`, `uiConstants.js`)

## 🧩 Component Development

### Component Structure

```javascript
// 1. Imports (grouped and ordered)
import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { Button, Table, Modal } from 'antd';
import PropTypes from 'prop-types';

import { userAPI } from '../../services/userAPI';
import { formatDate } from '../../utils/dateUtils';

// 2. Component definition with JSDoc
/**
 * UserTable Component
 * 
 * Displays users in a sortable, accessible table format.
 * 
 * @param {Object} props - Component props
 * @param {Array} props.users - Array of user objects
 * @param {boolean} props.loading - Loading state
 * @param {Function} props.onEdit - Edit callback
 */
const UserTable = React.memo(({ users, loading, onEdit }) => {
  // 3. State declarations
  const [selectedUsers, setSelectedUsers] = useState([]);
  
  // 4. Memoized values
  const sortedUsers = useMemo(() => 
    users.sort((a, b) => a.username.localeCompare(b.username)),
    [users]
  );
  
  // 5. Callback functions
  const handleEdit = useCallback((user) => {
    onEdit(user);
  }, [onEdit]);
  
  // 6. Effects
  useEffect(() => {
    // Effect logic
  }, []);
  
  // 7. Render
  return (
    <Table
      dataSource={sortedUsers}
      loading={loading}
      rowKey="id"
    />
  );
});

// 8. PropTypes
UserTable.propTypes = {
  users: PropTypes.arrayOf(PropTypes.object).isRequired,
  loading: PropTypes.bool,
  onEdit: PropTypes.func.isRequired
};

// 9. Default props
UserTable.defaultProps = {
  loading: false
};

// 10. Display name
UserTable.displayName = 'UserTable';

// 11. Export
export default UserTable;
```

### Component Guidelines

#### Size Limits
- **Maximum 150 lines** per component
- If larger, split into smaller components or extract logic to hooks

#### Single Responsibility
- Each component should have one clear purpose
- Separate concerns (UI, business logic, data fetching)

#### Performance Optimization
```javascript
// Use React.memo for components that receive stable props
const UserTable = React.memo(({ users, onEdit }) => {
  // Use useMemo for expensive calculations
  const processedUsers = useMemo(() => 
    users.map(user => ({ ...user, displayName: `${user.firstName} ${user.lastName}` })),
    [users]
  );
  
  // Use useCallback for event handlers passed to children
  const handleEdit = useCallback((user) => {
    onEdit(user);
  }, [onEdit]);
  
  return <Table dataSource={processedUsers} />;
});
```

## 🎣 Custom Hooks

### Hook Structure

```javascript
/**
 * Custom hook for user management operations
 * 
 * @returns {Object} User management state and actions
 */
export const useUserManagement = () => {
  const [state, dispatch] = useReducer(userReducer, initialState);
  
  // Memoized actions
  const actions = useMemo(() => ({
    fetchUsers: useCallback(async () => {
      // Implementation
    }, []),
    
    saveUser: useCallback(async (userData) => {
      // Implementation
    }, []),
    
    deleteUser: useCallback(async (userId) => {
      // Implementation
    }, [])
  }), []);
  
  return {
    ...state,
    actions
  };
};
```

### Hook Guidelines

- **Single Responsibility**: Each hook should manage one concern
- **Memoization**: Use useCallback and useMemo appropriately
- **Error Handling**: Include comprehensive error handling
- **Cleanup**: Provide cleanup for subscriptions and timers
- **Testing**: Write tests for all custom hooks

## 🔒 Security Best Practices

### Authentication

```javascript
// Secure token storage
import { SecureTokenStorage } from '../services/secureStorage';
import { JWTValidator } from '../services/jwtValidator';

// Always validate tokens before use
const token = SecureTokenStorage.getToken();
if (token && JWTValidator.validateToken(token)) {
  // Use token
} else {
  // Redirect to login
}
```

### Input Validation

```javascript
// Form validation example
const validateUserInput = (userData) => {
  const errors = {};
  
  // Username validation
  if (!userData.username || userData.username.length < 3) {
    errors.username = 'Username must be at least 3 characters';
  }
  
  // Email validation
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(userData.email)) {
    errors.email = 'Invalid email format';
  }
  
  return errors;
};
```

### XSS Prevention

```javascript
// Sanitize user input
import DOMPurify from 'dompurify';

const sanitizeInput = (input) => {
  return DOMPurify.sanitize(input);
};

// Use dangerouslySetInnerHTML carefully
const SafeHTML = ({ content }) => (
  <div dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(content) }} />
);
```

## ⚡ Performance Guidelines

### Bundle Optimization

```javascript
// Use named imports instead of default imports
import { Button, Table, Modal } from 'antd';

// Avoid wildcard imports
// ❌ Don't do this
import * as antd from 'antd';

// ✅ Do this instead
import { Button, Table } from 'antd';
```

### Code Splitting

```javascript
// Route-based code splitting
import { lazy, Suspense } from 'react';

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

### API Optimization

```javascript
// Use AbortController for cancellable requests
const useApiCall = () => {
  useEffect(() => {
    const controller = new AbortController();
    
    const fetchData = async () => {
      try {
        const data = await apiClient.get('/users', { 
          signal: controller.signal 
        });
        setData(data);
      } catch (error) {
        if (!controller.signal.aborted) {
          setError(error.message);
        }
      }
    };
    
    fetchData();
    
    return () => controller.abort();
  }, []);
};
```

## ♿ Accessibility Guidelines

### ARIA Labels

```javascript
// Provide meaningful ARIA labels
<Button 
  aria-label={`Edit user ${user.username}`}
  onClick={() => onEdit(user)}
>
  <EditOutlined />
</Button>

// Use ARIA roles for custom components
<div role="tablist">
  <button role="tab" aria-selected={isSelected}>
    Tab 1
  </button>
</div>
```

### Keyboard Navigation

```javascript
// Handle keyboard events
const handleKeyDown = (event) => {
  switch (event.key) {
    case 'Enter':
    case ' ':
      event.preventDefault();
      handleClick();
      break;
    case 'Escape':
      handleClose();
      break;
  }
};

<div 
  role="button"
  tabIndex={0}
  onKeyDown={handleKeyDown}
  onClick={handleClick}
>
  Custom Button
</div>
```

### Focus Management

```javascript
// Manage focus in modals
const Modal = ({ visible, onClose }) => {
  const modalRef = useRef();
  
  useEffect(() => {
    if (visible && modalRef.current) {
      modalRef.current.focus();
    }
  }, [visible]);
  
  return (
    <div 
      ref={modalRef}
      role="dialog"
      aria-modal="true"
      tabIndex={-1}
    >
      Modal content
    </div>
  );
};
```

## 🧪 Testing Guidelines

### Component Testing

```javascript
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UserTable } from './UserTable';

describe('UserTable Component', () => {
  const mockUsers = [
    { id: '1', username: 'john', email: 'john@example.com' }
  ];
  
  it('should display users in table format', () => {
    render(<UserTable users={mockUsers} />);
    
    expect(screen.getByText('john')).toBeInTheDocument();
    expect(screen.getByText('john@example.com')).toBeInTheDocument();
  });
  
  it('should call onEdit when edit button is clicked', async () => {
    const mockOnEdit = jest.fn();
    const user = userEvent.setup();
    
    render(<UserTable users={mockUsers} onEdit={mockOnEdit} />);
    
    await user.click(screen.getByRole('button', { name: /edit user john/i }));
    
    expect(mockOnEdit).toHaveBeenCalledWith(mockUsers[0]);
  });
});
```

### Hook Testing

```javascript
import { renderHook, act } from '@testing-library/react';
import { useUserManagement } from './useUserManagement';

describe('useUserManagement Hook', () => {
  it('should fetch users on mount', async () => {
    const mockUsers = [{ id: '1', username: 'john' }];
    jest.spyOn(userAPI, 'getUsers').mockResolvedValue(mockUsers);
    
    const { result } = renderHook(() => useUserManagement());
    
    await act(async () => {
      await result.current.actions.fetchUsers();
    });
    
    expect(result.current.users).toEqual(mockUsers);
    expect(result.current.loading).toBe(false);
  });
});
```

## 🎨 Styling Guidelines

### CSS-in-JS vs CSS Modules

```javascript
// Prefer CSS modules for component-specific styles
import styles from './UserTable.module.css';

const UserTable = () => (
  <div className={styles.container}>
    <table className={styles.table}>
      {/* Table content */}
    </table>
  </div>
);

// Use styled-components for dynamic styles
import styled from 'styled-components';

const StyledButton = styled(Button)`
  background-color: ${props => props.primary ? '#1890ff' : '#f0f0f0'};
  border-color: ${props => props.primary ? '#1890ff' : '#d9d9d9'};
`;
```

### Responsive Design

```css
/* Use CSS Grid and Flexbox for layouts */
.container {
  display: grid;
  grid-template-columns: 1fr 3fr;
  gap: 1rem;
}

/* Mobile-first responsive design */
@media (max-width: 768px) {
  .container {
    grid-template-columns: 1fr;
  }
}
```

## 🔧 Error Handling

### Error Boundaries

```javascript
// Specialized error boundary for user components
class UserErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }
  
  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }
  
  componentDidCatch(error, errorInfo) {
    // Log error to monitoring service
    errorReporting.logError(error, {
      component: 'UserErrorBoundary',
      errorInfo
    });
  }
  
  render() {
    if (this.state.hasError) {
      return (
        <Result
          status="error"
          title="User Management Error"
          subTitle="Something went wrong with user management."
          extra={[
            <Button type="primary" onClick={() => window.location.reload()}>
              Refresh Page
            </Button>
          ]}
        />
      );
    }
    
    return this.props.children;
  }
}
```

### Async Error Handling

```javascript
// Consistent error handling pattern
const useAsyncOperation = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  
  const executeOperation = useCallback(async (operation) => {
    setLoading(true);
    setError(null);
    
    try {
      const result = await operation();
      return result;
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);
  
  return { executeOperation, loading, error };
};
```

## 📊 State Management

### Local State

```javascript
// Use useState for simple local state
const [isVisible, setIsVisible] = useState(false);
const [formData, setFormData] = useState({});

// Use useReducer for complex state
const userReducer = (state, action) => {
  switch (action.type) {
    case 'SET_USERS':
      return { ...state, users: action.payload };
    case 'SET_LOADING':
      return { ...state, loading: action.payload };
    default:
      return state;
  }
};

const [state, dispatch] = useReducer(userReducer, initialState);
```

### Global State

```javascript
// Use Context for global state that doesn't change often
const ThemeContext = createContext();

export const ThemeProvider = ({ children }) => {
  const [theme, setTheme] = useState('light');
  
  return (
    <ThemeContext.Provider value={{ theme, setTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};

// Use external state management for complex global state
// (Redux, Zustand, etc.)
```

## 🚀 Deployment Guidelines

### Environment Configuration

```javascript
// Use environment variables for configuration
const config = {
  apiBaseUrl: process.env.REACT_APP_API_BASE_URL,
  jwtIssuer: process.env.REACT_APP_JWT_ISSUER,
  encryptionKey: process.env.REACT_APP_ENCRYPTION_KEY,
  environment: process.env.NODE_ENV
};

// Validate required environment variables
const requiredEnvVars = ['REACT_APP_API_BASE_URL', 'REACT_APP_JWT_ISSUER'];
requiredEnvVars.forEach(envVar => {
  if (!process.env[envVar]) {
    throw new Error(`Missing required environment variable: ${envVar}`);
  }
});
```

### Build Optimization

```javascript
// webpack.config.js optimizations
module.exports = {
  optimization: {
    splitChunks: {
      chunks: 'all',
      cacheGroups: {
        vendor: {
          test: /[\\/]node_modules[\\/]/,
          name: 'vendors',
          chunks: 'all',
        },
      },
    },
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
};
```

## 📝 Documentation Standards

### JSDoc Comments

```javascript
/**
 * Calculates the total price including tax
 * 
 * @param {number} basePrice - The base price before tax
 * @param {number} taxRate - The tax rate as a decimal (e.g., 0.08 for 8%)
 * @param {Object} options - Additional options
 * @param {boolean} options.roundToNearestCent - Whether to round to nearest cent
 * @returns {number} The total price including tax
 * 
 * @example
 * const total = calculateTotalPrice(100, 0.08, { roundToNearestCent: true });
 * console.log(total); // 108.00
 */
const calculateTotalPrice = (basePrice, taxRate, options = {}) => {
  const total = basePrice * (1 + taxRate);
  return options.roundToNearestCent ? Math.round(total * 100) / 100 : total;
};
```

### README Structure

```markdown
# Component Name

Brief description of what the component does.

## Usage

```jsx
<ComponentName
  prop1="value1"
  prop2={value2}
  onAction={handleAction}
/>
```

## Props

| Prop | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| prop1 | string | Yes | - | Description of prop1 |
| prop2 | number | No | 0 | Description of prop2 |

## Examples

### Basic Usage
[Example code]

### Advanced Usage
[Example code]

## Testing

[Testing instructions]

## Notes

[Additional notes or considerations]
```

## 🔄 Git Workflow

### Commit Messages

Follow conventional commit format:

```
type(scope): description

[optional body]

[optional footer]
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

Examples:
```
feat(user): add user deletion functionality
fix(auth): resolve token validation issue
docs(readme): update installation instructions
```

### Branch Naming

```
feature/user-management-refactor
bugfix/auth-token-validation
hotfix/security-vulnerability
chore/update-dependencies
```

### Pull Request Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No console.log statements
```

## 🎯 Code Quality Metrics

### Automated Checks

- **ESLint**: No errors, warnings under 10
- **Prettier**: All files formatted
- **Test Coverage**: Minimum 80%
- **Bundle Size**: Under 2MB
- **Performance**: Lighthouse score >90

### Manual Review

- **Code Readability**: Clear and understandable
- **Component Size**: Under 150 lines
- **Function Complexity**: Cyclomatic complexity <10
- **Documentation**: Adequate JSDoc comments
- **Accessibility**: WCAG 2.1 AA compliance

## 🚨 Common Pitfalls

### Performance Anti-patterns

```javascript
// ❌ Don't do this - creates new object on every render
<Component style={{ marginTop: 10 }} />

// ✅ Do this instead
const styles = { marginTop: 10 };
<Component style={styles} />

// ❌ Don't do this - missing dependencies
useEffect(() => {
  fetchData(userId);
}, []); // Missing userId dependency

// ✅ Do this instead
useEffect(() => {
  fetchData(userId);
}, [userId]);
```

### Security Anti-patterns

```javascript
// ❌ Don't do this - XSS vulnerability
<div dangerouslySetInnerHTML={{ __html: userInput }} />

// ✅ Do this instead
<div dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(userInput) }} />

// ❌ Don't do this - hardcoded secrets
const API_KEY = 'sk-1234567890abcdef';

// ✅ Do this instead
const API_KEY = process.env.REACT_APP_API_KEY;
```

### Accessibility Anti-patterns

```javascript
// ❌ Don't do this - missing accessibility
<div onClick={handleClick}>Click me</div>

// ✅ Do this instead
<button onClick={handleClick} aria-label="Perform action">
  Click me
</button>
```

## 📚 Learning Resources

### Required Reading
- [React Documentation](https://reactjs.org/docs)
- [Ant Design Documentation](https://ant.design/docs/react/introduce)
- [Web Content Accessibility Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
- [JavaScript Security Best Practices](https://owasp.org/www-project-top-ten/)

### Recommended Tools
- **VS Code Extensions**: ES7+ React/Redux/React-Native snippets, Prettier, ESLint
- **Browser Extensions**: React Developer Tools, axe DevTools
- **Testing Tools**: React Testing Library, Jest, Cypress
- **Performance Tools**: Lighthouse, React DevTools Profiler

### Team Resources
- **Slack**: #frontend-dev for questions
- **Wiki**: Internal documentation and guides
- **Code Reviews**: Learn from peer feedback
- **Pair Programming**: Collaborate on complex features

---

## 📞 Questions or Feedback?

These guidelines are living documents that evolve with our team and technology. If you have suggestions for improvements or questions about any guidelines:

- **Slack**: #frontend-dev channel
- **Email**: frontend-team@qrmfg.com
- **Weekly Sync**: Bring up during team meetings
- **Documentation**: Update this document via PR

Remember: Guidelines should enable great development, not hinder it. When in doubt, prioritize code clarity, user experience, and team collaboration.