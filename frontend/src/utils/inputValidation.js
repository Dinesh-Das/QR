/**
 * Input Validation and XSS Prevention Utilities
 * 
 * This module provides comprehensive input validation and sanitization
 * to prevent XSS attacks and ensure data integrity.
 * 
 * @module InputValidation
 * @since 1.0.0
 * @author QRMFG Security Team
 */

import DOMPurify from 'dompurify';

/**
 * XSS Prevention and Input Sanitization
 */
export class InputSanitizer {
  /**
   * Sanitize HTML content to prevent XSS attacks
   * @param {string} input - Raw HTML input
   * @param {Object} options - DOMPurify options
   * @returns {string} Sanitized HTML
   */
  static sanitizeHTML(input, options = {}) {
    if (typeof input !== 'string') return '';
    
    const defaultOptions = {
      ALLOWED_TAGS: ['b', 'i', 'em', 'strong', 'p', 'br'],
      ALLOWED_ATTR: [],
      KEEP_CONTENT: true,
      ...options
    };
    
    return DOMPurify.sanitize(input, defaultOptions);
  }

  /**
   * Sanitize plain text input by removing HTML tags and dangerous characters
   * @param {string} input - Raw text input
   * @returns {string} Sanitized text
   */
  static sanitizeText(input) {
    if (typeof input !== 'string') return '';
    
    return input
      .replace(/<[^>]*>/g, '') // Remove HTML tags
      .replace(/javascript:/gi, '') // Remove javascript: protocol
      .replace(/on\w+\s*=/gi, '') // Remove event handlers
      .replace(/&lt;script&gt;/gi, '') // Remove encoded script tags
      .replace(/&lt;\/script&gt;/gi, '')
      .trim();
  }

