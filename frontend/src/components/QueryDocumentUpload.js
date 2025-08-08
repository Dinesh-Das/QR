import {
    UploadOutlined,
    InboxOutlined,
    CheckCircleOutlined,
    DeleteOutlined,
    ExclamationCircleOutlined
} from '@ant-design/icons';
import {
    Upload,
    Button,
    Card,
    Space,
    Typography,
    Alert,
    message,
    List,
    Progress
} from 'antd';
import React, { useState } from 'react';

import { FILE_SIZE } from '../constants';
import { queryAPI } from '../services/queryAPI';


const { Text } = Typography;
const { Dragger } = Upload;

/**
 * QueryDocumentUpload Component
 * 
 * Provides file upload functionality for query documents with:
 * - Drag and drop interface
 * - File validation with user-friendly error messages
 * - Progress indicators and upload status feedback
 * - Support for both query creation and response attachment contexts
 * 
 * @param {Object} props
 * @param {string} props.queryId - Query ID (required for response context)
 * @param {string} props.responseId - Response ID (optional, for response attachments)
 * @param {Function} props.onUploadComplete - Callback when upload completes
 * @param {number} props.maxFiles - Maximum number of files allowed (default: 5)
 * @param {boolean} props.disabled - Whether upload is disabled
 * @param {string} props.context - Upload context: 'query' or 'response'
 */
