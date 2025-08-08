import {
  MessageOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined,
  HistoryOutlined
} from '@ant-design/icons';
import { Typography, Row, Col, Card, Statistic, Tabs, Alert, Space, Button } from 'antd';
import React, { useState, useEffect } from 'react';

import apiClient from '../api/client';
import QueryHistoryTracker from '../components/QueryHistoryTracker';
import QueryInbox from '../components/QueryInbox';
import { TechOnly } from '../components/RoleBasedComponent';
import WorkflowDashboard from '../components/WorkflowDashboard';
import { TEAM_NAMES } from '../constants';
import { useRoleBasedAccess } from '../hooks/useRoleBasedAccess';

const { Title } = Typography;
const { TabPane } = Tabs;

const TechView = () => {
  const [queryStats, setQueryStats] = useState({
    totalQueries: 0,
    openQueries: 0,
    resolvedToday: 0,
    overdueQueries: 0,
    avgResolutionTime: 0,
    highPriorityQueries: 0
  });

  // Use role-based access control
  const { isTechUser, isAdmin } = useRoleBasedAccess();

  useEffect(() => {
    const controller = new AbortController();

    loadDashboardStats(controller.signal);

    return () => {
      controller.abort();
    };
  }, []);

  const loadDashboardStats = async signal => {
    try {
      const [openCount, resolvedToday, overdueQueries, avgTime, highPriorityQueries] =
        await Promise.all([
          apiClient.get('/queries/stats/count-open/TECH', { signal }).catch(() => 0),
          apiClient.get('/queries/stats/resolved-today', { signal }).catch(() => 0),
          apiClient
            .get('/queries/overdue', { signal })
            .then(data =>
              Array.isArray(data) ? data.filter(q => q.assignedTeam === TEAM_NAMES.TECH).length : 0
            )
            .catch(() => 0),
          apiClient.get('/queries/stats/avg-resolution-time/TECH', { signal }).catch(() => 0),
          apiClient
            .get('/queries/high-priority', { signal })
            .then(data =>
              Array.isArray(data) ? data.filter(q => q.assignedTeam === TEAM_NAMES.TECH).length : 0
            )
            .catch(() => 0)
        ]);

      setQueryStats({
        totalQueries: openCount + resolvedToday,
        openQueries: openCount,
        resolvedToday,
        overdueQueries,
        avgResolutionTime: avgTime,
        highPriorityQueries
      });
    } catch (error) {
      console.error('Failed to load dashboard stats:', error);
    }
  };

  return (
    <TechOnly>
      <div style={{ padding: 24 }}>
        <Title level={2}>Tech Dashboard</Title>

      {/* Enhanced Quick Stats */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="Open Queries"
              value={queryStats.openQueries}
              prefix={<MessageOutlined />}
              valueStyle={{ color: '#cf1322' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Resolved Today"
              value={queryStats.resolvedToday}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#3f8600' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Overdue"
              value={queryStats.overdueQueries}
              prefix={<ExclamationCircleOutlined />}
              valueStyle={{ color: queryStats.overdueQueries > 0 ? '#cf1322' : '#3f8600' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Avg Resolution"
              value={queryStats.avgResolutionTime}
              precision={1}
              suffix="hrs"
              prefix={<ClockCircleOutlined />}
            />
          </Card>
        </Col>
      </Row>

      {/* Alerts for urgent items */}
      {queryStats.overdueQueries > 0 && (
        <Alert
          message={`${queryStats.overdueQueries} queries are overdue (>3 days)`}
          description="These queries require immediate attention to maintain SLA compliance."
          type="error"
          showIcon
          style={{ marginBottom: 16 }}
          action={
            <Button size="small" danger>
              View Overdue
            </Button>
          }
        />
      )}

      {queryStats.highPriorityQueries > 0 && (
        <Alert
          message={`${queryStats.highPriorityQueries} high priority queries pending`}
          description="These queries have been marked as high priority and need urgent resolution."
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          action={
            <Button size="small" type="primary">
              View High Priority
            </Button>
          }
        />
      )}

      {/* Enhanced Main Content Tabs */}
      <Tabs
        defaultActiveKey="queries"
        size="large"
        tabBarExtraContent={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={loadDashboardStats} size="small">
              Refresh Stats
            </Button>
          </Space>
        }
      >
        <TabPane
          tab={
            <Space>
              <MessageOutlined />
              <span>Query Inbox</span>
              {queryStats.openQueries > 0 && (
                <span
                  style={{
                    background: '#ff4d4f',
                    color: 'white',
                    borderRadius: '10px',
                    padding: '2px 6px',
                    fontSize: '12px'
                  }}
                >
                  {queryStats.openQueries}
                </span>
              )}
            </Space>
          }
          key="queries"
        >
          <QueryInbox team="TECH" userRole="TECH_USER" />
        </TabPane>
        <TabPane
          tab={
            <Space>
              <ClockCircleOutlined />
              <span>Workflow Dashboard</span>
            </Space>
          }
          key="dashboard"
        >
          <WorkflowDashboard userRole="TECH_USER" />
        </TabPane>
        <TabPane
          tab={
            <Space>
              <HistoryOutlined />
              <span>Query History</span>
            </Space>
          }
          key="history"
        >
          <QueryHistoryTracker />
        </TabPane>
      </Tabs>
      </div>
    </TechOnly>
  );
};

export default TechView;
