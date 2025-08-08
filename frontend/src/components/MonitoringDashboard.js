/**
 * Comprehensive Monitoring Dashboard
 * 
 * A unified dashboard for monitoring security, performance, and system health.
 * Provides real-time metrics, alerts, and actionable insights.
 * 
 * @component
 * @since 1.0.0
 * @author QRMFG Monitoring Team
 */

import {
  DashboardOutlined,
  SecurityScanOutlined,
  ThunderboltOutlined,
  AlertOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  ClockCircleOutlined,
  ReloadOutlined,
  DownloadOutlined,
  SettingOutlined,
  EyeOutlined,
  BugOutlined
} from '@ant-design/icons';
import {
  Card,
  Row,
  Col,
  Statistic,
  Progress,
  Alert,
  Button,
  Tabs,
  Table,
  Tag,
  Timeline,
  Space,
  Typography,
  Divider,
  Badge,
  List,
  Tooltip,
  Switch,
  Select,
  DatePicker,
  Modal,
  notification
} from 'antd';
import React, { useState, useEffect, useCallback, useMemo } from 'react';

import SecurityAuditDashboard from './SecurityAuditDashboard';
import { securityMonitoring, SECURITY_EVENT_TYPES, SECURITY_SEVERITY } from '../services/securityMonitoring';
import { vulnerabilityScanner } from '../services/vulnerabilityScanner';
import { securityAudit } from '../utils/securityAudit';
import { performanceTesting } from '../utils/performanceTesting';
import apiClient from '../api/client';

const { Title, Text, Paragraph } = Typography;
const { TabPane } = Tabs;
const { RangePicker } = DatePicker;

/**
 * Main Monitoring Dashboard Component
 */
