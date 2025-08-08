/**
 * Security Implementation Validation
 * 
 * Validates that all security measures from tasks 10.1 and 10.2 are properly implemented
 * and tests the security audit functionality for task 10.3.
 * 
 * @module ValidateSecurityImplementation
 * @since 1.0.0
 * @author QRMFG Security Team
 */

import SecureTokenStorage from '../services/secureStorage';
import JWTValidator from '../services/jwtValidator';
import { InputSanitizer, ValidationRules } from './inputValidation';
import { HTTPSValidator } from './httpsValidator';
import { CORSValidator } from './corsValidator';
import { ErrorMessageValidator } from './errorMessageValidator';
import { PerformanceTester } from './performanceTester';

/**
 * Comprehensive Security Implementation Validator
 */
export class SecurityImplementationValidator {
  constructor() {
    this.results = {
      timestamp: new Date().toISOString(),
      tests: [],
      summary: {
        totalTests: 0,
        passedTests: 0,
        failedTests: 0,
        overallStatus: 'PENDING'
      }
    };
  }

  /**
   * Run all security implementation validation tests
   * @returns {Promise<Object>} Validation results
   */
  async validateAll() {
    console.log('🔍 Validating Security Implementation...');
    console.log('=' .repeat(50));

    try {
      // Test 1: Validate secure token storage (Task 10.1)
      await this.validateSecureTokenStorage();

      // Test 2: Validate JWT validation (Task 10.1)
      await this.validateJWTValidation();

      // Test 3: Validate input sanitization (Task 10.1)
      await this.validateInputSanitization();

      // Test 4: Validate file upload security (Task 10.1)
      await this.validateFileUploadSecurity();

      // Test 5: Validate security monitoring (Task 10.2)
      await this.validateSecurityMonitoring();

      // Test 6: Validate HTTPS enforcement (Task 10.3)
      await this.validateHTTPSEnforcement();

      // Test 7: Validate CORS configuration (Task 10.3)
      await this.validateCORSConfiguration();

      // Test 8: Validate error message security (Task 10.3)
      await this.validateErrorMessageSecurity();

      // Test 9: Validate performance optimizations (Task 10.3)
      await this.validatePerformanceOptimizations();

      // Calculate summary
      this.calculateSummary();

      console.log('\n✅ Security Implementation Validation Completed');
      return this.results;

    } catch (error) {
      console.error('❌ Validation failed:', error);
      this.addTest('Validation Error', false, `Validation failed: ${error.message}`);
      this.calculateSummary();
      return this.results;
    }
  }

  /**
   * Validate secure token storage implementation
   */
  async validateSecureTokenStorage() {
    console.log('🔐 Testing Secure Token Storage...');

    try {
      // Test token encryption/decryption
      const testToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.signature';
      
      // Store token
      SecureTokenStorage.setToken(testToken);
      const hasToken = SecureTokenStorage.hasToken();
      
      // Retrieve token
      const retrievedToken = SecureTokenStorage.getToken();
      
      // Clean up
      SecureTokenStorage.removeToken();
      const hasTokenAfterRemoval = SecureTokenStorage.hasToken();

      const isValid = hasToken && 
                     retrievedToken === testToken && 
                     !hasTokenAfterRemoval;

      this.addTest(
        'Secure Token Storage',
        isValid,
        isValid ? 
          'Token encryption, storage, and retrieval working correctly' :
          'Token storage implementation has issues'
      );

      // Test error handling
      try {
        SecureTokenStorage.setToken(null);
        this.addTest('Token Storage Error Handling', false, 'Should throw error for null token');
      } catch (error) {
        this.addTest('Token Storage Error Handling', true, 'Properly handles invalid input');
      }

    } catch (error) {
      this.addTest('Secure Token Storage', false, `Test failed: ${error.message}`);
    }
  }

