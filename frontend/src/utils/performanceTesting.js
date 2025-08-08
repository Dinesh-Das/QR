/**
 * Performance Testing Utility
 * 
 * Comprehensive performance testing and optimization validation for the QRMFG frontend.
 * Tests bundle size, load times, memory usage, and runtime performance.
 * 
 * @module PerformanceTesting
 * @since 1.0.0
 * @author QRMFG Performance Team
 */

import { securityMonitoring, SECURITY_EVENT_TYPES, SECURITY_SEVERITY } from '../services/securityMonitoring';

/**
 * Performance Test Results
 */
export class PerformanceTestResults {
  constructor() {
    this.tests = [];
    this.metrics = {};
    this.summary = {
      total: 0,
      passed: 0,
      failed: 0,
      warnings: 0,
      overallScore: 0
    };
    this.startTime = null;
    this.endTime = null;
  }

  addTest(test) {
    this.tests.push({
      id: `perf_test_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      timestamp: new Date().toISOString(),
      ...test
    });
    
    this.summary.total++;
    
    if (test.status === 'PASSED') {
      this.summary.passed++;
    } else if (test.status === 'FAILED') {
      this.summary.failed++;
    } else if (test.status === 'WARNING') {
      this.summary.warnings++;
    }
  }

  addMetric(name, value, unit = '', threshold = null) {
    this.metrics[name] = {
      value,
      unit,
      threshold,
      status: threshold ? (value <= threshold ? 'GOOD' : 'POOR') : 'INFO',
      timestamp: new Date().toISOString()
    };
  }

  calculateOverallScore() {
    if (this.summary.total === 0) return 0;
    
    const baseScore = (this.summary.passed / this.summary.total) * 100;
    const warningPenalty = this.summary.warnings * 5;
    const failurePenalty = this.summary.failed * 15;
    
    this.summary.overallScore = Math.max(0, Math.round(baseScore - warningPenalty - failurePenalty));
    return this.summary.overallScore;
  }

  getRecommendations() {
    return this.tests
      .filter(test => (test.status === 'FAILED' || test.status === 'WARNING') && test.recommendation)
      .sort((a, b) => {
        const priorityOrder = { 'FAILED': 2, 'WARNING': 1 };
        return priorityOrder[b.status] - priorityOrder[a.status];
      })
      .slice(0, 10)
      .map(test => ({
        priority: test.status === 'FAILED' ? 'HIGH' : 'MEDIUM',
        title: test.name,
        recommendation: test.recommendation,
        impact: test.impact || 'Performance improvement'
      }));
  }
}

/**
 * Performance Testing Runner
 */
export class PerformanceTestRunner {
  constructor() {
    this.results = new PerformanceTestResults();
    this.observer = null;
  }

  /**
   * Run comprehensive performance tests
   */
  async runPerformanceTests() {
    console.log('[Performance Testing] Starting comprehensive performance tests...');
    
    this.results = new PerformanceTestResults();
    this.results.startTime = new Date().toISOString();

    try {
      // Collect baseline metrics
      await this.collectBaselineMetrics();
      
      // Run performance tests
      await this.testPageLoadPerformance();
      await this.testBundleSize();
      await this.testMemoryUsage();
      await this.testRuntimePerformance();
      await this.testNetworkPerformance();
      await this.testRenderingPerformance();
      await this.testInteractionPerformance();
      await this.testCacheEfficiency();
      await this.testLazyLoadingEffectiveness();
      await this.testComponentPerformance();

      this.results.endTime = new Date().toISOString();
      this.results.calculateOverallScore();
      
      // Log performance test completion
      securityMonitoring.logSecurityEvent(
        'PERFORMANCE_TEST_COMPLETED',
        {
          totalTests: this.results.summary.total,
          passed: this.results.summary.passed,
          failed: this.results.summary.failed,
          overallScore: this.results.summary.overallScore,
          duration: new Date(this.results.endTime) - new Date(this.results.startTime)
        },
        this.results.summary.failed > 0 ? SECURITY_SEVERITY.MEDIUM : SECURITY_SEVERITY.LOW
      );

      return this.results;

    } catch (error) {
      console.error('[Performance Testing] Tests failed:', error);
      
      securityMonitoring.logSecurityEvent(
        'PERFORMANCE_TEST_FAILED',
        { error: error.message },
        SECURITY_SEVERITY.MEDIUM
      );
      
      throw error;
    }
  }

  /**
   * Collect baseline performance metrics
   */
  async collectBaselineMetrics() {
    console.log('[Performance Testing] Collecting baseline metrics...');

    // Navigation timing metrics
    const navigation = performance.getEntriesByType('navigation')[0];
    if (navigation) {
      this.results.addMetric('DNS Lookup', Math.round(navigation.domainLookupEnd - navigation.domainLookupStart), 'ms', 100);
      this.results.addMetric('TCP Connection', Math.round(navigation.connectEnd - navigation.connectStart), 'ms', 100);
      this.results.addMetric('Server Response', Math.round(navigation.responseEnd - navigation.requestStart), 'ms', 500);
      this.results.addMetric('DOM Content Loaded', Math.round(navigation.domContentLoadedEventEnd - navigation.navigationStart), 'ms', 2000);
      this.results.addMetric('Page Load Complete', Math.round(navigation.loadEventEnd - navigation.navigationStart), 'ms', 3000);
    }

    // Memory metrics
    if (performance.memory) {
      this.results.addMetric('Used JS Heap Size', Math.round(performance.memory.usedJSHeapSize / 1024 / 1024), 'MB', 50);
      this.results.addMetric('Total JS Heap Size', Math.round(performance.memory.totalJSHeapSize / 1024 / 1024), 'MB', 100);
      this.results.addMetric('JS Heap Size Limit', Math.round(performance.memory.jsHeapSizeLimit / 1024 / 1024), 'MB');
    }

    // Resource timing metrics
    const resources = performance.getEntriesByType('resource');
    const totalResourceSize = resources.reduce((total, resource) => total + (resource.transferSize || 0), 0);
    this.results.addMetric('Total Resource Size', Math.round(totalResourceSize / 1024), 'KB', 2000);
    this.results.addMetric('Resource Count', resources.length, 'resources', 100);

    // First Paint and First Contentful Paint
    const paintEntries = performance.getEntriesByType('paint');
    paintEntries.forEach(entry => {
      if (entry.name === 'first-paint') {
        this.results.addMetric('First Paint', Math.round(entry.startTime), 'ms', 1000);
      } else if (entry.name === 'first-contentful-paint') {
        this.results.addMetric('First Contentful Paint', Math.round(entry.startTime), 'ms', 1500);
      }
    });
  }

  /**
   * Test page load performance
   */
  async testPageLoadPerformance() {
    console.log('[Performance Testing] Testing page load performance...');

    const navigation = performance.getEntriesByType('navigation')[0];
    if (!navigation) {
      this.results.addTest({
        name: 'Page Load Performance',
        category: 'Load Performance',
        status: 'WARNING',
        description: 'Navigation timing data not available',
        recommendation: 'Ensure performance API is supported'
      });
      return;
    }

    // Test DNS lookup time
    const dnsTime = navigation.domainLookupEnd - navigation.domainLookupStart;
    if (dnsTime > 100) {
      this.results.addTest({
        name: 'DNS Lookup Performance',
        category: 'Load Performance',
        status: 'WARNING',
        description: `DNS lookup took ${Math.round(dnsTime)}ms`,
        evidence: { dnsTime: Math.round(dnsTime) },
        recommendation: 'Consider using DNS prefetching or a faster DNS provider',
        impact: 'Faster initial connection'
      });
    } else {
      this.results.addTest({
        name: 'DNS Lookup Performance',
        category: 'Load Performance',
        status: 'PASSED',
        description: `DNS lookup completed in ${Math.round(dnsTime)}ms`
      });
    }

    // Test server response time
    const serverResponseTime = navigation.responseEnd - navigation.requestStart;
    if (serverResponseTime > 500) {
      this.results.addTest({
        name: 'Server Response Time',
        category: 'Load Performance',
        status: 'FAILED',
        description: `Server response took ${Math.round(serverResponseTime)}ms`,
        evidence: { responseTime: Math.round(serverResponseTime) },
        recommendation: 'Optimize server response time to under 500ms',
        impact: 'Significantly faster page loads'
      });
    } else if (serverResponseTime > 200) {
      this.results.addTest({
        name: 'Server Response Time',
        category: 'Load Performance',
        status: 'WARNING',
        description: `Server response took ${Math.round(serverResponseTime)}ms`,
        evidence: { responseTime: Math.round(serverResponseTime) },
        recommendation: 'Consider optimizing server response time',
        impact: 'Faster page loads'
      });
    } else {
      this.results.addTest({
        name: 'Server Response Time',
        category: 'Load Performance',
        status: 'PASSED',
        description: `Server responded in ${Math.round(serverResponseTime)}ms`
      });
    }

    // Test DOM content loaded time
    const domContentLoadedTime = navigation.domContentLoadedEventEnd - navigation.navigationStart;
    if (domContentLoadedTime > 2000) {
      this.results.addTest({
        name: 'DOM Content Loaded Time',
        category: 'Load Performance',
        status: 'FAILED',
        description: `DOM content loaded in ${Math.round(domContentLoadedTime)}ms`,
        evidence: { domTime: Math.round(domContentLoadedTime) },
        recommendation: 'Optimize critical rendering path and reduce blocking resources',
        impact: 'Faster time to interactive'
      });
    } else if (domContentLoadedTime > 1000) {
      this.results.addTest({
        name: 'DOM Content Loaded Time',
        category: 'Load Performance',
        status: 'WARNING',
        description: `DOM content loaded in ${Math.round(domContentLoadedTime)}ms`,
        recommendation: 'Consider optimizing critical resources'
      });
    } else {
      this.results.addTest({
        name: 'DOM Content Loaded Time',
        category: 'Load Performance',
        status: 'PASSED',
        description: `DOM content loaded in ${Math.round(domContentLoadedTime)}ms`
      });
    }

    // Test total page load time
    const totalLoadTime = navigation.loadEventEnd - navigation.navigationStart;
    if (totalLoadTime > 3000) {
      this.results.addTest({
        name: 'Total Page Load Time',
        category: 'Load Performance',
        status: 'FAILED',
        description: `Page loaded in ${Math.round(totalLoadTime)}ms`,
        evidence: { loadTime: Math.round(totalLoadTime) },
        recommendation: 'Optimize bundle size, implement lazy loading, and optimize images',
        impact: 'Much better user experience'
      });
    } else if (totalLoadTime > 2000) {
      this.results.addTest({
        name: 'Total Page Load Time',
        category: 'Load Performance',
        status: 'WARNING',
        description: `Page loaded in ${Math.round(totalLoadTime)}ms`,
        recommendation: 'Consider further optimizations for sub-2s load time'
      });
    } else {
      this.results.addTest({
        name: 'Total Page Load Time',
        category: 'Load Performance',
        status: 'PASSED',
        description: `Page loaded in ${Math.round(totalLoadTime)}ms`
      });
    }
  }

  /**
   * Test bundle size
   */
  async testBundleSize() {
    console.log('[Performance Testing] Testing bundle size...');

    const resources = performance.getEntriesByType('resource');
    
    // Analyze JavaScript bundles
    const jsResources = resources.filter(r => r.name.includes('.js'));
    const totalJSSize = jsResources.reduce((total, resource) => total + (resource.transferSize || 0), 0);
    
    if (totalJSSize > 1024 * 1024) { // 1MB
      this.results.addTest({
        name: 'JavaScript Bundle Size',
        category: 'Bundle Optimization',
        status: 'FAILED',
        description: `Total JS size is ${Math.round(totalJSSize / 1024)}KB`,
        evidence: { jsSize: Math.round(totalJSSize / 1024), bundleCount: jsResources.length },
        recommendation: 'Implement code splitting and tree shaking to reduce bundle size',
        impact: 'Faster downloads and parsing'
      });
    } else if (totalJSSize > 512 * 1024) { // 512KB
      this.results.addTest({
        name: 'JavaScript Bundle Size',
        category: 'Bundle Optimization',
        status: 'WARNING',
        description: `Total JS size is ${Math.round(totalJSSize / 1024)}KB`,
        recommendation: 'Consider further bundle optimization'
      });
    } else {
      this.results.addTest({
        name: 'JavaScript Bundle Size',
        category: 'Bundle Optimization',
        status: 'PASSED',
        description: `Total JS size is ${Math.round(totalJSSize / 1024)}KB`
      });
    }

    // Analyze CSS bundles
    const cssResources = resources.filter(r => r.name.includes('.css'));
    const totalCSSSize = cssResources.reduce((total, resource) => total + (resource.transferSize || 0), 0);
    
    if (totalCSSSize > 200 * 1024) { // 200KB
      this.results.addTest({
        name: 'CSS Bundle Size',
        category: 'Bundle Optimization',
        status: 'WARNING',
        description: `Total CSS size is ${Math.round(totalCSSSize / 1024)}KB`,
        evidence: { cssSize: Math.round(totalCSSSize / 1024) },
        recommendation: 'Consider CSS optimization and unused CSS removal',
        impact: 'Faster rendering'
      });
    } else {
      this.results.addTest({
        name: 'CSS Bundle Size',
        category: 'Bundle Optimization',
        status: 'PASSED',
        description: `Total CSS size is ${Math.round(totalCSSSize / 1024)}KB`
      });
    }

    // Check for duplicate resources
    const resourceNames = resources.map(r => r.name.split('?')[0]); // Remove query params
    const duplicates = resourceNames.filter((name, index) => resourceNames.indexOf(name) !== index);
    
    if (duplicates.length > 0) {
      this.results.addTest({
        name: 'Duplicate Resources',
        category: 'Bundle Optimization',
        status: 'WARNING',
        description: `${duplicates.length} duplicate resources detected`,
        evidence: { duplicates: [...new Set(duplicates)] },
        recommendation: 'Remove duplicate resource loading',
        impact: 'Reduced bandwidth usage'
      });
    } else {
      this.results.addTest({
        name: 'Duplicate Resources',
        category: 'Bundle Optimization',
        status: 'PASSED',
        description: 'No duplicate resources detected'
      });
    }
  }

  /**
   * Test memory usage
   */
  async testMemoryUsage() {
    console.log('[Performance Testing] Testing memory usage...');

    if (!performance.memory) {
      this.results.addTest({
        name: 'Memory Usage',
        category: 'Memory Performance',
        status: 'WARNING',
        description: 'Memory API not available',
        recommendation: 'Test in Chrome for memory metrics'
      });
      return;
    }

    const usedMemory = performance.memory.usedJSHeapSize;
    const totalMemory = performance.memory.totalJSHeapSize;
    const memoryLimit = performance.memory.jsHeapSizeLimit;
    
    const usedMemoryMB = Math.round(usedMemory / 1024 / 1024);
    const memoryUsagePercent = Math.round((usedMemory / memoryLimit) * 100);

    if (usedMemoryMB > 50) {
      this.results.addTest({
        name: 'Memory Usage',
        category: 'Memory Performance',
        status: 'FAILED',
        description: `Using ${usedMemoryMB}MB of memory (${memoryUsagePercent}%)`,
        evidence: { usedMemoryMB, memoryUsagePercent },
        recommendation: 'Investigate memory leaks and optimize memory usage',
        impact: 'Better performance on low-memory devices'
      });
    } else if (usedMemoryMB > 25) {
      this.results.addTest({
        name: 'Memory Usage',
        category: 'Memory Performance',
        status: 'WARNING',
        description: `Using ${usedMemoryMB}MB of memory (${memoryUsagePercent}%)`,
        recommendation: 'Monitor memory usage and consider optimizations'
      });
    } else {
      this.results.addTest({
        name: 'Memory Usage',
        category: 'Memory Performance',
        status: 'PASSED',
        description: `Using ${usedMemoryMB}MB of memory (${memoryUsagePercent}%)`
      });
    }

    // Test for potential memory leaks by monitoring over time
    setTimeout(() => {
      const newUsedMemory = performance.memory.usedJSHeapSize;
      const memoryIncrease = newUsedMemory - usedMemory;
      const memoryIncreaseMB = Math.round(memoryIncrease / 1024 / 1024);

      if (memoryIncreaseMB > 5) {
        this.results.addTest({
          name: 'Memory Leak Detection',
          category: 'Memory Performance',
          status: 'WARNING',
          description: `Memory increased by ${memoryIncreaseMB}MB in 5 seconds`,
          evidence: { memoryIncreaseMB },
          recommendation: 'Investigate potential memory leaks',
          impact: 'Prevent browser crashes and slowdowns'
        });
      } else {
        this.results.addTest({
          name: 'Memory Leak Detection',
          category: 'Memory Performance',
          status: 'PASSED',
          description: 'No significant memory increase detected'
        });
      }
    }, 5000);
  }

  /**
   * Test runtime performance
   */
  async testRuntimePerformance() {
    console.log('[Performance Testing] Testing runtime performance...');

    // Test JavaScript execution time
    const startTime = performance.now();
    
    // Simulate some work
    for (let i = 0; i < 100000; i++) {
      Math.random();
    }
    
    const executionTime = performance.now() - startTime;
    
    if (executionTime > 100) {
      this.results.addTest({
        name: 'JavaScript Execution Performance',
        category: 'Runtime Performance',
        status: 'WARNING',
        description: `JavaScript execution is slow (${Math.round(executionTime)}ms for test)`,
        evidence: { executionTime: Math.round(executionTime) },
        recommendation: 'Optimize JavaScript code and consider web workers for heavy tasks'
      });
    } else {
      this.results.addTest({
        name: 'JavaScript Execution Performance',
        category: 'Runtime Performance',
        status: 'PASSED',
        description: `JavaScript execution is fast (${Math.round(executionTime)}ms for test)`
      });
    }

    // Test DOM manipulation performance
    const domStartTime = performance.now();
    
    const testDiv = document.createElement('div');
    for (let i = 0; i < 1000; i++) {
      const span = document.createElement('span');
      span.textContent = `Item ${i}`;
      testDiv.appendChild(span);
    }
    document.body.appendChild(testDiv);
    
    const domTime = performance.now() - domStartTime;
    document.body.removeChild(testDiv);
    
    if (domTime > 50) {
      this.results.addTest({
        name: 'DOM Manipulation Performance',
        category: 'Runtime Performance',
        status: 'WARNING',
        description: `DOM manipulation is slow (${Math.round(domTime)}ms for 1000 elements)`,
        evidence: { domTime: Math.round(domTime) },
        recommendation: 'Optimize DOM operations and consider virtual scrolling'
      });
    } else {
      this.results.addTest({
        name: 'DOM Manipulation Performance',
        category: 'Runtime Performance',
        status: 'PASSED',
        description: `DOM manipulation is fast (${Math.round(domTime)}ms for 1000 elements)`
      });
    }
  }

  /**
   * Test network performance
   */
  async testNetworkPerformance() {
    console.log('[Performance Testing] Testing network performance...');

    const resources = performance.getEntriesByType('resource');
    
    // Test for HTTP/2 usage
    const http2Resources = resources.filter(r => r.nextHopProtocol && r.nextHopProtocol.includes('h2'));
    const http2Percentage = (http2Resources.length / resources.length) * 100;
    
    if (http2Percentage < 50) {
      this.results.addTest({
        name: 'HTTP/2 Usage',
        category: 'Network Performance',
        status: 'WARNING',
        description: `Only ${Math.round(http2Percentage)}% of resources use HTTP/2`,
        evidence: { http2Percentage: Math.round(http2Percentage) },
        recommendation: 'Configure server to use HTTP/2 for better performance'
      });
    } else {
      this.results.addTest({
        name: 'HTTP/2 Usage',
        category: 'Network Performance',
        status: 'PASSED',
        description: `${Math.round(http2Percentage)}% of resources use HTTP/2`
      });
    }

    // Test for compression
    const compressedResources = resources.filter(r => 
      r.transferSize && r.decodedBodySize && r.transferSize < r.decodedBodySize
    );
    const compressionRatio = compressedResources.length / resources.length;
    
    if (compressionRatio < 0.7) {
      this.results.addTest({
        name: 'Resource Compression',
        category: 'Network Performance',
        status: 'WARNING',
        description: `Only ${Math.round(compressionRatio * 100)}% of resources are compressed`,
        evidence: { compressionRatio: Math.round(compressionRatio * 100) },
        recommendation: 'Enable gzip/brotli compression for all text resources'
      });
    } else {
      this.results.addTest({
        name: 'Resource Compression',
        category: 'Network Performance',
        status: 'PASSED',
        description: `${Math.round(compressionRatio * 100)}% of resources are compressed`
      });
    }

    // Test for CDN usage
    const cdnResources = resources.filter(r => 
      r.name.includes('cdn.') || 
      r.name.includes('cloudfront.') || 
      r.name.includes('cloudflare.')
    );
    
    if (cdnResources.length > 0) {
      this.results.addTest({
        name: 'CDN Usage',
        category: 'Network Performance',
        status: 'PASSED',
        description: `${cdnResources.length} resources served from CDN`
      });
    } else {
      this.results.addTest({
        name: 'CDN Usage',
        category: 'Network Performance',
        status: 'WARNING',
        description: 'No CDN usage detected',
        recommendation: 'Consider using a CDN for static assets'
      });
    }
  }

  /**
   * Test rendering performance
   */
  async testRenderingPerformance() {
    console.log('[Performance Testing] Testing rendering performance...');

    // Test for layout thrashing
    const startTime = performance.now();
    
    // Force multiple reflows
    const testElement = document.createElement('div');
    testElement.style.position = 'absolute';
    testElement.style.left = '0px';
    document.body.appendChild(testElement);
    
    for (let i = 0; i < 100; i++) {
      testElement.style.left = i + 'px';
      testElement.offsetLeft; // Force reflow
    }
    
    const layoutTime = performance.now() - startTime;
    document.body.removeChild(testElement);
    
    if (layoutTime > 50) {
      this.results.addTest({
        name: 'Layout Performance',
        category: 'Rendering Performance',
        status: 'WARNING',
        description: `Layout operations are slow (${Math.round(layoutTime)}ms for 100 operations)`,
        evidence: { layoutTime: Math.round(layoutTime) },
        recommendation: 'Optimize CSS and avoid forced synchronous layouts'
      });
    } else {
      this.results.addTest({
        name: 'Layout Performance',
        category: 'Rendering Performance',
        status: 'PASSED',
        description: `Layout operations are fast (${Math.round(layoutTime)}ms for 100 operations)`
      });
    }

    // Test for paint performance
    const paintEntries = performance.getEntriesByType('paint');
    const firstPaint = paintEntries.find(entry => entry.name === 'first-paint');
    const firstContentfulPaint = paintEntries.find(entry => entry.name === 'first-contentful-paint');
    
    if (firstContentfulPaint && firstContentfulPaint.startTime > 1500) {
      this.results.addTest({
        name: 'First Contentful Paint',
        category: 'Rendering Performance',
        status: 'FAILED',
        description: `First Contentful Paint at ${Math.round(firstContentfulPaint.startTime)}ms`,
        evidence: { fcp: Math.round(firstContentfulPaint.startTime) },
        recommendation: 'Optimize critical rendering path and reduce render-blocking resources',
        impact: 'Users see content faster'
      });
    } else if (firstContentfulPaint && firstContentfulPaint.startTime > 1000) {
      this.results.addTest({
        name: 'First Contentful Paint',
        category: 'Rendering Performance',
        status: 'WARNING',
        description: `First Contentful Paint at ${Math.round(firstContentfulPaint.startTime)}ms`,
        recommendation: 'Consider optimizing critical rendering path'
      });
    } else if (firstContentfulPaint) {
      this.results.addTest({
        name: 'First Contentful Paint',
        category: 'Rendering Performance',
        status: 'PASSED',
        description: `First Contentful Paint at ${Math.round(firstContentfulPaint.startTime)}ms`
      });
    }
  }

  /**
   * Test interaction performance
   */
  async testInteractionPerformance() {
    console.log('[Performance Testing] Testing interaction performance...');

    // Test click response time
    const testButton = document.createElement('button');
    testButton.textContent = 'Test Button';
    testButton.style.position = 'absolute';
    testButton.style.left = '-9999px';
    document.body.appendChild(testButton);

    const clickStartTime = performance.now();
    
    testButton.addEventListener('click', () => {
      const clickResponseTime = performance.now() - clickStartTime;
      
      if (clickResponseTime > 100) {
        this.results.addTest({
          name: 'Click Response Time',
          category: 'Interaction Performance',
          status: 'WARNING',
          description: `Click response took ${Math.round(clickResponseTime)}ms`,
          evidence: { clickResponseTime: Math.round(clickResponseTime) },
          recommendation: 'Optimize event handlers and reduce main thread blocking'
        });
      } else {
        this.results.addTest({
          name: 'Click Response Time',
          category: 'Interaction Performance',
          status: 'PASSED',
          description: `Click response in ${Math.round(clickResponseTime)}ms`
        });
      }
      
      document.body.removeChild(testButton);
    });

    testButton.click();

    // Test scroll performance
    let frameCount = 0;
    let scrollStartTime = performance.now();
    
    const scrollHandler = () => {
      frameCount++;
      if (frameCount === 60) { // Test for 60 frames
        const scrollTime = performance.now() - scrollStartTime;
        const fps = Math.round(60000 / scrollTime);
        
        if (fps < 30) {
          this.results.addTest({
            name: 'Scroll Performance',
            category: 'Interaction Performance',
            status: 'FAILED',
            description: `Scroll performance is poor (${fps} FPS)`,
            evidence: { fps },
            recommendation: 'Optimize scroll handlers and use passive event listeners',
            impact: 'Smoother scrolling experience'
          });
        } else if (fps < 50) {
          this.results.addTest({
            name: 'Scroll Performance',
            category: 'Interaction Performance',
            status: 'WARNING',
            description: `Scroll performance could be better (${fps} FPS)`,
            recommendation: 'Consider scroll optimizations'
          });
        } else {
          this.results.addTest({
            name: 'Scroll Performance',
            category: 'Interaction Performance',
            status: 'PASSED',
            description: `Scroll performance is good (${fps} FPS)`
          });
        }
        
        window.removeEventListener('scroll', scrollHandler);
      }
    };

    window.addEventListener('scroll', scrollHandler, { passive: true });
    
    // Simulate scroll events
    for (let i = 0; i < 60; i++) {
      setTimeout(() => {
        window.scrollBy(0, 1);
      }, i * 16); // 60 FPS
    }
  }

  /**
   * Test cache efficiency
   */
  async testCacheEfficiency() {
    console.log('[Performance Testing] Testing cache efficiency...');

    const resources = performance.getEntriesByType('resource');
    
    // Test for cached resources
    const cachedResources = resources.filter(r => r.transferSize === 0 && r.decodedBodySize > 0);
    const cacheHitRate = (cachedResources.length / resources.length) * 100;
    
    if (cacheHitRate < 30) {
      this.results.addTest({
        name: 'Cache Efficiency',
        category: 'Caching Performance',
        status: 'WARNING',
        description: `Low cache hit rate (${Math.round(cacheHitRate)}%)`,
        evidence: { cacheHitRate: Math.round(cacheHitRate) },
        recommendation: 'Implement better caching strategies and set appropriate cache headers'
      });
    } else {
      this.results.addTest({
        name: 'Cache Efficiency',
        category: 'Caching Performance',
        status: 'PASSED',
        description: `Good cache hit rate (${Math.round(cacheHitRate)}%)`
      });
    }

    // Test for long-term caching
    const longTermCachedResources = resources.filter(r => 
      r.name.includes('.js') || r.name.includes('.css') || r.name.includes('.png') || r.name.includes('.jpg')
    );
    
    if (longTermCachedResources.length > 0) {
      this.results.addTest({
        name: 'Static Asset Caching',
        category: 'Caching Performance',
        status: 'PASSED',
        description: `${longTermCachedResources.length} static assets detected for caching`
      });
    } else {
      this.results.addTest({
        name: 'Static Asset Caching',
        category: 'Caching Performance',
        status: 'WARNING',
        description: 'No static assets detected for long-term caching',
        recommendation: 'Implement cache-busting for static assets'
      });
    }
  }

  /**
   * Test lazy loading effectiveness
   */
  async testLazyLoadingEffectiveness() {
    console.log('[Performance Testing] Testing lazy loading effectiveness...');

    // Test for lazy-loaded images
    const images = document.querySelectorAll('img');
    const lazyImages = document.querySelectorAll('img[loading="lazy"]');
    const lazyImagePercentage = (lazyImages.length / images.length) * 100;
    
    if (images.length > 10 && lazyImagePercentage < 50) {
      this.results.addTest({
        name: 'Image Lazy Loading',
        category: 'Lazy Loading',
        status: 'WARNING',
        description: `Only ${Math.round(lazyImagePercentage)}% of images use lazy loading`,
        evidence: { lazyImagePercentage: Math.round(lazyImagePercentage), totalImages: images.length },
        recommendation: 'Implement lazy loading for images below the fold'
      });
    } else if (images.length > 0) {
      this.results.addTest({
        name: 'Image Lazy Loading',
        category: 'Lazy Loading',
        status: 'PASSED',
        description: `${Math.round(lazyImagePercentage)}% of images use lazy loading`
      });
    }

    // Test for code splitting (check for multiple JS bundles)
    const jsResources = performance.getEntriesByType('resource').filter(r => r.name.includes('.js'));
    
    if (jsResources.length > 1) {
      this.results.addTest({
        name: 'Code Splitting',
        category: 'Lazy Loading',
        status: 'PASSED',
        description: `${jsResources.length} JavaScript bundles detected (code splitting active)`
      });
    } else {
      this.results.addTest({
        name: 'Code Splitting',
        category: 'Lazy Loading',
        status: 'WARNING',
        description: 'Single JavaScript bundle detected',
        recommendation: 'Implement code splitting for better performance'
      });
    }
  }

  /**
   * Test component performance
   */
  async testComponentPerformance() {
    console.log('[Performance Testing] Testing component performance...');

    // Test for React DevTools performance data (if available)
    if (window.__REACT_DEVTOOLS_GLOBAL_HOOK__) {
      this.results.addTest({
        name: 'React DevTools Integration',
        category: 'Component Performance',
        status: 'PASSED',
        description: 'React DevTools available for performance profiling'
      });
    } else {
      this.results.addTest({
        name: 'React DevTools Integration',
        category: 'Component Performance',
        status: 'WARNING',
        description: 'React DevTools not available',
        recommendation: 'Install React DevTools for component performance analysis'
      });
    }

    // Test for excessive DOM nodes
    const domNodeCount = document.querySelectorAll('*').length;
    
    if (domNodeCount > 1500) {
      this.results.addTest({
        name: 'DOM Node Count',
        category: 'Component Performance',
        status: 'WARNING',
        description: `High DOM node count (${domNodeCount} nodes)`,
        evidence: { domNodeCount },
        recommendation: 'Consider virtual scrolling or component optimization to reduce DOM nodes'
      });
    } else {
      this.results.addTest({
        name: 'DOM Node Count',
        category: 'Component Performance',
        status: 'PASSED',
        description: `Reasonable DOM node count (${domNodeCount} nodes)`
      });
    }

    // Test for deeply nested components
    const maxDepth = this.calculateMaxDOMDepth();
    
    if (maxDepth > 15) {
      this.results.addTest({
        name: 'DOM Depth',
        category: 'Component Performance',
        status: 'WARNING',
        description: `Deep DOM nesting detected (${maxDepth} levels)`,
        evidence: { maxDepth },
        recommendation: 'Flatten component structure to improve rendering performance'
      });
    } else {
      this.results.addTest({
        name: 'DOM Depth',
        category: 'Component Performance',
        status: 'PASSED',
        description: `Reasonable DOM depth (${maxDepth} levels)`
      });
    }
  }

  /**
   * Helper method to calculate maximum DOM depth
   */
  calculateMaxDOMDepth() {
    let maxDepth = 0;
    
    const calculateDepth = (element, currentDepth = 0) => {
      maxDepth = Math.max(maxDepth, currentDepth);
      
      for (const child of element.children) {
        calculateDepth(child, currentDepth + 1);
      }
    };
    
    calculateDepth(document.body);
    return maxDepth;
  }
}

// Export singleton instance
export const performanceTesting = new PerformanceTestRunner();

export default performanceTesting;