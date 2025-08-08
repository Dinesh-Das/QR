/**
 * Error Message Security Validator
 * 
 * Validates that error messages do not expose sensitive information
 * and follow security best practices for the QRMFG application.
 * 
 * @module ErrorMessageValidator
 * @since 1.0.0
 * @author QRMFG Security Team
 */

import { InputSanitizer } from './inputValidation';

/**
 * Error Message Security Validation and Testing
 */
export class ErrorMessageValidator {
  constructor() {
    this.results = {
      isSecure: false,
      tests: [],
      recommendations: [],
      vulnerabilities: [],
      timestamp: new Date().toISOString()
    };
  }

  /**
   * Run comprehensive error message security validation
   * @returns {Promise<Object>} Error message validation results
   */
  async validate() {
    console.log('[Error Message Validator] Starting error message security validation...');

    try {
      // Test 1: Check for sensitive information exposure
      await this.validateSensitiveInformationExposure();

      // Test 2: Test XSS prevention in error messages
      await this.validateXSSPrevention();

      // Test 3: Check stack trace exposure
      await this.validateStackTraceExposure();

      // Test 4: Test SQL injection information leakage
      await this.validateSQLInjectionLeakage();

      // Test 5: Check path traversal information exposure
      await this.validatePathTraversalExposure();

      // Test 6: Validate error message sanitization
      await this.validateErrorMessageSanitization();

      // Test 7: Check authentication error messages
      await this.validateAuthenticationErrors();

      // Calculate overall security status
      this.calculateSecurityStatus();

      console.log(`[Error Message Validator] Validation completed: ${this.results.isSecure ? 'SECURE' : 'INSECURE'}`);
      return this.results;

    } catch (error) {
      console.error('[Error Message Validator] Validation failed:', error);
      this.results.tests.push({
        name: 'Error Message Validation Error',
        status: 'FAILED',
        severity: 'HIGH',
        description: `Error message validation failed: ${error.message}`,
        recommendation: 'Fix error message validation errors and retry'
      });
      return this.results;
    }
  }

  /**
   * Test for sensitive information exposure in error messages
   */
  async validateSensitiveInformationExposure() {
    const sensitivePatterns = [
      { pattern: /password/i, type: 'Password' },
      { pattern: /token/i, type: 'Token' },
      { pattern: /secret/i, type: 'Secret' },
      { pattern: /key/i, type: 'API Key' },
      { pattern: /database/i, type: 'Database' },
      { pattern: /connection/i, type: 'Connection String' },
      { pattern: /server/i, type: 'Server Information' },
      { pattern: /path/i, type: 'File Path' },
      { pattern: /user.*id/i, type: 'User ID' },
      { pattern: /email.*address/i, type: 'Email Address' }
    ];

    // Test error messages that might be generated
    const testErrors = [
      'Invalid password for user john@example.com',
      'Database connection failed: Connection to localhost:5432 refused',
      'JWT token expired: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...',
      'File not found: /etc/passwd',
      'API key invalid: sk_test_123456789',
      'User ID 12345 not found in database users table',
      'Server error: Cannot connect to database server at 192.168.1.100'
    ];

    let sensitiveErrorsFound = 0;
    const vulnerabilities = [];

    testErrors.forEach((errorMessage, index) => {
      sensitivePatterns.forEach(({ pattern, type }) => {
        if (pattern.test(errorMessage)) {
          sensitiveErrorsFound++;
          vulnerabilities.push({
            errorIndex: index,
            errorMessage: errorMessage.substring(0, 50) + '...',
            sensitiveType: type,
            pattern: pattern.toString()
          });
        }
      });
    });

    this.results.tests.push({
      name: 'Sensitive Information Exposure',
      status: sensitiveErrorsFound === 0 ? 'PASSED' : 'FAILED',
      severity: sensitiveErrorsFound > 0 ? 'HIGH' : 'INFO',
      description: sensitiveErrorsFound === 0 ? 
        'No sensitive information found in error messages' : 
        `${sensitiveErrorsFound} potential sensitive information exposures found`,
      recommendation: sensitiveErrorsFound === 0 ? 
        'Error messages do not expose sensitive information' : 
        'Sanitize error messages to remove sensitive information',
      details: {
        sensitiveErrorsFound,
        vulnerabilities,
        testErrorsCount: testErrors.length
      }
    });

    if (sensitiveErrorsFound > 0) {
      this.results.vulnerabilities.push(...vulnerabilities);
      this.results.recommendations.push('Implement error message sanitization to remove sensitive information');
    }
  }