const MonitoringDashboard = React.memo(() => {
  // State management
  const [loading, setLoading] = useState(false);
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [refreshInterval, setRefreshInterval] = useState(30000); // 30 seconds
  const [selectedTimeRange, setSelectedTimeRange] = useState('24h');
  const [activeTab, setActiveTab] = useState('overview');
  
  // Data state
  const [systemHealth, setSystemHealth] = useState({});
  const [securityMetrics, setSecurityMetrics] = useState({});
  const [performanceMetrics, setPerformanceMetrics] = useState({});
  const [recentAlerts, setRecentAlerts] = useState([]);
  const [auditResults, setAuditResults] = useState(null);
  const [performanceResults, setPerformanceResults] = useState(null);

  // Load dashboard data
  const loadDashboardData = useCallback(async () => {
    setLoading(true);
    try {
      // Load security metrics
      const securityStats = securityMonitoring.getSecurityStatistics();
      const criticalAlerts = securityMonitoring.getCriticalAlerts();
      
      setSecurityMetrics(securityStats);
      setRecentAlerts(criticalAlerts.slice(-10));

      // Load performance metrics
      const cacheStats = apiClient.getCacheStats();
      setPerformanceMetrics({
        cacheHitRate: cacheStats.hitRate,
        totalRequests: cacheStats.totalRequests,
        cacheSize: cacheStats.size,
        memoryUsage: performance.memory ? 
          Math.round(performance.memory.usedJSHeapSize / 1024 / 1024) : 0
      });

      // Calculate system health score
      const healthScore = calculateSystemHealthScore(securityStats, cacheStats);
      setSystemHealth({
        score: healthScore,
        status: healthScore > 80 ? 'healthy' : healthScore > 60 ? 'warning' : 'critical',
        lastUpdated: new Date().toISOString()
      });

    } catch (error) {
      console.error('Failed to load dashboard data:', error);
      notification.error({
        message: 'Dashboard Error',
        description: 'Failed to load monitoring data'
      });
    } finally {
      setLoading(false);
    }
  }, []);

  // Auto-refresh functionality
  useEffect(() => {
    loadDashboardData();
    
    if (autoRefresh) {
      const interval = setInterval(loadDashboardData, refreshInterval);
      return () => clearInterval(interval);
    }
  }, [loadDashboardData, autoRefresh, refreshInterval]);

  // Calculate system health score
  const calculateSystemHealthScore = (securityStats, cacheStats) => {
    let score = 100;
    
    // Security penalties
    score -= securityStats.criticalEvents * 20;
    score -= securityStats.highSeverityEvents * 10;
    score -= securityStats.inputViolations * 5;
    
    // Performance bonuses/penalties
    if (cacheStats.hitRate > 0.8) score += 5;
    else if (cacheStats.hitRate < 0.5) score -= 10;
    
    // Memory usage penalty
    if (performanceMetrics.memoryUsage > 50) score -= 15;
    else if (performanceMetrics.memoryUsage > 25) score -= 5;
    
    return Math.max(0, Math.min(100, Math.round(score)));
  };

  // Run security audit
  const runSecurityAudit = useCallback(async () => {
    setLoading(true);
    try {
      const results = await securityAudit.runSecurityAudit();
      setAuditResults(results);
      
      notification.success({
        message: 'Security Audit Complete',
        description: `Completed ${results.summary.total} tests with ${results.summary.passed} passed`
      });
    } catch (error) {
      notification.error({
        message: 'Security Audit Failed',
        description: error.message
      });
    } finally {
      setLoading(false);
    }
  }, []);

  // Run performance tests
  const runPerformanceTests = useCallback(async () => {
    setLoading(true);
    try {
      const results = await performanceTesting.runPerformanceTests();
      setPerformanceResults(results);
      
      notification.success({
        message: 'Performance Tests Complete',
        description: `Overall score: ${results.summary.overallScore}%`
      });
    } catch (error) {
      notification.error({
        message: 'Performance Tests Failed',
        description: error.message
      });
    } finally {
      setLoading(false);
    }
  }, []);

  // Run vulnerability scan
  const runVulnerabilityScanner = useCallback(async () => {
    setLoading(true);
    try {
      const results = await vulnerabilityScanner.runComprehensiveScan();
      
      notification.success({
        message: 'Vulnerability Scan Complete',
        description: `Found ${results.summary.totalVulnerabilities} vulnerabilities`
      });
    } catch (error) {
      notification.error({
        message: 'Vulnerability Scan Failed',
        description: error.message
      });
    } finally {
      setLoading(false);
    }
  }, []);

  // Export monitoring data
  const exportMonitoringData = useCallback(() => {
    const data = {
      timestamp: new Date().toISOString(),
      systemHealth,
      securityMetrics,
      performanceMetrics,
      recentAlerts,
      auditResults: auditResults ? {
        summary: auditResults.summary,
        recommendations: auditResults.getRecommendations()
      } : null,
      performanceResults: performanceResults ? {
        summary: performanceResults.summary,
        recommendations: performanceResults.getRecommendations()
      } : null
    };

    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `monitoring-report-${new Date().toISOString().split('T')[0]}.json`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }, [systemHealth, securityMetrics, performanceMetrics, recentAlerts, auditResults, performanceResults]);

  // System health status color
  const getHealthStatusColor = (status) => {
    switch (status) {
      case 'healthy': return '#52c41a';
      case 'warning': return '#faad14';
      case 'critical': return '#ff4d4f';
      default: return '#d9d9d9';
    }
  };

  // Recent alerts for timeline
  const alertsTimeline = useMemo(() => {
    return recentAlerts.slice(0, 5).map(alert => ({
      color: alert.severity === 'CRITICAL' ? 'red' : 'orange',
      children: (
        <div>
          <Text strong>{alert.type}</Text>
          <br />
          <Text type="secondary">{new Date(alert.timestamp).toLocaleString()}</Text>
          <br />
          <Text>{alert.component || 'System'}</Text>
        </div>
      )
    }));
  }, [recentAlerts]);

  return (
    <div style={{ padding: 24 }}>
      {/* Header */}
      <Row justify="space-between" align="middle" style={{ marginBottom: 24 }}>
        <Col>
          <Title level={2}>
            <DashboardOutlined style={{ marginRight: 8 }} />
            System Monitoring Dashboard
          </Title>
        </Col>
        <Col>
          <Space>
            <Switch
              checked={autoRefresh}
              onChange={setAutoRefresh}
              checkedChildren="Auto"
              unCheckedChildren="Manual"
            />
            <Select
              value={refreshInterval}
              onChange={setRefreshInterval}
              style={{ width: 120 }}
              disabled={!autoRefresh}
            >
              <Select.Option value={10000}>10s</Select.Option>
              <Select.Option value={30000}>30s</Select.Option>
              <Select.Option value={60000}>1m</Select.Option>
              <Select.Option value={300000}>5m</Select.Option>
            </Select>
            <Button
              icon={<ReloadOutlined />}
              onClick={loadDashboardData}
              loading={loading}
            >
              Refresh
            </Button>
            <Button
              icon={<DownloadOutlined />}
              onClick={exportMonitoringData}
            >
              Export
            </Button>
          </Space>
        </Col>
      </Row>

      {/* System Health Overview */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="System Health"
              value={systemHealth.score || 0}
              suffix="%"
              prefix={
                systemHealth.status === 'healthy' ? <CheckCircleOutlined /> :
                systemHealth.status === 'warning' ? <ExclamationCircleOutlined /> :
                <AlertOutlined />
              }
              valueStyle={{ color: getHealthStatusColor(systemHealth.status) }}
            />
            <Progress
              percent={systemHealth.score || 0}
              strokeColor={getHealthStatusColor(systemHealth.status)}
              showInfo={false}
              size="small"
              style={{ marginTop: 8 }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Security Events"
              value={securityMetrics.recentEvents || 0}
              prefix={<SecurityScanOutlined />}
              valueStyle={{ color: securityMetrics.criticalEvents > 0 ? '#ff4d4f' : '#52c41a' }}
            />
            <Text type="secondary" style={{ fontSize: 12 }}>
              {securityMetrics.criticalEvents || 0} critical
            </Text>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Cache Hit Rate"
              value={Math.round((performanceMetrics.cacheHitRate || 0) * 100)}
              suffix="%"
              prefix={<ThunderboltOutlined />}
              valueStyle={{ 
                color: (performanceMetrics.cacheHitRate || 0) > 0.8 ? '#52c41a' : 
                       (performanceMetrics.cacheHitRate || 0) > 0.5 ? '#faad14' : '#ff4d4f' 
              }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Memory Usage"
              value={performanceMetrics.memoryUsage || 0}
              suffix="MB"
              prefix={<ClockCircleOutlined />}
              valueStyle={{ 
                color: (performanceMetrics.memoryUsage || 0) > 50 ? '#ff4d4f' : 
                       (performanceMetrics.memoryUsage || 0) > 25 ? '#faad14' : '#52c41a' 
              }}
            />
          </Card>
        </Col>
      </Row>

      {/* Critical Alerts */}
      {recentAlerts.length > 0 && (
        <Alert
          message="Recent Critical Alerts"
          description={
            <List
              size="small"
              dataSource={recentAlerts.slice(0, 3)}
              renderItem={alert => (
                <List.Item>
                  <Space>
                    <Tag color="red">CRITICAL</Tag>
                    <Text strong>{alert.type}</Text>
                    <Text type="secondary">
                      {new Date(alert.timestamp).toLocaleString()}
                    </Text>
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

      {/* Main Dashboard Tabs */}
      <Tabs activeKey={activeTab} onChange={setActiveTab}>
        {/* Overview Tab */}
        <TabPane tab="Overview" key="overview">
          <Row gutter={[16, 16]}>
            <Col xs={24} lg={16}>
              <Card title="Quick Actions" extra={<SettingOutlined />}>
                <Row gutter={[16, 16]}>
                  <Col xs={24} sm={12} md={6}>
                    <Button
                      type="primary"
                      icon={<SecurityScanOutlined />}
                      onClick={runSecurityAudit}
                      loading={loading}
                      block
                    >
                      Security Audit
                    </Button>
                  </Col>
                  <Col xs={24} sm={12} md={6}>
                    <Button
                      type="primary"
                      icon={<ThunderboltOutlined />}
                      onClick={runPerformanceTests}
                      loading={loading}
                      block
                    >
                      Performance Test
                    </Button>
                  </Col>
                  <Col xs={24} sm={12} md={6}>
                    <Button
                      type="primary"
                      icon={<BugOutlined />}
                      onClick={runVulnerabilityScanner}
                      loading={loading}
                      block
                    >
                      Vulnerability Scan
                    </Button>
                  </Col>
                  <Col xs={24} sm={12} md={6}>
                    <Button
                      icon={<EyeOutlined />}
                      onClick={() => setActiveTab('security')}
                      block
                    >
                      View Details
                    </Button>
                  </Col>
                </Row>
              </Card>

              {/* System Metrics */}
              <Card title="System Metrics" style={{ marginTop: 16 }}>
                <Row gutter={[16, 16]}>
                  <Col xs={24} md={12}>
                    <Card size="small" title="Security Metrics">
                      <Row gutter={[8, 8]}>
                        <Col span={12}>
                          <Text strong>Total Events:</Text>
                          <div>{securityMetrics.totalEvents || 0}</div>
                        </Col>
                        <Col span={12}>
                          <Text strong>Auth Events:</Text>
                          <div>{securityMetrics.authenticationEvents || 0}</div>
                        </Col>
                        <Col span={12}>
                          <Text strong>Input Violations:</Text>
                          <div>{securityMetrics.inputViolations || 0}</div>
                        </Col>
                        <Col span={12}>
                          <Text strong>API Events:</Text>
                          <div>{securityMetrics.apiSecurityEvents || 0}</div>
                        </Col>
                      </Row>
                    </Card>
                  </Col>
                  <Col xs={24} md={12}>
                    <Card size="small" title="Performance Metrics">
                      <Row gutter={[8, 8]}>
                        <Col span={12}>
                          <Text strong>Cache Size:</Text>
                          <div>{performanceMetrics.cacheSize || 0} items</div>
                        </Col>
                        <Col span={12}>
                          <Text strong>Total Requests:</Text>
                          <div>{performanceMetrics.totalRequests || 0}</div>
                        </Col>
                        <Col span={24}>
                          <Text strong>Cache Hit Rate:</Text>
                          <Progress
                            percent={Math.round((performanceMetrics.cacheHitRate || 0) * 100)}
                            size="small"
                            style={{ marginTop: 4 }}
                          />
                        </Col>
                      </Row>
                    </Card>
                  </Col>
                </Row>
              </Card>
            </Col>

            <Col xs={24} lg={8}>
              <Card title="Recent Alerts" extra={<Badge count={recentAlerts.length} />}>
                {alertsTimeline.length > 0 ? (
                  <Timeline items={alertsTimeline} />
                ) : (
                  <div style={{ textAlign: 'center', padding: 20 }}>
                    <CheckCircleOutlined style={{ fontSize: 48, color: '#52c41a' }} />
                    <div style={{ marginTop: 8 }}>No recent alerts</div>
                  </div>
                )}
              </Card>

              <Card title="System Status" style={{ marginTop: 16 }}>
                <List
                  size="small"
                  dataSource={[
                    { name: 'Security Monitoring', status: 'active', color: 'green' },
                    { name: 'Performance Tracking', status: 'active', color: 'green' },
                    { name: 'Error Boundaries', status: 'active', color: 'green' },
                    { name: 'Input Validation', status: 'active', color: 'green' },
                    { name: 'API Security', status: 'active', color: 'green' }
                  ]}
                  renderItem={item => (
                    <List.Item>
                      <Space>
                        <Badge color={item.color} />
                        <Text>{item.name}</Text>
                        <Tag color={item.color}>{item.status}</Tag>
                      </Space>
                    </List.Item>
                  )}
                />
              </Card>
            </Col>
          </Row>
        </TabPane>

        {/* Security Tab */}
        <TabPane tab="Security" key="security">
          <SecurityAuditDashboard />
          
          {auditResults && (
            <Card title="Latest Security Audit Results" style={{ marginTop: 16 }}>
              <Row gutter={[16, 16]}>
                <Col xs={24} md={12}>
                  <Statistic
                    title="Overall Score"
                    value={auditResults.getOverallScore()}
                    suffix="%"
                    valueStyle={{ 
                      color: auditResults.getOverallScore() > 80 ? '#52c41a' : 
                             auditResults.getOverallScore() > 60 ? '#faad14' : '#ff4d4f' 
                    }}
                  />
                </Col>
                <Col xs={24} md={12}>
                  <Statistic
                    title="Tests Passed"
                    value={auditResults.summary.passed}
                    suffix={`/ ${auditResults.summary.total}`}
                    valueStyle={{ color: '#52c41a' }}
                  />
                </Col>
              </Row>
              
              <Divider />
              
              <Title level={4}>Top Recommendations</Title>
              <List
                dataSource={auditResults.getRecommendations()}
                renderItem={rec => (
                  <List.Item>
                    <List.Item.Meta
                      avatar={<Tag color={rec.severity === 'CRITICAL' ? 'red' : 'orange'}>{rec.severity}</Tag>}
                      title={rec.title}
                      description={rec.recommendation}
                    />
                  </List.Item>
                )}
              />
            </Card>
          )}
        </TabPane>

        {/* Performance Tab */}
        <TabPane tab="Performance" key="performance">
          {performanceResults && (
            <Card title="Performance Test Results">
              <Row gutter={[16, 16]}>
                <Col xs={24} md={8}>
                  <Statistic
                    title="Overall Score"
                    value={performanceResults.summary.overallScore}
                    suffix="%"
                    valueStyle={{ 
                      color: performanceResults.summary.overallScore > 80 ? '#52c41a' : 
                             performanceResults.summary.overallScore > 60 ? '#faad14' : '#ff4d4f' 
                    }}
                  />
                </Col>
                <Col xs={24} md={8}>
                  <Statistic
                    title="Tests Passed"
                    value={performanceResults.summary.passed}
                    suffix={`/ ${performanceResults.summary.total}`}
                    valueStyle={{ color: '#52c41a' }}
                  />
                </Col>
                <Col xs={24} md={8}>
                  <Statistic
                    title="Warnings"
                    value={performanceResults.summary.warnings}
                    valueStyle={{ color: '#faad14' }}
                  />
                </Col>
              </Row>

              <Divider />

              <Title level={4}>Performance Metrics</Title>
              <Row gutter={[16, 16]}>
                {Object.entries(performanceResults.metrics).map(([key, metric]) => (
                  <Col xs={24} sm={12} md={8} key={key}>
                    <Card size="small">
                      <Statistic
                        title={key}
                        value={metric.value}
                        suffix={metric.unit}
                        valueStyle={{ 
                          color: metric.status === 'GOOD' ? '#52c41a' : '#ff4d4f',
                          fontSize: 16
                        }}
                      />
                      {metric.threshold && (
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          Threshold: {metric.threshold}{metric.unit}
                        </Text>
                      )}
                    </Card>
                  </Col>
                ))}
              </Row>

              <Divider />

              <Title level={4}>Recommendations</Title>
              <List
                dataSource={performanceResults.getRecommendations()}
                renderItem={rec => (
                  <List.Item>
                    <List.Item.Meta
                      avatar={<Tag color={rec.priority === 'HIGH' ? 'red' : 'orange'}>{rec.priority}</Tag>}
                      title={rec.title}
                      description={
                        <div>
                          <div>{rec.recommendation}</div>
                          <Text type="secondary" style={{ fontSize: 12 }}>
                            Impact: {rec.impact}
                          </Text>
                        </div>
                      }
                    />
                  </List.Item>
                )}
              />
            </Card>
          )}

          {!performanceResults && (
            <Card>
              <div style={{ textAlign: 'center', padding: 40 }}>
                <ThunderboltOutlined style={{ fontSize: 48, color: '#d9d9d9' }} />
                <div style={{ marginTop: 16 }}>
                  <Title level={4}>No Performance Data</Title>
                  <Paragraph>Run performance tests to see detailed metrics and recommendations.</Paragraph>
                  <Button
                    type="primary"
                    icon={<ThunderboltOutlined />}
                    onClick={runPerformanceTests}
                    loading={loading}
                  >
                    Run Performance Tests
                  </Button>
                </div>
              </div>
            </Card>
          )}
        </TabPane>

        {/* System Info Tab */}
        <TabPane tab="System Info" key="system">
          <Row gutter={[16, 16]}>
            <Col xs={24} md={12}>
              <Card title="Environment Information">
                <List
                  size="small"
                  dataSource={[
                    { label: 'Node Environment', value: process.env.NODE_ENV || 'development' },
                    { label: 'User Agent', value: navigator.userAgent.substring(0, 50) + '...' },
                    { label: 'Screen Resolution', value: `${screen.width}x${screen.height}` },
                    { label: 'Viewport Size', value: `${window.innerWidth}x${window.innerHeight}` },
                    { label: 'Language', value: navigator.language },
                    { label: 'Platform', value: navigator.platform },
                    { label: 'Online Status', value: navigator.onLine ? 'Online' : 'Offline' }
                  ]}
                  renderItem={item => (
                    <List.Item>
                      <List.Item.Meta
                        title={item.label}
                        description={item.value}
                      />
                    </List.Item>
                  )}
                />
              </Card>
            </Col>
            <Col xs={24} md={12}>
              <Card title="Performance API Support">
                <List
                  size="small"
                  dataSource={[
                    { feature: 'Navigation Timing', supported: !!performance.getEntriesByType },
                    { feature: 'Resource Timing', supported: !!performance.getEntriesByType },
                    { feature: 'Memory API', supported: !!performance.memory },
                    { feature: 'Observer API', supported: !!window.PerformanceObserver },
                    { feature: 'Paint Timing', supported: !!performance.getEntriesByType },
                    { feature: 'Long Tasks', supported: !!window.PerformanceObserver }
                  ]}
                  renderItem={item => (
                    <List.Item>
                      <Space>
                        <Text>{item.feature}</Text>
                        <Tag color={item.supported ? 'green' : 'red'}>
                          {item.supported ? 'Supported' : 'Not Supported'}
                        </Tag>
                      </Space>
                    </List.Item>
                  )}
                />
              </Card>
            </Col>
          </Row>
        </TabPane>
      </Tabs>
    </div>
  );
});

MonitoringDashboard.displayName = 'MonitoringDashboard';

export default MonitoringDashboard;