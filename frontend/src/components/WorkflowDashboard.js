import {
  ClockCircleOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  FileTextOutlined,
  TeamOutlined,
  CalendarOutlined
} from '@ant-design/icons';
import { Card, Row, Col, Statistic, Table, Tag, Button, Space, Spin, Alert } from 'antd';
import React, { useState, useEffect } from 'react';

import { UI_CONFIG, PAGINATION } from '../constants';
import { workflowAPI } from '../services/workflowAPI';

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

const WorkflowDashboard = ({ onWorkflowSelect }) => {
  const [loading, setLoading] = useState(true);
  const [dashboardData, setDashboardData] = useState({
    summary: {},
    overdueWorkflows: [],
    recentActivity: [],
    workflowsByState: {}
  });
  const [error, setError] = useState(null);
  const { isMobile } = useResponsive();

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      const [summary, overdue, recent, byState] = await Promise.all([
        workflowAPI.getDashboardSummary(),
        workflowAPI.getOverdueWorkflows(3),
        workflowAPI.getRecentActivity(7),
        workflowAPI.getWorkflowCountsByState()
      ]);

      setDashboardData({
        summary,
        overdueWorkflows: overdue,
        recentActivity: recent,
        workflowsByState: byState
      });
    } catch (err) {
      setError('Failed to load dashboard data');
      console.error('Dashboard error:', err);
    } finally {
      setLoading(false);
    }
  };

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

  // Responsive column configuration
  const getWorkflowColumns = () => {
    const baseColumns = [
      {
        title: 'Material Code',
        dataIndex: 'materialCode',
        key: 'materialCode',
        width: isMobile ? 120 : 140,
        fixed: isMobile ? 'left' : false,
        render: (text, record) => (
          <div>
            <div style={{ fontWeight: 'bold' }}>{text}</div>
            {record.itemDescription && (
              <div style={{ fontSize: '12px', color: '#666' }}>{record.itemDescription}</div>
            )}
          </div>
        )
      },

      {
        title: 'State',
        dataIndex: 'currentState',
        key: 'currentState',
        width: isMobile ? 80 : 100,
        render: state => (
          <Tag color={getStateColor(state)} size={isMobile ? 'small' : 'default'}>
            {isMobile ? state.split('_')[0] : state.replace('_', ' ')}
          </Tag>
        )
      },
      {
        title: 'Plant',
        dataIndex: 'assignedPlant',
        key: 'assignedPlant',
        width: 100,
        responsive: ['lg']
      },
      {
        title: 'Days',
        dataIndex: 'daysPending',
        key: 'daysPending',
        width: isMobile ? 60 : 100,
        render: days => <span style={{ color: days > 3 ? '#ff4d4f' : 'inherit' }}>{days}</span>
      },
      {
        title: 'Queries',
        key: 'queries',
        width: isMobile ? 70 : 80,
        responsive: ['sm'],
        render: (_, record) => (
          <span>
            {record.openQueries > 0 && (
              <Tag color="red" size="small">
                {record.openQueries}
              </Tag>
            )}
            {!isMobile && <small>{record.totalQueries} total</small>}
          </span>
        )
      },
      {
        title: 'Actions',
        key: 'actions',
        width: isMobile ? 80 : 100,
        fixed: isMobile ? 'right' : false,
        render: (_, record) => (
          <Space>
            <Button
              size="small"
              type="link"
              onClick={() => onWorkflowSelect && onWorkflowSelect(record.id)}
            >
              {isMobile ? 'View' : 'View'}
            </Button>
          </Space>
        )
      }
    ];

    return baseColumns;
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Spin size="large" />
      </div>
    );
  }

  if (error) {
    return (
      <Alert
        message="Error"
        description={error}
        type="error"
        showIcon
        action={
          <Button size="small" onClick={loadDashboardData}>
            Retry
          </Button>
        }
      />
    );
  }

  return (
    <div>
      {/* Summary Statistics */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }} className="workflow-dashboard-stats">
        <Col xs={12} sm={12} md={6} lg={6}>
          <Card>
            <Statistic
              title="Total Workflows"
              value={dashboardData.summary.totalWorkflows || 0}
              prefix={<FileTextOutlined />}
            />
          </Card>
        </Col>
        <Col xs={12} sm={12} md={6} lg={6}>
          <Card>
            <Statistic
              title="Active Workflows"
              value={dashboardData.summary.activeWorkflows || 0}
              prefix={<ClockCircleOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={12} md={6} lg={6}>
          <Card>
            <Statistic
              title="Overdue"
              value={dashboardData.summary.overdueWorkflows || 0}
              prefix={<ExclamationCircleOutlined />}
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={12} md={6} lg={6}>
          <Card>
            <Statistic
              title="Completed"
              value={dashboardData.summary.completedWorkflows || 0}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Secondary Statistics */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }} className="workflow-dashboard-stats">
        <Col xs={24} sm={8} md={8} lg={8}>
          <Card>
            <Statistic
              title="Open Queries"
              value={dashboardData.summary.openQueries || 0}
              prefix={<TeamOutlined />}
              valueStyle={{ color: '#fa8c16' }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={8} md={8} lg={8}>
          <Card>
            <Statistic
              title="Avg Resolution Time"
              value={Math.round(dashboardData.summary.avgResolutionTimeHours || 0)}
              suffix={isMobile ? 'hrs' : 'hours'}
              prefix={<CalendarOutlined />}
            />
          </Card>
        </Col>
        <Col xs={12} sm={8} md={8} lg={8}>
          <Card>
            <Statistic
              title="Recent Workflows"
              value={dashboardData.summary.recentWorkflows || 0}
              prefix={<FileTextOutlined />}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Overdue Workflows */}
      {dashboardData.overdueWorkflows.length > 0 && (
        <Card
          title="Overdue Workflows"
          style={{ marginBottom: 24 }}
          extra={
            <Button type="primary" size="small">
              View All
            </Button>
          }
        >
          <Table
            dataSource={dashboardData.overdueWorkflows}
            columns={getWorkflowColumns()}
            pagination={{ pageSize: PAGINATION.SMALL_PAGE_SIZE }}
            size="small"
            rowKey="id"
            scroll={isMobile ? { x: 600 } : undefined}
            className={isMobile ? 'workflow-table-mobile touch-friendly-table' : ''}
          />
        </Card>
      )}

      {/* Recent Activity */}
      <Card
        title="Recent Activity"
        extra={
          <Button type="primary" size="small">
            View All
          </Button>
        }
      >
        <Table
          dataSource={dashboardData.recentActivity}
          columns={getWorkflowColumns()}
          pagination={{ pageSize: isMobile ? 5 : 10 }}
          size="small"
          rowKey="id"
          scroll={isMobile ? { x: 600 } : undefined}
          className={isMobile ? 'workflow-table-mobile touch-friendly-table' : ''}
        />
      </Card>
    </div>
  );
};

export default WorkflowDashboard;
