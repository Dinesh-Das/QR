/**
 * CORS Configuration Validator
 * 
 * Validates Cross-Origin Resource Sharing (CORS) configuration
 * for the QRMFG application to ensure secure API access.
 * 
 * @module CORSValidator
 * @since 1.0.0
 * @author QRMFG Security Team
 */

/**
 * CORS Configuration Validation and Testing
 */
export class CORSValidator {
  constructor() {
    this.results = {
      isConfigured: false,
      tests: [],
      recommendations: [],
      corsHeaders: {},
      timestamp: new Date().toISOString()
    };
  }

  /**
   * Run comprehensive CORS validation
   * @returns {Promise<Object>} CORS validation results
   */
  async validate() {
    console.log('[CORS Validator] Starting CORS configuration validation...');

    try {
      // Test 1: Check CORS headers on API endpoints
      await this.validateAPIEndpointCORS();

      // Test 2: Test preflight requests
      await this.validatePreflightRequests();

      // Test 3: Check allowed origins
      await this.validateAllowedOrigins();

      // Test 4: Check allowed methods
      await this.validateAllowedMethods();

      // Test 5: Check allowed headers
      await this.validateAllowedHeaders();

      // Test 6: Check credentials handling
      await this.validateCredentialsHandling();

      // Calculate overall CORS status
      this.calculateCORSStatus();

      console.log(`[CORS Validator] Validation completed: ${this.results.isConfigured ? 'CONFIGURED' : 'NOT_CONFIGURED'}`);
      return this.results;

    } catch (error) {
      console.error('[CORS Validator] Validation failed:', error);
      this.results.tests.push({
        name: 'CORS Validation Error',
        status: 'FAILED',
        severity: 'HIGH',
        description: `CORS validation failed: ${error.message}`,
        recommendation: 'Fix CORS validation errors and retry'
      });
      return this.results;
    }
  }

  /**
   * Validate CORS headers on API endpoints
   */
  async validateAPIEndpointCORS() {
    const apiBaseURL = process.env.REACT_APP_API_BASE_URL || '/qrmfg/api/v1';
    const testEndpoints = [
      `${apiBaseURL}/health`,
      `${apiBaseURL}/auth/validate`,
      `${apiBaseURL}/users`,
      `${apiBaseURL}/workflows`
    ];

    const corsResults = [];

    for (const endpoint of testEndpoints) {
      try {
        // Make a simple GET request to check CORS headers
        const response = await fetch(endpoint, {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json'
          }
        });

        const corsHeaders = this.extractCORSHeaders(response);
        corsResults.push({
          endpoint,
          status: response.status,
          corsHeaders,
          hasCORS: Object.keys(corsHeaders).length > 0
        });

        // Store CORS headers for analysis
        if (Object.keys(corsHeaders).length > 0) {
          this.results.corsHeaders[endpoint] = corsHeaders;
        }

      } catch (error) {
        corsResults.push({
          endpoint,
          status: 'ERROR',
          error: error.message,
          hasCORS: false
        });
      }
    }

    const endpointsWithCORS = corsResults.filter(result => result.hasCORS).length;
    const totalEndpoints = corsResults.length;

    this.results.tests.push({
      name: 'API Endpoint CORS Headers',
      status: endpointsWithCORS > 0 ? 'PASSED' : 'FAILED',
      severity: endpointsWithCORS === 0 ? 'HIGH' : 'INFO',
      description: `${endpointsWithCORS}/${totalEndpoints} API endpoints have CORS headers`,
      recommendation: endpointsWithCORS === totalEndpoints ? 
        'All API endpoints have CORS headers configured' : 
        'Configure CORS headers for all API endpoints',
      details: {
        results: corsResults,
        endpointsWithCORS,
        totalEndpoints
      }
    });

