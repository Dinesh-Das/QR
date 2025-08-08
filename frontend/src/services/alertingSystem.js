/**
 * Automated Alerting System
 * 
 * Comprehensive alerting system for critical issues, performance degradation,
 * and security incidents. Provides multiple notification channels and escalation paths.
 * 
 * @module AlertingSystem
 * @since 1.0.0
 * @author QRMFG Monitoring Team
 */

import { securityMonitoring, SECURITY_EVENT_TYPES, SECURITY_SEVERITY } from './securityMonitoring';

/**
 * Alert Severity Levels
 */
export const ALERT_SEVERITY = {
  CRITICAL: 'CRITICAL',
  HIGH: 'HIGH',
  MEDIUM: 'MEDIUM',
  LOW: 'LOW',
  INFO: 'INFO'
};

/**
 * Alert Types
 */
export const ALERT_TYPES = {
  SECURITY_INCIDENT: 'SECURITY_INCIDENT',
  PERFORMANCE_DEGRADATION: 'PERFORMANCE_DEGRADATION',
  SYSTEM_ERROR: 'SYSTEM_ERROR',
  HEALTH_CHECK_FAILURE: 'HEALTH_CHECK_FAILURE',
  RESOURCE_EXHAUSTION: 'RESOURCE_EXHAUSTION',
  AUTHENTICATION_FAILURE: 'AUTHENTICATION_FAILURE',
  DATA_INTEGRITY: 'DATA_INTEGRITY',
  COMPLIANCE_VIOLATION: 'COMPLIANCE_VIOLATION'
};

/**
 * Notification Channels
 */
export const NOTIFICATION_CHANNELS = {
  EMAIL: 'EMAIL',
  SLACK: 'SLACK',
  SMS: 'SMS',
  WEBHOOK: 'WEBHOOK',
  PUSH: 'PUSH',
  CONSOLE: 'CONSOLE'
};

/**
 * Alert Configuration
 */
const ALERT_CONFIG = {
  // Thresholds for automated alerting
  thresholds: {
    errorRate: {
      critical: 0.05,    // 5%
      high: 0.02,        // 2%
      medium: 0.01       // 1%
    },
    responseTime: {
      critical: 10000,   // 10 seconds
      high: 5000,        // 5 seconds
      medium: 2000       // 2 seconds
    },
    memoryUsage: {
      critical: 0.9,     // 90%
      high: 0.8,         // 80%
      medium: 0.7        // 70%
    },
    securityEvents: {
      critical: 10,      // 10 events in 5 minutes
      high: 5,           // 5 events in 5 minutes
      medium: 2          // 2 events in 5 minutes
    }
  },
  
  // Escalation rules
  escalation: {
    timeouts: {
      critical: 300000,  // 5 minutes
      high: 900000,      // 15 minutes
      medium: 1800000,   // 30 minutes
      low: 3600000       // 1 hour
    },
    retryAttempts: 3,
    backoffMultiplier: 2
  },
  
  // Channel configurations
  channels: {
    [NOTIFICATION_CHANNELS.EMAIL]: {
      enabled: true,
      recipients: {
        [ALERT_SEVERITY.CRITICAL]: ['security@qrmfg.com', 'ops@qrmfg.com'],
        [ALERT_SEVERITY.HIGH]: ['ops@qrmfg.com'],
        [ALERT_SEVERITY.MEDIUM]: ['dev@qrmfg.com'],
        [ALERT_SEVERITY.LOW]: ['dev@qrmfg.com']
      }
    },
    [NOTIFICATION_CHANNELS.SLACK]: {
      enabled: true,
      channels: {
        [ALERT_SEVERITY.CRITICAL]: '#critical-alerts',
        [ALERT_SEVERITY.HIGH]: '#alerts',
        [ALERT_SEVERITY.MEDIUM]: '#monitoring',
        [ALERT_SEVERITY.LOW]: '#monitoring'
      }
    },
    [NOTIFICATION_CHANNELS.SMS]: {
      enabled: true,
      numbers: {
        [ALERT_SEVERITY.CRITICAL]: ['+1234567890', '+0987654321'],
        [ALERT_SEVERITY.HIGH]: ['+1234567890']
      }
    }
  }
};

