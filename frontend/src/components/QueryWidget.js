import { PlusOutlined, FilterOutlined, ReloadOutlined, CheckCircleOutlined, ClockCircleOutlined, UserOutlined, PaperClipOutlined } from '@ant-design/icons';
import { Card, Tabs, Tag, Button, Space, Modal, Form, Input, Select, Badge, Row, Col, Typography, Divider, Empty, List } from 'antd';
import React, { useState, useEffect, useCallback } from 'react';

import { UI_CONFIG, QUERY_STATUS, QUERY_STATUS_GROUPS, TEAM_NAMES } from '../constants';
import { queryAPI } from '../services/queryAPI';

// Hook to detect screen size
const useResponsive = () => {
  const [screenSize, setScreenSize] = useState({
    isMobile: window.innerWidth <= UI_CONFIG.MOBILE_BREAKPOINT,
    isTablet:
      window.innerWidth > UI_CONFIG.MOBILE_BREAKPOINT &&
      window.innerWidth <= UI_CONFIG.TABLET_BREAKPOINT,
    isDesktop: window.innerWidth > UI_CONFIG.TABLET_BREAKPOINT
  });

  useEffect(() => {
    const handleResize = () => {
      setScreenSize({
        isMobile: window.innerWidth <= UI_CONFIG.MOBILE_BREAKPOINT,
        isTablet:
          window.innerWidth > UI_CONFIG.MOBILE_BREAKPOINT &&
          window.innerWidth <= UI_CONFIG.TABLET_BREAKPOINT,
        isDesktop: window.innerWidth > UI_CONFIG.TABLET_BREAKPOINT
      });
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  return screenSize;
};

const { TabPane } = Tabs;
const { TextArea } = Input;
const { Option } = Select;
const { Text } = Typography;

const QueryWidget = ({ workflowId, userRole }) => {
  const [loading, setLoading] = useState(false);
  const [queries, setQueries] = useState({
    all: [],
    open: [],
    resolved: [],
    myQueries: []
  });
  const [filteredQueries, setFilteredQueries] = useState({
    all: [],
    open: [],
    resolved: [],
    myQueries: []
  });
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [resolveModalVisible, setResolveModalVisible] = useState(false);
  const [selectedQuery, setSelectedQuery] = useState(null);
  const [form] = Form.useForm();
  const [resolveForm] = Form.useForm();
  const { isMobile } = useResponsive();
  
  // Filter states
  const [filters, setFilters] = useState({
    team: 'all',
    status: 'all',
    priority: 'all',
    dateRange: 'all',
    materialCode: '',
    projectCode: '',
    plantCode: ''
  });

  useEffect(() => {
    // Always load all queries, not just for a specific workflow
    const controller = new AbortController();

    const loadQueriesWithAbort = async () => {
      try {
        setLoading(true);
        // Use the general queries endpoint to get all queries from all time
        const allQueries = await queryAPI.getAllQueries({
          signal: controller.signal
        });

        if (!controller.signal.aborted) {
          const queriesData = {
            all: allQueries,
            open: allQueries.filter(q => QUERY_STATUS_GROUPS.ACTIVE.includes(q.status)),
            resolved: allQueries.filter(q => QUERY_STATUS_GROUPS.INACTIVE.includes(q.status)),
            myQueries: allQueries.filter(q => q.createdBy === getCurrentUser())
          };
          setQueries(queriesData);
          setFilteredQueries(queriesData);
        }
      } catch (error) {
        if (!controller.signal.aborted) {
          console.error('Failed to load queries:', error);
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    };

    loadQueriesWithAbort();

    return () => {
      controller.abort();
    };
  }, []); // Remove workflowId dependency to load all queries

  const loadQueries = useCallback(
    async signal => {
      try {
        setLoading(true);
        // Load all queries from all time
        const allQueries = await queryAPI.getAllQueries({ signal });

        if (!signal?.aborted) {
          const queriesData = {
            all: allQueries,
            open: allQueries.filter(q => QUERY_STATUS_GROUPS.ACTIVE.includes(q.status)),
            resolved: allQueries.filter(q => QUERY_STATUS_GROUPS.INACTIVE.includes(q.status)),
            myQueries: allQueries.filter(q => q.createdBy === getCurrentUser())
          };
          setQueries(queriesData);
          setFilteredQueries(queriesData);
        }
      } catch (error) {
        if (!signal?.aborted) {
          console.error('Failed to load queries:', error);
        }
      } finally {
        if (!signal?.aborted) {
          setLoading(false);
        }
      }
    },
    [] // Remove workflowId dependency
  );

  const getCurrentUser = () => {
    // Get current user from auth context or localStorage
    return localStorage.getItem('username') || 'current_user';
  };

  const getStatusColor = status => {
    const colors = {
      OPEN: 'red',
      RESOLVED: 'green',
      CLOSED: 'gray'
    };
    return colors[status] || 'default';
  };

  const getTeamColor = team => {
    const colors = {
      CQS: 'blue',
      TECH: 'purple',
      JVC: 'orange',
      PLANT: 'green'
    };
    return colors[team] || 'default';
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
    if (days >= 7) return '#ff4d4f';
    if (days >= 3) return '#faad14';
    return '#52c41a';
  };

  const renderQueryCard = (query) => {
    const isResolved = query.status === 'RESOLVED';
    
    return (
      <Card 
        key={query.id}
        size="small" 
        style={{ 
          marginBottom: 16, 
          border: '1px solid #f0f0f0', 
          borderRadius: '8px',
          boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
        }}
      >
        <div>
          {/* Header with Query ID and Status */}
          <div style={{ marginBottom: 12 }}>
            <Space wrap>
              <Text strong style={{ fontSize: '16px' }}>Query #{query.id}</Text>
              <Tag color={getStatusColor(query.status)}>{query.status}</Tag>
              <Tag color={getTeamColor(query.assignedTeam)}>{query.assignedTeam}</Tag>
              {query.priorityLevel && (
                <Tag color={getPriorityColor(query.priorityLevel)}>{query.priorityLevel}</Tag>
              )}
            </Space>
          </div>

          {/* Material and Project Context */}
          <div style={{ marginBottom: 12, padding: '8px', background: '#fafafa', borderRadius: '4px' }}>
            <Row gutter={16}>
              {query.materialCode && (
                <Col span={8}>
                  <Text strong style={{ color: '#1890ff' }}>Material:</Text>
                  <div style={{ fontSize: '14px' }}>{query.materialCode}</div>
                  {query.materialName && (
                    <div style={{ fontSize: '12px', color: '#666' }}>{query.materialName}</div>
                  )}
                </Col>
              )}
              {query.projectCode && (
                <Col span={8}>
                  <Text strong style={{ color: '#1890ff' }}>Project:</Text>
                  <div style={{ fontSize: '14px' }}>{query.projectCode}</div>
                </Col>
              )}
              {query.assignedPlant && (
                <Col span={8}>
                  <Text strong style={{ color: '#1890ff' }}>Plant:</Text>
                  <div style={{ fontSize: '14px' }}>{query.assignedPlant}</div>
                </Col>
              )}
            </Row>
          </div>

          {/* Original Question Section */}
          {query.originalQuestion && (
            <div style={{ marginBottom: 12, padding: '8px', background: '#f6ffed', borderRadius: '4px', border: '1px solid #b7eb8f' }}>
              <Text strong style={{ color: '#52c41a' }}>Original Questionnaire Question:</Text>
              <div style={{ fontSize: '14px', fontStyle: 'italic', marginTop: '4px' }}>
                {query.originalQuestion}
              </div>
            </div>
          )}

          {/* Query Text (What was asked) */}
          <div style={{ marginBottom: 12, padding: '8px', background: '#fff7e6', borderRadius: '4px', border: '1px solid #ffd591' }}>
            <Text strong style={{ color: '#fa8c16' }}>Query Text (What was asked):</Text>
            <div style={{ fontSize: '14px', marginTop: '4px', whiteSpace: 'pre-wrap' }}>
              {query.question || 'No query text available'}
            </div>
          </div>

          {/* Query Metadata */}
          <div style={{ fontSize: '12px', color: '#666', marginBottom: 12 }}>
            <Row gutter={16}>
              <Col span={8}>
                <UserOutlined /> <strong>Raised by:</strong> {query.raisedBy}
              </Col>
              <Col span={8}>
                <ClockCircleOutlined /> <strong>Created:</strong> {query.createdAt && new Date(query.createdAt).toLocaleString()}
              </Col>
              <Col span={8}>
                <strong>Days open:</strong> <span style={{ color: getDaysOpenColor(query.daysOpen || 0) }}>{query.daysOpen || 0}</span>
              </Col>
            </Row>
          </div>

          {/* Resolution Section */}
          {query.response && (
            <div style={{ marginTop: 12, padding: '12px', background: '#f0f9ff', borderRadius: '6px', borderLeft: '4px solid #1890ff' }}>
              <div style={{ marginBottom: 8 }}>
                <Text strong style={{ fontSize: '15px', color: '#1890ff' }}>Resolution:</Text>
              </div>
              <div style={{ marginBottom: 8, fontSize: '14px', whiteSpace: 'pre-wrap' }}>
                {query.response}
              </div>
              <div style={{ fontSize: '12px', color: '#666' }}>
                <Row gutter={16}>
                  <Col span={12}>
                    <UserOutlined /> <strong>Resolved by:</strong> {query.resolvedBy}
                  </Col>
                  <Col span={12}>
                    <ClockCircleOutlined /> <strong>Resolved on:</strong> {query.resolvedAt && new Date(query.resolvedAt).toLocaleString()}
                  </Col>
                </Row>
              </div>
            </div>
          )}

          {/* Actions */}
          <div style={{ marginTop: 12, textAlign: 'right' }}>
            <Space>
              {query.status === QUERY_STATUS.OPEN && canResolveQuery(query) && (
                <Button
                  size="small"
                  type="primary"
                  icon={<CheckCircleOutlined />}
                  onClick={() => {
                    setSelectedQuery(query);
                    setResolveModalVisible(true);
                  }}
                >
                  Resolve
                </Button>
              )}
              {QUERY_STATUS_GROUPS.INACTIVE.includes(query.status) && (
                <Button
                  size="small"
                  type="link"
                  onClick={() => {
                    setSelectedQuery(query);
                    setResolveModalVisible(true);
                  }}
                >
                  View Details
                </Button>
              )}
            </Space>
          </div>

          {/* Query Category if available */}
          {query.queryCategory && (
            <div style={{ marginTop: 8 }}>
              <Text strong style={{ fontSize: '12px', color: '#722ed1' }}>Category: </Text>
              <Tag color="purple">{query.queryCategory}</Tag>
            </div>
          )}
        </div>
      </Card>
    );
  };

  // Filter queries based on current filter settings
  const applyFilters = useCallback(() => {
    const filterQueries = (queryList) => {
      return queryList.filter(query => {
        // Team filter
        if (filters.team !== 'all' && query.assignedTeam !== filters.team) {
          return false;
        }

        // Status filter
        if (filters.status !== 'all' && query.status !== filters.status) {
          return false;
        }

        // Priority filter
        if (filters.priority !== 'all' && query.priorityLevel !== filters.priority) {
          return false;
        }

        // Material Code filter
        if (filters.materialCode && !query.materialCode?.toLowerCase().includes(filters.materialCode.toLowerCase())) {
          return false;
        }

        // Project Code filter (if available)
        if (filters.projectCode && query.projectCode && !query.projectCode.toLowerCase().includes(filters.projectCode.toLowerCase())) {
          return false;
        }

        // Plant Code filter
        if (filters.plantCode && !query.assignedPlant?.toLowerCase().includes(filters.plantCode.toLowerCase())) {
          return false;
        }

        // Date Range filter
        if (filters.dateRange !== 'all') {
          const queryDate = new Date(query.createdAt);
          const now = new Date();
          let startDate;

          switch (filters.dateRange) {
            case 'today':
              startDate = new Date(now.getFullYear(), now.getMonth(), now.getDate());
              break;
            case 'week':
              startDate = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
              break;
            case 'month':
              startDate = new Date(now.getFullYear(), now.getMonth(), 1);
              break;
            case '3months':
              startDate = new Date(now.getFullYear(), now.getMonth() - 3, 1);
              break;
            default:
              return true;
          }

          if (queryDate < startDate) {
            return false;
          }
        }

        return true;
      });
    };

    setFilteredQueries({
      all: filterQueries(queries.all),
      open: filterQueries(queries.open),
      resolved: filterQueries(queries.resolved),
      myQueries: filterQueries(queries.myQueries)
    });
  }, [queries, filters]);

  // Apply filters whenever queries or filters change
  useEffect(() => {
    applyFilters();
  }, [queries, filters, applyFilters]);

  const handleCreateQuery = async values => {
    try {
      await queryAPI.createQuery({
        workflowId,
        ...values,
        createdBy: getCurrentUser()
      });
      setCreateModalVisible(false);
      form.resetFields();
      loadQueries();
    } catch (error) {
      console.error('Failed to create query:', error);
    }
  };

  const handleResolveQuery = async values => {
    try {
      await queryAPI.resolveQuery(selectedQuery.id, {
        resolution: values.resolution,
        resolvedBy: getCurrentUser()
      });
      setResolveModalVisible(false);
      resolveForm.resetFields();
      setSelectedQuery(null);
      loadQueries();
    } catch (error) {
      console.error('Failed to resolve query:', error);
    }
  };





  const canCreateQuery = () => {
    return [TEAM_NAMES.PLANT, TEAM_NAMES.JVC].includes(userRole);
  };

  const canResolveQuery = query => {
    return query.assignedTeam === userRole || userRole === 'ADMIN';
  };



  const getTabCount = queryList => {
    return queryList.length > 0 ? queryList.length : null;
  };

  return (
    <Card
      title="Queries"
      extra={
        <Space>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => {
              const controller = new AbortController();
              loadQueries(controller.signal);
            }}
          >
            Refresh
          </Button>
          {canCreateQuery() && (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setCreateModalVisible(true)}
            >
              Raise Query
            </Button>
          )}
        </Space>
      }
    >
      {/* Filter Controls */}
      <Card size="small" style={{ marginBottom: 16, background: '#fafafa' }}>
        <Row gutter={[16, 8]} align="middle">
          <Col xs={24} sm={6} md={4}>
            <Select
              placeholder="Team"
              value={filters.team}
              onChange={value => setFilters({ ...filters, team: value })}
              style={{ width: '100%' }}
              size="small"
            >
              <Select.Option value="all">All Teams</Select.Option>
              <Select.Option value="CQS">CQS</Select.Option>
              <Select.Option value="TECH">TECH</Select.Option>
              <Select.Option value="JVC">JVC</Select.Option>
              <Select.Option value="PLANT">PLANT</Select.Option>
            </Select>
          </Col>
          <Col xs={24} sm={6} md={4}>
            <Select
              placeholder="Status"
              value={filters.status}
              onChange={value => setFilters({ ...filters, status: value })}
              style={{ width: '100%' }}
              size="small"
            >
              <Select.Option value="all">All Status</Select.Option>
              <Select.Option value="OPEN">Open</Select.Option>
              <Select.Option value="RESOLVED">Resolved</Select.Option>
              <Select.Option value="CLOSED">Closed</Select.Option>
            </Select>
          </Col>
          <Col xs={24} sm={6} md={4}>
            <Select
              placeholder="Priority"
              value={filters.priority}
              onChange={value => setFilters({ ...filters, priority: value })}
              style={{ width: '100%' }}
              size="small"
            >
              <Select.Option value="all">All Priority</Select.Option>
              <Select.Option value="LOW">Low</Select.Option>
              <Select.Option value="MEDIUM">Medium</Select.Option>
              <Select.Option value="HIGH">High</Select.Option>
              <Select.Option value="URGENT">Urgent</Select.Option>
            </Select>
          </Col>
          <Col xs={24} sm={6} md={4}>
            <Select
              placeholder="Date Range"
              value={filters.dateRange}
              onChange={value => setFilters({ ...filters, dateRange: value })}
              style={{ width: '100%' }}
              size="small"
            >
              <Select.Option value="all">All Time</Select.Option>
              <Select.Option value="today">Today</Select.Option>
              <Select.Option value="week">Past Week</Select.Option>
              <Select.Option value="month">Past Month</Select.Option>
              <Select.Option value="3months">Past 3 Months</Select.Option>
            </Select>
          </Col>
          <Col xs={24} sm={12} md={4}>
            <Input
              placeholder="Material Code"
              value={filters.materialCode}
              onChange={e => setFilters({ ...filters, materialCode: e.target.value })}
              size="small"
            />
          </Col>
          <Col xs={24} sm={12} md={4}>
            <Input
              placeholder="Project Code"
              value={filters.projectCode}
              onChange={e => setFilters({ ...filters, projectCode: e.target.value })}
              size="small"
            />
          </Col>
          <Col xs={24} sm={12} md={4}>
            <Input
              placeholder="Plant Code"
              value={filters.plantCode}
              onChange={e => setFilters({ ...filters, plantCode: e.target.value })}
              size="small"
            />
          </Col>
          <Col xs={24} sm={12} md={4}>
            <Button
              icon={<FilterOutlined />}
              onClick={() => setFilters({
                team: 'all',
                status: 'all',
                priority: 'all',
                dateRange: 'all',
                materialCode: '',
                projectCode: '',
                plantCode: ''
              })}
              size="small"
            >
              Clear
            </Button>
          </Col>
        </Row>
      </Card>

      <Tabs
        defaultActiveKey="all"
        className={isMobile ? 'query-widget-mobile' : ''}
        size={isMobile ? 'small' : 'default'}
      >
        <TabPane
          tab={
            <Badge count={getTabCount(filteredQueries.all)} size="small">
              <span>All Queries</span>
            </Badge>
          }
          key="all"
        >
          {filteredQueries.all.length > 0 ? (
            <div style={{ maxHeight: '600px', overflowY: 'auto' }}>
              {filteredQueries.all.map(query => renderQueryCard(query))}
            </div>
          ) : (
            <Empty description="No queries found" />
          )}
        </TabPane>

        <TabPane
          tab={
            <Badge count={getTabCount(filteredQueries.open)} size="small">
              <span style={{ color: '#ff4d4f' }}>Open</span>
            </Badge>
          }
          key="open"
        >
          {filteredQueries.open.length > 0 ? (
            <div style={{ maxHeight: '600px', overflowY: 'auto' }}>
              {filteredQueries.open.map(query => renderQueryCard(query))}
            </div>
          ) : (
            <Empty description="No open queries found" />
          )}
        </TabPane>

        <TabPane
          tab={
            <Badge count={getTabCount(filteredQueries.resolved)} size="small">
              <span style={{ color: '#52c41a' }}>Resolved</span>
            </Badge>
          }
          key="resolved"
        >
          {filteredQueries.resolved.length > 0 ? (
            <div style={{ maxHeight: '600px', overflowY: 'auto' }}>
              {filteredQueries.resolved.map(query => renderQueryCard(query))}
            </div>
          ) : (
            <Empty description="No resolved queries found" />
          )}
        </TabPane>

        <TabPane
          tab={
            <Badge count={getTabCount(filteredQueries.myQueries)} size="small">
              <span>My Queries</span>
            </Badge>
          }
          key="my"
        >
          {filteredQueries.myQueries.length > 0 ? (
            <div style={{ maxHeight: '600px', overflowY: 'auto' }}>
              {filteredQueries.myQueries.map(query => renderQueryCard(query))}
            </div>
          ) : (
            <Empty description="No queries raised by you found" />
          )}
        </TabPane>
      </Tabs>

      {/* Create Query Modal */}
      <Modal
        title="Raise New Query"
        open={createModalVisible}
        onCancel={() => {
          setCreateModalVisible(false);
          form.resetFields();
        }}
        onOk={() => form.submit()}
        width={600}
      >
        <Form form={form} layout="vertical" onFinish={handleCreateQuery}>
          <Form.Item
            name="assignedTeam"
            label="Assign to Team"
            rules={[{ required: true, message: 'Please select a team' }]}
          >
            <Select placeholder="Select team">
              <Option value="CQS">CQS Team</Option>
              <Option value="TECH">Tech Team</Option>
              <Option value="JVC">JVC Team</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="fieldContext"
            label="Field Context"
            help="Which field or section is this query about?"
          >
            <Input placeholder="e.g., Material Name, Safety Data, etc." />
          </Form.Item>

          <Form.Item
            name="question"
            label="Question"
            rules={[{ required: true, message: 'Please enter your question' }]}
          >
            <TextArea rows={4} placeholder="Describe your question or issue in detail..." />
          </Form.Item>

          <Form.Item name="priority" label="Priority" initialValue="MEDIUM">
            <Select>
              <Option value="LOW">Low</Option>
              <Option value="MEDIUM">Medium</Option>
              <Option value="HIGH">High</Option>
              <Option value="URGENT">Urgent</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      {/* Resolve Query Modal */}
      <Modal
        title={`Resolve Query #${selectedQuery?.id}`}
        open={resolveModalVisible}
        onCancel={() => {
          setResolveModalVisible(false);
          resolveForm.resetFields();
          setSelectedQuery(null);
        }}
        onOk={() => resolveForm.submit()}
        width={600}
      >
        {selectedQuery && (
          <>
            <div style={{ marginBottom: 16, padding: 12, background: '#f5f5f5', borderRadius: 4 }}>
              <strong>Question:</strong> {selectedQuery.question}
              {selectedQuery.fieldContext && (
                <div>
                  <strong>Field Context:</strong> {selectedQuery.fieldContext}
                </div>
              )}
            </div>

            <Form form={resolveForm} layout="vertical" onFinish={handleResolveQuery}>
              <Form.Item
                name="resolution"
                label="Resolution"
                rules={[{ required: true, message: 'Please provide a resolution' }]}
              >
                <TextArea
                  rows={6}
                  placeholder="Provide detailed resolution or answer to the query..."
                />
              </Form.Item>
            </Form>
          </>
        )}
      </Modal>


    </Card>
  );
};

export default QueryWidget;
