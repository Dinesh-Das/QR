import {
  MessageOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  FilterOutlined,
  ReloadOutlined,
  EyeOutlined,
  PaperClipOutlined
} from '@ant-design/icons';
import {
  Card,
  Table,
  Tag,
  Button,
  Space,
  Modal,
  Form,
  Input,
  Select,
  Tooltip,
  Alert,
  Row,
  Col,
  Statistic,
  Progress,
  Typography,
  Divider,
  message
} from 'antd';
import React, { useState, useEffect, useCallback } from 'react';

import apiClient from '../api/client';
import { PAGINATION, QUERY_STATUS, TEAM_NAMES, PRIORITY_LEVELS } from '../constants';

import { AsyncErrorBoundary, ComponentErrorBoundary } from './ErrorBoundaries';
import MaterialContextDisplay from './MaterialContextDisplay';
import QueryDocumentList from './QueryDocumentList';
import QueryDocumentUpload from './QueryDocumentUpload';
import QueryHistoryTracker from './QueryHistoryTracker';


const { TextArea } = Input;
const { Option } = Select;
const { Text, Title } = Typography;

const QueryInbox = ({ team }) => {
  const [loading, setLoading] = useState(false);
  const [queries, setQueries] = useState([]);
  const [filteredQueries, setFilteredQueries] = useState([]);
  const [selectedQuery, setSelectedQuery] = useState(null);
  const [resolveModalVisible, setResolveModalVisible] = useState(false);
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [resolveForm] = Form.useForm();
  const [responseDocuments, setResponseDocuments] = useState([]);
  const [responseId, setResponseId] = useState(null);
  const [queryDocuments, setQueryDocuments] = useState([]);
  const [filters, setFilters] = useState({
    status: 'all',
    priority: 'all',
    material: '',
    daysOpen: 'all',
    project: '',
    plant: '',
    assignedTeam: 'all',
    hasAttachments: 'all'
  });
  const [stats, setStats] = useState({
    total: 0,
    open: 0,
    resolved: 0,
    resolvedToday: 0,
    overdue: 0,
    avgResolutionTime: 0
  });

  // Define functions before useEffect hooks that depend on them
  const loadQueries = useCallback(async signal => {
    try {
      setLoading(true);
      const data = await apiClient.get(`/queries/inbox/${team}`, { signal });
      if (!signal?.aborted) {
        setQueries(data);
      }
    } catch (error) {
      if (!signal?.aborted) {
        console.error('Failed to load queries:', error);
        message.error('Failed to load queries');
      }
    } finally {
      setLoading(false);
    }
  }, [team]);

  const loadStats = useCallback(async signal => {
    try {
      const [openCount, resolvedCount, resolvedTodayCount, overdueCount, avgTime] =
        await Promise.all([
          apiClient.get(`/queries/stats/count-open/${team}`, { signal }).catch(() => 0),
          apiClient.get(`/queries/stats/count-resolved/${team}`, { signal }).catch(() => 0),
          apiClient.get(`/queries/stats/resolved-today/${team}`, { signal }).catch(() => 0),
          apiClient.get(`/queries/stats/overdue-count/${team}`, { signal }).catch(() => 0),
          apiClient.get(`/queries/stats/avg-resolution-time/${team}`, { signal }).catch(() => 0)
        ]);

      if (!signal?.aborted) {
        setStats({
          total: openCount + resolvedCount,
          open: openCount,
          resolved: resolvedCount,
          resolvedToday: resolvedTodayCount,
          overdue: overdueCount,
          avgResolutionTime: avgTime
        });
      }
    } catch (error) {
      if (!signal?.aborted) {
        console.error('Failed to load stats:', error);
      }
    }
  }, [team]);

  const applyFilters = useCallback(() => {
    let filtered = [...queries];

    if (filters.status !== 'all') {
      filtered = filtered.filter(q => q.status === filters.status);
    }

    if (filters.priority !== 'all') {
      filtered = filtered.filter(q => q.priorityLevel === filters.priority);
    }

    if (filters.material) {
      filtered = filtered.filter(
        q =>
          q.materialCode?.toLowerCase().includes(filters.material.toLowerCase()) ||
          q.materialName?.toLowerCase().includes(filters.material.toLowerCase())
      );
    }

    if (filters.project) {
      filtered = filtered.filter(q =>
        q.projectCode?.toLowerCase().includes(filters.project.toLowerCase())
      );
    }

    if (filters.plant) {
      filtered = filtered.filter(
        q =>
          q.plantCode?.toLowerCase().includes(filters.plant.toLowerCase())
      );
    }

    if (filters.assignedTeam !== 'all') {
      filtered = filtered.filter(q => q.assignedTeam === filters.assignedTeam);
    }

    if (filters.daysOpen !== 'all') {
      const threshold = parseInt(filters.daysOpen);
      filtered = filtered.filter(q => q.daysOpen >= threshold);
    }

    if (filters.hasAttachments !== 'all') {
      if (filters.hasAttachments === 'with') {
        filtered = filtered.filter(q => (q.documentCount || 0) > 0);
      } else if (filters.hasAttachments === 'without') {
        filtered = filtered.filter(q => (q.documentCount || 0) === 0);
      }
    }

    setFilteredQueries(filtered);
  }, [queries, filters]);

  useEffect(() => {
    const controller = new AbortController();

    const fetchData = async () => {
      try {
        await Promise.all([loadQueries(controller.signal), loadStats(controller.signal)]);
      } catch (error) {
        if (!controller.signal.aborted) {
          console.error('Error loading query inbox data:', error);
        }
      }
    };

    fetchData();

    return () => {
      controller.abort();
    };
  }, [team, loadQueries, loadStats]);

  useEffect(() => {
    applyFilters();
  }, [queries, filters, applyFilters]);

  const handleResolveQuery = async values => {
    try {
      const resolveData = {
        response: values.response,
        priorityLevel: values.priorityLevel
      };

      const resolvedQuery = await apiClient.put(`/queries/${selectedQuery.id}/resolve`, resolveData);
      
      // Set response ID for document uploads
      if (resolvedQuery.responseId) {
        setResponseId(resolvedQuery.responseId);
      }

      const documentMessage = responseDocuments.length > 0 
        ? ` with ${responseDocuments.length} document(s) attached`
        : '';
      message.success(`Query resolved successfully${documentMessage}`);
      
      setResolveModalVisible(false);
      resolveForm.resetFields();
      setSelectedQuery(null);
      setResponseDocuments([]);
      setResponseId(null);
      setQueryDocuments([]);
      loadQueries();
      loadStats();
    } catch (error) {
      console.error('Failed to resolve query:', error);
      message.error('Failed to resolve query');
    }
  };

  // Load query documents when a query is selected
  const loadQueryDocuments = useCallback(async (queryId) => {
    try {
      const documents = await apiClient.get(`/queries/${queryId}/documents`);
      setQueryDocuments(documents);
    } catch (error) {
      console.error('Failed to load query documents:', error);
      setQueryDocuments([]);
    }
  }, []);

  // Handle response document upload
  const handleResponseDocumentUpload = (documents) => {
    setResponseDocuments(prev => [...prev, ...documents]);
    message.success(`${documents.length} document(s) uploaded to response`);
  };

  const getStatusColor = status => {
    return status === QUERY_STATUS.OPEN ? 'red' : 'green';
  };

  const getPriorityColor = priority => {
    const colors = {
      LOW: 'blue',
      MEDIUM: 'orange',
      HIGH: 'red',
      URGENT: 'purple'
    };
    return colors[priority] || 'default';
  };

  const getDaysOpenColor = days => {
    if (days >= 3) {
      return '#ff4d4f';
    }
    if (days >= 2) {
      return '#faad14';
    }
    return '#52c41a';
  };

  const getSLAProgress = daysOpen => {
    const slaThreshold = 3; // 3 days SLA
    const progress = Math.min((daysOpen / slaThreshold) * 100, 100);
    let status = 'normal';
    if (progress >= 100) {
      status = 'exception';
    } else if (progress >= 80) {
      status = 'active';
    }

    return { progress, status };
  };

  const columns = [
    {
      title: 'Material Context',
      dataIndex: 'materialCode',
      key: 'materialContext',
      width: 180,
      render: (text, record) => (
        <div>
          <Text strong>{text}</Text>
          {record.materialName && (
            <div style={{ fontSize: '12px', color: '#666' }}>{record.materialName}</div>
          )}
          {record.projectCode && (
            <div style={{ fontSize: '11px', color: '#999' }}>Project: {record.projectCode}</div>
          )}
          {record.plantCode && (
            <div style={{ fontSize: '11px', color: '#999' }}>
              Plant: {record.plantCode}
            </div>
          )}
        </div>
      )
    },
    {
      title: 'Question',
      dataIndex: 'question',
      key: 'question',
      ellipsis: true,
      render: text => (
        <Tooltip title={text}>
          <Text>{text}</Text>
        </Tooltip>
      )
    },
    {
      title: 'Attachments',
      key: 'attachments',
      width: 100,
      render: (_, record) => {
        const documentCount = (record.documentCount || 0);
        return documentCount > 0 ? (
          <Space>
            <PaperClipOutlined style={{ color: '#1890ff' }} />
            <Text type="secondary">{documentCount}</Text>
          </Space>
        ) : (
          <Text type="secondary">-</Text>
        );
      }
    },
    {
      title: 'Field Context',
      dataIndex: 'fieldContext',
      key: 'fieldContext',
      width: 120,
      render: (text, record) => (
        <div>
          {text && <Tag color="blue">{text}</Tag>}
          {record.stepNumber && (
            <div style={{ fontSize: '12px', color: '#666' }}>Step {record.stepNumber}</div>
          )}
        </div>
      )
    },
    {
      title: 'Priority',
      dataIndex: 'priorityLevel',
      key: 'priorityLevel',
      width: 80,
      render: priority => (
        <Tag color={getPriorityColor(priority)}>{priority || PRIORITY_LEVELS.MEDIUM}</Tag>
      )
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: status => <Tag color={getStatusColor(status)}>{status}</Tag>
    },
    {
      title: 'Days Open',
      dataIndex: 'daysOpen',
      key: 'daysOpen',
      width: 100,
      render: (days, _record) => {
        const { progress, status } = getSLAProgress(days);
        return (
          <div>
            <Text style={{ color: getDaysOpenColor(days) }}>{days} days</Text>
            <Progress
              percent={progress}
              status={status}
              size="small"
              showInfo={false}
              style={{ marginTop: 4 }}
            />
          </div>
        );
      }
    },
    {
      title: 'Raised By',
      dataIndex: 'raisedBy',
      key: 'raisedBy',
      width: 100
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 120,
      render: (_, record) => (
        <Space>
          <Button
            size="small"
            icon={<EyeOutlined />}
            onClick={() => {
              setSelectedQuery(record);
              setDetailModalVisible(true);
              loadQueryDocuments(record.id);
            }}
          >
            View
          </Button>
          {record.status === QUERY_STATUS.OPEN && (
            <Button
              size="small"
              type="primary"
              icon={<CheckCircleOutlined />}
              onClick={() => {
                setSelectedQuery(record);
                setResolveModalVisible(true);
                loadQueryDocuments(record.id);
              }}
            >
              Resolve
            </Button>
          )}
        </Space>
      )
    }
  ];

  return (
    <ComponentErrorBoundary componentName="QueryInbox">
      <div>
        {/* Team-Specific Statistics */}
        <Alert
          message={`${team} Team Statistics`}
          description="All statistics below are specific to your team and update in real-time."
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
        />

        {/* Statistics Cards - Team Specific */}
        <AsyncErrorBoundary operation="query statistics loading" onRetry={loadStats} maxRetries={3}>
          <Row gutter={16} style={{ marginBottom: 16 }}>
            <Col span={5}>
              <Card>
                <Statistic title="Total Queries" value={stats.total} prefix={<MessageOutlined />} />
              </Card>
            </Col>
            <Col span={5}>
              <Card>
                <Statistic
                  title="Open Queries"
                  value={stats.open}
                  valueStyle={{ color: '#cf1322' }}
                  prefix={<ClockCircleOutlined />}
                />
              </Card>
            </Col>
            <Col span={5}>
              <Card>
                <Statistic
                  title="Resolved Today"
                  value={stats.resolvedToday}
                  valueStyle={{ color: '#3f8600' }}
                  prefix={<CheckCircleOutlined />}
                />
              </Card>
            </Col>
            <Col span={5}>
              <Card>
                <Statistic
                  title="Overdue"
                  value={stats.overdue}
                  valueStyle={{ color: stats.overdue > 0 ? '#cf1322' : '#3f8600' }}
                  prefix={<ExclamationCircleOutlined />}
                />
              </Card>
            </Col>
            <Col span={4}>
              <Card>
                <Statistic
                  title="Avg Resolution Time"
                  value={stats.avgResolutionTime}
                  suffix="hrs"
                  precision={1}
                  prefix={<CheckCircleOutlined />}
                />
              </Card>
            </Col>
          </Row>
        </AsyncErrorBoundary>

        {/* Enhanced Filters */}
        <Card size="small" style={{ marginBottom: 16 }}>
          <Row gutter={[16, 8]} align="middle">
            <Col span={4}>
              <Select
                placeholder="Status"
                value={filters.status}
                onChange={value => setFilters({ ...filters, status: value })}
                style={{ width: '100%' }}
              >
                <Option value="all">All Status</Option>
                <Option value="OPEN">Open</Option>
                <Option value="RESOLVED">Resolved</Option>
              </Select>
            </Col>
            <Col span={4}>
              <Select
                placeholder="Priority"
                value={filters.priority}
                onChange={value => setFilters({ ...filters, priority: value })}
                style={{ width: '100%' }}
              >
                <Option value="all">All Priority</Option>
                <Option value="LOW">Low</Option>
                <Option value="MEDIUM">Medium</Option>
                <Option value="HIGH">High</Option>
                <Option value="URGENT">Urgent</Option>
              </Select>
            </Col>
            <Col span={4}>
              <Input
                placeholder="Material ID/Name"
                value={filters.material}
                onChange={e => setFilters({ ...filters, material: e.target.value })}
              />
            </Col>
            <Col span={4}>
              <Input
                placeholder="Project Code"
                value={filters.project}
                onChange={e => setFilters({ ...filters, project: e.target.value })}
              />
            </Col>
            <Col span={4}>
              <Input
                placeholder="Plant"
                value={filters.plant}
                onChange={e => setFilters({ ...filters, plant: e.target.value })}
              />
            </Col>
            <Col span={4}>
              <Select
                placeholder="Team"
                value={filters.assignedTeam}
                onChange={value => setFilters({ ...filters, assignedTeam: value })}
                style={{ width: '100%' }}
              >
                <Option value="all">All Teams</Option>
                <Option value="CQS">CQS</Option>
                <Option value="TECH">Tech</Option>
                <Option value="JVC">JVC</Option>
              </Select>
            </Col>
            <Col span={4}>
              <Select
                placeholder="Attachments"
                value={filters.hasAttachments}
                onChange={value => setFilters({ ...filters, hasAttachments: value })}
                style={{ width: '100%' }}
              >
                <Option value="all">All Queries</Option>
                <Option value="with">With Attachments</Option>
                <Option value="without">Without Attachments</Option>
              </Select>
            </Col>
            <Col span={4}>
              <Select
                placeholder="Days Open"
                value={filters.daysOpen}
                onChange={value => setFilters({ ...filters, daysOpen: value })}
                style={{ width: '100%' }}
              >
                <Option value="all">All</Option>
                <Option value="1">1+ days</Option>
                <Option value="2">2+ days</Option>
                <Option value="3">3+ days (Overdue)</Option>
              </Select>
            </Col>
            <Col span={8}>
              <Space>
                <Button
                  icon={<ReloadOutlined />}
                  onClick={() => {
                    loadQueries();
                    loadStats();
                  }}
                >
                  Refresh
                </Button>
                <Button
                  icon={<FilterOutlined />}
                  onClick={() =>
                    setFilters({
                      status: 'all',
                      priority: 'all',
                      material: '',
                      daysOpen: 'all',
                      project: '',
                      plant: '',
                      assignedTeam: 'all',
                      hasAttachments: 'all'
                    })
                  }
                >
                  Clear Filters
                </Button>
              </Space>
            </Col>
          </Row>
        </Card>

        {/* Query Table */}
        <AsyncErrorBoundary operation="query data loading" onRetry={loadQueries} maxRetries={3}>
          <Card
            title={
              <div>
                <Title level={4} style={{ margin: 0 }}>
                  {team} Team Query Inbox
                </Title>
                <Text type="secondary">
                  {filteredQueries.length} of {queries.length} queries
                </Text>
              </div>
            }
          >
            {stats.overdue > 0 && (
              <Alert
                message={`${stats.overdue} queries are overdue (>3 days)`}
                type="warning"
                showIcon
                style={{ marginBottom: 16 }}
              />
            )}

            <Table
              dataSource={filteredQueries}
              columns={columns}
              loading={loading}
              pagination={{
                pageSize: PAGINATION.DEFAULT_PAGE_SIZE,
                showSizeChanger: true,
                showQuickJumper: true,
                showTotal: (total, range) => `${range[0]}-${range[1]} of ${total} queries`
              }}
              rowKey="id"
              size="small"
              rowClassName={record => (record.daysOpen >= 3 ? 'overdue-row' : '')}
            />
          </Card>
        </AsyncErrorBoundary>

        {/* Enhanced Query Detail Modal */}
        <Modal
          title={`Query #${selectedQuery?.id} Details`}
          open={detailModalVisible}
          onCancel={() => {
            setDetailModalVisible(false);
            setSelectedQuery(null);
            setQueryDocuments([]);
          }}
          footer={[
            <Button key="close" onClick={() => setDetailModalVisible(false)}>
              Close
            </Button>,
            selectedQuery?.status === QUERY_STATUS.OPEN && (
              <Button
                key="resolve"
                type="primary"
                onClick={() => {
                  setDetailModalVisible(false);
                  setResolveModalVisible(true);
                  loadQueryDocuments(selectedQuery.id);
                }}
              >
                Resolve Query
              </Button>
            )
          ]}
          width={1200}
        >
          {selectedQuery && (
            <Row gutter={16}>
              <Col span={14}>
                {/* Query Details */}
                <div>
                  <Row gutter={16}>
                    <Col span={12}>
                      <Text strong>Material Code:</Text> {selectedQuery.materialCode}
                      {selectedQuery.materialName && (
                        <div style={{ fontSize: '12px', color: '#666' }}>
                          {selectedQuery.materialName}
                        </div>
                      )}
                    </Col>
                    <Col span={12}>
                      <Text strong>Status:</Text>{' '}
                      <Tag color={getStatusColor(selectedQuery.status)}>{selectedQuery.status}</Tag>
                    </Col>
                  </Row>
                  <Divider />

                  <Row gutter={16}>
                    <Col span={8}>
                      <Text strong>Priority:</Text>{' '}
                      <Tag color={getPriorityColor(selectedQuery.priorityLevel)}>
                        {selectedQuery.priorityLevel || PRIORITY_LEVELS.MEDIUM}
                      </Tag>
                    </Col>
                    <Col span={8}>
                      <Text strong>Team:</Text>{' '}
                      <Tag
                        color={
                          selectedQuery.assignedTeam === TEAM_NAMES.CQS
                            ? 'blue'
                            : selectedQuery.assignedTeam === TEAM_NAMES.TECH
                              ? 'purple'
                              : 'orange'
                        }
                      >
                        {selectedQuery.assignedTeam}
                      </Tag>
                    </Col>
                    <Col span={8}>
                      <Text strong>Days Open:</Text>{' '}
                      <Text style={{ color: getDaysOpenColor(selectedQuery.daysOpen) }}>
                        {selectedQuery.daysOpen} days
                      </Text>
                    </Col>
                  </Row>
                  <Divider />

                  {/* Enhanced Context Information */}
                  {(selectedQuery.projectCode ||
                    selectedQuery.plantCode) && (
                      <>
                        <Row gutter={16}>
                          {selectedQuery.projectCode && (
                            <Col span={8}>
                              <Text strong>Project:</Text> {selectedQuery.projectCode}
                            </Col>
                          )}
                          {selectedQuery.plantCode && (
                            <Col span={8}>
                              <Text strong>Plant:</Text> {selectedQuery.plantCode}
                            </Col>
                          )}

                        </Row>
                        <Divider />
                      </>
                    )}

                  {selectedQuery.fieldName && (
                    <>
                      <Text strong>Field Context:</Text> {selectedQuery.fieldName}
                      {selectedQuery.stepNumber && ` (Step ${selectedQuery.stepNumber})`}
                      <Divider />
                    </>
                  )}

                  <Text strong>Question:</Text>
                  <div
                    style={{
                      marginTop: 8,
                      padding: 12,
                      background: '#f5f5f5',
                      borderRadius: 4,
                      whiteSpace: 'pre-wrap'
                    }}
                  >
                    {selectedQuery.question}
                  </div>

                  {selectedQuery.response && (
                    <>
                      <Divider />
                      <Text strong>Response:</Text>
                      <div
                        style={{
                          marginTop: 8,
                          padding: 12,
                          background: '#f0f9ff',
                          borderRadius: 4,
                          whiteSpace: 'pre-wrap'
                        }}
                      >
                        {selectedQuery.response}
                      </div>
                      <div style={{ marginTop: 8, fontSize: '12px', color: '#666' }}>
                        Resolved by: {selectedQuery.resolvedBy} on{' '}
                        {selectedQuery.resolvedAt &&
                          new Date(selectedQuery.resolvedAt).toLocaleString()}
                      </div>
                    </>
                  )}

                  {/* Query Documents */}
                  {queryDocuments.length > 0 && (
                    <>
                      <Divider />
                      <Text strong>
                        <Space>
                          <PaperClipOutlined />
                          Attached Documents ({queryDocuments.length})
                        </Space>
                      </Text>
                      <div style={{ marginTop: 8 }}>
                        <QueryDocumentList 
                          documents={queryDocuments}
                          showActions={true}
                          compact={false}
                        />
                      </div>
                    </>
                  )}

                  <Divider />
                  <div style={{ fontSize: '12px', color: '#666' }}>
                    <div>Raised by: {selectedQuery.raisedBy}</div>
                    <div>
                      Created:{' '}
                      {selectedQuery.createdAt &&
                        new Date(selectedQuery.createdAt).toLocaleString()}
                    </div>
                  </div>
                </div>
              </Col>
              <Col span={10}>
                {/* Material Context and Query History */}
                <ComponentErrorBoundary componentName="MaterialContextDisplay">
                  <MaterialContextDisplay
                    materialCode={selectedQuery.materialCode}
                    workflowId={selectedQuery.workflowId}
                    compact={true}
                  />
                </ComponentErrorBoundary>
                <ComponentErrorBoundary componentName="QueryHistoryTracker">
                  <QueryHistoryTracker
                    materialCode={selectedQuery.materialCode}
                    workflowId={selectedQuery.workflowId}
                    compact={true}
                  />
                </ComponentErrorBoundary>
              </Col>
            </Row>
          )}
        </Modal>

        {/* Enhanced Resolve Query Modal */}
        <Modal
          title={`Resolve Query #${selectedQuery?.id}`}
          open={resolveModalVisible}
          onCancel={() => {
            setResolveModalVisible(false);
            resolveForm.resetFields();
            setSelectedQuery(null);
            setResponseDocuments([]);
            setResponseId(null);
            setQueryDocuments([]);
          }}
          onOk={() => resolveForm.submit()}
          width={1400}
          okText="Resolve Query"
          okButtonProps={{
            type: 'primary',
            size: 'large'
          }}
          cancelButtonProps={{
            size: 'large'
          }}
        >
          {selectedQuery && (
            <Row gutter={16}>
              <Col span={16}>
                {/* Query Context */}
                <div
                  style={{
                    marginBottom: 16,
                    padding: 16,
                    background: '#f5f5f5',
                    borderRadius: 6,
                    border: '1px solid #d9d9d9'
                  }}
                >
                  <Row gutter={16} style={{ marginBottom: 12 }}>
                    <Col span={12}>
                      <Text strong>Material:</Text> {selectedQuery.materialCode}
                      {selectedQuery.materialName && (
                        <div style={{ fontSize: '12px', color: '#666' }}>
                          {selectedQuery.materialName}
                        </div>
                      )}
                    </Col>
                    <Col span={12}>
                      <Space>
                        <Text strong>Team:</Text>
                        <Tag
                          color={
                            selectedQuery.assignedTeam === TEAM_NAMES.CQS
                              ? 'blue'
                              : selectedQuery.assignedTeam === TEAM_NAMES.TECH
                                ? 'purple'
                                : 'orange'
                          }
                        >
                          {selectedQuery.assignedTeam}
                        </Tag>
                        <Text strong>Priority:</Text>
                        <Tag color={getPriorityColor(selectedQuery.priorityLevel)}>
                          {selectedQuery.priorityLevel || 'MEDIUM'}
                        </Tag>
                      </Space>
                    </Col>
                  </Row>

                  {(selectedQuery.projectCode ||
                    selectedQuery.plantCode) && (
                      <Row gutter={16} style={{ marginBottom: 12 }}>
                        {selectedQuery.projectCode && (
                          <Col span={8}>
                            <Text strong>Project:</Text> {selectedQuery.projectCode}
                          </Col>
                        )}
                        {selectedQuery.plantCode && (
                          <Col span={8}>
                            <Text strong>Plant:</Text> {selectedQuery.plantCode}
                          </Col>
                        )}

                      </Row>
                    )}

                  <Text strong>Question:</Text>
                  <div style={{ marginTop: 8, whiteSpace: 'pre-wrap', fontSize: '14px' }}>
                    {selectedQuery.question}
                  </div>

                  {selectedQuery.fieldName && (
                    <div style={{ marginTop: 8 }}>
                      <Text strong>Field Context:</Text> {selectedQuery.fieldName}
                      {selectedQuery.stepNumber && ` (Step ${selectedQuery.stepNumber})`}
                    </div>
                  )}

                  <div style={{ marginTop: 8, fontSize: '12px', color: '#666' }}>
                    <Space>
                      <span>Raised by: {selectedQuery.raisedBy}</span>
                      <span>•</span>
                      <span>
                        {selectedQuery.createdAt &&
                          new Date(selectedQuery.createdAt).toLocaleString()}
                      </span>
                      <span>•</span>
                      <span style={{ color: getDaysOpenColor(selectedQuery.daysOpen) }}>
                        {selectedQuery.daysOpen} days open
                      </span>
                    </Space>
                  </div>
                </div>

                {/* Resolution Form */}
                <Form form={resolveForm} layout="vertical" onFinish={handleResolveQuery}>
                  <Form.Item
                    name="response"
                    label={
                      <Space>
                        <span>Resolution Response</span>
                        <Text type="secondary">(Required)</Text>
                      </Space>
                    }
                    rules={[{ required: true, message: 'Please provide a resolution response' }]}
                  >
                    <TextArea
                      rows={8}
                      placeholder="Provide detailed resolution or answer to the query. Include any relevant technical details, safety considerations, or references to documentation..."
                      showCount
                      maxLength={2000}
                      style={{
                        direction: 'ltr',
                        textAlign: 'left',
                        fontFamily: 'inherit'
                      }}
                    />
                  </Form.Item>

                  {/* Existing Query Documents */}
                  {queryDocuments.length > 0 && (
                    <Form.Item label={
                      <Space>
                        <PaperClipOutlined />
                        <span>Query Documents</span>
                        <Text type="secondary">(For context while responding)</Text>
                      </Space>
                    }>
                      <QueryDocumentList 
                        documents={queryDocuments}
                        showActions={false}
                        compact={true}
                      />
                    </Form.Item>
                  )}

                  {/* Response Document Upload */}
                  <Form.Item label={
                    <Space>
                      <PaperClipOutlined />
                      <span>Attach Documents to Response</span>
                      <Text type="secondary">(Optional)</Text>
                    </Space>
                  }>
                    <QueryDocumentUpload
                      queryId={selectedQuery?.id}
                      responseId={responseId}
                      context="response"
                      onUploadComplete={handleResponseDocumentUpload}
                      maxFiles={5}
                      disabled={false}
                    />
                  </Form.Item>

                  <Row gutter={16}>
                    <Col span={12}>
                      <Form.Item
                        name="priorityLevel"
                        label="Update Priority (Optional)"
                        initialValue={selectedQuery.priorityLevel || 'MEDIUM'}
                      >
                        <Select size="large">
                          <Option value="LOW">Low Priority</Option>
                          <Option value="MEDIUM">Medium Priority</Option>
                          <Option value="HIGH">High Priority</Option>
                          <Option value="URGENT">Urgent Priority</Option>
                        </Select>
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <div style={{ marginTop: 30 }}>
                        <Text type="secondary">
                          This resolution will be sent to the plant team and the workflow will
                          continue.
                        </Text>
                      </div>
                    </Col>
                  </Row>
                </Form>
              </Col>
              <Col span={8}>
                {/* Context and History */}
                <ComponentErrorBoundary componentName="MaterialContextDisplay">
                  <MaterialContextDisplay
                    materialCode={selectedQuery.materialCode}
                    workflowId={selectedQuery.workflowId}
                    compact={true}
                  />
                </ComponentErrorBoundary>
                <ComponentErrorBoundary componentName="QueryHistoryTracker">
                  <QueryHistoryTracker
                    materialCode={selectedQuery.materialCode}
                    workflowId={selectedQuery.workflowId}
                    compact={true}
                  />
                </ComponentErrorBoundary>
              </Col>
            </Row>
          )}
        </Modal>

        <style>{`
          .overdue-row {
            background-color: #fff2f0;
          }
          .overdue-row:hover {
            background-color: #ffebe6 !important;
          }
        `}</style>
      </div>
    </ComponentErrorBoundary>
  );
};

export default QueryInbox;