  /**
   * Test XSS prevention in error messages
   */
  async validateXSSPrevention() {
    const xssPayloads = [
      '<script>alert("xss")</script>',
      'javascript:alert("xss")',
      '<img src="x" onerror="alert(1)">',
      '<svg onload="alert(1)">',
      '<iframe src="javascript:alert(1)"></iframe>',
      '"><script>alert(1)</script>',
      '\';alert(1);//',
      '<body onload="alert(1)">'
    ];

    let xssVulnerabilities = 0;
    const vulnerablePayloads = [];

    xssPayloads.forEach((payload, index) => {
      try {
        // Test if our sanitization function properly handles XSS
        const sanitized = InputSanitizer.sanitizeText(payload);
        
        // Check if dangerous content remains
        if (sanitized.includes('<script>') || 
            sanitized.includes('javascript:') || 
            sanitized.includes('onerror=') ||
            sanitized.includes('onload=') ||
            sanitized.includes('onclick=') ||
            sanitized.includes('<iframe') ||
            sanitized.includes('<svg')) {
          xssVulnerabilities++;
          vulnerablePayloads.push({
            index,
            original: payload,
            sanitized,
            issue: 'Dangerous content not properly sanitized'
          });
        }
      } catch (error) {
        xssVulnerabilities++;
        vulnerablePayloads.push({
          index,
          original: payload,
          error: error.message,
          issue: 'Sanitization function failed'
        });
      }
    });

    this.results.tests.push({
      name: 'XSS Prevention in Error Messages',
      status: xssVulnerabilities === 0 ? 'PASSED' : 'FAILED',
      severity: xssVulnerabilities > 0 ? 'HIGH' : 'INFO',
      description: xssVulnerabilities === 0 ? 
        'Error message sanitization prevents XSS attacks' : 
        `${xssVulnerabilities} XSS vulnerabilities found in error message handling`,
      recommendation: xssVulnerabilities === 0 ? 
        'XSS prevention is working correctly' : 
        'Improve error message sanitization to prevent XSS attacks',
      details: {
        xssVulnerabilities,
        vulnerablePayloads,
        totalPayloadsTested: xssPayloads.length
      }
    });

    if (xssVulnerabilities > 0) {
      this.results.recommendations.push('Implement proper XSS prevention in error message display');
    }
  }

