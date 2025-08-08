# QRMFG Frontend Security Best Practices

## 🔒 Overview

This document outlines security best practices for the QRMFG frontend application. Security is a critical aspect of our application, and all developers must follow these guidelines to ensure the protection of user data and system integrity.

## 🛡️ Authentication & Authorization

### JWT Token Management

#### Secure Token Storage

```javascript
// ✅ Correct: Use encrypted storage
import { SecureTokenStorage } from '../services/secureStorage';

// Store token securely
SecureTokenStorage.setToken(jwtToken);

// Retrieve and validate token
const token = SecureTokenStorage.getToken();
if (token && JWTValidator.validateToken(token)) {
  // Use token for API calls
}

// ❌ Incorrect: Plain localStorage storage
localStorage.setItem('token', jwtToken); // Vulnerable to XSS
```

#### Token Validation

```javascript
// ✅ Comprehensive token validation
export class JWTValidator {
  static validateToken(token) {
    if (!token) return false;
    
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return false;
      
      const payload = JSON.parse(atob(parts[1]));
      
      // Check expiry
      if (payload.exp && payload.exp < Date.now() / 1000) {
        return false;
      }
      
      // Validate issuer
      if (payload.iss !== process.env.REACT_APP_JWT_ISSUER) {
        return false;
      }
      
      // Validate audience
      if (payload.aud !== process.env.REACT_APP_JWT_AUDIENCE) {
        return false;
      }
      
      return true;
    } catch (error) {
      return false;
    }
  }
}
```

#### Automatic Token Cleanup

```javascript
// ✅ Automatic cleanup on invalid tokens
const apiClient = axios.create();

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Clear invalid token
      SecureTokenStorage.removeToken();
      // Redirect to login
      window.location.href = '/qrmfg/login';
    }
    return Promise.reject(error);
  }
);
```

### Role-Based Access Control

```javascript
// ✅ Proper role checking
const usePermissions = () => {
  const { user } = useAuth();
  
  const hasPermission = useCallback((permission) => {
    if (!user || !user.roles) return false;
    
    return user.roles.some(role => 
      role.permissions?.includes(permission)
    );
  }, [user]);
  
  const hasRole = useCallback((roleName) => {
    if (!user || !user.roles) return false;
    
    return user.roles.some(role => role.name === roleName);
  }, [user]);
  
  return { hasPermission, hasRole };
};

// Usage in components
const UserManagement = () => {
  const { hasPermission } = usePermissions();
  
  if (!hasPermission('USER_MANAGEMENT')) {
    return <AccessDenied />;
  }
  
  return <UserTable />;
};
```

## 🚫 XSS Prevention

### Input Sanitization

```javascript
// ✅ Sanitize user input
import DOMPurify from 'dompurify';

const sanitizeInput = (input) => {
  return DOMPurify.sanitize(input, {
    ALLOWED_TAGS: ['b', 'i', 'em', 'strong', 'p', 'br'],
    ALLOWED_ATTR: []
  });
};

// Safe HTML rendering
const SafeHTML = ({ content }) => (
  <div 
    dangerouslySetInnerHTML={{ 
      __html: DOMPurify.sanitize(content) 
    }} 
  />
);

// ❌ Dangerous: Unsanitized HTML
const UnsafeHTML = ({ content }) => (
  <div dangerouslySetInnerHTML={{ __html: content }} />
);
```

### Form Validation

```javascript
// ✅ Comprehensive input validation
const validateUserInput = (data) => {
  const errors = {};
  
  // Username validation
  if (!data.username || data.username.length < 3) {
    errors.username = 'Username must be at least 3 characters';
  }
  
  // Prevent script injection
  if (/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi.test(data.username)) {
    errors.username = 'Invalid characters detected';
  }
  
  // Email validation
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(data.email)) {
    errors.email = 'Invalid email format';
  }
  
  return errors;
};

// Server-side validation should always be the primary defense
const submitForm = async (formData) => {
  // Client-side validation (user experience)
  const clientErrors = validateUserInput(formData);
  if (Object.keys(clientErrors).length > 0) {
    setErrors(clientErrors);
    return;
  }
  
  try {
    // Server will perform its own validation
    await apiClient.post('/users', formData);
  } catch (error) {
    // Handle server validation errors
    if (error.response?.data?.validationErrors) {
      setErrors(error.response.data.validationErrors);
    }
  }
};
```

### Content Security Policy

