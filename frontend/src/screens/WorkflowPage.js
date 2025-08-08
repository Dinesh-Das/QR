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
import DebugInfo from '../components/DebugInfo';
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
      width: 50,
      render: id => <Badge count={id} style={{ backgroundColor: '#1890ff' }} />
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
        <div style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <Title level={3} style={{ margin: 0 }}>
              Workflow Analytics
              <Badge
                count={filteredWorkflows.length}
                style={{ backgroundColor: '#1890ff', marginLeft: 12 }}
              />
            </Title>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => {
                const controller = new AbortController();
                fetchAllWorkflows(controller.signal);
              }}
              loading={dashboardLoading}
            >
              Refresh
            </Button>
          </div>

          {/* Date Filter Controls */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <Space>
              <FilterOutlined />
              <Text strong>Filter by:</Text>
              <Select
                value={dateFilter}
                onChange={handleDateFilterChange}
                style={{ width: 150 }}
                size="small"
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
                  size="small"
                  style={{ width: 240 }}
                />
              )}
            </Space>
          </div>
        </div>

        {/* Analytics Cards */}
        <Row gutter={16} style={{ marginBottom: 24 }}>
          <Col span={3}>
            <Card size="small">
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '20px', fontWeight: 'bold', color: '#1890ff' }}>
                  {analytics.total}
                </div>
                <div style={{ color: '#666', fontSize: '12px' }}>Total Projects</div>
              </div>
            </Card>
          </Col>
          <Col span={3}>
            <Card size="small">
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '20px', fontWeight: 'bold', color: '#52c41a' }}>
                  {analytics.completed}
                </div>
                <div style={{ color: '#666', fontSize: '12px' }}>Completed</div>
              </div>
            </Card>
          </Col>
          <Col span={3}>
            <Card size="small">
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '20px', fontWeight: 'bold', color: '#faad14' }}>
                  {analytics.jvcPending}
                </div>
                <div style={{ color: '#666', fontSize: '12px' }}>JVC Pending</div>
              </div>
            </Card>
          </Col>
          <Col span={3}>
            <Card size="small">
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '20px', fontWeight: 'bold', color: '#1890ff' }}>
                  {analytics.plantPending}
                </div>
                <div style={{ color: '#666', fontSize: '12px' }}>Plant Pending</div>
              </div>
            </Card>
          </Col>
          <Col span={3}>
            <Card size="small">
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '20px', fontWeight: 'bold', color: '#722ed1' }}>
                  {analytics.cqsPending}
                </div>
                <div style={{ color: '#666', fontSize: '12px' }}>CQS Pending</div>
              </div>
            </Card>
          </Col>
          <Col span={3}>
            <Card size="small">
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '20px', fontWeight: 'bold', color: '#13c2c2' }}>
                  {analytics.techPending}
                </div>
                <div style={{ color: '#666', fontSize: '12px' }}>Tech Pending</div>
              </div>
            </Card>
          </Col>
          <Col span={3}>
            <Card size="small">
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '20px', fontWeight: 'bold', color: '#ff4d4f' }}>
                  {analytics.overdue}
                </div>
                <div style={{ color: '#666', fontSize: '12px' }}>Overdue</div>
              </div>
            </Card>
          </Col>
          <Col span={3}>
            {/* Empty column for spacing */}
          </Col>
        </Row>

        {/* Main Table */}
        <Card>
          <Table
            columns={dashboardColumns}
            dataSource={filteredWorkflows}
            rowKey="id"
            loading={dashboardLoading}
            pagination={{
              pageSize: PAGINATION.LARGE_PAGE_SIZE,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total, range) => `${range[0]}-${range[1]} of ${total} workflows`
            }}
            size="small"
            onRow={(record) => ({
              onClick: () => {
                setSelectedWorkflowId(record.id);
                showWorkflowDetails(record);
              },
              style: { cursor: 'pointer' }
            })}
          />
        </Card>
      </div>
    );
  };

  return (
    <div style={{ padding: '0' }}>
      <DebugInfo />
      <Card
        title="MSDS Workflow Management"
        style={{ marginBottom: 24 }}
        extra={
          <span style={{ fontSize: '14px', color: '#666' }}>
            Welcome, {currentUser} ({userRole})
          </span>
        }
      >
        <Tabs activeKey={activeTab} onChange={handleTabChange} type="card" size="large">
          <TabPane
            tab={
              <span>
                <DashboardOutlined />
                Dashboard
                {filteredWorkflows.length > 0 && (
                  <Badge
                    count={filteredWorkflows.length}
                    style={{ backgroundColor: '#1890ff', marginLeft: 8 }}
                  />
                )}
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
                {selectedWorkflowId && (
                  <Badge
                    count={selectedWorkflowId}
                    style={{ backgroundColor: '#52c41a', marginLeft: 8 }}
                  />
                )}
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
      </Card>

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