    if (endpointsWithCORS === 0) {
      this.results.recommendations.push('Configure CORS headers on the API server');
    }
  }

  /**
   * Validate preflight requests (OPTIONS method)
   */
  async validatePreflightRequests() {
    const apiBaseURL = process.env.REACT_APP_API_BASE_URL || '/qrmfg/api/v1';
    const testEndpoint = `${apiBaseURL}/users`;

    try {
      // Make an OPTIONS request to test preflight
      const response = await fetch(testEndpoint, {
        method: 'OPTIONS',
        headers: {
          'Origin': window.location.origin,
          'Access-Control-Request-Method': 'POST',
          'Access-Control-Request-Headers': 'Content-Type, Authorization'
        }
      });

      const corsHeaders = this.extractCORSHeaders(response);
      const hasPreflightSupport = response.status === 200 || response.status === 204;

      this.results.tests.push({
        name: 'Preflight Request Support',
        status: hasPreflightSupport ? 'PASSED' : 'FAILED',
        severity: hasPreflightSupport ? 'INFO' : 'MEDIUM',
        description: hasPreflightSupport ? 
          'Preflight requests are supported' : 
          'Preflight requests are not properly handled',
        recommendation: hasPreflightSupport ? 
          'Preflight requests work correctly' : 
          'Configure server to handle OPTIONS requests for CORS preflight',
        details: {
          status: response.status,
          corsHeaders,
          endpoint: testEndpoint
        }
      });

      if (!hasPreflightSupport) {
        this.results.recommendations.push('Configure server to handle CORS preflight OPTIONS requests');
      }

    } catch (error) {
      this.results.tests.push({
        name: 'Preflight Request Support',
        status: 'FAILED',
        severity: 'MEDIUM',
        description: `Preflight request failed: ${error.message}`,
        recommendation: 'Ensure server supports CORS preflight requests'
      });
    }
  }

  /**
   * Validate allowed origins configuration
   */
  async validateAllowedOrigins() {
    const corsHeaders = Object.values(this.results.corsHeaders);
    if (corsHeaders.length === 0) {
      this.results.tests.push({
        name: 'Allowed Origins',
        status: 'SKIPPED',
        severity: 'LOW',
        description: 'No CORS headers found to validate origins',
        recommendation: 'Configure CORS headers first'
      });
      return;
    }

    const allowedOrigins = new Set();
    let hasWildcard = false;

    corsHeaders.forEach(headers => {
      if (headers['access-control-allow-origin']) {
        const origin = headers['access-control-allow-origin'];
        if (origin === '*') {
          hasWildcard = true;
        } else {
          allowedOrigins.add(origin);
        }
      }
    });

    const currentOrigin = window.location.origin;
    const isCurrentOriginAllowed = hasWildcard || allowedOrigins.has(currentOrigin);

    // Check if wildcard is used with credentials (security issue)
    const hasCredentials = corsHeaders.some(headers => 
      headers['access-control-allow-credentials'] === 'true'
    );

    const isSecure = !(hasWildcard && hasCredentials);

    this.results.tests.push({
      name: 'Allowed Origins',
      status: isCurrentOriginAllowed && isSecure ? 'PASSED' : 'WARNING',
      severity: !isSecure ? 'HIGH' : 'INFO',
      description: hasWildcard ? 
        'Wildcard (*) origin is allowed' : 
        `${allowedOrigins.size} specific origins are allowed`,
      recommendation: !isSecure ? 
        'Do not use wildcard origin (*) with credentials' : 
        isCurrentOriginAllowed ? 
          'Origin configuration is appropriate' : 
          'Add current origin to allowed origins list',
      details: {
        allowedOrigins: Array.from(allowedOrigins),
        hasWildcard,
        currentOrigin,
        isCurrentOriginAllowed,
        hasCredentials,
        isSecure
      }
    });

    if (!isSecure) {
      this.results.recommendations.push('Configure specific allowed origins instead of using wildcard with credentials');
    }

    if (!isCurrentOriginAllowed && !hasWildcard) {
      this.results.recommendations.push(`Add ${currentOrigin} to the allowed origins list`);
    }
  }

  /**
   * Validate allowed methods configuration
   */
  async validateAllowedMethods() {
    const corsHeaders = Object.values(this.results.corsHeaders);
    if (corsHeaders.length === 0) {
      return;
    }

    const allowedMethods = new Set();
    corsHeaders.forEach(headers => {
      if (headers['access-control-allow-methods']) {
        const methods = headers['access-control-allow-methods']
          .split(',')
          .map(method => method.trim().toUpperCase());
        methods.forEach(method => allowedMethods.add(method));
      }
    });

    const requiredMethods = ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'];
    const missingMethods = requiredMethods.filter(method => !allowedMethods.has(method));

    this.results.tests.push({
      name: 'Allowed Methods',
      status: missingMethods.length === 0 ? 'PASSED' : 'WARNING',
      severity: missingMethods.length > 2 ? 'MEDIUM' : 'LOW',
      description: `${allowedMethods.size} HTTP methods are allowed`,
      recommendation: missingMethods.length === 0 ? 
        'All required HTTP methods are allowed' : 
        `Consider allowing these methods: ${missingMethods.join(', ')}`,
      details: {
        allowedMethods: Array.from(allowedMethods),
        requiredMethods,
        missingMethods
      }
    });

    if (missingMethods.length > 0) {
      this.results.recommendations.push(`Configure CORS to allow these HTTP methods: ${missingMethods.join(', ')}`);
    }
  }

  /**
   * Validate allowed headers configuration
   */
  async validateAllowedHeaders() {
    const corsHeaders = Object.values(this.results.corsHeaders);
    if (corsHeaders.length === 0) {
      return;
    }

    const allowedHeaders = new Set();
    corsHeaders.forEach(headers => {
      if (headers['access-control-allow-headers']) {
        const headersList = headers['access-control-allow-headers']
          .split(',')
          .map(header => header.trim().toLowerCase());
        headersList.forEach(header => allowedHeaders.add(header));
      }
    });

    const requiredHeaders = ['content-type', 'authorization', 'x-requested-with'];
    const missingHeaders = requiredHeaders.filter(header => !allowedHeaders.has(header));

    this.results.tests.push({
      name: 'Allowed Headers',
      status: missingHeaders.length === 0 ? 'PASSED' : 'WARNING',
      severity: missingHeaders.includes('authorization') ? 'MEDIUM' : 'LOW',
      description: `${allowedHeaders.size} headers are allowed`,
      recommendation: missingHeaders.length === 0 ? 
        'All required headers are allowed' : 
        `Consider allowing these headers: ${missingHeaders.join(', ')}`,
      details: {
        allowedHeaders: Array.from(allowedHeaders),
        requiredHeaders,
        missingHeaders
      }
    });

    if (missingHeaders.length > 0) {
      this.results.recommendations.push(`Configure CORS to allow these headers: ${missingHeaders.join(', ')}`);
    }
  }

  /**
   * Validate credentials handling
   */
  async validateCredentialsHandling() {
    const corsHeaders = Object.values(this.results.corsHeaders);
    if (corsHeaders.length === 0) {
      return;
    }

    let allowsCredentials = false;
    let hasWildcardOrigin = false;

    corsHeaders.forEach(headers => {
      if (headers['access-control-allow-credentials'] === 'true') {
        allowsCredentials = true;
      }
      if (headers['access-control-allow-origin'] === '*') {
        hasWildcardOrigin = true;
      }
    });

    const isSecureCredentialsConfig = !allowsCredentials || !hasWildcardOrigin;

    this.results.tests.push({
      name: 'Credentials Handling',
      status: isSecureCredentialsConfig ? 'PASSED' : 'FAILED',
      severity: !isSecureCredentialsConfig ? 'HIGH' : 'INFO',
      description: allowsCredentials ? 
        'Credentials are allowed in CORS requests' : 
        'Credentials are not allowed in CORS requests',
      recommendation: !isSecureCredentialsConfig ? 
        'Do not use wildcard origin with credentials enabled' : 
        allowsCredentials ? 
          'Credentials handling is properly configured' : 
          'Consider if credentials should be allowed for authenticated requests',
      details: {
        allowsCredentials,
        hasWildcardOrigin,
        isSecureCredentialsConfig
      }
    });

    if (!isSecureCredentialsConfig) {
      this.results.recommendations.push('Configure specific origins when allowing credentials in CORS');
    }
  }

  /**
   * Calculate overall CORS status
   */
  calculateCORSStatus() {
    const totalTests = this.results.tests.length;
    const passedTests = this.results.tests.filter(test => test.status === 'PASSED').length;
    const failedTests = this.results.tests.filter(test => test.status === 'FAILED').length;
    const criticalFailures = this.results.tests.filter(test => 
      test.status === 'FAILED' && test.severity === 'HIGH'
    ).length;

    // Consider configured if most tests pass and no critical failures
    this.results.isConfigured = (passedTests >= totalTests / 2) && criticalFailures === 0;

    this.results.summary = {
      totalTests,
      passedTests,
      failedTests,
      criticalFailures,
      configurationScore: Math.round((passedTests / totalTests) * 100),
      hasCORSHeaders: Object.keys(this.results.corsHeaders).length > 0
    };

    // Add environment-specific recommendations
    if (process.env.NODE_ENV === 'production' && !this.results.isConfigured) {
      this.results.recommendations.unshift('Configure CORS properly for production deployment');
    }
  }

  /**
   * Extract CORS headers from response
   * @param {Response} response - Fetch response object
   * @returns {Object} CORS headers
   */
  extractCORSHeaders(response) {
    const corsHeaders = {};
    const corsHeaderNames = [
      'access-control-allow-origin',
      'access-control-allow-methods',
      'access-control-allow-headers',
      'access-control-allow-credentials',
      'access-control-expose-headers',
      'access-control-max-age'
    ];

    corsHeaderNames.forEach(headerName => {
      const headerValue = response.headers.get(headerName);
      if (headerValue) {
        corsHeaders[headerName] = headerValue;
      }
    });

    return corsHeaders;
  }

  /**
   * Generate CORS configuration recommendations
   * @returns {Array} Array of configuration recommendations
   */
  generateRecommendations() {
    const recommendations = [...this.results.recommendations];

    // Add general CORS best practices
    recommendations.push(
      'Use specific origins instead of wildcard (*) when possible',
      'Only allow necessary HTTP methods and headers',
      'Set appropriate max-age for preflight cache',
      'Monitor CORS errors in browser console',
      'Test CORS configuration from different origins'
    );

    return recommendations;
  }

  /**
   * Test CORS from different origins (for development)
   * @param {Array} testOrigins - Origins to test from
   * @returns {Promise<Object>} Test results
   */
  async testFromOrigins(testOrigins = []) {
    const results = {};

    for (const origin of testOrigins) {
      try {
        // This would need to be implemented with a proxy or test server
        // For now, we'll simulate the test
        results[origin] = {
          status: 'SIMULATED',
          message: 'CORS testing from different origins requires server-side testing'
        };
      } catch (error) {
        results[origin] = {
          status: 'ERROR',
          message: error.message
        };
      }
    }

    return results;
  }
}

export default CORSValidator;