```html
<!-- ✅ Strict CSP headers -->
<meta http-equiv="Content-Security-Policy" 
      content="default-src 'self'; 
               script-src 'self' 'unsafe-inline'; 
               style-src 'self' 'unsafe-inline'; 
               img-src 'self' data: https:; 
               connect-src 'self' https://api.qrmfg.com;">
```

## 🔐 Data Protection

### Sensitive Data Handling

```javascript
// ✅ Proper handling of sensitive data
const UserProfile = ({ user }) => {
  // Mask sensitive information
  const maskedSSN = user.ssn ? 
    `***-**-${user.ssn.slice(-4)}` : 
    'Not provided';
  
  const maskedCreditCard = user.creditCard ? 
    `****-****-****-${user.creditCard.slice(-4)}` : 
    'Not provided';
  
  return (
    <div>
      <p>Name: {user.name}</p>
      <p>SSN: {maskedSSN}</p>
      <p>Credit Card: {maskedCreditCard}</p>
    </div>
  );
};

// ❌ Don't expose sensitive data
const UnsafeUserProfile = ({ user }) => (
  <div>
    <p>SSN: {user.ssn}</p> {/* Exposed sensitive data */}
    <p>Credit Card: {user.creditCard}</p> {/* Exposed sensitive data */}
  </div>
);
```

### Environment Variables

```javascript
// ✅ Proper environment variable usage
const config = {
  apiBaseUrl: process.env.REACT_APP_API_BASE_URL,
  jwtIssuer: process.env.REACT_APP_JWT_ISSUER,
  // Only use REACT_APP_ prefix for non-sensitive config
};

// ❌ Don't hardcode secrets
const badConfig = {
  apiKey: 'sk-1234567890abcdef', // Hardcoded secret
  databaseUrl: 'postgres://user:pass@localhost/db' // Exposed credentials
};

// ✅ Validate required environment variables
const requiredEnvVars = [
  'REACT_APP_API_BASE_URL',
  'REACT_APP_JWT_ISSUER'
];

requiredEnvVars.forEach(envVar => {
  if (!process.env[envVar]) {
    throw new Error(`Missing required environment variable: ${envVar}`);
  }
});
```

## 🌐 Network Security

### HTTPS Enforcement

```javascript
// ✅ Enforce HTTPS in production
const apiClient = axios.create({
  baseURL: process.env.REACT_APP_API_BASE_URL,
  timeout: 30000,
  // Ensure HTTPS in production
  ...(process.env.NODE_ENV === 'production' && {
    httpsAgent: new https.Agent({
      rejectUnauthorized: true
    })
  })
});

// ✅ Check for HTTPS
if (process.env.NODE_ENV === 'production' && 
    window.location.protocol !== 'https:') {
  window.location.replace(`https:${window.location.href.substring(window.location.protocol.length)}`);
}
```

### CORS Configuration

```javascript
// ✅ Proper CORS handling (backend configuration)
const corsOptions = {
  origin: [
    'https://qrmfg.com',
    'https://app.qrmfg.com'
  ],
  credentials: true,
  optionsSuccessStatus: 200
};

// Frontend: Include credentials when needed
const apiCall = async () => {
  const response = await fetch('/api/data', {
    method: 'GET',
    credentials: 'include', // Include cookies/auth headers
    headers: {
      'Content-Type': 'application/json'
    }
  });
};
```

### Request Validation

```javascript
// ✅ Validate and sanitize API requests
const makeApiRequest = async (endpoint, data) => {
  // Validate endpoint
  const allowedEndpoints = ['/users', '/plants', '/workflows'];
  if (!allowedEndpoints.some(allowed => endpoint.startsWith(allowed))) {
    throw new Error('Invalid endpoint');
  }
  
  // Sanitize data
  const sanitizedData = Object.keys(data).reduce((acc, key) => {
    if (typeof data[key] === 'string') {
      acc[key] = DOMPurify.sanitize(data[key]);
    } else {
      acc[key] = data[key];
    }
    return acc;
  }, {});
  
  return apiClient.post(endpoint, sanitizedData);
};
```

## 📁 File Upload Security

### File Validation

