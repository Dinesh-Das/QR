/**
 * Input Validation Tests
 * 
 * Comprehensive tests for input validation and XSS prevention utilities
 * 
 * @since 1.0.0
 * @author QRMFG Security Team
 */

import { 
  InputSanitizer, 
  ValidationRules, 
  useInputValidation, 
  SecurityAuditLogger 
} from '../inputValidation';

// Mock DOMPurify for testing
jest.mock('dompurify', () => ({
  sanitize: jest.fn((input, options) => {
    // Simple mock implementation
    if (input.includes('<script>')) return input.replace(/<script.*?<\/script>/gi, '');
    if (input.includes('javascript:')) return input.replace(/javascript:/gi, '');
    return input;
  })
}));

describe('InputSanitizer', () => {
  describe('sanitizeText', () => {
    it('should remove HTML tags', () => {
      const input = '<script>alert("xss")</script>Hello World';
      const result = InputSanitizer.sanitizeText(input);
      expect(result).toBe('alert("xss")Hello World');
    });

    it('should remove javascript protocol', () => {
      const input = 'javascript:alert("xss")';
      const result = InputSanitizer.sanitizeText(input);
      expect(result).toBe('alert("xss")');
    });

    it('should remove event handlers', () => {
      const input = 'onclick="alert(1)" Hello';
      const result = InputSanitizer.sanitizeText(input);
      expect(result).toBe('"alert(1)" Hello');
    });

    it('should handle non-string input', () => {
      expect(InputSanitizer.sanitizeText(null)).toBe('');
      expect(InputSanitizer.sanitizeText(undefined)).toBe('');
      expect(InputSanitizer.sanitizeText(123)).toBe('');
    });

    it('should trim whitespace', () => {
      const input = '  Hello World  ';
      const result = InputSanitizer.sanitizeText(input);
      expect(result).toBe('Hello World');
    });
  });

  describe('sanitizeSQL', () => {
    it('should remove SQL injection patterns', () => {
      const input = "'; DROP TABLE users; --";
      const result = InputSanitizer.sanitizeSQL(input);
      expect(result).toBe('TABLE users');
    });

    it('should remove UNION statements', () => {
      const input = 'UNION SELECT * FROM passwords';
      const result = InputSanitizer.sanitizeSQL(input);
      expect(result).toBe('* FROM passwords');
    });

    it('should remove dangerous SQL keywords', () => {
      const input = 'SELECT password FROM users WHERE id=1';
      const result = InputSanitizer.sanitizeSQL(input);
      expect(result).toBe('password FROM users WHERE id=1');
    });
  });

  describe('sanitizeFilename', () => {
    it('should remove invalid filename characters', () => {
      const input = 'file<>:"/\\|?*.txt';
      const result = InputSanitizer.sanitizeFilename(input);
      expect(result).toBe('file.txt');
    });

    it('should remove path traversal attempts', () => {
      const input = '../../../etc/passwd';
      const result = InputSanitizer.sanitizeFilename(input);
      expect(result).toBe('etcpasswd');
    });

    it('should limit filename length', () => {
      const input = 'a'.repeat(300) + '.txt';
      const result = InputSanitizer.sanitizeFilename(input);
      expect(result.length).toBeLessThanOrEqual(255);
    });

    it('should remove leading and trailing dots', () => {
      const input = '...filename...';
      const result = InputSanitizer.sanitizeFilename(input);
      expect(result).toBe('filename');
    });
  });

  describe('sanitizeURL', () => {
    it('should allow valid HTTP URLs', () => {
      const input = 'https://example.com/path';
      const result = InputSanitizer.sanitizeURL(input);
      expect(result).toBe('https://example.com/path');
    });

    it('should allow relative URLs', () => {
      const input = '/path/to/resource';
      const result = InputSanitizer.sanitizeURL(input);
      expect(result).toBe('/path/to/resource');
    });

    it('should remove javascript protocol', () => {
      const input = 'javascript:alert("xss")';
      const result = InputSanitizer.sanitizeURL(input);
      expect(result).toBe('');
    });

    it('should remove data protocol', () => {
      const input = 'data:text/html,<script>alert(1)</script>';
      const result = InputSanitizer.sanitizeURL(input);
      expect(result).toBe('');
    });

    it('should reject invalid protocols', () => {
      const input = 'ftp://example.com';
      const result = InputSanitizer.sanitizeURL(input);
      expect(result).toBe('');
    });
  });
});

