import { PlusOutlined } from '@ant-design/icons';
import { Card, Tabs, Table, Tag, Button, Space, Modal, Form, Input, Select, Badge } from 'antd';
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

const QueryWidget = ({ workflowId, userRole }) => {
  const [loading, setLoading] = useState(false);
  const [queries, setQueries] = useState({
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

  useEffect(() => {
    if (workflowId) {
      const controller = new AbortController();

      const loadQueriesWithAbort = async () => {
        try {
          setLoading(true);
          const allQueries = await queryAPI.getQueriesByWorkflow(workflowId, {
            signal: controller.signal
          });

          if (!controller.signal.aborted) {
            setQueries({
              all: allQueries,
              open: allQueries.filter(q => QUERY_STATUS_GROUPS.ACTIVE.includes(q.status)),
              resolved: allQueries.filter(q => QUERY_STATUS_GROUPS.INACTIVE.includes(q.status)),
              myQueries: allQueries.filter(q => q.createdBy === getCurrentUser())
            });
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
    }
  }, [workflowId]); // Remove loadQueries dependency

  const loadQueries = useCallback(
    async signal => {
      try {
        setLoading(true);
        const allQueries = await queryAPI.getQueriesByWorkflow(workflowId, { signal });

        if (!signal?.aborted) {
          setQueries({
            all: allQueries,
            open: allQueries.filter(q => QUERY_STATUS_GROUPS.ACTIVE.includes(q.status)),
            resolved: allQueries.filter(q => QUERY_STATUS_GROUPS.INACTIVE.includes(q.status)),
            myQueries: allQueries.filter(q => q.createdBy === getCurrentUser())
          });
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
    [workflowId]
  );

  const getCurrentUser = () => {
    // Get current user from auth context or localStorage
    return localStorage.getItem('username') || 'current_user';
  };

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

  const getStatusColor = status => {
    const statusColors = {
      [QUERY_STATUS.OPEN]: 'red',
      [QUERY_STATUS.RESOLVED]: 'green',
      [QUERY_STATUS.CLOSED]: 'gray'
    };
    return statusColors[status] || 'default';
  };

  const getTeamColor = team => {
    const colors = {
      CQS: 'blue',
      TECH: 'purple',
      PLANT: 'orange',
      JVC: 'cyan'
    };
    return colors[team] || 'default';
  };

  // Responsive column configuration for queries
  const getQueryColumns = () => {
    const baseColumns = [
      {
        title: 'ID',
        dataIndex: 'id',
        key: 'id',
        width: isMobile ? 50 : 60,
        fixed: isMobile ? 'left' : false
      },
      {
        title: 'Question',
        dataIndex: 'question',
        key: 'question',
        ellipsis: true,
        render: text => (
          <span title={text}>
            {isMobile && text.length > 30 ? `${text.substring(0, 30)}...` : text}
          </span>
        )
      },
      {
        title: 'Team',
        dataIndex: 'assignedTeam',
        key: 'assignedTeam',
        width: isMobile ? 60 : 80,
        render: team => (
          <Tag color={getTeamColor(team)} size={isMobile ? 'small' : 'default'}>
            {team}
          </Tag>
        )
      },
      {
        title: 'Status',
        dataIndex: 'status',
        key: 'status',
        width: isMobile ? 60 : 80,
        render: status => {
          const statusDisplayNames = {
            [QUERY_STATUS.OPEN]: 'Open',
            [QUERY_STATUS.RESOLVED]: 'Resolved',
            [QUERY_STATUS.CLOSED]: 'Closed'
          };
          
          return (
            <Tag color={getStatusColor(status)} size={isMobile ? 'small' : 'default'}>
              {statusDisplayNames[status] || status}
            </Tag>
          );
        }
      },
      {
        title: 'Context',
        dataIndex: 'fieldContext',
        key: 'fieldContext',
        width: 120,
        ellipsis: true,
        responsive: ['md']
      },
      {
        title: 'Created',
        dataIndex: 'createdAt',
        key: 'createdAt',
        width: isMobile ? 80 : 100,
        responsive: ['sm'],
        render: date => {
          if (!date) {
            return '-';
          }
          const d = new Date(date);
          return isMobile
            ? d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
            : d.toLocaleDateString();
        }
      },
      {
        title: 'Actions',
        key: 'actions',
        width: isMobile ? 70 : 120,
        fixed: isMobile ? 'right' : false,
        render: (_, record) => (
          <Space>
            {/* Resolve Button - For team members on OPEN queries */}
            {record.status === QUERY_STATUS.OPEN && canResolveQuery(record) && (
              <Button
                size="small"
                type="primary"
                onClick={() => {
                  setSelectedQuery(record);
                  setResolveModalVisible(true);
                }}
              >
                {isMobile ? 'Resolve' : 'Resolve'}
              </Button>
            )}
            
            {/* View Button - For inactive queries */}
            {QUERY_STATUS_GROUPS.INACTIVE.includes(record.status) && (
              <Button
                size="small"
                type="link"
                onClick={() => {
                  setSelectedQuery(record);
                  setResolveModalVisible(true);
                }}
              >
                View
              </Button>
            )}
          </Space>
        )
      }
    ];

    return baseColumns;
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
        canCreateQuery() && (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setCreateModalVisible(true)}
          >
            Raise Query
          </Button>
        )
      }
    >
      <Tabs
        defaultActiveKey="all"
        className={isMobile ? 'query-widget-mobile' : ''}
        size={isMobile ? 'small' : 'default'}
      >
        <TabPane
          tab={
            <Badge count={getTabCount(queries.all)} size="small">
              <span>All Queries</span>
            </Badge>
          }
          key="all"
        >
          <Table
            dataSource={queries.all}
            columns={getQueryColumns()}
            loading={loading}
            pagination={{ pageSize: isMobile ? 5 : 10 }}
            size="small"
            rowKey="id"
            scroll={isMobile ? { x: 500 } : undefined}
            className={isMobile ? 'touch-friendly-table' : ''}
          />
        </TabPane>

        <TabPane
          tab={
            <Badge count={getTabCount(queries.open)} size="small">
              <span style={{ color: '#ff4d4f' }}>Open</span>
            </Badge>
          }
          key="open"
        >
          <Table
            dataSource={queries.open}
            columns={getQueryColumns()}
            loading={loading}
            pagination={{ pageSize: isMobile ? 5 : 10 }}
            size="small"
            rowKey="id"
            scroll={isMobile ? { x: 500 } : undefined}
            className={isMobile ? 'touch-friendly-table' : ''}
          />
        </TabPane>

        <TabPane
          tab={
            <Badge count={getTabCount(queries.resolved)} size="small">
              <span style={{ color: '#52c41a' }}>Resolved</span>
            </Badge>
          }
          key="resolved"
        >
          <Table
            dataSource={queries.resolved}
            columns={getQueryColumns()}
            loading={loading}
            pagination={{ pageSize: isMobile ? 5 : 10 }}
            size="small"
            rowKey="id"
            scroll={isMobile ? { x: 500 } : undefined}
            className={isMobile ? 'touch-friendly-table' : ''}
          />
        </TabPane>

        <TabPane
          tab={
            <Badge count={getTabCount(queries.myQueries)} size="small">
              <span>My Queries</span>
            </Badge>
          }
          key="my"
        >
          <Table
            dataSource={queries.myQueries}
            columns={getQueryColumns()}
            loading={loading}
            pagination={{ pageSize: isMobile ? 5 : 10 }}
            size="small"
            rowKey="id"
            scroll={isMobile ? { x: 500 } : undefined}
            className={isMobile ? 'touch-friendly-table' : ''}
          />
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
