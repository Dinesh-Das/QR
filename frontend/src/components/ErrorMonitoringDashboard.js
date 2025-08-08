import {
  BugOutlined,
  ExclamationCircleOutlined,
  WarningOutlined,
  InfoCircleOutlined,
  ReloadOutlined,
  DownloadOutlined,
  FilterOutlined
} from '@ant-design/icons';
import {
  Card,
  Row,
  Col,
  Statistic,
  Table,
  Tag,
  Button,
  Select,
  DatePicker,
  Alert,
  Progress,
  Typography,
  Space,
  Tooltip,
  Badge
} from 'antd';
import React, { useState, useEffect, useCallback } from 'react';

import errorReportingService, { ERROR_SEVERITY, ERROR_CATEGORY } from '../services/errorReporting';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;
const { Option } = Select;

/**
 * Error Monitoring Dashboard Component
 * Displays error statistics, trends, and detailed error reports
 */
const ErrorMonitoringDashboard = () => {
  const [loading, setLoading] = useState(false);
  const [errorStats, setErrorStats] = useState({
    total: 0,
    critical: 0,
    high: 0,
    medium: 0,
    low: 0,
    byCategory: {},
    byType: {},
    trends: []
  });
  const [errorReports, setErrorReports] = useState([]);
  const [filters, setFilters] = useState({
    severity: 'all',
    category: 'all',
    dateRange: null,
    search: ''
  });

  useEffect(() => {
    loadErrorData();
  }, [loadErrorData]);

  const loadErrorData = useCallback(async () => {
    setLoading(true);
    try {
      // In a real implementation, this would fetch from an API
      // For now, we'll show the current session stats
      const stats = errorReportingService.getErrorStats();
      setErrorStats({
        total: stats.reportCount,
        critical: Math.floor(stats.reportCount * 0.1),
        high: Math.floor(stats.reportCount * 0.2),
        medium: Math.floor(stats.reportCount * 0.5),
        low: Math.floor(stats.reportCount * 0.2),
        byCategory: {
          [ERROR_CATEGORY.APPLICATION]: Math.floor(stats.reportCount * 0.1),
          [ERROR_CATEGORY.COMPONENT]: Math.floor(stats.reportCount * 0.4),
          [ERROR_CATEGORY.API]: Math.floor(stats.reportCount * 0.3),
          [ERROR_CATEGORY.ROUTE]: Math.floor(stats.reportCount * 0.2)
        },
        byType: {
          javascript: Math.floor(stats.reportCount * 0.6),
          api: Math.floor(stats.reportCount * 0.3),
          network: Math.floor(stats.reportCount * 0.1)
        },
        trends: generateMockTrends()
      });

      // Mock error reports for demonstration
      setErrorReports(generateMockErrorReports(stats.reportCount));
    } catch (error) {
      console.error('Failed to load error data:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  const generateMockTrends = () => {
    const trends = [];
    const now = new Date();
    for (let i = 6; i >= 0; i--) {
      const date = new Date(now);
      date.setDate(date.getDate() - i);
      trends.push({
        date: date.toISOString().split('T')[0],
        errors: Math.floor(Math.random() * 20) + 5
      });
    }
    return trends;
  };

  const generateMockErrorReports = count => {
    const reports = [];
    const categories = Object.values(ERROR_CATEGORY);
    const severities = Object.values(ERROR_SEVERITY);

    for (let i = 0; i < Math.min(count, 50); i++) {
      reports.push({
        id: `error_${i}`,
        message: `Sample error message ${i}`,
        category: categories[Math.floor(Math.random() * categories.length)],
        severity: severities[Math.floor(Math.random() * severities.length)],
        timestamp: new Date(Date.now() - Math.random() * 86400000).toISOString(),
        userContext: {
          userId: `user_${Math.floor(Math.random() * 100)}`,
          username: `user${Math.floor(Math.random() * 100)}`
        },
        systemContext: {
          url: window.location.href,
          userAgent: `${navigator.userAgent.substring(0, 50)  }...`
        },
        retryCount: Math.floor(Math.random() * 3)
      });
    }

    return reports.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
  };

  const getSeverityColor = severity => {
    const colors = {
      [ERROR_SEVERITY.CRITICAL]: '#ff4d4f',
      [ERROR_SEVERITY.HIGH]: '#fa8c16',
      [ERROR_SEVERITY.MEDIUM]: '#faad14',
      [ERROR_SEVERITY.LOW]: '#52c41a'
    };
    return colors[severity] || '#d9d9d9';
  };

  const getSeverityIcon = severity => {
    const icons = {
      [ERROR_SEVERITY.CRITICAL]: <BugOutlined />,
      [ERROR_SEVERITY.HIGH]: <ExclamationCircleOutlined />,
      [ERROR_SEVERITY.MEDIUM]: <WarningOutlined />,
      [ERROR_SEVERITY.LOW]: <InfoCircleOutlined />
    };
    return icons[severity] || <InfoCircleOutlined />;
  };

  const getCategoryColor = category => {
    const colors = {
      [ERROR_CATEGORY.APPLICATION]: 'red',
      [ERROR_CATEGORY.ROUTE]: 'orange',
      [ERROR_CATEGORY.COMPONENT]: 'blue',
      [ERROR_CATEGORY.API]: 'green',
      [ERROR_CATEGORY.ASYNC]: 'purple',
      [ERROR_CATEGORY.AUTHENTICATION]: 'magenta'
    };
    return colors[category] || 'default';
  };

  const columns = [
    {
      title: 'Error ID',
      dataIndex: 'id',
      key: 'id',
      width: 120,
      render: id => <Text code>{id.substring(0, 12)}...</Text>
    },
    {
      title: 'Message',
      dataIndex: 'message',
      key: 'message',
      ellipsis: true,
      render: message => (
        <Tooltip title={message}>
          <Text>{message}</Text>
        </Tooltip>
      )
    },
    {
      title: 'Severity',
      dataIndex: 'severity',
      key: 'severity',
      width: 100,
      render: severity => (
        <Tag color={getSeverityColor(severity)} icon={getSeverityIcon(severity)}>
          {severity.toUpperCase()}
        </Tag>
      )
    },
    {
      title: 'Category',
      dataIndex: 'category',
      key: 'category',
      width: 120,
      render: category => <Tag color={getCategoryColor(category)}>{category.toUpperCase()}</Tag>
    },
    {
      title: 'User',
      dataIndex: ['userContext', 'username'],
      key: 'user',
      width: 100
    },
    {
      title: 'Retries',
      dataIndex: 'retryCount',
      key: 'retryCount',
      width: 80,
      render: count => (
        <Badge
          count={count}
          showZero
          style={{ backgroundColor: count > 0 ? '#faad14' : '#d9d9d9' }}
        />
      )
    },
    {
      title: 'Timestamp',
      dataIndex: 'timestamp',
      key: 'timestamp',
      width: 160,
      render: timestamp => new Date(timestamp).toLocaleString()
    }
  ];

  const filteredReports = errorReports.filter(report => {
    if (filters.severity !== 'all' && report.severity !== filters.severity) {
      return false;
    }
    if (filters.category !== 'all' && report.category !== filters.category) {
      return false;
    }
    if (filters.search && !report.message.toLowerCase().includes(filters.search.toLowerCase())) {
      return false;
    }
    return true;
  });

  const errorRate =
    errorStats.total > 0 ? ((errorStats.critical + errorStats.high) / errorStats.total) * 100 : 0;

  return (
    <div style={{ padding: '24px' }}>
      <div style={{ marginBottom: '24px' }}>
        <Title level={2}>
          <BugOutlined /> Error Monitoring Dashboard
        </Title>
        <Text type="secondary">
          Monitor application errors, track trends, and analyze error patterns
        </Text>
      </div>

      {/* Error Statistics */}
      <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Total Errors"
              value={errorStats.total}
              prefix={<BugOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Critical Errors"
              value={errorStats.critical}
              prefix={<ExclamationCircleOutlined />}
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Error Rate"
              value={errorRate}
              suffix="%"
              precision={1}
              prefix={<WarningOutlined />}
              valueStyle={{ color: errorRate > 10 ? '#ff4d4f' : '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <div style={{ textAlign: 'center' }}>
              <Text strong>System Health</Text>
              <Progress
                type="circle"
                percent={Math.max(0, 100 - errorRate)}
                status={errorRate > 20 ? 'exception' : errorRate > 10 ? 'active' : 'success'}
                size={80}
                style={{ marginTop: '8px' }}
              />
            </div>
          </Card>
        </Col>
      </Row>

      {/* Error Categories */}
      <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
        <Col xs={24} lg={12}>
          <Card title="Errors by Category" size="small">
            <Row gutter={[8, 8]}>
              {Object.entries(errorStats.byCategory).map(([category, count]) => (
                <Col span={12} key={category}>
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center'
                    }}
                  >
                    <Tag color={getCategoryColor(category)} style={{ margin: 0 }}>
                      {category.toUpperCase()}
                    </Tag>
                    <Text strong>{count}</Text>
                  </div>
                </Col>
              ))}
            </Row>
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="Errors by Severity" size="small">
            <Row gutter={[8, 8]}>
              {[
                { key: 'critical', label: 'Critical', count: errorStats.critical },
                { key: 'high', label: 'High', count: errorStats.high },
                { key: 'medium', label: 'Medium', count: errorStats.medium },
                { key: 'low', label: 'Low', count: errorStats.low }
              ].map(({ key, label, count }) => (
                <Col span={12} key={key}>
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center'
                    }}
                  >
                    <Tag
                      color={getSeverityColor(key)}
                      icon={getSeverityIcon(key)}
                      style={{ margin: 0 }}
                    >
                      {label}
                    </Tag>
                    <Text strong>{count}</Text>
                  </div>
                </Col>
              ))}
            </Row>
          </Card>
        </Col>
      </Row>

      {/* Alerts */}
      {errorRate > 20 && (
        <Alert
          message="High Error Rate Detected"
          description={`Current error rate is ${errorRate.toFixed(1)}%. Consider investigating critical issues.`}
          type="error"
          showIcon
          style={{ marginBottom: '24px' }}
        />
      )}

      {errorStats.critical > 5 && (
        <Alert
          message="Multiple Critical Errors"
          description={`${errorStats.critical} critical errors detected. Immediate attention required.`}
          type="warning"
          showIcon
          style={{ marginBottom: '24px' }}
        />
      )}

      {/* Filters */}
      <Card size="small" style={{ marginBottom: '16px' }}>
        <Row gutter={[16, 8]} align="middle">
          <Col xs={24} sm={6}>
            <Select
              placeholder="Filter by Severity"
              value={filters.severity}
              onChange={value => setFilters({ ...filters, severity: value })}
              style={{ width: '100%' }}
            >
              <Option value="all">All Severities</Option>
              <Option value={ERROR_SEVERITY.CRITICAL}>Critical</Option>
              <Option value={ERROR_SEVERITY.HIGH}>High</Option>
              <Option value={ERROR_SEVERITY.MEDIUM}>Medium</Option>
              <Option value={ERROR_SEVERITY.LOW}>Low</Option>
            </Select>
          </Col>
          <Col xs={24} sm={6}>
            <Select
              placeholder="Filter by Category"
              value={filters.category}
              onChange={value => setFilters({ ...filters, category: value })}
              style={{ width: '100%' }}
            >
              <Option value="all">All Categories</Option>
              {Object.values(ERROR_CATEGORY).map(category => (
                <Option key={category} value={category}>
                  {category.charAt(0).toUpperCase() + category.slice(1)}
                </Option>
              ))}
            </Select>
          </Col>
          <Col xs={24} sm={8}>
            <RangePicker
              placeholder={['Start Date', 'End Date']}
              onChange={dates => setFilters({ ...filters, dateRange: dates })}
              style={{ width: '100%' }}
            />
          </Col>
          <Col xs={24} sm={4}>
            <Space>
              <Button icon={<ReloadOutlined />} onClick={loadErrorData} loading={loading}>
                Refresh
              </Button>
              <Button
                icon={<FilterOutlined />}
                onClick={() =>
                  setFilters({
                    severity: 'all',
                    category: 'all',
                    dateRange: null,
                    search: ''
                  })
                }
              >
                Clear
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      {/* Error Reports Table */}
      <Card
        title={
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>Recent Error Reports</span>
            <Button
              icon={<DownloadOutlined />}
              size="small"
              onClick={() => {
                // Export functionality would be implemented here
                console.log('Exporting error reports...');
              }}
            >
              Export
            </Button>
          </div>
        }
      >
        <Table
          dataSource={filteredReports}
          columns={columns}
          loading={loading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => `${range[0]}-${range[1]} of ${total} errors`
          }}
          rowKey="id"
          size="small"
          scroll={{ x: 800 }}
        />
      </Card>
    </div>
  );
};

export default ErrorMonitoringDashboard;
