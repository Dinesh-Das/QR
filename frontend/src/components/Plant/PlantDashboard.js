import {
  ClockCircleOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  BarChartOutlined
} from '@ant-design/icons';
import { Card, Row, Col, Statistic, Progress, Alert } from 'antd';
import PropTypes from 'prop-types';
import React, { useMemo } from 'react';

/**
 * PlantDashboard component displays summary statistics and progress for plant workflows
 * Optimized with React.memo and useMemo for performance
 */
const PlantDashboard = React.memo(({ 
  dashboardStats, 
  loading, 
  error, 
  currentPlant,
  onRefresh 
}) => {
  /**
   * Calculate progress color based on completion percentage
   */
  const getProgressColor = useMemo(() => (percentage) => {
    if (percentage >= 80) return '#52c41a'; // Green
    if (percentage >= 60) return '#1890ff'; // Blue
    if (percentage >= 40) return '#faad14'; // Yellow
    if (percentage >= 20) return '#fa8c16'; // Orange
    return '#ff4d4f'; // Red
  }, []);

  /**
   * Calculate completion rate statistics
   */
  const completionStats = useMemo(() => {
    const { totalWorkflows, completedCount, inProgressCount, draftCount } = dashboardStats;
    
    if (totalWorkflows === 0) {
      return {
        completionRate: 0,
        inProgressRate: 0,
        draftRate: 0
      };
    }

    return {
      completionRate: Math.round((completedCount / totalWorkflows) * 100),
      inProgressRate: Math.round((inProgressCount / totalWorkflows) * 100),
      draftRate: Math.round((draftCount / totalWorkflows) * 100)
    };
  }, [dashboardStats]);

  /**
   * Get status color for statistics
   */
  const getStatisticColor = useMemo(() => (type) => {
    const colors = {
      total: '#1890ff',
      inProgress: '#fa8c16',
      completed: '#52c41a',
      average: '#52c41a'
    };
    return colors[type] || '#1890ff';
  }, []);

  if (error) {
    return (
      <Alert
        message="Dashboard Error"
        description={`Failed to load dashboard data: ${error}`}
        type="error"
        showIcon
        style={{ marginBottom: 24 }}
        action={
          onRefresh && (
            <button onClick={onRefresh} style={{ border: 'none', background: 'none', color: '#1890ff', cursor: 'pointer' }}>
              Retry
            </button>
          )
        }
      />
    );
  }

  return (
    <div style={{ marginBottom: 24 }}>
      {/* Main Statistics Row */}
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={12} sm={6} md={6} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="Total Materials"
              value={dashboardStats.totalWorkflows || 0}
              prefix={<ClockCircleOutlined />}
              valueStyle={{ color: getStatisticColor('total') }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={6} md={6} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="In Progress"
              value={dashboardStats.inProgressCount || 0}
              prefix={<ExclamationCircleOutlined />}
              valueStyle={{ color: getStatisticColor('inProgress') }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={6} md={6} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="Completed"
              value={dashboardStats.completedCount || 0}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: getStatisticColor('completed') }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={6} md={6} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="Average Progress"
              value={dashboardStats.averageCompletion || 0}
              suffix="%"
              prefix={<BarChartOutlined />}
              valueStyle={{ color: getStatisticColor('average') }}
            />
          </Card>
        </Col>
      </Row>

      {/* Progress Breakdown Row */}
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} md={8} lg={8}>
          <Card 
            title="Completion Rate" 
            loading={loading}
            size="small"
          >
            <Progress
              type="circle"
              percent={completionStats.completionRate}
              strokeColor={getProgressColor(completionStats.completionRate)}
              format={(percent) => `${percent}%`}
              size={80}
            />
            <div style={{ textAlign: 'center', marginTop: 8, fontSize: '12px', color: '#666' }}>
              {dashboardStats.completedCount} of {dashboardStats.totalWorkflows} completed
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={8} lg={8}>
          <Card 
            title="In Progress Rate" 
            loading={loading}
            size="small"
          >
            <Progress
              type="circle"
              percent={completionStats.inProgressRate}
              strokeColor="#fa8c16"
              format={(percent) => `${percent}%`}
              size={80}
            />
            <div style={{ textAlign: 'center', marginTop: 8, fontSize: '12px', color: '#666' }}>
              {dashboardStats.inProgressCount} workflows active
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={8} lg={8}>
          <Card 
            title="Draft Rate" 
            loading={loading}
            size="small"
          >
            <Progress
              type="circle"
              percent={completionStats.draftRate}
              strokeColor="#ff4d4f"
              format={(percent) => `${percent}%`}
              size={80}
            />
            <div style={{ textAlign: 'center', marginTop: 8, fontSize: '12px', color: '#666' }}>
              {dashboardStats.draftCount} not started
            </div>
          </Card>
        </Col>
      </Row>

      {/* Plant Information */}
      {currentPlant && (
        <Alert
          message={`Plant Dashboard - ${currentPlant}`}
          description="Overview of all assigned materials and their completion status for the selected plant."
          type="info"
          showIcon
          style={{ marginTop: 16 }}
        />
      )}
    </div>
  );
});

PlantDashboard.displayName = 'PlantDashboard';

PlantDashboard.propTypes = {
  dashboardStats: PropTypes.shape({
    totalWorkflows: PropTypes.number,
    completedCount: PropTypes.number,
    inProgressCount: PropTypes.number,
    draftCount: PropTypes.number,
    averageCompletion: PropTypes.number,
    completedToday: PropTypes.number
  }).isRequired,
  loading: PropTypes.bool,
  error: PropTypes.string,
  currentPlant: PropTypes.string,
  onRefresh: PropTypes.func
};

PlantDashboard.defaultProps = {
  loading: false,
  error: null,
  currentPlant: null,
  onRefresh: null
};

export default PlantDashboard;