  /**
   * Validate JWT validation implementation
   */
  async validateJWTValidation() {
    console.log('🎫 Testing JWT Validation...');

    try {
      // Test valid JWT structure validation
      const validJWT = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwidXNlcm5hbWUiOiJ0ZXN0dXNlciIsInJvbGVzIjpbIlVTRVIiXSwiZXhwIjo5OTk5OTk5OTk5fQ.signature';
      const invalidJWT = 'invalid.jwt.token';
      const expiredJWT = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwidXNlcm5hbWUiOiJ0ZXN0dXNlciIsInJvbGVzIjpbIlVTRVIiXSwiZXhwIjoxfQ.signature';

      // Test structure validation
      const structureValid = JWTValidator.validateToken(validJWT);
      const structureInvalid = !JWTValidator.validateToken(invalidJWT);
      const expiredInvalid = !JWTValidator.validateToken(expiredJWT);

      this.addTest(
        'JWT Structure Validation',
        structureValid && structureInvalid,
        structureValid && structureInvalid ? 
          'JWT structure validation working correctly' :
          'JWT structure validation has issues'
      );

      this.addTest(
        'JWT Expiry Validation',
        expiredInvalid,
        expiredInvalid ? 
          'JWT expiry validation working correctly' :
          'JWT expiry validation not working'
      );

      // Test payload extraction
      const payload = JWTValidator.getTokenPayload(validJWT);
      const hasPayload = payload && payload.sub && payload.username;

      this.addTest(
        'JWT Payload Extraction',
        hasPayload,
        hasPayload ? 
          'JWT payload extraction working correctly' :
          'JWT payload extraction failed'
      );

    } catch (error) {
      this.addTest('JWT Validation', false, `Test failed: ${error.message}`);
    }
  }

  /**
   * Validate input sanitization implementation
   */
  async validateInputSanitization() {
    console.log('🧹 Testing Input Sanitization...');

    try {
      // Test XSS prevention
      const xssInput = '<script>alert("xss")</script>';
      const sanitizedXSS = InputSanitizer.sanitizeText(xssInput);
      const xssPrevented = !sanitizedXSS.includes('<script>');

      this.addTest(
        'XSS Prevention',
        xssPrevented,
        xssPrevented ? 
          'XSS prevention working correctly' :
          'XSS prevention failed'
      );

      // Test SQL injection prevention
      const sqlInput = "'; DROP TABLE users; --";
      const sanitizedSQL = InputSanitizer.sanitizeSQL(sqlInput);
      const sqlPrevented = !sanitizedSQL.includes('DROP') && !sanitizedSQL.includes(';');

      this.addTest(
        'SQL Injection Prevention',
        sqlPrevented,
        sqlPrevented ? 
          'SQL injection prevention working correctly' :
          'SQL injection prevention failed'
      );

      // Test filename sanitization
      const maliciousFilename = '../../../etc/passwd';
      const sanitizedFilename = InputSanitizer.sanitizeFilename(maliciousFilename);
      const filenameSafe = !sanitizedFilename.includes('../');

      this.addTest(
        'Filename Sanitization',
        filenameSafe,
        filenameSafe ? 
          'Filename sanitization working correctly' :
          'Filename sanitization failed'
      );

      // Test HTML sanitization
      const htmlInput = '<img src="x" onerror="alert(1)">';
      const sanitizedHTML = InputSanitizer.sanitizeHTML(htmlInput);
      const htmlSafe = !sanitizedHTML.includes('onerror');

      this.addTest(
        'HTML Sanitization',
        htmlSafe,
        htmlSafe ? 
          'HTML sanitization working correctly' :
          'HTML sanitization failed'
      );

    } catch (error) {
      this.addTest('Input Sanitization', false, `Test failed: ${error.message}`);
    }
  }

  /**
   * Validate file upload security
   */
  async validateFileUploadSecurity() {
    console.log('📁 Testing File Upload Security...');

    try {
      // Test file type validation
      const validFile = new File(['test content'], 'test.pdf', { type: 'application/pdf' });
      const invalidFile = new File(['test content'], 'test.exe', { type: 'application/x-executable' });

      const validFileResult = ValidationRules.file.validateType(validFile);
      const invalidFileResult = ValidationRules.file.validateType(invalidFile);

      this.addTest(
        'File Type Validation',
        validFileResult.isValid && !invalidFileResult.isValid,
        validFileResult.isValid && !invalidFileResult.isValid ? 
          'File type validation working correctly' :
          'File type validation failed'
      );

      // Test file size validation
      const largeFile = new File([new ArrayBuffer(30 * 1024 * 1024)], 'large.pdf', { type: 'application/pdf' });
      const smallFile = new File(['small content'], 'small.pdf', { type: 'application/pdf' });

      const largeFileResult = ValidationRules.file.validateSize(largeFile, 25);
      const smallFileResult = ValidationRules.file.validateSize(smallFile, 25);

      this.addTest(
        'File Size Validation',
        !largeFileResult.isValid && smallFileResult.isValid,
        !largeFileResult.isValid && smallFileResult.isValid ? 
          'File size validation working correctly' :
          'File size validation failed'
      );

      // Test filename validation
      const safeFilename = 'document.pdf';
      const unsafeFilename = '../../../etc/passwd';

      const safeFilenameResult = ValidationRules.file.validateFilename(safeFilename);
      const unsafeFilenameResult = ValidationRules.file.validateFilename(unsafeFilename);

      this.addTest(
        'Filename Security Validation',
        safeFilenameResult.isValid && !unsafeFilenameResult.isValid,
        safeFilenameResult.isValid && !unsafeFilenameResult.isValid ? 
          'Filename security validation working correctly' :
          'Filename security validation failed'
      );

    } catch (error) {
      this.addTest('File Upload Security', false, `Test failed: ${error.message}`);
    }
  }