```javascript
// ✅ Comprehensive file validation
const validateFile = (file) => {
  const errors = [];
  
  // Check file size (10MB limit)
  const maxSize = 10 * 1024 * 1024;
  if (file.size > maxSize) {
    errors.push('File size must be less than 10MB');
  }
  
  // Check file type
  const allowedTypes = [
    'image/jpeg',
    'image/png',
    'image/gif',
    'application/pdf',
    'text/plain'
  ];
  
  if (!allowedTypes.includes(file.type)) {
    errors.push('File type not allowed');
  }
  
  // Check file extension
  const allowedExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.pdf', '.txt'];
  const fileExtension = file.name.toLowerCase().substring(file.name.lastIndexOf('.'));
  
  if (!allowedExtensions.includes(fileExtension)) {
    errors.push('File extension not allowed');
  }
  
  // Check for executable files
  const dangerousExtensions = ['.exe', '.bat', '.cmd', '.scr', '.pif', '.js'];
  if (dangerousExtensions.includes(fileExtension)) {
    errors.push('Executable files are not allowed');
  }
  
  return errors;
};

// ✅ Secure file upload component
const FileUpload = ({ onUpload }) => {
  const handleFileSelect = (event) => {
    const file = event.target.files[0];
    if (!file) return;
    
    const validationErrors = validateFile(file);
    if (validationErrors.length > 0) {
      message.error(validationErrors.join(', '));
      return;
    }
    
    // Create FormData for secure upload
    const formData = new FormData();
    formData.append('file', file);
    formData.append('uploadType', 'document');
    
    onUpload(formData);
  };
  
  return (
    <input
      type="file"
      onChange={handleFileSelect}
      accept=".jpg,.jpeg,.png,.gif,.pdf,.txt"
    />
  );
};
```

### File Content Scanning

```javascript
// ✅ Client-side file content validation
const scanFileContent = async (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    
    reader.onload = (e) => {
      const content = e.target.result;
      
      // Check for suspicious patterns
      const suspiciousPatterns = [
        /<script/i,
        /javascript:/i,
        /vbscript:/i,
        /onload=/i,
        /onerror=/i
      ];
      
      const hasSuspiciousContent = suspiciousPatterns.some(pattern => 
        pattern.test(content)
      );
      
      if (hasSuspiciousContent) {
        reject(new Error('File contains suspicious content'));
      } else {
        resolve(true);
      }
    };
    
    reader.onerror = () => reject(new Error('Failed to read file'));
    reader.readAsText(file);
  });
};
```

## 🔍 Error Handling Security

### Safe Error Messages

```javascript
// ✅ Safe error handling
const handleApiError = (error) => {
  // Log detailed error for developers
  console.error('API Error:', {
    message: error.message,
    stack: error.stack,
    url: error.config?.url,
    method: error.config?.method
  });
  
  // Show generic message to users
  let userMessage = 'An unexpected error occurred. Please try again.';
  
  if (error.response?.status === 401) {
    userMessage = 'Your session has expired. Please log in again.';
  } else if (error.response?.status === 403) {
    userMessage = 'You do not have permission to perform this action.';
  } else if (error.response?.status >= 400 && error.response?.status < 500) {
    userMessage = 'Invalid request. Please check your input and try again.';
  }
  
  message.error(userMessage);
};

// ❌ Don't expose sensitive error information
const unsafeErrorHandling = (error) => {
  // This exposes internal system information
  message.error(`Database error: ${error.message}`);
  message.error(`SQL: ${error.query}`);
  message.error(`Stack: ${error.stack}`);
};
```

### Error Boundary Security

