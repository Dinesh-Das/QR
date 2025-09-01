import { 
  CheckCircleOutlined, 
  ExclamationCircleOutlined, 
  InfoCircleOutlined,
  DatabaseOutlined,
  SyncOutlined 
} from '@ant-design/icons';
import { Card, Tag, Tooltip, Space, Typography, Row, Col, Progress, Alert } from 'antd';
import React from 'react';

const { Text, Title } = Typography;

const CqsFieldDisplay = ({ 
  field, 
  cqsData, 
  cqsFieldMapping = {}, 
  showDetails = false,
  compact = false 
}) => {
  
  // Get CQS value for this field
  const cqsValue = cqsData?.cqsData?.[field.name];
  const hasValue = cqsValue !== null && cqsValue !== undefined && cqsValue !== '';
  const displayName = cqsFieldMapping[field.name] || field.label || field.name;
  
  // Determine status
  const getStatus = () => {
    if (!field.isCqsAutoPopulated) return 'plant-input';
    if (hasValue) return 'populated';
    return 'pending';
  };
  
  const status = getStatus();
  
  // Status configurations
  const statusConfig = {
    'populated': {
      color: 'success',
      icon: <CheckCircleOutlined />,
      text: 'Auto-Populated',
      description: 'Value automatically populated from CQS system'
    },
    'pending': {
      color: 'warning', 
      icon: <ExclamationCircleOutlined />,
      text: 'Pending CQS',
      description: 'Waiting for CQS system to provide value'
    },
    'plant-input': {
      color: 'processing',
      icon: <InfoCircleOutlined />,
      text: 'Plant Input',
      description: 'Value to be provided by plant personnel'
    }
  };
  
  const config = statusConfig[status];
  
  if (compact) {
    return (
      <Space size="small">
        {field.isCqsAutoPopulated && (
          <Tooltip title={`${config.description}${hasValue ? ` - Value: ${cqsValue}` : ''}`}>
            <Tag 
              color={config.color} 
              icon={config.icon}
              size="small"
              style={{ 
                fontWeight: 'bold',
                borderRadius: '12px'
              }}
            >
              CQS {hasValue ? '✓' : '⏳'}
            </Tag>
          </Tooltip>
        )}
        {hasValue && (
          <Text 
            strong 
            style={{ 
              color: '#52c41a',
              background: '#f6ffed',
              padding: '2px 6px',
              borderRadius: '4px',
              fontSize: '12px'
            }}
          >
            {cqsValue}
          </Text>
        )}
      </Space>
    );
  }
  
  if (!showDetails && !field.isCqsAutoPopulated) {
    return null; // Don't show non-CQS fields in CQS display mode
  }
  
  return (
    <Card 
      size="small" 
      className={`cqs-field-card ${status}`}
      style={{ marginBottom: 8 }}
    >
      <Row gutter={[16, 8]} align="middle">
        <Col flex="auto">
          <Space direction="vertical" size="small" style={{ width: '100%' }}>
            <Space>
              <Tooltip title={config.description}>
                <Tag 
                  color={config.color} 
                  icon={config.icon}
                >
                  {config.text}
                </Tag>
              </Tooltip>
              <Text strong>{displayName}</Text>
            </Space>
            
            {showDetails && (
              <Text type="secondary" style={{ fontSize: '12px' }}>
                Field: {field.name}
              </Text>
            )}
          </Space>
        </Col>
        
        <Col>
          <Space direction="vertical" align="end" size="small">
            {hasValue ? (
              <Text strong style={{ color: '#52c41a' }}>
                {cqsValue}
              </Text>
            ) : field.isCqsAutoPopulated ? (
              <Text type="secondary" italic>
                Pending
              </Text>
            ) : (
              <Text type="secondary">
                Plant Input Required
              </Text>
            )}
          </Space>
        </Col>
      </Row>
    </Card>
  );
};

const CqsDataSummary = ({ 
  cqsData, 
  template, 
  cqsFieldMapping = {},
  showAllFields = false 
}) => {
  
  if (!cqsData) {
    return (
      <Alert
        message="CQS Data Not Available"
        description="CQS integration is pending implementation"
        type="info"
        icon={<DatabaseOutlined />}
        showIcon
      />
    );
  }
  
  // Calculate statistics
  const totalCqsFields = cqsData.totalFields || 0;
  const populatedFields = cqsData.populatedFields || 0;
  const completionPercentage = cqsData.completionPercentage || 0;
  
  // Get all CQS fields from template
  const cqsFields = [];
  if (template?.steps) {
    template.steps.forEach(step => {
      if (step.fields) {
        step.fields.forEach(field => {
          if (field.isCqsAutoPopulated || showAllFields) {
            cqsFields.push({
              ...field,
              stepTitle: step.title,
              stepNumber: step.stepNumber
            });
          }
        });
      }
    });
  }
  
  return (
    <Card 
      title={
        <Space>
          <DatabaseOutlined />
          <span>CQS Auto-Population Status</span>
          <Tag color={cqsData.syncStatus === 'ACTIVE' ? 'success' : 'warning'}>
            {cqsData.syncStatus || 'UNKNOWN'}
          </Tag>
        </Space>
      }
      extra={
        <Tooltip title="Refresh CQS Data">
          <SyncOutlined />
        </Tooltip>
      }
    >
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        
        {/* Progress Summary */}
        <Row gutter={[16, 16]}>
          <Col span={24}>
            <Title level={5}>Completion Progress</Title>
            <Progress 
              percent={Math.round(completionPercentage)} 
              status={completionPercentage === 100 ? 'success' : 'active'}
              format={() => `${populatedFields}/${totalCqsFields} fields`}
            />
          </Col>
        </Row>
        
        {/* Statistics */}
        <Row gutter={[16, 16]}>
          <Col span={8}>
            <Card size="small">
              <Text type="secondary">Total CQS Fields</Text>
              <br />
              <Text strong style={{ fontSize: '18px' }}>{totalCqsFields}</Text>
            </Card>
          </Col>
          <Col span={8}>
            <Card size="small">
              <Text type="secondary">Populated</Text>
              <br />
              <Text strong style={{ fontSize: '18px', color: '#52c41a' }}>
                {populatedFields}
              </Text>
            </Card>
          </Col>
          <Col span={8}>
            <Card size="small">
              <Text type="secondary">Pending</Text>
              <br />
              <Text strong style={{ fontSize: '18px', color: '#faad14' }}>
                {totalCqsFields - populatedFields}
              </Text>
            </Card>
          </Col>
        </Row>
        
        {/* Sync Status */}
        {cqsData.syncMessage && (
          <Alert
            message={cqsData.syncMessage}
            type={cqsData.syncStatus === 'ACTIVE' ? 'success' : 'info'}
            showIcon
          />
        )}
        
        {/* CQS Fields List */}
        {cqsFields.length > 0 && (
          <div>
            <Title level={5}>CQS Auto-Populated Fields</Title>
            <Space direction="vertical" style={{ width: '100%' }}>
              {cqsFields.map((field, index) => (
                <CqsFieldDisplay
                  key={`${field.name}-${index}`}
                  field={field}
                  cqsData={cqsData}
                  cqsFieldMapping={cqsFieldMapping}
                  showDetails={true}
                />
              ))}
            </Space>
          </div>
        )}
        
      </Space>
    </Card>
  );
};

export { CqsFieldDisplay, CqsDataSummary };
export default CqsFieldDisplay;