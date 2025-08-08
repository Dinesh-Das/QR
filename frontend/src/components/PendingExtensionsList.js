import {
  SendOutlined,
  FilterOutlined,
  ReloadOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  FileTextOutlined,
  EyeOutlined,
  FolderOpenOutlined,
  PlusOutlined
} from '@ant-design/icons';
import {
  Table,
  Card,
  Button,
  Space,
  Tag,
  Typography,
  Row,
  Col,
  Input,
  Select,
  DatePicker,
  message,
  Modal,
  Descriptions,
  List,
  Divider,
  Tooltip,
  Badge,
  Alert
} from 'antd';
import React, { useState, useEffect, useMemo, useCallback } from 'react';

import { PAGINATION, FILE_SIZE } from '../constants';
import { documentAPI } from '../services/documentAPI';
import { workflowAPI } from '../services/workflowAPI';

import DocumentUploadSection from './DocumentUploadSection';

const { Text, Title } = Typography;
const { Option } = Select;
const { RangePicker } = DatePicker;
const { Search } = Input;

const PendingExtensionsList = ({ onExtendToPlant, refreshTrigger }) => {
  const [pendingWorkflows, setPendingWorkflows] = useState([]);
  const [filteredWorkflows, setFilteredWorkflows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedWorkflow, setSelectedWorkflow] = useState(null);
  const [detailsModalVisible, setDetailsModalVisible] = useState(false);
  const [workflowDocuments, setWorkflowDocuments] = useState([]);
  const [showUploadSection, setShowUploadSection] = useState(false);
  const [filters, setFilters] = useState({
    search: '',
    projectCode: '',
    plantCode: '',
    materialCode: '',
    dateRange: null,
    slaStatus: '',
    documentCount: '',
    initiatedBy: ''
  });

  // Helper function to calculate days pending
  const calculateDaysPending = useCallback((createdAt) => {
    const created = new Date(createdAt);
    const now = new Date();
    const diffTime = Math.abs(now - created);
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  }, []);

  // Enhanced statistics calculation
  const statistics = useMemo(() => {
    const total = pendingWorkflows.length;
    const overdue = pendingWorkflows.filter(w => calculateDaysPending(w.createdAt) > 3).length;
    const warning = pendingWorkflows.filter(w => {
      const days = calculateDaysPending(w.createdAt);
      return days >= 2 && days <= 3;
    }).length;
    const normal = total - overdue - warning;
    const avgDaysPending =
      total > 0
        ? pendingWorkflows.reduce((sum, w) => sum + calculateDaysPending(w.createdAt), 0) / total
        : 0;

    // Additional project/material context statistics
    const uniqueProjects = new Set(pendingWorkflows.map(w => w.projectCode)).size;
    const uniqueMaterials = new Set(pendingWorkflows.map(w => w.materialCode)).size;
    const uniquePlants = new Set(pendingWorkflows.map(w => w.plantCode)).size;
    const totalDocuments = pendingWorkflows.reduce((sum, w) => sum + (w.documentCount || 0), 0);

    return {
      total,
      overdue,
      warning,
      normal,
      avgDaysPending: Math.round(avgDaysPending * 10) / 10,
      withDocuments: pendingWorkflows.filter(w => w.documentCount > 0).length,
      withoutDocuments: pendingWorkflows.filter(w => !w.documentCount || w.documentCount === 0)
        .length,
      uniqueProjects,
      uniqueMaterials,
      uniquePlants,
      totalDocuments,
      avgDocumentsPerWorkflow: total > 0 ? Math.round((totalDocuments / total) * 10) / 10 : 0
    };
  }, [pendingWorkflows, calculateDaysPending]);

  const loadPendingWorkflows = async () => {
    try {
      setLoading(true);
      const workflows = await workflowAPI.getWorkflowsByState('JVC_PENDING');
      console.log('Loaded workflows:', workflows); // Debug log
      setPendingWorkflows(workflows || []);
    } catch (error) {
      console.error('Error loading pending workflows:', error);
      message.error('Failed to load pending workflows');
    } finally {
      setLoading(false);
    }
  };

  const applyFilters = useCallback(() => {
    let filtered = [...pendingWorkflows];

    // Search filter
    if (filters.search) {
      const searchLower = filters.search.toLowerCase();
      filtered = filtered.filter(
        workflow =>
          workflow.projectCode?.toLowerCase().includes(searchLower) ||
          workflow.materialCode?.toLowerCase().includes(searchLower) ||
          workflow.plantCode?.toLowerCase().includes(searchLower)
      );
    }

    // Project code filter
    if (filters.projectCode) {
      filtered = filtered.filter(workflow => workflow.projectCode === filters.projectCode);
    }

    // Plant code filter
    if (filters.plantCode) {
      filtered = filtered.filter(
        workflow =>
          workflow.plantCode === filters.plantCode || workflow.assignedPlant === filters.plantCode
      );
    }

    // Material code filter
    if (filters.materialCode) {
      filtered = filtered.filter(workflow => workflow.materialCode === filters.materialCode);
    }



    // Document count filter
    if (filters.documentCount) {
      if (filters.documentCount === 'with') {
        filtered = filtered.filter(workflow => workflow.documentCount > 0);
      } else if (filters.documentCount === 'without') {
        filtered = filtered.filter(
          workflow => !workflow.documentCount || workflow.documentCount === 0
        );
      }
    }

    // Initiated by filter
    if (filters.initiatedBy) {
      filtered = filtered.filter(workflow =>
        workflow.initiatedBy?.toLowerCase().includes(filters.initiatedBy.toLowerCase())
      );
    }

    // Date range filter
    if (filters.dateRange && filters.dateRange.length === 2) {
      const [startDate, endDate] = filters.dateRange;
      filtered = filtered.filter(workflow => {
        const createdDate = new Date(workflow.createdAt);
        return createdDate >= startDate.toDate() && createdDate <= endDate.toDate();
      });
    }

    // SLA status filter
    if (filters.slaStatus) {
      filtered = filtered.filter(workflow => {
        const daysPending = calculateDaysPending(workflow.createdAt);
        if (filters.slaStatus === 'overdue') {
          return daysPending > 3;
        }
        if (filters.slaStatus === 'warning') {
          return daysPending >= 2 && daysPending <= 3;
        }
        if (filters.slaStatus === 'normal') {
          return daysPending < 2;
        }
        return true;
      });
    }

    setFilteredWorkflows(filtered);
  }, [pendingWorkflows, filters, calculateDaysPending]);

  useEffect(() => {
    loadPendingWorkflows();
  }, [refreshTrigger]);

  useEffect(() => {
    applyFilters();
  }, [applyFilters]);

  const getSLAStatus = createdAt => {
    const days = calculateDaysPending(createdAt);
    if (days > 3) {
      return { status: 'overdue', color: 'red', text: 'Overdue' };
    }
    if (days >= 2) {
      return { status: 'warning', color: 'orange', text: 'Warning' };
    }
    return { status: 'normal', color: 'green', text: 'On Track' };
  };

  const handleViewDetails = async workflow => {
    try {
      setSelectedWorkflow(workflow);
      setDetailsModalVisible(true);

      // Load workflow documents
      const documents = await documentAPI.getWorkflowDocuments(workflow.id);
      setWorkflowDocuments(documents || []);
    } catch (error) {
      console.error('Error loading workflow details:', error);
      message.error('Failed to load workflow details');
    }
  };

  const handleExtendWorkflow = async workflow => {
    try {
      await onExtendToPlant(workflow);
      await loadPendingWorkflows(); // Refresh the list
    } catch (error) {
      console.error('Error extending workflow:', error);
    }
  };

  const downloadDocument = async documentItem => {
    try {
      console.log('Downloading document:', documentItem.id, documentItem.originalFileName);
      const blob = await documentAPI.downloadDocument(documentItem.id);

      if (!blob || blob.size === 0) {
        throw new Error('Downloaded file is empty or invalid');
      }

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = documentItem.originalFileName || `document_${documentItem.id}`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      message.success(`Downloaded ${documentItem.originalFileName}`);
    } catch (error) {
      console.error('Error downloading document:', error);
      message.error(`Failed to download document: ${error.message}`);
    }
  };

  const resetFilters = () => {
    setFilters({
      search: '',
      projectCode: '',
      plantCode: '',
      materialCode: '',
      dateRange: null,
      slaStatus: '',
      documentCount: '',
      initiatedBy: ''
    });
  };

  // Get unique values for filter dropdowns
  const uniqueProjectCodes = [...new Set(pendingWorkflows.map(w => w.projectCode))].filter(Boolean);
  const uniquePlantCodes = [
    ...new Set(pendingWorkflows.map(w => w.plantCode || w.assignedPlant))
  ].filter(Boolean);
  const uniqueMaterialCodes = [...new Set(pendingWorkflows.map(w => w.materialCode))].filter(
    Boolean
  );

  const uniqueInitiators = [...new Set(pendingWorkflows.map(w => w.initiatedBy))].filter(Boolean);

  const columns = [
    {
      title: 'Project Code',
      dataIndex: 'projectCode',
      key: 'projectCode',
      render: (text, record) => (
        <Space direction="vertical" size="small">
          <Text strong>{text || 'N/A'}</Text>
          <Text type="secondary" style={{ fontSize: '11px' }}>
            {record.projectDescription || 'Project Extension'}
          </Text>
        </Space>
      ),
      sorter: (a, b) => (a.projectCode || '').localeCompare(b.projectCode || ''),
      width: 140
    },
    {
      title: 'Material Code',
      dataIndex: 'materialCode',
      key: 'materialCode',
      render: (text, record) => (
        <Space direction="vertical" size="small">
          <Text code>{text || 'N/A'}</Text>
          <Text type="secondary" style={{ fontSize: '11px' }}>
            {record.itemDescription || record.materialDescription || 'Material Safety Extension'}
          </Text>
        </Space>
      ),
      sorter: (a, b) => (a.materialCode || '').localeCompare(b.materialCode || ''),
      width: 140
    },
    {
      title: 'Plant Code',
      key: 'plantCode',
      render: (_, record) => (
        <Text strong>{record.plantCode || record.assignedPlant || 'N/A'}</Text>
      ),
      sorter: (a, b) => {
        const aPlant = a.plantCode || a.assignedPlant || '';
        const bPlant = b.plantCode || b.assignedPlant || '';
        return aPlant.localeCompare(bPlant);
      },
      width: 120
    },
    {
      title: 'Documents',
      key: 'documentCount',
      render: (_, record) => (
        <Space>
          <Badge
            count={record.documentCount || 0}
            style={{ backgroundColor: record.documentCount > 0 ? '#52c41a' : '#ff4d4f' }}
          />
          <Text type={record.documentCount > 0 ? 'secondary' : 'warning'}>
            {record.documentCount > 0 ? 'files' : 'none'}
          </Text>
          {(!record.documentCount || record.documentCount === 0) && (
            <Tooltip title="No documents uploaded. Click 'View Details' to upload documents.">
              <ExclamationCircleOutlined style={{ color: '#fa8c16' }} />
            </Tooltip>
          )}
        </Space>
      ),
      sorter: (a, b) => (a.documentCount || 0) - (b.documentCount || 0),
      filters: [
        { text: 'With Documents', value: 'with' },
        { text: 'Without Documents', value: 'without' }
      ],
      onFilter: (value, record) => {
        if (value === 'with') {
          return record.documentCount > 0;
        }
        if (value === 'without') {
          return !record.documentCount || record.documentCount === 0;
        }
        return true;
      }
    },
    {
      title: 'Initiated By',
      dataIndex: 'initiatedBy',
      key: 'initiatedBy',
      render: text => <Text>{text || 'Unknown'}</Text>,
      sorter: (a, b) => (a.initiatedBy || '').localeCompare(b.initiatedBy || '')
    },
    {
      title: 'Created Date',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: date => (
        <Space direction="vertical" size="small">
          <Text>{new Date(date).toLocaleDateString()}</Text>
          <Text type="secondary" style={{ fontSize: '12px' }}>
            {new Date(date).toLocaleTimeString()}
          </Text>
        </Space>
      ),
      sorter: (a, b) => new Date(a.createdAt) - new Date(b.createdAt)
    },
    {
      title: 'Days Pending',
      key: 'daysPending',
      render: (_, record) => {
        const days = calculateDaysPending(record.createdAt);
        const sla = getSLAStatus(record.createdAt);
        return (
          <Space>
            <Badge count={days} style={{ backgroundColor: sla.color }} />
            <Text type={sla.status === 'overdue' ? 'danger' : 'secondary'}>
              {days} day{days !== 1 ? 's' : ''}
            </Text>
          </Space>
        );
      },
      sorter: (a, b) => calculateDaysPending(a.createdAt) - calculateDaysPending(b.createdAt)
    },
    {
      title: 'SLA Status',
      key: 'slaStatus',
      render: (_, record) => {
        const sla = getSLAStatus(record.createdAt);
        return (
          <Tag
            color={sla.color}
            icon={
              sla.status === 'overdue' ? <ExclamationCircleOutlined /> : <ClockCircleOutlined />
            }
          >
            {sla.text}
          </Tag>
        );
      },
      filters: [
        { text: 'On Track', value: 'normal' },
        { text: 'Warning', value: 'warning' },
        { text: 'Overdue', value: 'overdue' }
      ],
      onFilter: (value, record) => {
        const sla = getSLAStatus(record.createdAt);
        return sla.status === value;
      }
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Space>
          <Tooltip title="View Details">
            <Button
              type="text"
              icon={<EyeOutlined />}
              onClick={() => handleViewDetails(record)}
              size="small"
            />
          </Tooltip>
          <Button
            type="primary"
            icon={<SendOutlined />}
            onClick={() => handleExtendWorkflow(record)}
            size="small"
          >
            Send to Plant
          </Button>
        </Space>
      )
    }
  ];

  return (
    <>
      <Card
        title={
          <Space>
            <Title level={4} style={{ margin: 0 }}>
              Pending Extensions
            </Title>
            <Badge count={filteredWorkflows.length} style={{ backgroundColor: '#fa8c16' }} />
          </Space>
        }
        extra={
          <Button icon={<ReloadOutlined />} onClick={loadPendingWorkflows} loading={loading}>
            Refresh
          </Button>
        }
      >
        {/* Enhanced Statistics Dashboard */}
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={6}>
            <Card size="small">
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#1890ff' }}>
                  {statistics.total}
                </div>
                <div style={{ color: '#666' }}>Total Pending</div>
                <div style={{ fontSize: '12px', color: '#999', marginTop: '4px' }}>
                  Avg: {statistics.avgDaysPending} days
                </div>
              </div>
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#ff4d4f' }}>
                  {statistics.overdue}
                </div>
                <div style={{ color: '#666' }}>Overdue (&gt;3 days)</div>
                <div style={{ fontSize: '12px', color: '#999', marginTop: '4px' }}>
                  {statistics.total > 0
                    ? Math.round((statistics.overdue / statistics.total) * 100)
                    : 0}
                  % of total
                </div>
              </div>
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#fa8c16' }}>
                  {statistics.warning}
                </div>
                <div style={{ color: '#666' }}>Warning (2-3 days)</div>
                <div style={{ fontSize: '12px', color: '#999', marginTop: '4px' }}>
                  Need attention soon
                </div>
              </div>
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#52c41a' }}>
                  {statistics.normal}
                </div>
                <div style={{ color: '#666' }}>On Track (&lt;2 days)</div>
                <div style={{ fontSize: '12px', color: '#999', marginTop: '4px' }}>Within SLA</div>
              </div>
            </Card>
          </Col>
        </Row>

        {/* Enhanced Project/Material Context Statistics */}
        <Card
          title={
            <Space>
              <FolderOpenOutlined />
              <span>Project & Material Context Overview</span>
            </Space>
          }
          size="small"
          style={{ marginBottom: 16 }}
        >
          <Row gutter={16} style={{ marginBottom: 12 }}>
            <Col span={6}>
              <Card
                size="small"
                style={{ backgroundColor: '#f6ffed', border: '1px solid #b7eb8f' }}
              >
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: '20px', fontWeight: 'bold', color: '#389e0d' }}>
                    {statistics.uniqueProjects}
                  </div>
                  <div style={{ color: '#666', fontSize: '12px' }}>Unique Projects</div>
                  <div style={{ color: '#999', fontSize: '10px' }}>
                    Across all pending extensions
                  </div>
                </div>
              </Card>
            </Col>
            <Col span={6}>
              <Card
                size="small"
                style={{ backgroundColor: '#f6ffed', border: '1px solid #b7eb8f' }}
              >
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: '20px', fontWeight: 'bold', color: '#389e0d' }}>
                    {statistics.uniqueMaterials}
                  </div>
                  <div style={{ color: '#666', fontSize: '12px' }}>Unique Materials</div>
                  <div style={{ color: '#999', fontSize: '10px' }}>Material safety extensions</div>
                </div>
              </Card>
            </Col>
            <Col span={6}>
              <Card
                size="small"
                style={{ backgroundColor: '#f6ffed', border: '1px solid #b7eb8f' }}
              >
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: '20px', fontWeight: 'bold', color: '#389e0d' }}>
                    {statistics.uniquePlants}
                  </div>
                  <div style={{ color: '#666', fontSize: '12px' }}>Target Plants</div>
                  <div style={{ color: '#999', fontSize: '10px' }}>Plant locations involved</div>
                </div>
              </Card>
            </Col>
            <Col span={6}>
              <Card
                size="small"
                style={{ backgroundColor: '#f6ffed', border: '1px solid #b7eb8f' }}
              >
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: '20px', fontWeight: 'bold', color: '#389e0d' }}>
                    {statistics.totalDocuments}
                  </div>
                  <div style={{ color: '#666', fontSize: '12px' }}>Total Documents</div>
                  <div style={{ color: '#999', fontSize: '10px' }}>
                    Avg: {statistics.avgDocumentsPerWorkflow} per workflow
                  </div>
                </div>
              </Card>
            </Col>
          </Row>

          <Alert
            message="Enhanced Project/Material Context"
            description={`This view provides comprehensive filtering and context for ${statistics.total} pending extensions across ${statistics.uniqueProjects} projects, ${statistics.uniqueMaterials} materials, and ${statistics.uniquePlants} plant locations. Use the filters below to narrow down by specific project codes, material codes, plant locations, or document availability.`}
            type="info"
            showIcon
            size="small"
          />
        </Card>

        {/* Enhanced Filters with Project/Material Context */}
        <Card size="small" style={{ marginBottom: 16, backgroundColor: '#fafafa' }}>
          <Row gutter={16} align="middle">
            <Col span={6}>
              <Space>
                <FilterOutlined />
                <Text strong>Enhanced Filtering:</Text>
              </Space>
            </Col>
            <Col span={18}>
              <Text type="secondary">
                Filter by project codes, material codes, plant locations, SLA status,
                document availability, and creation dates for comprehensive workflow management
              </Text>
            </Col>
          </Row>
        </Card>

        <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
          <Col span={6}>
            <Search
              placeholder="Search by project, material, plant, or initiator"
              value={filters.search}
              onChange={e => setFilters(prev => ({ ...prev, search: e.target.value }))}
              allowClear
              size="large"
            />
          </Col>
          <Col span={4}>
            <Select
              placeholder="Project Code"
              value={filters.projectCode}
              onChange={value => setFilters(prev => ({ ...prev, projectCode: value }))}
              allowClear
              style={{ width: '100%' }}
              showSearch
              optionFilterProp="children"
            >
              {uniqueProjectCodes.map(code => (
                <Option key={code} value={code}>
                  {code}
                </Option>
              ))}
            </Select>
          </Col>
          <Col span={4}>
            <Select
              placeholder="Material Code"
              value={filters.materialCode}
              onChange={value => setFilters(prev => ({ ...prev, materialCode: value }))}
              allowClear
              style={{ width: '100%' }}
              showSearch
              optionFilterProp="children"
            >
              {uniqueMaterialCodes.map(code => (
                <Option key={code} value={code}>
                  {code}
                </Option>
              ))}
            </Select>
          </Col>
          <Col span={4}>
            <Select
              placeholder="Plant Code"
              value={filters.plantCode}
              onChange={value => setFilters(prev => ({ ...prev, plantCode: value }))}
              allowClear
              style={{ width: '100%' }}
            >
              {uniquePlantCodes.map(code => (
                <Option key={code} value={code}>
                  {code}
                </Option>
              ))}
            </Select>
          </Col>

          <Col span={6}>
            <Button
              icon={<FilterOutlined />}
              onClick={resetFilters}
              title="Reset All Filters"
              style={{ width: '100%' }}
            >
              Reset All Filters
            </Button>
          </Col>
        </Row>

        <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
          <Col span={4}>
            <Select
              placeholder="SLA Status"
              value={filters.slaStatus}
              onChange={value => setFilters(prev => ({ ...prev, slaStatus: value }))}
              allowClear
              style={{ width: '100%' }}
            >
              <Option value="normal">On Track</Option>
              <Option value="warning">Warning</Option>
              <Option value="overdue">Overdue</Option>
            </Select>
          </Col>
          <Col span={4}>
            <Select
              placeholder="Documents"
              value={filters.documentCount}
              onChange={value => setFilters(prev => ({ ...prev, documentCount: value }))}
              allowClear
              style={{ width: '100%' }}
            >
              <Option value="with">With Documents</Option>
              <Option value="without">Without Documents</Option>
            </Select>
          </Col>
          <Col span={4}>
            <Select
              placeholder="Initiated By"
              value={filters.initiatedBy}
              onChange={value => setFilters(prev => ({ ...prev, initiatedBy: value }))}
              allowClear
              style={{ width: '100%' }}
              showSearch
              optionFilterProp="children"
            >
              {uniqueInitiators.map(initiator => (
                <Option key={initiator} value={initiator}>
                  {initiator}
                </Option>
              ))}
            </Select>
          </Col>
          <Col span={6}>
            <RangePicker
              value={filters.dateRange}
              onChange={dates => setFilters(prev => ({ ...prev, dateRange: dates }))}
              style={{ width: '100%' }}
              placeholder={['Start Date', 'End Date']}
            />
          </Col>
          <Col span={6}>
            <Text type="secondary" style={{ lineHeight: '32px', textAlign: 'center' }}>
              Showing {filteredWorkflows.length} of {pendingWorkflows.length} extensions
            </Text>
          </Col>
        </Row>

        <Table
          dataSource={filteredWorkflows}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{
            pageSize: PAGINATION.DEFAULT_PAGE_SIZE,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => `${range[0]}-${range[1]} of ${total} items`
          }}
          locale={{
            emptyText: 'No pending extensions found'
          }}
          scroll={{ x: 1200 }}
        />
      </Card>

      {/* Workflow Details Modal */}
      <Modal
        title={`Workflow Details - ${selectedWorkflow?.projectCode} / ${selectedWorkflow?.materialCode}`}
        open={detailsModalVisible}
        onCancel={() => {
          setDetailsModalVisible(false);
          setSelectedWorkflow(null);
          setWorkflowDocuments([]);
          setShowUploadSection(false);
        }}
        footer={[
          <Button
            key="close"
            onClick={() => {
              setDetailsModalVisible(false);
              setShowUploadSection(false);
            }}
          >
            Close
          </Button>,
          workflowDocuments.length === 0 ? (
            <Button
              key="extend-warning"
              type="default"
              icon={<ExclamationCircleOutlined />}
              onClick={() => {
                Modal.confirm({
                  title: 'Extend Workflow Without Documents?',
                  content:
                    'This workflow has no documents attached. Are you sure you want to extend it to the plant? Consider uploading documents first.',
                  okText: 'Extend Anyway',
                  cancelText: 'Cancel',
                  onOk: () => {
                    handleExtendWorkflow(selectedWorkflow);
                    setDetailsModalVisible(false);
                    setShowUploadSection(false);
                  }
                });
              }}
            >
              Extend to Plant (No Docs)
            </Button>
          ) : (
            <Button
              key="extend"
              type="primary"
              icon={<SendOutlined />}
              onClick={() => {
                handleExtendWorkflow(selectedWorkflow);
                setDetailsModalVisible(false);
                setShowUploadSection(false);
              }}
            >
              Extend to Plant
            </Button>
          )
        ]}
        width={800}
      >
        {selectedWorkflow && (
          <Space direction="vertical" style={{ width: '100%' }}>
            <Descriptions bordered column={2}>
              <Descriptions.Item label="Project Code">
                {selectedWorkflow.projectCode}
              </Descriptions.Item>
              <Descriptions.Item label="Material Code">
                {selectedWorkflow.materialCode}
              </Descriptions.Item>
              <Descriptions.Item label="Plant Code">{selectedWorkflow.plantCode}</Descriptions.Item>
              <Descriptions.Item label="Created Date">
                {new Date(selectedWorkflow.createdAt).toLocaleString()}
              </Descriptions.Item>
              <Descriptions.Item label="Days Pending">
                <Space>
                  <Badge count={calculateDaysPending(selectedWorkflow.createdAt)} />
                  <Text>{calculateDaysPending(selectedWorkflow.createdAt)} days</Text>
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="SLA Status" span={2}>
                {(() => {
                  const sla = getSLAStatus(selectedWorkflow.createdAt);
                  return (
                    <Tag color={sla.color} icon={<ClockCircleOutlined />}>
                      {sla.text}
                    </Tag>
                  );
                })()}
              </Descriptions.Item>
            </Descriptions>

            <Divider />

            <Space direction="vertical" style={{ width: '100%' }}>
              <Row justify="space-between" align="middle">
                <Col>
                  <Title level={5} style={{ margin: 0 }}>
                    <FileTextOutlined /> Documents ({workflowDocuments.length})
                  </Title>
                </Col>
                <Col>
                  <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    size="small"
                    onClick={() => setShowUploadSection(!showUploadSection)}
                  >
                    {showUploadSection ? 'Hide Upload' : 'Add Documents'}
                  </Button>
                </Col>
              </Row>

              {workflowDocuments.length > 0 && (
                <List
                  dataSource={workflowDocuments}
                  renderItem={docItem => (
                    <List.Item
                      actions={[
                        <Button
                          key="download"
                          type="link"
                          icon={<EyeOutlined />}
                          onClick={() => downloadDocument(docItem)}
                          size="small"
                        >
                          Download
                        </Button>
                      ]}
                    >
                      <List.Item.Meta
                        title={
                          <Space>
                            <Text strong>{docItem.originalFileName}</Text>
                            <Tag color="blue">{docItem.fileType?.toUpperCase()}</Tag>
                            {docItem.isReused && <Tag color="green">Reused</Tag>}
                          </Space>
                        }
                        description={
                          <Space split={<Divider type="vertical" />}>
                            <Text type="secondary">
                              Size: {(docItem.fileSize / FILE_SIZE.BYTES_PER_MB).toFixed(2)} MB
                            </Text>
                            <Text type="secondary">
                              Uploaded: {new Date(docItem.uploadedAt).toLocaleDateString()}
                            </Text>
                            <Text type="secondary">By: {docItem.uploadedBy}</Text>
                          </Space>
                        }
                      />
                    </List.Item>
                  )}
                />
              )}

              {(workflowDocuments.length === 0 || showUploadSection) && (
                <DocumentUploadSection
                  workflowId={selectedWorkflow?.id}
                  projectCode={selectedWorkflow?.projectCode}
                  materialCode={selectedWorkflow?.materialCode}
                  onDocumentsUploaded={() => {
                    // Refresh documents list
                    handleViewDetails(selectedWorkflow);
                    // Refresh the main workflow list to update document count
                    loadPendingWorkflows();
                    // Hide upload section after successful upload
                    setShowUploadSection(false);
                  }}
                />
              )}
            </Space>
          </Space>
        )}
      </Modal>
    </>
  );
};

export default PendingExtensionsList;
