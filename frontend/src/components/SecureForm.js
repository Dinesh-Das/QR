/**
 * SecureForm Component
 * 
 * A wrapper component that provides enhanced security features for forms including:
 * - Input validation and sanitization
 * - XSS prevention
 * - Security audit logging
 * - Real-time validation feedback
 * 
 * @component
 * @since 1.0.0
 * @author QRMFG Security Team
 */

import { Form, Input, message } from 'antd';
import PropTypes from 'prop-types';
import React, { useCallback, useEffect, useState } from 'react';

import { 
  InputSanitizer, 
  ValidationRules, 
  useInputValidation, 
  SecurityAuditLogger 
} from '../utils/inputValidation';

const { TextArea } = Input;

/**
 * SecureInput Component - Enhanced input with validation and sanitization
 */
const SecureInput = React.memo(({ 
  value, 
  onChange, 
  validationType = 'text',
  componentName = 'SecureInput',
  fieldName = 'input',
  onValidationChange,
  ...props 
}) => {
  const { validateInput } = useInputValidation();
  const [validationState, setValidationState] = useState({
    isValid: true,
    errors: [],
    wasModified: false
  });

  const handleChange = useCallback((e) => {
    const inputValue = e.target.value;
    const validation = validateInput(inputValue, validationType);
    
    setValidationState(validation);
    
    // Log potential security issues
    if (validation.wasModified) {
      SecurityAuditLogger.logXSSAttempt(
        inputValue,
        validation.sanitizedValue,
        componentName,
        fieldName
      );
      
      message.warning('Input was sanitized for security reasons');
    }
    
    // Call parent onChange with sanitized value
    if (onChange) {
      onChange({
        ...e,
        target: {
          ...e.target,
          value: validation.sanitizedValue
        }
      });
    }
    
    // Notify parent of validation state
    if (onValidationChange) {
      onValidationChange(validation);
    }
  }, [onChange, validationType, componentName, fieldName, onValidationChange, validateInput]);

  return (
    <Input
      {...props}
      value={value}
      onChange={handleChange}
      status={validationState.isValid ? '' : 'error'}
      title={validationState.errors.join(', ')}
    />
  );
});

/**
 * SecureTextArea Component - Enhanced textarea with validation and sanitization
 */
const SecureTextArea = React.memo(({ 
  value, 
  onChange, 
  validationType = 'richtext',
  componentName = 'SecureTextArea',
  fieldName = 'textarea',
  onValidationChange,
  ...props 
}) => {
  const { validateInput } = useInputValidation();
  const [validationState, setValidationState] = useState({
    isValid: true,
    errors: [],
    wasModified: false
  });

  const handleChange = useCallback((e) => {
    const inputValue = e.target.value;
    const validation = validateInput(inputValue, validationType);
    
    setValidationState(validation);
    
    // Log potential security issues
    if (validation.wasModified) {
      SecurityAuditLogger.logXSSAttempt(
        inputValue,
        validation.sanitizedValue,
        componentName,
        fieldName
      );
      
      message.warning('Input was sanitized for security reasons');
    }
    
    // Call parent onChange with sanitized value
    if (onChange) {
      onChange({
        ...e,
        target: {
          ...e.target,
          value: validation.sanitizedValue
        }
      });
    }
    
    // Notify parent of validation state
    if (onValidationChange) {
      onValidationChange(validation);
    }
  }, [onChange, validationType, componentName, fieldName, onValidationChange, validateInput]);

  return (
    <TextArea
      {...props}
      value={value}
      onChange={handleChange}
      status={validationState.isValid ? '' : 'error'}
      title={validationState.errors.join(', ')}
    />
  );
});

/**
 * SecureForm Component - Enhanced form with security features
 */