describe('ValidationRules', () => {
  describe('username validation', () => {
    it('should require username', () => {
      const rules = ValidationRules.username;
      const requiredRule = rules.find(rule => rule.required);
      expect(requiredRule).toBeDefined();
      expect(requiredRule.message).toBe('Username is required');
    });

    it('should enforce minimum length', () => {
      const rules = ValidationRules.username;
      const minRule = rules.find(rule => rule.min);
      expect(minRule.min).toBe(3);
    });

    it('should enforce maximum length', () => {
      const rules = ValidationRules.username;
      const maxRule = rules.find(rule => rule.max);
      expect(maxRule.max).toBe(50);
    });

    it('should enforce pattern', () => {
      const rules = ValidationRules.username;
      const patternRule = rules.find(rule => rule.pattern);
      expect(patternRule.pattern).toEqual(/^[a-zA-Z0-9_-]+$/);
    });
  });

  describe('email validation', () => {
    it('should require email', () => {
      const rules = ValidationRules.email;
      const requiredRule = rules.find(rule => rule.required);
      expect(requiredRule).toBeDefined();
    });

    it('should validate email format', () => {
      const rules = ValidationRules.email;
      const typeRule = rules.find(rule => rule.type === 'email');
      expect(typeRule).toBeDefined();
    });
  });

  describe('password validation', () => {
    it('should require password', () => {
      const rules = ValidationRules.password;
      const requiredRule = rules.find(rule => rule.required);
      expect(requiredRule).toBeDefined();
    });

    it('should enforce minimum length', () => {
      const rules = ValidationRules.password;
      const minRule = rules.find(rule => rule.min);
      expect(minRule.min).toBe(8);
    });

    it('should enforce complexity pattern', () => {
      const rules = ValidationRules.password;
      const patternRule = rules.find(rule => rule.pattern);
      expect(patternRule).toBeDefined();
    });
  });

  describe('file validation', () => {
    const createMockFile = (name, type, size) => ({
      name,
      type,
      size
    });

    describe('validateType', () => {
      it('should accept valid file types', () => {
        const file = createMockFile('document.pdf', 'application/pdf', 1024);
        const result = ValidationRules.file.validateType(file);
        expect(result.isValid).toBe(true);
      });

      it('should reject invalid file types', () => {
        const file = createMockFile('script.exe', 'application/x-executable', 1024);
        const result = ValidationRules.file.validateType(file);
        expect(result.isValid).toBe(false);
      });

      it('should handle missing file', () => {
        const result = ValidationRules.file.validateType(null);
        expect(result.isValid).toBe(false);
        expect(result.message).toBe('No file provided');
      });
    });

    describe('validateSize', () => {
      it('should accept files within size limit', () => {
        const file = createMockFile('document.pdf', 'application/pdf', 1024 * 1024); // 1MB
        const result = ValidationRules.file.validateSize(file, 25);
        expect(result.isValid).toBe(true);
      });

      it('should reject files exceeding size limit', () => {
        const file = createMockFile('large.pdf', 'application/pdf', 30 * 1024 * 1024); // 30MB
        const result = ValidationRules.file.validateSize(file, 25);
        expect(result.isValid).toBe(false);
      });
    });

    describe('validateFilename', () => {
      it('should accept valid filenames', () => {
        const result = ValidationRules.file.validateFilename('document.pdf');
        expect(result.isValid).toBe(true);
      });

      it('should reject filenames with invalid characters', () => {
        const result = ValidationRules.file.validateFilename('file<>:"/\\|?*.txt');
        expect(result.isValid).toBe(false);
      });

      it('should handle empty filename', () => {
        const result = ValidationRules.file.validateFilename('');
        expect(result.isValid).toBe(false);
      });
    });
  });
});

