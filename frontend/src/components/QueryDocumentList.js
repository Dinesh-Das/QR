import {
  FileTextOutlined,
  DownloadOutlined,
  DeleteOutlined,
  FolderOpenOutlined,
  QuestionCircleOutlined,
  MessageOutlined,
  CalendarOutlined,
  UserOutlined,
  FileOutlined
} from '@ant-design/icons';
import {
  Card,
  List,
  Button,
  Space,
  Typography,
  Tag,
  Divider,
  Empty,
  message,
  Popconfirm,
  Tooltip,
  Alert,
  Spin
} from 'antd';
import React, { useState, useEffect, useCallback } from 'react';

import { queryAPI } from '../services/queryAPI';


const { Text, Title } = Typography;

/**
 * QueryDocumentList Component
 * 
 * Displays all documents associated with a query, including:
 * - Documents attached to the original query
 * - Documents attached to query responses
 * - Grouped display with clear source indicators
 * - Download functionality with proper error handling
 * - Document metadata (size, type, upload date, uploader)
 * 
 * @param {Object} props
 * @param {string} props.queryId - Query ID to fetch documents for
 * @param {boolean} props.allowDelete - Whether to show delete buttons (default: false)
 * @param {Function} props.onDocumentDeleted - Callback when document is deleted
 * @param {boolean} props.showBulkActions - Whether to show bulk action buttons (default: true)
 * @param {string} props.currentUser - Current user for permission checks
 */