const SecureForm = React.memo(({ 
  children, 
  onFinish, 
  componentName = 'SecureForm',
  enableSecurityLogging = true,
  ...props 
}) => {
  const [securityEvents, setSecurityEvents] = useState([]);

  // Handle form submission with security validation
  const handleFinish = useCallback(async (values) => {
    try {
      // Sanitize all form values before submission
      const sanitizedValues = {};
      const securityIssues = [];

      Object.keys(values).forEach(key => {
        const originalValue = values[key];
        
        if (typeof originalValue === 'string') {
          const sanitizedValue = InputSanitizer.sanitizeText(originalValue);
          sanitizedValues[key] = sanitizedValue;
          
          if (sanitizedValue !== originalValue) {
            securityIssues.push({
              field: key,
              originalValue: originalValue.substring(0, 50),
              sanitizedValue: sanitizedValue.substring(0, 50)
            });
          }
        } else {
          sanitizedValues[key] = originalValue;
        }
      });

      // Log security issues if any
      if (securityIssues.length > 0 && enableSecurityLogging) {
        securityIssues.forEach(issue => {
          SecurityAuditLogger.logXSSAttempt(
            issue.originalValue,
            issue.sanitizedValue,
            componentName,
            issue.field
          );
        });
        
        message.warning(`${securityIssues.length} field(s) were sanitized for security`);
      }

      // Call original onFinish with sanitized values
      if (onFinish) {
        await onFinish(sanitizedValues);
      }
    } catch (error) {
      console.error('SecureForm submission error:', error);
      message.error('Form submission failed');
      throw error;
    }
  }, [onFinish, componentName, enableSecurityLogging]);

  // Track security events
  useEffect(() => {
    if (enableSecurityLogging) {
      const events = SecurityAuditLogger.getRecentEvents();
      setSecurityEvents(events.slice(-10)); // Keep last 10 events
    }
  }, [enableSecurityLogging]);

  return (
    <Form
      {...props}
      onFinish={handleFinish}
      validateTrigger={['onChange', 'onBlur']}
    >
      {children}
      
      {/* Development mode security info */}
      {process.env.NODE_ENV === 'development' && securityEvents.length > 0 && (
        <div style={{ 
          marginTop: 16, 
          padding: 8, 
          background: '#fff7e6', 
          border: '1px solid #ffd591',
          borderRadius: 4,
          fontSize: 12
        }}>
          <strong>Security Events (Dev Mode):</strong>
          <ul style={{ margin: '4px 0', paddingLeft: 16 }}>
            {securityEvents.slice(-3).map((event, index) => (
              <li key={index}>
                {event.type} in {event.component}.{event.field} at {new Date(event.timestamp).toLocaleTimeString()}
              </li>
            ))}
          </ul>
        </div>
      )}
    </Form>
  );
});

// Form.Item wrapper with enhanced validation
const SecureFormItem = React.memo(({ 
  children, 
  validationType = 'text',
  name,
  rules = [],
  ...props 
}) => {
  // Combine custom rules with security validation rules
  const securityRules = React.useMemo(() => {
    let baseRules = [];
    
    switch (validationType) {
      case 'username':
        baseRules = ValidationRules.username;
        break;
      case 'email':
        baseRules = ValidationRules.email;
        break;
      case 'password':
        baseRules = ValidationRules.password;
        break;
      case 'projectCode':
        baseRules = ValidationRules.projectCode;
        break;
      case 'materialCode':
        baseRules = ValidationRules.materialCode;
        break;
      case 'plantCode':
        baseRules = ValidationRules.plantCode;
        break;
      case 'text':
        baseRules = ValidationRules.text(false, 0, 255);
        break;
      case 'richtext':
        baseRules = ValidationRules.richText(false, 0, 1000);
        break;
      default:
        baseRules = [];
    }
    
    return [...baseRules, ...rules];
  }, [validationType, rules]);

  return (
    <Form.Item
      {...props}
      name={name}
      rules={securityRules}
    >
      {children}
    </Form.Item>
  );
});

// PropTypes
SecureInput.propTypes = {
  value: PropTypes.string,
  onChange: PropTypes.func,
  validationType: PropTypes.oneOf(['text', 'username', 'email', 'sql', 'filename', 'url']),
  componentName: PropTypes.string,
  fieldName: PropTypes.string,
  onValidationChange: PropTypes.func
};

SecureTextArea.propTypes = {
  value: PropTypes.string,
  onChange: PropTypes.func,
  validationType: PropTypes.oneOf(['text', 'richtext']),
  componentName: PropTypes.string,
  fieldName: PropTypes.string,
  onValidationChange: PropTypes.func
};

SecureForm.propTypes = {
  children: PropTypes.node.isRequired,
  onFinish: PropTypes.func,
  componentName: PropTypes.string,
  enableSecurityLogging: PropTypes.bool
};

SecureFormItem.propTypes = {
  children: PropTypes.node.isRequired,
  validationType: PropTypes.string,
  name: PropTypes.string,
  rules: PropTypes.array
};

// Display names
SecureInput.displayName = 'SecureInput';
SecureTextArea.displayName = 'SecureTextArea';
SecureForm.displayName = 'SecureForm';
SecureFormItem.displayName = 'SecureFormItem';

// Exports
export { SecureInput, SecureTextArea, SecureFormItem };
export default SecureForm;