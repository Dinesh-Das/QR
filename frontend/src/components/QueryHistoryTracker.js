import {
  ClockCircleOutlined,
  CheckCircleOutlined,
  UserOutlined,
  MessageOutlined,
  HistoryOutlined
} from '@ant-design/icons';
import {
  Card,
  Timeline,
  Tag,
  Typography,
  Space,
  Button,
  Spin,
  Empty,
  Row,
  Col,
  Statistic
} from 'antd';
import React, { useState, useEffect, useCallback } from 'react';

import apiClient from '../api/client';
import { getUserRole } from '../services/auth';


const { Text } = Typography;

const QueryHistoryTracker = ({ queryId, materialCode, workflowId, compact = false }) => {
  const [loading, setLoading] = useState(false);
  const [history, setHistory] = useState([]);
  const [stats, setStats] = useState({
    totalQueries: 0,
    resolvedQueries: 0,
    avgResolutionTime: 0,
    currentSLA: 0
  });

  // Define function before useEffect that depends on it
  const loadQueryHistory = useCallback(async () => {
    try {
      setLoading(true);

      // Get current user's role to filter queries
      const userRole = getUserRole();
      console.log('QueryHistoryTracker - userRole:', userRole);
      
      let endpoint = '/queries';
      
      // If specific query/workflow/material is requested, use those endpoints
      if (queryId) {
        endpoint += `/${queryId}/history`;
      } else if (workflowId) {
        endpoint += `/workflow/${workflowId}`;
      } else if (materialCode) {
        endpoint += `/material/${materialCode}`;
      } else {
        // Default: Get all resolved queries for the user's team
        if (userRole) {
          // Map user role to team name for the endpoint
          const teamMap = {
            'CQS_USER': 'CQS',
            'TECH_USER': 'TECH', 
            'JVC_USER': 'JVC'
          };
          const team = teamMap[userRole];
          console.log('QueryHistoryTracker - mapped team:', team);
          if (team) {
            endpoint += `/resolved/${team}`;
          } else {
            console.warn('QueryHistoryTracker - unknown role, using CQS fallback');
            endpoint += `/resolved/CQS`; // fallback
          }
        } else {
          endpoint += `/resolved/CQS`; // fallback if no role
        }
      }

      console.log('QueryHistoryTracker - endpoint:', endpoint);
      
      const data = await apiClient.get(endpoint);
      
      console.log('QueryHistoryTracker - received data:', data);
      console.log('QueryHistoryTracker - data type:', typeof data);
      console.log('QueryHistoryTracker - is array:', Array.isArray(data));
      console.log('QueryHistoryTracker - data length:', data?.length);
      
      const historyData = Array.isArray(data) ? data : (data ? [data] : []);
      console.log('QueryHistoryTracker - setting history:', historyData);
      setHistory(historyData);

      // Calculate stats
      const dataArray = Array.isArray(data) ? data : (data ? [data] : []);
      const resolved = dataArray.filter(q => q.status === 'RESOLVED');
      const avgTime =
        resolved.length > 0
          ? resolved.reduce((sum, q) => sum + (q.daysOpen || 0), 0) / resolved.length
          : 0;

      console.log('QueryHistoryTracker - stats calculation:', {
        dataArray: dataArray.length,
        resolved: resolved.length,
        avgTime
      });

      setStats({
        totalQueries: dataArray.length,
        resolvedQueries: resolved.length,
        avgResolutionTime: avgTime,
        currentSLA: dataArray.filter(q => q.daysOpen > 3).length
      });
    } catch (error) {
      console.error('Failed to load query history:', error);
    } finally {
      setLoading(false);
    }
  }, [queryId, materialCode, workflowId]);

  useEffect(() => {
    // Always load query history - either for specific context or all resolved queries for the team
    loadQueryHistory();
  }, [queryId, materialCode, workflowId, loadQueryHistory]);

  const getStatusColor = status => {
    return status === 'OPEN' ? 'red' : 'green';
  };

  const getTeamColor = team => {
    const colors = {
      CQS: 'blue',
      TECH: 'purple',
      JVC: 'orange'
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

  const formatTimelineItem = query => {
    const isResolved = query.status === 'RESOLVED';

    return {
      dot: isResolved ? (
        <CheckCircleOutlined style={{ color: '#52c41a' }} />
      ) : (
        <ClockCircleOutlined style={{ color: '#faad14' }} />
      ),
      color: isResolved ? 'green' : 'blue',
      children: (
        <div>
          <div style={{ marginBottom: 8 }}>
            <Space wrap>
              <Text strong>Query #{query.id}</Text>
              <Tag color={getStatusColor(query.status)}>{query.status}</Tag>
              <Tag color={getTeamColor(query.assignedTeam)}>{query.assignedTeam}</Tag>
              {query.priorityLevel && (
                <Tag color={getPriorityColor(query.priorityLevel)}>{query.priorityLevel}</Tag>
              )}
            </Space>
          </div>

          <div style={{ marginBottom: 8 }}>
            <Text>{query.question}</Text>
          </div>

          {query.fieldName && (
            <div style={{ marginBottom: 4, fontSize: '12px', color: '#666' }}>
              Field: {query.fieldName} {query.stepNumber && `(Step ${query.stepNumber})`}
            </div>
          )}

          <div style={{ fontSize: '12px', color: '#666', marginBottom: 8 }}>
            <Space>
              <span>
                <UserOutlined /> Raised by: {query.raisedBy}
              </span>
              <span>
                <ClockCircleOutlined />{' '}
                {query.createdAt && new Date(query.createdAt).toLocaleString()}
              </span>
              <span>Days open: {query.daysOpen}</span>
            </Space>
          </div>

          {query.response && (
            <div
              style={{
                marginTop: 8,
                padding: 8,
                background: '#f0f9ff',
                borderRadius: 4,
                borderLeft: '3px solid #1890ff'
              }}
            >
              <div style={{ marginBottom: 4 }}>
                <Text strong>Resolution:</Text>
              </div>
              <div style={{ marginBottom: 4 }}>
                <Text>{query.response}</Text>
              </div>
              <div style={{ fontSize: '11px', color: '#666' }}>
                Resolved by: {query.resolvedBy} on{' '}
                {query.resolvedAt && new Date(query.resolvedAt).toLocaleString()}
              </div>
            </div>
          )}
        </div>
      )
    };
  };

  if (loading) {
    return (
      <Card size="small">
        <div style={{ textAlign: 'center', padding: '20px' }}>
          <Spin size="small" />
          <div style={{ marginTop: 8 }}>Loading query history...</div>
        </div>
      </Card>
    );
  }

  if (compact) {
    return (
      <Card
        size="small"
        title={
          <Space>
            <HistoryOutlined />
            <span>
              {queryId ? 'Query History' : 
               workflowId ? 'Workflow Queries' :
               materialCode ? 'Material Queries' :
               'Team History'}
            </span>
          </Space>
        }
        style={{ marginBottom: 16 }}
      >
        <Row gutter={16}>
          <Col span={6}>
            <Statistic title="Total" value={stats.totalQueries} prefix={<MessageOutlined />} />
          </Col>
          <Col span={6}>
            <Statistic
              title="Resolved"
              value={stats.resolvedQueries}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#3f8600' }}
            />
          </Col>
          <Col span={6}>
            <Statistic
              title="Avg Days"
              value={stats.avgResolutionTime}
              precision={1}
              prefix={<ClockCircleOutlined />}
            />
          </Col>
          <Col span={6}>
            <Statistic
              title="Overdue"
              value={stats.currentSLA}
              prefix={<ClockCircleOutlined />}
              valueStyle={{ color: stats.currentSLA > 0 ? '#cf1322' : '#3f8600' }}
            />
          </Col>
        </Row>
      </Card>
    );
  }

  return (
    <Card
      title={
        <Space>
          <HistoryOutlined />
          <span>
            {queryId ? 'Query History' : 
             workflowId ? 'Workflow Query History' :
             materialCode ? 'Material Query History' :
             'Team Resolved Queries'}
          </span>
        </Space>
      }
      size="small"
      extra={
        <Button size="small" icon={<HistoryOutlined />} onClick={loadQueryHistory}>
          Refresh
        </Button>
      }
    >
      {/* Stats Summary */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Statistic
            title="Total Queries"
            value={stats.totalQueries}
            prefix={<MessageOutlined />}
          />
        </Col>
        <Col span={6}>
          <Statistic
            title="Resolved"
            value={stats.resolvedQueries}
            prefix={<CheckCircleOutlined />}
            valueStyle={{ color: '#3f8600' }}
          />
        </Col>
        <Col span={6}>
          <Statistic
            title="Avg Resolution Time"
            value={stats.avgResolutionTime}
            precision={1}
            suffix="days"
            prefix={<ClockCircleOutlined />}
          />
        </Col>
        <Col span={6}>
          <Statistic
            title="Overdue Queries"
            value={stats.currentSLA}
            prefix={<ClockCircleOutlined />}
            valueStyle={{ color: stats.currentSLA > 0 ? '#cf1322' : '#3f8600' }}
          />
        </Col>
      </Row>

      {/* Timeline */}
      {history.length > 0 ? (
        <Timeline items={history.map(formatTimelineItem)} mode="left" />
      ) : (
        <Empty description="No query history available" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      )}
    </Card>
  );
};

export default QueryHistoryTracker;