  /**
   * Sanitize SQL input to prevent SQL injection
   * @param {string} input - Raw SQL input
   * @returns {string} Sanitized input
   */
  static sanitizeSQL(input) {
    if (typeof input !== 'string') return '';
    
    return input
      .replace(/['";\\]/g, '') // Remove dangerous SQL characters
      .replace(/--/g, '') // Remove SQL comments
      .replace(/\/\*/g, '') // Remove SQL block comments
      .replace(/\*\//g, '')
      .replace(/\bUNION\b/gi, '') // Remove UNION statements
      .replace(/\bSELECT\b/gi, '') // Remove SELECT statements
      .replace(/\bINSERT\b/gi, '') // Remove INSERT statements
      .replace(/\bUPDATE\b/gi, '') // Remove UPDATE statements
      .replace(/\bDELETE\b/gi, '') // Remove DELETE statements
      .replace(/\bDROP\b/gi, '') // Remove DROP statements
      .trim();
  }

  /**
   * Sanitize file names to prevent path traversal attacks
   * @param {string} filename - Original filename
   * @returns {string} Sanitized filename
   */
  static sanitizeFilename(filename) {
    if (typeof filename !== 'string') return '';
    
    return filename
      .replace(/[<>:"/\\|?*]/g, '') // Remove invalid filename characters
      .replace(/\.\./g, '') // Remove path traversal attempts
      .replace(/^\.+/, '') // Remove leading dots
      .replace(/\.+$/, '') // Remove trailing dots
      .substring(0, 255) // Limit filename length
      .trim();
  }

  /**
   * Sanitize URL input to prevent malicious redirects
   * @param {string} url - Raw URL input
   * @returns {string} Sanitized URL
   */
  static sanitizeURL(url) {
    if (typeof url !== 'string') return '';
    
    // Only allow http, https, and relative URLs
    const allowedProtocols = /^(https?:\/\/|\/)/i;
    
    if (!allowedProtocols.test(url)) {
      return '';
    }
    
    return url
      .replace(/javascript:/gi, '') // Remove javascript protocol
      .replace(/data:/gi, '') // Remove data protocol
      .replace(/vbscript:/gi, '') // Remove vbscript protocol
      .trim();
  }
}

/**
 * Form Input Validation Rules
 */
export class ValidationRules {
  /**
   * Username validation rules
   */
  static username = [
    { required: true, message: 'Username is required' },
    { min: 3, message: 'Username must be at least 3 characters' },
    { max: 50, message: 'Username must not exceed 50 characters' },
    { 
      pattern: /^[a-zA-Z0-9_-]+$/, 
      message: 'Username can only contain letters, numbers, underscores, and hyphens' 
    },
    {
      validator: (_, value) => {
        if (!value) return Promise.resolve();
        const sanitized = InputSanitizer.sanitizeText(value);
        if (sanitized !== value) {
          return Promise.reject(new Error('Username contains invalid characters'));
        }
        return Promise.resolve();
      }
    }
  ];

  /**
   * Email validation rules
   */
  static email = [
    { required: true, message: 'Email is required' },
    { type: 'email', message: 'Please enter a valid email address' },
    { max: 100, message: 'Email must not exceed 100 characters' },
    {
      validator: (_, value) => {
        if (!value) return Promise.resolve();
        const sanitized = InputSanitizer.sanitizeText(value);
        if (sanitized !== value) {
          return Promise.reject(new Error('Email contains invalid characters'));
        }
        return Promise.resolve();
      }
    }
  ];

  /**
   * Password validation rules
   */
  static password = [
    { required: true, message: 'Password is required' },
    { min: 8, message: 'Password must be at least 8 characters' },
    { max: 128, message: 'Password must not exceed 128 characters' },
    {
      pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]/,
      message: 'Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character'
    }
  ];

  /**
   * Text input validation rules
   */
  static text = (required = false, minLength = 0, maxLength = 255) => [
    ...(required ? [{ required: true, message: 'This field is required' }] : []),
    ...(minLength > 0 ? [{ min: minLength, message: `Must be at least ${minLength} characters` }] : []),
    { max: maxLength, message: `Must not exceed ${maxLength} characters` },
    {
      validator: (_, value) => {
        if (!value) return Promise.resolve();
        const sanitized = InputSanitizer.sanitizeText(value);
        if (sanitized !== value) {
          return Promise.reject(new Error('Input contains invalid characters'));
        }
        return Promise.resolve();
      }
    }
  ];

  /**
   * Rich text validation rules (for text areas with limited HTML)
   */
  static richText = (required = false, minLength = 0, maxLength = 1000) => [
    ...(required ? [{ required: true, message: 'This field is required' }] : []),
    ...(minLength > 0 ? [{ min: minLength, message: `Must be at least ${minLength} characters` }] : []),
    { max: maxLength, message: `Must not exceed ${maxLength} characters` },
    {
      validator: (_, value) => {
        if (!value) return Promise.resolve();
        const sanitized = InputSanitizer.sanitizeHTML(value);
        // Check if sanitization removed dangerous content
        const textOnly = value.replace(/<[^>]*>/g, '');
        const sanitizedTextOnly = sanitized.replace(/<[^>]*>/g, '');
        if (textOnly !== sanitizedTextOnly) {
          return Promise.reject(new Error('Input contains potentially dangerous content'));
        }
        return Promise.resolve();
      }
    }
  ];

  /**
   * File validation rules
   */
  static file = {
    /**
     * Validate file type
     * @param {File} file - File object
     * @param {string[]} allowedTypes - Array of allowed MIME types
     * @returns {Object} Validation result
     */
    validateType: (file, allowedTypes = []) => {
      if (!file) return { isValid: false, message: 'No file provided' };
      
      const allowedExtensions = ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.txt'];
      const allowedMimeTypes = [
        'application/pdf',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'application/vnd.ms-excel',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'text/plain'
      ];

      const fileExtension = file.name.toLowerCase().substring(file.name.lastIndexOf('.'));
      const mimeType = file.type;

      const typesToCheck = allowedTypes.length > 0 ? allowedTypes : allowedMimeTypes;
      
      if (!allowedExtensions.includes(fileExtension) || !typesToCheck.includes(mimeType)) {
        return {
          isValid: false,
          message: 'Invalid file type. Only PDF, Word, Excel, and text files are allowed.'
        };
      }

      return { isValid: true, message: 'File type is valid' };
    },

    /**
     * Validate file size
     * @param {File} file - File object
     * @param {number} maxSizeMB - Maximum file size in MB
     * @returns {Object} Validation result
     */
    validateSize: (file, maxSizeMB = 25) => {
      if (!file) return { isValid: false, message: 'No file provided' };
      
      const maxSizeBytes = maxSizeMB * 1024 * 1024;
      
      if (file.size > maxSizeBytes) {
        return {
          isValid: false,
          message: `File size exceeds ${maxSizeMB}MB limit (${(file.size / 1024 / 1024).toFixed(2)}MB)`
        };
      }

      return { isValid: true, message: 'File size is valid' };
    },

    /**
     * Validate filename for security
     * @param {string} filename - Original filename
     * @returns {Object} Validation result
     */
    validateFilename: (filename) => {
      if (!filename) return { isValid: false, message: 'No filename provided' };
      
      const sanitized = InputSanitizer.sanitizeFilename(filename);
      
      if (sanitized !== filename) {
        return {
          isValid: false,
          message: 'Filename contains invalid characters'
        };
      }

      if (sanitized.length === 0) {
        return {
          isValid: false,
          message: 'Filename is empty after sanitization'
        };
      }

      return { isValid: true, message: 'Filename is valid' };
    }
  };

  /**
   * Project/Material code validation
   */
  static projectCode = [
    { required: true, message: 'Project code is required' },
    { 
      pattern: /^[A-Z0-9-_]+$/i, 
      message: 'Project code can only contain letters, numbers, hyphens, and underscores' 
    },
    { min: 2, message: 'Project code must be at least 2 characters' },
    { max: 20, message: 'Project code must not exceed 20 characters' }
  ];

  /**
   * Material code validation
   */
  static materialCode = [
    { required: true, message: 'Material code is required' },
    { 
      pattern: /^[A-Z0-9-_]+$/i, 
      message: 'Material code can only contain letters, numbers, hyphens, and underscores' 
    },
    { min: 2, message: 'Material code must be at least 2 characters' },
    { max: 30, message: 'Material code must not exceed 30 characters' }
  ];

  /**
   * Plant code validation
   */
  static plantCode = [
    { required: true, message: 'Plant code is required' },
    { 
      pattern: /^[A-Z0-9-_]+$/i, 
      message: 'Plant code can only contain letters, numbers, hyphens, and underscores' 
    },
    { min: 2, message: 'Plant code must be at least 2 characters' },
    { max: 10, message: 'Plant code must not exceed 10 characters' }
  ];
}

/**
 * Real-time input validation hook
 */
export const useInputValidation = () => {
  /**
   * Validate and sanitize input in real-time
   * @param {string} value - Input value
   * @param {string} type - Validation type
   * @returns {Object} Validation result with sanitized value
   */
  const validateInput = (value, type = 'text') => {
    if (!value) return { isValid: true, sanitizedValue: '', errors: [] };

    let sanitizedValue = value;
    const errors = [];

    switch (type) {
      case 'username':
        sanitizedValue = InputSanitizer.sanitizeText(value);
        if (!/^[a-zA-Z0-9_-]+$/.test(sanitizedValue)) {
          errors.push('Username can only contain letters, numbers, underscores, and hyphens');
        }
        break;

      case 'email':
        sanitizedValue = InputSanitizer.sanitizeText(value);
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(sanitizedValue)) {
          errors.push('Please enter a valid email address');
        }
        break;

      case 'text':
        sanitizedValue = InputSanitizer.sanitizeText(value);
        break;

      case 'richtext':
        sanitizedValue = InputSanitizer.sanitizeHTML(value);
        break;

      case 'sql':
        sanitizedValue = InputSanitizer.sanitizeSQL(value);
        break;

      case 'filename':
        sanitizedValue = InputSanitizer.sanitizeFilename(value);
        break;

      case 'url':
        sanitizedValue = InputSanitizer.sanitizeURL(value);
        break;

      default:
        sanitizedValue = InputSanitizer.sanitizeText(value);
    }

    return {
      isValid: errors.length === 0,
      sanitizedValue,
      errors,
      wasModified: sanitizedValue !== value
    };
  };

  return { validateInput };
};

/**
 * Security audit logging for input validation
 */
export class SecurityAuditLogger {
  /**
   * Log potential XSS attempt
   * @param {string} originalInput - Original input
   * @param {string} sanitizedInput - Sanitized input
   * @param {string} component - Component name
   * @param {string} field - Field name
   */
  static logXSSAttempt(originalInput, sanitizedInput, component, field) {
    if (originalInput !== sanitizedInput) {
      const auditEvent = {
        type: 'XSS_ATTEMPT',
        timestamp: new Date().toISOString(),
        component,
        field,
        originalInput: originalInput.substring(0, 100), // Limit logged content
        sanitizedInput: sanitizedInput.substring(0, 100),
        userAgent: navigator.userAgent,
        url: window.location.href,
        severity: 'HIGH'
      };

      // Log to console in development
      if (process.env.NODE_ENV === 'development') {
        console.warn('Potential XSS attempt detected:', auditEvent);
      }

      // Send to monitoring service in production
      if (process.env.NODE_ENV === 'production') {
        this.sendToMonitoring(auditEvent);
      }
    }
  }

  /**
   * Log file upload security event
   * @param {string} filename - Original filename
   * @param {string} sanitizedFilename - Sanitized filename
   * @param {string} fileType - File MIME type
   * @param {number} fileSize - File size in bytes
   * @param {boolean} isValid - Whether file passed validation
   */
  static logFileUploadEvent(filename, sanitizedFilename, fileType, fileSize, isValid) {
    const auditEvent = {
      type: 'FILE_UPLOAD',
      timestamp: new Date().toISOString(),
      filename: filename.substring(0, 100),
      sanitizedFilename: sanitizedFilename.substring(0, 100),
      fileType,
      fileSize,
      isValid,
      userAgent: navigator.userAgent,
      url: window.location.href,
      severity: isValid ? 'INFO' : 'MEDIUM'
    };

    // Log to console in development
    if (process.env.NODE_ENV === 'development') {
      console.log('File upload event:', auditEvent);
    }

    // Send to monitoring service
    if (process.env.NODE_ENV === 'production') {
      this.sendToMonitoring(auditEvent);
    }
  }

  /**
   * Send audit event to monitoring service
   * @param {Object} auditEvent - Audit event data
   */
  static async sendToMonitoring(auditEvent) {
    try {
      // This would integrate with your monitoring service
      // For now, we'll store in sessionStorage for demonstration
      const existingEvents = JSON.parse(sessionStorage.getItem('securityAuditEvents') || '[]');
      existingEvents.push(auditEvent);
      
      // Keep only last 100 events
      if (existingEvents.length > 100) {
        existingEvents.splice(0, existingEvents.length - 100);
      }
      
      sessionStorage.setItem('securityAuditEvents', JSON.stringify(existingEvents));
    } catch (error) {
      console.error('Failed to log security audit event:', error);
    }
  }

  /**
   * Get recent security audit events
   * @returns {Array} Array of recent audit events
   */
  static getRecentEvents() {
    try {
      return JSON.parse(sessionStorage.getItem('securityAuditEvents') || '[]');
    } catch (error) {
      console.error('Failed to retrieve security audit events:', error);
      return [];
    }
  }
}

export default {
  InputSanitizer,
  ValidationRules,
  useInputValidation,
  SecurityAuditLogger
};