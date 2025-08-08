/**
 * Security Audit Execution Script
 * 
 * Executes the comprehensive security audit and performance testing
 * for task 10.3 validation.
 * 
 * @module RunSecurityAudit
 * @since 1.0.0
 * @author QRMFG Security Team
 */

import { SecurityAuditRunner } from './securityAuditRunner';

/**
 * Execute comprehensive security audit and performance testing
 */
async function executeSecurityAudit() {
  console.log('🔒 QRMFG Frontend Security Audit & Performance Testing');
  console.log('=' .repeat(60));
  console.log('Starting comprehensive security audit and performance validation...\n');

  try {
    const auditRunner = new SecurityAuditRunner();
    const results = await auditRunner.runCompleteAudit();

    // Save results for monitoring dashboard
    auditRunner.saveAuditResults(results);

    // Display summary
    console.log('\n📊 AUDIT SUMMARY');
    console.log('-'.repeat(40));
    console.log(`Overall Score: ${results.summary.overallScore}/100 (${results.summary.status})`);
    console.log(`Environment: ${results.metadata.environment}`);
    console.log(`Timestamp: ${results.metadata.timestamp}`);

    // Display critical issues
    if (results.summary.criticalIssues.length > 0) {
      console.log('\n❌ CRITICAL ISSUES FOUND:');
      results.summary.criticalIssues.forEach((issue, index) => {
        console.log(`${index + 1}. [${issue.category}] ${issue.issue}`);
        console.log(`   ${issue.description}`);
        console.log(`   Recommendation: ${issue.recommendation}\n`);
      });
    } else {
      console.log('\n✅ No critical security issues found');
    }

    // Display recommendations
    if (results.summary.recommendations.length > 0) {
      console.log('\n💡 TOP RECOMMENDATIONS:');
      results.summary.recommendations.slice(0, 5).forEach((rec, index) => {
        console.log(`${index + 1}. ${rec}`);
      });
    }

    // Display component scores
    console.log('\n📈 COMPONENT SCORES:');
    console.log(`Security: ${results.security.summary?.passedTests || 0}/${results.security.summary?.totalTests || 0} tests passed`);
    console.log(`Performance: ${results.performance.overallScore || 0}/100`);
    console.log(`HTTPS: ${results.https.isSecure ? 'SECURE' : 'INSECURE'}`);
    console.log(`CORS: ${results.cors.isConfigured ? 'CONFIGURED' : 'NOT CONFIGURED'}`);
    console.log(`Error Messages: ${results.errorMessages.isSecure ? 'SECURE' : 'INSECURE'}`);

    console.log('\n' + '='.repeat(60));
    console.log('🎯 AUDIT COMPLETED SUCCESSFULLY');
    console.log('='.repeat(60));

    return results;

  } catch (error) {
    console.error('\n❌ AUDIT FAILED:', error.message);
    console.error('Stack trace:', error.stack);
    
    return {
      status: 'FAILED',
      error: error.message,
      timestamp: new Date().toISOString()
    };
  }
}

// Execute if running directly
if (typeof window !== 'undefined') {
  // Browser environment - can be called from console
  window.runSecurityAudit = executeSecurityAudit;
  console.log('Security audit available: Call window.runSecurityAudit() to execute');
}

export { executeSecurityAudit };
export default executeSecurityAudit;