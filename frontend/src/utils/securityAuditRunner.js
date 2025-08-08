/**
 * Security Audit Runner
 * 
 * Comprehensive security audit and performance testing utility
 * that validates all implemented security measures and performance optimizations.
 * 
 * @module SecurityAuditRunner
 * @since 1.0.0
 * @author QRMFG Security Team
 */

import { SecurityAudit } from './securityAudit';
import { PerformanceTester } from './performanceTester';
import { HTTPSValidator } from './httpsValidator';
import { CORSValidator } from './corsValidator';
import { ErrorMessageValidator } from './errorMessageValidator';

/**
 * Main Security Audit Runner class
 */
export class SecurityAuditRunner {
  constructor() {
    this.results = {
      security: {},
      performance: {},
      https: {},
      cors: {},
      errorMessages: {},
      overall: {
        score: 0,
        status: 'PENDING',
        timestamp: new Date().toISOString(),
        recommendations: []
      }
    };
  }

  /**
   * Run comprehensive security audit and performance testing
   * @returns {Promise<Object>} Complete audit results
   */
  async runCompleteAudit() {
    console.log('🔒 Starting comprehensive security audit and performance testing...');
    
    try {
      // Run all audit components
      await this.runSecurityAudit();
      await this.runPerformanceTests();
      await this.runHTTPSValidation();
      await this.runCORSValidation();
      await this.runErrorMessageValidation();
      
      // Calculate overall score and status
      this.calculateOverallResults();
      
      // Generate final report
      const report = this.generateAuditReport();
      
      console.log('✅ Security audit and performance testing completed');
      return report;
      
    } catch (error) {
      console.error('❌ Security audit failed:', error);
      this.results.overall.status = 'FAILED';
      this.results.overall.error = error.message;
      return this.results;
    }
  }

  /**
   * Run security audit tests
   */
  async runSecurityAudit() {
    console.log('🔍 Running security audit tests...');
    
    const securityAudit = new SecurityAudit();
    this.results.security = await securityAudit.runAudit();
    
    console.log(`Security audit completed: ${this.results.security.summary.totalTests} tests, ${this.results.security.summary.passedTests} passed`);
  }

  /**
   * Run performance tests
   */
  async runPerformanceTests() {
    console.log('⚡ Running performance tests...');
    
    const performanceTester = new PerformanceTester();
    this.results.performance = await performanceTester.runTests();
    
    console.log(`Performance tests completed: Score ${this.results.performance.overallScore}/100`);
  }

  /**
   * Run HTTPS validation
   */
  async runHTTPSValidation() {
    console.log('🔐 Validating HTTPS enforcement...');
    
    const httpsValidator = new HTTPSValidator();
    this.results.https = await httpsValidator.validate();
    
    console.log(`HTTPS validation: ${this.results.https.isSecure ? 'PASSED' : 'FAILED'}`);
  }

  /**
   * Run CORS validation
   */
  async runCORSValidation() {
    console.log('🌐 Validating CORS configuration...');
    
    const corsValidator = new CORSValidator();
    this.results.cors = await corsValidator.validate();
    
    console.log(`CORS validation: ${this.results.cors.isConfigured ? 'PASSED' : 'FAILED'}`);
  }

  /**
   * Run error message security validation
   */
  async runErrorMessageValidation() {
    console.log('🚨 Validating error message security...');
    
    const errorValidator = new ErrorMessageValidator();
    this.results.errorMessages = await errorValidator.validate();
    
    console.log(`Error message validation: ${this.results.errorMessages.isSecure ? 'PASSED' : 'FAILED'}`);
  }

  /**
   * Calculate overall audit results and score
   */
  calculateOverallResults() {
    const scores = [];
    const recommendations = [];

    // Security score (40% weight)
    if (this.results.security.summary) {
      const securityScore = (this.results.security.summary.passedTests / this.results.security.summary.totalTests) * 100;
      scores.push({ score: securityScore, weight: 0.4 });
      
      if (securityScore < 90) {
        recommendations.push('Address security vulnerabilities to improve security score');
      }
    }

    // Performance score (30% weight)
    if (this.results.performance.overallScore) {
      scores.push({ score: this.results.performance.overallScore, weight: 0.3 });
      
      if (this.results.performance.overallScore < 80) {
        recommendations.push('Optimize performance to improve user experience');
      }
    }

    // HTTPS score (15% weight)
    const httpsScore = this.results.https.isSecure ? 100 : 0;
    scores.push({ score: httpsScore, weight: 0.15 });
    
    if (!this.results.https.isSecure) {
      recommendations.push('Enforce HTTPS in production environment');
    }

    // CORS score (10% weight)
    const corsScore = this.results.cors.isConfigured ? 100 : 0;
    scores.push({ score: corsScore, weight: 0.1 });
    
    if (!this.results.cors.isConfigured) {
      recommendations.push('Configure CORS properly for production');
    }

    // Error message security score (5% weight)
    const errorScore = this.results.errorMessages.isSecure ? 100 : 0;
    scores.push({ score: errorScore, weight: 0.05 });
    
    if (!this.results.errorMessages.isSecure) {
      recommendations.push('Sanitize error messages to prevent information disclosure');
    }

    // Calculate weighted average
    const totalWeightedScore = scores.reduce((sum, item) => sum + (item.score * item.weight), 0);
    const totalWeight = scores.reduce((sum, item) => sum + item.weight, 0);
    
    this.results.overall.score = Math.round(totalWeightedScore / totalWeight);
    this.results.overall.recommendations = recommendations;

    // Determine overall status
    if (this.results.overall.score >= 90) {
      this.results.overall.status = 'EXCELLENT';
    } else if (this.results.overall.score >= 80) {
      this.results.overall.status = 'GOOD';
    } else if (this.results.overall.score >= 70) {
      this.results.overall.status = 'FAIR';
    } else {
      this.results.overall.status = 'NEEDS_IMPROVEMENT';
    }
  }

