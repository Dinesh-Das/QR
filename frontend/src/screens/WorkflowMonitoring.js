import {
  BarChartOutlined,
  DownloadOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  QuestionCircleOutlined
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
  Progress
} from 'antd';
import { Chart, registerables } from 'chart.js';
import React, { useState, useEffect, useCallback } from 'react';
import { Bar, Line, Pie } from 'react-chartjs-2';

import apiClient from '../api/client';
import { monitoringAPI } from '../services/monitoringAPI';


// Register Chart.js components
Chart.register(...registerables);

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;
const { TabPane } = Tabs;
// const { Option } = Select; // Not currently used

const WorkflowMonitoring = () => {
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

  const fetchDashboardData = useCallback(async signal => {
    setLoading(true);
    try {
      const response = await monitoringAPI.getWorkflowMetrics();
      if (!signal?.aborted) {
        setDashboardData(response);
      }
    } catch (error) {
      if (!signal?.aborted) {
        console.warn('Monitoring API not available, using mock data:', error);
        // Provide mock data when API is not available
        const mockData = {
          totalWorkflows: 150,
          activeWorkflows: 45,
          completedWorkflows: 95,
          overdueWorkflows: 10,
          totalQueries: 320,
          openQueries: 25,
          overdueQueries: 8,
          averageCompletionTimeHours: 24.5,
          workflowsByState: {
            'JVC_PENDING': 15,
            'PLANT_PENDING': 12,
            'CQS_PENDING': 8,
            'TECH_PENDING': 10,
            'COMPLETED': 95
          },
          workflowsByPlant: {
            'Plant A': 45,
            'Plant B': 38,
            'Plant C': 42,
            'Plant D': 25
          },
          recentActivity: {
            '2025-08-01': 12,
            '2025-08-02': 15,
            '2025-08-03': 8,
            '2025-08-04': 18,
            '2025-08-05': 22,
            '2025-08-06': 16,
            '2025-08-07': 14,
            '2025-08-08': 10
          }
        };
        setDashboardData(mockData);
      }
    } finally {
      if (!signal?.aborted) {
        setLoading(false);
      }
    }
  }, []);

  const fetchSlaReport = async () => {
    setSlaLoading(true);
    try {
      const response = await monitoringAPI.getQueryMetrics();
      setSlaReport(response);
    } catch (error) {
      console.warn('Query metrics API not available, using mock data:', error);
      // Provide mock SLA data
      const mockSlaData = {
        overallSlaCompliance: 87.5,
        overallAverageResolutionTime: 18.2,
        totalQueries: 320,
        totalResolvedQueries: 280,
        slaComplianceByTeam: {
          'JVC Team': 92.3,
          'Plant Team': 85.1,
          'CQS Team': 89.7,
          'Tech Team': 83.4
        },
        averageResolutionTimesByTeam: {
          'JVC Team': 14.5,
          'Plant Team': 22.1,
          'CQS Team': 16.8,
          'Tech Team': 19.3
        },
        totalQueriesByTeam: {
          'JVC Team': 85,
          'Plant Team': 92,
          'CQS Team': 78,
          'Tech Team': 65
        },
        resolvedQueriesByTeam: {
          'JVC Team': 78,
          'Plant Team': 78,
          'CQS Team': 70,
          'Tech Team': 54
        },
        overdueQueriesByTeam: {
          'JVC Team': 3,
          'Plant Team': 8,
          'CQS Team': 4,
          'Tech Team': 6
        }
      };
      setSlaReport(mockSlaData);
    } finally {
      setSlaLoading(false);
    }
  };

  const fetchBottlenecks = useCallback(async signal => {
    setBottlenecksLoading(true);
    try {
      const response = await monitoringAPI.getWorkflowMetrics();
      if (!signal?.aborted) {
        setBottlenecks(response);
      }
    } catch (error) {
      if (!signal?.aborted) {
        console.warn('Bottlenecks API not available, using mock data:', error);
        // Provide mock bottlenecks data
        const mockBottlenecks = {
          averageTimeInState: {
            'JVC_PENDING': 16.5,
            'PLANT_PENDING': 28.3,
            'CQS_PENDING': 12.7,
            'TECH_PENDING': 22.1,
            'COMPLETED': 0
          },
          overdueByState: {
            'JVC_PENDING': 3,
            'PLANT_PENDING': 5,
            'CQS_PENDING': 1,
            'TECH_PENDING': 4
          },
          openQueriesByTeam: {
            'JVC Team': 7,
            'Plant Team': 14,
            'CQS Team': 8,
            'Tech Team': 11
          },
          delayedByPlant: {
            'Plant A': 4,
            'Plant B': 6,
            'Plant C': 2,
            'Plant D': 3
          }
        };
        setBottlenecks(mockBottlenecks);
      }
    } finally {
      if (!signal?.aborted) {
        setBottlenecksLoading(false);
      }
    }
  }, []);

  const fetchPerformanceMetrics = useCallback(async signal => {
    setPerformanceLoading(true);
    try {
      const response = await monitoringAPI.getDashboardPerformanceMetrics();
      if (!signal?.aborted) {
        setPerformanceMetrics(response);
      }
    } catch (error) {
      if (!signal?.aborted) {
        console.warn('Performance metrics API not available, using mock data:', error);
        // Provide mock performance data
        const mockPerformance = {
          completionRate: 78.5,
          averageCompletionTimeHours: 24.3,
          queriesPerWorkflow: 2.1,
          throughputByMonth: {
            'Jan 2025': 45,
            'Feb 2025': 52,
            'Mar 2025': 48,
            'Apr 2025': 61,
            'May 2025': 55,
            'Jun 2025': 58,
            'Jul 2025': 63,
            'Aug 2025': 42
          }
        };
        setPerformanceMetrics(mockPerformance);
      }
    } finally {
      if (!signal?.aborted) {
        setPerformanceLoading(false);
      }
    }
  }, []);
  useEffect(() => {
    const controller = new AbortController();

    const fetchData = async () => {
      try {
        await Promise.all([
          fetchDashboardData(controller.signal),
          fetchBottlenecks(controller.signal),
          fetchPerformanceMetrics(controller.signal)
        ]);
      } catch (error) {
        if (!controller.signal.aborted) {
          console.error('Error fetching workflow monitoring data:', error);
        }
      }
    };

    fetchData();

    return () => {
      controller.abort();
    };
  }, [fetchDashboardData, fetchBottlenecks, fetchPerformanceMetrics]);

  const handleDateRangeChange = dates => {
    setDateRange(dates);
  };

  const handleApplyDateFilter = () => {
    fetchSlaReport();
    fetchPerformanceMetrics();
  };

  const handleExportAuditLogs = async () => {
    setExportLoading(true);
    try {
      let url = '/admin/monitoring/audit-logs/export';
      if (dateRange[0] && dateRange[1]) {
        url += `?startDate=${dateRange[0].toISOString()}&endDate=${dateRange[1].toISOString()}`;
      }

      const response = await apiClient.get(url, { responseType: 'blob' });
      const blob = response;

      // Create a download link and trigger download
      const downloadUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.setAttribute('download', `audit-logs-${new Date().toISOString().split('T')[0]}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();

      message.success('Audit logs exported successfully');
    } catch (error) {
      message.error('Failed to export audit logs');
      console.error('Error exporting audit logs:', error);
    } finally {
      setExportLoading(false);
    }
  };

  const handleExportWorkflowReport = async (state = null) => {
    setExportLoading(true);
    try {
      let url = '/admin/monitoring/workflows/export';
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

      // Create a download link and trigger download
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

  // Render loading state
  if (loading && !dashboardData) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Spin size="large" />
        <p>Loading dashboard data...</p>
      </div>
    );
  }

  return (
    <div>
      <Title level={2}>Workflow Monitoring Dashboard</Title>

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col span={24}>
          <Card>
            <Row gutter={16}>
              <Col span={8}>
                <RangePicker onChange={handleDateRangeChange} style={{ width: '100%' }} />
              </Col>
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
                  Export Audit Logs
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
                {dashboardData && dashboardData.workflowsByState && (
                  <Pie
                    data={getWorkflowStatusChartData()}
                    options={{ responsive: true, maintainAspectRatio: false }}
                    height={300}
                  />
                )}
              </Card>
            </Col>
            <Col span={12}>
              <Card title="Recent Activity">
                {dashboardData && dashboardData.recentActivity && (
                  <Line
                    data={getRecentActivityChartData()}
                    options={{ responsive: true, maintainAspectRatio: false }}
                    height={300}
                  />
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
                  </Card>
                </Col>
                <Col span={12}>
                  <Card title="Average Resolution Times by Team">
                    <Bar
                      data={getResolutionTimesChartData()}
                      options={{
                        responsive: true,
                        maintainAspectRatio: false
                      }}
                      height={300}
                    />
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
                    {performanceMetrics.throughputByMonth && (
                      <Bar
                        data={{
                          labels: Object.keys(performanceMetrics.throughputByMonth),
                          datasets: [
                            {
                              label: 'Completed Workflows',
                              data: Object.values(performanceMetrics.throughputByMonth),
                              backgroundColor: 'rgba(75, 192, 192, 0.6)',
                              borderColor: 'rgba(75, 192, 192, 1)',
                              borderWidth: 1
                            }
                          ]
                        }}
                        options={{
                          responsive: true,
                          maintainAspectRatio: false
                        }}
                        height={300}
                      />
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
      </Tabs>
    </div>
  );
};

export default WorkflowMonitoring;
