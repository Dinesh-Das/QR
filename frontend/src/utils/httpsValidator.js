/**
 * HTTPS Enforcement Validator
 * 
 * Validates HTTPS enforcement and secure connection requirements
 * for the QRMFG application in production environments.
 * 
 * @module HTTPSValidator
 * @since 1.0.0
 * @author QRMFG Security Team
 */

/**
 * HTTPS Validation and Enforcement Checker
 */
export class HTTPSValidator {
  constructor() {
    this.results = {
      isSecure: false,
      protocol: window.location.protocol,
      host: window.location.host,
      tests: [],
      recommendations: [],
      timestamp: new Date().toISOString()
    };
  }

  /**
   * Run comprehensive HTTPS validation
   * @returns {Promise<Object>} HTTPS validation results
   */
  async validate() {
    console.log('[HTTPS Validator] Starting HTTPS enforcement validation...');

    try {
      // Test 1: Check current protocol
      this.validateCurrentProtocol();

      // Test 2: Check secure context
      this.validateSecureContext();

      // Test 3: Check mixed content
      this.validateMixedContent();

      // Test 4: Check security headers (if available)
      await this.validateSecurityHeaders();

      // Test 5: Check certificate validity (production only)
      if (process.env.NODE_ENV === 'production') {
        await this.validateCertificate();
      }

      // Test 6: Check HSTS header
      await this.validateHSTS();

      // Calculate overall security status
      this.calculateSecurityStatus();

      console.log(`[HTTPS Validator] Validation completed: ${this.results.isSecure ? 'SECURE' : 'INSECURE'}`);
      return this.results;

    } catch (error) {
      console.error('[HTTPS Validator] Validation failed:', error);
      this.results.tests.push({
        name: 'HTTPS Validation Error',
        status: 'FAILED',
        severity: 'HIGH',
        description: `HTTPS validation failed: ${error.message}`,
        recommendation: 'Fix HTTPS validation errors and retry'
      });
      return this.results;
    }
  }

  /**
   * Validate current protocol is HTTPS
   */
  validateCurrentProtocol() {
    const isHTTPS = window.location.protocol === 'https:';
    const isDevelopment = process.env.NODE_ENV === 'development';
    const isLocalhost = window.location.hostname === 'localhost' || 
                       window.location.hostname === '127.0.0.1' ||
                       window.location.hostname.startsWith('192.168.');

    this.results.tests.push({
      name: 'Protocol Check',
      status: (isHTTPS || (isDevelopment && isLocalhost)) ? 'PASSED' : 'FAILED',
      severity: (isDevelopment && isLocalhost) ? 'LOW' : 'HIGH',
      description: `Current protocol: ${window.location.protocol}`,
      recommendation: isHTTPS ? 'HTTPS is properly enforced' : 'Configure server to enforce HTTPS',
      details: {
        protocol: window.location.protocol,
        isDevelopment,
        isLocalhost,
        shouldEnforceHTTPS: !isDevelopment || !isLocalhost
      }
    });

    // In production, HTTPS is mandatory
    if (process.env.NODE_ENV === 'production' && !isHTTPS) {
      this.results.recommendations.push('Configure server to redirect all HTTP traffic to HTTPS');
      this.results.recommendations.push('Implement HTTP Strict Transport Security (HSTS)');
    }
  }

  /**
   * Validate secure context availability
   */
  validateSecureContext() {
    const isSecureContext = window.isSecureContext;
    
    this.results.tests.push({
      name: 'Secure Context',
      status: isSecureContext ? 'PASSED' : 'FAILED',
      severity: isSecureContext ? 'INFO' : 'MEDIUM',
      description: `Secure context available: ${isSecureContext}`,
      recommendation: isSecureContext ? 
        'Secure context is available for sensitive operations' : 
        'Secure context required for sensitive browser APIs',
      details: {
        isSecureContext,
        secureContextFeatures: this.getSecureContextFeatures()
      }
    });

    if (!isSecureContext) {
      this.results.recommendations.push('Ensure HTTPS is used to enable secure context features');
    }
  }

