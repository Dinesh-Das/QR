/**
 * SecureFileUpload Component
 * 
 * A secure file upload component with comprehensive validation including:
 * - File type validation
 * - File size validation
 * - Filename sanitization
 * - Malware scanning simulation
 * - Security audit logging
 * 
 * @component
 * @since 1.0.0
 * @author QRMFG Security Team
 */

import { UploadOutlined, FileTextOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { Upload, Button, message, Alert, Progress, List, Tag } from 'antd';
import PropTypes from 'prop-types';
import React, { useCallback, useState, useMemo } from 'react';

import { ValidationRules, SecurityAuditLogger, InputSanitizer } from '../utils/inputValidation';

const { Dragger } = Upload;

/**
 * SecureFileUpload Component
 */
const SecureFileUpload = React.memo(({
  fileList = [],
  onChange,
  maxFiles = 10,
  maxSizeMB = 25,
  allowedTypes = [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'text/plain'
  ],
  allowedExtensions = ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.txt'],
  componentName = 'SecureFileUpload',
  enableMalwareScan = true,
  showSecurityInfo = true,
  ...props
}) => {
  const [scanningFiles, setScanningFiles] = useState(new Set());
  const [securityAlerts, setSecurityAlerts] = useState([]);

  // File validation function
  const validateFile = useCallback((file) => {
    const validationResults = {
      isValid: true,
      errors: [],
      warnings: []
    };

    // Validate file type
    const typeValidation = ValidationRules.file.validateType(file, allowedTypes);
    if (!typeValidation.isValid) {
      validationResults.isValid = false;
      validationResults.errors.push(typeValidation.message);
    }

    // Validate file size
    const sizeValidation = ValidationRules.file.validateSize(file, maxSizeMB);
    if (!sizeValidation.isValid) {
      validationResults.isValid = false;
      validationResults.errors.push(sizeValidation.message);
    }

    // Validate filename
    const filenameValidation = ValidationRules.file.validateFilename(file.name);
    if (!filenameValidation.isValid) {
      validationResults.isValid = false;
      validationResults.errors.push(filenameValidation.message);
    }

    // Check file extension
    const fileExtension = file.name.toLowerCase().substring(file.name.lastIndexOf('.'));
    if (!allowedExtensions.includes(fileExtension)) {
      validationResults.isValid = false;
      validationResults.errors.push(`File extension ${fileExtension} is not allowed`);
    }

    // Sanitize filename
    const sanitizedFilename = InputSanitizer.sanitizeFilename(file.name);
    if (sanitizedFilename !== file.name) {
      validationResults.warnings.push('Filename was sanitized for security');
      file.name = sanitizedFilename;
    }

    // Additional security checks
    if (file.name.length > 255) {
      validationResults.isValid = false;
      validationResults.errors.push('Filename is too long');
    }

    // Check for suspicious patterns
    const suspiciousPatterns = [
      /\.exe$/i,
      /\.bat$/i,
      /\.cmd$/i,
      /\.scr$/i,
      /\.vbs$/i,
      /\.js$/i,
      /\.jar$/i,
      /\.com$/i,
      /\.pif$/i
    ];

    const hasSuspiciousPattern = suspiciousPatterns.some(pattern => pattern.test(file.name));
    if (hasSuspiciousPattern) {
      validationResults.isValid = false;
      validationResults.errors.push('File type is potentially dangerous');
    }

    return validationResults;
  }, [allowedTypes, allowedExtensions, maxSizeMB]);

  // Simulate malware scanning
  const simulateMalwareScan = useCallback(async (file) => {
    if (!enableMalwareScan) return { isClean: true };

    return new Promise((resolve) => {
      // Simulate scanning delay
      setTimeout(() => {
        // Simple heuristic checks
        const suspiciousIndicators = [
          file.size === 0, // Empty files
          file.name.includes('..'), // Path traversal
          file.name.toLowerCase().includes('virus'),
          file.name.toLowerCase().includes('malware'),
          file.name.toLowerCase().includes('trojan')
        ];

        const isClean = !suspiciousIndicators.some(indicator => indicator);

        resolve({
          isClean,
          scanTime: Math.random() * 2000 + 500, // 0.5-2.5 seconds
          threats: isClean ? [] : ['Suspicious file pattern detected']
        });
      }, Math.random() * 1000 + 500);
    });
  }, [enableMalwareScan]);

  // Handle file upload - just validate, don't actually upload here
  const handleUpload = useCallback(async (options) => {
    const { file, onSuccess, onError } = options;

    try {
      // Validate file
      const validation = validateFile(file);

      if (!validation.isValid) {
        const errorMessage = validation.errors.join(', ');

        // Log security event
        SecurityAuditLogger.logFileUploadEvent(
          file.name,
          InputSanitizer.sanitizeFilename(file.name),
          file.type,
          file.size,
          false
        );

        onError(new Error(errorMessage));
        return;
      }

      // Show warnings
      if (validation.warnings.length > 0) {
        validation.warnings.forEach(warning => message.warning(warning));
      }

      // Perform malware scan
      const scanResult = await simulateMalwareScan(file);

      if (!scanResult.isClean) {
        const threatMessage = `Security scan failed: ${scanResult.threats.join(', ')}`;

        setSecurityAlerts(prev => [...prev, {
          type: 'MALWARE_DETECTED',
          filename: file.name,
          threats: scanResult.threats,
          timestamp: new Date().toISOString()
        }]);

        onError(new Error(threatMessage));
        return;
      }

      // File is valid and clean - mark as ready for upload
      SecurityAuditLogger.logFileUploadEvent(
        file.name,
        InputSanitizer.sanitizeFilename(file.name),
        file.type,
        file.size,
        true
      );

      onSuccess(file);

    } catch (error) {
      onError(error);
    }
  }, [validateFile, simulateMalwareScan]);

  // Handle file list changes
  const handleChange = useCallback((info) => {
    let newFileList = [...info.fileList];

    // Limit number of files
    if (newFileList.length > maxFiles) {
      message.warning(`Maximum ${maxFiles} files allowed`);
      newFileList = newFileList.slice(0, maxFiles);
    }

    // Update file status and validation
    newFileList = newFileList.map(file => {
      if (file.originFileObj) {
        const validation = validateFile(file.originFileObj);

        if (!validation.isValid) {
          file.status = 'error';
          file.response = validation.errors.join(', ');
        } else if (scanningFiles.has(file.uid)) {
          file.status = 'uploading';
        }
      }

      return file;
    });

    if (onChange) {
      onChange({ fileList: newFileList });
    }
  }, [maxFiles, validateFile, scanningFiles, onChange]);

  // Before upload validation
  const beforeUpload = useCallback((file) => {
    const validation = validateFile(file);

    if (!validation.isValid) {
      message.error(`File validation failed: ${validation.errors.join(', ')}`);
      return false;
    }

    return true;
  }, [validateFile]);

  // Security info display
  const securityInfo = useMemo(() => {
    if (!showSecurityInfo) return null;

    const validFiles = fileList.filter(f => f.status === 'done').length;
    const errorFiles = fileList.filter(f => f.status === 'error').length;
    const uploadingFiles = fileList.filter(f => f.status === 'uploading').length;

    return (
      <div style={{ marginTop: 16 }}>
        <Alert
          message="File Upload Security"
          description={
            <div>
              <p><strong>Security Features:</strong></p>
              <ul style={{ marginBottom: 8 }}>
                <li>File type validation ({allowedExtensions.join(', ')})</li>
                <li>File size limit: {maxSizeMB}MB per file</li>
                <li>Filename sanitization</li>
                {enableMalwareScan && <li>Malware scanning simulation</li>}
                <li>Security audit logging</li>
              </ul>
              <p><strong>Status:</strong> {validFiles} valid, {errorFiles} errors, {uploadingFiles} uploading</p>
            </div>
          }
          type="info"
          showIcon
          icon={<SafetyCertificateOutlined />}
        />

        {securityAlerts.length > 0 && (
          <Alert
            message="Security Alerts"
            description={
              <List
                size="small"
                dataSource={securityAlerts.slice(-3)}
                renderItem={alert => (
                  <List.Item>
                    <Tag color="red">{alert.type}</Tag>
                    {alert.filename}: {alert.threats.join(', ')}
                  </List.Item>
                )}
              />
            }
            type="warning"
            showIcon
            style={{ marginTop: 8 }}
          />
        )}
      </div>
    );
  }, [showSecurityInfo, fileList, allowedExtensions, maxSizeMB, enableMalwareScan, securityAlerts]);

  return (
    <div>
      <Dragger
        {...props}
        fileList={fileList}
        onChange={handleChange}
        customRequest={handleUpload}
        beforeUpload={beforeUpload}
        multiple
        showUploadList={{
          showPreviewIcon: true,
          showRemoveIcon: true,
          showDownloadIcon: false
        }}
      >
        <p className="ant-upload-drag-icon">
          <UploadOutlined />
        </p>
        <p className="ant-upload-text">Click or drag files to this area to upload</p>
        <p className="ant-upload-hint">
          Support for {allowedExtensions.join(', ')} files up to {maxSizeMB}MB each.
          Maximum {maxFiles} files allowed.
        </p>
      </Dragger>

      {/* File list with security status */}
      {fileList.length > 0 && (
        <div style={{ marginTop: 16 }}>
          <List
            size="small"
            header={<div><FileTextOutlined /> Uploaded Files ({fileList.length})</div>}
            bordered
            dataSource={fileList}
            renderItem={file => (
              <List.Item
                actions={[
                  scanningFiles.has(file.uid) && (
                    <div key="scanning">
                      <Progress
                        type="circle"
                        size={20}
                        percent={file.percent || 0}
                        showInfo={false}
                      />
                      <span style={{ marginLeft: 8 }}>Scanning...</span>
                    </div>
                  )
                ].filter(Boolean)}
              >
                <List.Item.Meta
                  avatar={
                    <FileTextOutlined
                      style={{
                        color: file.status === 'done' ? '#52c41a' :
                          file.status === 'error' ? '#ff4d4f' : '#1890ff'
                      }}
                    />
                  }
                  title={file.name}
                  description={
                    <div>
                      <span>Size: {(file.size / 1024 / 1024).toFixed(2)} MB</span>
                      {file.status === 'error' && (
                        <Tag color="red" style={{ marginLeft: 8 }}>
                          {file.response || 'Upload failed'}
                        </Tag>
                      )}
                      {file.status === 'done' && (
                        <Tag color="green" style={{ marginLeft: 8 }}>
                          Secure
                        </Tag>
                      )}
                      {scanningFiles.has(file.uid) && (
                        <Tag color="blue" style={{ marginLeft: 8 }}>
                          Scanning
                        </Tag>
                      )}
                    </div>
                  }
                />
              </List.Item>
            )}
          />
        </div>
      )}

      {securityInfo}
    </div>
  );
});

SecureFileUpload.propTypes = {
  fileList: PropTypes.array,
  onChange: PropTypes.func,
  maxFiles: PropTypes.number,
  maxSizeMB: PropTypes.number,
  allowedTypes: PropTypes.arrayOf(PropTypes.string),
  allowedExtensions: PropTypes.arrayOf(PropTypes.string),
  componentName: PropTypes.string,
  enableMalwareScan: PropTypes.bool,
  showSecurityInfo: PropTypes.bool
};

SecureFileUpload.displayName = 'SecureFileUpload';

export default SecureFileUpload;