  /**
   * Generate comprehensive audit report
   * @returns {Object} Formatted audit report
   */
  generateAuditReport() {
    const report = {
      ...this.results,
      metadata: {
        auditVersion: '1.0.0',
        timestamp: new Date().toISOString(),
        userAgent: navigator.userAgent,
        url: window.location.href,
        environment: process.env.NODE_ENV || 'development'
      },
      summary: {
        overallScore: this.results.overall.score,
        status: this.results.overall.status,
        criticalIssues: this.getCriticalIssues(),
        recommendations: this.results.overall.recommendations,
        nextAuditRecommended: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString() // 30 days
      }
    };

    // Log summary to console
    this.logAuditSummary(report);

    return report;
  }

  /**
   * Get critical issues from all audit results
   * @returns {Array} Array of critical issues
   */
  getCriticalIssues() {
    const criticalIssues = [];

    // Check security issues
    if (this.results.security.tests) {
      this.results.security.tests.forEach(test => {
        if (test.status === 'FAILED' && test.severity === 'HIGH') {
          criticalIssues.push({
            category: 'Security',
            issue: test.name,
            description: test.description,
            recommendation: test.recommendation
          });
        }
      });
    }

    // Check performance issues
    if (this.results.performance.metrics) {
      Object.entries(this.results.performance.metrics).forEach(([metric, data]) => {
        if (data.status === 'FAILED' && data.impact === 'HIGH') {
          criticalIssues.push({
            category: 'Performance',
            issue: metric,
            description: data.description,
            recommendation: data.recommendation
          });
        }
      });
    }

    // Check HTTPS issues
    if (!this.results.https.isSecure && process.env.NODE_ENV === 'production') {
      criticalIssues.push({
        category: 'Security',
        issue: 'HTTPS Not Enforced',
        description: 'Application is not enforcing HTTPS in production',
        recommendation: 'Configure server to redirect all HTTP traffic to HTTPS'
      });
    }

    return criticalIssues;
  }

  /**
   * Log audit summary to console
   * @param {Object} report - Complete audit report
   */
  logAuditSummary(report) {
    console.log('\n' + '='.repeat(60));
    console.log('🔒 SECURITY AUDIT & PERFORMANCE TEST SUMMARY');
    console.log('='.repeat(60));
    console.log(`Overall Score: ${report.summary.overallScore}/100 (${report.summary.status})`);
    console.log(`Timestamp: ${report.metadata.timestamp}`);
    console.log(`Environment: ${report.metadata.environment}`);
    
    if (report.summary.criticalIssues.length > 0) {
      console.log('\n❌ CRITICAL ISSUES:');
      report.summary.criticalIssues.forEach((issue, index) => {
        console.log(`${index + 1}. [${issue.category}] ${issue.issue}`);
        console.log(`   ${issue.description}`);
        console.log(`   Recommendation: ${issue.recommendation}\n`);
      });
    } else {
      console.log('\n✅ No critical issues found');
    }

    if (report.summary.recommendations.length > 0) {
      console.log('\n💡 RECOMMENDATIONS:');
      report.summary.recommendations.forEach((rec, index) => {
        console.log(`${index + 1}. ${rec}`);
      });
    }

    console.log(`\n📅 Next audit recommended: ${new Date(report.summary.nextAuditRecommended).toLocaleDateString()}`);
    console.log('='.repeat(60) + '\n');
  }

  /**
   * Save audit results to local storage for monitoring dashboard
   * @param {Object} report - Complete audit report
   */
  saveAuditResults(report) {
    try {
      const auditHistory = JSON.parse(localStorage.getItem('securityAuditHistory') || '[]');
      auditHistory.push({
        timestamp: report.metadata.timestamp,
        score: report.summary.overallScore,
        status: report.summary.status,
        criticalIssues: report.summary.criticalIssues.length,
        environment: report.metadata.environment
      });

      // Keep only last 10 audit results
      if (auditHistory.length > 10) {
        auditHistory.splice(0, auditHistory.length - 10);
      }

      localStorage.setItem('securityAuditHistory', JSON.stringify(auditHistory));
      localStorage.setItem('latestSecurityAudit', JSON.stringify(report));
      
      console.log('📊 Audit results saved to local storage');
    } catch (error) {
      console.error('Failed to save audit results:', error);
    }
  }

  /**
   * Export audit results as JSON file
   * @param {Object} report - Complete audit report
   */
  exportAuditResults(report) {
    try {
      const dataStr = JSON.stringify(report, null, 2);
      const dataBlob = new Blob([dataStr], { type: 'application/json' });
      
      const link = document.createElement('a');
      link.href = URL.createObjectURL(dataBlob);
      link.download = `security-audit-${new Date().toISOString().split('T')[0]}.json`;
      
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      
      console.log('📁 Audit results exported as JSON file');
    } catch (error) {
      console.error('Failed to export audit results:', error);
    }
  }
}

/**
 * Quick audit runner for development
 * @returns {Promise<Object>} Audit results
 */
export const runQuickSecurityAudit = async () => {
  const runner = new SecurityAuditRunner();
  const results = await runner.runCompleteAudit();
  runner.saveAuditResults(results);
  return results;
};

/**
 * Full audit runner with export
 * @returns {Promise<Object>} Audit results
 */
export const runFullSecurityAudit = async () => {
  const runner = new SecurityAuditRunner();
  const results = await runner.runCompleteAudit();
  runner.saveAuditResults(results);
  runner.exportAuditResults(results);
  return results;
};

export default SecurityAuditRunner;