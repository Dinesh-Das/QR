import { Card, Row, Col, Statistic, Typography, Table, Divider, Tabs } from 'antd';
import React, { useEffect, useState } from 'react';

import apiClient from '../api/client';
import { FILE_SIZE } from '../constants';

const { Title } = Typography;
const { TabPane } = Tabs;

const Dashboard = () => {
  // User Analytics State
  const [userStats, setUserStats] = useState({ total: 0, active: 0 });
  const [roleDist, setRoleDist] = useState([]);
  const [activityTimeline, setActivityTimeline] = useState([]);
  
  // System Health State
  const [health, setHealth] = useState({});
  const [systemStats, setSystemStats] = useState({});
  
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const controller = new AbortController();

    const fetchDashboardData = async () => {
      setLoading(true);
      try {
        const [userStats, roleDistribution, activityTimeline, health, systemStats] = await Promise.all([
          apiClient.get('/reports/analytics/user-stats', { signal: controller.signal }).catch(() => ({ total: 0, active: 0 })),
          apiClient.get('/reports/analytics/role-distribution', { signal: controller.signal }).catch(() => ({})),
          apiClient.get('/reports/analytics/activity-timeline', { signal: controller.signal }).catch(() => ({})),
          apiClient.get('/system/health', { signal: controller.signal }).catch(() => ({})),
          apiClient.get('/system/stats', { signal: controller.signal }).catch(() => ({}))
        ]);

        if (!controller.signal.aborted) {
          setUserStats(userStats);
          setRoleDist(Object.entries(roleDistribution).map(([role, count]) => ({ role, count })));
          setActivityTimeline(
            Object.entries(activityTimeline).map(([date, count]) => ({ date, count }))
          );
          setHealth(health);
          setSystemStats(systemStats);
        }
      } catch (error) {
        if (!controller.signal.aborted) {
          console.error('Failed to fetch dashboard data:', error);
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    };

    fetchDashboardData();

    return () => {
      controller.abort();
    };
  }, []);

  const formatBytes = bytes => {
    if (bytes === 0) {
      return '0 Bytes';
    }
    const k = FILE_SIZE.BYTES_PER_KB || 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
  };

  const formatUptime = ms => {
    const seconds = Math.floor(ms / 1000);
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    return `${days}d ${hours}h ${minutes}m`;
  };

  const renderOverviewTab = () => (
    <>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="System Status"
              value={health.status || 'Unknown'}
              valueStyle={{ color: health.status === 'UP' ? '#3f8600' : '#cf1322' }}
              loading={loading}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="Total Users" value={userStats.total} loading={loading} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="Active Users" value={userStats.active || systemStats.activeUsers || 0} loading={loading} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Uptime"
              value={health.uptime ? formatUptime(health.uptime) : 'N/A'}
              loading={loading}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={12}>
          <Card title="Role Distribution">
            <Table
              dataSource={roleDist}
              columns={[
                { 
                  title: 'Role', 
                  dataIndex: 'role',
                  render: text => text.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())
                },
                { title: 'Count', dataIndex: 'count' }
              ]}
              rowKey="role"
              pagination={false}
              loading={loading}
              size="small"
            />
          </Card>
        </Col>
        <Col span={12}>
          <Card title="Recent Activity">
            <Table
              dataSource={activityTimeline.slice(0, 10)}
              columns={[
                { title: 'Date', dataIndex: 'date' },
                { title: 'Activities', dataIndex: 'count' }
              ]}
              rowKey="date"
              pagination={false}
              loading={loading}
              size="small"
            />
          </Card>
        </Col>
      </Row>
    </>
  );

  const renderSystemTab = () => (
    <>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="System Status"
              value={health.status || 'Unknown'}
              valueStyle={{ color: health.status === 'UP' ? '#3f8600' : '#cf1322' }}
              loading={loading}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Memory Used"
              value={health.memory ? formatBytes(health.memory) : 'N/A'}
              loading={loading}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Uptime"
              value={health.uptime ? formatUptime(health.uptime) : 'N/A'}
              loading={loading}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="Active Sessions" value={systemStats.activeSessions || 0} loading={loading} />
          </Card>
        </Col>
      </Row>

      <Row gutter={16}>
        <Col span={24}>
          <Card title="System Metrics">
            <Table
              dataSource={Object.entries(systemStats).map(([key, value]) => ({
                key,
                metric: key.replace(/([A-Z])/g, ' $1').toLowerCase(),
                value: typeof value === 'number' ? value.toLocaleString() : value
              }))}
              columns={[
                {
                  title: 'Metric',
                  dataIndex: 'metric',
                  key: 'metric',
                  render: text => text.charAt(0).toUpperCase() + text.slice(1)
                },
                {
                  title: 'Value',
                  dataIndex: 'value',
                  key: 'value'
                }
              ]}
              pagination={false}
              loading={loading}
              size="small"
            />
          </Card>
        </Col>
      </Row>
    </>
  );

  const renderAnalyticsTab = () => (
    <>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={8}>
          <Card>
            <Statistic title="Total Users" value={userStats.total} loading={loading} />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic title="Active Users" value={userStats.active} loading={loading} />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic 
              title="User Activity Rate" 
              value={userStats.total > 0 ? Math.round((userStats.active / userStats.total) * 100) : 0}
              suffix="%" 
              loading={loading} 
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={16}>
        <Col span={12}>
          <Card title="User Role Distribution">
            <Table
              dataSource={roleDist}
              columns={[
                { 
                  title: 'Role', 
                  dataIndex: 'role',
                  render: text => text.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())
                },
                { title: 'Count', dataIndex: 'count' },
                { 
                  title: 'Percentage', 
                  dataIndex: 'count',
                  render: (count) => {
                    const total = roleDist.reduce((sum, item) => sum + item.count, 0);
                    return total > 0 ? `${Math.round((count / total) * 100)}%` : '0%';
                  }
                }
              ]}
              rowKey="role"
              pagination={false}
              loading={loading}
              size="small"
            />
          </Card>
        </Col>
        <Col span={12}>
          <Card title="Activity Timeline">
            <Table
              dataSource={activityTimeline}
              columns={[
                { title: 'Date', dataIndex: 'date' },
                { title: 'Activities', dataIndex: 'count' }
              ]}
              rowKey="date"
              pagination={{ pageSize: 10 }}
              loading={loading}
              size="small"
            />
          </Card>
        </Col>
      </Row>
    </>
  );

  return (
    <div style={{ padding: 24 }}>
      <Title level={2}>System Dashboard</Title>
      
      <Tabs defaultActiveKey="overview" size="large">
        <TabPane tab="Overview" key="overview">
          {renderOverviewTab()}
        </TabPane>
        
        <TabPane tab="System Health" key="system">
          {renderSystemTab()}
        </TabPane>
        
        <TabPane tab="User Analytics" key="analytics">
          {renderAnalyticsTab()}
        </TabPane>
      </Tabs>
    </div>
  );
};

export default Dashboard;
