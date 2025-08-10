/**
 * Security Monitoring Service
 * 
 * Comprehensive security monitoring and audit logging system for the QRMFG application.
 * Tracks authentication events, security violations, and suspicious activities.
 * 
 * @module SecurityMonitoring
 * @since 1.0.0
 * @author QRMFG Security Team
 */
import { SecurityAuditLogger } from '../utils/inputValidation';

import { trackUserActivity } from './monitoringAPI';


/**
 * Security Event Types
 */
export const SECURITY_EVENT_TYPES = {
  // Authentication Events
  LOGIN_SUCCESS: 'LOGIN_SUCCESS',
  LOGIN_FAILURE: 'LOGIN_FAILURE',
  LOGOUT: 'LOGOUT',
  TOKEN_REFRESH: 'TOKEN_REFRESH',
  TOKEN_EXPIRED: 'TOKEN_EXPIRED',
  SESSION_TIMEOUT: 'SESSION_TIMEOUT',
  
  // Authorization Events
  ACCESS_DENIED: 'ACCESS_DENIED',
  PRIVILEGE_ESCALATION_ATTEMPT: 'PRIVILEGE_ESCALATION_ATTEMPT',
  UNAUTHORIZED_API_ACCESS: 'UNAUTHORIZED_API_ACCESS',
  
  // Input Security Events
  XSS_ATTEMPT: 'XSS_ATTEMPT',
  SQL_INJECTION_ATTEMPT: 'SQL_INJECTION_ATTEMPT',
  PATH_TRAVERSAL_ATTEMPT: 'PATH_TRAVERSAL_ATTEMPT',
  MALICIOUS_FILE_UPLOAD: 'MALICIOUS_FILE_UPLOAD',
  
  // Application Security Events
  CSRF_TOKEN_MISMATCH: 'CSRF_TOKEN_MISMATCH',
  SUSPICIOUS_REQUEST_PATTERN: 'SUSPICIOUS_REQUEST_PATTERN',
  RATE_LIMIT_EXCEEDED: 'RATE_LIMIT_EXCEEDED',
  UNUSUAL_USER_BEHAVIOR: 'UNUSUAL_USER_BEHAVIOR',
  
  // System Security Events
  CONFIGURATION_CHANGE: 'CONFIGURATION_CHANGE',
  SECURITY_POLICY_VIOLATION: 'SECURITY_POLICY_VIOLATION',
  DATA_EXPORT_ATTEMPT: 'DATA_EXPORT_ATTEMPT',
  ADMIN_ACTION: 'ADMIN_ACTION'
};

/**
 * Security Severity Levels
 */
export const SECURITY_SEVERITY = {
  LOW: 'LOW',
  MEDIUM: 'MEDIUM',
  HIGH: 'HIGH',
  CRITICAL: 'CRITICAL'
};

/**
 * Security Monitoring Service
 */
export class SecurityMonitoringService {
  constructor() {
    this.eventQueue = [];
    this.isProcessing = false;
    this.maxQueueSize = 100;
    this.flushInterval = 30000; // 30 seconds
    
    // Start periodic flush
    this.startPeriodicFlush();
    
    // Track page visibility for session monitoring
    this.setupVisibilityTracking();
  }

  /**
   * Log a security event
   * @param {string} eventType - Type of security event
   * @param {Object} eventData - Event data
   * @param {string} severity - Event severity level
   */
  logSecurityEvent(eventType, eventData = {}, severity = SECURITY_SEVERITY.MEDIUM) {
    const securityEvent = {
      id: this.generateEventId(),
      type: eventType,
      severity,
      timestamp: new Date().toISOString(),
      userAgent: navigator.userAgent,
      url: window.location.href,
      referrer: document.referrer,
      sessionId: this.getSessionId(),
      userId: this.getCurrentUserId(),
      ipAddress: this.getClientIP(),
      ...eventData
    };

    // Add to queue
    this.eventQueue.push(securityEvent);
    
    // Immediate processing for critical events
    if (severity === SECURITY_SEVERITY.CRITICAL) {
      this.processCriticalEvent(securityEvent);
    }
    
    // Flush queue if it's getting full
    if (this.eventQueue.length >= this.maxQueueSize) {
      this.flushEvents();
    }
    
    // Log to console in development
    if (process.env.NODE_ENV === 'development') {
      console.warn('Security Event:', securityEvent);
    }
    
    return securityEvent.id;
  }

