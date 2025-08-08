# QRMFG Frontend Application

A modern, secure, and performant React application for Quality Resource Management and Facility Governance (QRMFG). Built with React 18, Ant Design 5, and following enterprise-grade development practices.

## 🏗️ Architecture Overview

The application follows a component-based architecture with the following key principles:

- **Modular Components**: Large monolithic components have been refactored into smaller, focused components
- **Custom Hooks**: Business logic is extracted into reusable custom hooks
- **Error Boundaries**: Comprehensive error handling with specialized error boundaries
- **Performance Optimization**: React.memo, useMemo, and useCallback for optimal performance
- **Security First**: Secure JWT token management with encryption and validation
- **Accessibility**: ARIA labels, keyboard navigation, and screen reader support

### Directory Structure

```
src/
├── api/                    # Unified API client
├── components/             # Reusable UI components
│   ├── ErrorBoundaries/   # Specialized error boundaries
│   ├── User/              # User management components
│   ├── Plant/             # Plant management components
│   └── ...
├── hooks/                 # Custom React hooks
├── screens/               # Page-level components
├── services/              # Business logic and API services
├── constants/             # Application constants
└── utils/                 # Utility functions
```

## 🚀 Features

### Core Functionality
- **User Management**: Complete CRUD operations with role-based access control
- **Plant Workflow Management**: Advanced workflow tracking and management
- **Document Management**: Secure document upload and management
- **Query System**: Comprehensive query raising and tracking system
- **Audit Logging**: Complete audit trail for all user actions
- **Real-time Notifications**: WebSocket-based real-time updates

### Security Features
- **Secure Authentication**: JWT tokens with AES encryption
- **Token Validation**: Comprehensive JWT validation with expiry checking
- **XSS Protection**: Input sanitization and validation
- **CSRF Protection**: Cross-site request forgery protection
- **Role-based Access**: Granular permission system

### Performance Features
- **Code Splitting**: Route-based lazy loading
- **Memoization**: Optimized re-rendering with React.memo
- **API Caching**: Intelligent caching with TTL
- **Bundle Optimization**: Tree shaking and optimized imports

## 🛠️ Development Setup

### Prerequisites
- Node.js 18.x or 20.x
- npm 8.x or higher

### Installation

1. **Clone and navigate to frontend directory**
   ```bash
   cd frontend
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Set up environment variables**
   ```bash
   cp .env.development.example .env.development
   ```
   
   Configure the following variables:
   ```env
   REACT_APP_API_BASE_URL=http://localhost:8080/qrmfg/api/v1
   REACT_APP_JWT_ISSUER=qrmfg-system
   REACT_APP_JWT_AUDIENCE=qrmfg-frontend
   REACT_APP_ENCRYPTION_KEY=your-encryption-key
   ```

4. **Start development server**
   ```bash
   npm start
   ```

The application will be available at `http://localhost:3000/qrmfg`

## 🧪 Testing

### Running Tests

```bash
# Run all tests
npm test

# Run tests with coverage
npm test -- --coverage

# Run tests in watch mode
npm test -- --watch
```

### Test Structure

- **Unit Tests**: Component and hook testing with React Testing Library
- **Integration Tests**: API integration and user workflow testing
- **E2E Tests**: Complete user journey testing (planned)

### Coverage Requirements

- Minimum 80% code coverage
- All critical paths must be tested
- Error scenarios must be covered

## 🔧 Code Quality

### Pre-commit Hooks

The project uses Husky for pre-commit hooks that run:

- **ESLint**: Code linting and style checking
- **Prettier**: Code formatting
- **Tests**: Related tests for changed files
- **Commit Linting**: Conventional commit message format

### ESLint Configuration

Strict ESLint rules enforce:
- No unused variables or imports
- Proper React hooks usage
- Accessibility best practices
- Import ordering and organization
- Security best practices

### Code Formatting

Prettier configuration ensures consistent code formatting:
- 2-space indentation
- Single quotes
- No trailing commas
- 100 character line width

## 📦 Build and Deployment

### Production Build

```bash
# Create production build
npm run build

# Analyze bundle size
npm run analyze
```

### Build Optimization

- **Tree Shaking**: Removes unused code
- **Code Splitting**: Separates vendor and application code
- **Asset Optimization**: Minification and compression
- **Source Maps**: Available for debugging

### Deployment

The application is designed to be deployed as static files and can be served by any web server:

```bash
# Build for production
npm run build

# Serve static files (example with nginx)
# Copy build/ contents to your web server directory
```

## 🔒 Security Considerations

### Authentication
- JWT tokens are encrypted using AES before storage
- Tokens are stored in sessionStorage (not localStorage)
- Automatic token validation and cleanup
- Secure logout with token invalidation