```javascript
// ✅ Secure error boundary
class SecureErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }
  
  static getDerivedStateFromError(error) {
    return { hasError: true };
  }
  
  componentDidCatch(error, errorInfo) {
    // Log error securely (no sensitive data)
    const sanitizedError = {
      message: error.message,
      componentStack: errorInfo.componentStack,
      timestamp: new Date().toISOString(),
      userAgent: navigator.userAgent,
      url: window.location.href
    };
    
    // Send to monitoring service
    errorReporting.logError(sanitizedError);
  }
  
  render() {
    if (this.state.hasError) {
      return (
        <Result
          status="error"
          title="Something went wrong"
          subTitle="We're sorry for the inconvenience. Please refresh the page or contact support."
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

## 🔒 Session Management

### Session Security

```javascript
// ✅ Secure session handling
const useSession = () => {
  const [session, setSession] = useState(null);
  
  useEffect(() => {
    // Check session validity on mount
    const checkSession = async () => {
      const token = SecureTokenStorage.getToken();
      
      if (!token || !JWTValidator.validateToken(token)) {
        // Invalid session, redirect to login
        window.location.href = '/qrmfg/login';
        return;
      }
      
      try {
        // Verify session with server
        const response = await apiClient.get('/auth/verify');
        setSession(response.data);
      } catch (error) {
        // Session invalid on server
        SecureTokenStorage.removeToken();
        window.location.href = '/qrmfg/login';
      }
    };
    
    checkSession();
    
    // Set up session refresh interval
    const interval = setInterval(checkSession, 5 * 60 * 1000); // 5 minutes
    
    return () => clearInterval(interval);
  }, []);
  
  const logout = useCallback(() => {
    SecureTokenStorage.removeToken();
    setSession(null);
    window.location.href = '/qrmfg/login';
  }, []);
  
  return { session, logout };
};
```

### Idle Session Timeout

```javascript
// ✅ Automatic session timeout
const useIdleTimeout = (timeoutMinutes = 30) => {
  const timeoutRef = useRef();
  const { logout } = useSession();
  
  const resetTimeout = useCallback(() => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }
    
    timeoutRef.current = setTimeout(() => {
      message.warning('Session expired due to inactivity');
      logout();
    }, timeoutMinutes * 60 * 1000);
  }, [timeoutMinutes, logout]);
  
  useEffect(() => {
    const events = ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart'];
    
    const resetTimeoutHandler = () => resetTimeout();
    
    events.forEach(event => {
      document.addEventListener(event, resetTimeoutHandler, true);
    });
    
    resetTimeout(); // Initial timeout
    
    return () => {
      events.forEach(event => {
        document.removeEventListener(event, resetTimeoutHandler, true);
      });
      
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
    };
  }, [resetTimeout]);
};
```

## 🛡️ Content Security

### Iframe Security

```javascript
// ✅ Secure iframe usage
const SecureIframe = ({ src, title }) => {
  // Validate iframe source
  const allowedDomains = [
    'https://trusted-domain.com',
    'https://api.qrmfg.com'
  ];
  
  const isAllowedSource = allowedDomains.some(domain => 
    src.startsWith(domain)
  );
  
  if (!isAllowedSource) {
    return <div>Content not available</div>;
  }
  
  return (
    <iframe
      src={src}
      title={title}
      sandbox="allow-scripts allow-same-origin"
      referrerPolicy="strict-origin-when-cross-origin"
      style={{ border: 'none', width: '100%', height: '400px' }}
    />
  );
};
```

### External Link Security

```javascript
// ✅ Secure external links
const ExternalLink = ({ href, children, ...props }) => {
  return (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      {...props}
    >
      {children}
    </a>
  );
};

// ✅ Link validation
const validateExternalLink = (url) => {
  try {
    const parsedUrl = new URL(url);
    
    // Block dangerous protocols
    const dangerousProtocols = ['javascript:', 'data:', 'vbscript:'];
    if (dangerousProtocols.includes(parsedUrl.protocol)) {
      return false;
    }
    
    // Only allow HTTP/HTTPS
    if (!['http:', 'https:'].includes(parsedUrl.protocol)) {
      return false;
    }
    
    return true;
  } catch (error) {
    return false;
  }
};
```

## 📊 Security Monitoring

### Activity Tracking

```javascript
// ✅ Secure activity tracking
const useActivityTracking = () => {
  const trackActivity = useCallback((action, details = {}) => {
    const activityData = {
      action,
      timestamp: new Date().toISOString(),
      userAgent: navigator.userAgent,
      url: window.location.href,
      // Don't include sensitive data in tracking
      details: sanitizeTrackingData(details)
    };
    
    // Send to monitoring service
    monitoringAPI.trackActivity(activityData);
  }, []);
  
  return { trackActivity };
};

const sanitizeTrackingData = (data) => {
  const sensitiveKeys = ['password', 'token', 'ssn', 'creditCard'];
  
  return Object.keys(data).reduce((acc, key) => {
    if (sensitiveKeys.includes(key.toLowerCase())) {
      acc[key] = '[REDACTED]';
    } else {
      acc[key] = data[key];
    }
    return acc;
  }, {});
};
```

### Security Event Logging

```javascript
// ✅ Security event logging
const logSecurityEvent = (eventType, details) => {
  const securityEvent = {
    type: eventType,
    timestamp: new Date().toISOString(),
    userAgent: navigator.userAgent,
    ipAddress: 'CLIENT_IP', // Will be filled by server
    sessionId: getSessionId(),
    details: sanitizeSecurityEventDetails(details)
  };
  
  // Send to security monitoring service
  securityAPI.logEvent(securityEvent);
};

