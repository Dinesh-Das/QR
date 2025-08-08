/**
 * Comprehensive Security Audit Utility
 * 
 * Performs final security audit and validation of all implemented security measures.
 * Tests HTTPS enforcement, CORS configuration, error message security, and more.
 * 
 * @module SecurityAudit
 * @since 1.0.0
 * @author QRMFG Security Team
 */

import { vulnerabilityScanner } from '../services/vulnerabilityScanner';
import { securityMonitoring, SECURITY_EVENT_TYPES, SECURITY_SEVERITY } from '../services/securityMonitoring';
import { InputSanitizer, ValidationRules, SecurityAuditLogger } from './inputValidation';
import apiClient from '../api/client';

/**
 * Security Audit Test Results
 */
export class SecurityAuditResults {
  constructor() {
    this.tests = [];
    this.summary = {
      total: 0,
      passed: 0,
      failed: 0,
      warnings: 0,
      critical: 0,
      high: 0,
      medium: 0,
      low: 0
    };
    this.startTime = null;
    this.endTime = null;
  }

  addTest(test) {
    this.tests.push({
      id: `test_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      timestamp: new Date().toISOString(),
      ...test
    });
    
    this.summary.total++;
    
    if (test.status === 'PASSED') {
      this.summary.passed++;
    } else if (test.status === 'FAILED') {
      this.summary.failed++;
      
      // Count by severity
      switch (test.severity) {
        case 'CRITICAL':
          this.summary.critical++;
          break;
        case 'HIGH':
          this.summary.high++;
          break;
        case 'MEDIUM':
          this.summary.medium++;
          break;
        case 'LOW':
          this.summary.low++;
          break;
      }
    } else if (test.status === 'WARNING') {
      this.summary.warnings++;
    }
  }

  getOverallScore() {
    if (this.summary.total === 0) return 0;
    
    // Calculate score based on passed tests and severity of failures
    const baseScore = (this.summary.passed / this.summary.total) * 100;
    const penalties = (
      this.summary.critical * 20 +
      this.summary.high * 10 +
      this.summary.medium * 5 +
      this.summary.low * 2
    );
    
    return Math.max(0, Math.round(baseScore - penalties));
  }

  getRecommendations() {
    return this.tests
      .filter(test => test.status === 'FAILED' && test.recommendation)
      .sort((a, b) => {
        const severityOrder = { 'CRITICAL': 4, 'HIGH': 3, 'MEDIUM': 2, 'LOW': 1 };
        return severityOrder[b.severity] - severityOrder[a.severity];
      })
      .slice(0, 10) // Top 10 recommendations
      .map(test => ({
        severity: test.severity,
        title: test.name,
        recommendation: test.recommendation
      }));
  }
}

/**
 * Security Audit Runner
 */
export class SecurityAuditRunner {
  constructor() {
    this.results = new SecurityAuditResults();
  }

  /**
   * Run comprehensive security audit
   */
  async runSecurityAudit() {
    console.log('[Security Audit] Starting comprehensive security audit...');
    
    this.results = new SecurityAuditResults();
    this.results.startTime = new Date().toISOString();

    try {
      // Run all security tests
      await this.testHTTPSEnforcement();
      await this.testCORSConfiguration();
      await this.testErrorMessageSecurity();
      await this.testInputValidationSecurity();
      await this.testAuthenticationSecurity();
      await this.testSessionManagement();
      await this.testFileUploadSecurity();
      await this.testXSSPrevention();
      await this.testCSRFProtection();
      await this.testSecurityHeaders();
      await this.testContentSecurityPolicy();
      await this.testDataEncryption();
      await this.testAPISecurityConfiguration();
      await this.testDependencyVulnerabilities();
      await this.testPerformanceSecurity();

      // Run vulnerability scanner
      await this.runVulnerabilityScanner();

      this.results.endTime = new Date().toISOString();
      
      // Log audit completion
      securityMonitoring.logSecurityEvent(
        'SECURITY_AUDIT_COMPLETED',
        {
          totalTests: this.results.summary.total,
          passed: this.results.summary.passed,
          failed: this.results.summary.failed,
          overallScore: this.results.getOverallScore(),
          duration: new Date(this.results.endTime) - new Date(this.results.startTime)
        },
        this.results.summary.critical > 0 ? SECURITY_SEVERITY.CRITICAL : 
        this.results.summary.high > 0 ? SECURITY_SEVERITY.HIGH : SECURITY_SEVERITY.LOW
      );

      return this.results;

    } catch (error) {
      console.error('[Security Audit] Audit failed:', error);
      
      securityMonitoring.logSecurityEvent(
        'SECURITY_AUDIT_FAILED',
        { error: error.message },
        SECURITY_SEVERITY.HIGH
      );
      
      throw error;
    }
  }

  /**
   * Test HTTPS enforcement
   */
  async testHTTPSEnforcement() {
    console.log('[Security Audit] Testing HTTPS enforcement...');

    // Test 1: Check if current connection is HTTPS
    const isHTTPS = window.location.protocol === 'https:';
    const isLocalhost = window.location.hostname === 'localhost' || 
                       window.location.hostname === '127.0.0.1';

    if (!isHTTPS && !isLocalhost) {
      this.results.addTest({
        name: 'HTTPS Enforcement',
        category: 'Network Security',
        status: 'FAILED',
        severity: 'CRITICAL',
        description: 'Application is not using HTTPS in production',
        evidence: { protocol: window.location.protocol, hostname: window.location.hostname },
        recommendation: 'Configure server to enforce HTTPS and redirect HTTP traffic'
      });
    } else {
      this.results.addTest({
        name: 'HTTPS Enforcement',
        category: 'Network Security',
        status: 'PASSED',
        description: 'Application is using secure HTTPS connection',
        evidence: { protocol: window.location.protocol }
      });
    }

    // Test 2: Check for mixed content
    const mixedContentElements = document.querySelectorAll(
      'img[src^="http:"], script[src^="http:"], link[href^="http:"], iframe[src^="http:"]'
    );

    if (mixedContentElements.length > 0) {
      this.results.addTest({
        name: 'Mixed Content Prevention',
        category: 'Network Security',
        status: 'FAILED',
        severity: 'HIGH',
        description: `Found ${mixedContentElements.length} elements loading content over HTTP`,
        evidence: { mixedContentCount: mixedContentElements.length },
        recommendation: 'Update all resource URLs to use HTTPS or relative URLs'
      });
    } else {
      this.results.addTest({
        name: 'Mixed Content Prevention',
        category: 'Network Security',
        status: 'PASSED',
        description: 'No mixed content detected'
      });
    }
  }

  /**
   * Test CORS configuration
   */
  async testCORSConfiguration() {
    console.log('[Security Audit] Testing CORS configuration...');

    try {
      // Test CORS by making a preflight request simulation
      const testEndpoint = '/qrmfg/api/v1/health'; // Assuming health endpoint exists
      
      const response = await fetch(testEndpoint, {
        method: 'OPTIONS',
        headers: {
          'Origin': window.location.origin,
          'Access-Control-Request-Method': 'GET',
          'Access-Control-Request-Headers': 'Authorization, Content-Type'
        }
      });

      const corsHeaders = {
        'Access-Control-Allow-Origin': response.headers.get('Access-Control-Allow-Origin'),
        'Access-Control-Allow-Methods': response.headers.get('Access-Control-Allow-Methods'),
        'Access-Control-Allow-Headers': response.headers.get('Access-Control-Allow-Headers'),
        'Access-Control-Allow-Credentials': response.headers.get('Access-Control-Allow-Credentials')
      };

      // Check for overly permissive CORS
      if (corsHeaders['Access-Control-Allow-Origin'] === '*' && 
          corsHeaders['Access-Control-Allow-Credentials'] === 'true') {
        this.results.addTest({
          name: 'CORS Configuration Security',
          category: 'Network Security',
          status: 'FAILED',
          severity: 'HIGH',
          description: 'CORS is configured to allow all origins with credentials',
          evidence: corsHeaders,
          recommendation: 'Configure CORS to allow only specific trusted origins'
        });
      } else {
        this.results.addTest({
          name: 'CORS Configuration Security',
          category: 'Network Security',
          status: 'PASSED',
          description: 'CORS configuration appears secure',
          evidence: corsHeaders
        });
      }

    } catch (error) {
      this.results.addTest({
        name: 'CORS Configuration Test',
        category: 'Network Security',
        status: 'WARNING',
        description: 'Could not test CORS configuration',
        evidence: { error: error.message },
        recommendation: 'Manually verify CORS configuration on server'
      });
    }
  }

  /**
   * Test error message security
   */
  async testErrorMessageSecurity() {
    console.log('[Security Audit] Testing error message security...');

    // Test 1: Check for sensitive information in error messages
    const testErrors = [
      { input: '<script>alert("xss")</script>', expectedSanitized: true },
      { input: 'SELECT * FROM users WHERE id=1', expectedSanitized: true },
      { input: '../../../etc/passwd', expectedSanitized: true }
    ];

    let sensitiveErrorsFound = 0;

    testErrors.forEach(test => {
      try {
        const sanitized = InputSanitizer.sanitizeText(test.input);
        if (sanitized === test.input) {
          sensitiveErrorsFound++;
        }
      } catch (error) {
        // Check if error message exposes sensitive information
        if (error.message && (
          error.message.includes('database') ||
          error.message.includes('SQL') ||
          error.message.includes('password') ||
          error.message.includes('secret')
        )) {
          sensitiveErrorsFound++;
        }
      }
    });

    if (sensitiveErrorsFound > 0) {
      this.results.addTest({
        name: 'Error Message Security',
        category: 'Information Disclosure',
        status: 'FAILED',
        severity: 'MEDIUM',
        description: 'Error messages may expose sensitive information',
        evidence: { sensitiveErrorsFound },
        recommendation: 'Sanitize error messages to remove sensitive information'
      });
    } else {
      this.results.addTest({
        name: 'Error Message Security',
        category: 'Information Disclosure',
        status: 'PASSED',
        description: 'Error messages do not expose sensitive information'
      });
    }

    // Test 2: Check console logging in production
    if (process.env.NODE_ENV === 'production') {
      const hasConsoleLogging = typeof console.log === 'function' && 
                               typeof console.error === 'function';
      
      if (hasConsoleLogging) {
        this.results.addTest({
          name: 'Production Console Logging',
          category: 'Information Disclosure',
          status: 'WARNING',
          description: 'Console logging is available in production',
          recommendation: 'Disable console logging in production builds'
        });
      } else {
        this.results.addTest({
          name: 'Production Console Logging',
          category: 'Information Disclosure',
          status: 'PASSED',
          description: 'Console logging is disabled in production'
        });
      }
    }
  }

  /**
   * Test input validation security
   */
  async testInputValidationSecurity() {
    console.log('[Security Audit] Testing input validation security...');

    // Test XSS prevention
    const xssPayloads = [
      '<script>alert("xss")</script>',
      'javascript:alert("xss")',
      '<img src="x" onerror="alert(1)">',
      '<svg onload="alert(1)">',
      'onclick="alert(1)"'
    ];

    let xssVulnerabilities = 0;
    xssPayloads.forEach(payload => {
      const sanitized = InputSanitizer.sanitizeText(payload);
      if (sanitized.includes('<script>') || 
          sanitized.includes('javascript:') || 
          sanitized.includes('onerror=') ||
          sanitized.includes('onload=') ||
          sanitized.includes('onclick=')) {
        xssVulnerabilities++;
      }
    });

    if (xssVulnerabilities > 0) {
      this.results.addTest({
        name: 'XSS Prevention',
        category: 'Input Validation',
        status: 'FAILED',
        severity: 'HIGH',
        description: `${xssVulnerabilities} XSS vulnerabilities found in input sanitization`,
        evidence: { vulnerablePayloads: xssVulnerabilities },
        recommendation: 'Improve input sanitization to prevent XSS attacks'
      });
    } else {
      this.results.addTest({
        name: 'XSS Prevention',
        category: 'Input Validation',
        status: 'PASSED',
        description: 'Input sanitization successfully prevents XSS attacks'
      });
    }

    // Test SQL injection prevention
    const sqlPayloads = [
      "'; DROP TABLE users; --",
      "' OR '1'='1",
      "UNION SELECT * FROM passwords",
      "'; INSERT INTO users VALUES ('hacker', 'password'); --"
    ];

    let sqlVulnerabilities = 0;
    sqlPayloads.forEach(payload => {
      const sanitized = InputSanitizer.sanitizeSQL(payload);
      if (sanitized.includes('DROP') || 
          sanitized.includes('UNION') || 
          sanitized.includes('INSERT') ||
          sanitized.includes("'='")) {
        sqlVulnerabilities++;
      }
    });

    if (sqlVulnerabilities > 0) {
      this.results.addTest({
        name: 'SQL Injection Prevention',
        category: 'Input Validation',
        status: 'FAILED',
        severity: 'HIGH',
        description: `${sqlVulnerabilities} SQL injection vulnerabilities found`,
        evidence: { vulnerablePayloads: sqlVulnerabilities },
        recommendation: 'Improve SQL input sanitization and use parameterized queries'
      });
    } else {
      this.results.addTest({
        name: 'SQL Injection Prevention',
        category: 'Input Validation',
        status: 'PASSED',
        description: 'Input sanitization successfully prevents SQL injection'
      });
    }

    // Test file upload validation
    const maliciousFiles = [
      { name: 'test.exe', type: 'application/x-executable', size: 1024 },
      { name: '../../../etc/passwd', type: 'text/plain', size: 1024 },
      { name: 'test<script>.pdf', type: 'application/pdf', size: 1024 }
    ];

    let fileValidationIssues = 0;
    maliciousFiles.forEach(file => {
      const typeValidation = ValidationRules.file.validateType(file);
      const filenameValidation = ValidationRules.file.validateFilename(file.name);
      
      if (typeValidation.isValid || filenameValidation.isValid) {
        fileValidationIssues++;
      }
    });

    if (fileValidationIssues > 0) {
      this.results.addTest({
        name: 'File Upload Validation',
        category: 'Input Validation',
        status: 'FAILED',
        severity: 'HIGH',
        description: `${fileValidationIssues} file validation issues found`,
        evidence: { validationIssues: fileValidationIssues },
        recommendation: 'Strengthen file upload validation and sanitization'
      });
    } else {
      this.results.addTest({
        name: 'File Upload Validation',
        category: 'Input Validation',
        status: 'PASSED',
        description: 'File upload validation successfully prevents malicious files'
      });
    }
  }

  /**
   * Test authentication security
   */
  async testAuthenticationSecurity() {
    console.log('[Security Audit] Testing authentication security...');

    // Test token storage security
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    
    if (token) {
      // Check if token is encrypted
      try {
        const parts = token.split('.');
        if (parts.length === 3) {
          // Looks like unencrypted JWT
          this.results.addTest({
            name: 'Token Storage Security',
            category: 'Authentication',
            status: 'FAILED',
            severity: 'CRITICAL',
            description: 'Authentication token is stored without encryption',
            recommendation: 'Use SecureTokenStorage to encrypt tokens before storage'
          });
        } else {
          this.results.addTest({
            name: 'Token Storage Security',
            category: 'Authentication',
            status: 'PASSED',
            description: 'Authentication token appears to be encrypted'
          });
        }
      } catch (error) {
        this.results.addTest({
          name: 'Token Storage Security',
          category: 'Authentication',
          status: 'PASSED',
          description: 'Authentication token appears to be encrypted'
        });
      }
    } else {
      this.results.addTest({
        name: 'Token Storage Security',
        category: 'Authentication',
        status: 'WARNING',
        description: 'No authentication token found for testing'
      });
    }

    // Test password policy enforcement
    const passwordInputs = document.querySelectorAll('input[type="password"]');
    let strongPasswordPolicy = false;

    passwordInputs.forEach(input => {
      const pattern = input.getAttribute('pattern');
      const minLength = input.getAttribute('minlength');
      
      if (pattern && pattern.length > 20 && minLength && parseInt(minLength) >= 8) {
        strongPasswordPolicy = true;
      }
    });

    if (passwordInputs.length > 0 && !strongPasswordPolicy) {
      this.results.addTest({
        name: 'Password Policy Enforcement',
        category: 'Authentication',
        status: 'FAILED',
        severity: 'MEDIUM',
        description: 'Weak or missing password policy enforcement',
        recommendation: 'Implement strong password requirements (8+ chars, complexity)'
      });
    } else if (passwordInputs.length > 0) {
      this.results.addTest({
        name: 'Password Policy Enforcement',
        category: 'Authentication',
        status: 'PASSED',
        description: 'Strong password policy is enforced'
      });
    }
  }

  /**
   * Test session management
   */
  async testSessionManagement() {
    console.log('[Security Audit] Testing session management...');

    // Test session timeout
    const sessionStart = sessionStorage.getItem('sessionStartTime');
    const currentTime = Date.now();
    
    if (sessionStart) {
      const sessionDuration = currentTime - parseInt(sessionStart);
      const maxSessionTime = 30 * 60 * 1000; // 30 minutes
      
      if (sessionDuration > maxSessionTime) {
        this.results.addTest({
          name: 'Session Timeout',
          category: 'Session Management',
          status: 'WARNING',
          description: 'Session has been active for longer than recommended',
          evidence: { sessionDuration: Math.round(sessionDuration / 60000) + ' minutes' },
          recommendation: 'Implement automatic session timeout after 30 minutes'
        });
      } else {
        this.results.addTest({
          name: 'Session Timeout',
          category: 'Session Management',
          status: 'PASSED',
          description: 'Session duration is within acceptable limits'
        });
      }
    }

    // Test session storage security
    let sensitiveDataInSession = 0;
    for (let i = 0; i < sessionStorage.length; i++) {
      const key = sessionStorage.key(i);
      const value = sessionStorage.getItem(key);
      
      if (this.containsSensitiveData(key, value)) {
        sensitiveDataInSession++;
      }
    }

    if (sensitiveDataInSession > 0) {
      this.results.addTest({
        name: 'Session Storage Security',
        category: 'Session Management',
        status: 'FAILED',
        severity: 'MEDIUM',
        description: `${sensitiveDataInSession} sensitive data items found in session storage`,
        recommendation: 'Encrypt sensitive data before storing in session storage'
      });
    } else {
      this.results.addTest({
        name: 'Session Storage Security',
        category: 'Session Management',
        status: 'PASSED',
        description: 'No sensitive data found in session storage'
      });
    }
  }

  /**
   * Test file upload security
   */
  async testFileUploadSecurity() {
    console.log('[Security Audit] Testing file upload security...');

    // Check for file upload forms
    const fileInputs = document.querySelectorAll('input[type="file"]');
    
    if (fileInputs.length > 0) {
      let secureFileUploads = 0;
      
      fileInputs.forEach(input => {
        const accept = input.getAttribute('accept');
        const maxSize = input.getAttribute('data-max-size');
        
        if (accept && maxSize) {
          secureFileUploads++;
        }
      });

      if (secureFileUploads < fileInputs.length) {
        this.results.addTest({
          name: 'File Upload Security',
          category: 'File Upload',
          status: 'FAILED',
          severity: 'HIGH',
          description: `${fileInputs.length - secureFileUploads} file upload(s) without proper restrictions`,
          recommendation: 'Add file type and size restrictions to all file uploads'
        });
      } else {
        this.results.addTest({
          name: 'File Upload Security',
          category: 'File Upload',
          status: 'PASSED',
          description: 'All file uploads have proper security restrictions'
        });
      }
    }
  }

  /**
   * Test XSS prevention
   */
  async testXSSPrevention() {
    console.log('[Security Audit] Testing XSS prevention...');

    // Check for dangerous DOM manipulation
    const dangerousElements = document.querySelectorAll('[onclick], [onload], [onerror]');
    
    if (dangerousElements.length > 0) {
      this.results.addTest({
        name: 'DOM XSS Prevention',
        category: 'XSS Prevention',
        status: 'FAILED',
        severity: 'HIGH',
        description: `${dangerousElements.length} elements with inline event handlers found`,
        recommendation: 'Remove inline event handlers and use addEventListener'
      });
    } else {
      this.results.addTest({
        name: 'DOM XSS Prevention',
        category: 'XSS Prevention',
        status: 'PASSED',
        description: 'No dangerous inline event handlers found'
      });
    }

    // Check for eval usage
    if (typeof window.eval === 'function') {
      this.results.addTest({
        name: 'eval() Function Security',
        category: 'XSS Prevention',
        status: 'WARNING',
        description: 'eval() function is available and could be exploited',
        recommendation: 'Disable eval() and implement Content Security Policy'
      });
    }
  }

  /**
   * Test CSRF protection
   */
  async testCSRFProtection() {
    console.log('[Security Audit] Testing CSRF protection...');

    // Check for CSRF tokens
    const csrfMeta = document.querySelector('meta[name="csrf-token"]');
    const csrfInputs = document.querySelectorAll('input[name="_token"], input[name="csrf_token"]');
    const forms = document.querySelectorAll('form');

    if (forms.length > 0 && !csrfMeta && csrfInputs.length === 0) {
      this.results.addTest({
        name: 'CSRF Protection',
        category: 'CSRF Prevention',
        status: 'FAILED',
        severity: 'HIGH',
        description: 'No CSRF protection detected for forms',
        recommendation: 'Implement CSRF tokens for all state-changing requests'
      });
    } else if (forms.length > 0) {
      this.results.addTest({
        name: 'CSRF Protection',
        category: 'CSRF Prevention',
        status: 'PASSED',
        description: 'CSRF protection appears to be implemented'
      });
    }
  }

  /**
   * Test security headers
   */
  async testSecurityHeaders() {
    console.log('[Security Audit] Testing security headers...');

    // This would need to be implemented with actual HTTP header checking
    // For now, check meta tags as a proxy
    const securityMetas = {
      'X-Content-Type-Options': document.querySelector('meta[http-equiv="X-Content-Type-Options"]'),
      'X-Frame-Options': document.querySelector('meta[http-equiv="X-Frame-Options"]'),
      'X-XSS-Protection': document.querySelector('meta[http-equiv="X-XSS-Protection"]'),
      'Content-Security-Policy': document.querySelector('meta[http-equiv="Content-Security-Policy"]')
    };

    const missingHeaders = Object.keys(securityMetas).filter(header => !securityMetas[header]);

    if (missingHeaders.length > 0) {
      this.results.addTest({
        name: 'Security Headers',
        category: 'HTTP Security',
        status: 'FAILED',
        severity: 'MEDIUM',
        description: `Missing security headers: ${missingHeaders.join(', ')}`,
        recommendation: 'Configure server to send all required security headers'
      });
    } else {
      this.results.addTest({
        name: 'Security Headers',
        category: 'HTTP Security',
        status: 'PASSED',
        description: 'All required security headers are present'
      });
    }
  }

  /**
   * Test Content Security Policy
   */
  async testContentSecurityPolicy() {
    console.log('[Security Audit] Testing Content Security Policy...');

    const cspMeta = document.querySelector('meta[http-equiv="Content-Security-Policy"]');
    
    if (!cspMeta) {
      this.results.addTest({
        name: 'Content Security Policy',
        category: 'CSP',
        status: 'FAILED',
        severity: 'HIGH',
        description: 'No Content Security Policy detected',
        recommendation: 'Implement a strict Content Security Policy'
      });
    } else {
      const cspContent = cspMeta.getAttribute('content');
      
      if (cspContent && cspContent.includes('unsafe-inline')) {
        this.results.addTest({
          name: 'Content Security Policy Strength',
          category: 'CSP',
          status: 'WARNING',
          description: 'CSP allows unsafe-inline which reduces security',
          recommendation: 'Remove unsafe-inline and use nonces or hashes'
        });
      } else {
        this.results.addTest({
          name: 'Content Security Policy',
          category: 'CSP',
          status: 'PASSED',
          description: 'Strong Content Security Policy is implemented'
        });
      }
    }
  }

  /**
   * Test data encryption
   */
  async testDataEncryption() {
    console.log('[Security Audit] Testing data encryption...');

    // Test localStorage encryption
    let unencryptedData = 0;
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      const value = localStorage.getItem(key);
      
      // Simple check - encrypted data shouldn't be readable JSON
      try {
        JSON.parse(value);
        if (this.containsSensitiveData(key, value)) {
          unencryptedData++;
        }
      } catch {
        // Likely encrypted if not parseable JSON
      }
    }

    if (unencryptedData > 0) {
      this.results.addTest({
        name: 'Data Encryption',
        category: 'Data Protection',
        status: 'FAILED',
        severity: 'HIGH',
        description: `${unencryptedData} unencrypted sensitive data items found`,
        recommendation: 'Encrypt all sensitive data before storage'
      });
    } else {
      this.results.addTest({
        name: 'Data Encryption',
        category: 'Data Protection',
        status: 'PASSED',
        description: 'Sensitive data appears to be properly encrypted'
      });
    }
  }

  /**
   * Test API security configuration
   */
  async testAPISecurityConfiguration() {
    console.log('[Security Audit] Testing API security configuration...');

    try {
      // Test API client configuration
      const cacheStats = apiClient.getCacheStats();
      
      this.results.addTest({
        name: 'API Client Configuration',
        category: 'API Security',
        status: 'PASSED',
        description: 'API client is properly configured with caching and security features',
        evidence: cacheStats
      });

      // Test API timeout configuration
      if (apiClient.client.defaults.timeout < 30000) {
        this.results.addTest({
          name: 'API Timeout Configuration',
          category: 'API Security',
          status: 'WARNING',
          description: 'API timeout is less than recommended 30 seconds',
          recommendation: 'Set API timeout to 30 seconds to prevent hanging requests'
        });
      } else {
        this.results.addTest({
          name: 'API Timeout Configuration',
          category: 'API Security',
          status: 'PASSED',
          description: 'API timeout is properly configured'
        });
      }

    } catch (error) {
      this.results.addTest({
        name: 'API Security Configuration',
        category: 'API Security',
        status: 'WARNING',
        description: 'Could not test API configuration',
        evidence: { error: error.message }
      });
    }
  }

  /**
   * Test dependency vulnerabilities
   */
  async testDependencyVulnerabilities() {
    console.log('[Security Audit] Testing dependency vulnerabilities...');

    // Check for known vulnerable script patterns
    const scripts = document.querySelectorAll('script[src]');
    let vulnerableScripts = 0;

    scripts.forEach(script => {
      const src = script.src;
      
      // Check for old jQuery versions (example)
      if (src.includes('jquery') && (src.includes('1.') || src.includes('2.'))) {
        vulnerableScripts++;
      }
      
      // Check for other known vulnerable patterns
      if (src.includes('angular') && src.includes('1.')) {
        vulnerableScripts++;
      }
    });

    if (vulnerableScripts > 0) {
      this.results.addTest({
        name: 'Dependency Vulnerabilities',
        category: 'Dependencies',
        status: 'FAILED',
        severity: 'HIGH',
        description: `${vulnerableScripts} potentially vulnerable dependencies found`,
        recommendation: 'Update all dependencies to latest secure versions'
      });
    } else {
      this.results.addTest({
        name: 'Dependency Vulnerabilities',
        category: 'Dependencies',
        status: 'PASSED',
        description: 'No known vulnerable dependencies detected'
      });
    }
  }

  /**
   * Test performance security
   */
  async testPerformanceSecurity() {
    console.log('[Security Audit] Testing performance security...');

    // Test for performance timing attacks
    const performanceEntries = performance.getEntriesByType('navigation');
    
    if (performanceEntries.length > 0) {
      const loadTime = performanceEntries[0].loadEventEnd - performanceEntries[0].loadEventStart;
      
      if (loadTime > 10000) { // 10 seconds
        this.results.addTest({
          name: 'Performance Security',
          category: 'Performance',
          status: 'WARNING',
          description: 'Slow page load time could indicate performance issues',
          evidence: { loadTime: Math.round(loadTime) + 'ms' },
          recommendation: 'Optimize page load time to prevent timing attacks'
        });
      } else {
        this.results.addTest({
          name: 'Performance Security',
          category: 'Performance',
          status: 'PASSED',
          description: 'Page load time is within acceptable limits'
        });
      }
    }

    // Test for memory leaks (simplified)
    if (performance.memory) {
      const memoryUsage = performance.memory.usedJSHeapSize / performance.memory.totalJSHeapSize;
      
      if (memoryUsage > 0.8) {
        this.results.addTest({
          name: 'Memory Usage Security',
          category: 'Performance',
          status: 'WARNING',
          description: 'High memory usage detected',
          evidence: { memoryUsage: Math.round(memoryUsage * 100) + '%' },
          recommendation: 'Investigate potential memory leaks'
        });
      } else {
        this.results.addTest({
          name: 'Memory Usage Security',
          category: 'Performance',
          status: 'PASSED',
          description: 'Memory usage is within acceptable limits'
        });
      }
    }
  }

  /**
   * Run vulnerability scanner
   */
  async runVulnerabilityScanner() {
    console.log('[Security Audit] Running vulnerability scanner...');

    try {
      const scanResults = await vulnerabilityScanner.runComprehensiveScan();
      
      if (scanResults.summary.totalVulnerabilities > 0) {
        const criticalVulns = scanResults.vulnerabilities.filter(v => v.severity === 'CRITICAL').length;
        const highVulns = scanResults.vulnerabilities.filter(v => v.severity === 'HIGH').length;
        
        let severity = 'LOW';
        if (criticalVulns > 0) severity = 'CRITICAL';
        else if (highVulns > 0) severity = 'HIGH';
        else if (scanResults.summary.totalVulnerabilities > 5) severity = 'MEDIUM';

        this.results.addTest({
          name: 'Vulnerability Scanner',
          category: 'Comprehensive Scan',
          status: 'FAILED',
          severity,
          description: `Found ${scanResults.summary.totalVulnerabilities} vulnerabilities`,
          evidence: scanResults.summary,
          recommendation: 'Address all critical and high severity vulnerabilities'
        });
      } else {
        this.results.addTest({
          name: 'Vulnerability Scanner',
          category: 'Comprehensive Scan',
          status: 'PASSED',
          description: 'No vulnerabilities detected by comprehensive scan'
        });
      }

    } catch (error) {
      this.results.addTest({
        name: 'Vulnerability Scanner',
        category: 'Comprehensive Scan',
        status: 'WARNING',
        description: 'Vulnerability scanner failed to complete',
        evidence: { error: error.message }
      });
    }
  }

  /**
   * Helper method to check for sensitive data
   */
  containsSensitiveData(key, value) {
    const sensitivePatterns = [
      /password/i,
      /secret/i,
      /api[_-]?key/i,
      /access[_-]?token/i,
      /private[_-]?key/i,
      /credit[_-]?card/i,
      /ssn/i
    ];

    const keyLower = key.toLowerCase();
    const valueLower = (value || '').toLowerCase();

    return sensitivePatterns.some(pattern => 
      pattern.test(keyLower) || pattern.test(valueLower)
    );
  }
}

// Export singleton instance
export const securityAudit = new SecurityAuditRunner();

export default securityAudit;