### Input Validation
- All user inputs are validated and sanitized
- XSS protection through proper escaping
- File upload validation and restrictions
- SQL injection prevention through parameterized queries

### Error Handling
- No sensitive information exposed in error messages
- Comprehensive error boundaries prevent application crashes
- Error reporting to monitoring systems
- Graceful degradation for network issues

## 📊 Performance Monitoring

### Metrics Tracked
- Page load times
- Component render times
- API response times
- Error rates and types
- User interaction patterns

### Optimization Techniques
- React.memo for component memoization
- useMemo for expensive calculations
- useCallback for event handlers
- Lazy loading for routes and components
- API response caching

## 🎨 UI/UX Guidelines

### Design System
- **Ant Design 5**: Primary component library
- **Consistent Spacing**: 8px grid system
- **Color Palette**: Defined in theme configuration
- **Typography**: Consistent font sizes and weights

### Accessibility
- **ARIA Labels**: All interactive elements
- **Keyboard Navigation**: Full keyboard support
- **Screen Readers**: Semantic HTML and ARIA attributes
- **Color Contrast**: WCAG 2.1 AA compliance

### Responsive Design
- **Mobile First**: Responsive design approach
- **Breakpoints**: Standard Ant Design breakpoints
- **Touch Friendly**: Appropriate touch targets
- **Progressive Enhancement**: Works without JavaScript

## 🔄 Component Refactoring

### Before Refactoring
- Users.js: 300+ lines (monolithic)
- PlantView.js: 400+ lines (monolithic)
- Mixed concerns (UI, business logic, API calls)
- Performance issues due to unnecessary re-renders

### After Refactoring
- **UserTable**: <150 lines (focused on display)
- **UserModal**: <150 lines (focused on editing)
- **PlantDashboard**: <150 lines (focused on metrics)
- **WorkflowTable**: <150 lines (focused on data display)
- **Custom Hooks**: Business logic extracted
- **Error Boundaries**: Comprehensive error handling

### Migration Benefits
- 60% reduction in component size
- Improved maintainability
- Better test coverage
- Enhanced performance
- Clearer separation of concerns

## 📚 Documentation

### Component Documentation
- **JSDoc Comments**: All components and hooks
- **Usage Examples**: Practical implementation examples
- **Props Documentation**: Complete prop specifications
- **Integration Patterns**: How components work together

### API Documentation
- **Service Layer**: Complete API service documentation
- **Error Handling**: Error response formats and handling
- **Authentication**: Token management and validation
- **Caching**: Cache strategies and invalidation

## 🤝 Contributing

### Development Workflow

1. **Create Feature Branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make Changes**
   - Follow coding standards
   - Add tests for new functionality
   - Update documentation

3. **Run Quality Checks**
   ```bash
   npm run lint
   npm test
   npm run build
   ```

4. **Commit Changes**
   ```bash
   git commit -m "feat: add new feature description"
   ```

5. **Create Pull Request**
   - Fill out PR template
   - Ensure all checks pass
   - Request code review

### Code Review Checklist

- [ ] Code follows established patterns
- [ ] Tests are included and passing
- [ ] Documentation is updated
- [ ] Performance impact is considered
- [ ] Security implications are reviewed
- [ ] Accessibility requirements are met

## 🐛 Troubleshooting

### Common Issues

1. **Build Failures**
   - Check Node.js version compatibility
   - Clear node_modules and reinstall
   - Verify environment variables

2. **Performance Issues**
   - Check for missing memoization
   - Verify dependency arrays in hooks
   - Use React DevTools Profiler

3. **Authentication Issues**
   - Verify JWT configuration
   - Check token encryption/decryption
   - Validate API endpoints

### Debug Tools

- **React DevTools**: Component inspection and profiling
- **Redux DevTools**: State management debugging (if applicable)
- **Network Tab**: API request/response analysis
- **Console Logs**: Application flow debugging

## 📈 Roadmap

### Short Term (Next 3 months)
- [ ] TypeScript migration
- [ ] Storybook integration
- [ ] Enhanced test coverage
- [ ] Performance monitoring dashboard

### Medium Term (3-6 months)
- [ ] Progressive Web App features
- [ ] Offline functionality
- [ ] Advanced caching strategies
- [ ] Micro-frontend architecture

### Long Term (6+ months)
- [ ] AI-powered features
- [ ] Advanced analytics
- [ ] Multi-tenant support
- [ ] Mobile application

## 📄 License

This project is proprietary software owned by QRMFG. All rights reserved.

## 📞 Support

For technical support or questions:
- **Development Team**: dev-team@qrmfg.com
- **Documentation**: [Internal Wiki](https://wiki.qrmfg.com/frontend)
- **Issue Tracking**: [JIRA Project](https://jira.qrmfg.com/projects/FRONTEND)
