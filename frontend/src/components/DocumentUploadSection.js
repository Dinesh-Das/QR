import {
  UploadOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  DeleteOutlined
} from '@ant-design/icons';
import { Upload, Button, Card, Space, Typography, Alert, message, List } from 'antd';
import React, { useState } from 'react';

import { documentAPI } from '../services/documentAPI';

const { Text, Title } = Typography;
const { Dragger } = Upload;

const DocumentUploadSection = ({ workflowId, projectCode, materialCode, onDocumentsUploaded }) => {
  const [fileList, setFileList] = useState([]);
  const [uploading, setUploading] = useState(false);

  const handleFileChange = ({ fileList: newFileList }) => {
    const validatedFileList = newFileList.map(file => {
      if (file.originFileObj) {
        const validation = documentAPI.validateFile(file.originFileObj);
        if (!validation.isValidType) {
          file.status = 'error';
          file.response = 'Invalid file type. Only PDF, Word, and Excel files are allowed.';
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

  const handleUpload = async () => {
    const validFiles = fileList.filter(file => file.status === 'done');

    if (validFiles.length === 0) {
      message.error('Please select at least one valid file to upload.');
      return;
    }

    try {
      setUploading(true);

      const files = validFiles.map(file => file.originFileObj || file);

      await documentAPI.uploadDocuments(files, projectCode, materialCode, workflowId);

      message.success(`Successfully uploaded ${validFiles.length} document(s) to the workflow.`);

      // Clear the file list
      setFileList([]);

      // Notify parent component to refresh
      if (onDocumentsUploaded) {
        onDocumentsUploaded();
      }
    } catch (error) {
      console.error('Error uploading documents:', error);
      message.error('Failed to upload documents. Please try again.');
    } finally {
      setUploading(false);
    }
  };

  const removeFile = file => {
    const newFileList = fileList.filter(item => item.uid !== file.uid);
    setFileList(newFileList);
  };

  return (
    <Card size="small" style={{ backgroundColor: '#f9f9f9', border: '1px dashed #d9d9d9' }}>
      <Space direction="vertical" style={{ width: '100%' }}>
        <Alert
          message="No Documents Found"
          description="This workflow was initiated without documents. You can upload safety documents now to complete the workflow preparation."
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
        />

        <Title level={5}>
          <UploadOutlined /> Upload Documents
        </Title>

        <Dragger
          multiple
          fileList={fileList}
          onChange={handleFileChange}
          beforeUpload={() => false} // Prevent auto upload
          accept=".pdf,.doc,.docx,.xls,.xlsx"
          style={{ marginBottom: 16 }}
        >
          <p className="ant-upload-drag-icon">
            <UploadOutlined />
          </p>
          <p className="ant-upload-text">Click or drag files to this area to upload</p>
          <p className="ant-upload-hint">
            Support for PDF, Word, and Excel files. Maximum file size: 25MB each
          </p>
        </Dragger>

        {fileList.length > 0 && (
          <Card size="small" title="Selected Files" style={{ marginBottom: 16 }}>
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
                        <FileTextOutlined style={{ color: '#ff4d4f' }} />
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
                          <Text type="secondary">({(file.size / 1024 / 1024).toFixed(2)} MB)</Text>
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

        <div style={{ textAlign: 'center' }}>
          <Button
            type="primary"
            icon={<UploadOutlined />}
            onClick={handleUpload}
            loading={uploading}
            disabled={fileList.filter(f => f.status === 'done').length === 0}
            size="large"
          >
            Upload {fileList.filter(f => f.status === 'done').length} Document(s)
          </Button>
        </div>

        <Alert
          message="Upload Guidelines"
          description={
            <ul style={{ margin: 0, paddingLeft: 20 }}>
              <li>Only PDF, Word (.doc, .docx), and Excel (.xls, .xlsx) files are allowed</li>
              <li>Maximum file size is 25MB per file</li>
              <li>You can upload multiple files at once</li>
              <li>Files will be associated with this workflow for plant review</li>
            </ul>
          }
          type="warning"
          showIcon
          size="small"
        />
      </Space>
    </Card>
  );
};

export default DocumentUploadSection;