  /**
   * Check for mixed content issues
   */
  validateMixedContent() {
    const mixedContentIssues = [];
    
    // Check for HTTP resources in HTTPS page
    if (window.location.protocol === 'https:') {
      // Check images
      const images = document.querySelectorAll('img[src^="http:"]');
      if (images.length > 0) {
        mixedContentIssues.push(`${images.length} HTTP images found`);
      }

      // Check scripts
      const scripts = document.querySelectorAll('script[src^="http:"]');
      if (scripts.length > 0) {
        mixedContentIssues.push(`${scripts.length} HTTP scripts found`);
      }

      // Check stylesheets
      const stylesheets = document.querySelectorAll('link[href^="http:"]');
      if (stylesheets.length > 0) {
        mixedContentIssues.push(`${stylesheets.length} HTTP stylesheets found`);
      }

      // Check iframes
      const iframes = document.querySelectorAll('iframe[src^="http:"]');
      if (iframes.length > 0) {
        mixedContentIssues.push(`${iframes.length} HTTP iframes found`);
      }
    }

    this.results.tests.push({
      name: 'Mixed Content Check',
      status: mixedContentIssues.length === 0 ? 'PASSED' : 'FAILED',
      severity: mixedContentIssues.length === 0 ? 'INFO' : 'MEDIUM',
      description: mixedContentIssues.length === 0 ? 
        'No mixed content issues found' : 
        `Mixed content issues: ${mixedContentIssues.join(', ')}`,
      recommendation: mixedContentIssues.length === 0 ? 
        'No mixed content issues detected' : 
        'Update all HTTP resources to use HTTPS',
      details: {
        issues: mixedContentIssues,
        totalIssues: mixedContentIssues.length
      }
    });

    if (mixedContentIssues.length > 0) {
      this.results.recommendations.push('Update all HTTP resources to use HTTPS or relative URLs');
    }
  }

  /**
   * Validate security headers
   */
  async validateSecurityHeaders() {
    try {
      // Make a HEAD request to check response headers
      const response = await fetch(window.location.href, { method: 'HEAD' });
      const headers = {};
      
      // Convert headers to object for easier checking
      response.headers.forEach((value, key) => {
        headers[key.toLowerCase()] = value;
      });

      const securityHeaders = {
        'strict-transport-security': 'HSTS header enforces HTTPS',
        'x-content-type-options': 'Prevents MIME type sniffing',
        'x-frame-options': 'Prevents clickjacking attacks',
        'x-xss-protection': 'Enables XSS filtering',
        'content-security-policy': 'Prevents various injection attacks',
        'referrer-policy': 'Controls referrer information'
      };

      const missingHeaders = [];
      const presentHeaders = [];

      Object.entries(securityHeaders).forEach(([header, description]) => {
        if (headers[header]) {
          presentHeaders.push({ header, value: headers[header], description });
        } else {
          missingHeaders.push({ header, description });
        }
      });

      this.results.tests.push({
        name: 'Security Headers',
        status: missingHeaders.length === 0 ? 'PASSED' : 'WARNING',
        severity: missingHeaders.length > 3 ? 'MEDIUM' : 'LOW',
        description: `${presentHeaders.length}/${Object.keys(securityHeaders).length} security headers present`,
        recommendation: missingHeaders.length === 0 ? 
          'All recommended security headers are present' : 
          `Consider adding missing security headers: ${missingHeaders.map(h => h.header).join(', ')}`,
        details: {
          presentHeaders,
          missingHeaders,
          allHeaders: headers
        }
      });

      if (missingHeaders.length > 0) {
        this.results.recommendations.push('Configure server to include recommended security headers');
      }

    } catch (error) {
      this.results.tests.push({
        name: 'Security Headers',
        status: 'FAILED',
        severity: 'LOW',
        description: `Could not check security headers: ${error.message}`,
        recommendation: 'Manually verify security headers are configured on the server'
      });
    }
  }

  /**
   * Validate SSL certificate (production only)
   */
  async validateCertificate() {
    if (window.location.protocol !== 'https:') {
      return;
    }

    try {
      // Check if we can make a secure request
      const response = await fetch(window.location.origin + '/favicon.ico', {
        method: 'HEAD',
        cache: 'no-cache'
      });

      this.results.tests.push({
        name: 'SSL Certificate',
        status: response.ok ? 'PASSED' : 'WARNING',
        severity: 'MEDIUM',
        description: response.ok ? 
          'SSL certificate appears to be valid' : 
          'SSL certificate may have issues',
        recommendation: response.ok ? 
          'SSL certificate is working correctly' : 
          'Verify SSL certificate is properly configured and not expired',
        details: {
          status: response.status,
          statusText: response.statusText
        }
      });

    } catch (error) {
      this.results.tests.push({
        name: 'SSL Certificate',
        status: 'FAILED',
        severity: 'HIGH',
        description: `SSL certificate validation failed: ${error.message}`,
        recommendation: 'Check SSL certificate configuration and validity'
      });

      this.results.recommendations.push('Verify SSL certificate is properly installed and not expired');
    }
  }