const QueryDocumentUpload = ({
    queryId,
    responseId,
    onUploadComplete,
    maxFiles = 5,
    disabled = false,
    context = 'query'
}) => {
    const [fileList, setFileList] = useState([]);
    const [uploading, setUploading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(0);

    // File validation function
    const validateFile = (file) => {
        const validTypes = [
            'application/pdf',
            'application/msword',
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            'application/vnd.ms-excel',
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
            'image/jpeg',
            'image/jpg',
            'image/png'
        ];

        const validExtensions = ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.jpg', '.jpeg', '.png'];
        const maxSize = FILE_SIZE.MAX_UPLOAD_SIZE;

        const fileExtension = file.name.toLowerCase().substring(file.name.lastIndexOf('.'));

        return {
            isValidType: validTypes.includes(file.type) || validExtensions.includes(fileExtension),
            isValidSize: file.size <= maxSize,
            type: file.type,
            size: file.size,
            extension: fileExtension
        };
    };

    // Handle file selection and validation
    const handleFileChange = ({ fileList: newFileList }) => {
        // Limit number of files
        if (newFileList.length > maxFiles) {
            message.warning(`Maximum ${maxFiles} files allowed. Only the first ${maxFiles} files will be processed.`);
            newFileList = newFileList.slice(0, maxFiles);
        }

        const validatedFileList = newFileList.map(file => {
            if (file.originFileObj) {
                const validation = validateFile(file.originFileObj);

                if (!validation.isValidType) {
                    file.status = 'error';
                    file.response = 'Invalid file type. Only PDF, Word, Excel, and image files (JPG, PNG) are allowed.';
                } else if (!validation.isValidSize) {
                    file.status = 'error';
                    file.response = `File size exceeds 25MB limit (${(file.originFileObj.size / 1024 / 1024).toFixed(2)}MB).`;
                } else {
                    file.status = 'done';
                    file.percent = 100;
                }

                file.size = file.originFileObj.size;
                file.type = file.originFileObj.type;
                file.lastModified = file.originFileObj.lastModified;
            }
            return file;
        });

        setFileList(validatedFileList);

        // Show validation feedback
        const validFiles = validatedFileList.filter(f => f.status === 'done').length;
        const errorFiles = validatedFileList.filter(f => f.status === 'error').length;

        if (errorFiles > 0) {
            message.warning(
                `${errorFiles} file(s) have validation errors. Please check file types and sizes.`
            );
        } else if (validFiles > 0) {
            message.success(`${validFiles} file(s) ready for upload.`);
        }
    };

    // Handle file upload
    const handleUpload = async () => {
        const validFiles = fileList.filter(file => file.status === 'done');

        if (validFiles.length === 0) {
            message.error('Please select at least one valid file to upload.');
            return;
        }

        if (!queryId) {
            message.error('Query ID is required for document upload.');
            return;
        }

        try {
            setUploading(true);
            setUploadProgress(0);

            const files = validFiles.map(file => file.originFileObj || file);

            // Simulate progress for better UX
            const progressInterval = setInterval(() => {
                setUploadProgress(prev => {
                    if (prev >= 90) {
                        clearInterval(progressInterval);
                        return 90;
                    }
                    return prev + 10;
                });
            }, 200);

            // Call appropriate API based on context
            let result;
            if (context === 'response' && responseId) {
                result = await queryAPI.uploadResponseDocuments(queryId, responseId, files);
            } else {
                result = await queryAPI.uploadQueryDocuments(queryId, files);
            }

            clearInterval(progressInterval);
            setUploadProgress(100);

            message.success(
                `Successfully uploaded ${validFiles.length} document(s) to the ${context}.`
            );

            // Clear the file list
            setFileList([]);
            setUploadProgress(0);

            // Notify parent component
            if (onUploadComplete) {
                onUploadComplete(result);
            }
        } catch (error) {
            console.error('Error uploading documents:', error);
            message.error(`Failed to upload documents to ${context}. Please try again.`);
            setUploadProgress(0);
        } finally {
            setUploading(false);
        }
    };

    // Remove file from list
    const removeFile = (file) => {
        const newFileList = fileList.filter(item => item.uid !== file.uid);
        setFileList(newFileList);
    };

    // Format file size for display
    const formatFileSize = (bytes) => {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
    };

    const contextLabel = context === 'response' ? 'Response' : 'Query';
    const validFileCount = fileList.filter(f => f.status === 'done').length;

    return (
        <Card
            size="small"
            title={
                <Space>
                    <UploadOutlined />
                    <span>Upload Documents to {contextLabel}</span>
                </Space>
            }
            style={{ backgroundColor: '#fafafa', border: '1px dashed #d9d9d9' }}
        >
            <Space direction="vertical" style={{ width: '100%' }}>
                {/* Upload Guidelines */}
                <Alert
                    message={`Document Upload for ${contextLabel}`}
                    description={
                        <ul style={{ margin: 0, paddingLeft: 20 }}>
                            <li>Supported formats: PDF, Word (.doc, .docx), Excel (.xls, .xlsx), Images (JPG, PNG)</li>
                            <li>Maximum file size: 25MB per file</li>
                            <li>Maximum {maxFiles} files allowed</li>
                            <li>Files will be attached to this {context.toLowerCase()}</li>
                        </ul>
                    }
                    type="info"
                    showIcon
                    size="small"
                    style={{ marginBottom: 16 }}
                />

                {/* Drag and Drop Upload Area */}
                <Dragger
                    multiple
                    fileList={fileList}
                    onChange={handleFileChange}
                    beforeUpload={() => false} // Prevent auto upload
                    accept=".pdf,.doc,.docx,.xls,.xlsx,.jpg,.jpeg,.png"
                    disabled={disabled || uploading}
                    style={{ marginBottom: 16 }}
                >
                    <p className="ant-upload-drag-icon">
                        <InboxOutlined />
                    </p>
                    <p className="ant-upload-text">
                        Click or drag files to this area to upload
                    </p>
                    <p className="ant-upload-hint">
                        Support for PDF, Word, Excel, and image files. Maximum 25MB each.
                    </p>
                </Dragger>

                {/* Upload Progress */}
                {uploading && uploadProgress > 0 && (
                    <Card size="small" style={{ marginBottom: 16 }}>
                        <Space direction="vertical" style={{ width: '100%' }}>
                            <Text>Uploading documents...</Text>
                            <Progress
                                percent={uploadProgress}
                                status={uploadProgress === 100 ? 'success' : 'active'}
                                strokeColor={{
                                    '0%': '#108ee9',
                                    '100%': '#87d068',
                                }}
                            />
                        </Space>
                    </Card>
                )}

                {/* Selected Files List */}
                {fileList.length > 0 && (
                    <Card
                        size="small"
                        title={`Selected Files (${fileList.length}/${maxFiles})`}
                        style={{ marginBottom: 16 }}
                    >
                        <List
                            size="small"
                            dataSource={fileList}
                            renderItem={file => (
                                <List.Item
                                    actions={[
                                        <Button
                                            key="remove"
                                            type="text"
                                            danger
                                            icon={<DeleteOutlined />}
                                            onClick={() => removeFile(file)}
                                            size="small"
                                            disabled={uploading}
                                        >
                                            Remove
                                        </Button>
                                    ]}
                                >
                                    <List.Item.Meta
                                        avatar={
                                            file.status === 'done' ? (
                                                <CheckCircleOutlined style={{ color: '#52c41a' }} />
                                            ) : (
                                                <ExclamationCircleOutlined style={{ color: '#ff4d4f' }} />
                                            )
                                        }
                                        title={
                                            <Space>
                                                <Text
                                                    strong={file.status === 'done'}
                                                    type={file.status === 'error' ? 'danger' : 'default'}
                                                >
                                                    {file.name}
                                                </Text>
                                                {file.size && (
                                                    <Text type="secondary">({formatFileSize(file.size)})</Text>
                                                )}
                                            </Space>
                                        }
                                        description={
                                            file.status === 'error' ? (
                                                <Text type="danger">{file.response}</Text>
                                            ) : (
                                                <Text type="secondary">Ready for upload</Text>
                                            )
                                        }
                                    />
                                </List.Item>
                            )}
                        />
                    </Card>
                )}

                {/* Upload Button */}
                <div style={{ textAlign: 'center' }}>
                    <Button
                        type="primary"
                        icon={<UploadOutlined />}
                        onClick={handleUpload}
                        loading={uploading}
                        disabled={disabled || validFileCount === 0}
                        size="large"
                    >
                        {uploading
                            ? `Uploading ${validFileCount} Document(s)...`
                            : `Upload ${validFileCount} Document(s) to ${contextLabel}`
                        }
                    </Button>
                </div>

                {/* File Validation Summary */}
                {fileList.length > 0 && (
                    <Alert
                        message="File Validation Summary"
                        description={
                            <Space direction="vertical" size="small">
                                <Text>
                                    <CheckCircleOutlined style={{ color: '#52c41a', marginRight: 4 }} />
                                    Valid files: {validFileCount}
                                </Text>
                                {fileList.filter(f => f.status === 'error').length > 0 && (
                                    <Text type="danger">
                                        <ExclamationCircleOutlined style={{ marginRight: 4 }} />
                                        Files with errors: {fileList.filter(f => f.status === 'error').length}
                                    </Text>
                                )}
                            </Space>
                        }
                        type={fileList.filter(f => f.status === 'error').length > 0 ? 'warning' : 'success'}
                        showIcon
                        size="small"
                    />
                )}
            </Space>
        </Card>
    );
};

export default QueryDocumentUpload;