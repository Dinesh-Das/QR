import React, { useState, useEffect } from 'react';
import { 
  Card, 
  Table, 
  Button, 
  Space, 
  Tag, 
  Tabs, 
  Modal, 
  message, 
  Tooltip,
  Divider,
  Typography,
  Row,
  Col,
  Statistic
} from 'antd';
import {
  FileOutlined,
  DownloadOutlined,
  SearchOutlined,
  ExportOutlined,
  InfoCircleOutlined,
  FolderOpenOutlined,
  QuestionCircleOutlined
} from '@ant-design/icons';
import { documentAPI } from '../services/documentAPI';
import { queryAPI } from '../services/queryAPI';
import UnifiedDocumentSearch from './UnifiedDocumentSearch';

const { TabPane } = Tabs;
const { Text, Title } = Typography;

/**
 * Enhanced Workflow Document View Component
 * 
 * Features:
 * - Shows workflow documents with source information
 * - Displays related query documents
 * - Unified document search and export
 * - Document source categorization
 */
const WorkflowDocumentView = ({ 
  workflowId, 
  projectCode, 
  materialCode,
  showRelatedDocuments = true,
  allowExport = true 
}) => {
  const [loading, setLoading] = useState(false);
  const [workflowDocuments, setWorkflowDocuments] = useState([]);
  const [relatedQueryDocuments, setRelatedQueryDocuments] = useState([]);
  const [searchModalVisible, setSearchModalVisible] = useState(false);
  const [exportModalVisible, setExportModalVisible] = useState(false);
  const [documentStats, setDocumentStats] = useState({});

  useEffect(() => {
    if (workflowId) {
      loadWorkflowDocuments();
    }
    if (showRelatedDocuments && projectCode && materialCode) {
      loadRelatedDocuments();
    }
  }, [workflowId, projectCode, materialCode, showRelatedDocuments]);

  const loadWorkflowDocuments = async () => {
    try {
      setLoading(true);
      const documents = await documentAPI.getWorkflowDocuments(workflowId);
      setWorkflowDocuments(documents || []);
      
      // Calculate document statistics
      const stats = calculateDocumentStats(documents || []);
      setDocumentStats(stats);
    } catch (error) {
      console.error('Error loading workflow documents:', error);
      message.error('Failed to load workflow documents');
    } finally {
      setLoading(false);
    }
  };

  const loadRelatedDocuments = async () => {
    try {
      // Get all documents for the same project/material from queries
      const searchResult = await documentAPI.searchAllDocuments('', projectCode, materialCode, ['QUERY', 'RESPONSE']);
      
      if (searchResult && searchResult.allDocuments) {
        const queryDocs = searchResult.allDocuments.filter(doc => 
          doc.documentSource === 'QUERY' || doc.documentSource === 'RESPONSE'
        );
        setRelatedQueryDocuments(queryDocs);
      }
    } catch (error) {
      console.error('Error loading related query documents:', error);
      // Don't show error message for related documents as it's supplementary
    }
  };

  const calculateDocumentStats = (documents) => {
    const stats = {
      total: documents.length,
      bySource: {},
      byType: {},
      totalSize: 0,
      reusedCount: 0
    };

    documents.forEach(doc => {
      // Count by source
      const source = doc.documentSource || 'WORKFLOW';
      stats.bySource[source] = (stats.bySource[source] || 0) + 1;
      
      // Count by file type
      const type = doc.fileType || 'unknown';
      stats.byType[type] = (stats.byType[type] || 0) + 1;
      
      // Total size
      stats.totalSize += doc.fileSize || 0;
      
      // Reused documents
      if (doc.isReused) {
        stats.reusedCount++;
      }
    });

    return stats;
  };

  const handleDownload = async (document) => {
    try {
      let blob;
      if (document.documentSource === 'WORKFLOW') {
        blob = await documentAPI.downloadDocument(document.id, workflowId);
      } else {
        blob = await queryAPI.downloadQueryDocument(document.queryId, document.id);
      }
      
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = document.originalFileName;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      
      message.success(`Downloaded: ${document.originalFileName}`);
    } catch (error) {
      console.error('Download error:', error);
      message.error(`Failed to download: ${document.originalFileName}`);
    }
  };

  const handleExportAll = async (format = 'zip') => {
    try {
      setLoading(true);
      
      // For now, download all documents individually
      // In a real implementation, you'd have a backend endpoint to create a zip
      const allDocuments = [...workflowDocuments, ...relatedQueryDocuments];
      
      for (const doc of allDocuments) {
        await handleDownload(doc);
        // Add small delay to prevent overwhelming the server
        await new Promise(resolve => setTimeout(resolve, 500));
      }
      
      message.success(`Exported ${allDocuments.length} documents`);
      setExportModalVisible(false);
    } catch (error) {
      console.error('Export error:', error);
      message.error('Failed to export documents');
    } finally {
      setLoading(false);
    }
  };

  const getSourceTag = (source) => {
    const sourceConfig = {
      WORKFLOW: { color: 'blue', text: 'Workflow' },
      QUERY: { color: 'orange', text: 'Query' },
      RESPONSE: { color: 'purple', text: 'Response' }
    };
    
    const config = sourceConfig[source] || { color: 'default', text: source };
    return <Tag color={config.color}>{config.text}</Tag>;
  };

  const formatFileSize = (bytes) => {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const workflowDocumentColumns = [
    {
      title: 'Document Name',
      dataIndex: 'originalFileName',
      key: 'originalFileName',
      render: (text, record) => (
        <div>
          <div style={{ fontWeight: 'bold' }}>
            <FileOutlined style={{ marginRight: 8 }} />
            {text}
          </div>
          {record.isReused && (
            <Text type="secondary" style={{ fontSize: '12px' }}>
              <InfoCircleOutlined style={{ marginRight: 4 }} />
              Reused from original document
            </Text>
          )}
        </div>
      )
    },
    {
      title: 'Source',
      dataIndex: 'documentSource',
      key: 'documentSource',
      width: 100,
      render: (source) => getSourceTag(source)
    },
    {
      title: 'Type',
      dataIndex: 'fileType',
      key: 'fileType',
      width: 80,
      render: (type) => <Tag>{type?.toUpperCase()}</Tag>
    },
    {
      title: 'Size',
      dataIndex: 'fileSize',
      key: 'fileSize',
      width: 100,
      render: (size) => formatFileSize(size)
    },
    {
      title: 'Uploaded By',
      dataIndex: 'uploadedBy',
      key: 'uploadedBy',
      width: 120
    },
    {
      title: 'Upload Date',
      dataIndex: 'uploadedAt',
      key: 'uploadedAt',
      width: 120,
      render: (date) => new Date(date).toLocaleDateString()
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 100,
      render: (_, record) => (
        <Space>
          <Tooltip title="Download">
            <Button
              type="text"
              icon={<DownloadOutlined />}
              onClick={() => handleDownload(record)}
              size="small"
            />
          </Tooltip>
        </Space>
      )
    }
  ];

  const relatedDocumentColumns = [
    {
      title: 'Document Name',
      dataIndex: 'originalFileName',
      key: 'originalFileName',
      render: (text, record) => (
        <div>
          <div style={{ fontWeight: 'bold' }}>
            <FileOutlined style={{ marginRight: 8 }} />
            {text}
          </div>
          <Text type="secondary" style={{ fontSize: '12px' }}>
            {record.documentSource === 'QUERY' ? 'From Query' : 'From Query Response'}
            {record.queryId && ` #${record.queryId}`}
          </Text>
        </div>
      )
    },
    {
      title: 'Source',
      dataIndex: 'documentSource',
      key: 'documentSource',
      width: 100,
      render: (source) => getSourceTag(source)
    },
    {
      title: 'Type',
      dataIndex: 'fileType',
      key: 'fileType',
      width: 80,
      render: (type) => <Tag>{type?.toUpperCase()}</Tag>
    },
    {
      title: 'Size',
      dataIndex: 'fileSize',
      key: 'fileSize',
      width: 100,
      render: (size) => formatFileSize(size)
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 100,
      render: (_, record) => (
        <Space>
          <Tooltip title="Download">
            <Button
              type="text"
              icon={<DownloadOutlined />}
              onClick={() => handleDownload(record)}
              size="small"
            />
          </Tooltip>
        </Space>
      )
    }
  ];

  return (
    <div>
      {/* Document Statistics */}
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title="Total Documents"
              value={documentStats.total || 0}
              prefix={<FileOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title="Related Query Docs"
              value={relatedQueryDocuments.length}
              prefix={<QuestionCircleOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title="Reused Documents"
              value={documentStats.reusedCount || 0}
              prefix={<InfoCircleOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title="Total Size"
              value={formatFileSize(documentStats.totalSize || 0)}
              prefix={<FolderOpenOutlined />}
            />
          </Card>
        </Col>
      </Row>

      {/* Main Document View */}
      <Card
        title="Workflow Documents"
        extra={
          <Space>
            <Button
              icon={<SearchOutlined />}
              onClick={() => setSearchModalVisible(true)}
            >
              Search All Documents
            </Button>
            {allowExport && (
              <Button
                icon={<ExportOutlined />}
                onClick={() => setExportModalVisible(true)}
              >
                Export All
              </Button>
            )}
          </Space>
        }
      >
        <Tabs defaultActiveKey="workflow">
          <TabPane tab={`Workflow Documents (${workflowDocuments.length})`} key="workflow">
            <Table
              dataSource={workflowDocuments}
              columns={workflowDocumentColumns}
              loading={loading}
              rowKey="id"
              size="small"
              pagination={{ pageSize: 10 }}
              locale={{
                emptyText: 'No documents attached to this workflow'
              }}
            />
          </TabPane>
          
          {showRelatedDocuments && (
            <TabPane tab={`Related Query Documents (${relatedQueryDocuments.length})`} key="related">
              <div style={{ marginBottom: 16 }}>
                <Text type="secondary">
                  Documents from queries and responses for the same project/material combination
                </Text>
              </div>
              <Table
                dataSource={relatedQueryDocuments}
                columns={relatedDocumentColumns}
                loading={loading}
                rowKey="id"
                size="small"
                pagination={{ pageSize: 10 }}
                locale={{
                  emptyText: 'No related query documents found'
                }}
              />
            </TabPane>
          )}
        </Tabs>
      </Card>

      {/* Unified Document Search Modal */}
      <Modal
        title="Unified Document Search"
        visible={searchModalVisible}
        onCancel={() => setSearchModalVisible(false)}
        footer={null}
        width={1000}
        destroyOnClose
      >
        <UnifiedDocumentSearch
          projectCode={projectCode}
          materialCode={materialCode}
          defaultSources={['WORKFLOW', 'QUERY', 'RESPONSE']}
        />
      </Modal>

      {/* Export Modal */}
      <Modal
        title="Export All Documents"
        visible={exportModalVisible}
        onCancel={() => setExportModalVisible(false)}
        onOk={() => handleExportAll('zip')}
        confirmLoading={loading}
      >
        <div>
          <p>This will download all documents related to this workflow:</p>
          <ul>
            <li>Workflow documents: {workflowDocuments.length}</li>
            <li>Related query documents: {relatedQueryDocuments.length}</li>
            <li>Total: {workflowDocuments.length + relatedQueryDocuments.length} documents</li>
          </ul>
          <Divider />
          <Text type="secondary">
            Documents will be downloaded individually. In a production environment, 
            this would create a single ZIP file containing all documents.
          </Text>
        </div>
      </Modal>
    </div>
  );
};

export default WorkflowDocumentView;