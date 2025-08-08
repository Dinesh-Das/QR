import {
  InfoCircleOutlined,
  FileTextOutlined,
  TeamOutlined,
  CalendarOutlined,
  DownloadOutlined,
  WarningOutlined
} from '@ant-design/icons';
import {
  Card,
  Descriptions,
  Tag,
  Space,
  Button,
  Collapse,
  Alert,
  Divider,
  Typography,
  Tooltip,
  Badge,
  Spin,
  Progress,
  message
} from 'antd';
import React, { useState, useEffect, useCallback } from 'react';

import { workflowAPI } from '../services/workflowAPI';

const { Panel } = Collapse;
const { Text } = Typography;

const MaterialContextPanel = ({ workflowData }) => {
  const [loading, setLoading] = useState(false);
  const [jvcDocuments, setJvcDocuments] = useState([]);
  const [expanded, setExpanded] = useState(['basic', 'workflow', 'jvc-data']);

  const loadJvcDocuments = useCallback(async () => {
    try {
      setLoading(true);
      
      // Get reusable documents for the same project/material combination
      if (workflowData?.projectCode && workflowData?.materialCode) {
        const documents = await workflowAPI.getReusableDocuments(
          workflowData.projectCode, 
          workflowData.materialCode
        );
        setJvcDocuments(documents || []);
      } else {
        // Fallback to workflow documents if project/material codes are not available
        const documents = await workflowAPI.getWorkflowDocuments(workflowData.id);
        setJvcDocuments(documents || []);
      }
    } catch (error) {
      console.error('Failed to load JVC documents:', error);
      setJvcDocuments([]);
    } finally {
      setLoading(false);
    }
  }, [workflowData?.id, workflowData?.projectCode, workflowData?.materialCode]);

  useEffect(() => {
    if (workflowData?.id) {
      loadJvcDocuments();
    }
  }, [workflowData?.id, workflowData?.projectCode, workflowData?.materialCode, loadJvcDocuments]);

  const getStateColor = state => {
    const colors = {
      JVC_PENDING: 'blue',
      PLANT_PENDING: 'orange',
      CQS_PENDING: 'purple',
      TECH_PENDING: 'cyan',
      COMPLETED: 'green'
    };
    return colors[state] || 'default';
  };

  const getStateName = state => {
    const names = {
      JVC_PENDING: 'JVC Pending',
      PLANT_PENDING: 'Plant Pending',
      CQS_PENDING: 'CQS Pending',
      TECH_PENDING: 'Tech Pending',
      COMPLETED: 'Completed'
    };
    return names[state] || state;
  };

  const formatDate = dateString => {
    if (!dateString) {
      return 'N/A';
    }
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const calculateDaysInState = lastModified => {
    if (!lastModified) {
      return 0;
    }
    const now = new Date();
    const modified = new Date(lastModified);
    const diffTime = Math.abs(now - modified);
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  };

  const handleDocumentDownload = async (documentId, filename) => {
    try {
      console.log('Downloading document:', { documentId, filename });
      const blob = await workflowAPI.downloadDocument(documentId);
      console.log('Download blob received:', blob);
      
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename || `document_${documentId}`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      message.success(`Downloaded ${filename || 'document'}`);
    } catch (error) {
      console.error('Failed to download document:', error);
      message.error(`Failed to download document: ${error.message || 'Unknown error'}`);
    }
  };



  if (!workflowData) {
    return (
      <Card title="Material Context" style={{ height: 'fit-content' }}>
        <Alert
          message="No Material Data"
          description="Material context will appear here once a workflow is selected."
          type="info"
          showIcon
        />
      </Card>
    );
  }

  const daysInCurrentState = calculateDaysInState(workflowData.lastModified);
  const isOverdue = daysInCurrentState > 3;

  return (
    <Card
      title={
        <Space>
          <InfoCircleOutlined />
          Material Context
          {isOverdue && <Badge status="error" text={`${daysInCurrentState} days`} />}
        </Space>
      }
      style={{ height: 'fit-content', position: 'sticky', top: 24 }}
      size="small"
    >
      <Collapse activeKey={expanded} onChange={setExpanded} ghost size="small">
        {/* Basic Material Information */}
        <Panel
          header={
            <Space>
              <FileTextOutlined />
              <strong>Basic Information</strong>
            </Space>
          }
          key="basic"
        >
          <Descriptions column={1} size="small">
            <Descriptions.Item label="Material Code">
              <Text strong>{workflowData.materialCode}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Material Name">
              {workflowData.materialName || 'Not specified'}
            </Descriptions.Item>
            <Descriptions.Item label="Assigned Plant">
              <Tag color="blue">{workflowData.assignedPlant}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Initiated By">
              <Space>
                <TeamOutlined />
                {workflowData.initiatedBy}
              </Space>
            </Descriptions.Item>
            <Descriptions.Item label="Created">
              <Space>
                <CalendarOutlined />
                {formatDate(workflowData.createdAt)}
              </Space>
            </Descriptions.Item>
          </Descriptions>
        </Panel>

        {/* Workflow Status */}
        <Panel
          header={
            <Space>
              <TeamOutlined />
              <strong>Workflow Status</strong>
              {isOverdue && <WarningOutlined style={{ color: '#ff4d4f' }} />}
            </Space>
          }
          key="workflow"
        >
          <Space direction="vertical" style={{ width: '100%' }}>
            <div>
              <Text strong>Current State:</Text>
              <div style={{ marginTop: 4 }}>
                <Tag color={getStateColor(workflowData.state)}>
                  {getStateName(workflowData.state)}
                </Tag>
              </div>
            </div>

            <div>
              <Text strong>Time in Current State:</Text>
              <div style={{ marginTop: 4 }}>
                <Text style={{ color: isOverdue ? '#ff4d4f' : 'inherit' }}>
                  {daysInCurrentState} day{daysInCurrentState !== 1 ? 's' : ''}
                  {isOverdue && ' (Overdue)'}
                </Text>
              </div>
            </div>

            <div>
              <Text strong>Last Modified:</Text>
              <div style={{ marginTop: 4 }}>
                <Text type="secondary">{formatDate(workflowData.lastModified)}</Text>
              </div>
            </div>

            {workflowData.totalQueries > 0 && (
              <div>
                <Text strong>Queries:</Text>
                <div style={{ marginTop: 4 }}>
                  <Space>
                    <Tag color="red">{workflowData.openQueries || 0} Open</Tag>
                    <Tag color="green">
                      {(workflowData.totalQueries || 0) - (workflowData.openQueries || 0)} Resolved
                    </Tag>
                  </Space>
                </div>
              </div>
            )}

            {/* Progress indicator */}
            {workflowData.completionPercentage !== undefined && (
              <div>
                <Text strong>Completion Progress:</Text>
                <div style={{ marginTop: 4 }}>
                  <Progress
                    percent={workflowData.completionPercentage}
                    size="small"
                    status={workflowData.completionPercentage === 100 ? 'success' : 'active'}
                  />
                </div>
              </div>
            )}
          </Space>
        </Panel>

        {/* JVC Provided Documents */}
        <Panel
          header={
            <Space>
              <FileTextOutlined />
              <strong>JVC Documents</strong>
              <Badge count={jvcDocuments.length} size="small" />
            </Space>
          }
          key="documents"
        >
          {loading ? (
            <div style={{ textAlign: 'center', padding: '20px' }}>
              <Spin size="small" />
            </div>
          ) : jvcDocuments.length > 0 ? (
            <Space direction="vertical" style={{ width: '100%' }}>
              {jvcDocuments.map((doc, index) => (
                <Card
                  key={doc.id || index}
                  size="small"
                  style={{ marginBottom: 8 }}
                  styles={{ body: { padding: '8px 12px' } }}
                >
                  <div style={{ marginBottom: 4 }}>
                    <Text strong style={{ fontSize: '12px' }}>
                      {doc.originalFileName || doc.fileName || `Document ${index + 1}`}
                    </Text>
                  </div>
                  <div style={{ marginBottom: 8 }}>
                    <Text type="secondary" style={{ fontSize: '11px' }}>
                      {doc.sourceDescription || 
                       `${doc.documentSource || 'Document'} • ${doc.fileType?.toUpperCase() || 'File'} • ${
                         doc.fileSize ? `${(doc.fileSize / 1024 / 1024).toFixed(1)} MB` : 'Unknown size'
                       } • Uploaded by ${doc.uploadedBy || 'Unknown'}`}
                    </Text>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <Tooltip title="Download document">
                      <Button
                        type="primary"
                        size="small"
                        icon={<DownloadOutlined />}
                        onClick={() => handleDocumentDownload(doc.id, doc.originalFileName || doc.fileName)}
                      >
                        Download
                      </Button>
                    </Tooltip>
                  </div>
                </Card>
              ))}
            </Space>
          ) : (
            <Alert
              message="No Documents"
              description="No documents have been provided by the JVC team for this material."
              type="info"
              showIcon
              size="small"
            />
          )}
        </Panel>

        {/* Enhanced JVC Material Data */}
        <Panel
          header={
            <Space>
              <InfoCircleOutlined />
              <strong>JVC Material Data</strong>
              <Tooltip title="Information provided by JVC team for questionnaire completion">
                <InfoCircleOutlined style={{ fontSize: '12px', color: '#1890ff' }} />
              </Tooltip>
            </Space>
          }
          key="jvc-data"
        >
          <Descriptions column={1} size="small">
            <Descriptions.Item label="Material Category">
              <Tag color="blue">{workflowData.materialCategory || 'Not specified'}</Tag>
            </Descriptions.Item>

            <Descriptions.Item label="Supplier Information">
              <Space direction="vertical" size="small" style={{ width: '100%' }}>
                <div>
                  <Text strong style={{ fontSize: '11px' }}>
                    Supplier Name:
                  </Text>
                  <div style={{ fontSize: '12px' }}>
                    {workflowData.supplierName || 'Not specified'}
                  </div>
                </div>
                <div>
                  <Text strong style={{ fontSize: '11px' }}>
                    Supplier Code:
                  </Text>
                  <div style={{ fontSize: '12px' }}>
                    {workflowData.supplierCode || 'Not specified'}
                  </div>
                </div>
                <div>
                  <Text strong style={{ fontSize: '11px' }}>
                    Contact Info:
                  </Text>
                  <div style={{ fontSize: '12px' }}>
                    {workflowData.supplierContact || 'Not provided'}
                  </div>
                </div>
              </Space>
            </Descriptions.Item>

            <Descriptions.Item label="Purchase Information">
              <Space direction="vertical" size="small" style={{ width: '100%' }}>
                <div>
                  <Text strong style={{ fontSize: '11px' }}>
                    Purchase Order:
                  </Text>
                  <div style={{ fontSize: '12px' }}>
                    {workflowData.purchaseOrder || 'Not specified'}
                  </div>
                </div>
                <div>
                  <Text strong style={{ fontSize: '11px' }}>
                    Quantity Ordered:
                  </Text>
                  <div style={{ fontSize: '12px' }}>
                    {workflowData.quantityOrdered || 'Not specified'}
                  </div>
                </div>
                <div>
                  <Text strong style={{ fontSize: '11px' }}>
                    Expected Delivery:
                  </Text>
                  <div style={{ fontSize: '12px' }}>
                    {workflowData.expectedDelivery
                      ? formatDate(workflowData.expectedDelivery)
                      : 'Not specified'}
                  </div>
                </div>
              </Space>
            </Descriptions.Item>

            <Descriptions.Item label="Usage & Application">
              <Space direction="vertical" size="small" style={{ width: '100%' }}>
                <div>
                  <Text strong style={{ fontSize: '11px' }}>
                    Expected Usage:
                  </Text>
                  <div style={{ fontSize: '12px' }}>
                    {workflowData.expectedUsage || 'Not specified'}
                  </div>
                </div>
                <div>
                  <Text strong style={{ fontSize: '11px' }}>
                    Application Area:
                  </Text>
                  <div style={{ fontSize: '12px' }}>
                    {workflowData.applicationArea || 'Not specified'}
                  </div>
                </div>
                <div>
                  <Text strong style={{ fontSize: '11px' }}>
                    Process Requirements:
                  </Text>
                  <div style={{ fontSize: '12px' }}>
                    {workflowData.processRequirements || 'Standard processing'}
                  </div>
                </div>
              </Space>
            </Descriptions.Item>

            <Descriptions.Item label="Regulatory Requirements">
              <div
                style={{
                  padding: '6px 8px',
                  backgroundColor: '#fff7e6',
                  border: '1px solid #ffd591',
                  borderRadius: '4px',
                  fontSize: '12px'
                }}
              >
                {workflowData.regulatoryRequirements || 'Standard compliance required'}
              </div>
            </Descriptions.Item>

            <Descriptions.Item label="Special Instructions">
              {workflowData.specialInstructions ? (
                <div
                  style={{
                    padding: '6px 8px',
                    backgroundColor: '#f6ffed',
                    border: '1px solid #b7eb8f',
                    borderRadius: '4px',
                    fontSize: '12px'
                  }}
                >
                  {workflowData.specialInstructions}
                </div>
              ) : (
                <Text type="secondary" style={{ fontSize: '12px' }}>
                  None
                </Text>
              )}
            </Descriptions.Item>

            <Descriptions.Item label="Priority & Impact">
              <Space direction="vertical" size="small" style={{ width: '100%' }}>
                <div>
                  <Text strong style={{ fontSize: '11px' }}>
                    Urgency Level:
                  </Text>
                  <div style={{ marginTop: 2 }}>
                    <Tag
                      color={
                        workflowData.urgencyLevel === 'HIGH'
                          ? 'red'
                          : workflowData.urgencyLevel === 'MEDIUM'
                            ? 'orange'
                            : 'green'
                      }
                    >
                      {workflowData.urgencyLevel || 'NORMAL'}
                    </Tag>
                  </div>
                </div>
                <div>
                  <Text strong style={{ fontSize: '11px' }}>
                    Business Impact:
                  </Text>
                  <div style={{ fontSize: '12px', marginTop: 2 }}>
                    {workflowData.businessImpact || 'Standard processing'}
                  </div>
                </div>
                <div>
                  <Text strong style={{ fontSize: '11px' }}>
                    Cost Center:
                  </Text>
                  <div style={{ fontSize: '12px', marginTop: 2 }}>
                    {workflowData.costCenter || 'Not specified'}
                  </div>
                </div>
              </Space>
            </Descriptions.Item>
          </Descriptions>

          {workflowData.jvcNotes && (
            <div style={{ marginTop: 12 }}>
              <Text strong style={{ fontSize: '12px' }}>
                JVC Notes:
              </Text>
              <div
                style={{
                  marginTop: 4,
                  padding: '8px',
                  backgroundColor: '#f6ffed',
                  border: '1px solid #b7eb8f',
                  borderRadius: '4px',
                  fontSize: '12px',
                  whiteSpace: 'pre-wrap'
                }}
              >
                {workflowData.jvcNotes}
              </div>
            </div>
          )}

          {/* Known Material Properties */}
          {workflowData.knownProperties && Object.keys(workflowData.knownProperties).length > 0 && (
            <div style={{ marginTop: 12 }}>
              <Text strong style={{ fontSize: '12px' }}>
                Known Properties:
              </Text>
              <div
                style={{
                  marginTop: 4,
                  padding: '8px',
                  backgroundColor: '#f0f5ff',
                  border: '1px solid #adc6ff',
                  borderRadius: '4px',
                  fontSize: '11px'
                }}
              >
                {Object.entries(workflowData.knownProperties).map(([key, value]) => (
                  <div key={key} style={{ marginBottom: 2 }}>
                    <strong>
                      {key.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase())}:
                    </strong>{' '}
                    {value}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Additional Context for Plant Team */}
          <Divider style={{ margin: '12px 0 8px 0' }} />
          <div style={{ fontSize: '11px', color: '#666' }}>
            <Text strong>For Plant Team Reference:</Text>
            <ul style={{ margin: '4px 0 0 16px', padding: 0 }}>
              <li>Review all JVC-provided information before starting questionnaire</li>
              <li>Use this context when raising queries to other teams</li>
              <li>Reference material ID and supplier details in communications</li>
              <li>Contact JVC team for clarification on any provided data</li>
              <li>Consider regulatory requirements when completing safety sections</li>
            </ul>
          </div>
        </Panel>

        {/* Material Specifications */}
        {workflowData.specifications && (
          <Panel
            header={
              <Space>
                <InfoCircleOutlined />
                <strong>Technical Specifications</strong>
              </Space>
            }
            key="specifications"
          >
            <Descriptions column={1} size="small">
              {Object.entries(workflowData.specifications).map(([key, value]) => (
                <Descriptions.Item
                  key={key}
                  label={key.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase())}
                >
                  {typeof value === 'object' ? JSON.stringify(value) : String(value)}
                </Descriptions.Item>
              ))}
            </Descriptions>
          </Panel>
        )}

        {/* Safety Notes */}
        {workflowData.safetyNotes && (
          <Panel
            header={
              <Space>
                <WarningOutlined />
                <strong>Safety Notes</strong>
              </Space>
            }
            key="safety"
          >
            <Alert
              message="Important Safety Information"
              description={workflowData.safetyNotes}
              type="warning"
              showIcon
              size="small"
            />
          </Panel>
        )}
      </Collapse>

      <Divider style={{ margin: '12px 0' }} />
{/* 
      Quick Actions
      <Space direction="vertical" style={{ width: '100%' }}>
        <Text strong style={{ fontSize: '12px' }}>
          Quick Actions:
        </Text>
        <Space wrap>
          <Button size="small" type="link" style={{ padding: '0 4px', height: 'auto' }}>
            View Full History
          </Button>
          <Button size="small" type="link" style={{ padding: '0 4px', height: 'auto' }}>
            Contact JVC
          </Button>
          <Button size="small" type="link" style={{ padding: '0 4px', height: 'auto' }}>
            Export Data
          </Button>
        </Space>
      </Space> */}
    </Card>
  );
};

export default MaterialContextPanel;