const QueryDocumentList = ({
  queryId,
  allowDelete = false,
  onDocumentDeleted,
  showBulkActions = true,
  currentUser
}) => {
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [downloadingIds, setDownloadingIds] = useState(new Set());
  const [deletingIds, setDeletingIds] = useState(new Set());

  // Fetch documents on component mount and when queryId changes
  useEffect(() => {
    if (queryId) {
      fetchDocuments();
    }
  }, [queryId, fetchDocuments]);

  // Fetch all documents for the query
  const fetchDocuments = useCallback(async () => {
    try {
      setLoading(true);
      const result = await queryAPI.getQueryDocuments(queryId);
      setDocuments(result || []);
    } catch (error) {
      console.error('Error fetching query documents:', error);
      message.error('Failed to load documents. Please try again.');
      setDocuments([]);
    } finally {
      setLoading(false);
    }
  }, [queryId]);

  // Format file size for display
  const formatFileSize = (bytes) => {
    if (!bytes || bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
  };

  // Format date for display
  const formatDate = (dateString) => {
    if (!dateString) return 'Unknown';
    try {
      return new Date(dateString).toLocaleDateString('en-GB', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (error) {
      return 'Invalid Date';
    }
  };

  // Get file type icon based on file extension
  const getFileIcon = (fileName) => {
    if (!fileName) return <FileOutlined />;
    
    const extension = fileName.toLowerCase().split('.').pop();
    const iconStyle = { fontSize: '16px' };
    
    switch (extension) {
      case 'pdf':
        return <FileTextOutlined style={{ ...iconStyle, color: '#ff4d4f' }} />;
      case 'doc':
      case 'docx':
        return <FileTextOutlined style={{ ...iconStyle, color: '#1890ff' }} />;
      case 'xls':
      case 'xlsx':
        return <FileTextOutlined style={{ ...iconStyle, color: '#52c41a' }} />;
      case 'jpg':
      case 'jpeg':
      case 'png':
        return <FileOutlined style={{ ...iconStyle, color: '#722ed1' }} />;
      default:
        return <FileOutlined style={iconStyle} />;
    }
  };

  // Handle document download
  const handleDownload = async (document) => {
    try {
      setDownloadingIds(prev => new Set([...prev, document.id]));
      
      const blob = await queryAPI.downloadQueryDocument(queryId, document.id);
      
      // Create download link
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = document.originalFileName || document.fileName;
      document.body.appendChild(link);
      link.click();
      
      // Cleanup
      window.URL.revokeObjectURL(url);
      document.body.removeChild(link);
      
      message.success(`Downloaded ${document.originalFileName || document.fileName}`);
    } catch (error) {
      console.error('Error downloading document:', error);
      message.error('Failed to download document. Please try again.');
    } finally {
      setDownloadingIds(prev => {
        const newSet = new Set(prev);
        newSet.delete(document.id);
        return newSet;
      });
    }
  };

  // Handle document deletion
  const handleDelete = async (document) => {
    try {
      setDeletingIds(prev => new Set([...prev, document.id]));
      
      await queryAPI.deleteQueryDocument(queryId, document.id);
      
      message.success(`Deleted ${document.originalFileName || document.fileName}`);
      
      // Remove from local state
      setDocuments(prev => prev.filter(doc => doc.id !== document.id));
      
      // Notify parent component
      if (onDocumentDeleted) {
        onDocumentDeleted(document);
      }
    } catch (error) {
      console.error('Error deleting document:', error);
      message.error('Failed to delete document. Please try again.');
    } finally {
      setDeletingIds(prev => {
        const newSet = new Set(prev);
        newSet.delete(document.id);
        return newSet;
      });
    }
  };

  // Handle bulk download
  const handleBulkDownload = async () => {
    if (documents.length === 0) {
      message.warning('No documents to download.');
      return;
    }

    message.info(`Starting download of ${documents.length} documents...`);
    
    for (const doc of documents) {
      try {
        await handleDownload(doc);
        // Small delay between downloads to avoid overwhelming the server
        await new Promise(resolve => setTimeout(resolve, 500));
      } catch (error) {
        console.error(`Failed to download ${doc.originalFileName}:`, error);
      }
    }
  };

  // Group documents by source (query vs response)
  const groupedDocuments = documents.reduce((groups, doc) => {
    const source = doc.responseId ? 'response' : 'query';
    if (!groups[source]) {
      groups[source] = [];
    }
    groups[source].push(doc);
    return groups;
  }, {});

  // Check if user can delete document
  const canDeleteDocument = (document) => {
    if (!allowDelete) return false;
    if (!currentUser) return false;
    return document.uploadedBy === currentUser || currentUser.role === 'ADMIN';
  };

  if (loading) {
    return (
      <Card>
        <div style={{ textAlign: 'center', padding: '20px' }}>
          <Spin size="large" />
          <div style={{ marginTop: '16px' }}>
            <Text type="secondary">Loading documents...</Text>
          </div>
        </div>
      </Card>
    );
  }

  if (documents.length === 0) {
    return (
      <Card>
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description={
            <Space direction="vertical">
              <Text type="secondary">No documents attached to this query</Text>
              <Text type="secondary" style={{ fontSize: '12px' }}>
                Documents can be attached when creating queries or responding to them
              </Text>
            </Space>
          }
        />
      </Card>
    );
  }

  return (
    <Card
      title={
        <Space>
          <FolderOpenOutlined />
          <span>Query Documents ({documents.length})</span>
        </Space>
      }
      extra={
        showBulkActions && documents.length > 0 && (
          <Space>
            <Button
              icon={<DownloadOutlined />}
              onClick={handleBulkDownload}
              size="small"
            >
              Download All
            </Button>
          </Space>
        )
      }
    >
      <Space direction="vertical" style={{ width: '100%' }}>
        {/* Summary Alert */}
        <Alert
          message={`${documents.length} document(s) attached to this query`}
          description={
            <Space>
              {groupedDocuments.query && (
                <Text type="secondary">
                  {groupedDocuments.query.length} from original query
                </Text>
              )}
              {groupedDocuments.response && (
                <Text type="secondary">
                  {groupedDocuments.response.length} from responses
                </Text>
              )}
            </Space>
          }
          type="info"
          showIcon
          size="small"
        />

        {/* Original Query Documents */}
        {groupedDocuments.query && groupedDocuments.query.length > 0 && (
          <>
            <Title level={5}>
              <QuestionCircleOutlined style={{ color: '#1890ff' }} />
              <span style={{ marginLeft: '8px' }}>
                Original Query Documents ({groupedDocuments.query.length})
              </span>
            </Title>
            
            <List
              size="small"
              dataSource={groupedDocuments.query}
              renderItem={document => (
                <List.Item
                  actions={[
                    <Tooltip title="Download document" key="download">
                      <Button
                        type="text"
                        icon={<DownloadOutlined />}
                        onClick={() => handleDownload(document)}
                        loading={downloadingIds.has(document.id)}
                        size="small"
                      />
                    </Tooltip>,
                    ...(canDeleteDocument(document) ? [
                      <Popconfirm
                        key="delete"
                        title="Delete Document"
                        description="Are you sure you want to delete this document?"
                        onConfirm={() => handleDelete(document)}
                        okText="Yes"
                        cancelText="No"
                        icon={<QuestionCircleOutlined style={{ color: 'red' }} />}
                      >
                        <Tooltip title="Delete document">
                          <Button
                            type="text"
                            danger
                            icon={<DeleteOutlined />}
                            loading={deletingIds.has(document.id)}
                            size="small"
                          />
                        </Tooltip>
                      </Popconfirm>
                    ] : [])
                  ]}
                >
                  <List.Item.Meta
                    avatar={getFileIcon(document.originalFileName)}
                    title={
                      <Space>
                        <Text strong>{document.originalFileName || document.fileName}</Text>
                        <Tag color="blue" size="small">Query</Tag>
                      </Space>
                    }
                    description={
                      <Space direction="vertical" size="small">
                        <Space size="large">
                          <Text type="secondary">
                            <FileOutlined style={{ marginRight: '4px' }} />
                            {formatFileSize(document.fileSize)}
                          </Text>
                          <Text type="secondary">
                            <CalendarOutlined style={{ marginRight: '4px' }} />
                            {formatDate(document.uploadedAt)}
                          </Text>
                          <Text type="secondary">
                            <UserOutlined style={{ marginRight: '4px' }} />
                            {document.uploadedBy || 'Unknown'}
                          </Text>
                        </Space>
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          </>
        )}

        {/* Response Documents */}
        {groupedDocuments.response && groupedDocuments.response.length > 0 && (
          <>
            {groupedDocuments.query && <Divider />}
            
            <Title level={5}>
              <MessageOutlined style={{ color: '#52c41a' }} />
              <span style={{ marginLeft: '8px' }}>
                Response Documents ({groupedDocuments.response.length})
              </span>
            </Title>
            
            <List
              size="small"
              dataSource={groupedDocuments.response}
              renderItem={document => (
                <List.Item
                  actions={[
                    <Tooltip title="Download document" key="download">
                      <Button
                        type="text"
                        icon={<DownloadOutlined />}
                        onClick={() => handleDownload(document)}
                        loading={downloadingIds.has(document.id)}
                        size="small"
                      />
                    </Tooltip>,
                    ...(canDeleteDocument(document) ? [
                      <Popconfirm
                        key="delete"
                        title="Delete Document"
                        description="Are you sure you want to delete this document?"
                        onConfirm={() => handleDelete(document)}
                        okText="Yes"
                        cancelText="No"
                        icon={<QuestionCircleOutlined style={{ color: 'red' }} />}
                      >
                        <Tooltip title="Delete document">
                          <Button
                            type="text"
                            danger
                            icon={<DeleteOutlined />}
                            loading={deletingIds.has(document.id)}
                            size="small"
                          />
                        </Tooltip>
                      </Popconfirm>
                    ] : [])
                  ]}
                >
                  <List.Item.Meta
                    avatar={getFileIcon(document.originalFileName)}
                    title={
                      <Space>
                        <Text strong>{document.originalFileName || document.fileName}</Text>
                        <Tag color="green" size="small">Response</Tag>
                        {document.responseId && (
                          <Tag color="orange" size="small">#{document.responseId}</Tag>
                        )}
                      </Space>
                    }
                    description={
                      <Space direction="vertical" size="small">
                        <Space size="large">
                          <Text type="secondary">
                            <FileOutlined style={{ marginRight: '4px' }} />
                            {formatFileSize(document.fileSize)}
                          </Text>
                          <Text type="secondary">
                            <CalendarOutlined style={{ marginRight: '4px' }} />
                            {formatDate(document.uploadedAt)}
                          </Text>
                          <Text type="secondary">
                            <UserOutlined style={{ marginRight: '4px' }} />
                            {document.uploadedBy || 'Unknown'}
                          </Text>
                        </Space>
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          </>
        )}
      </Space>
    </Card>
  );
};

export default QueryDocumentList;