describe('SecurityAuditLogger', () => {
  beforeEach(() => {
    // Clear sessionStorage before each test
    sessionStorage.clear();
  });

  describe('logXSSAttempt', () => {
    it('should log XSS attempts when input is modified', () => {
      // Clear any existing events first
      sessionStorage.removeItem('securityAuditEvents');
      
      const originalInput = '<script>alert("xss")</script>Hello';
      const sanitizedInput = 'Hello';
      
      SecurityAuditLogger.logXSSAttempt(
        originalInput,
        sanitizedInput,
        'TestComponent',
        'testField'
      );

      const events = SecurityAuditLogger.getRecentEvents();
      expect(events).toHaveLength(1);
      expect(events[0].type).toBe('XSS_ATTEMPT');
      expect(events[0].component).toBe('TestComponent');
      expect(events[0].field).toBe('testField');
    });

    it('should not log when input is not modified', () => {
      const input = 'Hello World';
      
      SecurityAuditLogger.logXSSAttempt(
        input,
        input,
        'TestComponent',
        'testField'
      );

      const events = SecurityAuditLogger.getRecentEvents();
      expect(events).toHaveLength(0);
    });
  });

  describe('logFileUploadEvent', () => {
    it('should log file upload events', () => {
      // Clear any existing events first
      sessionStorage.removeItem('securityAuditEvents');
      
      SecurityAuditLogger.logFileUploadEvent(
        'document.pdf',
        'document.pdf',
        'application/pdf',
        1024,
        true
      );

      const events = SecurityAuditLogger.getRecentEvents();
      expect(events).toHaveLength(1);
      expect(events[0].type).toBe('FILE_UPLOAD');
      expect(events[0].filename).toBe('document.pdf');
      expect(events[0].isValid).toBe(true);
    });
  });

  describe('getRecentEvents', () => {
    it('should return empty array when no events', () => {
      const events = SecurityAuditLogger.getRecentEvents();
      expect(events).toEqual([]);
    });

    it('should handle corrupted sessionStorage', () => {
      sessionStorage.setItem('securityAuditEvents', 'invalid json');
      const events = SecurityAuditLogger.getRecentEvents();
      expect(events).toEqual([]);
    });
  });
});

// Integration tests
describe('Input Validation Integration', () => {
  it('should handle complete XSS attack scenario', () => {
    const maliciousInput = '<script>document.cookie="stolen"</script><img src="x" onerror="alert(1)">Hello';
    
    // Sanitize the input
    const sanitized = InputSanitizer.sanitizeText(maliciousInput);
    
    // Verify dangerous content is removed
    expect(sanitized).not.toContain('<script>');
    expect(sanitized).not.toContain('onerror');
    expect(sanitized).toContain('Hello');
    
    // Verify logging would occur
    expect(sanitized).not.toBe(maliciousInput);
  });

  it('should handle SQL injection attempt', () => {
    const maliciousInput = "admin'; DROP TABLE users; --";
    
    // Sanitize the input
    const sanitized = InputSanitizer.sanitizeSQL(maliciousInput);
    
    // Verify SQL injection patterns are removed
    expect(sanitized).not.toContain("';");
    expect(sanitized).not.toContain('--');
    expect(sanitized).not.toContain('DROP');
  });

  it('should handle file upload security', () => {
    const maliciousFile = {
      name: '../../../etc/passwd<script>alert(1)</script>.exe',
      type: 'application/x-executable',
      size: 50 * 1024 * 1024 // 50MB
    };

    // Validate file type
    const typeValidation = ValidationRules.file.validateType(maliciousFile);
    expect(typeValidation.isValid).toBe(false);

    // Validate file size
    const sizeValidation = ValidationRules.file.validateSize(maliciousFile, 25);
    expect(sizeValidation.isValid).toBe(false);

    // Sanitize filename
    const sanitizedFilename = InputSanitizer.sanitizeFilename(maliciousFile.name);
    expect(sanitizedFilename).not.toContain('../');
    expect(sanitizedFilename).not.toContain('<script>');
    // Note: .exe extension is removed by sanitizeFilename but the text remains
    expect(sanitizedFilename).toContain('exe'); // The text 'exe' remains but not as extension
  });
});