/**
 * Alert Class
 */
class Alert {
  constructor(type, severity, message, metadata = {}) {
    this.id = this.generateId();
    this.type = type;
    this.severity = severity;
    this.message = message;
    this.metadata = metadata;
    this.timestamp = new Date();
    this.acknowledged = false;
    this.resolved = false;
    this.escalated = false;
    this.notificationsSent = [];
  }

  generateId() {
    return `alert_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  acknowledge(userId) {
    this.acknowledged = true;
    this.acknowledgedBy = userId;
    this.acknowledgedAt = new Date();
  }

  resolve(userId, resolution) {
    this.resolved = true;
    this.resolvedBy = userId;
    this.resolvedAt = new Date();
    this.resolution = resolution;
  }

  escalate() {
    this.escalated = true;
    this.escalatedAt = new Date();
  }
}

/**
 * Alerting System Class
 */
class AlertingSystem {
  constructor() {
    this.alerts = new Map();
    this.activeAlerts = new Set();
    this.suppressedAlerts = new Set();
    this.escalationTimers = new Map();
    this.notificationQueue = [];
    this.isProcessing = false;
    
    this.initializeSystem();
  }

  initializeSystem() {
    // Start notification processor
    this.startNotificationProcessor();
    
    // Initialize security monitoring integration
    this.initializeSecurityIntegration();
    
    // Start health checks
    this.startHealthChecks();
  }

  /**
   * Create and process an alert
   */
  async createAlert(type, severity, message, metadata = {}) {
    try {
      const alert = new Alert(type, severity, message, metadata);
      
      // Check for suppression rules
      if (this.isAlertSuppressed(alert)) {
        console.log(`Alert suppressed: ${alert.id}`);
        return null;
      }

      // Store alert
      this.alerts.set(alert.id, alert);
      this.activeAlerts.add(alert.id);

      // Log security event
      await securityMonitoring.logSecurityEvent(
        SECURITY_EVENT_TYPES.ALERT_GENERATED,
        SECURITY_SEVERITY.MEDIUM,
        `Alert created: ${type} - ${severity}`,
        { alertId: alert.id, type, severity }
      );

      // Process notifications
      await this.processAlert(alert);

      // Set up escalation if needed
      this.setupEscalation(alert);

      return alert;
    } catch (error) {
      console.error('Error creating alert:', error);
      throw error;
    }
  }

  /**
   * Process alert notifications
   */
  async processAlert(alert) {
    const channels = this.getNotificationChannels(alert.severity);
    
    for (const channel of channels) {
      await this.queueNotification(alert, channel);
    }
  }

  /**
   * Get notification channels for severity level
   */
  getNotificationChannels(severity) {
    const channels = [];
    
    Object.entries(ALERT_CONFIG.channels).forEach(([channelType, config]) => {
      if (config.enabled && this.shouldNotifyChannel(channelType, severity)) {
        channels.push(channelType);
      }
    });
    
    return channels;
  }

  shouldNotifyChannel(channelType, severity) {
    const config = ALERT_CONFIG.channels[channelType];
    
    switch (channelType) {
      case NOTIFICATION_CHANNELS.EMAIL:
        return config.recipients[severity] && config.recipients[severity].length > 0;
      case NOTIFICATION_CHANNELS.SLACK:
        return config.channels[severity];
      case NOTIFICATION_CHANNELS.SMS:
        return severity === ALERT_SEVERITY.CRITICAL || severity === ALERT_SEVERITY.HIGH;
      default:
        return true;
    }
  }

  /**
   * Queue notification for processing
   */
  async queueNotification(alert, channel) {
    this.notificationQueue.push({
      alert,
      channel,
      timestamp: new Date(),
      attempts: 0
    });
  }

  /**
   * Start notification processor
   */
  startNotificationProcessor() {
    setInterval(async () => {
      if (!this.isProcessing && this.notificationQueue.length > 0) {
        this.isProcessing = true;
        await this.processNotificationQueue();
        this.isProcessing = false;
      }
    }, 1000);
  }

  /**
   * Process notification queue
   */
  async processNotificationQueue() {
    while (this.notificationQueue.length > 0) {
      const notification = this.notificationQueue.shift();
      
      try {
        await this.sendNotification(notification);
      } catch (error) {
        console.error('Notification failed:', error);
        
        // Retry logic
        if (notification.attempts < ALERT_CONFIG.escalation.retryAttempts) {
          notification.attempts++;
          setTimeout(() => {
            this.notificationQueue.push(notification);
          }, Math.pow(ALERT_CONFIG.escalation.backoffMultiplier, notification.attempts) * 1000);
        }
      }
    }
  }

  /**
   * Send notification through specific channel
   */
  async sendNotification(notification) {
    const { alert, channel } = notification;
    
    switch (channel) {
      case NOTIFICATION_CHANNELS.EMAIL:
        await this.sendEmailNotification(alert);
        break;
      case NOTIFICATION_CHANNELS.SLACK:
        await this.sendSlackNotification(alert);
        break;
      case NOTIFICATION_CHANNELS.SMS:
        await this.sendSMSNotification(alert);
        break;
      case NOTIFICATION_CHANNELS.WEBHOOK:
        await this.sendWebhookNotification(alert);
        break;
      case NOTIFICATION_CHANNELS.CONSOLE:
        this.sendConsoleNotification(alert);
        break;
      default:
        console.warn(`Unknown notification channel: ${channel}`);
    }
    
    alert.notificationsSent.push({
      channel,
      timestamp: new Date(),
      success: true
    });
  }

  /**
   * Send email notification
   */
  async sendEmailNotification(alert) {
    const config = ALERT_CONFIG.channels[NOTIFICATION_CHANNELS.EMAIL];
    const recipients = config.recipients[alert.severity] || [];
    
    if (recipients.length === 0) return;
    
    const emailContent = this.formatEmailContent(alert);
    
    // Simulate email sending (replace with actual email service)
    console.log(`📧 Email sent to ${recipients.join(', ')}`);
    console.log(`Subject: [${alert.severity}] ${alert.type} Alert`);
    console.log(`Content: ${emailContent}`);
  }

  /**
   * Send Slack notification
   */
  async sendSlackNotification(alert) {
    const config = ALERT_CONFIG.channels[NOTIFICATION_CHANNELS.SLACK];
    const channel = config.channels[alert.severity];
    
    if (!channel) return;
    
    const slackMessage = this.formatSlackMessage(alert);
    
    // Simulate Slack sending (replace with actual Slack API)
    console.log(`💬 Slack message sent to ${channel}`);
    console.log(`Message: ${slackMessage}`);
  }

  /**
   * Send SMS notification
   */
  async sendSMSNotification(alert) {
    const config = ALERT_CONFIG.channels[NOTIFICATION_CHANNELS.SMS];
    const numbers = config.numbers[alert.severity] || [];
    
    if (numbers.length === 0) return;
    
    const smsContent = this.formatSMSContent(alert);
    
    // Simulate SMS sending (replace with actual SMS service)
    console.log(`📱 SMS sent to ${numbers.join(', ')}`);
    console.log(`Content: ${smsContent}`);
  }

  /**
   * Send webhook notification
   */
  async sendWebhookNotification(alert) {
    const webhookPayload = {
      alert: {
        id: alert.id,
        type: alert.type,
        severity: alert.severity,
        message: alert.message,
        timestamp: alert.timestamp,
        metadata: alert.metadata
      }
    };
    
    // Simulate webhook sending (replace with actual HTTP request)
    console.log('🔗 Webhook notification sent');
    console.log('Payload:', JSON.stringify(webhookPayload, null, 2));
  }

  /**
   * Send console notification
   */
  sendConsoleNotification(alert) {
    const severityEmoji = {
      [ALERT_SEVERITY.CRITICAL]: '🚨',
      [ALERT_SEVERITY.HIGH]: '⚠️',
      [ALERT_SEVERITY.MEDIUM]: '⚡',
      [ALERT_SEVERITY.LOW]: 'ℹ️',
      [ALERT_SEVERITY.INFO]: '📝'
    };
    
    console.log(`${severityEmoji[alert.severity]} [${alert.severity}] ${alert.type}: ${alert.message}`);
    if (Object.keys(alert.metadata).length > 0) {
      console.log('Metadata:', alert.metadata);
    }
  }

  /**
   * Format email content
   */
  formatEmailContent(alert) {
    return `
Alert Details:
- ID: ${alert.id}
- Type: ${alert.type}
- Severity: ${alert.severity}
- Message: ${alert.message}
- Timestamp: ${alert.timestamp.toISOString()}
- Metadata: ${JSON.stringify(alert.metadata, null, 2)}

Please investigate and take appropriate action.
    `.trim();
  }

  /**
   * Format Slack message
   */
  formatSlackMessage(alert) {
    const severityColor = {
      [ALERT_SEVERITY.CRITICAL]: '#FF0000',
      [ALERT_SEVERITY.HIGH]: '#FF8C00',
      [ALERT_SEVERITY.MEDIUM]: '#FFD700',
      [ALERT_SEVERITY.LOW]: '#32CD32',
      [ALERT_SEVERITY.INFO]: '#87CEEB'
    };
    
    return `🚨 *${alert.severity}* Alert: ${alert.type}
*Message:* ${alert.message}
*Time:* ${alert.timestamp.toISOString()}
*Alert ID:* ${alert.id}`;
  }

  /**
   * Format SMS content
   */
  formatSMSContent(alert) {
    return `[${alert.severity}] ${alert.type}: ${alert.message} (${alert.id})`;
  }

  /**
   * Setup escalation timer
   */
  setupEscalation(alert) {
    if (alert.severity === ALERT_SEVERITY.INFO || alert.severity === ALERT_SEVERITY.LOW) {
      return; // No escalation for low priority alerts
    }
    
    const timeout = ALERT_CONFIG.escalation.timeouts[alert.severity.toLowerCase()];
    
    const timer = setTimeout(async () => {
      if (!alert.acknowledged && !alert.resolved) {
        await this.escalateAlert(alert);
      }
    }, timeout);
    
    this.escalationTimers.set(alert.id, timer);
  }

  /**
   * Escalate alert
   */
  async escalateAlert(alert) {
    alert.escalate();
    
    // Log escalation
    await securityMonitoring.logSecurityEvent(
      SECURITY_EVENT_TYPES.ALERT_ESCALATED,
      SECURITY_SEVERITY.HIGH,
      `Alert escalated: ${alert.id}`,
      { alertId: alert.id, originalSeverity: alert.severity }
    );
    
    // Send escalation notifications
    await this.sendEscalationNotifications(alert);
    
    console.log(`🔺 Alert escalated: ${alert.id}`);
  }

  /**
   * Send escalation notifications
   */
  async sendEscalationNotifications(alert) {
    // Send to critical channels regardless of original severity
    const criticalChannels = this.getNotificationChannels(ALERT_SEVERITY.CRITICAL);
    
    for (const channel of criticalChannels) {
      await this.queueNotification(alert, channel);
    }
  }

  /**
   * Acknowledge alert
   */
  async acknowledgeAlert(alertId, userId) {
    const alert = this.alerts.get(alertId);
    if (!alert) {
      throw new Error(`Alert not found: ${alertId}`);
    }
    
    alert.acknowledge(userId);
    
    // Clear escalation timer
    const timer = this.escalationTimers.get(alertId);
    if (timer) {
      clearTimeout(timer);
      this.escalationTimers.delete(alertId);
    }
    
    // Log acknowledgment
    await securityMonitoring.logSecurityEvent(
      SECURITY_EVENT_TYPES.ALERT_ACKNOWLEDGED,
      SECURITY_SEVERITY.LOW,
      `Alert acknowledged: ${alertId}`,
      { alertId, userId }
    );
    
    console.log(`✅ Alert acknowledged: ${alertId} by ${userId}`);
    return alert;
  }

  /**
   * Resolve alert
   */
  async resolveAlert(alertId, userId, resolution) {
    const alert = this.alerts.get(alertId);
    if (!alert) {
      throw new Error(`Alert not found: ${alertId}`);
    }
    
    alert.resolve(userId, resolution);
    this.activeAlerts.delete(alertId);
    
    // Clear escalation timer
    const timer = this.escalationTimers.get(alertId);
    if (timer) {
      clearTimeout(timer);
      this.escalationTimers.delete(alertId);
    }
    
    // Log resolution
    await securityMonitoring.logSecurityEvent(
      SECURITY_EVENT_TYPES.ALERT_RESOLVED,
      SECURITY_SEVERITY.LOW,
      `Alert resolved: ${alertId}`,
      { alertId, userId, resolution }
    );
    
    console.log(`✅ Alert resolved: ${alertId} by ${userId}`);
    return alert;
  }

  /**
   * Check if alert should be suppressed
   */
  isAlertSuppressed(alert) {
    // Check for duplicate alerts within time window
    const recentAlerts = Array.from(this.alerts.values()).filter(existingAlert => 
      existingAlert.type === alert.type &&
      existingAlert.severity === alert.severity &&
      (Date.now() - existingAlert.timestamp.getTime()) < 300000 && // 5 minutes
      !existingAlert.resolved
    );
    
    return recentAlerts.length > 0;
  }

  /**
   * Get active alerts
   */
  getActiveAlerts() {
    return Array.from(this.activeAlerts).map(id => this.alerts.get(id));
  }

  /**
   * Get alert by ID
   */
  getAlert(alertId) {
    return this.alerts.get(alertId);
  }

  /**
   * Get alerts by criteria
   */
  getAlerts(criteria = {}) {
    let alerts = Array.from(this.alerts.values());
    
    if (criteria.type) {
      alerts = alerts.filter(alert => alert.type === criteria.type);
    }
    
    if (criteria.severity) {
      alerts = alerts.filter(alert => alert.severity === criteria.severity);
    }
    
    if (criteria.resolved !== undefined) {
      alerts = alerts.filter(alert => alert.resolved === criteria.resolved);
    }
    
    if (criteria.acknowledged !== undefined) {
      alerts = alerts.filter(alert => alert.acknowledged === criteria.acknowledged);
    }
    
    if (criteria.startTime) {
      alerts = alerts.filter(alert => alert.timestamp >= criteria.startTime);
    }
    
    if (criteria.endTime) {
      alerts = alerts.filter(alert => alert.timestamp <= criteria.endTime);
    }
    
    return alerts.sort((a, b) => b.timestamp - a.timestamp);
  }

  /**
   * Initialize security monitoring integration
   */
  initializeSecurityIntegration() {
    // Listen for security events and create alerts
    securityMonitoring.on('securityEvent', async (event) => {
      if (event.severity === SECURITY_SEVERITY.CRITICAL || event.severity === SECURITY_SEVERITY.HIGH) {
        await this.createAlert(
          ALERT_TYPES.SECURITY_INCIDENT,
          event.severity === SECURITY_SEVERITY.CRITICAL ? ALERT_SEVERITY.CRITICAL : ALERT_SEVERITY.HIGH,
          `Security incident detected: ${event.type}`,
          {
            securityEvent: event,
            source: 'securityMonitoring'
          }
        );
      }
    });
  }

  /**
   * Start health checks
   */
  startHealthChecks() {
    setInterval(async () => {
      await this.performHealthChecks();
    }, 60000); // Every minute
  }

  /**
   * Perform system health checks
   */
  async performHealthChecks() {
    try {
      // Check system metrics
      const metrics = await this.getSystemMetrics();
      
      // Check error rate
      if (metrics.errorRate > ALERT_CONFIG.thresholds.errorRate.critical) {
        await this.createAlert(
          ALERT_TYPES.SYSTEM_ERROR,
          ALERT_SEVERITY.CRITICAL,
          `Critical error rate detected: ${(metrics.errorRate * 100).toFixed(2)}%`,
          { errorRate: metrics.errorRate, threshold: ALERT_CONFIG.thresholds.errorRate.critical }
        );
      } else if (metrics.errorRate > ALERT_CONFIG.thresholds.errorRate.high) {
        await this.createAlert(
          ALERT_TYPES.SYSTEM_ERROR,
          ALERT_SEVERITY.HIGH,
          `High error rate detected: ${(metrics.errorRate * 100).toFixed(2)}%`,
          { errorRate: metrics.errorRate, threshold: ALERT_CONFIG.thresholds.errorRate.high }
        );
      }
      
      // Check response time
      if (metrics.responseTime > ALERT_CONFIG.thresholds.responseTime.critical) {
        await this.createAlert(
          ALERT_TYPES.PERFORMANCE_DEGRADATION,
          ALERT_SEVERITY.CRITICAL,
          `Critical response time detected: ${metrics.responseTime}ms`,
          { responseTime: metrics.responseTime, threshold: ALERT_CONFIG.thresholds.responseTime.critical }
        );
      }
      
      // Check memory usage
      if (metrics.memoryUsage > ALERT_CONFIG.thresholds.memoryUsage.critical) {
        await this.createAlert(
          ALERT_TYPES.RESOURCE_EXHAUSTION,
          ALERT_SEVERITY.CRITICAL,
          `Critical memory usage detected: ${(metrics.memoryUsage * 100).toFixed(2)}%`,
          { memoryUsage: metrics.memoryUsage, threshold: ALERT_CONFIG.thresholds.memoryUsage.critical }
        );
      }
      
    } catch (error) {
      console.error('Health check failed:', error);
      await this.createAlert(
        ALERT_TYPES.HEALTH_CHECK_FAILURE,
        ALERT_SEVERITY.HIGH,
        'Health check system failure',
        { error: error.message }
      );
    }
  }

  /**
   * Get system metrics (mock implementation)
   */
  async getSystemMetrics() {
    // This would integrate with actual monitoring systems
    return {
      errorRate: Math.random() * 0.1, // 0-10%
      responseTime: Math.random() * 15000, // 0-15 seconds
      memoryUsage: Math.random() * 0.95 // 0-95%
    };
  }

  /**
   * Get alert statistics
   */
  getAlertStatistics(timeRange = 24 * 60 * 60 * 1000) { // Default 24 hours
    const cutoffTime = new Date(Date.now() - timeRange);
    const recentAlerts = Array.from(this.alerts.values())
      .filter(alert => alert.timestamp >= cutoffTime);
    
    const stats = {
      total: recentAlerts.length,
      active: this.activeAlerts.size,
      resolved: recentAlerts.filter(alert => alert.resolved).length,
      acknowledged: recentAlerts.filter(alert => alert.acknowledged).length,
      escalated: recentAlerts.filter(alert => alert.escalated).length,
      bySeverity: {},
      byType: {}
    };
    
    // Count by severity
    Object.values(ALERT_SEVERITY).forEach(severity => {
      stats.bySeverity[severity] = recentAlerts.filter(alert => alert.severity === severity).length;
    });
    
    // Count by type
    Object.values(ALERT_TYPES).forEach(type => {
      stats.byType[type] = recentAlerts.filter(alert => alert.type === type).length;
    });
    
    return stats;
  }
}

// Create singleton instance
export const alertingSystem = new AlertingSystem();

// Convenience functions for creating specific alert types
export const createSecurityAlert = (severity, message, metadata) => 
  alertingSystem.createAlert(ALERT_TYPES.SECURITY_INCIDENT, severity, message, metadata);

export const createPerformanceAlert = (severity, message, metadata) => 
  alertingSystem.createAlert(ALERT_TYPES.PERFORMANCE_DEGRADATION, severity, message, metadata);

export const createSystemErrorAlert = (severity, message, metadata) => 
  alertingSystem.createAlert(ALERT_TYPES.SYSTEM_ERROR, severity, message, metadata);

export const createHealthCheckAlert = (severity, message, metadata) => 
  alertingSystem.createAlert(ALERT_TYPES.HEALTH_CHECK_FAILURE, severity, message, metadata);

// Export the Alert class for external use
export { Alert };

export default alertingSystem;
     