  /**
   * Log authentication events
   */
  logAuthenticationEvent(eventType, details = {}) {
    const authData = {
      component: 'Authentication',
      username: details.username || this.getCurrentUsername(),
      loginMethod: details.loginMethod || 'password',
      ...details
    };

    let severity = SECURITY_SEVERITY.LOW;
    if (eventType === SECURITY_EVENT_TYPES.LOGIN_FAILURE) {
      severity = SECURITY_SEVERITY.MEDIUM;
    } else if (eventType === SECURITY_EVENT_TYPES.PRIVILEGE_ESCALATION_ATTEMPT) {
      severity = SECURITY_SEVERITY.HIGH;
    }

    return this.logSecurityEvent(eventType, authData, severity);
  }

  /**
   * Log input security violations
   */
  logInputSecurityViolation(eventType, originalInput, sanitizedInput, component, field) {
    const inputData = {
      component,
      field,
      originalInput: originalInput.substring(0, 200), // Limit logged content
      sanitizedInput: sanitizedInput.substring(0, 200),
      inputLength: originalInput.length,
      violationType: this.detectViolationType(originalInput)
    };

    let severity = SECURITY_SEVERITY.MEDIUM;
    if (eventType === SECURITY_EVENT_TYPES.XSS_ATTEMPT) {
      severity = SECURITY_SEVERITY.HIGH;
    } else if (eventType === SECURITY_EVENT_TYPES.SQL_INJECTION_ATTEMPT) {
      severity = SECURITY_SEVERITY.HIGH;
    }

    return this.logSecurityEvent(eventType, inputData, severity);
  }

  /**
   * Log file upload security events
   */
  logFileUploadSecurityEvent(filename, fileType, fileSize, isValid, threats = []) {
    const fileData = {
      component: 'FileUpload',
      filename: filename.substring(0, 100),
      fileType,
      fileSize,
      isValid,
      threats,
      sanitizedFilename: filename !== filename ? 'YES' : 'NO'
    };

    const severity = threats.length > 0 ? SECURITY_SEVERITY.HIGH : SECURITY_SEVERITY.LOW;
    const eventType = threats.length > 0 ? 
      SECURITY_EVENT_TYPES.MALICIOUS_FILE_UPLOAD : 
      'FILE_UPLOAD_VALIDATED';

    return this.logSecurityEvent(eventType, fileData, severity);
  }

  /**
   * Log API security events
   */
  logApiSecurityEvent(endpoint, method, statusCode, details = {}) {
    const apiData = {
      component: 'API',
      endpoint,
      method,
      statusCode,
      ...details
    };

    let eventType = 'API_ACCESS';
    let severity = SECURITY_SEVERITY.LOW;

    if (statusCode === 401) {
      eventType = SECURITY_EVENT_TYPES.UNAUTHORIZED_API_ACCESS;
      severity = SECURITY_SEVERITY.MEDIUM;
    } else if (statusCode === 403) {
      eventType = SECURITY_EVENT_TYPES.ACCESS_DENIED;
      severity = SECURITY_SEVERITY.MEDIUM;
    } else if (statusCode === 429) {
      eventType = SECURITY_EVENT_TYPES.RATE_LIMIT_EXCEEDED;
      severity = SECURITY_SEVERITY.HIGH;
    }

    return this.logSecurityEvent(eventType, apiData, severity);
  }

  /**
   * Log user behavior anomalies
   */
  logUserBehaviorAnomaly(anomalyType, details = {}) {
    const behaviorData = {
      component: 'UserBehavior',
      anomalyType,
      sessionDuration: this.getSessionDuration(),
      actionsCount: this.getSessionActionsCount(),
      ...details
    };

    return this.logSecurityEvent(
      SECURITY_EVENT_TYPES.UNUSUAL_USER_BEHAVIOR,
      behaviorData,
      SECURITY_SEVERITY.MEDIUM
    );
  }

  /**
   * Process critical security events immediately
   */
  processCriticalEvent(event) {
    // Send immediate alert
    this.sendImmediateAlert(event);
    
    // Log to activity tracking
    trackUserActivity('security_critical', `${event.type}:${event.component || 'unknown'}`);
    
    // Store in persistent storage for offline access
    this.storeCriticalEvent(event);
  }