  /**
   * Validate HTTP Strict Transport Security (HSTS)
   */
  async validateHSTS() {
    try {
      const response = await fetch(window.location.href, { method: 'HEAD' });
      const hstsHeader = response.headers.get('strict-transport-security');

      if (hstsHeader) {
        // Parse HSTS header
        const maxAge = hstsHeader.match(/max-age=(\d+)/);
        const includeSubDomains = hstsHeader.includes('includeSubDomains');
        const preload = hstsHeader.includes('preload');

        const maxAgeSeconds = maxAge ? parseInt(maxAge[1]) : 0;
        const maxAgeDays = Math.floor(maxAgeSeconds / (24 * 60 * 60));

        this.results.tests.push({
          name: 'HSTS Header',
          status: maxAgeSeconds > 0 ? 'PASSED' : 'WARNING',
          severity: maxAgeSeconds > 31536000 ? 'INFO' : 'LOW', // 1 year
          description: `HSTS configured with max-age: ${maxAgeDays} days`,
          recommendation: maxAgeSeconds > 31536000 ? 
            'HSTS is properly configured' : 
            'Consider increasing HSTS max-age to at least 1 year (31536000 seconds)',
          details: {
            header: hstsHeader,
            maxAgeSeconds,
            maxAgeDays,
            includeSubDomains,
            preload
          }
        });

        if (maxAgeSeconds < 31536000) {
          this.results.recommendations.push('Increase HSTS max-age to at least 1 year for better security');
        }

      } else {
        this.results.tests.push({
          name: 'HSTS Header',
          status: process.env.NODE_ENV === 'production' ? 'FAILED' : 'WARNING',
          severity: process.env.NODE_ENV === 'production' ? 'MEDIUM' : 'LOW',
          description: 'HSTS header not found',
          recommendation: 'Configure server to send HSTS header to enforce HTTPS'
        });

        if (process.env.NODE_ENV === 'production') {
          this.results.recommendations.push('Configure HSTS header to enforce HTTPS connections');
        }
      }

    } catch (error) {
      this.results.tests.push({
        name: 'HSTS Header',
        status: 'FAILED',
        severity: 'LOW',
        description: `Could not check HSTS header: ${error.message}`,
        recommendation: 'Manually verify HSTS header is configured on the server'
      });
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

    // Consider secure if:
    // 1. No critical failures, OR
    // 2. In development with localhost (relaxed rules)
    const isDevelopment = process.env.NODE_ENV === 'development';
    const isLocalhost = window.location.hostname === 'localhost' || 
                       window.location.hostname === '127.0.0.1' ||
                       window.location.hostname.startsWith('192.168.');

    this.results.isSecure = criticalFailures === 0 || (isDevelopment && isLocalhost);

    this.results.summary = {
      totalTests,
      passedTests,
      failedTests,
      criticalFailures,
      securityScore: Math.round((passedTests / totalTests) * 100),
      environment: process.env.NODE_ENV || 'development',
      isLocalhost
    };

    // Add environment-specific recommendations
    if (process.env.NODE_ENV === 'production' && !this.results.isSecure) {
      this.results.recommendations.unshift('CRITICAL: Fix HTTPS configuration before deploying to production');
    }
  }

  /**
   * Get available secure context features
   * @returns {Object} Available secure context features
   */
  getSecureContextFeatures() {
    return {
      crypto: typeof window.crypto !== 'undefined',
      serviceWorker: 'serviceWorker' in navigator,
      geolocation: 'geolocation' in navigator,
      getUserMedia: !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia),
      clipboard: !!(navigator.clipboard),
      webAuthn: !!(window.PublicKeyCredential)
    };
  }

  /**
   * Generate HTTPS configuration recommendations
   * @returns {Array} Array of configuration recommendations
   */
  generateRecommendations() {
    const recommendations = [...this.results.recommendations];

    // Add general HTTPS best practices
    if (process.env.NODE_ENV === 'production') {
      recommendations.push(
        'Ensure all API endpoints use HTTPS',
        'Configure automatic HTTP to HTTPS redirects',
        'Use HTTPS for all external resources and CDNs',
        'Regularly monitor SSL certificate expiration',
        'Consider implementing Certificate Transparency monitoring'
      );
    }

    return recommendations;
  }
}

export default HTTPSValidator;