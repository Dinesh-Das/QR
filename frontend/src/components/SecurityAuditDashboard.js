/**
 * Security Audit Dashboard Component
 * 
 * A comprehensive dashboard for monitoring security events, threats, and system health.
 * Provides real-time security monitoring and historical analysis capabilities.
 * 
 * @component
 * @since 1.0.0
 * @author QRMFG Security Team
 */

import {
  ShieldOutlined,
  WarningOutlined,
  ExclamationCircleOutlined,
  SafetyCertificateOutlined,
  EyeOutlined,
  FileProtectOutlined,
  UserOutlined,
  ApiOutlined,
  ClockCircleOutlined,
  ReloadOutlined
} from '@ant-design/icons';
import {
  Card,
  Row,
  Col,
  Statistic,
  Table,
  Tag,
  Alert,
  Timeline,
  Progress,
  Button,
  Select,
  DatePicker,
  Space,
  Tooltip,
  Badge,
  List,
  Typography,
  Divider
} from 'antd';
import React, { useState, useEffect, useMemo, useCallback } from 'react';

import { securityMonitoring, SECURITY_EVENT_TYPES, SECURITY_SEVERITY } from '../services/securityMonitoring';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

/**
 * Security Audit Dashboard Component
 */
const SecurityAuditDashboard = React.memo(() => {
  const [securityEvents, setSecurityEvents] = useState([]);
  const [criticalAlerts, setCriticalAlerts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedSeverity, setSelectedSeverity] = useState('ALL');
  const [selectedEventType, setSelectedEventType] = useState('ALL');
  const [dateRange, setDateRange] = useState(null);
  const [autoRefresh, setAutoRefresh] = useState(true);

  // Load security data
  const loadSecurityData = useCallback(async () => {
    setLoading(true);
    try {
      const events = securityMonitoring.getSecurityEvents(500);
      const alerts = securityMonitoring.getCriticalAlerts();
      
      setSecurityEvents(events);
      setCriticalAlerts(alerts);
    } catch (error) {
      console.error('Failed to load security data:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  // Auto-refresh data
  useEffect(() => {
    loadSecurityData();
    
    if (autoRefresh) {
      const interval = setInterval(loadSecurityData, 30000); // Refresh every 30 seconds
      return () => clearInterval(interval);
    }
  }, [loadSecurityData, autoRefresh]);

  // Filter events based on selected criteria
  const filteredEvents = useMemo(() => {
    let filtered = [...securityEvents];

    // Filter by severity
    if (selectedSeverity !== 'ALL') {
      filtered = filtered.filter(event => event.severity === selectedSeverity);
    }

    // Filter by event type
    if (selectedEventType !== 'ALL') {
      filtered = filtered.filter(event => event.type === selectedEventType);
    }

    // Filter by date range
    if (dateRange && dateRange.length === 2) {
      const [startDate, endDate] = dateRange;
      filtered = filtered.filter(event => {
        const eventDate = new Date(event.timestamp);
        return eventDate >= startDate.toDate() && eventDate <= endDate.toDate();
      });
    }

    return filtered;
  }, [securityEvents, selectedSeverity, selectedEventType, dateRange]);

  // Calculate security statistics
  const securityStats = useMemo(() => {
    return securityMonitoring.getSecurityStatistics();
  }, [securityEvents]);

  // Get severity color
  const getSeverityColor = (severity) => {
    switch (severity) {
      case SECURITY_SEVERITY.CRITICAL: return 'red';
      case SECURITY_SEVERITY.HIGH: return 'orange';
      case SECURITY_SEVERITY.MEDIUM: return 'yellow';
      case SECURITY_SEVERITY.LOW: return 'green';
      default: return 'default';
    }
  };

  // Get event type icon
  const getEventTypeIcon = (eventType) => {
    if (eventType.includes('LOGIN') || eventType.includes('AUTH')) {
      return <UserOutlined />;
    } else if (eventType.includes('XSS') || eventType.includes('SQL')) {
      return <WarningOutlined />;
    } else if (eventType.includes('FILE')) {
      return <FileProtectOutlined />;
    } else if (eventType.includes('API')) {
      return <ApiOutlined />;
    }
    return <ShieldOutlined />;
  };

  // Table columns for security events
  const eventColumns = [
    {
      title: 'Time',
      dataIndex: 'timestamp',
      key: 'timestamp',
      width: 150,
      render: (timestamp) => new Date(timestamp).toLocaleString(),
      sorter: (a, b) => new Date(a.timestamp) - new Date(b.timestamp),
      defaultSortOrder: 'descend'
    },
    {
      title: 'Type',
      dataIndex: 'type',
      key: 'type',
      width: 200,
      render: (type) => (
        <Space>
          {getEventTypeIcon(type)}
          <Text code>{type}</Text>
        </Space>
      ),
      filters: Object.values(SECURITY_EVENT_TYPES).map(type => ({
        text: type,
        value: type
      })),
      onFilter: (value, record) => record.type === value
    },
    {
      title: 'Severity',
      dataIndex: 'severity',
      key: 'severity',
      width: 100,
      render: (severity) => (
        <Tag color={getSeverityColor(severity)}>{severity}</Tag>
      ),
      filters: Object.values(SECURITY_SEVERITY).map(severity => ({
        text: severity,
        value: severity
      })),
      onFilter: (value, record) => record.severity === value
    },
    {
      title: 'Component',
      dataIndex: 'component',
      key: 'component',
      width: 150,
      render: (component) => component || 'Unknown'
    },
    {
      title: 'User',
      dataIndex: 'userId',
      key: 'userId',
      width: 120,
      render: (userId) => userId || 'Anonymous'
    },
    {
      title: 'Details',
      key: 'details',
      render: (_, record) => (
        <Tooltip title={JSON.stringify(record, null, 2)}>
          <Button type="link" size="small" icon={<EyeOutlined />}>
            View Details
          </Button>
        </Tooltip>
      )
    }
  ];

  // Recent critical alerts
  const recentCriticalAlerts = criticalAlerts.slice(-5).reverse();

  return (
    <div style={{ padding: 24 }}>
      {/* Header */}
      <Row justify="space-between" align="middle" style={{ marginBottom: 24 }}>
        <Col>
          <Title level={2}>
            <ShieldOutlined style={{ marginRight: 8 }} />
            Security Audit Dashboard
          </Title>
        </Col>
        <Col>
          <Space>
            <Button
              icon={<ReloadOutlined />}
              onClick={loadSecurityData}
              loading={loading}
            >
              Refresh
            </Button>
            <Select
              value={autoRefresh}
              onChange={setAutoRefresh}
              style={{ width: 120 }}
            >
              <Select.Option value={true}>Auto Refresh</Select.Option>
              <Select.Option value={false}>Manual</Select.Option>
            </Select>
          </Space>
        </Col>
      </Row>

      {/* Critical Alerts */}
      {recentCriticalAlerts.length > 0 && (
        <Alert
          message="Critical Security Alerts"
          description={
            <List
              size="small"
              dataSource={recentCriticalAlerts}
              renderItem={alert => (
                <List.Item>
                  <Space>
                    <ExclamationCircleOutlined style={{ color: '#ff4d4f' }} />
                    <Text strong>{alert.type}</Text>
                    <Text type="secondary">
                      {new Date(alert.timestamp).toLocaleString()}
                    </Text>
                    <Text>{alert.component || 'Unknown Component'}</Text>
                  </Space>
                </List.Item>
              )}
            />
          }
          type="error"
          showIcon
          style={{ marginBottom: 24 }}
        />
      )}

      {/* Security Statistics */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Total Events"
              value={securityStats.totalEvents}
              prefix={<ShieldOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Recent Events (24h)"
              value={securityStats.recentEvents}
              prefix={<ClockCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Critical Events"
              value={securityStats.criticalEvents}
              prefix={<ExclamationCircleOutlined />}
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Input Violations"
              value={securityStats.inputViolations}
              prefix={<WarningOutlined />}
              valueStyle={{ color: '#fa8c16' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Security Health Indicators */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} md={12}>
          <Card title="Security Health Score" extra={<SafetyCertificateOutlined />}>
            <div style={{ textAlign: 'center' }}>
              <Progress
                type="circle"
                percent={Math.max(0, 100 - (securityStats.criticalEvents * 10 + securityStats.highSeverityEvents * 5))}
                status={securityStats.criticalEvents > 0 ? 'exception' : 'success'}
                format={percent => `${percent}%`}
              />
              <div style={{ marginTop: 16 }}>
                <Text type="secondary">
                  Based on recent security events and threat levels
                </Text>
              </div>
            </div>
          </Card>
        </Col>
        <Col xs={24} md={12}>
          <Card title="Event Distribution" extra={<Badge count={filteredEvents.length} />}>
            <Row gutter={[8, 8]}>
              <Col span={12}>
                <Text strong>Authentication:</Text>
                <div>{securityStats.authenticationEvents}</div>
              </Col>
              <Col span={12}>
                <Text strong>Input Violations:</Text>
                <div>{securityStats.inputViolations}</div>
              </Col>
              <Col span={12}>
                <Text strong>File Uploads:</Text>
                <div>{securityStats.fileUploadEvents}</div>
              </Col>
              <Col span={12}>
                <Text strong>API Security:</Text>
                <div>{securityStats.apiSecurityEvents}</div>
              </Col>
            </Row>
          </Card>
        </Col>
      </Row>

      {/* Filters */}
      <Card style={{ marginBottom: 24 }}>
        <Row gutter={[16, 16]} align="middle">
          <Col xs={24} sm={8} md={6}>
            <Text strong>Severity:</Text>
            <Select
              value={selectedSeverity}
              onChange={setSelectedSeverity}
              style={{ width: '100%', marginTop: 4 }}
            >
              <Select.Option value="ALL">All Severities</Select.Option>
              {Object.values(SECURITY_SEVERITY).map(severity => (
                <Select.Option key={severity} value={severity}>
                  <Tag color={getSeverityColor(severity)}>{severity}</Tag>
                </Select.Option>
              ))}
            </Select>
          </Col>
          <Col xs={24} sm={8} md={6}>
            <Text strong>Event Type:</Text>
            <Select
              value={selectedEventType}
              onChange={setSelectedEventType}
              style={{ width: '100%', marginTop: 4 }}
            >
              <Select.Option value="ALL">All Types</Select.Option>
              {Object.values(SECURITY_EVENT_TYPES).map(type => (
                <Select.Option key={type} value={type}>
                  {type}
                </Select.Option>
              ))}
            </Select>
          </Col>
          <Col xs={24} sm={8} md={12}>
            <Text strong>Date Range:</Text>
            <RangePicker
              value={dateRange}
              onChange={setDateRange}
              style={{ width: '100%', marginTop: 4 }}
              showTime
            />
          </Col>
        </Row>
      </Card>

      {/* Security Events Table */}
      <Card
        title={`Security Events (${filteredEvents.length})`}
        extra={
          <Space>
            <Text type="secondary">
              Showing {filteredEvents.length} of {securityEvents.length} events
            </Text>
          </Space>
        }
      >
        <Table
          columns={eventColumns}
          dataSource={filteredEvents}
          rowKey="id"
          loading={loading}
          pagination={{
            pageSize: 20,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => 
              `${range[0]}-${range[1]} of ${total} events`
          }}
          scroll={{ x: 1000 }}
          size="small"
        />
      </Card>

      {/* Security Timeline */}
      {filteredEvents.length > 0 && (
        <Card title="Recent Security Timeline" style={{ marginTop: 24 }}>
          <Timeline mode="left">
            {filteredEvents.slice(0, 10).map(event => (
              <Timeline.Item
                key={event.id}
                color={getSeverityColor(event.severity)}
                label={new Date(event.timestamp).toLocaleTimeString()}
              >
                <Space direction="vertical" size="small">
                  <Space>
                    {getEventTypeIcon(event.type)}
                    <Text strong>{event.type}</Text>
                    <Tag color={getSeverityColor(event.severity)}>
                      {event.severity}
                    </Tag>
                  </Space>
                  <Text type="secondary">
                    {event.component} - {event.userId || 'Anonymous'}
                  </Text>
                </Space>
              </Timeline.Item>
            ))}
          </Timeline>
        </Card>
      )}
    </div>
  );
});

SecurityAuditDashboard.displayName = 'SecurityAuditDashboard';

export default SecurityAuditDashboard;