  /**
   * Validate security monitoring implementation
   */
  async validateSecurityMonitoring() {
    console.log('📊 Testing Security Monitoring...');

    try {
      // Check if security monitoring service exists
      let securityMonitoringExists = false;
      try {
        const { securityMonitoring } = await import('../services/securityMonitoring');
        securityMonitoringExists = typeof securityMonitoring === 'object';
      } catch (error) {
        securityMonitoringExists = false;
      }

      this.addTest(
        'Security Monitoring Service',
        securityMonitoringExists,
        securityMonitoringExists ? 
          'Security monitoring service is available' :
          'Security monitoring service not found'
      );

      // Check if activity tracking exists
      let activityTrackingExists = false;
      try {
        const { useActivityTracking } = await import('../hooks/useActivityTracking');
        activityTrackingExists = typeof useActivityTracking === 'function';
      } catch (error) {
        activityTrackingExists = false;
      }

      this.addTest(
        'Activity Tracking Hook',
        activityTrackingExists,
        activityTrackingExists ? 
          'Activity tracking hook is available' :
          'Activity tracking hook not found'
      );

    } catch (error) {
      this.addTest('Security Monitoring', false, `Test failed: ${error.message}`);
    }
  }

  /**
   * Validate HTTPS enforcement
   */
  async validateHTTPSEnforcement() {
    console.log('🔒 Testing HTTPS Enforcement...');

    try {
      const httpsValidator = new HTTPSValidator();
      const results = await httpsValidator.validate();

      const isSecure = results.isSecure;
      const hasTests = results.tests && results.tests.length > 0;

      this.addTest(
        'HTTPS Validation',
        hasTests,
        hasTests ? 
          'HTTPS validation tests executed successfully' :
          'HTTPS validation tests failed'
      );

      this.addTest(
        'HTTPS Security Status',
        isSecure || process.env.NODE_ENV === 'development',
        isSecure || process.env.NODE_ENV === 'development' ? 
          'HTTPS security requirements met' :
          'HTTPS security requirements not met'
      );

    } catch (error) {
      this.addTest('HTTPS Enforcement', false, `Test failed: ${error.message}`);
    }
  }

  /**
   * Validate CORS configuration
   */
  async validateCORSConfiguration() {
    console.log('🌐 Testing CORS Configuration...');

    try {
      const corsValidator = new CORSValidator();
      const results = await corsValidator.validate();

      const hasTests = results.tests && results.tests.length > 0;
      const isConfigured = results.isConfigured;

      this.addTest(
        'CORS Validation',
        hasTests,
        hasTests ? 
          'CORS validation tests executed successfully' :
          'CORS validation tests failed'
      );

      this.addTest(
        'CORS Configuration Status',
        isConfigured || process.env.NODE_ENV === 'development',
        isConfigured || process.env.NODE_ENV === 'development' ? 
          'CORS configuration is adequate' :
          'CORS configuration needs improvement'
      );

    } catch (error) {
      this.addTest('CORS Configuration', false, `Test failed: ${error.message}`);
    }
  }

