import {
  EyeOutlined,
  FileTextOutlined,
  MessageOutlined,
  HistoryOutlined,
  LockOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined
} from '@ant-design/icons';
import {
  Card,
  Descriptions,
  Tag,
  Spin,
  Alert,
  Tabs,
  Table,
  Space,
  Button,
  Divider,
  Typography,
  Row,
  Col,
  Statistic
} from 'antd';
import React, { useState, useEffect } from 'react';

import { PAGINATION, WORKFLOW_STATES, QUERY_STATUS } from '../constants';
import { auditAPI } from '../services/auditAPI';

import AuditTimeline from './AuditTimeline';
import VersionHistory from './VersionHistory';

const { TabPane } = Tabs;
const { Title, Text } = Typography;

const ReadOnlyWorkflowView = ({ workflowId }) => {
  const [loading, setLoading] = useState(true);
  const [workflowData, setWorkflowData] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (workflowId) {
      loadReadOnlyWorkflowData();
    }
  }, [workflowId]);

  const loadReadOnlyWorkflowData = async () => {
    try {
      setLoading(true);
      const data = await auditAPI.getReadOnlyWorkflowView(workflowId);
      setWorkflowData(data);
    } catch (err) {
      setError('Failed to load workflow data');
      console.error('Read-only workflow error:', err);
    } finally {
      setLoading(false);
    }
  };

  const getStateColor = state => {
    switch (state) {
      case WORKFLOW_STATES.COMPLETED:
        return 'green';
      case 'JVC_PENDING':
        return 'blue';
      case 'PLANT_PENDING':
        return 'orange';
      case 'CQS_PENDING':
        return 'purple';
      case 'TECH_PENDING':
        return 'cyan';
      default:
        return 'default';
    }
  };

  const getStatusIcon = status => {
    switch (status) {
      case WORKFLOW_STATES.COMPLETED:
        return <CheckCircleOutlined />;
      case QUERY_STATUS.RESOLVED:
        return <CheckCircleOutlined />;
      case QUERY_STATUS.OPEN:
        return <ClockCircleOutlined />;
      default:
        return <ClockCircleOutlined />;
    }
  };

  const queryColumns = [
    {
      title: 'Question',
      dataIndex: 'question',
      key: 'question',
      ellipsis: true,
      width: '40%'
    },
    {
      title: 'Team',
      dataIndex: 'assignedTeam',
      key: 'assignedTeam',
      width: '15%',
      render: team => <Tag color="blue">{team}</Tag>
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: '15%',
      render: status => (
        <Tag
          color={status === QUERY_STATUS.RESOLVED ? 'green' : 'orange'}
          icon={getStatusIcon(status)}
        >
          {status}
        </Tag>
      )
    },
    {
      title: 'Raised By',
      dataIndex: 'raisedBy',
      key: 'raisedBy',
      width: '15%'
    },
    {
      title: 'Created',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: '15%',
      render: date => new Date(date).toLocaleDateString()
    }
  ];

  const responseColumns = [
    {
      title: 'Step',
      dataIndex: 'stepNumber',
      key: 'stepNumber',
      width: '10%',
      sorter: (a, b) => a.stepNumber - b.stepNumber
    },
    {
      title: 'Field',
      dataIndex: 'fieldName',
      key: 'fieldName',
      width: '25%'
    },
    {
      title: 'Value',
      dataIndex: 'fieldValue',
      key: 'fieldValue',
      width: '35%',
      ellipsis: true,
      render: value => value || <Text type="secondary">Not provided</Text>
    },
    {
      title: 'Type',
      dataIndex: 'fieldType',
      key: 'fieldType',
      width: '15%',
      render: type => <Tag>{type}</Tag>
    },
    {
      title: 'Modified',
      dataIndex: 'lastModified',
      key: 'lastModified',
      width: '15%',
      render: date => new Date(date).toLocaleDateString()
    }
  ];

  if (loading) {
    return (
      <Card>
        <div style={{ textAlign: 'center', padding: '40px' }}>
          <Spin size="large" />
          <div style={{ marginTop: '16px' }}>
            <Text>Loading workflow data...</Text>
          </div>
        </div>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <Alert
          message="Error"
          description={error}
          type="error"
          showIcon
          action={
            <Button size="small" onClick={loadReadOnlyWorkflowData}>
              Retry
            </Button>
          }
        />
      </Card>
    );
  }

  if (!workflowData) {
    return (
      <Card>
        <Alert message="No Data" description="Workflow data not found" type="warning" showIcon />
      </Card>
    );
  }

  return (
    <div style={{ padding: '24px' }}>
      {/* Header with read-only indicator */}
      <Card style={{ marginBottom: '16px' }}>
        <Row align="middle" justify="space-between">
          <Col>
            <Space>
              <LockOutlined style={{ color: '#faad14', fontSize: '20px' }} />
              <Title level={3} style={{ margin: 0 }}>
                Read-Only Workflow View
              </Title>
              <Tag color="gold">ARCHIVED</Tag>
            </Space>
          </Col>
          <Col>
            <Space>
              <Button type="primary" icon={<EyeOutlined />} onClick={() => window.print()}>
                Print View
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      {/* Workflow Overview */}
      <Card
        title={
          <Space>
            <FileTextOutlined />
            Workflow Overview
          </Space>
        }
        style={{ marginBottom: '16px' }}
      >
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12} md={8}>
            <Statistic
              title="Material Code"
              value={workflowData.materialCode}
              valueStyle={{ fontSize: '16px' }}
            />
            {workflowData.itemDescription && (
              <div style={{ fontSize: '12px', color: '#666', marginTop: '4px' }}>
                {workflowData.itemDescription}
              </div>
            )}
          </Col>
          <Col xs={24} sm={12} md={8}>
            <Statistic
              title="Current State"
              value={workflowData.state}
              valueRender={() => (
                <Tag
                  color={getStateColor(workflowData.state)}
                  style={{ fontSize: '14px', padding: '4px 8px' }}
                >
                  {workflowData.state}
                </Tag>
              )}
            />
          </Col>
        </Row>

        <Divider />

        <Descriptions column={{ xs: 1, sm: 2, md: 3 }} size="small">
          <Descriptions.Item label="Material Name">
            {workflowData.materialName || 'Not specified'}
          </Descriptions.Item>
          <Descriptions.Item label="Assigned Plant">
            {workflowData.assignedPlant || 'Not assigned'}
          </Descriptions.Item>
          <Descriptions.Item label="Initiated By">{workflowData.initiatedBy}</Descriptions.Item>
          <Descriptions.Item label="Created At">
            {new Date(workflowData.createdAt).toLocaleString()}
          </Descriptions.Item>
          <Descriptions.Item label="Last Modified">
            {new Date(workflowData.lastModified).toLocaleString()}
          </Descriptions.Item>
          {workflowData.completedAt && (
            <Descriptions.Item label="Completed At">
              {new Date(workflowData.completedAt).toLocaleString()}
            </Descriptions.Item>
          )}
        </Descriptions>

        {workflowData.materialDescription && (
          <>
            <Divider />
            <div>
              <Text strong>Material Description:</Text>
              <div
                style={{
                  marginTop: '8px',
                  padding: '12px',
                  backgroundColor: '#f5f5f5',
                  borderRadius: '4px'
                }}
              >
                <Text>{workflowData.materialDescription}</Text>
              </div>
            </div>
          </>
        )}
      </Card>

      {/* Tabbed Content */}
      <Card>
        <Tabs defaultActiveKey="queries" size="large">
          <TabPane
            tab={
              <Space>
                <MessageOutlined />
                Queries ({workflowData.queries?.length || 0})
              </Space>
            }
            key="queries"
          >
            <Table
              columns={queryColumns}
              dataSource={workflowData.queries}
              rowKey="id"
              size="small"
              pagination={{
                pageSize: PAGINATION.DEFAULT_PAGE_SIZE,
                showSizeChanger: true,
                showTotal: (total, range) => `${range[0]}-${range[1]} of ${total} queries`
              }}
              expandable={{
                expandedRowRender: record => (
                  <div style={{ padding: '16px', backgroundColor: '#fafafa' }}>
                    <Descriptions size="small" column={1}>
                      <Descriptions.Item label="Question">{record.question}</Descriptions.Item>
                      {record.response && (
                        <Descriptions.Item label="Response">{record.response}</Descriptions.Item>
                      )}
                      {record.resolvedBy && (
                        <Descriptions.Item label="Resolved By">
                          {record.resolvedBy}
                        </Descriptions.Item>
                      )}
                      {record.resolvedAt && (
                        <Descriptions.Item label="Resolved At">
                          {new Date(record.resolvedAt).toLocaleString()}
                        </Descriptions.Item>
                      )}
                    </Descriptions>
                  </div>
                ),
                rowExpandable: record => record.response || record.resolvedBy
              }}
            />
          </TabPane>

          <TabPane
            tab={
              <Space>
                <FileTextOutlined />
                Responses ({workflowData.responses?.length || 0})
              </Space>
            }
            key="responses"
          >
            <Table
              columns={responseColumns}
              dataSource={workflowData.responses}
              rowKey="id"
              size="small"
              pagination={{
                pageSize: PAGINATION.LARGE_PAGE_SIZE,
                showSizeChanger: true,
                showTotal: (total, range) => `${range[0]}-${range[1]} of ${total} responses`
              }}
              defaultSortOrder="ascend"
              sortDirections={['ascend', 'descend']}
            />
          </TabPane>

          <TabPane
            tab={
              <Space>
                <HistoryOutlined />
                Audit Trail
              </Space>
            }
            key="audit"
          >
            <AuditTimeline workflowId={workflowId} entityType="complete" />
          </TabPane>

          <TabPane
            tab={
              <Space>
                <HistoryOutlined />
                Version History
              </Space>
            }
            key="versions"
          >
            <VersionHistory workflowId={workflowId} entityType="workflow" />
          </TabPane>
        </Tabs>
      </Card>

      {/* Audit Summary */}
      {workflowData.auditSummary && (
        <Card title="Audit Summary" size="small" style={{ marginTop: '16px' }}>
          <Row gutter={16}>
            <Col span={8}>
              <Statistic title="Total Revisions" value={workflowData.auditSummary.totalRevisions} />
            </Col>
            <Col span={8}>
              <Statistic
                title="Last Modified By"
                value={workflowData.auditSummary.lastModifiedBy || 'Unknown'}
                valueStyle={{ fontSize: '14px' }}
              />
            </Col>
            <Col span={8}>
              <Statistic
                title="Last Modified At"
                value={
                  workflowData.auditSummary.lastModifiedAt
                    ? new Date(workflowData.auditSummary.lastModifiedAt).toLocaleDateString()
                    : 'Unknown'
                }
                valueStyle={{ fontSize: '14px' }}
              />
            </Col>
          </Row>
        </Card>
      )}
    </div>
  );
};

export default ReadOnlyWorkflowView;
