import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  FileTextOutlined,
  EyeOutlined,
  InfoCircleOutlined,
  UserOutlined,
  CalendarOutlined,
  CheckOutlined,
  ExclamationCircleOutlined,
  DashboardOutlined,
  BarChartOutlined,
  TeamOutlined,
  SettingOutlined,
  BulbOutlined,
  StarOutlined,
  ThunderboltOutlined,
  SafetyOutlined,
  RocketOutlined,
  CrownOutlined
} from '@ant-design/icons';
import {
  Card,
  Spin,
  Alert,
  Typography,
  Row,
  Col,
  Tag,
  Divider,
  Steps,
  Collapse,
  Form,
  Input,
  Radio,
  Select,
  Button,
  Space,
  Progress,
  Badge,
  Statistic,
  Avatar,
  Timeline,
  Tabs,
  Empty,
  Tooltip,
  Affix
} from 'antd';
import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';


import apiClient from '../api/client';
import { calculateCorrectFieldCounts } from '../utils/questionnaireUtils';

const { Title, Text, Paragraph } = Typography;
const { Panel } = Collapse;
const { TextArea } = Input;
const { TabPane } = Tabs;

const QuestionnaireViewerPage = ({ workflowId: propWorkflowId, onClose }) => {
  const { workflowId: paramWorkflowId } = useParams();
  const navigate = useNavigate();

  // Use prop workflowId if provided (for modal), otherwise use URL param
  const workflowId = propWorkflowId || paramWorkflowId;
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [questionnaireData, setQuestionnaireData] = useState(null);
  const [activeStep, setActiveStep] = useState(0);
  const [activeView, setActiveView] = useState('overview');

  console.log('QuestionnaireViewerPage mounted with workflowId:', workflowId);

  // Calculate completion metrics at component level for use across tabs
  const plantInputs = questionnaireData?.plantData?.plantInputs || {};
  const { totalFields, completedFields, completionPercentage } =
    questionnaireData ? calculateCorrectFieldCounts(plantInputs, questionnaireData.template?.steps) :
      { totalFields: 0, completedFields: 0, completionPercentage: 0 };

  // Create overview dashboard
  const renderOverviewDashboard = (questionnaireData) => {
    const { template, plantData, workflow, completionStatus } = questionnaireData;

    const totalSteps = template?.steps?.length || 0;
    const completedSteps = template?.steps?.filter(step => {
      const stepFields = step.fields || [];
      // Count ALL fields (both CQS and plant fields) for step completion
      const completedFields = stepFields.filter(field => {
        const value = plantData?.plantInputs?.[field.name] || '';
        return value && value.trim() !== '';
      });
      return stepFields.length > 0 && completedFields.length === stepFields.length;
    }).length || 0;

    const overallProgress = totalSteps > 0 ? Math.round((completedSteps / totalSteps) * 100) : 0;

    return (
      <div style={{ padding: '0 4px' }}>
        {/* Hero Stats */}
        <Row gutter={[24, 24]} style={{ marginBottom: 32 }}>
          <Col xs={24} sm={12} lg={6}>
            <Card style={{
              background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
              border: 'none',
              borderRadius: 16,
              color: 'white',
              textAlign: 'center'
            }}>
              <Statistic
                title={<span style={{ color: 'rgba(255,255,255,0.8)' }}>Overall Progress</span>}
                value={overallProgress}
                suffix="%"
                valueStyle={{ color: 'white', fontSize: 36, fontWeight: 700 }}
                prefix={<RocketOutlined />}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card style={{
              background: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
              border: 'none',
              borderRadius: 16,
              color: 'white',
              textAlign: 'center'
            }}>
              <Statistic
                title={<span style={{ color: 'rgba(255,255,255,0.8)' }}>Completed Steps</span>}
                value={completedSteps}
                suffix={`/ ${totalSteps}`}
                valueStyle={{ color: 'white', fontSize: 36, fontWeight: 700 }}
                prefix={<CheckCircleOutlined />}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card style={{
              background: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
              border: 'none',
              borderRadius: 16,
              color: 'white',
              textAlign: 'center'
            }}>
              <Statistic
                title={<span style={{ color: 'rgba(255,255,255,0.8)' }}>Total Fields</span>}
                value={totalFields}
                valueStyle={{ color: 'white', fontSize: 36, fontWeight: 700 }}
                prefix={<FileTextOutlined />}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card style={{
              background: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
              border: 'none',
              borderRadius: 16,
              color: 'white',
              textAlign: 'center'
            }}>
              <Statistic
                title={<span style={{ color: 'rgba(255,255,255,0.8)' }}>Completed Fields</span>}
                value={completedFields}
                valueStyle={{ color: 'white', fontSize: 36, fontWeight: 700 }}
                prefix={<StarOutlined />}
              />
            </Card>
          </Col>
        </Row>

        {/* Progress Timeline */}
        <Card
          title={
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <Avatar style={{ backgroundColor: '#1890ff' }} icon={<BarChartOutlined />} />
              <span>Step Progress Timeline</span>
            </div>
          }
          style={{ borderRadius: 16, marginBottom: 24 }}
        >
          <Timeline mode="left">
            {template?.steps?.map((step, index) => {
              const stepFields = step.fields || [];
              // Count ALL fields (both CQS and plant fields) for step progress
              const completedFields = stepFields.filter(field => {
                const value = plantData?.plantInputs?.[field.name] || '';
                return value && value.trim() !== '';
              });
              const stepProgress = stepFields.length > 0 ? Math.round((completedFields.length / stepFields.length) * 100) : 0;
              const isCompleted = stepProgress === 100;

              return (
                <Timeline.Item
                  key={index}
                  color={isCompleted ? '#52c41a' : stepProgress > 0 ? '#faad14' : '#d9d9d9'}
                  dot={
                    <Avatar
                      size={32}
                      style={{
                        backgroundColor: isCompleted ? '#52c41a' : stepProgress > 0 ? '#faad14' : '#d9d9d9'
                      }}
                    >
                      {isCompleted ? <CheckOutlined /> : step.stepNumber}
                    </Avatar>
                  }
                >
                  <div style={{ paddingBottom: 16 }}>
                    <Title level={5} style={{ margin: 0, marginBottom: 8 }}>
                      {step.title}
                    </Title>
                    <Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
                      {step.description}
                    </Text>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                      <Progress
                        percent={stepProgress}
                        size="small"
                        style={{ flex: 1 }}
                        strokeColor={isCompleted ? '#52c41a' : '#1890ff'}
                      />
                      <Text strong style={{ fontSize: 13 }}>
                        {completedFields.length}/{stepFields.length} fields
                      </Text>
                    </div>
                  </div>
                </Timeline.Item>
              );
            })}
          </Timeline>
        </Card>
      </div>
    );
  };

  const loadQuestionnaireData = useCallback(async () => {
    try {
      setLoading(true);
      console.log('Loading questionnaire data for workflowId:', workflowId);
      const response = await apiClient.get(`/questionnaire/${workflowId}`);
      console.log('Questionnaire response:', response);
      setQuestionnaireData(response);
      setError(null);
    } catch (err) {
      console.error('Error loading questionnaire:', err);
      console.error('Error details:', err.response?.data || err.message);
      setError('Failed to load questionnaire data. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [workflowId]);

  useEffect(() => {
    if (workflowId) {
      loadQuestionnaireData();
    } else {
      setError('No workflow ID provided');
      setLoading(false);
    }
  }, [loadQuestionnaireData, workflowId]);

  const renderFieldValue = (field, plantInputs, cqsData) => {
    const value = plantInputs[field.name] || field.cqsValue || '';
    const isEmpty = !value || value.trim() === '';
    const isCqsField = field.cqsAutoPopulated;

    // Format value based on field type
    let displayValue = value;
    if (!isEmpty && field.type === 'radio' && field.options) {
      const option = field.options.find(opt => opt.value === value);
      displayValue = option ? option.label : value;
    } else if (!isEmpty && field.type === 'select' && field.options) {
      const option = field.options.find(opt => opt.value === value);
      displayValue = option ? option.label : value;
    }

    const getFieldIcon = (type) => {
      switch (type) {
        case 'radio': return <CheckCircleOutlined />;
        case 'select': return <SettingOutlined />;
        case 'text': return <FileTextOutlined />;
        case 'textarea': return <FileTextOutlined />;
        default: return <InfoCircleOutlined />;
      }
    };

    return (
      <Col xs={24} sm={12} lg={8} key={field.name} style={{ marginBottom: 16 }}>
        <Card
          hoverable
          style={{
            height: '100%',
            borderRadius: 16,
            border: isEmpty ? '2px dashed #e8e8e8' : isCqsField ? '2px solid #1890ff' : '2px solid #f0f0f0',
            background: isEmpty ? '#fafafa' : isCqsField ? 'linear-gradient(135deg, #e6f7ff 0%, #f0f9ff 100%)' : '#ffffff',
            transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
            position: 'relative',
            overflow: 'hidden'
          }}
          bodyStyle={{ padding: '20px' }}
        >
          {/* Status indicator */}
          <div style={{
            position: 'absolute',
            top: 0,
            right: 0,
            width: 0,
            height: 0,
            borderLeft: '30px solid transparent',
            borderTop: isEmpty ? '30px solid #faad14' : '30px solid #52c41a'
          }} />

          {/* Field header */}
          <div style={{ marginBottom: 16, display: 'flex', alignItems: 'flex-start', gap: 12 }}>
            <Avatar
              size={40}
              style={{
                backgroundColor: isEmpty ? '#faad14' : isCqsField ? '#1890ff' : '#52c41a',
                flexShrink: 0
              }}
              icon={getFieldIcon(field.type)}
            />
            <div style={{ flex: 1, minWidth: 0 }}>
              <Text strong style={{
                fontSize: 15,
                color: '#262626',
                display: 'block',
                lineHeight: 1.3,
                marginBottom: 4
              }}>
                {field.label}
                {field.required && <Text type="danger" style={{ marginLeft: 4 }}>*</Text>}
              </Text>
              <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                <Tag
                  color={field.type === 'radio' ? 'blue' : field.type === 'select' ? 'purple' : 'green'}
                  style={{ fontSize: 10, padding: '0 6px', borderRadius: 8 }}
                >
                  {field.type.toUpperCase()}
                </Tag>
                {isCqsField && (
                  <Tag color="processing" style={{ fontSize: 10, padding: '0 6px', borderRadius: 8 }}>
                    AUTO
                  </Tag>
                )}
              </div>
            </div>
          </div>

          {/* Field value */}
          <div style={{
            padding: '16px',
            backgroundColor: isEmpty ? 'rgba(250, 173, 20, 0.1)' : 'rgba(82, 196, 26, 0.1)',
            borderRadius: 12,
            border: isEmpty ? '1px solid rgba(250, 173, 20, 0.3)' : '1px solid rgba(82, 196, 26, 0.3)',
            minHeight: 60,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            textAlign: 'center'
          }}>
            {isEmpty ? (
              <div>
                <ExclamationCircleOutlined style={{
                  color: '#faad14',
                  fontSize: 20,
                  marginBottom: 8,
                  display: 'block'
                }} />
                <Text type="secondary" style={{ fontSize: 13, fontStyle: 'italic' }}>
                  No data provided
                </Text>
              </div>
            ) : (
              <div>
                <CheckOutlined style={{
                  color: '#52c41a',
                  fontSize: 16,
                  marginBottom: 8,
                  display: 'block'
                }} />
                <Text strong style={{ fontSize: 14, color: '#262626' }}>
                  {displayValue}
                </Text>
              </div>
            )}
          </div>

          {/* Help text */}
          {field.helpText && (
            <div style={{
              marginTop: 12,
              padding: '8px 12px',
              backgroundColor: 'rgba(24, 144, 255, 0.05)',
              borderRadius: 8,
              borderLeft: '3px solid #1890ff'
            }}>
              <Text type="secondary" style={{ fontSize: 11, lineHeight: 1.4 }}>
                <BulbOutlined style={{ marginRight: 4, color: '#1890ff' }} />
                {field.helpText}
              </Text>
            </div>
          )}
        </Card>
      </Col>
    );
  };

  const renderStepDetails = (step, plantInputs, cqsData) => {
    // Count ALL fields (both CQS and plant fields) for step details
    const stepFields = step.fields || [];
    const completedFields = stepFields.filter(field => {
      const value = plantInputs[field.name] || '';
      return value && value.trim() !== '';
    }).length;

    const completionRate = stepFields.length > 0 ? (completedFields / stepFields.length) * 100 : 0;

    return (
      <div key={step.stepNumber} style={{ marginBottom: 32 }}>
        {/* Step Header */}
        <Card
          style={{
            marginBottom: 24,
            borderRadius: 16,
            background: `linear-gradient(135deg, ${completionRate === 100 ? '#52c41a' : completionRate > 0 ? '#faad14' : '#8c8c8c'
              } 0%, ${completionRate === 100 ? '#73d13d' : completionRate > 0 ? '#ffc53d' : '#bfbfbf'
              } 100%)`,
            border: 'none',
            color: 'white'
          }}
          bodyStyle={{ padding: '24px 32px' }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
              <Avatar
                size={48}
                style={{
                  backgroundColor: 'rgba(255,255,255,0.2)',
                  color: 'white',
                  fontSize: 20,
                  fontWeight: 700
                }}
              >
                {step.stepNumber}
              </Avatar>
              <div>
                <Title level={3} style={{ margin: 0, color: 'white', fontWeight: 600 }}>
                  {step.title}
                </Title>
                <Text style={{ color: 'rgba(255,255,255,0.8)', fontSize: 14 }}>
                  {completedFields} of {stepFields.length} fields completed • {Math.round(completionRate)}% done
                </Text>
              </div>
            </div>
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: 32, fontWeight: 700, marginBottom: 4 }}>
                {Math.round(completionRate)}%
              </div>
              <Progress
                percent={completionRate}
                showInfo={false}
                strokeColor="rgba(255,255,255,0.8)"
                trailColor="rgba(255,255,255,0.2)"
                style={{ width: 120 }}
              />
            </div>
          </div>
        </Card>

        {/* Step Description */}
        {step.description && (
          <Card
            style={{
              marginBottom: 24,
              borderRadius: 12,
              borderLeft: '4px solid #1890ff',
              backgroundColor: '#f6ffed'
            }}
            bodyStyle={{ padding: '20px 24px' }}
          >
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
              <InfoCircleOutlined style={{ color: '#1890ff', fontSize: 16, marginTop: 2 }} />
              <Text style={{ fontSize: 14, color: '#595959', lineHeight: 1.6 }}>
                {step.description}
              </Text>
            </div>
          </Card>
        )}

        {/* Fields Grid */}
        <Card
          title={
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <Avatar style={{ backgroundColor: '#722ed1' }} icon={<SafetyOutlined />} />
              <span>Field Details</span>
              <Badge count={step.fields.length} style={{ backgroundColor: '#1890ff' }} />
            </div>
          }
          style={{ borderRadius: 16 }}
          bodyStyle={{ padding: '24px' }}
        >
          {step.fields.length > 0 ? (
            <Row gutter={[16, 16]}>
              {step.fields.map(field => renderFieldValue(field, plantInputs, cqsData))}
            </Row>
          ) : (
            <Empty
              description="No fields available for this step"
              image={Empty.PRESENTED_IMAGE_SIMPLE}
            />
          )}
        </Card>
      </div>
    );
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '60px 20px' }}>
        <Spin size="large" />
        <div style={{ marginTop: 16 }}>Loading questionnaire data...</div>
        <div style={{ marginTop: 8, color: '#666' }}>Workflow ID: {workflowId}</div>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ padding: 24 }}>
        <Card>
          <Button
            icon={<ArrowLeftOutlined />}
            onClick={() => onClose ? onClose() : navigate(-1)}
            style={{ marginBottom: 16 }}
          >
            Back
          </Button>
          <Alert
            message="Error Loading Questionnaire"
            description={
              <div>
                <div>{error}</div>
                <div style={{ marginTop: 8, fontSize: '12px', color: '#666' }}>
                  Workflow ID: {workflowId}
                </div>
              </div>
            }
            type="error"
            showIcon
            action={
              <Button size="small" onClick={loadQuestionnaireData}>
                Retry
              </Button>
            }
          />
        </Card>
      </div>
    );
  }

  if (!questionnaireData) {
    return (
      <div style={{ padding: 24 }}>
        <Alert
          message="No Data Found"
          description="Questionnaire data not found."
          type="warning"
          showIcon
        />
      </div>
    );
  }

  const { template, plantData, workflow, completionStatus, cqsData, accessControl } = questionnaireData;
  const cqsDataValues = cqsData?.data || {};



  // Debug logging to see the actual data structure
  console.log('Full questionnaire data:', questionnaireData);
  console.log('Plant data:', plantData);
  console.log('Plant inputs:', plantInputs);
  console.log('Plant inputs keys:', Object.keys(plantInputs));
  console.log('Template steps:', template?.steps);

  // Log first few fields to see what field names we're looking for
  if (template?.steps?.[0]?.fields) {
    console.log('First step fields:', template.steps[0].fields.map(f => ({ name: f.name, label: f.label })));
  }

  return (
    <div style={{
      background: 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)',
      minHeight: '100vh',
      padding: '16px'
    }}>
      <div style={{ maxWidth: 1400, margin: '0 auto' }}>
        {/* Floating Header */}
        <Affix offsetTop={16}>
          <Card
            style={{
              marginBottom: 24,
              borderRadius: 20,
              border: 'none',
              boxShadow: '0 12px 40px rgba(0,0,0,0.15)',
              background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
              color: 'white'
            }}
            bodyStyle={{ padding: '20px 32px' }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 20 }}>
                <Button
                  icon={<ArrowLeftOutlined />}
                  onClick={() => onClose ? onClose() : navigate(-1)}
                  style={{
                    backgroundColor: 'rgba(255,255,255,0.15)',
                    border: '2px solid rgba(255,255,255,0.3)',
                    color: 'white',
                    borderRadius: 12,
                    height: 44,
                    fontWeight: 600
                  }}
                  size="large"
                >
                  Back
                </Button>
                <div>
                  <Title level={2} style={{ margin: 0, color: 'white', fontWeight: 700 }}>
                    🚀 QRMFG Questionnaire Dashboard
                  </Title>
                  <Text style={{ color: 'rgba(255,255,255,0.9)', fontSize: 15 }}>
                    {workflow?.materialName} • {workflow?.plantCode} • Workflow #{workflowId}
                  </Text>
                </div>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                <Tag
                  style={{
                    backgroundColor: 'rgba(255,255,255,0.15)',
                    border: '2px solid rgba(255,255,255,0.3)',
                    color: 'white',
                    borderRadius: 20,
                    padding: '6px 16px',
                    fontSize: 13,
                    fontWeight: 600
                  }}
                >
                  <EyeOutlined /> READ-ONLY
                </Tag>
                <Avatar.Group>
                  <Tooltip title={accessControl?.currentUser}>
                    <Avatar style={{ backgroundColor: '#f56a00' }} icon={<UserOutlined />} />
                  </Tooltip>
                </Avatar.Group>
              </div>
            </div>
          </Card>
        </Affix>

        {/* Main Content with Tabs */}
        <Card
          style={{
            borderRadius: 20,
            border: 'none',
            boxShadow: '0 8px 32px rgba(0,0,0,0.1)',
            overflow: 'hidden'
          }}
          bodyStyle={{ padding: 0 }}
        >
          <Tabs
            activeKey={activeView}
            onChange={setActiveView}
            size="large"
            style={{ margin: 0 }}
            tabBarStyle={{
              margin: 0,
              padding: '0 32px',
              backgroundColor: '#fafbfc',
              borderBottom: '2px solid #f0f0f0'
            }}
          >
            <TabPane
              tab={
                <span style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 15, fontWeight: 600 }}>
                  <DashboardOutlined />
                  Overview Dashboard
                </span>
              }
              key="overview"
            >
              <div style={{ padding: '32px' }}>
                {renderOverviewDashboard(questionnaireData)}
              </div>
            </TabPane>

            <TabPane
              tab={
                <span style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 15, fontWeight: 600 }}>
                  <FileTextOutlined />
                  Detailed Steps
                  <Badge count={template?.steps?.length || 0} style={{ backgroundColor: '#1890ff' }} />
                </span>
              }
              key="details"
            >
              <div style={{ padding: '32px', backgroundColor: '#fafbfc' }}>
                {template?.steps?.length > 0 ? (
                  template.steps.map(step =>
                    renderStepDetails(step, plantInputs, cqsDataValues)
                  )
                ) : (
                  <Empty
                    description="No questionnaire steps available"
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                    style={{ padding: '60px 0' }}
                  />
                )}
              </div>
            </TabPane>

            <TabPane
              tab={
                <span style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 15, fontWeight: 600 }}>
                  <BarChartOutlined />
                  Analytics
                </span>
              }
              key="analytics"
            >
              <div style={{ padding: '32px' }}>
                <Row gutter={[24, 24]}>
                  <Col span={24}>
                    <Card
                      title={
                        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                          <Avatar style={{ backgroundColor: '#722ed1' }} icon={<CrownOutlined />} />
                          <span>Completion Analytics</span>
                        </div>
                      }
                      style={{ borderRadius: 16 }}
                    >
                      <Row gutter={[24, 24]}>
                        <Col span={8}>
                          <Statistic
                            title="Completion Rate"
                            value={completionPercentage}
                            suffix="%"
                            valueStyle={{ color: '#3f8600' }}
                            prefix={<ThunderboltOutlined />}
                          />
                        </Col>
                        <Col span={8}>
                          <Statistic
                            title="Fields Remaining"
                            value={totalFields - completedFields}
                            valueStyle={{ color: '#cf1322' }}
                            prefix={<ClockCircleOutlined />}
                          />
                        </Col>
                        <Col span={8}>
                          <Statistic
                            title="Auto-populated Fields"
                            value={template?.steps?.reduce((acc, step) =>
                              acc + (step.fields?.filter(f => f.cqsAutoPopulated)?.length || 0), 0) || 0}
                            valueStyle={{ color: '#1890ff' }}
                            prefix={<RocketOutlined />}
                          />
                        </Col>
                      </Row>
                    </Card>
                  </Col>
                </Row>
              </div>
            </TabPane>
          </Tabs>
        </Card>
      </div>
    </div>
  );
};

export default QuestionnaireViewerPage;