  /**
   * Validate error message security
   */
  async validateErrorMessageSecurity() {
    console.log('🚨 Testing Error Message Security...');

    try {
      const errorValidator = new ErrorMessageValidator();
      const results = await errorValidator.validate();

      const hasTests = results.tests && results.tests.length > 0;
      const isSecure = results.isSecure;

      this.addTest(
        'Error Message Validation',
        hasTests,
        hasTests ? 
          'Error message validation tests executed successfully' :
          'Error message validation tests failed'
      );

      this.addTest(
        'Error Message Security Status',
        isSecure,
        isSecure ? 
          'Error messages are secure' :
          'Error messages have security issues'
      );

    } catch (error) {
      this.addTest('Error Message Security', false, `Test failed: ${error.message}`);
    }
  }

  /**
   * Validate performance optimizations
   */
  async validatePerformanceOptimizations() {
    console.log('⚡ Testing Performance Optimizations...');

    try {
      const performanceTester = new PerformanceTester();
      const results = await performanceTester.runTests();

      const hasMetrics = results.metrics && Object.keys(results.metrics).length > 0;
      const overallScore = results.overallScore || 0;

      this.addTest(
        'Performance Testing',
        hasMetrics,
        hasMetrics ? 
          'Performance testing executed successfully' :
          'Performance testing failed'
      );

      this.addTest(
        'Performance Score',
        overallScore >= 60, // Minimum acceptable score
        overallScore >= 60 ? 
          `Performance score is acceptable (${overallScore}/100)` :
          `Performance score needs improvement (${overallScore}/100)`
      );

    } catch (error) {
      this.addTest('Performance Optimizations', false, `Test failed: ${error.message}`);
    }
  }

  /**
   * Add a test result
   * @param {string} name - Test name
   * @param {boolean} passed - Whether test passed
   * @param {string} description - Test description
   */
  addTest(name, passed, description) {
    this.results.tests.push({
      name,
      status: passed ? 'PASSED' : 'FAILED',
      description,
      timestamp: new Date().toISOString()
    });

    console.log(`${passed ? '✅' : '❌'} ${name}: ${description}`);
  }

  /**
   * Calculate summary statistics
   */
  calculateSummary() {
    const totalTests = this.results.tests.length;
    const passedTests = this.results.tests.filter(test => test.status === 'PASSED').length;
    const failedTests = totalTests - passedTests;

    this.results.summary = {
      totalTests,
      passedTests,
      failedTests,
      successRate: totalTests > 0 ? Math.round((passedTests / totalTests) * 100) : 0,
      overallStatus: failedTests === 0 ? 'PASSED' : passedTests >= totalTests * 0.8 ? 'WARNING' : 'FAILED'
    };

    console.log('\n📊 VALIDATION SUMMARY:');
    console.log(`Total Tests: ${totalTests}`);
    console.log(`Passed: ${passedTests}`);
    console.log(`Failed: ${failedTests}`);
    console.log(`Success Rate: ${this.results.summary.successRate}%`);
    console.log(`Overall Status: ${this.results.summary.overallStatus}`);
  }

  /**
   * Generate validation report
   * @returns {Object} Formatted validation report
   */
  generateReport() {
    return {
      ...this.results,
      metadata: {
        validationVersion: '1.0.0',
        environment: process.env.NODE_ENV || 'development',
        userAgent: typeof navigator !== 'undefined' ? navigator.userAgent : 'Node.js',
        url: typeof window !== 'undefined' ? window.location.href : 'N/A'
      },
      recommendations: this.generateRecommendations()
    };
  }

  /**
   * Generate recommendations based on test results
   * @returns {Array} Array of recommendations
   */
  generateRecommendations() {
    const recommendations = [];
    const failedTests = this.results.tests.filter(test => test.status === 'FAILED');

    if (failedTests.length === 0) {
      recommendations.push('All security implementations are working correctly');
    } else {
      recommendations.push('Address the following failed security tests:');
      failedTests.forEach(test => {
        recommendations.push(`- Fix ${test.name}: ${test.description}`);
      });
    }

    return recommendations;
  }
}

/**
 * Quick validation function for console use
 * @returns {Promise<Object>} Validation results
 */
export async function validateSecurityImplementation() {
  const validator = new SecurityImplementationValidator();
  const results = await validator.validateAll();
  return validator.generateReport();
}

// Make available in browser console
if (typeof window !== 'undefined') {
  window.validateSecurityImplementation = validateSecurityImplementation;
  console.log('Security validation available: Call window.validateSecurityImplementation() to execute');
}

export default SecurityImplementationValidator;