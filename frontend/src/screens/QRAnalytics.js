import {
  BarChartOutlined,
  DownloadOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  QuestionCircleOutlined,
  QrcodeOutlined
} from '@ant-design/icons';
import {
  Row,
  Col,
  Card,
  Statistic,
  Table,
  DatePicker,
  Button,
  Spin,
  Tabs,
  Typography,
  message,
  Progress,
  Select
} from 'antd';
import { Chart, registerables } from 'chart.js';
import React, { useState, useEffect, useCallback } from 'react';
import { Bar, Line, Pie } from 'react-chartjs-2';

import apiClient from '../api/client';
import { useRoleBasedAccess } from '../hooks/useRoleBasedAccess';


// Register Chart.js components
Chart.register(...registerables);

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;
const { TabPane } = Tabs;

const QRAnalytics = () => {
  const [loading, setLoading] = useState(true);
  const [dashboardData, setDashboardData] = useState(null);
  const [dateRange, setDateRange] = useState([null, null]);
  const [slaReport, setSlaReport] = useState(null);
  const [slaLoading, setSlaLoading] = useState(false);
  const [exportLoading, setExportLoading] = useState(false);
  const [bottlenecks, setBottlenecks] = useState(null);
  const [bottlenecksLoading, setBottlenecksLoading] = useState(false);
  const [performanceMetrics, setPerformanceMetrics] = useState(null);
  const [performanceLoading, setPerformanceLoading] = useState(false);
  const [queryStatusBreakdown, setQueryStatusBreakdown] = useState(null);
  const [queryStatusLoading, setQueryStatusLoading] = useState(false);
  const [performanceRankings, setPerformanceRankings] = useState(null);
  const [rankingsLoading, setRankingsLoading] = useState(false);
  const [selectedPlant, setSelectedPlant] = useState(null);

  // Role-based access control
  const { 
    isAdmin, 
    isPlantUser, 
    primaryPlant
  } = useRoleBasedAccess();

  const fetchDashboardData = useCallback(async (signal) => {
    setLoading(true);
    try {
      const startDate = dateRange[0]?.toISOString();
      const endDate = dateRange[1]?.toISOString();
      const plantCode = isPlantUser && !isAdmin ? primaryPlant : selectedPlant;
      
      const response = await apiClient.get(`/test-analytics/dashboard${startDate || endDate || plantCode ? '?' : ''}${startDate ? `startDate=${startDate}&` : ''}${endDate ? `endDate=${endDate}&` : ''}${plantCode ? `plantCode=${plantCode}` : ''}`.replace(/&$/, ''));
      if (!signal?.aborted) {
        setDashboardData(response);
      }
    } catch (error) {
      if (!signal?.aborted) {
        console.error('Failed to fetch QR Analytics dashboard data:', error);
        message.error('Failed to load dashboard data. Please check your connection and try again.');
        setDashboardData(null);
      }
    } finally {
      if (!signal?.aborted) {
        setLoading(false);
      }
    }
  }, [dateRange, isAdmin, isPlantUser, primaryPlant, selectedPlant]);

  const fetchSlaReport = async () => {
    setSlaLoading(true);
    try {
      const startDate = dateRange[0]?.toISOString();
      const endDate = dateRange[1]?.toISOString();
      const plantCode = isPlantUser && !isAdmin ? primaryPlant : null;
      
      const response = await apiClient.get(`/test-analytics/sla-metrics${startDate || endDate || plantCode ? '?' : ''}${startDate ? `startDate=${startDate}&` : ''}${endDate ? `endDate=${endDate}&` : ''}${plantCode ? `plantCode=${plantCode}` : ''}`.replace(/&$/, ''));
      setSlaReport(response);
    } catch (error) {
      console.error('Failed to fetch SLA metrics:', error);
      message.error('Failed to load SLA report. Please check your connection and try again.');
      setSlaReport(null);
    } finally {
      setSlaLoading(false);
    }
  };

  const fetchBottlenecks = useCallback(async (signal) => {
    setBottlenecksLoading(true);
    try {
      const startDate = dateRange[0]?.toISOString();
      const endDate = dateRange[1]?.toISOString();
      const plantCode = isPlantUser && !isAdmin ? primaryPlant : null;
      
      const response = await apiClient.get(`/test-analytics/bottlenecks${startDate || endDate || plantCode ? '?' : ''}${startDate ? `startDate=${startDate}&` : ''}${endDate ? `endDate=${endDate}&` : ''}${plantCode ? `plantCode=${plantCode}` : ''}`.replace(/&$/, ''));
      if (!signal?.aborted) {
        setBottlenecks(response);
      }
    } catch (error) {
      if (!signal?.aborted) {
        console.error('Failed to fetch bottlenecks data:', error);
        message.error('Failed to load bottlenecks analysis. Please check your connection and try again.');
        setBottlenecks(null);
      }
    } finally {
      if (!signal?.aborted) {
        setBottlenecksLoading(false);
      }
    }
  }, [dateRange, isAdmin, isPlantUser, primaryPlant]);

  const fetchPerformanceMetrics = useCallback(async (signal) => {
    setPerformanceLoading(true);
    try {
      const startDate = dateRange[0]?.toISOString();
      const endDate = dateRange[1]?.toISOString();
      const plantCode = isPlantUser && !isAdmin ? primaryPlant : null;
      
      const response = await apiClient.get(`/test-analytics/performance-metrics${startDate || endDate || plantCode ? '?' : ''}${startDate ? `startDate=${startDate}&` : ''}${endDate ? `endDate=${endDate}&` : ''}${plantCode ? `plantCode=${plantCode}` : ''}`.replace(/&$/, ''));
      if (!signal?.aborted) {
        setPerformanceMetrics(response);
      }
    } catch (error) {
      if (!signal?.aborted) {
        console.error('Failed to fetch performance metrics:', error);
        message.error('Failed to load performance metrics. Please check your connection and try again.');
        setPerformanceMetrics(null);
      }
    } finally {
      if (!signal?.aborted) {
        setPerformanceLoading(false);
      }
    }
  }, [dateRange, isAdmin, isPlantUser, primaryPlant]);

  const fetchQueryStatusBreakdown = useCallback(async (signal) => {
    setQueryStatusLoading(true);
    try {
      const startDate = dateRange[0]?.toISOString();
      const endDate = dateRange[1]?.toISOString();
      const plantCode = isPlantUser && !isAdmin ? primaryPlant : null;
      
      const response = await apiClient.get(`/test-analytics/query-status-breakdown${startDate || endDate || plantCode ? '?' : ''}${startDate ? `startDate=${startDate}&` : ''}${endDate ? `endDate=${endDate}&` : ''}${plantCode ? `plantCode=${plantCode}` : ''}`.replace(/&$/, ''));
      if (!signal?.aborted) {
        setQueryStatusBreakdown(response);
      }
    } catch (error) {
      if (!signal?.aborted) {
        console.error('Failed to fetch query status breakdown:', error);
        message.error('Failed to load query status breakdown. Please check your connection and try again.');
        setQueryStatusBreakdown(null);
      }
    } finally {
      if (!signal?.aborted) {
        setQueryStatusLoading(false);
      }
    }
  }, [dateRange, isAdmin, isPlantUser, primaryPlant]);

  const fetchPerformanceRankings = useCallback(async (signal) => {
    setRankingsLoading(true);
    try {
      const startDate = dateRange[0]?.toISOString();
      const endDate = dateRange[1]?.toISOString();
      const plantCode = isPlantUser && !isAdmin ? primaryPlant : null;
      
      const response = await apiClient.get(`/test-analytics/performance-rankings${startDate || endDate || plantCode ? '?' : ''}${startDate ? `startDate=${startDate}&` : ''}${endDate ? `endDate=${endDate}&` : ''}${plantCode ? `plantCode=${plantCode}` : ''}`.replace(/&$/, ''));
      if (!signal?.aborted) {
        setPerformanceRankings(response);
      }
    } catch (error) {
      if (!signal?.aborted) {
        console.error('Failed to fetch performance rankings:', error);
        message.error('Failed to load performance rankings. Please check your connection and try again.');
        setPerformanceRankings(null);
      }
    } finally {
      if (!signal?.aborted) {
        setRankingsLoading(false);
      }
    }
  }, [dateRange, isAdmin, isPlantUser, primaryPlant]);

  useEffect(() => {
    const controller = new AbortController();

    const fetchData = async () => {
      try {
        await Promise.all([
          fetchDashboardData(controller.signal),
          fetchBottlenecks(controller.signal),
          fetchPerformanceMetrics(controller.signal),
          fetchQueryStatusBreakdown(controller.signal),
          fetchPerformanceRankings(controller.signal)
        ]);
      } catch (error) {
        if (!controller.signal.aborted) {
          console.error('Error fetching QR analytics data:', error);
        }
      }
    };

    fetchData();

    return () => {
      controller.abort();
    };
  }, [fetchDashboardData, fetchBottlenecks, fetchPerformanceMetrics, fetchQueryStatusBreakdown, fetchPerformanceRankings]);

  const handleDateRangeChange = (dates) => {
    setDateRange(dates);
  };

  const handleApplyDateFilter = () => {
    // Refresh all data with new date filters
    const controller = new AbortController();
    Promise.all([
      fetchDashboardData(controller.signal),
      fetchBottlenecks(controller.signal),
      fetchPerformanceMetrics(controller.signal),
      fetchQueryStatusBreakdown(controller.signal),
      fetchPerformanceRankings(controller.signal)
    ]);
    fetchSlaReport();
  };

  const handleExportAuditLogs = async () => {
    setExportLoading(true);
    try {
      let url = '/test-analytics/export';
      if (dateRange[0] && dateRange[1]) {
        url += `?startDate=${dateRange[0].toISOString()}&endDate=${dateRange[1].toISOString()}`;
      }

      const response = await apiClient.get(url, { responseType: 'blob' });
      const blob = response;

      const downloadUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.setAttribute('download', `qr-analytics-${new Date().toISOString().split('T')[0]}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();

      message.success('QR Analytics report exported successfully');
    } catch (error) {
      message.error('Failed to export QR Analytics report');
      console.error('Error exporting QR Analytics report:', error);
    } finally {
      setExportLoading(false);
    }
  };

  const handleExportWorkflowReport = async (state = null) => {
    setExportLoading(true);
    try {
      let url = '/test-analytics/export';
      const params = [];

      if (dateRange[0] && dateRange[1]) {
        params.push(`startDate=${dateRange[0].toISOString()}`);
        params.push(`endDate=${dateRange[1].toISOString()}`);
      }

      if (state) {
        params.push(`state=${state}`);
      }

      if (params.length > 0) {
        url += `?${params.join('&')}`;
      }

      const response = await apiClient.get(url, { responseType: 'blob' });
      const blob = response;

      const downloadUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.setAttribute('download', `workflow-report-${new Date().toISOString().split('T')[0]}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();

      message.success('Workflow report exported successfully');
    } catch (error) {
      message.error('Failed to export workflow report');
      console.error('Error exporting workflow report:', error);
    } finally {
      setExportLoading(false);
    }
  };

  // Prepare chart data for workflow status distribution
  const getWorkflowStatusChartData = () => {
    if (!dashboardData || !dashboardData.workflowsByState) {
      return null;
    }

    const labels = Object.keys(dashboardData.workflowsByState);
    const data = Object.values(dashboardData.workflowsByState);

    return {
      labels,
      datasets: [
        {
          label: 'Workflows by State',
          data,
          backgroundColor: [
            'rgba(54, 162, 235, 0.6)',
            'rgba(255, 206, 86, 0.6)',
            'rgba(75, 192, 192, 0.6)',
            'rgba(153, 102, 255, 0.6)',
            'rgba(255, 159, 64, 0.6)'
          ],
          borderColor: [
            'rgba(54, 162, 235, 1)',
            'rgba(255, 206, 86, 1)',
            'rgba(75, 192, 192, 1)',
            'rgba(153, 102, 255, 1)',
            'rgba(255, 159, 64, 1)'
          ],
          borderWidth: 1
        }
      ]
    };
  };

  // Prepare chart data for SLA compliance
  const getSlaComplianceChartData = () => {
    if (!slaReport || !slaReport.slaComplianceByTeam) {
      return null;
    }

    const labels = Object.keys(slaReport.slaComplianceByTeam);
    const data = Object.values(slaReport.slaComplianceByTeam);

    return {
      labels,
      datasets: [
        {
          label: 'SLA Compliance (%)',
          data,
          backgroundColor: 'rgba(75, 192, 192, 0.6)',
          borderColor: 'rgba(75, 192, 192, 1)',
          borderWidth: 1
        }
      ]
    };
  };

  // Prepare chart data for resolution times
  const getResolutionTimesChartData = () => {
    if (!slaReport || !slaReport.averageResolutionTimesByTeam) {
      return null;
    }

    const labels = Object.keys(slaReport.averageResolutionTimesByTeam);
    const data = Object.values(slaReport.averageResolutionTimesByTeam);

    return {
      labels,
      datasets: [
        {
          label: 'Average Resolution Time (hours)',
          data,
          backgroundColor: 'rgba(255, 159, 64, 0.6)',
          borderColor: 'rgba(255, 159, 64, 1)',
          borderWidth: 1
        }
      ]
    };
  };

  // Prepare chart data for recent activity
  const getRecentActivityChartData = () => {
    if (!dashboardData || !dashboardData.recentActivity) {
      return null;
    }

    const sortedDates = Object.keys(dashboardData.recentActivity).sort();
    const data = sortedDates.map(date => dashboardData.recentActivity[date]);

    return {
      labels: sortedDates,
      datasets: [
        {
          label: 'Workflow Activity',
          data,
          fill: false,
          backgroundColor: 'rgba(54, 162, 235, 0.6)',
          borderColor: 'rgba(54, 162, 235, 1)',
          tension: 0.1
        }
      ]
    };
  };

  // Prepare chart data for team performance rankings
  const getTeamPerformanceChartData = () => {
    if (!performanceRankings || !performanceRankings.teamPerformance) {
      return null;
    }

    const teams = Object.keys(performanceRankings.teamPerformance);
    const resolutionRates = teams.map(team => performanceRankings.teamPerformance[team].resolutionRate || 0);
    const avgResolutionTimes = teams.map(team => performanceRankings.teamPerformance[team].avgResolutionTime || 0);

    return {
      labels: teams,
      datasets: [
        {
          label: 'Resolution Rate (%)',
          data: resolutionRates,
          backgroundColor: 'rgba(75, 192, 192, 0.6)',
          borderColor: 'rgba(75, 192, 192, 1)',
          borderWidth: 1,
          yAxisID: 'y'
        },
        {
          label: 'Avg Resolution Time (hrs)',
          data: avgResolutionTimes,
          backgroundColor: 'rgba(255, 159, 64, 0.6)',
          borderColor: 'rgba(255, 159, 64, 1)',
          borderWidth: 1,
          yAxisID: 'y1'
        }
      ]
    };
  };

  // Prepare chart data for plant performance rankings
  const getPlantPerformanceChartData = () => {
    if (!performanceRankings || !performanceRankings.plantPerformance) {
      return null;
    }

    const plants = Object.keys(performanceRankings.plantPerformance);
    const completionRates = plants.map(plant => performanceRankings.plantPerformance[plant].completionRate || 0);
    const avgCompletionTimes = plants.map(plant => performanceRankings.plantPerformance[plant].avgCompletionTime || 0);

    return {
      labels: plants,
      datasets: [
        {
          label: 'Completion Rate (%)',
          data: completionRates,
          backgroundColor: 'rgba(153, 102, 255, 0.6)',
          borderColor: 'rgba(153, 102, 255, 1)',
          borderWidth: 1,
          yAxisID: 'y'
        },
        {
          label: 'Avg Completion Time (hrs)',
          data: avgCompletionTimes,
          backgroundColor: 'rgba(255, 99, 132, 0.6)',
          borderColor: 'rgba(255, 99, 132, 1)',
          borderWidth: 1,
          yAxisID: 'y1'
        }
      ]
    };
  };

  // Prepare pie chart data for top performers distribution
  const getTopPerformersDistributionData = () => {
    if (!performanceRankings || !performanceRankings.topQueryResolvers) {
      return null;
    }

    const topResolvers = performanceRankings.topQueryResolvers.slice(0, 5);
    const labels = topResolvers.map(resolver => resolver.username || resolver.name || 'Unknown');
    const data = topResolvers.map(resolver => resolver.queriesResolved || resolver.count || 0);

    return {
      labels,
      datasets: [
        {
          label: 'Queries Resolved',
          data,
          backgroundColor: [
            'rgba(255, 206, 86, 0.8)',
            'rgba(75, 192, 192, 0.8)',
            'rgba(153, 102, 255, 0.8)',
            'rgba(255, 159, 64, 0.8)',
            'rgba(54, 162, 235, 0.8)'
          ],
          borderColor: [
            'rgba(255, 206, 86, 1)',
            'rgba(75, 192, 192, 1)',
            'rgba(153, 102, 255, 1)',
            'rgba(255, 159, 64, 1)',
            'rgba(54, 162, 235, 1)'
          ],
          borderWidth: 2
        }
      ]
    };
  };

  // Prepare horizontal bar chart for workflow creators
  const getWorkflowCreatorsChartData = () => {
    if (!performanceRankings || !performanceRankings.topWorkflowCreators) {
      return null;
    }

    const topCreators = performanceRankings.topWorkflowCreators.slice(0, 10);
    const labels = topCreators.map(creator => creator.username || creator.name || 'Unknown');
    const data = topCreators.map(creator => creator.workflowsCreated || creator.count || 0);

    return {
      labels,
      datasets: [
        {
          label: 'Workflows Created',
          data,
          backgroundColor: 'rgba(54, 162, 235, 0.6)',
          borderColor: 'rgba(54, 162, 235, 1)',
          borderWidth: 1
        }
      ]
    };
  };

  // Render loading state
  if (loading && !dashboardData) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Spin size="large" />
        <p>Loading QR Analytics dashboard...</p>
      </div>
    );
  }

  // Render error state when no data is available
  if (!loading && !dashboardData) {
    return (
      <div>
        <Title level={2}>
          <QrcodeOutlined style={{ marginRight: 8 }} />
          QR Analytics Dashboard
        </Title>
        <div style={{ textAlign: 'center', padding: '50px' }}>
          <ExclamationCircleOutlined style={{ fontSize: '48px', color: '#ff4d4f', marginBottom: '16px' }} />
          <Title level={4}>Unable to Load Analytics Data</Title>
          <Text type="secondary">
            The QR Analytics service is currently unavailable. Please check your connection and try again.
          </Text>
          <br />
          <Button 
            type="primary" 
            onClick={() => window.location.reload()} 
            style={{ marginTop: '16px' }}
          >
            Retry
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div>
      <Title level={2}>
        <QrcodeOutlined style={{ marginRight: 8 }} />
        QR Analytics Dashboard
      </Title>

      {/* User Context Display */}
      {dashboardData?.userContext && (
        <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
          <Col span={24}>
            <Card size="small" style={{ backgroundColor: '#f6ffed', border: '1px solid #b7eb8f' }}>
              <Row gutter={16} align="middle">
                <Col>
                  <Text strong>User: </Text>
                  <Text>{dashboardData.userContext.username}</Text>
                </Col>
                <Col>
                  <Text strong>Role: </Text>
                  <Text style={{ 
                    color: dashboardData.userContext.role === 'ADMIN' ? '#1890ff' : 
                           dashboardData.userContext.role === 'PLANT_USER' ? '#52c41a' : '#722ed1',
                    fontWeight: 'bold'
                  }}>
                    {dashboardData.userContext.role}
                  </Text>
                </Col>
                {dashboardData.userContext.plantCode && (
                  <Col>
                    <Text strong>Plant: </Text>
                    <Text>{dashboardData.userContext.plantCode}</Text>
                  </Col>
                )}
                {dashboardData.userContext.hasPlantRestriction && (
                  <Col>
                    <Text type="warning" style={{ fontSize: '12px' }}>
                      (Data filtered by plant access)
                    </Text>
                  </Col>
                )}
              </Row>
            </Card>
          </Col>
        </Row>
      )}

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col span={24}>
          <Card>
            <Row gutter={16}>
              <Col span={6}>
                <RangePicker onChange={handleDateRangeChange} style={{ width: '100%' }} />
              </Col>
              {isAdmin && !isPlantUser && (
                <Col span={4}>
                  <Select
                    placeholder="Select Plant"
                    allowClear
                    style={{ width: '100%' }}
                    value={selectedPlant}
                    onChange={setSelectedPlant}
                    options={[
                      { value: 'PLANT_A', label: 'Plant A' },
                      { value: 'PLANT_B', label: 'Plant B' },
                      { value: 'PLANT_C', label: 'Plant C' },
                      { value: 'PLANT_D', label: 'Plant D' }
                    ]}
                  />
                </Col>
              )}
              <Col span={4}>
                <Button type="primary" onClick={handleApplyDateFilter}>
                  Apply Filter
                </Button>
              </Col>
              <Col span={12} style={{ textAlign: 'right' }}>
                <Button
                  icon={<DownloadOutlined />}
                  onClick={handleExportAuditLogs}
                  loading={exportLoading}
                  style={{ marginRight: 8 }}
                >
                  Export Analytics
                </Button>
                <Button
                  icon={<DownloadOutlined />}
                  onClick={() => handleExportWorkflowReport()}
                  loading={exportLoading}
                >
                  Export Workflow Report
                </Button>
              </Col>
            </Row>
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col span={6}>
          <Card>
            <Statistic
              title="Total Workflows"
              value={dashboardData?.totalWorkflows || 0}
              prefix={<BarChartOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Active Workflows"
              value={dashboardData?.activeWorkflows || 0}
              prefix={<ClockCircleOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Completed Workflows"
              value={dashboardData?.completedWorkflows || 0}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#3f8600' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Overdue Workflows"
              value={dashboardData?.overdueWorkflows || 0}
              prefix={<ExclamationCircleOutlined />}
              valueStyle={{ color: '#cf1322' }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="Total Queries"
              value={dashboardData?.totalQueries || 0}
              prefix={<QuestionCircleOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Open Queries"
              value={dashboardData?.openQueries || 0}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Overdue Queries"
              value={dashboardData?.overdueQueries || 0}
              valueStyle={{ color: '#cf1322' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Avg. Completion Time (hrs)"
              value={dashboardData?.averageCompletionTimeHours?.toFixed(1) || 0}
              precision={1}
            />
          </Card>
        </Col>
      </Row>

      <Tabs defaultActiveKey="1" style={{ marginTop: 16 }}>
        <TabPane tab="Workflow Status" key="1">
          <Row gutter={[16, 16]}>
            <Col span={12}>
              <Card title="Workflow Status Distribution">
                {dashboardData && dashboardData.workflowsByState && Object.keys(dashboardData.workflowsByState).length > 0 ? (
                  <Pie
                    data={getWorkflowStatusChartData()}
                    options={{ responsive: true, maintainAspectRatio: false }}
                    height={300}
                  />
                ) : (
                  <div style={{ textAlign: 'center', padding: '50px' }}>
                    <Text type="secondary">No workflow status data available</Text>
                  </div>
                )}
              </Card>
            </Col>
            <Col span={12}>
              <Card title="Recent Activity">
                {dashboardData && dashboardData.recentActivity && Object.keys(dashboardData.recentActivity).length > 0 ? (
                  <Line
                    data={getRecentActivityChartData()}
                    options={{ responsive: true, maintainAspectRatio: false }}
                    height={300}
                  />
                ) : (
                  <div style={{ textAlign: 'center', padding: '50px' }}>
                    <Text type="secondary">No recent activity data available</Text>
                  </div>
                )}
              </Card>
            </Col>
          </Row>

          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            <Col span={24}>
              <Card title="Workflows by Plant">
                <Table
                  dataSource={
                    dashboardData && dashboardData.workflowsByPlant
                      ? Object.entries(dashboardData.workflowsByPlant).map(([plant, count]) => ({
                        key: plant,
                        plant,
                        count
                      }))
                      : []
                  }
                  columns={[
                    { title: 'Plant', dataIndex: 'plant', key: 'plant' },
                    { title: 'Workflow Count', dataIndex: 'count', key: 'count' }
                  ]}
                  pagination={false}
                  locale={{
                    emptyText: 'No workflow data available'
                  }}
                />
              </Card>
            </Col>
          </Row>
        </TabPane>

        <TabPane tab="Query SLA Reports" key="2">
          <Button
            type="primary"
            onClick={fetchSlaReport}
            loading={slaLoading}
            style={{ marginBottom: 16 }}
          >
            Generate SLA Report
          </Button>

          {slaLoading ? (
            <div style={{ textAlign: 'center', padding: '50px' }}>
              <Spin />
              <p>Generating SLA report...</p>
            </div>
          ) : slaReport ? (
            <>
              <Row gutter={[16, 16]}>
                <Col span={8}>
                  <Card>
                    <Statistic
                      title="Overall SLA Compliance"
                      value={slaReport.overallSlaCompliance.toFixed(1)}
                      suffix="%"
                      precision={1}
                    />
                    <Progress
                      percent={slaReport.overallSlaCompliance}
                      status={slaReport.overallSlaCompliance >= 90 ? 'success' : 'active'}
                    />
                  </Card>
                </Col>
                <Col span={8}>
                  <Card>
                    <Statistic
                      title="Average Resolution Time"
                      value={slaReport.overallAverageResolutionTime.toFixed(1)}
                      suffix="hours"
                      precision={1}
                    />
                  </Card>
                </Col>
                <Col span={8}>
                  <Card>
                    <Statistic
                      title="Resolution Rate"
                      value={(
                        (slaReport.totalResolvedQueries / slaReport.totalQueries) *
                        100
                      ).toFixed(1)}
                      suffix="%"
                      precision={1}
                    />
                  </Card>
                </Col>
              </Row>

              <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
                <Col span={12}>
                  <Card title="SLA Compliance by Team">
                    {slaReport.slaComplianceByTeam && Object.keys(slaReport.slaComplianceByTeam).length > 0 ? (
                      <Bar
                        data={getSlaComplianceChartData()}
                        options={{
                          responsive: true,
                          maintainAspectRatio: false,
                          scales: {
                            y: {
                              beginAtZero: true,
                              max: 100
                            }
                          }
                        }}
                        height={300}
                      />
                    ) : (
                      <div style={{ textAlign: 'center', padding: '50px' }}>
                        <Text type="secondary">No SLA compliance data available</Text>
                      </div>
                    )}
                  </Card>
                </Col>
                <Col span={12}>
                  <Card title="Average Resolution Times by Team">
                    {slaReport.averageResolutionTimesByTeam && Object.keys(slaReport.averageResolutionTimesByTeam).length > 0 ? (
                      <Bar
                        data={getResolutionTimesChartData()}
                        options={{
                          responsive: true,
                          maintainAspectRatio: false
                        }}
                        height={300}
                      />
                    ) : (
                      <div style={{ textAlign: 'center', padding: '50px' }}>
                        <Text type="secondary">No resolution time data available</Text>
                      </div>
                    )}
                  </Card>
                </Col>
              </Row>

              <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
                <Col span={24}>
                  <Card title="Query Metrics by Team">
                    <Table
                      dataSource={
                        slaReport.totalQueriesByTeam
                          ? Object.keys(slaReport.totalQueriesByTeam).map(team => ({
                            key: team,
                            team,
                            total: slaReport.totalQueriesByTeam[team],
                            resolved: slaReport.resolvedQueriesByTeam[team],
                            overdue: slaReport.overdueQueriesByTeam[team],
                            avgTime: slaReport.averageResolutionTimesByTeam[team]?.toFixed(1),
                            compliance: slaReport.slaComplianceByTeam[team]?.toFixed(1)
                          }))
                          : []
                      }
                      columns={[
                        { title: 'Team', dataIndex: 'team', key: 'team' },
                        { title: 'Total Queries', dataIndex: 'total', key: 'total' },
                        { title: 'Resolved', dataIndex: 'resolved', key: 'resolved' },
                        { title: 'Overdue', dataIndex: 'overdue', key: 'overdue' },
                        {
                          title: 'Avg. Resolution Time (hrs)',
                          dataIndex: 'avgTime',
                          key: 'avgTime'
                        },
                        {
                          title: 'SLA Compliance',
                          dataIndex: 'compliance',
                          key: 'compliance',
                          render: text => `${text}%`
                        }
                      ]}
                      pagination={false}
                      locale={{
                        emptyText: 'No query metrics data available'
                      }}
                    />
                  </Card>
                </Col>
              </Row>
            </>
          ) : (
            <div style={{ textAlign: 'center', padding: '50px' }}>
              <Text type="secondary">Click "Generate SLA Report" to view query SLA metrics</Text>
            </div>
          )}
        </TabPane>

        <TabPane tab="Bottlenecks Analysis" key="3">
          <Button
            type="primary"
            onClick={() => fetchBottlenecks()}
            loading={bottlenecksLoading}
            style={{ marginBottom: 16 }}
          >
            Refresh Bottlenecks Analysis
          </Button>

          {bottlenecksLoading ? (
            <div style={{ textAlign: 'center', padding: '50px' }}>
              <Spin />
              <p>Analyzing bottlenecks...</p>
            </div>
          ) : bottlenecks ? (
            <>
              <Row gutter={[16, 16]}>
                <Col span={12}>
                  <Card title="Average Time in Each State (hours)">
                    <Table
                      dataSource={
                        bottlenecks.averageTimeInState
                          ? Object.entries(bottlenecks.averageTimeInState).map(
                            ([state, hours]) => ({
                              key: state,
                              state,
                              hours: hours.toFixed(1)
                            })
                          )
                          : []
                      }
                      columns={[
                        { title: 'Workflow State', dataIndex: 'state', key: 'state' },
                        { title: 'Average Hours', dataIndex: 'hours', key: 'hours' }
                      ]}
                      pagination={false}
                      locale={{
                        emptyText: 'No state timing data available'
                      }}
                    />
                  </Card>
                </Col>
                <Col span={12}>
                  <Card title="Overdue Workflows by State">
                    <Table
                      dataSource={
                        bottlenecks.overdueByState
                          ? Object.entries(bottlenecks.overdueByState).map(([state, count]) => ({
                            key: state,
                            state,
                            count
                          }))
                          : []
                      }
                      columns={[
                        { title: 'Workflow State', dataIndex: 'state', key: 'state' },
                        { title: 'Overdue Count', dataIndex: 'count', key: 'count' }
                      ]}
                      pagination={false}
                      locale={{
                        emptyText: 'No overdue workflow data available'
                      }}
                    />
                  </Card>
                </Col>
              </Row>

              <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
                <Col span={12}>
                  <Card title="Open Queries by Team">
                    <Table
                      dataSource={
                        bottlenecks.openQueriesByTeam
                          ? Object.entries(bottlenecks.openQueriesByTeam).map(([team, count]) => ({
                            key: team,
                            team,
                            count
                          }))
                          : []
                      }
                      columns={[
                        { title: 'Team', dataIndex: 'team', key: 'team' },
                        { title: 'Open Queries', dataIndex: 'count', key: 'count' }
                      ]}
                      pagination={false}
                      locale={{
                        emptyText: 'No open queries data available'
                      }}
                    />
                  </Card>
                </Col>
                <Col span={12}>
                  <Card title="Delayed Workflows by Plant">
                    <Table
                      dataSource={
                        bottlenecks.delayedByPlant
                          ? Object.entries(bottlenecks.delayedByPlant).map(([plant, count]) => ({
                            key: plant,
                            plant,
                            count
                          }))
                          : []
                      }
                      columns={[
                        { title: 'Plant', dataIndex: 'plant', key: 'plant' },
                        { title: 'Delayed Count', dataIndex: 'count', key: 'count' }
                      ]}
                      pagination={false}
                      locale={{
                        emptyText: 'No delayed workflow data available'
                      }}
                    />
                  </Card>
                </Col>
              </Row>
            </>
          ) : (
            <div style={{ textAlign: 'center', padding: '50px' }}>
              <Text type="secondary">
                Click "Refresh Bottlenecks Analysis" to view workflow bottlenecks
              </Text>
            </div>
          )}
        </TabPane>

        <TabPane tab="Performance Metrics" key="4">
          <Button
            type="primary"
            onClick={() => fetchPerformanceMetrics()}
            loading={performanceLoading}
            style={{ marginBottom: 16 }}
          >
            Refresh Performance Metrics
          </Button>

          {performanceLoading ? (
            <div style={{ textAlign: 'center', padding: '50px' }}>
              <Spin />
              <p>Loading performance metrics...</p>
            </div>
          ) : performanceMetrics ? (
            <>
              <Row gutter={[16, 16]}>
                <Col span={8}>
                  <Card>
                    <Statistic
                      title="Completion Rate"
                      value={performanceMetrics.completionRate?.toFixed(1)}
                      suffix="%"
                      precision={1}
                    />
                    <Progress
                      percent={performanceMetrics.completionRate}
                      status={performanceMetrics.completionRate >= 80 ? 'success' : 'active'}
                    />
                  </Card>
                </Col>
                <Col span={8}>
                  <Card>
                    <Statistic
                      title="Average Completion Time"
                      value={performanceMetrics.averageCompletionTimeHours?.toFixed(1)}
                      suffix="hours"
                      precision={1}
                    />
                  </Card>
                </Col>
                <Col span={8}>
                  <Card>
                    <Statistic
                      title="Queries Per Workflow"
                      value={performanceMetrics.queriesPerWorkflow?.toFixed(1)}
                      precision={1}
                    />
                  </Card>
                </Col>
              </Row>

              <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
                <Col span={24}>
                  <Card title="Monthly Workflow Throughput">
                    {performanceMetrics.throughputByMonth && Object.keys(performanceMetrics.throughputByMonth).length > 0 ? (
                      <Bar
                        data={{
                          labels: Object.keys(performanceMetrics.throughputByMonth),
                          datasets: [
                            {
                              label: 'Completed Workflows',
                              data: Object.values(performanceMetrics.throughputByMonth),
                              backgroundColor: 'rgba(54, 162, 235, 0.6)',
                              borderColor: 'rgba(54, 162, 235, 1)',
                              borderWidth: 1
                            }
                          ]
                        }}
                        options={{
                          responsive: true,
                          maintainAspectRatio: false,
                          scales: {
                            y: {
                              beginAtZero: true
                            }
                          }
                        }}
                        height={300}
                      />
                    ) : (
                      <div style={{ textAlign: 'center', padding: '50px' }}>
                        <Text type="secondary">No throughput data available</Text>
                      </div>
                    )}
                  </Card>
                </Col>
              </Row>
            </>
          ) : (
            <div style={{ textAlign: 'center', padding: '50px' }}>
              <Text type="secondary">
                Click "Refresh Performance Metrics" to view workflow performance data
              </Text>
            </div>
          )}
        </TabPane>

        <TabPane tab="Query Status Analysis" key="5">
          <Button
            type="primary"
            onClick={() => fetchQueryStatusBreakdown()}
            loading={queryStatusLoading}
            style={{ marginBottom: 16 }}
          >
            Refresh Query Status Analysis
          </Button>

          {queryStatusLoading ? (
            <div style={{ textAlign: 'center', padding: '50px' }}>
              <Spin />
              <p>Analyzing query statuses...</p>
            </div>
          ) : queryStatusBreakdown ? (
            <>
              <Row gutter={[16, 16]}>
                <Col span={8}>
                  <Card>
                    <Statistic
                      title="Total Queries"
                      value={queryStatusBreakdown.totalQueries || 0}
                      prefix={<QuestionCircleOutlined />}
                    />
                  </Card>
                </Col>
                <Col span={8}>
                  <Card>
                    <Statistic
                      title="Active Queries"
                      value={queryStatusBreakdown.activeQueries || 0}
                      prefix={<ClockCircleOutlined />}
                      valueStyle={{ color: '#1890ff' }}
                    />
                  </Card>
                </Col>
                <Col span={8}>
                  <Card>
                    <Statistic
                      title="Resolved Queries"
                      value={queryStatusBreakdown.resolvedQueries || 0}
                      prefix={<CheckCircleOutlined />}
                      valueStyle={{ color: '#3f8600' }}
                    />
                  </Card>
                </Col>
              </Row>

              <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
                <Col span={12}>
                  <Card title="Query Status Distribution">
                    <Table
                      dataSource={
                        queryStatusBreakdown.statusCounts
                          ? Object.entries(queryStatusBreakdown.statusCounts).map(([status, count]) => ({
                            key: status,
                            status: status.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase()),
                            count,
                            percentage: ((count / queryStatusBreakdown.totalQueries) * 100).toFixed(1)
                          }))
                          : []
                      }
                      columns={[
                        { title: 'Status', dataIndex: 'status', key: 'status' },
                        { title: 'Count', dataIndex: 'count', key: 'count' },
                        { 
                          title: 'Percentage', 
                          dataIndex: 'percentage', 
                          key: 'percentage',
                          render: text => `${text}%`
                        }
                      ]}
                      pagination={false}
                      locale={{
                        emptyText: 'No query status data available'
                      }}
                    />
                  </Card>
                </Col>
                <Col span={12}>
                  <Card title="Query Status by Team">
                    <Table
                      dataSource={
                        queryStatusBreakdown.teamStatusCounts
                          ? Object.entries(queryStatusBreakdown.teamStatusCounts).map(([team, statusCounts]) => ({
                            key: team,
                            team,
                            open: statusCounts.OPEN || 0,
                            resolved: statusCounts.RESOLVED || 0,
                            closed: statusCounts.CLOSED || 0,
                            total: Object.values(statusCounts).reduce((sum, count) => sum + count, 0)
                          }))
                          : []
                      }
                      columns={[
                        { title: 'Team', dataIndex: 'team', key: 'team' },
                        { title: 'Open', dataIndex: 'open', key: 'open' },
                        { title: 'Resolved', dataIndex: 'resolved', key: 'resolved' },
                        { title: 'Closed', dataIndex: 'closed', key: 'closed' },
                        { title: 'Total', dataIndex: 'total', key: 'total', render: text => <strong>{text}</strong> }
                      ]}
                      pagination={false}
                      scroll={{ x: 800 }}
                      locale={{
                        emptyText: 'No team query data available'
                      }}
                    />
                  </Card>
                </Col>
              </Row>
            </>
          ) : (
            <div style={{ textAlign: 'center', padding: '50px' }}>
              <Text type="secondary">Click "Refresh Query Status Analysis" to view detailed query status breakdown</Text>
            </div>
          )}
        </TabPane>

        <TabPane tab="Performance Rankings" key="6">
          <Button
            type="primary"
            onClick={() => fetchPerformanceRankings()}
            loading={rankingsLoading}
            style={{ marginBottom: 16 }}
          >
            Refresh Performance Rankings
          </Button>

          {rankingsLoading ? (
            <div style={{ textAlign: 'center', padding: '50px' }}>
              <Spin />
              <p>Loading performance rankings...</p>
            </div>
          ) : performanceRankings ? (
            <>
              {/* Performance Overview Cards */}
              <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
                <Col span={6}>
                  <Card>
                    <Statistic
                      title="Top Resolver"
                      value={performanceRankings.topQueryResolvers?.[0]?.count || 0}
                      suffix="queries"
                      prefix="🏆"
                      valueStyle={{ color: '#52c41a' }}
                    />
                    <Text type="secondary" style={{ fontSize: '12px' }}>
                      {performanceRankings.topQueryResolvers?.[0]?.name || 'N/A'}
                    </Text>
                  </Card>
                </Col>
                <Col span={6}>
                  <Card>
                    <Statistic
                      title="Top Creator"
                      value={performanceRankings.topWorkflowCreators?.[0]?.count || 0}
                      suffix="workflows"
                      prefix="🚀"
                      valueStyle={{ color: '#1890ff' }}
                    />
                    <Text type="secondary" style={{ fontSize: '12px' }}>
                      {performanceRankings.topWorkflowCreators?.[0]?.name || 'N/A'}
                    </Text>
                  </Card>
                </Col>
                <Col span={6}>
                  <Card>
                    <Statistic
                      title="Best Team"
                      value={Object.keys(performanceRankings.teamPerformance || {})[0] || 'N/A'}
                      prefix="👥"
                      valueStyle={{ color: '#722ed1' }}
                    />
                    <Text type="secondary" style={{ fontSize: '12px' }}>
                      Highest resolution rate
                    </Text>
                  </Card>
                </Col>
                <Col span={6}>
                  <Card>
                    <Statistic
                      title="Best Plant"
                      value={Object.keys(performanceRankings.plantPerformance || {})[0] || 'N/A'}
                      prefix="🏭"
                      valueStyle={{ color: '#fa8c16' }}
                    />
                    <Text type="secondary" style={{ fontSize: '12px' }}>
                      Highest completion rate
                    </Text>
                  </Card>
                </Col>
              </Row>

              {/* Charts Row 1 */}
              <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
                <Col span={12}>
                  <Card title="🏆 Top Query Resolvers Distribution">
                    {performanceRankings.topQueryResolvers && performanceRankings.topQueryResolvers.length > 0 ? (
                      <Pie
                        data={getTopPerformersDistributionData()}
                        options={{
                          responsive: true,
                          maintainAspectRatio: false,
                          plugins: {
                            legend: {
                              position: 'bottom'
                            }
                          }
                        }}
                        height={300}
                      />
                    ) : (
                      <div style={{ textAlign: 'center', padding: '50px' }}>
                        <Text type="secondary">No query resolver data available</Text>
                      </div>
                    )}
                  </Card>
                </Col>
                <Col span={12}>
                  <Card title="🚀 Top Workflow Creators">
                    {performanceRankings.topWorkflowCreators && performanceRankings.topWorkflowCreators.length > 0 ? (
                      <Bar
                        data={getWorkflowCreatorsChartData()}
                        options={{
                          responsive: true,
                          maintainAspectRatio: false,
                          indexAxis: 'y',
                          plugins: {
                            legend: {
                              display: false
                            }
                          },
                          scales: {
                            x: {
                              beginAtZero: true
                            }
                          }
                        }}
                        height={300}
                      />
                    ) : (
                      <div style={{ textAlign: 'center', padding: '50px' }}>
                        <Text type="secondary">No workflow creator data available</Text>
                      </div>
                    )}
                  </Card>
                </Col>
              </Row>

              {/* Charts Row 2 */}
              <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
                <Col span={12}>
                  <Card title="👥 Team Performance Comparison">
                    {performanceRankings.teamPerformance && Object.keys(performanceRankings.teamPerformance).length > 0 ? (
                      <Bar
                        data={getTeamPerformanceChartData()}
                        options={{
                          responsive: true,
                          maintainAspectRatio: false,
                          scales: {
                            y: {
                              type: 'linear',
                              display: true,
                              position: 'left',
                              title: {
                                display: true,
                                text: 'Resolution Rate (%)'
                              }
                            },
                            y1: {
                              type: 'linear',
                              display: true,
                              position: 'right',
                              title: {
                                display: true,
                                text: 'Avg Resolution Time (hrs)'
                              },
                              grid: {
                                drawOnChartArea: false
                              }
                            }
                          }
                        }}
                        height={300}
                      />
                    ) : (
                      <div style={{ textAlign: 'center', padding: '50px' }}>
                        <Text type="secondary">No team performance data available</Text>
                      </div>
                    )}
                  </Card>
                </Col>
                <Col span={12}>
                  <Card title="🏭 Plant Performance Comparison">
                    {performanceRankings.plantPerformance && Object.keys(performanceRankings.plantPerformance).length > 0 ? (
                      <Bar
                        data={getPlantPerformanceChartData()}
                        options={{
                          responsive: true,
                          maintainAspectRatio: false,
                          scales: {
                            y: {
                              type: 'linear',
                              display: true,
                              position: 'left',
                              title: {
                                display: true,
                                text: 'Completion Rate (%)'
                              }
                            },
                            y1: {
                              type: 'linear',
                              display: true,
                              position: 'right',
                              title: {
                                display: true,
                                text: 'Avg Completion Time (hrs)'
                              },
                              grid: {
                                drawOnChartArea: false
                              }
                            }
                          }
                        }}
                        height={300}
                      />
                    ) : (
                      <div style={{ textAlign: 'center', padding: '50px' }}>
                        <Text type="secondary">No plant performance data available</Text>
                      </div>
                    )}
                  </Card>
                </Col>
              </Row>

              {/* Detailed Tables */}
              <Row gutter={[16, 16]}>
                <Col span={8}>
                  <Card title="🏆 Top Query Resolvers" size="small">
                    <Table
                      dataSource={performanceRankings.topQueryResolvers?.slice(0, 5) || []}
                      columns={[
                        { 
                          title: '#', 
                          key: 'rank',
                          width: 40,
                          render: (_, __, index) => (
                            <span style={{ 
                              fontWeight: 'bold', 
                              color: index === 0 ? '#faad14' : index === 1 ? '#8c8c8c' : index === 2 ? '#d4b106' : '#666'
                            }}>
                              {index + 1}
                            </span>
                          )
                        },
                        { 
                          title: 'Name', 
                          dataIndex: 'name', 
                          key: 'name',
                          ellipsis: true
                        },
                        { 
                          title: 'Count', 
                          dataIndex: 'count', 
                          key: 'count',
                          width: 60
                        }
                      ]}
                      pagination={false}
                      size="small"
                      locale={{ emptyText: 'No data' }}
                    />
                  </Card>
                </Col>
                <Col span={8}>
                  <Card title="🚀 Top Workflow Creators" size="small">
                    <Table
                      dataSource={performanceRankings.topWorkflowCreators?.slice(0, 5) || []}
                      columns={[
                        { 
                          title: '#', 
                          key: 'rank',
                          width: 40,
                          render: (_, __, index) => (
                            <span style={{ 
                              fontWeight: 'bold', 
                              color: index === 0 ? '#faad14' : index === 1 ? '#8c8c8c' : index === 2 ? '#d4b106' : '#666'
                            }}>
                              {index + 1}
                            </span>
                          )
                        },
                        { 
                          title: 'Name', 
                          dataIndex: 'name', 
                          key: 'name',
                          ellipsis: true
                        },
                        { 
                          title: 'Count', 
                          dataIndex: 'count', 
                          key: 'count',
                          width: 60
                        }
                      ]}
                      pagination={false}
                      size="small"
                      locale={{ emptyText: 'No data' }}
                    />
                  </Card>
                </Col>
                <Col span={8}>
                  <Card title="✅ Top Form Completers" size="small">
                    <Table
                      dataSource={performanceRankings.topFormCompleters?.slice(0, 5) || []}
                      columns={[
                        { 
                          title: '#', 
                          key: 'rank',
                          width: 40,
                          render: (_, __, index) => (
                            <span style={{ 
                              fontWeight: 'bold', 
                              color: index === 0 ? '#faad14' : index === 1 ? '#8c8c8c' : index === 2 ? '#d4b106' : '#666'
                            }}>
                              {index + 1}
                            </span>
                          )
                        },
                        { 
                          title: 'Name', 
                          dataIndex: 'name', 
                          key: 'name',
                          ellipsis: true
                        },
                        { 
                          title: 'Count', 
                          dataIndex: 'count', 
                          key: 'count',
                          width: 60
                        }
                      ]}
                      pagination={false}
                      size="small"
                      locale={{ emptyText: 'No data' }}
                    />
                  </Card>
                </Col>
                <Col span={12}>
                  <Card title="📊 Team Performance Comparison">
                    <Table
                      dataSource={
                        performanceRankings.teamPerformance
                          ? Object.entries(performanceRankings.teamPerformance).map(([team, stats]) => ({
                            key: team,
                            team,
                            ...stats
                          }))
                          : []
                      }
                      columns={[
                        { title: 'Team', dataIndex: 'team', key: 'team', render: team => `👥 ${team}` },
                        { title: 'Total', dataIndex: 'totalQueries', key: 'totalQueries' },
                        { title: 'Resolved', dataIndex: 'resolvedQueries', key: 'resolvedQueries' },
                        { title: 'Open', dataIndex: 'openQueries', key: 'openQueries' },
                        { 
                          title: 'Resolution %', 
                          dataIndex: 'resolutionRate', 
                          key: 'resolutionRate',
                          render: rate => `${rate}%`
                        },
                        { 
                          title: 'Avg Time (hrs)', 
                          dataIndex: 'avgResolutionTime', 
                          key: 'avgResolutionTime',
                          render: time => `${time}h`
                        }
                      ]}
                      pagination={false}
                      size="small"
                      locale={{ emptyText: 'No team performance data available' }}
                    />
                  </Card>
                </Col>
              </Row>

              <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
                <Col span={24}>
                  <Card title="🏭 Plant Performance Comparison">
                    <Table
                      dataSource={
                        performanceRankings.plantPerformance
                          ? Object.entries(performanceRankings.plantPerformance).map(([plant, stats]) => ({
                            key: plant,
                            plant,
                            ...stats
                          }))
                          : []
                      }
                      columns={[
                        { title: 'Plant', dataIndex: 'plant', key: 'plant', render: plant => `🏭 ${plant}` },
                        { title: 'Total Workflows', dataIndex: 'totalWorkflows', key: 'totalWorkflows' },
                        { title: 'Completed', dataIndex: 'completedWorkflows', key: 'completedWorkflows' },
                        { title: 'Active', dataIndex: 'activeWorkflows', key: 'activeWorkflows' },
                        { 
                          title: 'Completion Rate', 
                          dataIndex: 'completionRate', 
                          key: 'completionRate',
                          render: rate => (
                            <span style={{ 
                              color: rate >= 80 ? '#52c41a' : rate >= 60 ? '#faad14' : '#ff4d4f',
                              fontWeight: 'bold'
                            }}>
                              {rate}%
                            </span>
                          )
                        },
                        { 
                          title: 'Avg Completion Time (hrs)', 
                          dataIndex: 'avgCompletionTime', 
                          key: 'avgCompletionTime',
                          render: time => `${time}h`
                        }
                      ]}
                      pagination={false}
                      size="small"
                      scroll={{ x: 800 }}
                      locale={{ emptyText: 'No plant performance data available' }}
                    />
                  </Card>
                </Col>
              </Row>
            </>
          ) : (
            <div style={{ textAlign: 'center', padding: '50px' }}>
              <Text type="secondary">Click "Refresh Performance Rankings" to view detailed performance rankings and comparisons</Text>
            </div>
          )}
        </TabPane>
      </Tabs>
    </div>
  );
};

export default QRAnalytics;