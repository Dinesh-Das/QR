import {
  DashboardOutlined,
  QuestionCircleOutlined,
  HistoryOutlined,
  ReloadOutlined,
  FileTextOutlined,
  InfoCircleOutlined,
  FilterOutlined
} from '@ant-design/icons';
import {
  Tabs,
  Card,
  Row,
  Col,
  Typography,
  Table,
  Tag,
  Button,
  message,
  Space,
  Badge,
  Modal,
  Descriptions,
  Select,
  DatePicker
} from 'antd';
import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';

import apiClient from '../api/client';
import AuditTimeline from '../components/AuditTimeline';
import QueryWidget from '../components/QueryWidget';
import { PAGINATION, WORKFLOW_STATES } from '../constants';
import { getCurrentUser, getUserRole } from '../services/auth';

import QuestionnaireViewerPage from './QuestionnaireViewerPage';

// Test auth imports removed - files not found

const { TabPane } = Tabs;
const { Title, Text } = Typography;
const { Option } = Select;
const { RangePicker } = DatePicker;

const WorkflowPage = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('dashboard');
  const [selectedWorkflowId, setSelectedWorkflowId] = useState(null);
  const [selectedWorkflow, setSelectedWorkflow] = useState(null);
  const [detailsModalVisible, setDetailsModalVisible] = useState(false);
  const [questionnaireModalVisible, setQuestionnaireModalVisible] = useState(false);
  const [questionnaireWorkflowId, setQuestionnaireWorkflowId] = useState(null);

  // All Workflows State (for dashboard)
  const [allWorkflows, setAllWorkflows] = useState([]);
  const [filteredWorkflows, setFilteredWorkflows] = useState([]);
  const [dashboardLoading, setDashboardLoading] = useState(false);

  // Date filtering state
  const [dateFilter, setDateFilter] = useState('current_month');
  const [customDateRange, setCustomDateRange] = useState(null);

  const currentUser = getCurrentUser();
  const userRole = getUserRole();

  // Debug logging
  console.log('WorkflowPage - currentUser:', currentUser);
  console.log('WorkflowPage - userRole:', userRole);

  // Apply date filtering to workflows
  const applyDateFilter = useCallback((workflows, filter, customRange) => {
    // Ensure workflows is always an array
    const safeWorkflows = Array.isArray(workflows) ? workflows : [];
    const now = new Date();
    let startDate;

    switch (filter) {
      case 'current_month':
        startDate = new Date(now.getFullYear(), now.getMonth(), 1);
        break;
      case 'past_3_months':
        startDate = new Date(now.getFullYear(), now.getMonth() - 3, 1);
        break;
      case 'past_6_months':
        startDate = new Date(now.getFullYear(), now.getMonth() - 6, 1);
        break;
      case 'past_12_months':
        startDate = new Date(now.getFullYear(), now.getMonth() - 12, 1);
        break;
      case 'custom':
        if (customRange && customRange.length === 2) {
          startDate = customRange[0].toDate();
          const endDate = customRange[1].toDate();
          const filtered = safeWorkflows.filter(w => {
            const workflowDate = new Date(w.createdAt);
            return workflowDate >= startDate && workflowDate <= endDate;
          });
          setFilteredWorkflows(filtered);
          return;
        }
        break;
      default:
        setFilteredWorkflows(safeWorkflows);
        return;
    }

    if (startDate) {
      const filtered = safeWorkflows.filter(w => {
        const workflowDate = new Date(w.createdAt);
        return workflowDate >= startDate;
      });
      setFilteredWorkflows(filtered);
    } else {
      setFilteredWorkflows(safeWorkflows);
    }
  }, []);

  // Fetch all workflows for dashboard
  const fetchAllWorkflows = useCallback(async signal => {
    setDashboardLoading(true);
    try {
      const response = await apiClient.get('/workflows', { signal });
      if (!signal?.aborted) {
        // Ensure response is always an array
        const safeResponse = Array.isArray(response) ? response : [];
        setAllWorkflows(safeResponse);
        applyDateFilter(safeResponse, dateFilter, customDateRange);
      }
    } catch (error) {
      if (!signal?.aborted) {
        console.error('Error fetching workflows:', error);
        message.error('Failed to fetch workflows');
        // Set empty arrays on error
        setAllWorkflows([]);
        setFilteredWorkflows([]);
      }
    } finally {
      if (!signal?.aborted) {
        setDashboardLoading(false);
      }
    }
  }, [dateFilter, customDateRange, applyDateFilter]);

  // Handle date filter change
  const handleDateFilterChange = (value) => {
    setDateFilter(value);
    if (value !== 'custom') {
      setCustomDateRange(null);
    }
    applyDateFilter(allWorkflows, value, customDateRange);
  };

  // Handle custom date range change
  const handleCustomDateRangeChange = (dates) => {
    setCustomDateRange(dates);
    if (dates && dates.length === 2) {
      applyDateFilter(allWorkflows, 'custom', dates);
    }
  };

  useEffect(() => {
    const controller = new AbortController();
    fetchAllWorkflows(controller.signal);

    return () => {
      controller.abort();
    };
  }, [fetchAllWorkflows]);

  // Apply date filter when dateFilter or customDateRange changes
  useEffect(() => {
    if (allWorkflows.length > 0) {
      applyDateFilter(allWorkflows, dateFilter, customDateRange);
    }
  }, [dateFilter, customDateRange, allWorkflows, applyDateFilter]);

  const handleTabChange = key => {
    setActiveTab(key);
  };

  const showWorkflowDetails = workflow => {
    setSelectedWorkflow(workflow);
    setDetailsModalVisible(true);
  };

  const openQuestionnaire = workflowId => {
    // Open questionnaire in modal overlay
    setQuestionnaireWorkflowId(workflowId);
    setQuestionnaireModalVisible(true);
  };



  const downloadExcelReport = async workflowId => {
    try {
      const response = await apiClient.get(`/workflows/${workflowId}/excel-report`, {
        responseType: 'blob'
      });

      // Create download link
      const url = window.URL.createObjectURL(new Blob([response]));
      const link = document.createElement('a');
      link.href = url;
      link.download = `QRMFG_Report_Workflow_${workflowId}.xlsx`;
      document.body.appendChild(link);
      link.click();

      // Cleanup
      window.URL.revokeObjectURL(url);
      document.body.removeChild(link);

      message.success('Excel report downloaded successfully');
    } catch (error) {
      message.error('Failed to download Excel report');
    }
  };

  const closeDetailsModal = () => {
    setDetailsModalVisible(false);
    setSelectedWorkflow(null);
  };

  // Dashboard Table Columns - Optimized for no horizontal scrolling
  const dashboardColumns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
      render: id => (
        <Text
          style={{
            fontFamily: 'monospace',
            fontSize: '13px',
            fontWeight: 600,
            color: '#495057',
            padding: '4px 8px',
            background: '#f8f9fa',
            borderRadius: '4px',
            border: '1px solid #e9ecef'
          }}
        >
          #{id}
        </Text>
      )
    },
    {
      title: 'Project',
      dataIndex: 'projectCode',
      key: 'projectCode',
      width: 80,
      render: text => <Text strong>{text}</Text>
    },
    {
      title: 'Material',
      dataIndex: 'materialCode',
      key: 'materialCode',
      width: 90,
      render: text => <Text code>{text}</Text>
    },
    {
      title: 'Description',
      dataIndex: 'materialName',
      key: 'materialName',
      width: 100,
      ellipsis: true,
      render: text => text || 'No description'
    },
    {
      title: 'Plant',
      dataIndex: 'assignedPlant',
      key: 'assignedPlant',
      width: 70,
      render: plant => <Tag color="green">{plant}</Tag>
    },
    {
      title: 'Status',
      dataIndex: 'state',
      key: 'state',
      width: 100,
      render: state => {
        const stateConfig = {
          JVC_PENDING: { color: 'orange', text: 'JVC' },
          PLANT_PENDING: { color: 'blue', text: 'Plant' },
          CQS_PENDING: { color: 'purple', text: 'CQS' },
          TECH_PENDING: { color: 'cyan', text: 'Tech' },
          [WORKFLOW_STATES.COMPLETED]: { color: 'green', text: 'Done' }
        };
        const config = stateConfig[state] || { color: 'default', text: state };
        return <Tag color={config.color}>{config.text}</Tag>;
      }
    },
    {
      title: 'Start Date',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 90,
      render: date => date ? new Date(date).toLocaleDateString('en-GB') : 'N/A'
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 180,
      render: (_, record) => (
        <Space size="small">
          <Button
            size="small"
            icon={<FileTextOutlined />}
            onClick={() => openQuestionnaire(record.id)}
            title="Open Questionnaire"
          >
            Quest.
          </Button>
          <Button
            size="small"
            type="default"
            style={{ backgroundColor: '#52c41a', borderColor: '#52c41a', color: 'white' }}
            onClick={() => downloadExcelReport(record.id)}
            title="Download Excel Report"
          >
            Excel
          </Button>
        </Space>
      )
    }
  ];

  // Calculate analytics for filtered workflows
  const calculateAnalytics = (workflows) => {
    const total = workflows.length;
    const completed = workflows.filter(w => w.state === WORKFLOW_STATES.COMPLETED).length;
    const jvcPending = workflows.filter(w => w.state === 'JVC_PENDING').length;
    const plantPending = workflows.filter(w => w.state === 'PLANT_PENDING').length;
    const cqsPending = workflows.filter(w => w.state === 'CQS_PENDING').length;
    const techPending = workflows.filter(w => w.state === 'TECH_PENDING').length;
    const overdue = workflows.filter(w => (w.daysPending || 0) > 7).length;

    return { total, completed, jvcPending, plantPending, cqsPending, techPending, overdue };
  };

  // Render Dashboard Tab
  const renderDashboardTab = () => {
    const analytics = calculateAnalytics(filteredWorkflows);

    return (
      <div>
        {/* Header Section with Date Filters */}
        <div style={{ marginBottom: 24 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            {/* <Title level={3} style={{ margin: 0, color: '#1a1a1a', fontSize: '20px', fontWeight: 600 }}>
              Dashboard
            </Title> */}
            <Button
              icon={<ReloadOutlined />}
              onClick={() => {
                const controller = new AbortController();
                fetchAllWorkflows(controller.signal);
              }}
              loading={dashboardLoading}
              type="default"
            >
              Refresh
            </Button>
          </div>

          {/* Date Filter Controls */}
          <div style={{
            padding: '16px 24px',
            background: '#f8f9fa',
            borderRadius: '8px',
            border: '1px solid #e9ecef',
            marginBottom: '24px'
          }}>
            <Space align="center">
              <FilterOutlined style={{ color: '#495057' }} />
              <Text style={{ color: '#495057', fontSize: '14px', fontWeight: 500 }}>Filter by:</Text>
              <Select
                value={dateFilter}
                onChange={handleDateFilterChange}
                size="middle"
                style={{ width: 160 }}
              >
                <Option value="current_month">Current Month</Option>
                <Option value="past_3_months">Past 3 Months</Option>
                <Option value="past_6_months">Past 6 Months</Option>
                <Option value="past_12_months">Past 12 Months</Option>
                <Option value="custom">Custom Range</Option>
              </Select>
              {dateFilter === 'custom' && (
                <RangePicker
                  value={customDateRange}
                  onChange={handleCustomDateRangeChange}
                  size="middle"
                  style={{ width: 260 }}
                />
              )}
            </Space>
          </div>
        </div>

        {/* Analytics Cards */}
        <div style={{ marginBottom: '32px' }}>
          <Row gutter={[20, 16]}>
            <Col xs={24} sm={12} md={8} lg={6} xl={4}>
              <Card
                style={{
                  borderRadius: '8px',
                  border: '1px solid #e8e9ea',
                  boxShadow: 'none',
                  background: '#ffffff'
                }}
                bodyStyle={{ padding: '24px 20px' }}
              >
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: '32px', fontWeight: 700, marginBottom: '8px', color: '#1a1a1a' }}>
                    {analytics.total}
                  </div>
                  <div style={{ fontSize: '14px', color: '#666', fontWeight: 500 }}>Total Projects</div>
                </div>
              </Card>
            </Col>
            <Col xs={24} sm={12} md={8} lg={6} xl={4}>
              <Card
                style={{
                  borderRadius: '8px',
                  border: '1px solid #d4edda',
                  boxShadow: 'none',
                  background: '#f8fff9'
                }}
                bodyStyle={{ padding: '24px 20px' }}
              >
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: '32px', fontWeight: 700, marginBottom: '8px', color: '#28a745' }}>
                    {analytics.completed}
                  </div>
                  <div style={{ fontSize: '14px', color: '#155724', fontWeight: 500 }}>Completed</div>
                </div>
              </Card>
            </Col>
            <Col xs={24} sm={12} md={8} lg={6} xl={4}>
              <Card
                style={{
                  borderRadius: '8px',
                  border: '1px solid #f5c6cb',
                  boxShadow: 'none',
                  background: '#fdf2f2'
                }}
                bodyStyle={{ padding: '24px 20px' }}
              >
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: '32px', fontWeight: 700, marginBottom: '8px', color: '#dc3545' }}>
                    {analytics.jvcPending}
                  </div>
                  <div style={{ fontSize: '14px', color: '#721c24', fontWeight: 500 }}>JVC Pending</div>
                </div>
              </Card>
            </Col>
            <Col xs={24} sm={12} md={8} lg={6} xl={4}>
              <Card
                style={{
                  borderRadius: '8px',
                  border: '1px solid #bee5eb',
                  boxShadow: 'none',
                  background: '#f1f9fa'
                }}
                bodyStyle={{ padding: '24px 20px' }}
              >
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: '32px', fontWeight: 700, marginBottom: '8px', color: '#17a2b8' }}>
                    {analytics.plantPending}
                  </div>
                  <div style={{ fontSize: '14px', color: '#0c5460', fontWeight: 500 }}>Plant Pending</div>
                </div>
              </Card>
            </Col>
            <Col xs={24} sm={12} md={8} lg={6} xl={4}>
              <Card
                style={{
                  borderRadius: '8px',
                  border: '1px solid #d1ecf1',
                  boxShadow: 'none',
                  background: '#f8f9fa'
                }}
                bodyStyle={{ padding: '24px 20px' }}
              >
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: '32px', fontWeight: 700, marginBottom: '8px', color: '#6c757d' }}>
                    {analytics.cqsPending}
                  </div>
                  <div style={{ fontSize: '14px', color: '#495057', fontWeight: 500 }}>CQS Pending</div>
                </div>
              </Card>
            </Col>
            <Col xs={24} sm={12} md={8} lg={6} xl={4}>
              <Card
                style={{
                  borderRadius: '8px',
                  border: '1px solid #ffeaa7',
                  boxShadow: 'none',
                  background: '#fffbf0'
                }}
                bodyStyle={{ padding: '24px 20px' }}
              >
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: '32px', fontWeight: 700, marginBottom: '8px', color: '#fd7e14' }}>
                    {analytics.overdue}
                  </div>
                  <div style={{ fontSize: '14px', color: '#8a4a00', fontWeight: 500 }}>Overdue</div>
                </div>
              </Card>
            </Col>
          </Row>
        </div>

        {/* Data Table */}
        <div style={{
          background: '#fff',
          border: '1px solid #e8e9ea',
          borderRadius: '8px',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.06)'
        }}>
          <div style={{
            padding: '20px 24px',
            borderBottom: '1px solid #e8e9ea',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <FileTextOutlined style={{ color: '#495057', fontSize: '16px' }} />
              <span style={{ fontSize: '16px', fontWeight: 600, color: '#1a1a1a' }}>
                Workflows
              </span>
              <span style={{ 
                fontSize: '14px', 
                color: '#6c757d',
                background: '#f8f9fa',
                padding: '2px 8px',
                borderRadius: '12px',
                fontWeight: 500
              }}>
                {filteredWorkflows.length}
              </span>
            </div>
          </div>
          <Table
            columns={dashboardColumns}
            dataSource={filteredWorkflows}
            rowKey="id"
            loading={dashboardLoading}
            pagination={{
              pageSize: PAGINATION.LARGE_PAGE_SIZE,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total, range) => `${range[0]}-${range[1]} of ${total} items`,
              style: { padding: '16px 24px', borderTop: '1px solid #e8e9ea' }
            }}
            size="middle"
            onRow={(record) => ({
              onClick: () => {
                setSelectedWorkflowId(record.id);
                showWorkflowDetails(record);
              },
              style: {
                cursor: 'pointer'
              }
            })}
          />
        </div>
      </div>
    );
  };

  return (
    <div style={{
      padding: '0',
      background: '#f5f5f5',
      minHeight: '100vh'
    }}>
      {/* Clean Header */}
      <div style={{
        background: '#fff',
        borderBottom: '1px solid #e8e9ea',
        padding: '24px 40px',
        marginBottom: '0'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <Title level={2} style={{ color: '#1a1a1a', margin: 0, fontWeight: 600, fontSize: '24px' }}>
              Workflow Management
            </Title>
          </div>
          <div style={{
            textAlign: 'right',
            padding: '12px 20px',
            background: '#f8f9fa',
            borderRadius: '8px',
            border: '1px solid #e9ecef'
          }}>
            <Text style={{ color: '#495057', fontSize: '14px', fontWeight: 500 }}>
              {currentUser} ({userRole})
            </Text>
          </div>
        </div>
      </div>

      {/* Tab Navigation */}
      <div style={{
        background: '#fff',
        borderBottom: '1px solid #e8e9ea',
        marginBottom: '24px'
      }}>
        <Tabs
          activeKey={activeTab}
          onChange={handleTabChange}
          size="large"
          style={{ margin: 0 }}
          tabBarStyle={{
            margin: 0,
            padding: '0 24px',
            background: 'transparent',
            borderBottom: 'none'
          }}
        >
          <TabPane
            tab={
              <span>
                <DashboardOutlined />
                Dashboard
              </span>
            }
            key="dashboard"
          >
            {renderDashboardTab()}
          </TabPane>

          <TabPane
            tab={
              <span>
                <QuestionCircleOutlined />
                Queries
              </span>
            }
            key="queries"
          >
            <Row gutter={[16, 16]}>
              <Col span={24}>
                <QueryWidget workflowId={selectedWorkflowId} userRole={userRole} />
              </Col>
            </Row>
          </TabPane>

          <TabPane
            tab={
              <span>
                <HistoryOutlined />
                Audit Trail
              </span>
            }
            key="audit"
          >
            <Row gutter={[16, 16]}>
              <Col span={24}>
                {selectedWorkflowId ? (
                  <AuditTimeline workflowId={selectedWorkflowId} entityType="complete" />
                ) : (
                  <Card>
                    <div
                      style={{
                        textAlign: 'center',
                        padding: '40px 20px',
                        color: '#999'
                      }}
                    >
                      <HistoryOutlined style={{ fontSize: '48px', marginBottom: '16px' }} />
                      <h3>Select a Workflow</h3>
                      <p>Choose a workflow from the dashboard to view its audit trail</p>
                    </div>
                  </Card>
                )}
              </Col>
            </Row>
          </TabPane>
        </Tabs>
      </div>

      {/* Workflow Details Modal */}
      <Modal
        title={
          <Space>
            <InfoCircleOutlined />
            Workflow Details
            {selectedWorkflow && (
              <Badge count={selectedWorkflow.id} style={{ backgroundColor: '#1890ff' }} />
            )}
          </Space>
        }
        open={detailsModalVisible}
        onCancel={closeDetailsModal}
        footer={[
          <Button key="close" onClick={closeDetailsModal}>
            Close
          </Button>,
          selectedWorkflow && (
            <Button
              key="questionnaire"
              type="primary"
              icon={<FileTextOutlined />}
              onClick={() => {
                openQuestionnaire(selectedWorkflow.id);
                closeDetailsModal();
              }}
            >
              Open Questionnaire
            </Button>
          ),
          selectedWorkflow && (
            <Button
              key="excel-report"
              type="default"
              style={{ backgroundColor: '#52c41a', borderColor: '#52c41a', color: 'white' }}
              onClick={() => {
                downloadExcelReport(selectedWorkflow.id);
                closeDetailsModal();
              }}
            >
              Download Excel Report
            </Button>
          )
        ]}
        width={800}
      >
        {selectedWorkflow && (
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="Workflow ID">{selectedWorkflow.id}</Descriptions.Item>
            <Descriptions.Item label="Status">
              <Tag color="blue">{selectedWorkflow.state}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Material Code">
              {selectedWorkflow.materialCode}
            </Descriptions.Item>
            <Descriptions.Item label="Material Name">
              {selectedWorkflow.materialName || 'Not available'}
            </Descriptions.Item>
            <Descriptions.Item label="Project Code">
              {selectedWorkflow.projectCode}
            </Descriptions.Item>
            <Descriptions.Item label="Assigned Plant">
              <Tag color="green">{selectedWorkflow.assignedPlant}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Days Pending">
              <Tag color={(selectedWorkflow.daysPending || 0) > 7 ? 'red' : 'green'}>
                {selectedWorkflow.daysPending || 0} days
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Initiated By">
              {selectedWorkflow.initiatedBy}
            </Descriptions.Item>
            <Descriptions.Item label="Created At">
              {selectedWorkflow.createdAt
                ? new Date(selectedWorkflow.createdAt).toLocaleString()
                : 'N/A'}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>

      {/* Questionnaire Modal */}
      <Modal
        title="QRMFG Questionnaire Viewer"
        open={questionnaireModalVisible}
        onCancel={() => setQuestionnaireModalVisible(false)}
        footer={null}
        width="90%"
        style={{ top: 20 }}
        bodyStyle={{ padding: 0, height: 'calc(100vh - 200px)', overflow: 'hidden' }}
      >
        {questionnaireWorkflowId && (
          <div style={{ height: '100%', overflow: 'auto' }}>
            <QuestionnaireViewerPage
              workflowId={questionnaireWorkflowId}
              onClose={() => setQuestionnaireModalVisible(false)}
            />
          </div>
        )}
      </Modal>
    </div>
  );
};

export default WorkflowPage;