// Usage examples
const handleLoginAttempt = (username, success) => {
  logSecurityEvent('LOGIN_ATTEMPT', {
    username,
    success,
    timestamp: new Date().toISOString()
  });
};

const handleSuspiciousActivity = (activity) => {
  logSecurityEvent('SUSPICIOUS_ACTIVITY', {
    activity,
    severity: 'HIGH'
  });
};
```

## 🚨 Security Incident Response

### Automated Response

```javascript
// ✅ Automated security response
const handleSecurityIncident = (incidentType, severity) => {
  switch (severity) {
    case 'CRITICAL':
      // Immediate logout and token invalidation
      SecureTokenStorage.removeToken();
      window.location.href = '/qrmfg/login?reason=security';
      break;
      
    case 'HIGH':
      // Show security warning
      Modal.warning({
        title: 'Security Alert',
        content: 'Suspicious activity detected. Please verify your identity.',
        onOk: () => {
          // Redirect to re-authentication
          window.location.href = '/qrmfg/verify-identity';
        }
      });
      break;
      
    case 'MEDIUM':
      // Log and continue with warning
      message.warning('Security notice: Please ensure you are on a secure connection.');
      break;
      
    default:
      // Log for monitoring
      console.warn('Security incident detected:', incidentType);
  }
  
  // Always log the incident
  logSecurityEvent('SECURITY_INCIDENT', {
    type: incidentType,
    severity,
    automaticResponse: true
  });
};
```

## 📋 Security Checklist

### Pre-deployment Security Review

- [ ] **Authentication**
  - [ ] JWT tokens are encrypted before storage
  - [ ] Token validation includes expiry, issuer, and audience checks
  - [ ] Automatic token cleanup on invalid tokens
  - [ ] Session timeout implemented

- [ ] **Authorization**
  - [ ] Role-based access control implemented
  - [ ] Protected routes check permissions
  - [ ] API calls include proper authorization headers

- [ ] **Input Validation**
  - [ ] All user inputs are validated and sanitized
  - [ ] XSS prevention measures in place
  - [ ] File uploads are validated for type, size, and content
  - [ ] SQL injection prevention (parameterized queries)

- [ ] **Data Protection**
  - [ ] Sensitive data is masked in UI
  - [ ] No hardcoded secrets in code
  - [ ] Environment variables used for configuration
  - [ ] HTTPS enforced in production

- [ ] **Error Handling**
  - [ ] Error messages don't expose sensitive information
  - [ ] Error boundaries implemented
  - [ ] Proper logging without sensitive data

- [ ] **Security Headers**
  - [ ] Content Security Policy configured
  - [ ] X-Frame-Options set
  - [ ] X-Content-Type-Options set
  - [ ] Referrer-Policy configured

- [ ] **Monitoring**
  - [ ] Security event logging implemented
  - [ ] Activity tracking in place
  - [ ] Automated incident response configured

### Regular Security Maintenance

- [ ] **Monthly**
  - [ ] Review and update dependencies
  - [ ] Check for security vulnerabilities
  - [ ] Review access logs

- [ ] **Quarterly**
  - [ ] Security audit of authentication flow
  - [ ] Review and update security policies
  - [ ] Penetration testing

- [ ] **Annually**
  - [ ] Comprehensive security review
  - [ ] Update security training
  - [ ] Review and update incident response procedures

## 📞 Security Incident Reporting

If you discover a security vulnerability or incident:

1. **Immediate Action**: Do not discuss publicly
2. **Report**: Contact security@qrmfg.com immediately
3. **Document**: Provide detailed information about the issue
4. **Follow Up**: Work with security team on resolution

## 📚 Security Resources

### Training Materials
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [React Security Best Practices](https://reactjs.org/docs/dom-elements.html#dangerouslysetinnerhtml)
- [Web Security Fundamentals](https://developer.mozilla.org/en-US/docs/Web/Security)

### Tools
- [ESLint Security Plugin](https://github.com/nodesecurity/eslint-plugin-security)
- [Snyk](https://snyk.io/) - Vulnerability scanning
- [OWASP ZAP](https://www.zaproxy.org/) - Security testing

### Internal Resources
- **Security Team**: security@qrmfg.com
- **Security Wiki**: [Internal Security Documentation]
- **Incident Response**: [Emergency Procedures]

---

Remember: Security is everyone's responsibility. When in doubt, err on the side of caution and consult with the security team.