  /**
   * Send immediate alert for critical events
   */
  sendImmediateAlert(event) {
    // In a real implementation, this would send alerts via:
    // - Email notifications
    // - SMS alerts
    // - Push notifications to security team
    
    console.error('CRITICAL SECURITY EVENT:', event);
    
    // For now, store in localStorage for demonstration
    const alerts = JSON.parse(localStorage.getItem('criticalSecurityAlerts') || '[]');
    alerts.push({
      ...event,
      alertSent: new Date().toISOString()
    });
    
    // Keep only last 50 critical alerts
    if (alerts.length > 50) {
      alerts.splice(0, alerts.length - 50);
    }
    
    localStorage.setItem('criticalSecurityAlerts', JSON.stringify(alerts));
  }

  /**
   * Store critical events for offline access
   */
  storeCriticalEvent(event) {
    try {
      const criticalEvents = JSON.parse(localStorage.getItem('criticalSecurityEvents') || '[]');
      criticalEvents.push(event);
      
      // Keep only last 100 critical events
      if (criticalEvents.length > 100) {
        criticalEvents.splice(0, criticalEvents.length - 100);
      }
      
      localStorage.setItem('criticalSecurityEvents', JSON.stringify(criticalEvents));
    } catch (error) {
      console.error('Failed to store critical security event:', error);
    }
  }

  /**
   * Flush events to monitoring service
   */
  async flushEvents() {
    if (this.isProcessing || this.eventQueue.length === 0) {
      return;
    }

    this.isProcessing = true;
    const eventsToProcess = [...this.eventQueue];
    this.eventQueue = [];

    try {
      // Send events to monitoring service
      await this.sendEventsToMonitoring(eventsToProcess);
      
      // Also send to activity tracking for integration
      eventsToProcess.forEach(event => {
        trackUserActivity('security_event', `${event.type}:${event.severity}`);
      });
      
    } catch (error) {
      console.error('Failed to flush security events:', error);
      
      // Re-queue events on failure (with limit)
      if (this.eventQueue.length < this.maxQueueSize / 2) {
        this.eventQueue.unshift(...eventsToProcess.slice(-10)); // Keep last 10 events
      }
    } finally {
      this.isProcessing = false;
    }
  }

  /**
   * Send events to monitoring service
   */
  async sendEventsToMonitoring(events) {
    // In production, this would send to your monitoring service
    // For now, we'll store in sessionStorage and log
    
    try {
      const existingEvents = JSON.parse(sessionStorage.getItem('securityMonitoringEvents') || '[]');
      const allEvents = [...existingEvents, ...events];
      
      // Keep only last 500 events in session storage
      if (allEvents.length > 500) {
        allEvents.splice(0, allEvents.length - 500);
      }
      
      sessionStorage.setItem('securityMonitoringEvents', JSON.stringify(allEvents));
      
      // Log summary
      console.log(`Processed ${events.length} security events`);
      
    } catch (error) {
      throw new Error(`Failed to store security events: ${error.message}`);
    }
  }

  /**
   * Start periodic event flushing
   */
  startPeriodicFlush() {
    setInterval(() => {
      this.flushEvents();
    }, this.flushInterval);
  }

  /**
   * Setup page visibility tracking for session monitoring
   */
  setupVisibilityTracking() {
    document.addEventListener('visibilitychange', () => {
      if (document.hidden) {
        this.logSecurityEvent('PAGE_HIDDEN', {
          component: 'SessionMonitoring',
          duration: this.getPageVisibleDuration()
        });
      } else {
        this.logSecurityEvent('PAGE_VISIBLE', {
          component: 'SessionMonitoring'
        });
      }
    });
  }