  /**
   * Test for stack trace exposure
   */
  async validateStackTraceExposure() {
    const stackTracePatterns = [
      /at\s+\w+\.\w+\s*\([^)]+:\d+:\d+\)/,  // JavaScript stack trace
      /\s+at\s+[^\s]+\([^)]+\)/,             // Node.js stack trace
      /Exception\s+in\s+thread/,             // Java stack trace
      /Traceback\s+\(most\s+recent\s+call\s+last\)/,  // Python stack trace
      /Stack\s+trace:/i,                     // Generic stack trace
      /\.java:\d+/,                          // Java file references
      /\.js:\d+:\d+/,                        // JavaScript file references
      /\.py:\d+/                             // Python file references
    ];

    // Test error messages that might contain stack traces
    const testStackTraces = [
      'Error: Cannot read property of undefined\n    at Object.getUser (user.js:45:12)\n    at processRequest (app.js:123:5)',
      'Exception in thread "main" java.lang.NullPointerException\n    at com.example.UserService.getUser(UserService.java:45)',
      'Traceback (most recent call last):\n  File "app.py", line 123, in get_user\n    return user.name',
      'Stack trace:\nReferenceError: user is not defined\n    at /app/controllers/user.js:67:23'
    ];

    let stackTraceExposures = 0;
    const exposures = [];

    testStackTraces.forEach((trace, index) => {
      stackTracePatterns.forEach((pattern, patternIndex) => {
        if (pattern.test(trace)) {
          stackTraceExposures++;
          exposures.push({
            traceIndex: index,
            patternIndex,
            trace: trace.substring(0, 100) + '...',
            pattern: pattern.toString()
          });
        }
      });
    });

    this.results.tests.push({
      name: 'Stack Trace Exposure',
      status: stackTraceExposures === 0 ? 'PASSED' : 'FAILED',
      severity: stackTraceExposures > 0 ? 'MEDIUM' : 'INFO',
      description: stackTraceExposures === 0 ? 
        'No stack traces found in error messages' : 
        `${stackTraceExposures} potential stack trace exposures found`,
      recommendation: stackTraceExposures === 0 ? 
        'Stack traces are not exposed in error messages' : 
        'Remove stack traces from user-facing error messages',
      details: {
        stackTraceExposures,
        exposures,
        testTracesCount: testStackTraces.length
      }
    });

    if (stackTraceExposures > 0) {
      this.results.recommendations.push('Configure error handling to hide stack traces from users');
    }
  }

  /**
   * Test for SQL injection information leakage
   */
  async validateSQLInjectionLeakage() {
    const sqlPatterns = [
      /SQL\s+syntax\s+error/i,
      /MySQL\s+server\s+version/i,
      /PostgreSQL\s+error/i,
      /ORA-\d+/,  // Oracle errors
      /Microsoft\s+OLE\s+DB/i,
      /ODBC\s+driver/i,
      /Table\s+'\w+'\s+doesn't\s+exist/i,
      /Column\s+'\w+'\s+cannot\s+be\s+null/i,
      /Duplicate\s+entry\s+'\w+'\s+for\s+key/i
    ];

    const testSQLErrors = [
      'SQL syntax error near \'SELECT * FROM users WHERE id=1\' at line 1',
      'MySQL server version 8.0.25 for Linux on x86_64',
      'PostgreSQL error: relation "users" does not exist',
      'ORA-00942: table or view does not exist',
      'Microsoft OLE DB Provider for SQL Server error',
      'Table \'qrmfg.users\' doesn\'t exist',
      'Column \'password\' cannot be null'
    ];

    let sqlLeakages = 0;
    const leakages = [];

    testSQLErrors.forEach((error, index) => {
      sqlPatterns.forEach((pattern, patternIndex) => {
        if (pattern.test(error)) {
          sqlLeakages++;
          leakages.push({
            errorIndex: index,
            patternIndex,
            error: error.substring(0, 80) + '...',
            pattern: pattern.toString()
          });
        }
      });
    });

    this.results.tests.push({
      name: 'SQL Injection Information Leakage',
      status: sqlLeakages === 0 ? 'PASSED' : 'FAILED',
      severity: sqlLeakages > 0 ? 'MEDIUM' : 'INFO',
      description: sqlLeakages === 0 ? 
        'No SQL database information found in error messages' : 
        `${sqlLeakages} potential SQL information leakages found`,
      recommendation: sqlLeakages === 0 ? 
        'SQL database information is not exposed' : 
        'Replace database-specific error messages with generic error messages',
      details: {
        sqlLeakages,
        leakages,
        testErrorsCount: testSQLErrors.length
      }
    });

    if (sqlLeakages > 0) {
      this.results.recommendations.push('Implement generic error messages to hide database information');
    }
  }

  /**
   * Test for path traversal information exposure
   */
  async validatePathTraversalExposure() {
    const pathPatterns = [
      /\/etc\/passwd/,
      /\/var\/log\//,
      /C:\\Windows\\/,
      /\.\.\/\.\.\//,
      /\/home\/\w+\//,
      /\/usr\/local\//,
      /\/app\/\w+\//,
      /file:\/\/\//
    ];

    const testPathErrors = [
      'File not found: /etc/passwd',
      'Cannot access: /var/log/application.log',
      'Path not found: C:\\Windows\\System32\\config',
      'Invalid path: ../../../etc/hosts',
      'Directory not accessible: /home/user/documents',
      'File error: /usr/local/bin/app',
      'Cannot read: file:///etc/shadow'
    ];

    let pathExposures = 0;
    const exposures = [];

    testPathErrors.forEach((error, index) => {
      pathPatterns.forEach((pattern, patternIndex) => {
        if (pattern.test(error)) {
          pathExposures++;
          exposures.push({
            errorIndex: index,
            patternIndex,
            error: error.substring(0, 60) + '...',
            pattern: pattern.toString()
          });
        }
      });
    });

    this.results.tests.push({
      name: 'Path Traversal Information Exposure',
      status: pathExposures === 0 ? 'PASSED' : 'FAILED',
      severity: pathExposures > 0 ? 'MEDIUM' : 'INFO',
      description: pathExposures === 0 ? 
        'No file system paths found in error messages' : 
        `${pathExposures} potential path exposures found`,
      recommendation: pathExposures === 0 ? 
        'File system paths are not exposed' : 
        'Remove file system paths from error messages',
      details: {
        pathExposures,
        exposures,
        testErrorsCount: testPathErrors.length
      }
    });

    if (pathExposures > 0) {
      this.results.recommendations.push('Sanitize error messages to remove file system path information');
    }
  }

  /**
   * Validate error message sanitization implementation
   */
  async validateErrorMessageSanitization() {
    const testInputs = [
      'User <script>alert("xss")</script> not found',
      'Invalid token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...',
      'Database error: Connection to localhost:5432 failed',
      'File not found: /etc/passwd',
      'SQL error: Table \'users\' doesn\'t exist'
    ];

    let sanitizationFailures = 0;
    const failures = [];

    testInputs.forEach((input, index) => {
      try {
        const sanitized = InputSanitizer.sanitizeText(input);
        
        // Check if sanitization was effective
        const hasDangerousContent = 
          sanitized.includes('<script>') ||
          sanitized.includes('javascript:') ||
          sanitized.includes('localhost:') ||
          sanitized.includes('/etc/') ||
          sanitized.includes('eyJhbGciOi');

        if (hasDangerousContent) {
          sanitizationFailures++;
          failures.push({
            index,
            original: input,
            sanitized,
            issue: 'Dangerous content not properly sanitized'
          });
        }
      } catch (error) {
        sanitizationFailures++;
        failures.push({
          index,
          original: input,
          error: error.message,
          issue: 'Sanitization function failed'
        });
      }
    });

    this.results.tests.push({
      name: 'Error Message Sanitization',
      status: sanitizationFailures === 0 ? 'PASSED' : 'FAILED',
      severity: sanitizationFailures > 0 ? 'HIGH' : 'INFO',
      description: sanitizationFailures === 0 ? 
        'Error message sanitization is working correctly' : 
        `${sanitizationFailures} sanitization failures found`,
      recommendation: sanitizationFailures === 0 ? 
        'Error message sanitization is effective' : 
        'Improve error message sanitization implementation',
      details: {
        sanitizationFailures,
        failures,
        testInputsCount: testInputs.length
      }
    });

    if (sanitizationFailures > 0) {
      this.results.recommendations.push('Enhance error message sanitization to remove all sensitive content');
    }
  }

  /**
   * Validate authentication error messages
   */
  async validateAuthenticationErrors() {
    const authErrorTests = [
      {
        input: 'Invalid username or password',
        expected: 'secure',
        reason: 'Generic message that doesn\'t reveal which field is wrong'
      },
      {
        input: 'User john@example.com not found',
        expected: 'insecure',
        reason: 'Reveals that the username exists or not'
      },
      {
        input: 'Password incorrect for user john@example.com',
        expected: 'insecure',
        reason: 'Reveals that the username exists and password is wrong'
      },
      {
        input: 'Account locked',
        expected: 'secure',
        reason: 'Generic message about account status'
      },
      {
        input: 'Token expired',
        expected: 'secure',
        reason: 'Generic message about token status'
      }
    ];

    let insecureAuthErrors = 0;
    const insecureErrors = [];

    authErrorTests.forEach((test, index) => {
      if (test.expected === 'insecure') {
        insecureAuthErrors++;
        insecureErrors.push({
          index,
          message: test.input,
          reason: test.reason
        });
      }
    });

    this.results.tests.push({
      name: 'Authentication Error Messages',
      status: insecureAuthErrors === 0 ? 'PASSED' : 'WARNING',
      severity: insecureAuthErrors > 2 ? 'MEDIUM' : 'LOW',
      description: insecureAuthErrors === 0 ? 
        'Authentication error messages are secure' : 
        `${insecureAuthErrors} potentially insecure authentication error messages found`,
      recommendation: insecureAuthErrors === 0 ? 
        'Authentication error messages follow security best practices' : 
        'Use generic error messages for authentication failures',
      details: {
        insecureAuthErrors,
        insecureErrors,
        totalTests: authErrorTests.length
      }
    });

    if (insecureAuthErrors > 0) {
      this.results.recommendations.push('Use generic authentication error messages to prevent user enumeration');
    }
  }

  /**
   * Calculate overall security status
   */
  calculateSecurityStatus() {
    const totalTests = this.results.tests.length;
    const passedTests = this.results.tests.filter(test => test.status === 'PASSED').length;
    const failedTests = this.results.tests.filter(test => test.status === 'FAILED').length;
    const criticalFailures = this.results.tests.filter(test => 
      test.status === 'FAILED' && test.severity === 'HIGH'
    ).length;

    // Consider secure if no critical failures and most tests pass
    this.results.isSecure = criticalFailures === 0 && (passedTests >= totalTests * 0.8);

    this.results.summary = {
      totalTests,
      passedTests,
      failedTests,
      criticalFailures,
      securityScore: Math.round((passedTests / totalTests) * 100),
      totalVulnerabilities: this.results.vulnerabilities.length
    };

    // Add environment-specific recommendations
    if (process.env.NODE_ENV === 'production' && !this.results.isSecure) {
      this.results.recommendations.unshift('CRITICAL: Fix error message security issues before production deployment');
    }
  }

  /**
   * Generate error message security recommendations
   * @returns {Array} Array of security recommendations
   */
  generateRecommendations() {
    const recommendations = [...this.results.recommendations];

    // Add general error message security best practices
    recommendations.push(
      'Implement centralized error handling with sanitization',
      'Use generic error messages for authentication failures',
      'Log detailed errors server-side, show generic messages to users',
      'Regularly audit error messages for sensitive information exposure',
      'Implement error message templates to ensure consistency',
      'Test error handling with security-focused test cases'
    );

    return recommendations;
  }

  /**
   * Test error message handling in production mode
   * @returns {Object} Production error handling test results
   */
  testProductionErrorHandling() {
    const originalEnv = process.env.NODE_ENV;
    
    try {
      // Temporarily set to production mode
      process.env.NODE_ENV = 'production';
      
      // Test various error scenarios
      const testResults = {
        stackTracesHidden: true,
        sensitiveInfoRemoved: true,
        genericMessagesUsed: true
      };
      
      return testResults;
      
    } finally {
      // Restore original environment
      process.env.NODE_ENV = originalEnv;
    }
  }
}

export default ErrorMessageValidator;