  /**
   * Utility methods
   */
  generateEventId() {
    return `sec_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  getSessionId() {
    let sessionId = sessionStorage.getItem('securitySessionId');
    if (!sessionId) {
      sessionId = `sess_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
      sessionStorage.setItem('securitySessionId', sessionId);
    }
    return sessionId;
  }

  getCurrentUserId() {
    // Get from your auth service
    try {
      const token = localStorage.getItem('token') || sessionStorage.getItem('token');
      if (token) {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload.sub || payload.userId || 'unknown';
      }
    } catch (error) {
      // Ignore parsing errors
    }
    return 'anonymous';
  }

  getCurrentUsername() {
    try {
      const token = localStorage.getItem('token') || sessionStorage.getItem('token');
      if (token) {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload.username || payload.sub || 'unknown';
      }
    } catch (error) {
      // Ignore parsing errors
    }
    return 'anonymous';
  }

  getClientIP() {
    // In a real implementation, this would be provided by the server
    // For client-side, we can't reliably get the real IP
    return 'client-side-unknown';
  }

  getSessionDuration() {
    const sessionStart = sessionStorage.getItem('sessionStartTime');
    if (sessionStart) {
      return Date.now() - parseInt(sessionStart);
    }
    return 0;
  }

  getSessionActionsCount() {
    return parseInt(sessionStorage.getItem('sessionActionsCount') || '0');
  }

  getPageVisibleDuration() {
    const pageLoadTime = performance.timing?.loadEventEnd || Date.now();
    return Date.now() - pageLoadTime;
  }

  detectViolationType(input) {
    const violations = [];
    
    if (/<script|javascript:|on\w+\s*=/i.test(input)) {
      violations.push('XSS');
    }
    
    if (/('|"|;|--|\bUNION\b|\bSELECT\b|\bINSERT\b|\bUPDATE\b|\bDELETE\b|\bDROP\b)/i.test(input)) {
      violations.push('SQL_INJECTION');
    }
    
    if (/\.\.|\/\.\.|\\\.\./.test(input)) {
      violations.push('PATH_TRAVERSAL');
    }
    
    return violations.join(',') || 'UNKNOWN';
  }

  /**
   * Get security events for dashboard
   */
  getSecurityEvents(limit = 100) {
    try {
      const events = JSON.parse(sessionStorage.getItem('securityMonitoringEvents') || '[]');
      return events.slice(-limit).reverse(); // Most recent first
    } catch (error) {
      console.error('Failed to retrieve security events:', error);
      return [];
    }
  }

  /**
   * Get critical security alerts
   */
  getCriticalAlerts() {
    try {
      return JSON.parse(localStorage.getItem('criticalSecurityAlerts') || '[]');
    } catch (error) {
      console.error('Failed to retrieve critical alerts:', error);
      return [];
    }
  }

  /**
   * Get security statistics
   */
  getSecurityStatistics() {
    const events = this.getSecurityEvents(1000);
    const now = new Date();
    const last24Hours = new Date(now.getTime() - 24 * 60 * 60 * 1000);
    
    const recent = events.filter(e => new Date(e.timestamp) > last24Hours);
    
    const stats = {
      totalEvents: events.length,
      recentEvents: recent.length,
      criticalEvents: events.filter(e => e.severity === SECURITY_SEVERITY.CRITICAL).length,
      highSeverityEvents: events.filter(e => e.severity === SECURITY_SEVERITY.HIGH).length,
      authenticationEvents: events.filter(e => e.type.includes('LOGIN') || e.type.includes('LOGOUT')).length,
      inputViolations: events.filter(e => e.type.includes('XSS') || e.type.includes('SQL')).length,
      fileUploadEvents: events.filter(e => e.type.includes('FILE')).length,
      apiSecurityEvents: events.filter(e => e.component === 'API').length
    };
    
    return stats;
  }
}

// Create singleton instance
export const securityMonitoring = new SecurityMonitoringService();

// Enhanced activity tracking with security monitoring
export const useSecurityAwareActivityTracking = (componentName) => {
  const trackSecureAction = (action, details = {}) => {
    // Track normal activity
    trackUserActivity(action, componentName);
    
    // Log security event if needed
    if (action.includes('login') || action.includes('auth')) {
      securityMonitoring.logAuthenticationEvent(
        action.toUpperCase(),
        { component: componentName, ...details }
      );
    } else if (action.includes('error') || action.includes('failure')) {
      securityMonitoring.logSecurityEvent(
        'APPLICATION_ERROR',
        { component: componentName, action, ...details },
        SECURITY_SEVERITY.MEDIUM
      );
    }
  };

  return { trackSecureAction };
};

export default securityMonitoring;